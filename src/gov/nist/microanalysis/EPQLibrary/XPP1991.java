package gov.nist.microanalysis.EPQLibrary;

import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Implements the XPP algorithm as described in Pouchou &amp; Pichoir in
 * Electron Probe Quantitation. The uncertainty calculation is described in
 * Ritchie &amp; Newbury, 2012 Anal. Chem. 2012, 84, 9956−9962.
 * dx.doi.org/10.1021/ac301843h
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class XPP1991 extends CorrectionAlgorithm.PhiRhoZAlgorithm implements CorrectionAlgorithm.IAbsorptionSensitivity {

   public static final String UNCERTAINTY_COMP = "C";
   public static final String UNCERTAINTY_ETA = "\u03B7";
   public static final String UNCERTAINTY_MAC = "[\u03BC/\u03C1]";
   public static final String[] ALL_COMPONENTS = new String[]{UNCERTAINTY_MAC, UNCERTAINTY_ETA, UNCERTAINTY_COMP};

   static protected final double SQRT_TWO = Math.sqrt(2.0);
   static protected final double TINY = 1.0e-6;
   static private final double ABSOLUTE_UNCERTAINTY_ETA = 0.01;
   protected double mPhi0;
   protected double mBigA;
   protected double mBigB;
   protected double mLittleA;
   protected double mLittleB;
   // Integral of the area under the phi(rho z) curve
   protected double mF;
   protected double mEps;

   /**
    * Constructs an object implementing the XPP correction algorithm.
    */
   public XPP1991() {
      super("XPP - Pouchou & Pichoir Simplified", LitReference.PAPinEPQ);
   }

   /**
    * Constructs an object implementing the XPP correction algorithm. For
    * extension only.
    * 
    * @param name
    * @param ref
    */
   protected XPP1991(String name, LitReference ref) {
      super(name, ref);
   }

   @Override
   protected void initializeDefaultStrategy() {
      super.initializeDefaultStrategy();
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
      addDefaultAlgorithm(BackscatterFactor.class, BackscatterFactor.Pouchou1991);
      addDefaultAlgorithm(SurfaceIonization.class, SurfaceIonization.Pouchou1991);
      addDefaultAlgorithm(StoppingPower.class, StoppingPower.Pouchou1991);
      addDefaultAlgorithm(ProportionalIonizationCrossSection.class, ProportionalIonizationCrossSection.Pouchou86);
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props) throws EPQException {
      comp = comp.normalize();
      final boolean res = super.initialize(comp, shell, props);
      if (res) {
         final SurfaceIonization si = (SurfaceIonization) getAlgorithm(SurfaceIonization.class);
         final BackscatterFactor bf = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
         final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
         final ProportionalIonizationCrossSection icx = (ProportionalIonizationCrossSection) getAlgorithm(ProportionalIonizationCrossSection.class);
         // Compute the total area under the phi(rho z) curve. Same as PAP when
         // corrected.
         // mF = (R/S)*(1/Qj(E0))
         mF = (bf.compute(mComposition, mShell, mBeamEnergy) * StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(mComposition, mShell, mBeamEnergy)))
               / icx.computeFamily(mShell, mBeamEnergy);
         assert mF > 0.0 : "The integral must be larger than zero.";
         mPhi0 = si.compute(mComposition, mShell, mBeamEnergy);
         final double meanZb = PAP1991.papMeanAtomicNumber(mComposition);
         final double u0 = mBeamEnergy / shell.getEdgeEnergy();
         // Compute the mean depth of ionization (rBar)
         double rBar;
         {
            // x1 = 1.0 + 1.3 * log(Zb);
            final double x = 1.0 + (1.3 * Math.log(meanZb));
            // y1 = Zb / 200. + .2;
            final double y = 0.2 + (meanZb / 200.0);
            // F / (1.0 + (x1 * log( 1.0 + y1 * (1.0 - 1.0/pow(u0, 0.42) ) ) ) /
            // log( 1.0 + y1));
            rBar = mF / (1.0 + ((x * Math.log(1.0 + (y * (1.0 - Math.pow(u0, -0.42))))) / Math.log(1.0 + y)));
            // Impose the special condition
            if ((mF / rBar) < mPhi0)
               rBar = mF / mPhi0;
         }
         // Calculate the initial slope (p)
         double p;
         {
            // g1 = 0.22 * log(Zb * 4.) * (1.0 - 2.0 * exp( Zb * (u0 - 1.) /
            // -15.0 ) );
            final double g = 0.22 * Math.log(4.0 * meanZb) * (1.0 - (2.0 * Math.exp((meanZb * (1.0 - u0)) / 15.0)));
            // h1 = 1.0 - 10.0 * (1.0 - 1.0 / (u0 / 10.0 + 1.0)) / ( Zb * Zb );
            final double h = 1.0 - ((10.0 * (1.0 - (1.0 / (1.0 + (u0 / 10.0))))) / (meanZb * meanZb));
            double gh4 = g * (h * h) * (h * h);
            // b = sqrt(2.0) * ( 1.0 + sqrt(1.0 - Rbar * phi0 / F) ) / Rbar;
            mLittleB = (SQRT_TWO * (1.0 + Math.sqrt(1.0 - ((rBar * mPhi0) / mF)))) / rBar;
            if (true) {
               final double limit = 0.9 * mLittleB * rBar * rBar * (mLittleB - ((2.0 * mPhi0) / mF));
               if (gh4 > limit)
                  gh4 = limit;
            }
            // pp = g1 * h4 * FonR / Rbar;
            p = (gh4 * mF) / (rBar * rBar);
         }
         // a = (pp + b * (phi0 * 2.0 - b * F)) / (b * F * (2 - b * Rbar) -
         // phi0);
         mLittleA = (p + (mLittleB * ((2.0 * mPhi0) - (mLittleB * mF)))) / ((mLittleB * mF * (2.0 - (mLittleB * rBar))) - mPhi0);
         mEps = (mLittleA - mLittleB) / mLittleB;
         if (Math.abs(mEps) < TINY) {
            mEps = Math.signum(mEps)*TINY;
            mLittleA = mLittleB * (1.0 + mEps);
         }
         // B = (b * b * F * (atemp + 1.0) - pp - phi0 * b * (atemp + 2.0)) /
         // atemp;
         mBigB = ((mLittleB * mLittleB * mF * (1.0 + mEps)) - p - (mPhi0 * mLittleB * (2.0 + mEps))) / mEps;
         // A = (B / b + phi0 - b * F) * (atemp + 1) / atemp;
         mBigA = ((((mBigB / mLittleB) + mPhi0) - (mLittleB * mF)) * (1 + mEps)) / mEps;
      }
      return res;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm#computeZACorrection(gov.nist.microanalysis.EPQLibrary.XRayTransition)
    */
   @Override
   public double computeZACorrection(XRayTransition xrt) throws EPQException {
      final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
      assert chi >= 0.0 : "chi = " + Double.toString(chi);
      assert chi >= 0.0;
      return toSI(((mPhi0 + (mBigB / (mLittleB + chi))) - ((mBigA * mLittleB * mEps) / ((mLittleB * (1.0 + mEps)) + chi))) / (mLittleB + chi));
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm#generated(gov.nist.microanalysis.EPQLibrary.XRayTransition)
    */
   @Override
   public double generated(XRayTransition xrt) {
      return toSI(mF);
   }

   @Override
   public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
      return CaveatBase.None;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm.PhiRhoZAlgorithm#computeCurve(double)
    */
   @Override
   public double computeCurve(double rhoZ) {
      if (rhoZ >= 0.0) {
         // rhoZ in kg/m^2 -> rhoZ in g/cm^2
         rhoZ *= 0.1;
         return (mBigA * Math.exp(-mLittleA * rhoZ)) + ((((mBigB * rhoZ) + mPhi0) - mBigA) * Math.exp(-mLittleB * rhoZ));
      } else
         return 0.0;
   }

   /**
    * Converts from mg/cm<sup>2</sup> to kg/m<sup>2</sup>
    *
    * @param rhoZ
    *           in mg/cm<sup>2</sup>
    * @return rhoZ in kg/m<sup>2</sup>
    */
   public double rhoZ_kgPerSqrMeter(double rhoZ) {
      // 1 mg/cm2 = (1 mg)(0.001 g/mg)(0.001 kg/g)/((0.01 m/cm)(1 cm))^2
      // 1 mg/cm2 = 1.0e-6 kg * 10^4 1/m^2
      // 1 mg/cm2 = 1.0e-2 kg/m^2
      return 0.01 * rhoZ;
   }

   /**
    * Converts from kg/m<sup>2</sup> to g/cm<sup>2</sup>
    *
    * @param rhoZ
    *           in kg/m<sup>2</sup>
    * @return rhoZ in g/cm<sup>2</sup>
    */
   public double rhoZ_gPerSqrCentimeter(double rhoZ) {
      // 1 kg/m2 = (1 kg)(1000.0 g/kg)/((100.0 cm/m)(1 m))^2
      // 1 kg/m2 = 1000.0 g * 10^-4 1/cm^2
      // 1 mg/cm2 = 0.1 g/cm^2
      return 0.1 * rhoZ;
   }

   private static final double cosec(double theta) {
      return 1.0 / Math.sin(theta);
   }

   /**
    * This is Eqn (8) times (sin(psi)/C_z)
    * 
    * @param xrt
    * @return double
    * @throws EPQException
    */
   private double termX(final XRayTransition xrt) throws EPQException {
      final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
      final double lbx = mLittleB + chi;
      return ((mBigA * ((1.0 / (lbx * lbx)) - (1.0 / Math2.sqr(mLittleA + chi)))) - (((2.0 * mBigB) + (mPhi0 * lbx)) / Math.pow(lbx, 3.0)));
   }

   private double dR(double u0) {
      // Weight averaged mean Z from Appendix 1 in XPP
      double zb = 0.0;
      {
         final Composition comp = mComposition;
         // If we don't normalize the weight fraction here then the eta
         // uncertainty blows up when the analytical total is substantially
         // above unity.
         final double total = Math.min(1.1, comp.sumWeightFraction());
         for (final Element elm : comp.getElementSet())
            zb += Math.sqrt(elm.getAtomicNumber()) * (comp.weightFraction(elm, false) / total);
         zb = Math2.sqr(zb);
      }
      // Third eqn in Appendix 1 of XPP
      final double etaBar = (1.75e-3 * zb) + (0.37 * (1.0 - Math.exp(-0.015 * Math.pow(zb, 1.3)))); // Ok
      // Eqn 11
      final double dWb = 0.27027 + (4.55 * Math.pow(etaBar, 3.55)); // Ok
      // Second eqn in Appendix 1 of XPP
      final double wb = 0.595 + (etaBar / 3.7) + Math.pow(etaBar, 4.55);

      final double dq = dWb / Math2.sqr(wb - 1.0); // Ok
      final double q = ((2.0 * wb) - 1.0) / (1.0 - wb); // Ok
      final double opq = 1.0 + q;
      final double tpq = 2.0 + q;
      final double up = Math.pow(u0, -opq);
      final double ju = 1.0 + (u0 * (Math.log(u0) - 1.0)); // Ok
      // double xx = -((-1.0 + u0 - (1.0 - Math.pow(u0, -opq)) / opq) / (ju *
      // tpq * tpq))
      final double dG = ((((1.0 - u0) + ((1 - up) / opq)) / (ju * tpq * tpq))
            + ((((1 - up) / (opq * opq)) - ((up * Math.log(u0)) / opq)) / (ju * tpq))) * dq; // Fixed
                                                                                             // 6-Mar-2012
      // XPP Appendix 1
      final double g = (u0 - 1.0 - ((1.0 - (1.0 * up)) / opq)) / (tpq * ju); // Ok
      // Eqn 11 (fixed 6-Mar-2013)
      return (etaBar * wb * dG) - ((1.0 - g) * (wb + (etaBar * dWb)));
   }

   /**
    * Computes the u(&eta;) component of the uncertainty budget in Eqn. 7.
    * 
    * @param stdXpp
    * @param unkXpp
    * @param xrt
    * @return u(&eta;) as a double
    * @throws EPQException
    */
   public static double uEta(XPP1991 stdXpp, XPP1991 unkXpp, XRayTransition xrt) throws EPQException {
      final StoppingPower sp = (StoppingPower) stdXpp.getAlgorithm(StoppingPower.class);
      final double u0 = stdXpp.mBeamEnergy / xrt.getEnergy();
      double stdZA, unkZA, dIstd, dIunk;
      Composition cUnk;
      {
         final double dRstd = stdXpp.dR(u0);
         final double iSstd = StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(stdXpp.mComposition, xrt.getDestination(), stdXpp.mBeamEnergy));
         stdZA = stdXpp.computeZACorrection(xrt);
         // Implements Eqn 13
         dIstd = ((iSstd * stdZA) / stdXpp.generated(xrt)) * dRstd;
      }
      {
         final double dRunk = unkXpp.dR(u0);
         cUnk = unkXpp.mComposition;
         final double iSunk = StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(cUnk, xrt.getDestination(), stdXpp.mBeamEnergy));
         unkZA = unkXpp.computeZACorrection(xrt);
         // Implements Eqn 13
         dIunk = ((iSunk * unkZA) / unkXpp.generated(xrt)) * dRunk;
      }
      // From Eqn 5, k*cStd*stdF/unkF = cUnk*unkZA/stdZA
      final double res = cUnk.weightFraction(xrt.getElement(), false) * (unkZA / stdZA)
            * Math.sqrt(Math2.sqr(dIstd / stdZA) + Math2.sqr((stdZA * dIunk) / (unkZA * unkZA))) * ABSOLUTE_UNCERTAINTY_ETA;
      return res;
   }

   /**
    * This implements the term in Eqn 7 corresponding to u([&mu;/&rho;]).
    * 
    * @param stdXpp
    * @param unkXpp
    * @param xrt
    * @return u([&mu;/&rho;]) as a double
    * @throws EPQException
    */
   public static double uMAC(XPP1991 stdXpp, XPP1991 unkXpp, XRayTransition xrt) throws EPQException {
      final double csc = cosec(unkXpp.mTakeOffAngle);
      final double unkTermX = unkXpp.termX(xrt);
      final double unkZA = fromSI(unkXpp.computeZACorrection(xrt));
      final double stdTermX = stdXpp.termX(xrt);
      final double stdZA = fromSI(stdXpp.computeZACorrection(xrt));
      final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) unkXpp.getAlgorithm(MassAbsorptionCoefficient.class);
      final Set<Element> allElms = new TreeSet<Element>();
      allElms.addAll(stdXpp.mComposition.getElementSet());
      allElms.addAll(unkXpp.mComposition.getElementSet());
      double sumSqr = 0.0;
      // Implements line 3 in Eqn 7
      for (final Element elm : allElms) {
         // This is Eqn 8 for the standard
         final double dIstd = stdTermX * stdXpp.mComposition.weightFraction(elm, false) * csc;
         // This is Eqn 8 for the unknown
         final double dIunk = unkTermX * unkXpp.mComposition.weightFraction(elm, false) * csc;
         // From Eqn 7
         final double dIoI = (dIstd - ((stdZA * dIunk) / unkZA)) / unkZA;
         final UncertainValue2 mu = mac.computeWithUncertaintyEstimate(elm, xrt);
         sumSqr += Math2.sqr(dIoI * MassAbsorptionCoefficient.toCmSqrPerGram(mu.uncertainty()));
      }
      final double cUnk = unkXpp.mComposition.weightFraction(xrt.getElement(), false);
      // From Eqn 5, k*cStd*stdF/unkF = cUnk*unkZA/stdZA
      return cUnk * (unkZA / stdZA) * Math.sqrt(sumSqr);
   }

   public static double uC(Composition std, Composition unk, Element elm) {
      return unk.weightFraction(elm, false) * std.weightFractionU(elm, false).fractionalUncertainty();
   }

   /**
    * Computes the uncertainty associated with the mass fraction 'unk' as
    * measured using the specified standard 'std', x-ray transition 'xrt' and
    * SpectrumProperties 'props'.
    * 
    * @param std
    *           Composition of the standard
    * @param unk
    *           Composition of the unknown
    * @param xrt
    *           The x-ray transition
    * @param props
    *           The properties describing the conditions under which the
    *           spectrum was collected.
    * @return {@link UncertainValue2}
    * @throws EPQException
    */
   public static UncertainValue2 massFraction(Composition std, Composition unk, XRayTransition xrt, SpectrumProperties props) throws EPQException {
      final XPP1991 unkXpp = new XPP1991();
      unkXpp.initialize(unk, xrt.getDestination(), props);
      final XPP1991 stdXpp = new XPP1991();
      stdXpp.initialize(std, xrt.getDestination(), props);
      final UncertainValue2 res = new UncertainValue2(unk.weightFraction(xrt.getElement(), false));
      res.assignComponent(UNCERTAINTY_MAC, uMAC(stdXpp, unkXpp, xrt));
      res.assignComponent(UNCERTAINTY_ETA, uEta(stdXpp, unkXpp, xrt));
      res.assignComponent(UNCERTAINTY_COMP, uC(std, unk, xrt.getElement()));
      return res;
   }

   @Override
   public UncertainValue2 kratio(Composition std, Composition unk, XRayTransition xrt, SpectrumProperties props) throws EPQException {
      final XPP1991 unkXpp = new XPP1991();
      unkXpp.initialize(unk.normalize(), xrt.getDestination(), props);
      final XPP1991 stdXpp = new XPP1991();
      stdXpp.initialize(std.normalize(), xrt.getDestination(), props);
      final UncertainValue2 cUnk = new UncertainValue2(unk.weightFraction(xrt.getElement(), false));
      cUnk.assignComponent(UNCERTAINTY_MAC, uMAC(stdXpp, unkXpp, xrt));
      cUnk.assignComponent(UNCERTAINTY_ETA, uEta(stdXpp, unkXpp, xrt));
      cUnk.assignComponent(UNCERTAINTY_COMP, uC(std, unk, xrt.getElement()));
      // assert cUnk.equals(massFraction(std, unk, xrt, props), 1.0e-6);
      assert UncertainValue2.subtract(cUnk, massFraction(std, unk, xrt, props)).abs().doubleValue() < 1.0e-6;
      final double corr = unkXpp.computeZAFCorrection(xrt) / (stdXpp.computeZAFCorrection(xrt) * std.weightFraction(xrt.getElement(), false));
      return UncertainValue2.multiply(corr, cUnk);
   }

   public UncertainValue2 kratio(Composition std, Composition unk, XRayTransitionSet xrts, SpectrumProperties props) throws EPQException {
      UncertainValue2 res = UncertainValue2.ZERO;
      final XPP1991 stdXpp = new XPP1991();
      final XPP1991 unkXpp = new XPP1991();
      double wSum = 0.0;
      for (final XRayTransition xrt : xrts) {
         unkXpp.initialize(unk.normalize(), xrt.getDestination(), props);
         stdXpp.initialize(std.normalize(), xrt.getDestination(), props);
         final UncertainValue2 mf = new UncertainValue2(unk.weightFraction(xrt.getElement(), false));
         mf.assignComponent(UNCERTAINTY_MAC, uMAC(stdXpp, unkXpp, xrt));
         mf.assignComponent(UNCERTAINTY_ETA, uEta(stdXpp, unkXpp, xrt));
         mf.assignComponent(UNCERTAINTY_COMP, uC(std, unk, xrt.getElement()));
         assert UncertainValue2.subtract(mf, massFraction(std, unk, xrt, props)).abs().doubleValue() < 1.0e-6;
         final double corr = unkXpp.computeZAFCorrection(xrt) / (stdXpp.computeZAFCorrection(xrt) * std.weightFraction(xrt.getElement(), false));
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         wSum += w;
         res = UncertainValue2.add(res, UncertainValue2.multiply(w * corr, mf));
      }
      return UncertainValue2.divide(res, wSum);
   }

}

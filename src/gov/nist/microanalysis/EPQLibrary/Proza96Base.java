package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.FindRoot;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Collection;

/**
 * <p>
 * Implements the Proza96 phi(rho-z) correction scheme.
 * </p>
 * <p>
 * Ref: Bastin GF, Dijkstra JM and Heijligers HJM, X-Ray Spectrometry, Vol 27,
 * 3-10 (1998)
 * </p>
 * <p>
 * Note: Energies in keV and lengths in cm are used extensively in internals and
 * the private methods associated with this class. The public methods all follow
 * the library standard of using SI units.
 * </p>
 * <p>
 * Description:
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
public class Proza96Base
   extends CorrectionAlgorithm.PhiRhoZAlgorithm {

   // Dependent parameters (computed in the constructor from the input
   // parameters)
   protected double mPhiZero; // Surface ionization
   protected double mPhiMax; // The height of the phi(rho z) curve
   protected double mRhoZMax; // The rho-z position of the peak of the
   // phi(rho-z) curve (in cm)
   protected double mAlpha; // The width of the inner branch
   protected double mBeta; // The width of the surface branch

   /**
    * Constructs an object implementing the Proza96 correction algorithm
    */
   public Proza96Base() {
      super("Proza96 - Bastin et al.", LitReference.Proza96);
   }

   /**
    * Constructs an object implementing the Proza96 correction algorithm for
    * extending
    * 
    * @param name
    * @param ref
    */
   protected Proza96Base(String name, LitReference ref) {
      super(name, ref);
   }

   /**
    * caveat - Determine any caveats that have been identified for this
    * algorithm.
    * 
    * @param comp Composition
    * @param shell AtomicShell
    * @param props SpectrumProperties
    * @return String
    */
   @Override
   public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
      String res = CaveatBase.None;
      double e0;
      try {
         e0 = ToSI.keV(props.getNumericProperty(SpectrumProperties.BeamEnergy));
         if((e0 / shell.getEdgeEnergy()) < 1.3)
            res = "This algorithm has problems with overvoltages of less than about 1.3.";
         final BackscatterFactor bf = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
         res = CaveatBase.append(res, bf.caveat(comp, shell, e0));
         final Collection<XRayTransition> col = XRayTransition.createByDestinationShell(shell);
         final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
         for(final XRayTransition xrt : col)
            try {
               res = CaveatBase.append(res, mac.caveat(comp, xrt.getEnergy()));
            }
            catch(final EPQException ex) {
               res = CaveatBase.append(res, ex.toString());
            }
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         for(final Element elm : comp.getElementSet())
            res = CaveatBase.append(res, mip.caveat(elm));
      }
      catch(final EPQException epq) {
         res = CaveatBase.append(res, epq.getMessage());
      }
      return CaveatBase.format(this, res);
   }

   @Override
   protected void initializeDefaultStrategy() {
      super.initializeDefaultStrategy();
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Pouchou1991);
      addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Sternheimer64);
      addDefaultAlgorithm(SurfaceIonization.class, SurfaceIonization.Bastin1998);
      addDefaultAlgorithm(BackscatterFactor.class, BackscatterFactor.Pouchou1991);
      addDefaultAlgorithm(ElectronRange.class, ElectronRange.Pouchou1991);
      addDefaultAlgorithm(StoppingPower.class, StoppingPower.Proza96);
      addDefaultAlgorithm(ProportionalIonizationCrossSection.class, ProportionalIonizationCrossSection.Proza96);
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props)
         throws EPQException {
      final boolean res = super.initialize(comp, shell, props);
      if(res) {
         // Compute items that expect inputs in SI units
         final SurfaceIonization si = (SurfaceIonization) getAlgorithm(SurfaceIonization.class);
         final BackscatterFactor bf = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
         final ElectronRange er = (ElectronRange) getAlgorithm(ElectronRange.class);
         final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
         final ProportionalIonizationCrossSection icx = (ProportionalIonizationCrossSection) getAlgorithm(ProportionalIonizationCrossSection.class);
         mPhiZero = si.compute(mComposition, mShell, mBeamEnergy);
         // Convert input units into keV for internal use only...
         final double e0keV = FromSI.keV(mBeamEnergy);
         final double eC = FromSI.keV(mShell.getEdgeEnergy());
         final double u0 = e0keV / eC;
         if(u0 <= 1.0)
            throw new IllegalArgumentException("The edge energy exceeds the beam energy.");
         { // Compute the location of the peak of the phi(rho z) curve
           // (mRhoZMax). This is Proza96 specific...
            final double meanZ = mComposition.weightAvgAtomicNumber();
            final double a = 2.1040483 + ((((0.044934014 - (5.518453e-4 * meanZ)) * Math.sqrt(meanZ)) + (2.46257718e-5 * meanZ * meanZ)) * meanZ);
            final double b = Math.exp(2.6219621 - (2.5091694 / Math.sqrt(meanZ)) - (4.6725352 / meanZ));
            final double c = Math2.sqr((3.2561755 + ((-0.060134019 + (1.2310844e-3 * meanZ)) * meanZ))
                  / (1.0 + ((-1.7374036e-2 + (2.524835e-4 * meanZ)) * meanZ)));
            // mRhoZMax in g/(cm^2) [tests against Al]
            mRhoZMax = ElectronRange.toGramPerCmSqr(er.compute(comp, shell, mBeamEnergy))
                  / (a + (b / Math.sqrt(u0)) + (c / (u0 * Math.sqrt(u0))));
         }
         mAlpha = alpha(mComposition, mShell, e0keV);
         // mF = (R/S)*(1/Qj(E0))
         mFi = (bf.compute(mComposition, mShell, mBeamEnergy) * StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(comp, shell, mBeamEnergy)))
               / icx.computeFamily(shell, mBeamEnergy);
         // Compute the dependent parameter mBeta
         if(mFi > (mPhiZero * (mRhoZMax + (Math2.SQRT_PI / (2.0 * mAlpha))))) {
            // This is the normal case. fi is larger than the minimum
            // acheivable by modulating mBeta.
            // Find the mBeta such that fi equals the integral.
            final FindRoot rt = new FindRoot() {
               private double mPhi0;
               private double mFI;

               @Override
               public void initialize(double[] params) {
                  mPhi0 = params[0];
                  mFI = params[1];
               }

               @Override
               public double function(double beta) {
                  final double t = Math.exp(Math2.sqr(beta * mRhoZMax));
                  final double p5 = p5(beta * mRhoZMax);
                  return ((t - p5) / beta) + (t / mAlpha) + ((-2.0 * mFI) / (Math2.SQRT_PI * mPhi0));
               }
            };
            assert mFi > (mPhiZero * (mRhoZMax + (Math2.SQRT_PI / (2.0 * mAlpha)))) : "The minimum area is too large.";
            rt.initialize(new double[] {
               mPhiZero,
               mFi
            });
            double x0 = 0.2 * mAlpha;
            if(rt.function(x0) > 0)
               x0 = 1.0e-3 * mAlpha;
            if(rt.function(x0) < 0.0) {
               assert (rt.function(x0) < 0.0);
               double x1 = 2.0 * mAlpha;
               if(rt.function(x1) < 0.0)
                  x1 = 10.0 * mAlpha;
               assert (rt.function(x1) > 0.0);
               try {
                  mBeta = rt.perform(x0, x1, 1.0e-5, 20);
               }
               catch(final Exception ex) {
                  mBeta = rt.bestX();
               }
            } else
               mBeta = 1.0e-3 * mAlpha;
         } else {
            // A pathalogical case in which no choice of mBeta will produce
            // an integral equal to fi. Instead set mBeta=0.0 and solve for
            // mAlpha such that the integral is fi. I've chosen to modify
            // mAlpha rather than phiZero or mRhoZMax since mAlpha effects
            // the deepest part of the phi(rho z) curve and thus is likely to
            // have the smallest erroneous effect on the result.
            assert mFi > (mRhoZMax * mPhiZero) : "fi is too small.";
            mBeta = 0.0;
            mAlpha = (0.5 * (mPhiZero * Math2.SQRT_PI)) / (mFi - (mRhoZMax * mPhiZero));
            // phiZero=(2.0*fi*mAlpha)/(2.0*mAlpha*mRhoZMax + SQRT_PI);
            // mRhoZMax=(2.0*fi*mAlpha -
            // SQRT_PI*phiZero)/(2.0*mAlpha*phiZero);
            assert (Math.abs(mFi - (mPhiZero * (mRhoZMax + (Math2.SQRT_PI / (2.0 * mAlpha))))) < 1e-10);
         }
         // Compute the maximum height of the phi(rho*z) curve
         mPhiMax = mPhiZero * Math.exp(Math2.sqr(mBeta * mRhoZMax));
      }
      return res;
   }

   /**
    * alpha - Computes the alpha parameter for atom and shell specified by the
    * AtomicShell and a beam energy specified by e0.
    * 
    * @param matEl Element - The matrix material element
    * @param shell AtomicShell - The shell producing the x-rays
    * @param e0 double - The beam energy (Note: in keV!)
    * @return double
    */
   private double alpha(Element matEl, AtomicShell shell, double e0) {
      final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
      final double eC = FromSI.keV(shell.getEdgeEnergy());
      final double z = matEl.getAtomicNumber();
      final double j = FromSI.keV(mip.compute(matEl));
      return (Math.exp(12.93774 - (0.003426515 * z)) / Math.pow(e0, 1.0 / (1.139231 + (0.002775625 * z))))
            * Math.sqrt(((z / matEl.getAtomicWeight()) * Math.log(1.166 * (e0 / j))) / ((e0 * e0) - (eC * eC)));
   }

   /**
    * alpha - Compute alpha for the specified transition family in the specified
    * matrix material.
    * 
    * @param comp Composition
    * @param shell AtomicShell
    * @param e0 double - Note: in keV!!
    * @return double
    */
   private double alpha(Composition comp, AtomicShell shell, double e0) {
      double num = 0.0, den = 0.0;
      for(final Element el : comp.getElementSet()) {
         final double k = (comp.weightFraction(el, true) * el.getAtomicNumber()) / el.getAtomicWeight();
         num += k / alpha(el, shell, e0);
         den += k;
      }
      return den / num;
   }

   /**
    * p5 - The polynomial used in Bastin's approximation to the error function.
    * 
    * @param x double
    * @return double
    */
   private double p5(double x) {
      final double t = 1.0 / (1.0 + (0.3275911 * x));
      return ((((((((1.061405429 * t) - 1.453152027) * t) + 1.421413741) * t) - 0.284496736) * t) + 0.254829592) * t;
   }

   /**
    * erf - Bastin's approximation to the error function
    * 
    * @param x double
    * @return double
    */
   protected double erf(double x) {
      if(x == 0)
         return 0.0;
      boolean pos = true;
      if(x < 0) {
         x = -x;
         pos = false;
      }
      final double res = (1.0 - (p5(x) * Math.exp(-x * x)));
      // assert(Math.abs(res-Math2.erf(x))<1.0e-6);
      return pos ? res : -res;
   }

   @Override
   public double generated(XRayTransition xrt) {
      assert (mComposition != null);
      double d;
      if(mBeta != 0)
         d = erf(mBeta * mRhoZMax) / mBeta;
      else
         // Special case for mBeta=0
         d = (2 * mRhoZMax) / Math2.SQRT_PI;
      return toSI(0.5 * mPhiMax * Math2.SQRT_PI * (d + (1.0 / mAlpha)));
   }

   @Override
   public double computeZACorrection(XRayTransition xrt)
         throws EPQException {
      assert (mComposition != null);
      assert Math.abs(mTakeOffAngle - mExitAngle) < Math.toRadians(1.0) : "The take-off and exit angles should agree to within 1\u00B0";
      final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
      double emitted;
      if(mBeta != 0.0) {
         final double chi_2b = (0.5 * chi) / mBeta;
         final double chi_2a = (0.5 * chi) / mAlpha;
         final double lhs = (Math.exp((chi_2b * chi_2b) - (chi * mRhoZMax)) * (erf((mBeta * mRhoZMax) - chi_2b) + erf(chi_2b)))
               / mBeta;
         final double rhs = (Math.exp((chi_2a * chi_2a) - (chi * mRhoZMax)) * (1.0 - erf(chi_2a))) / mAlpha;
         emitted = 0.5 * mPhiMax * Math2.SQRT_PI * (lhs + rhs);
      } else
         // Special case for beta=0
         emitted = ((Math.exp(mRhoZMax * chi) - 1.0) * mPhiMax * Math.exp(mRhoZMax * chi)) / chi;
      // if there is really strong absorption emitted can be NaN
      return Double.isNaN(emitted) ? 0.0 : toSI(emitted);
   }

   /**
    * Computes the uncertainty in the result computeZACorrection(...) due to
    * imprecise knowledge of the mass absorption correction.
    * 
    * @param xrt
    * @return double
    * @throws EPQException
    */
   public double computeAbsorptionUncertainty(XRayTransition xrt)
         throws EPQException {
      assert (mComposition != null);
      final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
      final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
      final double dChi = chi * mac.fractionalUncertainty(mComposition, xrt);
      double dEmitted;
      if(mBeta != 0.0)
         dEmitted = (mPhiMax * Math2.SQRT_PI * ((-(1 / (Math2.sqr(mAlpha) * Math2.SQRT_PI))
               - ((-1 + Math.exp(mRhoZMax * (chi - (Math2.sqr(mBeta) * mRhoZMax)))) / (Math2.sqr(mBeta) * Math2.SQRT_PI)) - ((0.5
               * Math.exp(Math2.sqr(chi) / (4. * Math2.sqr(mAlpha))) * (-chi + (2.0 * Math2.sqr(mAlpha) * mRhoZMax)) * (1.0 - Math2.erf((0.5 * chi)
               / mAlpha))) / Math.pow(mAlpha, 3.0))) + ((Math.exp(Math2.sqr(chi) / (4.0 * Math2.sqr(mBeta)))
               * (chi - (2.0 * Math2.sqr(mBeta) * mRhoZMax)) * (Math2.erf(chi / (2.0 * mBeta)) - Math2.erf((chi / (2.0 * mBeta))
               - (mBeta * mRhoZMax)))) / (2.0 * Math.pow(mBeta, 3.0)))))
               / (2.0 * Math.exp(chi * mRhoZMax));
      else
         // Special case for beta=0
         dEmitted = (Math.exp(chi * mRhoZMax) * mPhiMax * ((1.0 - (chi * mRhoZMax)) + (Math.exp(chi * mRhoZMax) * (-1.0 + (2.0 * chi * mRhoZMax)))))
               / Math2.sqr(chi);
      // if there is really strong absorption emitted can be NaN
      return Double.isNaN(dEmitted) ? 0.0 : Math.abs(toSI(dEmitted * dChi));
   }

   static public final Proza96Base Proza96Base = new Proza96Base();
   protected double mFi;

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm.PhiRhoZAlgorithm#computeCurve(double)
    */
   @Override
   public double computeCurve(double rhoZ) {
      if(rhoZ > 0.0) {
         rhoZ /= 10.0; // convert from kg/m^2 to g/cm^2
         return rhoZ < mRhoZMax ? mPhiMax * Math.exp(-Math2.sqr(mBeta * (rhoZ - mRhoZMax))) : mPhiMax
               * Math.exp(-Math2.sqr(mAlpha * (rhoZ - mRhoZMax)));
      } else
         return 0.0;

   }
}

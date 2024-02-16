package gov.nist.microanalysis.EPQLibrary.Detector;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Objects;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * Description
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class SiLiCalibration extends EDSCalibration {

   protected static final double MIN_OVERVOLTAGE = 1.1;
   private transient double[] mEfficiency;
   protected final double mFWHM;

   private int mFirstLElement = Element.elmCa;
   private int mFirstKElement = Element.elmB;
   private int mFirstMElement = Element.elmLa;
   private int mFirstNElement = Element.elmAm;

   private static Material createMaterial(Element elm) {
      try {
         return MaterialFactory.createPureElement(elm);
      } catch (final EPQException e) {
         e.printStackTrace();
      }
      return null;
   }

   static String formatFWHM(double fwhm) {
      final NumberFormat df = new HalfUpFormat("0.0 eV");
      return df.format(fwhm);
   }

   static String formatFano(double fano, double noise) {
      final NumberFormat df1 = new HalfUpFormat("0.0000");
      final NumberFormat df2 = new HalfUpFormat("0.00");
      return "fano=" + df1.format(fano) + ", noise=" + df2.format(noise) + " eV";
   }

   /**
    * Constructs an object representing the calibration of a standard Si(Li)
    * detector.
    */
   public SiLiCalibration(double scale, double offset, double fwhm) {
      super(formatFWHM(fwhm), scale, offset, new BasicSiLiLineshape(fwhm));
      mFWHM = fwhm;
   }

   /**
    * Constructs an object representing the calibration of a standard Si(Li)
    * detector.
    */
   public SiLiCalibration(double scale, double offset, double quad, double fano, double noise) {
      super(formatFano(fano, noise), scale, offset, quad, new FanoSiLiLineshape(fano, noise));
      mFWHM = getLineshape().getFWHMatMnKa();
   }

   protected SiLiCalibration(SiLiCalibration model) {
      super(model.toString(), model.getChannelWidth(), model.getZeroOffset(), model.getLineshape().clone());
      mFWHM = getLineshape().getFWHMatMnKa();
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#getEfficiency(gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties)
    */
   @Override
   public double[] getEfficiency(DetectorProperties dp) {
      if (mEfficiency == null) {
         mEfficiency = new double[dp.getChannelCount()];
         final SpectrumProperties sp = dp.getProperties();
         final double goldLayer = sp.getNumericWithDefault(SpectrumProperties.GoldLayer, 0.0) * 1.0e-9;
         final double nickelLayer = sp.getNumericWithDefault(SpectrumProperties.NickelLayer, 0.0) * 1.0e-9;
         final double deadLayer = sp.getNumericWithDefault(SpectrumProperties.DeadLayer, 0.0) * 1.0e-6;
         final double alLayer = sp.getNumericWithDefault(SpectrumProperties.AluminumLayer, 0.0) * 1.0e-9;
         final double thickness = sp.getNumericWithDefault(SpectrumProperties.DetectorThickness, 5.0) * 1.0e-3;
         final double area = sp.getNumericWithDefault(SpectrumProperties.DetectorArea, 10.0) * 1.0e-6;

         final Material gold = createMaterial(Element.Au);
         final Material si = createMaterial(Element.Si);
         final Material al = createMaterial(Element.Al);
         final Material ni = createMaterial(Element.Ni);
         final MassAbsorptionCoefficient mac = AlgorithmUser.getDefaultMAC();
         final IXRayWindowProperties wind = dp.getWindow();

         final double scale = getChannelWidth();
         final double offset = getZeroOffset();

         final double niMassThickness = ni.getDensity() * nickelLayer;
         final double goldMassThickness = gold.getDensity() * goldLayer;
         final double alMassThickness = al.getDensity() * alLayer;
         final double deadMassThickness = si.getDensity() * deadLayer;
         final double detMassThickness = si.getDensity() * thickness;
         final double oo4pi = 1.0 / (4.0 * Math.PI);
         for (int i = 0; i < mEfficiency.length; ++i) {
            final double e = ToSI.eV((scale * (i + 0.5)) + offset);
            if (e > ToSI.eV(20.0)) {
               mEfficiency[i] = (wind != null ? wind.transmission(e) : 1.0) * (mContamination != null ? mContamination.transmission(e) : 1.0) * area
                     * Math.exp(-mac.compute(ni, e) * niMassThickness) * Math.exp(-mac.compute(gold, e) * goldMassThickness)
                     * Math.exp(-mac.compute(al, e) * alMassThickness) * Math.exp(-mac.compute(si, e) * deadMassThickness)
                     * (1.0 - Math.exp(-mac.compute(si, e) * detMassThickness)) * oo4pi;
            }
         }
      }
      return mEfficiency;
   }

   public Element getFirstVisible(int family) {
      Element res = null;
      switch (family) {
         case AtomicShell.KFamily :
            res = Element.byAtomicNumber(mFirstKElement);
            if (res.equals(Element.None))
               res = Element.B;
            break;
         case AtomicShell.LFamily :
            res = Element.byAtomicNumber(mFirstLElement);
            if (res.equals(Element.None))
               res = Element.Sc;
            break;
         case AtomicShell.MFamily :
            res = Element.byAtomicNumber(mFirstMElement);
            if (res.equals(Element.None))
               res = Element.La;
            break;
         default :
            res = Element.Uub;
      }
      return res;

   }

   public void setFirstVisible(int family, Element elm) {
      switch (family) {
         case AtomicShell.KFamily :
            mFirstKElement = elm.getAtomicNumber();
            break;
         case AtomicShell.LFamily :
            mFirstLElement = elm.getAtomicNumber();
            break;
         case AtomicShell.MFamily :
            mFirstMElement = elm.getAtomicNumber();
            break;
         case AtomicShell.NFamily :
            mFirstNElement = elm.getAtomicNumber();
            break;
         default :
            assert false;
      }
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#isVisible(gov.nist.microanalysis.EPQLibrary.XRayTransition,
    *      double)
    */
   @Override
   public boolean isVisible(XRayTransition xrt, double eBeam) {
      switch (xrt.getFamily()) {
         case AtomicShell.KFamily :
            return (xrt.getElement().getAtomicNumber() >= mFirstKElement) && (xrt.getEdgeEnergy() < (eBeam / MIN_OVERVOLTAGE));
         case AtomicShell.LFamily :
            return (xrt.getElement().getAtomicNumber() >= mFirstLElement) && (xrt.getEdgeEnergy() < (eBeam / MIN_OVERVOLTAGE));
         case AtomicShell.MFamily :
            return (xrt.getElement().getAtomicNumber() >= mFirstMElement) && (xrt.getEdgeEnergy() < (eBeam / MIN_OVERVOLTAGE));
         case AtomicShell.NFamily :
            return (xrt.getElement().getAtomicNumber() >= mFirstNElement) && (xrt.getEdgeEnergy() < (eBeam / MIN_OVERVOLTAGE));
         default :
            return false;
      }
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration#clone()
    */
   @Override
   public EDSCalibration clone() {
      return new SiLiCalibration(this);
   }

   private Object readResolve() {
      mEfficiency = null;
      mHash = Integer.MAX_VALUE;
      // THese were selected as the first element that has shells which can
      // contribute to these families.
      if (mFirstKElement < Element.elmLi)
         mFirstKElement = Element.elmB;
      if (mFirstLElement < Element.elmAl)
         mFirstLElement = Element.elmSc;
      if (mFirstMElement < Element.elmSr)
         mFirstMElement = Element.elmLa;
      if (mFirstNElement < Element.elmU)
         mFirstNElement = Element.elmU;
      return this;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      if (mHash == Integer.MAX_VALUE)
         mHash = Objects.hash(super.hashCode(), mEfficiency, mFirstKElement, mFirstLElement, mFirstMElement, mFWHM);
      // System.out.println("SiLi.hashCode()=" + Integer.toString(mHash));
      return mHash;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (getClass() != obj.getClass())
         return false;
      if (!super.equals(obj))
         return false;
      final SiLiCalibration other = (SiLiCalibration) obj;
      return Arrays.equals(mEfficiency, other.mEfficiency) && Objects.equals(mFirstKElement, other.mFirstKElement)
            && Objects.equals(mFirstLElement, other.mFirstLElement) && Objects.equals(mFirstMElement, other.mFirstMElement)
            && Objects.equals(mFWHM, other.mFWHM);
   }

   @Override
   public String toString() {
      final NumberFormat df = new HalfUpFormat("0.0 eV");
      return "FWHM[Mn K\u03B1]=" + df.format(mFWHM) + " - " + super.toString();
   }
}

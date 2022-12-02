/**
 * gov.nist.microanalysis.EPQLibrary.Detector.SDDCalibration Created by:
 * nritchie Date: Oct 30, 2018
 */
package gov.nist.microanalysis.EPQLibrary.Detector;

import java.util.Objects;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;

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
public class SDDCalibration extends SiLiCalibration {

   private int mFirstNElement = Element.elmAm;

   public SDDCalibration(SiLiCalibration sili) {
      super(sili);
      mFirstNElement = Element.elmAm;
   }

   public SDDCalibration(double scale, double offset, double quad, double fano, double noise) {
      super(scale, offset, quad, fano, noise);
   }

   public SDDCalibration(double scale, double offset, double fwhm) {
      super(scale, offset, fwhm);
   }

   @Override
   public Element getFirstVisible(int family) {
      Element res = null;
      switch (family) {
         case AtomicShell.KFamily :
         case AtomicShell.LFamily :
         case AtomicShell.MFamily :
            return super.getFirstVisible(family);
         case AtomicShell.NFamily :
            res = Element.byAtomicNumber(mFirstNElement);
            if (res.equals(Element.None))
               res = Element.Am;
            break;
         default :
            res = Element.Uub;
      }
      return res;

   }

   @Override
   public void setFirstVisible(int family, Element elm) {
      switch (family) {
         case AtomicShell.KFamily :
         case AtomicShell.LFamily :
         case AtomicShell.MFamily :
            super.setFirstVisible(family, elm);
         case AtomicShell.NFamily :
            mFirstNElement = elm.getAtomicNumber();
            break;
         default :
            assert false;
      }
   }

   @Override
   public boolean isVisible(XRayTransition xrt, double eBeam) {
      switch (xrt.getFamily()) {
         case AtomicShell.KFamily :
         case AtomicShell.LFamily :
         case AtomicShell.MFamily :
            return super.isVisible(xrt, eBeam);
         case AtomicShell.NFamily :
            return (xrt.getElement().getAtomicNumber() >= mFirstNElement) && (xrt.getEdgeEnergy() < (eBeam / MIN_OVERVOLTAGE));
         default :
            return false;
      }
   }

   private Object readResolve() {
      if (mFirstNElement < Element.elmW)
         mFirstNElement = Element.elmW;
      return this;
   }

   @Override
   public int hashCode() {
      if (mHash == Integer.MAX_VALUE)
         mHash = Objects.hash(super.hashCode(), mFirstNElement);
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
      final SDDCalibration other = (SDDCalibration) obj;
      return super.equals(obj) && Objects.equals(mFirstNElement, other.mFirstNElement);
   }

}

package gov.nist.microanalysis.EPQLibrary.Detector;

import java.util.ArrayList;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;

/**
 * <p>
 * A class for modeling the transmission properties of an x-ray window.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */
public class XRayWindow implements IXRayWindowProperties {

   transient int mHash = Integer.MAX_VALUE;

   private SpectrumProperties mProperties;

   private static Material createMaterial(String comp, double density) {
      try {
         return MaterialFactory.createCompound(comp, density);
      } catch (final EPQException e) {
         throw new EPQFatalException(e);
      }
   }

   final static Material PARYLENE = createMaterial("C10H8O4N", ToSI.gPerCC(1.39));
   final static Material DEFAULT_MOXTEK = createMaterial("C10H8O4N", ToSI.gPerCC(1.39));
   final static Material BORON_NITRIDE = createMaterial("BN", ToSI.gPerCC(2.18));
   final static Material DIAMOND = createMaterial("C", ToSI.gPerCC(3.5));
   final static Material MYLAR = createMaterial("C10H8O4", ToSI.gPerCC(1.37));
   final static Material SILICON_NITRIDE = createMaterial("Si3N4", ToSI.gPerCC(3.44));
   final static Material ICE = createMaterial("H2O", ToSI.gPerCC(0.917));
   final static Material AL = createMaterial("Al", ToSI.gPerCC(2.7));
   final static Material SI = createMaterial("Si", ToSI.gPerCC(2.33));

   private class Layer {
      transient int mHash = Integer.MAX_VALUE;
      private final double mThickness; // in meters
      private final Material mMaterial;

      private Layer(Material mat, double thickness) {
         mMaterial = mat;
         mThickness = thickness;
      }

      private Object readResolve() {
         mHash = Integer.MAX_VALUE;
         return this;
      }

      private double massAbsorptionCoefficient(double energy) {
         return AlgorithmUser.getDefaultMAC().compute(mMaterial, energy) * mMaterial.getDensity() * mThickness;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         if (mHash == Integer.MAX_VALUE) {
            // Make sure it does not change...
            final int PRIME = 31;
            int result = 1;
            result = (PRIME * result) + ((mMaterial == null) ? 0 : mMaterial.hashCode());
            long temp;
            temp = Double.doubleToLongBits(mThickness);
            result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
            if (result == Integer.MAX_VALUE)
               result = Integer.MIN_VALUE;
            mHash = result;
         }
         return mHash;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         final Layer other = (Layer) obj;
         if (mMaterial == null) {
            if (other.mMaterial != null)
               return false;
         } else if (!mMaterial.equals(other.mMaterial))
            return false;
         if (Double.doubleToLongBits(mThickness) != Double.doubleToLongBits(other.mThickness))
            return false;
         return true;
      }
   }

   List<Layer> mLayers = new ArrayList<Layer>(); // A list of Layer objects
   /**
    * What fraction of the window is not blocked?
    */
   protected double mOpenFraction = 1.0;

   /**
    * A factor that accounts for differences in low energy efficiency
    */
   protected double mLowEnergyCorrection = 1.0;

   /**
    * What fraction of the detector is occluded by this window. The remainder is
    * assumed to pass 100% of the incident x-rays.
    */
   private double mCoverage = 1.0;
   private String mName;

   /**
    * Creates an x-ray window with the specified open area fraction. The
    * fraction that is not open is assumed to be blocked 100%.
    * 
    * @param openAreaFrac
    *           double - The open area fraction (0,1.0]
    */
   public XRayWindow(double openAreaFrac) {
      assert openAreaFrac > 0.0;
      assert openAreaFrac <= 1.0;
      mOpenFraction = openAreaFrac;
      mCoverage = 1.0;
      mProperties = new SpectrumProperties();
      mProperties.setNumericProperty(SpectrumProperties.WindowOpenArea, openAreaFrac * 100.0);
   }

   private Object readResolve() {
      mHash = Integer.MAX_VALUE;
      return this;
   }

   private void updateProperty(SpectrumProperties.PropertyId pid, double increment) {
      final double old = mProperties.getNumericWithDefault(pid, 0.0);
      mProperties.setNumericProperty(pid, old + increment);
   }

   /**
    * addLayer - Add a new material layer to the window.
    * 
    * @param mat
    *           Material
    * @param thickness
    *           double
    */
   public void addLayer(Material mat, double thickness) {
      assert (mat.getDensity() > 0.0);
      mLayers.add(new Layer(mat, thickness));
      if (mat.weightFraction(Element.Be, true) == 1.0)
         updateProperty(SpectrumProperties.BerylliumWindow, FromSI.micrometer(thickness));
      if (mat.weightFraction(Element.Al, true) == 1.0)
         updateProperty(SpectrumProperties.AluminumWindow, FromSI.nanometer(thickness));
      else if (mat.weightFraction(Element.Au, true) == 1.0)
         updateProperty(SpectrumProperties.GoldLayer, FromSI.nanometer(thickness));
      else if (mat.weightFraction(Element.Ni, true) == 1.0)
         updateProperty(SpectrumProperties.NickelLayer, FromSI.nanometer(thickness));
      else if (mat == DEFAULT_MOXTEK)
         updateProperty(SpectrumProperties.MoxtekWindow, FromSI.micrometer(thickness));
      else if (mat.equals(PARYLENE))
         updateProperty(SpectrumProperties.ParaleneWindow, FromSI.micrometer(thickness));
      else if (mat.equals(BORON_NITRIDE))
         updateProperty(SpectrumProperties.BoronNitrideWindow, FromSI.micrometer(thickness));
      else if (mat.equals(DIAMOND))
         updateProperty(SpectrumProperties.DiamondWindow, FromSI.micrometer(thickness));
      else if (mat.equals(MYLAR))
         updateProperty(SpectrumProperties.MylarWindow, FromSI.micrometer(thickness));
      else if (mat.equals(PARYLENE))
         updateProperty(SpectrumProperties.PyroleneWindow, FromSI.micrometer(thickness));
      else if (mat.equals(SILICON_NITRIDE))
         updateProperty(SpectrumProperties.SiliconNitrideWindow, FromSI.micrometer(thickness));
      else if (mat.equals(ICE))
         updateProperty(SpectrumProperties.IceThickness, FromSI.micrometer(thickness));
   }

   /**
    * addIce - Add a layer of ice of the specified thickness to the window.
    * 
    * @param thickness
    *           double
    */
   public void addIce(double thickness) throws EPQException {
      addLayer(ICE, thickness);
   }

   /**
    * massAbsorptionCoefficient - Gets the mass absorption coefficient for the
    * full window at the specified energy.
    * 
    * @param energy
    *           double - In Joules
    * @return double
    */
   private double massAbsorptionCoefficient(double energy) {
      double mac = 0.0;
      for (final Layer l : mLayers)
         mac += l.massAbsorptionCoefficient(energy);
      return mac;
   }

   /**
    * transmission - Computest the fraction of incident photons of the specified
    * energy that will be transmitted through the window.
    * 
    * @param energy
    *           double - In Joules
    * @return double
    */
   @Override
   public double transmission(double energy) {
      return mOpenFraction * ((1.0 - mCoverage) + (mCoverage * Math.exp(-massAbsorptionCoefficient(energy))));
   }

   /**
    * absorption - Computest the fraction of incident photons of the specified
    * energy that will be absorbed by the window.
    * 
    * @param energy
    *           double - In Joules
    * @return double
    */
   public final double absorption(double energy) {
      return 1.0 - transmission(energy);
   }

   /**
    * Gets the current value assigned to name
    * 
    * @return Returns the name.
    */
   @Override
   public String getName() {
      return mName;
   }

   /**
    * Sets the value assigned to name.
    * 
    * @param name
    *           The value to which to set name.
    */
   @Override
   public void setName(String name) {
      mName = name;
   }

   @Override
   public String toString() {
      return mName;
   }

   @Override
   public int hashCode() {
      if (mHash == Integer.MAX_VALUE) {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mCoverage);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         result = (prime * result) + ((mLayers == null) ? 0 : mLayers.hashCode());
         result = (prime * result) + ((mName == null) ? 0 : mName.hashCode());
         temp = Double.doubleToLongBits(mOpenFraction);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         result = (prime * result) + ((mProperties == null) ? 0 : mProperties.hashCode());
         if (result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHash = result;
      }
      return mHash;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final XRayWindow other = (XRayWindow) obj;
      if (Double.doubleToLongBits(mCoverage) != Double.doubleToLongBits(other.mCoverage))
         return false;
      if (mLayers == null) {
         if (other.mLayers != null)
            return false;
      } else if (!mLayers.equals(other.mLayers))
         return false;
      if (mName == null) {
         if (other.mName != null)
            return false;
      } else if (!mName.equals(other.mName))
         return false;
      if (Double.doubleToLongBits(mOpenFraction) != Double.doubleToLongBits(other.mOpenFraction))
         return false;
      if (mProperties == null) {
         if (other.mProperties != null)
            return false;
      } else if (!mProperties.equals(other.mProperties))
         return false;
      return true;
   }

   @Override
   public XRayWindow clone() {
      final XRayWindow xrw = new XRayWindow(mOpenFraction);
      xrw.mCoverage = mCoverage;
      xrw.mLayers.addAll(mLayers);
      xrw.mName = mName;
      xrw.mProperties = mProperties.clone();
      return xrw;
   }

   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }
}

package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;

/**
 * <p>
 * Creates a block of material with randomly distributed pores.
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

public class PorousBlock {

   private Material mBulkMaterial;
   private final Material mPoreMaterial;
   private double mPoreRadius; // meters
   private double mPoreDensity; // pores per cubic meter
   private final double[] mDimension = new double[3];
   private MonteCarloSS.Region mRegion = null;
   private final MonteCarloSS mMonte;
   private MonteCarloSS.Region mParent = null; // null->mMonte->getChamber()
   transient private double mEstPoreFraction;

   /**
    * PorousBlock - Create a porous block of the specified dimensions (3 element
    * array containing x,y and z dimensions in meters). The default bulk
    * material is iron and the default pore material is vacuum. The default pore
    * radius is 10 nm with a default density of 100 per cubic micrometer or 1e20
    * per cubic meter.
    * 
    * @param mcss
    *           MonteCarloSS
    * @param dimension
    *           double[]
    */
   public PorousBlock(MonteCarloSS mcss, double[] dimension) {
      try {
         mBulkMaterial = MaterialFactory.createPureElement(Element.Fe);
      } catch (final EPQException ex) {
         // Iron doesn't ever throw an exception
      }
      mPoreMaterial = (Material) MaterialFactory.createMaterial(MaterialFactory.PerfectVacuum);
      mPoreRadius = 1.0e-8; // meters
      mPoreDensity = 1.0e20; // pores per cubic meter
      //
      mMonte = mcss;
      System.arraycopy(dimension, 0, mDimension, 0, 3);
   }

   /**
    * setBulkMaterial - Set the material for the bulk of the region.
    * 
    * @param mat
    *           Material
    */
   public void setBulkMaterial(Material mat) {
      if (mRegion != null)
         throw new EPQFatalException("Changing a configuration parameter after the region is created is forbidden.");
      if (!mat.equals(mBulkMaterial)) {
         mBulkMaterial = mat;
         mRegion = null;
      }
   }

   /**
    * setPoreRadius - Set the radius of the pores. All pores have the same
    * radius.
    * 
    * @param radius
    *           double
    */
   public void setPoreRadius(double radius) {
      assert (radius > 0.0);
      assert (radius < 1.0e-3);
      if (mRegion != null)
         throw new EPQFatalException("Changing a configuration parameter after the region is created is forbidden.");
      if ((radius > 0.0) && (radius < 1.0e-3) && (radius != mPoreRadius)) {
         mPoreRadius = radius;
         mRegion = null;
      }
   }

   /**
    * setPoreDensity - Sets the density of pores in the bulk material. The pore
    * density is defined as the number of pores in one cubic meter of material.
    * For comparison, a typical analytical volume is about 1 micrometer on an
    * edge or a volume of 1.0e-18 meters cubed. One pore per micrometer cube
    * would be equivalent to 1.0e18 pores per meter.
    * 
    * @param density
    *           double
    */
   public void setPoreDensity(double density) {
      if (mRegion != null)
         throw new EPQFatalException("Changing a configuration parameter after the region is created is forbidden.");
      if (mPoreDensity != density) {
         mPoreDensity = density;
         mRegion = null;
      }
   }

   /**
    * setParentRegion - Set the parent region in which this region is created.
    * By default (or if setParentRegion(null) is called) then the parent region
    * is the chamber region.
    * 
    * @param parent
    *           Region
    */
   public void setParentRegion(MonteCarloSS.Region parent) {
      if (mRegion != null)
         throw new EPQFatalException("Changing a configuration parameter after the region is created is forbidden.");
      if (mParent != parent) {
         mParent = parent;
         mRegion = null;
      }
   }

   /**
    * createRegion - Create a region based on the current set of parameters.
    * 
    * @return Region
    */
   public MonteCarloSS.Region createRegion() throws EPQException {
      if (mRegion == null) {
         final double totalVol = mDimension[0] * mDimension[1] * mDimension[2];
         final int poreCount = (int) Math.round(mPoreDensity * totalVol);
         final MultiPlaneShape mps = MultiPlaneShape.createBlock(mDimension, new double[]{0.0, 0.0, 0.0}, 0.0, 0.0, 0.0);
         final MonteCarloSS.Region bulk = mMonte.addSubRegion(mParent == null ? mMonte.getChamber() : mParent, mBulkMaterial, mps);
         final double[] center = new double[3];
         double nonPoreVol = totalVol;
         final double poreVol = ((4.0 * Math.PI) / 3.0) * mPoreRadius * mPoreRadius * mPoreRadius;
         for (int i = 0; i < poreCount; ++i) {
            center[0] = mDimension[0] * (Math.random() - 0.5);
            center[1] = mDimension[1] * (Math.random() - 0.5);
            center[2] = mDimension[2] * (Math.random() - 0.5);
            final Sphere sphere = new Sphere(center, mPoreRadius);
            mMonte.addSubRegion(bulk, mPoreMaterial, sphere);
            // Estimate the remaining non-pore volume
            nonPoreVol -= poreVol * (nonPoreVol / totalVol);
         }
         mEstPoreFraction = nonPoreVol / totalVol;
         mRegion = bulk;
      }
      return mRegion;
   }

   /**
    * estimatePoreFraction - Returns an estimate of how much of the original
    * volume of the substrate region is filled with pores. The estimate accounts
    * for overlapping pores in an average sense.
    * 
    * @return double
    */
   public double estimatedPoreFraction() throws EPQException {
      createRegion();
      return mEstPoreFraction;
   }

   @Override
   public String toString() {
      return "Porous[" + mBulkMaterial.toString() + ";" + mPoreMaterial.toString() + ";r=" + mPoreRadius + ";\u03C1=" + mPoreDensity + "]";
   }

}

/*
 * This scatter mechanism was part of the original MONSEL series. I mean to keep
 * it around in this form for historical interest. It should only be edited to
 * correct differences between it and the original MONSEL algorithms. Ideas for
 * improvements should be implemented in a new scatter mechanism under a new
 * name.
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.BrowningEmpiricalCrossSection;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Extends ScatterMechanism to create a Mott Elastic mechanism approximated
 * using Browning's interpolation. This class differs from the
 * BrowningEmpiricalCrossSection class because the latter computes scattering
 * for elements only. The present class extends the functionality to materials
 * composed of more than one element.
 * </p>
 * <p>
 * The Mott mechanism represents elastic scattering. It never generates a
 * secondary electron. Hence, its scatter() method returns null.
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined: density, elemental composition, and
 * weight fractions of elements.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class BrowningMottElasticSM
   extends
   ScatterMechanism
   implements
   Cloneable {

   private BrowningEmpiricalCrossSection[] browningElement = null;

   private double[] scalefactor; // weight fraction/atomic weight

   /* We use cross sections divided by atomic weight */
   private double[] cumulativeScaledCrossSection;

   private double totalScaledCrossSection;

   private int nce; // # constituent elements

   private double densityNa; // Avagadro's # * density for this material

   private double cached_eK = -1.; // Initialize to impossible value
   private double rateMultiplier = 1.; // temporary

   /**
    *
    */
   public BrowningMottElasticSM(Material mat) {
      super();
      setMaterial(mat);
   }

   private void setCache(double eK) {
      /*
       * Algorithm: 1. Get scaled cross section (cross section times weight
       * fraction divided by atomic weight) for each element in this material 2.
       * From this, determine the total scaled cross section. 3. Cache these for
       * later use.
       */
      totalScaledCrossSection = 0.;
      for(int i = 0; i < nce; i++) {
         totalScaledCrossSection += browningElement[i].totalCrossSection(eK) * scalefactor[i] * rateMultiplier;
         cumulativeScaledCrossSection[i] = totalScaledCrossSection;
      }
      // Remember kinetic energy for which the cache was created
      cached_eK = eK;
   }

   /*
    * (non-Javadoc)
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(gov.nist
    * .microanalysis.EPQLibrary.Material, double)
    */
   @Override
   public double scatterRate(Electron pe) {
      setCache(pe.getEnergy()); // computes totalScaledCrossSection for this
      // eK
      return totalScaledCrossSection * densityNa;
   }

   /*
    * (non-Javadoc)
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#randomScatteringAngle
    * (gov.nist.microanalysis.EPQLibrary.Material, double)
    */
   @Override
   public Electron scatter(Electron pe) {
      final double eK = pe.getPreviousEnergy();
      if(eK != cached_eK)
         setCache(eK);
      // Decide which element we scatter from
      final double r = Math2.rgen.nextDouble() * totalScaledCrossSection;
      int index = 0; // Index is first index

      // Increment index and mechanism until cumulative scatter rate exceeds r
      while(cumulativeScaledCrossSection[index] < r)
         index++;

      final double alpha = browningElement[index].randomScatteringAngle(eK);
      final double beta = 2 * Math.PI * Math2.rgen.nextDouble();
      pe.updateDirection(alpha, beta);
      return null; // This mechanism is elastic. No SE.
   }

   /*
    * (non-Javadoc)
    * @seegov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#init(gov.nist.
    * microanalysis.EPQLibrary.Material)
    */
   @Override
   public void setMaterial(Material mat) {
      nce = mat.getElementCount();
      densityNa = mat.getDensity() * PhysicalConstants.AvagadroNumber;
      if(nce > 0) {
         // Element[] elements = (Element[]) mat.getElementSet().toArray();
         final Set<Element> elements = mat.getElementSet();
         browningElement = new BrowningEmpiricalCrossSection[nce];
         scalefactor = new double[nce];
         cumulativeScaledCrossSection = new double[nce];

         int i = 0;
         for(final Element elm : elements) {
            browningElement[i] = new BrowningEmpiricalCrossSection(elm);
            // The factor of 1000 in the next line is to convert atomic
            // weight in g/mole to kg/mole.
            scalefactor[i] = (1000. * mat.weightFraction(elm, true)) / elm.getAtomicWeight();
            i++;
         }
      }
   }

   /**
    * Gets the current value assigned to rateMultiplier
    *
    * @return Returns the rateMultiplier.
    */
   public double getRateMultiplier() {
      return rateMultiplier;
   }

   /**
    * Sets the value assigned to rateMultiplier. I expect to remove this method.
    * I inserted it on 8/3/2012 to do some tests on the effect of scattering
    * cross-section changes without any change in angular distribution.
    *
    * @param rateMultiplier The value to which to set rateMultiplier.
    */
   public void setRateMultiplier(double rateMultiplier) {
      if(rateMultiplier > 0.)
         this.rateMultiplier = rateMultiplier;
      else
         throw new EPQFatalException("rateMultiplier must be positive.");
   }

   @Override
   public BrowningMottElasticSM clone()
         throws CloneNotSupportedException {
      return (BrowningMottElasticSM) super.clone();
   }

}

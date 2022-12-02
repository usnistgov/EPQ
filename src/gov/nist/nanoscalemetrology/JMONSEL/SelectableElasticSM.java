/**
 * gov.nist.nanoscalemetrology.JMONSEL.SelectableElasticSM Created by: John
 * Villarrubia Date: Oct 29, 2009
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatter;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * SelectableElasticSM is a ScatterMechanism for elastic scattering in materials
 * comprised of one or more elemental constituents. The algorithm it uses is
 * selected by supplying a RandomizedScatterFactory of the appropriate type to
 * the constructor. The RandomizedScatterFactory (e.g.,
 * NISTMottScatteringAngle.Factory (the default),
 * CzyzewskiMottScatteringAngle.Factory,
 * ScreenedRutherfordScatteringAngle.Factory, ...) determines the underlying
 * scattering cross sections, both total and differential, that in turn
 * determine the mean free path and the distribution of scattering angles.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class SelectableElasticSM extends ScatterMechanism implements Cloneable {

   // Array of randomized scatterers, one for each element
   private RandomizedScatter[] rse = null;
   // Set scatter class default to NISTMottScatteringAngle
   private final RandomizedScatterFactory rsf;

   private double[] scalefactor; // weight fraction/atomic weight
   /* We use cross sections divided by atomic weight */
   private double[] cumulativeScaledCrossSection;

   private double totalScaledCrossSection;
   private int nce; // # constituent elements

   private double densityNa; // Avagadro's # * density for this material

   private double cached_kE = -1.; // Initialize to impossible value

   /**
    * Constructs a SelectableElasticSM
    *
    * @param mat
    *           - The Material for which scattering is to be determined
    * @param rsf
    *           - The RandomizedScatterFactory that is to be used for the
    *           calculations
    */
   public SelectableElasticSM(Material mat, RandomizedScatterFactory rsf) {
      super();
      this.rsf = rsf;
      setMaterial(mat);
   }

   /**
    * Constructs a SelectableElasticSM. This form of the constructor defaults to
    * algorithms determined by NISTMottScatteringAngle.
    *
    * @param mat
    *           - The Material for which scattering is to be determined
    */
   public SelectableElasticSM(Material mat) {
      this(mat, NISTMottScatteringAngle.Factory); // Set to default
   }

   private void setCache(double kE) {
      /*
       * Algorithm: 1. Get scaled cross section (cross section times weight
       * fraction divided by atomic weight) for each element in this material 2.
       * From this, determine the total scaled cross section. 3. Cache these for
       * later use.
       */
      totalScaledCrossSection = 0.;
      for (int i = 0; i < nce; i++) {
         totalScaledCrossSection += rse[i].totalCrossSection(kE) * scalefactor[i];
         cumulativeScaledCrossSection[i] = totalScaledCrossSection;
      }
      // Remember kinetic energy for which the cache was created
      cached_kE = kE;
   }

   /*
    * (non-Javadoc)
    * 
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
    * 
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#randomScatteringAngle
    * (gov.nist.microanalysis.EPQLibrary.Material, double)
    */
   @Override
   public Electron scatter(Electron pe) {
      final double kE = pe.getPreviousEnergy();
      if (kE != cached_kE)
         setCache(kE);
      // Decide which element we scatter from
      final double r = Math2.rgen.nextDouble() * totalScaledCrossSection;
      int index = 0; // Index is first index

      // Increment index and mechanism until cumulative scatter rate exceeds r
      while (cumulativeScaledCrossSection[index] < r)
         index++;

      final double alpha = rse[index].randomScatteringAngle(kE);
      final double beta = 2 * Math.PI * Math2.rgen.nextDouble();
      pe.updateDirection(alpha, beta);
      pe.setScatteringElement(rse[index].getElement());
      return null; // This mechanism is elastic. No SE.
   }

   /**
    * @param mat
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#setMaterial(gov.nist.microanalysis.EPQLibrary.Material)
    */
   @Override
   public void setMaterial(Material mat) {
      nce = mat.getElementCount();
      densityNa = mat.getDensity() * PhysicalConstants.AvagadroNumber;
      if (nce > 0) {
         // Element[] elements = (Element[]) mat.getElementSet().toArray();
         final Set<Element> elements = mat.getElementSet();
         rse = new RandomizedScatter[nce];
         scalefactor = new double[nce];
         cumulativeScaledCrossSection = new double[nce];

         int i = 0;
         for (final Element elm : elements) {
            rse[i] = rsf.get(elm);
            // The factor of 1000 in the next line is to convert atomic
            // weight in g/mole to kg/mole.
            scalefactor[i] = (1000. * mat.weightFraction(elm, true)) / elm.getAtomicWeight();
            i++;
         }
      }

   }

}

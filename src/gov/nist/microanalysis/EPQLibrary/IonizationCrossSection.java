package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Simplex;
import gov.nist.microanalysis.Utility.UtilException;

/**
 * <p>
 * An abstract class implementing various different algorithms to calculate the
 * ionization cross section. Note that the implementations fall into two classes
 * - those that calculate an absolute cross section and those that calculate a
 * proportional cross section. The absolute cross sections are useful for
 * standardless analysis and absolute instensity prediction (Monte Carlos) while
 * the proportional cross sections are useful for standards-based analysis. The
 * proportional cross sections are good for calculating the cross section at one
 * overvoltage relative to another overvoltage.
 * </p>
 * <p>
 * An additional subtlety is the dependence on the shell within the family. Many
 * algorithms provide the ionization cross- section for a family of lines.
 * However for consistency we want to compute the ionization cross-section for
 * each shell. The shell depenedence is encapsulated in the method
 * shellDependence and is used to extend family-based expressions to consider
 * individual shells.
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

abstract public class IonizationCrossSection
   extends AlgorithmClass {

   /**
    * Converts from m^2 to cm^2
    * 
    * @param x
    * @return double a cross-section in cm^2
    */
   static public double toCmSqr(double x) {
      return FromSI.CM * FromSI.CM * x;
   }

   /**
    * Converts from cm^2 to m^2
    * 
    * @param x
    * @return double a cross-section in m^2
    */
   static public double fromCmSqr(double x) {
      return ToSI.CM * ToSI.CM * x;
   }

   /**
    * shellDependence - In addition to the overvoltage dependence, the
    * ionization cross section depends upon which shell is ionized. The
    * dependence is a proportional to the number of electrons in the shell.
    * 
    * @param sh AtomicShell
    * @return double
    */
   static public double shellDependence(AtomicShell sh) {
      return shellDependence(sh.getShell());
   }

   /**
    * shellDependence - In addition to the overvoltage dependence, the
    * ionization cross section depends upon which shell is ionized. The
    * dependence is a proportional to the number of electrons in the shell.
    * 
    * @param shell [AtomicShell.K, AtomicShell.LI, AtomicShell.LII, ...,
    *           AtomicShell.Last)
    * @return double
    */
   static public double shellDependence(int shell) {
      assert (AtomicShell.isValid(shell));
      final double n = ((AtomicShell.getFamily(shell) - AtomicShell.KFamily) + 1);
      return AtomicShell.getCapacity(shell) / (2.0 * n * n);
   }

   protected IonizationCrossSection(String name, String ref) {
      super("Ionization Cross Section", name, ref);
   }

   protected IonizationCrossSection(String name, LitReference ref) {
      super("Ionization Cross Section", name, ref);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * Computes the ionization cross section for an energetic electron on the
    * specified element/shell.
    * 
    * @param shell AtomicShell - Specifies the element and shell (K, LI, ...)
    * @param beamE double - The beam energy in Joules
    * @return double - The ionization cross section (in m^2)
    */
   abstract public double computeShell(AtomicShell shell, double beamE);

   public double computeFamily(AtomicShell shell, double beamE) {
      return computeShell(shell, beamE) / shellDependence(shell);
   }

   /**
    * Finds the energy with the largest ionization cross section.
    * 
    * @param shell
    * @return double - Energy in Joules
    * @throws UtilException
    */
   public double peak(AtomicShell shell)
         throws UtilException {
      final Object[] param = new Object[] {
         shell
      };
      final Simplex s = new Simplex(param) {
         @Override
         public double function(double[] x) {
            final AtomicShell shell = (AtomicShell) getParameters()[0];
            return -computeFamily(shell, x[0]);
         }
      };
      final double[] sp = new double[] {
         3.0 * shell.getEdgeEnergy()
      };
      final double[] res = s.perform(Simplex.randomizedStartingPoints(sp, Math2.multiply(0.1, sp)));
      return res[0];
   }
};

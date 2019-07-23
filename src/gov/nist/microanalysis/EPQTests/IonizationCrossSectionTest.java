package gov.nist.microanalysis.EPQTests;

import java.util.List;

import gov.nist.microanalysis.EPQLibrary.AlgorithmClass;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ProportionalIonizationCrossSection;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the IonizationCrossSection classes.
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
public class IonizationCrossSectionTest
   extends TestCase {

   /**
    * Constructs an IonizationCrossSectionTest
    * 
    * @param arg0
    */
   public IonizationCrossSectionTest(String arg0) {
      super(arg0);
   }

   public void testOne() {
      final List<AlgorithmClass> algs = ProportionalIonizationCrossSection.Pouchou86.getAllImplementations();
      final AtomicShell[] shells = {
         new AtomicShell(Element.Ca, AtomicShell.LIII)
      };

      for(final AlgorithmClass ac : algs) {
         final ProportionalIonizationCrossSection alg = (ProportionalIonizationCrossSection) ac;
         for(final AtomicShell shell : shells) {
            final double e0 = shell.getEdgeEnergy();
            final double[] e0s = {
               1.4 * e0,
               2.0 * e0,
               4.0 * e0,
               8.0 * e0
            };
            for(final double e02 : e0s) {
               if(false) {
                  System.out.print("P\t");
                  System.out.print(alg);
                  System.out.print("\t");
                  System.out.print(shell);
                  System.out.print("\t");
                  System.out.print(e02 / e0);
                  System.out.print("\t");
                  System.out.print(alg.computeShell(shell, e02));
                  System.out.print("\t");
                  System.out.print(ProportionalIonizationCrossSection.Pouchou86.computeShell(shell, e02));
                  System.out.println();
               }
               assertEquals(alg.computeShell(shell, e02), ProportionalIonizationCrossSection.Pouchou86.computeShell(shell, e02), 0.1
                     * ProportionalIonizationCrossSection.Pouchou86.computeShell(shell, e02));
            }
         }
      }
   }
}

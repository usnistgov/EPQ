package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AlgorithmClass;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.BackscatterFactor;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;

import java.util.List;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the BackscatterFactor class.
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
public class BackscatterFactorTest
   extends TestCase {

   /**
    * Constructs a BackscatterFactorTest
    * 
    * @param arg0
    */
   public BackscatterFactorTest(String arg0) {
      super(arg0);
   }

   @SuppressWarnings({
      "deprecation"
   })
   public void testOne()
         throws EPQException {
      // Implements an internal self consistency test
      final Composition comp = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final List<AlgorithmClass> algs = BackscatterFactor.Default.getAllImplementations();
         final AtomicShell[] shells = {
            new AtomicShell(Element.Ca, AtomicShell.K),
            new AtomicShell(Element.Fe, AtomicShell.LIII),
            new AtomicShell(Element.Si, AtomicShell.K),
            new AtomicShell(Element.O, AtomicShell.K)
         };
         for(final AlgorithmClass alg : algs) {
            final BackscatterFactor bs = (BackscatterFactor) alg;
            for(final AtomicShell shell : shells) {
               final double def = BackscatterFactor.Default.compute(comp, shell, e0);
               final double cur = bs.compute(comp, shell, e0);
               assertEquals(def, cur, 0.1);
            }
         }
      }
      {
         final BackscatterFactor algs[] = {
            BackscatterFactor.Yakowitz1973,
            BackscatterFactor.Pouchou1991,
            BackscatterFactor.Love1978
         };
         final double answer[] = {
            1.0072,
            1.0081,
            1.0065
         };
         final AtomicShell shell = new AtomicShell(Element.Si, AtomicShell.K);
         for(int i = 0; i < algs.length; ++i) {
            final BackscatterFactor alg = algs[i];
            System.out.print(shell.toString() + "\t");
            System.out.print(alg.toString() + "\t");
            final double res = alg.compute(comp, shell, e0)
                  / alg.compute(MaterialFactory.createPureElement(shell.getElement()), shell, e0);
            System.out.println(res);
            assertEquals(res, answer[i], 0.01);
         }
      }
      {
         final BackscatterFactor algs[] = {
            BackscatterFactor.Yakowitz1973,
            BackscatterFactor.Pouchou1991,
            BackscatterFactor.Love1978
         };
         final double answer[] = {
            0.9988,
            1.0003,
            0.9990
         };
         final AtomicShell shell = new AtomicShell(Element.Al, AtomicShell.K);
         for(int i = 0; i < algs.length; ++i) {
            final BackscatterFactor alg = algs[i];
            System.out.print(shell.toString() + "\t");
            System.out.print(alg.toString() + "\t");
            final double res = alg.compute(comp, shell, e0)
                  / alg.compute(MaterialFactory.createPureElement(shell.getElement()), shell, e0);
            System.out.println(res);
            assertEquals(res, answer[i], 0.01);
         }
      }
   }

   public void testLove78A()
         throws EPQException {
      final BackscatterFactor bs = BackscatterFactor.Love1978;
      final double e0 = ToSI.keV(20.0);
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         assertEquals(bs.compute(mat, sh, e0)
               / bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0), 1.0528, 0.001);
      }
   }

   public void testLove78B()
         throws EPQException {
      final BackscatterFactor bs = BackscatterFactor.Love1978;
      final double e0 = ToSI.keV(20.0);
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         assertEquals(bs.compute(mat, sh, e0)
               / bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0), 1.0635, 0.001);
      }
   }

   public void testPouchou91A()
         throws EPQException {
      final BackscatterFactor bs = BackscatterFactor.Pouchou1991;
      final double e0 = ToSI.keV(20.0);
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         // assertEquals(bs.compute(mat, sh, e0), 0.228, 0.001);
         assertEquals(bs.compute(mat, sh, e0)
               / bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0), 1.0448, 0.001);
      }
   }

   public void testPouchou91B()
         throws EPQException {
      final BackscatterFactor bs = BackscatterFactor.Pouchou1991;
      final double e0 = ToSI.keV(20.0);
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         // assertEquals(bs.compute(mat, sh, e0), 0.248, 0.001);
         assertEquals(bs.compute(mat, sh, e0)
               / bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0), 1.0544, 0.001);
      }
   }

   public void testDuncumb81A()
         throws EPQException {
      final BackscatterFactor bs = BackscatterFactor.Duncumb1981;
      final double e0 = ToSI.keV(20.0);
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         // assertEquals(bs.compute(mat, sh, e0), 0.228, 0.001);
         assertEquals(bs.compute(mat, sh, e0)
               / bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0), 1.0396, 0.001);
      }
   }

   public void testDuncumb81B()
         throws EPQException {
      final BackscatterFactor bs = BackscatterFactor.Duncumb1981;
      final double e0 = ToSI.keV(20.0);
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         // assertEquals(bs.compute(mat, sh, e0), 0.248, 0.001);
         assertEquals(bs.compute(mat, sh, e0)
               / bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0), 1.0613, 0.001);
      }
   }

   /*
    * public void testMyklebust1991A() throws EPQException { BackscatterFactor
    * bs = BackscatterFactor.Myklebust1991; double e0 = ToSI.keV(20.0);
    * Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189); {
    * AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K); //
    * assertEquals(bs.compute(mat, sh, e0), 0.228, 0.001);
    * assertEquals(bs.compute(mat, sh, e0) /
    * bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0),
    * 1.0518, 0.001); } } public void testMyklebust1991B() throws EPQException {
    * BackscatterFactor bs = BackscatterFactor.Myklebust1991; double e0 =
    * ToSI.keV(20.0); Composition mat =
    * MaterialFactory.createMaterial(MaterialFactory.K3189); { AtomicShell sh =
    * new AtomicShell(Element.Ti, AtomicShell.K); //
    * assertEquals(bs.compute(mat, sh, e0), 0.248, 0.001);
    * assertEquals(bs.compute(mat, sh, e0) /
    * bs.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0),
    * 1.0666, 0.001); } }
    */
}

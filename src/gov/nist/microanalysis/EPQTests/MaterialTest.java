package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQTools.EPQXStream;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the Material class.
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
public class MaterialTest
   extends TestCase {
   /**
    * MaterialTest
    */
   public MaterialTest(String test) {
      super(test);
   }

   public void testOne()
         throws EPQException {
      final double eps = 1.0e-8;
      {
         final Composition mat = new Composition();
         // Titanium dioxide - TiO2 (Z(Ti)=22,
         final double wTi = Element.getAtomicWeight(Element.elmTi)
               / (Element.getAtomicWeight(Element.elmTi) + (2.0 * Element.getAtomicWeight(Element.elmO)));
         final double wO = (2.0 * Element.getAtomicWeight(Element.elmO))
               / (Element.getAtomicWeight(Element.elmTi) + (2.0 * Element.getAtomicWeight(Element.elmO)));
         assertEquals(wTi + wO, 1.0, eps);
         final double fac = 1.1;
         mat.addElement(Element.elmTi, fac * wTi);
         mat.addElement(Element.elmO, fac * wO);
         {
            final Element[] elms = {
               Element.Ti,
               Element.O
            };
            final double[] wgts = {
               fac * wTi,
               fac * wO
            };
            final Composition dup = new Composition(elms, wgts);
            assertTrue(mat.equals(dup));
         }
         assertEquals(mat.weightFraction(Element.Ti, true), wTi, eps);
         assertEquals(mat.weightFraction(Element.O, true), wO, eps);
         assertEquals(mat.weightFraction(Element.Ti, false), fac * wTi, eps);
         assertEquals(mat.weightFraction(Element.O, false), fac * wO, eps);
         assertTrue(mat.equals(mat));
         assertTrue(!mat.equals(null));
         {
            final Material mm = new Material(1.0);
            mm.addElement(Element.elmW, 2.0);
            mm.addElement(Element.elmO, 1.0);
            assertTrue(!mat.equals(mm));
         }
      }
      { // K3189 - NIST SRM glass
         final Element[] elms = {
            Element.Si,
            Element.O,
            Element.Al,
            Element.Ca,
            Element.Mg,
            Element.Ti,
            Element.Fe
         };
         final double[] mF = {
            0.151971599,
            0.608811816,
            0.062690151,
            0.05699065,
            0.056638401,
            0.005716531,
            0.057180851
         };
         final double[] wF = {
            0.186973968,
            0.426700667,
            0.074093109,
            0.100056707,
            0.06030359,
            0.011986858,
            0.139885101
         };
         final Material mat = new Material(1.0);
         mat.defineByMoleFraction(elms, mF);
         for(int i = 0; i < elms.length; ++i) {
            assertEquals(mat.weightFraction(elms[i], true), wF[i], 1.0e-5);
            assertEquals(mat.atomicPercent(elms[i]), mF[i], 1.0e-5);
         }
         { // Try an alternative method to define a K3189
            final Material[] mats = new Material[6];
            mats[0] = new Material(1.0);
            mats[0].defineByMoleFraction(new Element[] {
               Element.Si,
               Element.O
            }, new double[] {
               1.0,
               2.0
            });
            mats[1] = new Material(1.0);
            mats[1].defineByMoleFraction(new Element[] {
               Element.Al,
               Element.O
            }, new double[] {
               2.0,
               3.0
            });
            mats[2] = new Material(1.0);
            mats[2].defineByMoleFraction(new Element[] {
               Element.Ca,
               Element.O
            }, new double[] {
               1.0,
               1.0
            });
            mats[3] = new Material(1.0);
            mats[3].defineByMoleFraction(new Element[] {
               Element.Mg,
               Element.O
            }, new double[] {
               1.0,
               1.0
            });
            mats[4] = new Material(1.0);
            mats[4].defineByMoleFraction(new Element[] {
               Element.Ti,
               Element.O
            }, new double[] {
               1.0,
               2.0
            });
            mats[5] = new Material(1.0);
            mats[5].defineByMoleFraction(new Element[] {
               Element.Fe,
               Element.O
            }, new double[] {
               2.0,
               3.0
            });
            mat.defineByMaterialFraction(mats, new double[] {
               0.40,
               0.14,
               0.14,
               0.10,
               0.02,
               0.20
            });
            for(int i = 0; i < elms.length; ++i)
               assertEquals(mat.weightFraction(elms[i], true), wF[i], 1.0e-5);
            final Material dup = mat.clone();
            assertTrue(mat.equals(dup));
            dup.addElement(Element.elmAr, 0.12);
            assertTrue(!mat.equals(dup));
         }
      }
      {
         final String[] mats = {
            MaterialFactory.K2450,
            MaterialFactory.Ice,
            MaterialFactory.Al2O3
         };
         for(final String mat : mats) {
            final Composition comp = MaterialFactory.createMaterial(mat);
            final String ms = EPQXStream.getInstance().toXML(comp);
            assertTrue(EPQXStream.getInstance().fromXML(ms).equals(comp));
         }
      }
   }
}

package gov.nist.microanalysis.EPQLibrary;

import java.io.IOException;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * An interface to the Pouchou and Pichoir database of microanalytical
 * measurements of standard materials. This database was published in Electron
 * Probe Quantitation, Heinrich &amp; Newbury (editors)
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

public class PandPDatabase {
   double[][] mData;

   /**
    * PandPDatabase - Creates an object representing the Pouchou and Pichoir
    * database of micronalytical measurements of known materials.
    * 
    * @throws IOException
    */
   public PandPDatabase()
         throws IOException {
      final CSVReader cr = new CSVReader.ResourceReader("PandPdb.csv", false);
      mData = cr.getResource(PandPDatabase.class);
      assert (mData.length == 826);
      assert (mData[0].length == 7);
   }

   /**
    * getSize - Get the number of items in the database. (Nominally 826)
    * 
    * @return int
    */
   public int getSize() {
      return mData.length;
   }

   /**
    * createMaterial - Creates the material defined by the index-th item in the
    * database.
    * 
    * @param index int
    * @return Material
    */
   public Material createMaterial(int index) {
      final Material mat = new Material(0.0);
      final Element[] elms = new Element[2];
      final double[] datum = mData[index];
      elms[0] = Element.byAtomicNumber((int) Math.round(datum[0]));
      elms[1] = Element.byAtomicNumber((int) Math.round(datum[2]));
      final double[] wgtPct = new double[2];
      wgtPct[0] = datum[4];
      wgtPct[1] = 1.0 - datum[4];
      mat.defineByWeightFraction(elms, wgtPct);
      mat.setDensity(MaterialFactory.estimatedDensity(mat));
      return mat;
   }

   /**
    * elementA - Returns the Element on which the measurement was performed.
    * 
    * @param index int
    * @return Element
    */
   public Element elementA(int index) {
      return Element.byAtomicNumber((int) Math.round(mData[index][0]));
   }

   /**
    * elementB - Returns the other element in the matrix.
    * 
    * @param index int
    * @return Element
    */
   public Element elementB(int index) {
      return Element.byAtomicNumber((int) Math.round(mData[index][2]));
   }

   /**
    * createStandard - Create a Material representing element A (the element to
    * be measured.)
    * 
    * @param index int
    * @throws EPQException
    * @return Material
    */
   public Material createStandard(int index)
         throws EPQException {
      return MaterialFactory.createPureElement(elementA(index));
   }

   /**
    * kRatio - Returns the k-ration (dimensionless) of the index-th item in the
    * database.
    * 
    * @param index int
    * @return double
    */
   public double kRatio(int index) {
      return mData[index][5];
   }

   /**
    * takeOffAngle - Returns the take off angle (in radians) of the index-th
    * item in the database.
    * 
    * @param index int
    * @return double
    */
   public double takeOffAngle(int index) {
      return Math.toRadians(mData[index][6]);
   }

   /**
    * beamEnergy - Returns the beam energy (in Joules) of the index-th item in
    * the database.
    * 
    * @param index int
    * @return double
    */
   public double beamEnergy(int index) {
      return ToSI.keV(mData[index][3]);
   }

   /**
    * transition - Returns the x-ray transition associated with the index-th
    * item in the database.
    * 
    * @param index int
    * @return XRayTransition
    */
   public XRayTransition transition(int index) {
      final double[] datum = mData[index];
      return new XRayTransition(Element.byAtomicNumber((int) Math.round(datum[0])), (int) Math.round(datum[1]));
   }
}

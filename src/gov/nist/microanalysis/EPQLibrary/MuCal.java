package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * MuCal implements the prescription for parameterizing the coherent and
 * incoherent scattering cross sections based on the work of McMaster published
 * as UCRL-50174 Sec. II Rev 1.
 * </p>
 * <p>
 * The implementation is based on the work of Pathikrit Badyopadhyay who
 * acknowledges
 * <ul>
 * <li>Dr. B. A. Bunker, Dr. Q. T. Islam, B. I. Boaynov for their helpful
 * suggestions.</li>
 * <li>Dr. M. Zanabria for helping in typing the data in.</li>
 * <li>Dr. B. A. Bunker and Dr. Anne Tabor-Morris for subsequent
 * modifications.</li>
 * </ul>
 * <p>
 * Data tables extracted from
 * <a href="http://csrri.iit.edu/mucal-src/mucal_c-1.3.tar.gz">mucal.c, version
 * 1.3</a>. Addition values for Z = 84, 85, 97, 88, 89, 91 and 94 were
 * interpolated from the tabulated values.
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
public class MuCal {

   static final int Z_COL = 0;
   static final int CON_FAC_COL = 1;
   static final int NCOHER_COL = 3;
   static final int COHER_COL = 7;

   final double[] conv_fac;
   final double[][] xsect_coh;
   final double[][] xsect_ncoh;

   /**
    * Constructs a MuCal
    */
   public MuCal() {
      // Cols are Z, conv_fac, ncoh[0-3], coh[0-3]
      final CSVReader cr = new CSVReader.ResourceReader("McMaster.csv", false);
      double[][] table = cr.getResource(MassAbsorptionCoefficient.class);
      conv_fac = new double[table.length];
      xsect_coh = new double[table.length][4];
      xsect_ncoh = new double[table.length][4];
      for (int i = 0; i < table.length; ++i) {
         final double[] row = table[i];
         assert (int) Math.round(row[0]) == i + 1;
         conv_fac[i] = row[1];
         xsect_ncoh[i][0] = row[2];
         xsect_ncoh[i][1] = row[3];
         xsect_ncoh[i][2] = row[4];
         xsect_ncoh[i][3] = row[5];
         xsect_coh[i][0] = row[6];
         xsect_coh[i][1] = row[7];
         xsect_coh[i][2] = row[8];
         xsect_coh[i][3] = row[9];
      }
   }

   final private static double computeEqn4(double ephot, double[] fit) {
      final double log_e = Math.log(ephot);
      double xsec = 0.0;
      for (int i = 0; i < 4; i++)
         xsec += fit[i] * Math.pow(log_e, i);
      return Math.exp(xsec);
   }

   /**
    * Returns the coherent scattering cross section for the specified photon
    * energy and atom.
    * 
    * @param elm
    * @param ePhoton
    *           Photon energy in joules
    * @return The cross section in meters<sup>2</sup>/kg
    */
   final public double computeCoherent(Element elm, double ePhoton) {
      final int idx = elm.getAtomicNumber() - 1;
      final double res = (computeEqn4(FromSI.keV(ePhoton), xsect_coh[idx]) * 0.1) / conv_fac[idx];
      return res;
   }

   /**
    * Returns the incoherent scattering cross section for the specified photon
    * energy and atom.
    * 
    * @param elm
    * @param ePhoton
    *           Photon energy in joules
    * @return The cross section in meters<sup>2</sup>/kg
    */
   final public double computeIncoherent(Element elm, double ePhoton) {
      final int idx = elm.getAtomicNumber() - 1;
      final double res = (computeEqn4(FromSI.keV(ePhoton), xsect_ncoh[idx]) * 0.1) / conv_fac[idx];
      return res;
   }

   /**
    * Converts MACs from cm^2/g
    * 
    * @param x
    * @return double
    */
   static public double fromCmSqrPerGram(double x) {
      assert Math.abs(((ToSI.CM * ToSI.CM) / ToSI.GRAM) - 0.1) < 1.0e-6;
      return 0.1 * x;
   }

   /**
    * Converts MACs from SI to cm^2/g
    * 
    * @param x
    * @return double
    */
   static public double toCmSqrPerGram(double x) {
      assert Math.abs(((FromSI.CM * FromSI.CM) / FromSI.GRAM) - 10.) < 1.0e-6;
      return 10.0 * x;
   }

   /**
    * Compute the scatter coefficient for an x-ray of the specified energy in
    * the specified material. Note the value returned is the absorption for a
    * material of a nominal density of 1 kg/m^3. Multiply this by the density to
    * get the true MAC.
    * 
    * @param comp
    *           Composition - The absorbing material
    * @param ePhoton
    *           double - The x-ray energy in Joules
    * @return Absorption per unit length per kg/m<sup>3</sup>
    */
   final public double computeCoherent(Composition comp, double ePhoton) {
      double mac = 0.0;
      for (final Element elm : comp.getElementSet())
         mac += computeCoherent(elm, ePhoton) * comp.weightFraction(elm, false);
      return mac;
   }

   /**
    * Compute the scatter coefficient for an x-ray of the specified energy in
    * the specified material. Note the value returned is the absorption for a
    * material of a nominal density of 1 kg/m^3.
    * 
    * @param comp
    *           Composition - The absorbing material
    * @param ePhoton
    *           double - The x-ray energy in Joules
    * @return Scatters per m per kg/m<sup>3</sup>
    */
   final public double computeIncoherent(Composition comp, double ePhoton) {
      double scat = 0.0;
      for (final Element elm : comp.getElementSet())
         scat += computeIncoherent(elm, ePhoton) * comp.weightFraction(elm, false);
      return scat;
   }

   /**
    * Mean distance that the specified x-ray will travel in the specified
    * material before undergoing a coherent event
    * 
    * @param mat
    * @param ePhoton
    * @return double Meters
    * @throws EPQException
    */
   public double coherentMeanFreePath(Material mat, double ePhoton) throws EPQException {
      return 1.0 / (computeCoherent(mat, ePhoton) * mat.getDensity()); // m^2
      // per kg
   }

   /**
    * Mean distance that the specified x-ray will travel in the specified
    * material before undergoing an incoherent event
    * 
    * @param mat
    * @param ePhoton
    * @return double Meters
    * @throws EPQException
    */
   public double incoherentMeanFreePath(Material mat, double ePhoton) throws EPQException {
      return 1.0 / (computeIncoherent(mat, ePhoton) * mat.getDensity());
   }
}

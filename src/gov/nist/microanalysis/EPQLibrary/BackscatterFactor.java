package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * Models for calculating the R factor - an estimate of the ratio of the total
 * inner shell ionization cross section which is actually produced in the
 * specimen over that which would occur in the absence of electron
 * backscattering. The single best reference that I've discovered that brings
 * together various different R models is the Myklebust &amp; Newbury article in
 * the book "Electron Probe Quantitation"
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
abstract public class BackscatterFactor
   extends AlgorithmClass {

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   protected BackscatterFactor(String name, LitReference reference) {
      super("Backscatter Factor", name, reference);
   }

   /**
    * caveat - The caveat method provides a mechanism for identifying the
    * limitations of a specific implementation of this algorithm based on the
    * arguments to the compute method. The result is a user friendly string
    * itemizing the limitations of the algorithm as known by the library author.
    * 
    * @param comp Composition
    * @param shell AtomicShell
    * @param e0 double
    * @return String
    */
   public String caveat(Composition comp, AtomicShell shell, double e0) {
      return CaveatBase.None;
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the BackscatterFactor class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * compute - Compute R, the x-ray loss due to electron backscatter for an
    * electron of energy e0 (Joules) impinging on the Composition comp, ionizing
    * the AtomicShell shell.
    * 
    * @param comp Composition
    * @param shell AtomicShell
    * @param e0 double - In Joules
    * @return double - On range (0.0,1.0] (Dimensionless)
    */
   abstract public double compute(Composition comp, AtomicShell shell, double e0);

   /**
    * Pouchou1991 - The backscatter correction algorithm described in Pouchou
    * and Pichoir's chapter in the book "Electron Probe Quantitation", Heinrich
    * &amp; Newbury editors (1991). (Produces very similar results to CITZAF)
    */
   static public class PouchouBackscatterFactor
      extends BackscatterFactor {
      PouchouBackscatterFactor() {
         super("Pouchou & Pichoir 1991", LitReference.PAPinEPQ);
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(BackscatterCoefficient.class, BackscatterCoefficient.PouchouAndPichoir91);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         assert (e0 < ToSI.keV(1.0e3));
         assert (e0 > ToSI.keV(0.01));
         // no potential for unit ambiguities here... (Only Z and u
         // dependencies)
         final BackscatterCoefficient bc = (BackscatterCoefficient) getAlgorithm(BackscatterCoefficient.class);
         final double eta = bc.compute(comp, e0);
         // avg reduced energy of backscattered electrons
         final double w = 0.595 + (eta / 3.7) + Math.pow(eta, 4.55);
         final double u0 = e0 / shell.getEdgeEnergy();
         final double ju = 1.0 + (u0 * (Math.log(u0) - 1.0));
         final double q = ((2.0 * w) - 1.0) / (1.0 - w);
         final double gu = (u0 - 1.0 - ((1.0 - (1.0 / Math.pow(u0, 1.0 + q))) / (1.0 + q))) / ((2.0 + q) * ju);
         return 1.0 - (eta * w * (1.0 - gu));
      }
   }

   static public final BackscatterFactor Pouchou1991 = new PouchouBackscatterFactor();

   /**
    * Myklebust1991 - The backscatter correction algorithm described in
    * Myklebust &amp; Newbury's chapter in the book "Electron Probe
    * Quantitation", Heinrich &amp; Newbury editors (1991).
    */

   static public class Myklebust91BackscatterFactor
      extends BackscatterFactor {
      Myklebust91BackscatterFactor() {
         super("Myklebust & Newbury 1991", new LitReference.BookChapter(LitReference.ElectronProbeQuant, new LitReference.Author[] {
            LitReference.RMyklebust,
            LitReference.DNewbury
         }));
      }

      @Override
      public String caveat(Composition comp, AtomicShell shell, double e0) {
         return CaveatBase.format(this, CaveatBase.NotImplemented);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         throw new UnsupportedOperationException("Myklebust1991 has not yet been implemented.");
      }
   }

   static public final BackscatterFactor Myklebust1991 = new Myklebust91BackscatterFactor();

   /**
    * Duncumb1981 - Duncumb &amp; Reed in Quantitative Electron Probe
    * Microanalysis, Heinrich Ed, NBS Special Publication 298 as described in
    * Heinrich's Electron Beam X-ray Microanalysis, 1981. (Note: this algorithm
    * is only hinted at in Pub. 298. Is the Heinrich's reference wrong?)
    * (Produces similar numbers as CITZAF.)
    */
   static public class DuncumbBackscatterFactor
      extends BackscatterFactor {
      DuncumbBackscatterFactor() {
         super("Duncumb & Reed 1981", new LitReference.BookChapter(LitReference.QuantitativeElectronProbeMicroanalysis, new LitReference.Author[] {
            LitReference.PDuncumb,
            LitReference.SReed
         }));
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         // Implementation taken from Myklebust &amp; Newbury in Electron Probe
         // Quantification and
         // Heinrich's Electron Beam X-ray Microanalysis.
         final double w = shell.getEdgeEnergy() / e0;
         double rAvg = 0.0;
         for(final Element el : comp.getElementSet()) {
            final double z = el.getAtomicNumber();
            final double rZ = 1.0
                  + (((((((((((2.962e-2 * w) - 8.619e-2) * w) + 9.213e-2) * w) - 5.137e-2) * w) + 2.162e-2) * w) - 0.581e-2)
                        * z)
                  + (((((((((((-17.676e-4 * w) + 46.540e-4) * w) - 47.744e-4) * w) + 28.791e-4) * w) - 8.298e-4) * w)
                        - 1.609e-4) * z * z)
                  + (// The sign of the 110.700 term is different in CITZAF
                     // (which
                     // is right!!!!!)
                  ((((((((((41.792e-6 * w) + 110.700e-6) * w) + 120.050e-6) * w) - 75.733e-6) * w) + 19.184e-6) * w) + 5.4000e-6) * z * z * z) + (((((((((((-42.445e-8 * w) + 117.750e-8) * w) - 136.060e-8) * w) + 88.128e-8) * w) - 21.645e-8) * w) - 5.725e-8) * z * z * z * z) + (((((((((((15.851e-10 * w) - 46.079e-10) * w) + 55.694e-10) * w) - 36.510e-10) * w) + 8.947e-10) * w) + 2.095e-10) * z * z * z * z * z);
            rAvg += comp.weightFraction(el, true) * rZ;
         }
         return rAvg;
      }
   }

   static public final BackscatterFactor Duncumb1981 = new DuncumbBackscatterFactor();

   /**
    * Yakowitz1973 - Simple expression used by early versions of the FRAME
    * application. This expression seems fine at low overvoltages but it is
    * completely nuts at higher overvoltages (u&gt;&gt;10). Yakowitz, Myklebust
    * &amp; Heinrich, FRAME: An On-Line Correction Procedure for Quantitative
    * Electron Probe Micronanalysis, NBS Tech Note 796, 1973
    * 
    * @deprecated Too limited to be of broad utility
    */
   @Deprecated
   public static class YakowitzBackscatterFactor
      extends BackscatterFactor {

      YakowitzBackscatterFactor() {
         super("Yakowitz/FRAME 1973", LitReference.FrameBook);
      }

      @Override
      public String caveat(Composition comp, AtomicShell shell, double e0) {
         final double u = e0 / shell.getEdgeEnergy();
         final NumberFormat df = new HalfUpFormat("0.0");
         if(u > 10)
            return CaveatBase.format(this, "This algorithm works very poorly at overvoltages above 10. (u=" + df.format(u)
                  + ")");
         if(u > 5)
            return CaveatBase.format(this, "This algorithm has problems with large overvoltages. (u=" + df.format(u) + ")");
         return super.caveat(comp, shell, e0);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         // Implementation taken from Myklebust &amp; Newbury in Electron Probe
         // Quantification
         // Compares equivalent to CITZAF implementation
         final double u = e0 / shell.getEdgeEnergy();
         final double a = (((((0.00873 * u) - 0.1669) * u) + 0.9662) * u) + 0.4523;
         final double b = (((((0.002703 * u) - 0.05182) * u) + 0.302) * u) - 0.1836;
         final double d = ((0.887 - (3.44 / u)) + (9.33 / (u * u))) - (6.43 / (u * u * u));
         double rAvg = 0.0;
         for(final Element el : comp.getElementSet()) {
            final double z = el.getAtomicNumber();
            final double rZ = a - (b * Math.log((d * z) + 25));
            rAvg += comp.weightFraction(el, true) * rZ;
         }
         return rAvg;
      }
   }

   @Deprecated
   static public final BackscatterFactor Yakowitz1973 = new YakowitzBackscatterFactor();

   /**
    * Myklebust1984 - Simple express taken used by later versions of the FRAME
    * application. J. de Physique 45, suppliment 2, 1984, C2-41 (Produces
    * similar numbers as CITZAF.)
    */
   static public class Myklebust84BackscatterFactor
      extends BackscatterFactor {
      Myklebust84BackscatterFactor() {
         super("Myklebust/FRAME 1984", new LitReference.CrudeReference("J. de Physique 45, suppliment 2, 1984, C2-41"));
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         // Implementation taken from Myklebust &amp; Newbury in Electron Probe
         // Quantification
         final double u = e0 / shell.getEdgeEnergy();
         double rAvg = 0.0;
         e0 = FromSI.keV(e0);
         for(final Element el : comp.getElementSet()) {
            final double z = el.getAtomicNumber();
            final double rZ = (1.0 - (0.0081512 * z)) + (3.613e-5 * z * z) + (0.009582 * z * Math.exp(-u)) + (0.00114 * e0);
            rAvg += comp.weightFraction(el, true) * rZ;
         }
         return rAvg;
      }
   }

   static public final BackscatterFactor Myklebust1984 = new Myklebust84BackscatterFactor();

   /**
    * Love1978 - Love &amp; Scott, Evaluation of a new correction procedure for
    * quantitative electron probe microanalysis, J. Phys. D. 11, 1978 p 1369
    * (Produces similar but slightly different numbers from CITZAF)
    */
   public static class LoveBackscatterFactor
      extends BackscatterFactor {
      LoveBackscatterFactor() {
         super("Love & Scott 1978", LitReference.LoveScott1978);
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(BackscatterCoefficient.class, BackscatterCoefficient.Love1978);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         // Implementation taken from CITZAF
         // Scott &amp; Love's Quant. Elect. Probe Micro.
         final BackscatterCoefficient bc = (BackscatterCoefficient) getAlgorithm(BackscatterCoefficient.class);
         final double u = e0 / shell.getEdgeEnergy();
         final double v = Math.log(u);
         final double eta = bc.compute(comp, e0);
         final double gU = (v * (2.87898 + (v * (-1.51307 + (v * (0.81313 + (v * -0.08241))))))) / u;
         final double iU = v * (0.33148 + (v * (0.05596 + (v * (-0.06339 + (v * 0.00947))))));
         return 1.0 - (eta * Math.pow(iU + (eta * gU), 1.67));
      }
   };

   static public final BackscatterFactor Love1978 = new LoveBackscatterFactor();

   /**
    * Czyzewski1982 - Czyzewski &amp; Szymanski, Proc. 10th Int. Cong. Electron
    * Microscopy, Vol 1, Offizon Paul Hartung, Hamburg, 1982, p 261
    * 
    * @deprecated Doesn't seem to work
    */
   @Deprecated
   static public class CzyzewskiBackscatterFactor
      extends BackscatterFactor {
      CzyzewskiBackscatterFactor() {
         super("Czyzewski & Szymanksi 1982", new LitReference.CrudeReference("Czyzewski & Szymanski, Proc. 10th Int. Cong. Electron Microscopy, Vol 1, Offizon Paul Hartung, Hamburg, 1982, p 261"));
      }

      @Override
      public String caveat(Composition comp, AtomicShell shell, double e0) {
         return CaveatBase.format(this, CaveatBase.Broken);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         if(false) {
            // Implementation taken from Myklebust &amp; Newbury in Electron
            // Probe
            // Quantification
            final double[][] aa = new double[][] {
               {
                  0.0,
                  0.0,
                  -0.5531081141e-5,
                  0.5955796251e-7,
                  -0.3210316856e-9
               },
               {
                  0.3401533559e-1,
                  -0.1601761397e-3,
                  0.2473523226e-5,
                  -0.3020861042e-7
               },
               {
                  0.9916651666e-1,
                  -0.4615018255 - 3,
                  -0.4332933627e-6
               },
               {
                  1.0300997920e-1,
                  -0.3113053618e-1
               },
               {
                  0.3630169747e-1
               }
            };
            final double invUm1 = (shell.getEdgeEnergy() / e0) - 1.0;
            double rAvg = 0.0;
            for(final Element el : comp.getElementSet()) {
               final double z = el.getAtomicNumber();
               double rZ = 1.0;
               for(int jj = 1; jj <= 5; ++jj)
                  for(int ii = 1; ii <= jj; ++ii)
                     rZ += aa[ii - 1][jj - ii] * Math.pow(invUm1, ii) * Math.pow(z, ((jj - ii) + 1));
               rAvg += comp.weightFraction(el, true) * rZ;
            }
            return rAvg;
         } else
            throw new UnsupportedOperationException("Czyzewski1982 almost works :^)");
      }
   };

   @Deprecated
   static public final BackscatterFactor Czyzewski1982 = new CzyzewskiBackscatterFactor();

   static public final BackscatterFactor Default = Pouchou1991;

   static private final AlgorithmClass[] mAllImplementations = {
      // BackscatterFactor.Czyzewski1982, // Broken!!
      BackscatterFactor.Duncumb1981,
      // BackscatterFactor.Love1978, // Broken!!!
      BackscatterFactor.Myklebust1984,
      // BackscatterFactor.Myklebust1991, // Not yet implemented
      BackscatterFactor.Pouchou1991,
         // BackscatterFactor.Yakowitz1973 // Too limited to be generally useful
   };
}

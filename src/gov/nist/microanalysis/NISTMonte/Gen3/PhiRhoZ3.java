package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.CharacteristicXRay;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.XRay;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Accumulates the generated and emitted &phi;(&rho;z) curve for a the specified
 * XRayTransport object. To use this construct a MonteCarloSS object, attach an
 * XRayTransport, then create and attach a PhiRhoZ object to the XRayTransport
 * instance. A single instance of PhiRhoZ will collect data on all the
 * transitions excited in the sample.
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
final public class PhiRhoZ3 implements ActionListener {

   final double mMinZ;
   final double mMaxZ;
   final int mNBins;
   private int mElectronCount;

   private class TransitionDatum {
      final double[] mGenerated;
      final double[] mEmitted;
      double mNorm;

      private TransitionDatum(int nBins) {
         mGenerated = new double[nBins];
         mEmitted = new double[nBins];
         mNorm = Double.NaN;
      }

      private void compute(XRay xrt, int bin) throws EPQException {
         mEmitted[bin] += xrt.getIntensity();
         mGenerated[bin] += xrt.getGenerated();
         if (Double.isNaN(mNorm) && (xrt.getGenerated() > 0.0)) {
            final Electron e = mEventListener.mMonte.getElectron();
            final MonteCarloSS.RegionBase r = e.getCurrentRegion();
            if ((r != null) && (r.getMaterial().getDensity() > ToSI.gPerCC(0.001)))
               mNorm = (binWidth() * xrt.getGenerated()) / Math2.distance(e.getPrevPosition(), e.getPosition());
         }

      }

      private double normalize(double v) {
         final double n = mNorm * mElectronCount;
         return !(Double.isNaN(n) || (n == 0.0)) ? v / n : 0.0;
      }

      private double getGenerated(int bin) {
         return normalize(mGenerated[bin]);
      }

      private double getEmitted(int bin) {
         return normalize(mEmitted[bin]);
      }

      private double getGeneratedIntensity(int bin) {
         return mElectronCount > 0 ? mGenerated[bin] / (mElectronCount * binWidth()) : 0.0;
      }

      private double getEmittedIntensity(int bin) {
         return mElectronCount > 0 ? mEmitted[bin] / (mElectronCount * binWidth()) : 0.0;
      }
   }

   final TreeMap<XRayTransition, TransitionDatum> mTransitionData;
   final XRayTransport3 mEventListener;

   /**
    * Constructs an object for tabulating all the &phi;(&rho; z) curves
    * represented by the specified XRayTransport3. Note it is tabulated in z
    * (not &rho; z) because this is the only consistent way with materials of
    * varying densities.
    * 
    * @param xrel
    * @param minZ
    * @param maxZ
    * @param nBins
    * @throws EPQException
    */
   public PhiRhoZ3(XRayTransport3 xrel, double minZ, double maxZ, int nBins) throws EPQException {
      mEventListener = xrel;
      mMinZ = minZ;
      mMaxZ = maxZ;
      mNBins = nBins;
      mTransitionData = new TreeMap<XRayTransition, TransitionDatum>();
      mElectronCount = 0;
   }

   /**
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      assert ae.getSource() == mEventListener;
      switch (ae.getID()) {
         case BaseXRayGeneration3.XRayGeneration : {
            try {
               for (int i = mEventListener.getEventCount() - 1; i >= 0; i--) {
                  final XRayTransport3.XRay xrt = mEventListener.getXRay(i);
                  if (xrt instanceof CharacteristicXRay) {
                     final XRayTransition tr = ((CharacteristicXRay) xrt).getTransition();
                     if (xrt != null) {
                        TransitionDatum td = mTransitionData.get(tr);
                        if (td == null) {
                           td = new TransitionDatum(mNBins);
                           mTransitionData.put(tr, td);
                        }
                        final double[] pos = xrt.getGenerationPos();
                        final int z = (int) ((pos[2] - mMinZ) / binWidth());
                        if ((z >= 0) && (z < mNBins))
                           td.compute(xrt, z);
                     }
                  }
               }
            } catch (final EPQException e) {
               e.printStackTrace();
            }
         }
            break;
         case MonteCarloSS.TrajectoryStartEvent :
            ++mElectronCount;
            break;
         case MonteCarloSS.FirstTrajectoryEvent :
            mElectronCount = 0;
            mTransitionData.clear();
            break;
      }
   }

   final public int binCount() {
      return mNBins;
   }

   public String binName(int z) {
      final double min = (z * binWidth()) + mMinZ;
      final double max = ((z + 1) * binWidth()) + mMinZ;
      final NumberFormat nf = new HalfUpFormat("0.000");
      return nf.format(min / 1.0e-6) + "\t" + nf.format(max / 1.0e-6);
   }

   public void write(PrintWriter wr) {
      final NumberFormat nf = new HalfUpFormat("0.000");
      wr.print("Min\tMax");
      final TreeSet<XRayTransition> xrts = new TreeSet<XRayTransition>(mTransitionData.keySet());
      for (final XRayTransition xrt : xrts) {
         wr.print("\tGen[" + xrt.toString() + "]");
         wr.print("\tEmit[" + xrt.toString() + "]");
      }
      for (final XRayTransition xrt : xrts) {
         wr.print("\tGen_I[" + xrt.toString() + "]");
         wr.print("\tEmit_I[" + xrt.toString() + "]");
      }
      wr.println();
      wr.print("\t");
      for (@SuppressWarnings("unused")
      final XRayTransition xrt : xrts) {
         wr.print("\tNorm");
         wr.print("\tNorm");
      }
      for (@SuppressWarnings("unused")
      final XRayTransition xrt : xrts) {
         wr.print("\t1/(meter e-)");
         wr.print("\t1/(meter e-)");
      }
      wr.println();
      wr.flush();
      for (int bin = 0; bin < mNBins; ++bin) {
         wr.print(binName(bin));
         for (final XRayTransition xrt : xrts) {
            final TransitionDatum td = mTransitionData.get(xrt);
            wr.print("\t");
            wr.print(nf.format(td.getGenerated(bin)));
            wr.print("\t");
            wr.print(nf.format(td.getEmitted(bin)));
         }
         for (final XRayTransition xrt : xrts) {
            final TransitionDatum td = mTransitionData.get(xrt);
            wr.print("\t");
            wr.print(nf.format(td.getGeneratedIntensity(bin)));
            wr.print("\t");
            wr.print(nf.format(td.getEmittedIntensity(bin)));
         }
         wr.println();
         wr.flush();
      }
   }

   private TransitionDatum find(XRayTransition xrt) {
      return mTransitionData.get(xrt);
   }

   final public double binWidth() {
      return (mMaxZ - mMinZ) / mNBins;
   }

   public double[] getGenerated(XRayTransition xrt) {
      final TransitionDatum td = find(xrt);
      return td != null ? Math2.divide(td.mGenerated, td.mNorm * mElectronCount) : new double[mNBins];
   }

   public double[] getEmitted(XRayTransition xrt) {
      final TransitionDatum td = find(xrt);
      return td != null ? Math2.divide(td.mEmitted, td.mNorm * mElectronCount) : new double[mNBins];
   }

   public double[] getGeneratedIntensity(XRayTransition xrt) {
      final TransitionDatum td = find(xrt);
      return td != null ? Math2.divide(td.mGenerated, mElectronCount * binWidth()) : new double[mNBins];
   }

   public double[] getEmittedIntensity(XRayTransition xrt) {
      final TransitionDatum td = find(xrt);
      return td != null ? Math2.divide(td.mEmitted, mElectronCount * binWidth()) : new double[mNBins];
   }

   public void write(Writer wr) {
      write(new PrintWriter(wr));
   }
}

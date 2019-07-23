package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A square electron detector divided into a grid like a chess board. The
 * detector is designed to record the position of passage of electrons from high
 * Z to lower Z through a Z-plane.
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
public class GridDetector
   implements ActionListener {
   /**
    * The coordinate of the corner of the detector grid.
    */
   final double[] mCorner;
   /**
    * The full dimension of an edge of the detector. The edges of the detector
    * are assumed to be parallel to the X and Y axes.
    */
   final double mSize;
   /**
    * The location in which electrons are counted.
    */
   final int[][] mBins;

   /**
    * The total number of electron trajectories.
    */
   transient int mTotal;

   /**
    * Constructs a square GridDetector with an edge dimension given by size (in
    * meters) divided into nBins equal sized bins.
    * 
    * @param center double[3] coordinate of center in meters
    * @param size double dimension of detector edge in meters
    * @param nBins int number of bins (rounded up to odd)
    */
   public GridDetector(double[] center, double size, int nBins) {
      mCorner = new double[] {
         center[0] - (0.5 * size),
         center[1] - (0.5 * size),
         center[2]
      };
      mSize = size;
      nBins = (2 * (nBins / 2)) + 1; // Round up to next odd
      mBins = new int[nBins][nBins];
      mTotal = 0;
   }

   private int[] bindex(double[] p0, double[] p1) {
      final double fy = (mCorner[2] - p0[2]) / (p1[2] - p0[2]);
      // Does this trajectory pass through the Z plane from high Z side?
      if((fy >= 0.0) && (fy < 1.0) && (mCorner[2] < p0[2])) {
         final double px = (((p0[0] * (1.0 - fy)) + (p1[0] * fy)) - mCorner[0]) / mSize;
         final double py = (((p0[1] * (1.0 - fy)) + (p1[1] * fy)) - mCorner[1]) / mSize;
         // Does this trajectory pass through the detector?
         if((px >= 0) && (px < 1.0) && (py >= 0) && (py < 1.0))
            return new int[] {
               (int) (px * mBins.length),
               (int) (py * mBins[0].length)
            };
      }
      return null;
   }

   /**
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      switch(ae.getID()) {
      // case MonteCarloSS.ScatterEvent:
      // case MonteCarloSS.NonScatterEvent:
         case MonteCarloSS.BackscatterEvent: {
            final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
            final Electron el = mcss.getElectron();
            final int[] idx = bindex(el.getPrevPosition(), el.getPosition());
            if(idx != null)
               ++mBins[idx[0]][idx[1]];
            break;
         }
         case MonteCarloSS.TrajectoryStartEvent:
            ++mTotal;
            break;
      }
   }

   /**
    * dump - Writes a summary of the results collected by this detector.
    * 
    * @param wr
    * @throws IOException
    */
   public void dump(Writer wr)
         throws IOException {
      final NumberFormat df = new HalfUpFormat("0.000E0");
      wr.append("Grid detector - " + Integer.toString(mTotal) + "\n");
      wr.append("Center = (" + df.format(1000.0 * (mCorner[0] + (0.5 * mSize))) + ","
            + df.format(1000.0 * (mCorner[1] + (0.5 * mSize))) + "," + df.format(1000.0 * mCorner[2]) + ")\n");
      wr.append("Total electon trajectories = " + Integer.toString(mTotal) + "\n");
      // Row of X indices
      wr.append("Index\t");
      for(int dx = 0; dx < mBins.length; ++dx) {
         wr.append("\t");
         wr.append(Integer.toString(dx));
      }
      wr.append("\n");
      // Row of X offsets
      wr.append("Offset (µm)\t");
      for(int dx = 0; dx < mBins.length; ++dx) {
         wr.append("\t");
         wr.append(df.format(((mSize * ((dx - (mBins.length / 2)) + 0.5)) / mBins.length) * 1.0e6));
      }
      wr.append("\n");
      for(int dy = 0; dy < mBins[0].length; ++dy) {
         wr.append(Integer.toString(dy));
         wr.append("\t");
         wr.append(df.format(((mSize * ((dy - (mBins[0].length / 2)) + 0.5)) / mBins[1].length) * 1.0e6));
         for(final int[] mBin : mBins) {
            wr.append("\t");
            wr.append(Integer.toString(mBin[dy]));
         }
         wr.append("\n");
      }
   }
}

/**
 * gov.nist.microanalysis.EPQTools.RippleSpectrum Created by: nritchie Date: May
 * 15, 2008
 */
package gov.nist.microanalysis.EPQTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.MapImage;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.VectorSet;
import gov.nist.microanalysis.EPQLibrary.VectorSet.Vector;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A class for accessing the spectra stored in a Ripple/Raw file as
 * ISpectrumData objects.
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
public class RippleSpectrum
   extends
   BaseSpectrum {

   private final RippleFile mRipple;
   private final SpectrumProperties mProperties;
   private final double mBaseLiveTime;
   private int mChannelCount;
   // Coordinates of the upper left corner

   transient private int mDepth;
   transient private int mRow;
   transient private int mCol;
   transient private int mBrukerOffset;
   // Permit averaging together blocks of adjacent pixels
   transient private int mRowSpan;
   transient private int mColSpan;
   transient private double[] mData;

   /**
    * Constructs a RippleSpectrum object and loads the first spectrum.
    * 
    * @throws Exception
    * @throws IOException
    * @throws FileNotFoundException
    */
   public RippleSpectrum(String filename, SpectrumProperties sp)
         throws FileNotFoundException,
         IOException,
         EPQException,
         Exception {
      mRipple = new RippleFile(filename, true);
      mProperties = new SpectrumProperties();
      mProperties.addAll(sp);
      mProperties.setTextProperty(SpectrumProperties.SourceFile, filename);
      mBaseLiveTime = mProperties.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN);
      setEnergyScale(sp.getNumericProperty(SpectrumProperties.EnergyOffset), sp.getNumericProperty(SpectrumProperties.EnergyScale));
      final File file = new File(filename);
      SpectrumUtils.rename(this, file.getName());
      mRow = -1;
      mRowSpan = 1;
      mColSpan = 1;
      mDepth = mRipple.getDepth();
      mChannelCount = mDepth;
      setPosition(0, 0);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return mChannelCount;
   }

   /**
    * Returns the currently selected row.
    * 
    * @return int
    */
   public int getRow() {
      return mRow;
   }

   public void setChannelCount(int chCount) {
      mChannelCount = chCount;
   }

   /**
    * Returns the currently selected column.
    * 
    * @return int
    */
   public int getColumn() {
      return mCol;
   }

   /**
    * Returns the number of rows in the ripple file
    * 
    * @return int
    */
   public int getRows() {
      return mRipple.getHeight();
   }

   /**
    * Returns the number of columns in the ripple file
    * 
    * @return int
    */
   public int getColumns() {
      return mRipple.getWidth();
   }

   /**
    * Make the active spectrum in this RippleSpectrum object the specified x and
    * y coordinate.
    * 
    * @param x
    * @param y
    * @throws IOException
    */
   public void setPosition(int x, int y)
         throws IOException {
      seek(y, x);
   }

   public void seek(int row, int col)
         throws IOException {
      if((mRow != row) || (mCol != col)) {
         mRow = row;
         mCol = col;
         updateData();
      }
   }

   private void updateData()
         throws IOException {
      mData = new double[mDepth];
      final int maxRow = Math.min(mRow + mRowSpan, getRows());
      final int maxCol = Math.min(mCol + mColSpan, getColumns());
      for(int r = mRow; r < maxRow; ++r)
         for(int c = mCol; c < maxCol; ++c) {
            mRipple.seek(r, c);
            Math2.plusEquals(mData, mRipple.readDouble(mData.length));
         }
      mProperties.setTextProperty(SpectrumProperties.SampleId, toString() + "[[" + Integer.toString(mRow) + ", "
            + Integer.toString(maxRow) + "),[" + Integer.toString(mCol) + ", " + Integer.toString(maxCol) + ")]");
      if(!Double.isNaN(mBaseLiveTime))
         mProperties.setNumericProperty(SpectrumProperties.LiveTime, (maxRow - mRow) * (maxCol - mCol) * mBaseLiveTime);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      i -= mBrukerOffset;
      return (i >= 0) && (i < mDepth) ? mData[i] : 0.0;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   public void setSpan(int rowSpan, int colSpan)
         throws IOException {
      final int newRowSpan = Math.max(1, rowSpan);
      final int newColSpan = Math.max(1, colSpan);
      if((newRowSpan != mRowSpan) || (newColSpan != mColSpan)) {
         mRowSpan = newRowSpan;
         mColSpan = newColSpan;
         updateData();
      }
   }

   /**
    * <p>
    * Corrects the offset of the spectrum to account for Bruker's habit of
    * sometimes writing 95 channels before zero and sometimes writing 47
    * channels. If the offset evidenced by the position of the zero strobe peak
    * is at the 95th channel then the normal -475 eV for 5 eV/ch and -950 eV for
    * 10 eV/ch will work ok. Sometimes Bruker writes the zero strobe peak at the
    * 47th channel in which case it is necessary to offset the spectrum by 95-47
    * channels (setBrukerOffset(95-47)).
    * </p>
    * <p>
    * This function can also be used in a similar manner for addressing other
    * vendor's poor design decisions.
    * </p>
    * 
    * @param off The number of channels to offset the data
    */
   public void setBrukerOffset(int off) {
      mBrukerOffset = off;
   }

   public MapImage process(VectorSet vecs, int binSize)
         throws IOException {
      Vector[] vs = vecs.getVectors().toArray(new Vector[vecs.getVectors().size()]);
      RegionOfInterest[] rois = new RegionOfInterest[vs.length];
      for(int i = 0; i < vs.length; ++i)
         rois[i] = vs[i].getROI();
      final int ww = getColumns(), hh = getRows();
      // System.out.println("Width=" + ww);
      // System.out.println("Height=" + hh);
      final MapImage mi = new MapImage(ww / binSize, hh / binSize, rois, vecs.toString(), MapImage.DataType.K_RATIOS);
      for(int r = 0; r < (hh - binSize + 1) / binSize; ++r) {
         for(int rr = r * binSize; rr < (r + 1) * binSize; ++rr) {
            for(int c = 0; c < (ww - binSize + 1) / binSize; ++c) {
               // build the sum spectrum
               double[] spec = new double[mData.length];
               for(int cc = c * binSize; cc < (c + 1) * binSize; ++cc) {
                  seek(rr, cc);
                  Math2.addInPlace(spec, this.mData);
               }
               for(int i = 0; i < vs.length; ++i)
                  mi.set(c, r, i, vs[i].apply(spec));
            }
         }
      }
      return mi;
   }
}

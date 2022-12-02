package gov.nist.microanalysis.EPQTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * <p>
 * A class designed to read EMISPEC file types
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

public class EMISPECFile extends BaseSpectrum {

   private class DimensionArrayFormat {

      // Member variables (only present to store data for possible use later)
      @SuppressWarnings("unused")
      private int mDimensionSize;
      @SuppressWarnings("unused")
      private String mDescription, mUnits;

      public DimensionArrayFormat() {

      }

      /**
       * readDimensionArrayFormat - reads the data in an emispec file pertaining
       * to the dimension array.
       * 
       * @param ledis
       *           LEDataInputStream - ALTERS, the emispec file being read.
       * @throws IOException
       */
      private void readDimensionArrayFormat(LEDataInputStream ledis) throws IOException {
         final StringBuffer DescriptionBuffer = new StringBuffer();
         final StringBuffer UnitsBuffer = new StringBuffer();
         int DescriptionLength, UnitsLength;

         mDimensionSize = ledis.readInt();
         final double offset = ledis.readDouble();
         final double scale = ledis.readDouble();
         setEnergyScale(offset, scale);
         ledis.readInt(); // mCalibrationElement

         DescriptionLength = ledis.readInt();
         for (int index = 0; index < DescriptionLength; index++)
            DescriptionBuffer.append((char) ledis.readByte());
         mDescription = DescriptionBuffer.toString();
         mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, mDescription);

         UnitsLength = ledis.readInt();
         for (int index = 0; index < UnitsLength; index++)
            UnitsBuffer.append((char) ledis.readByte());
         mUnits = UnitsBuffer.toString();
      }
   }

   private class DataElementFormat extends BaseSpectrum {

      private int mChannelCount;
      private short mDataType;
      private double[] mChannels;

      @Override
      public int getChannelCount() {
         return mChannelCount;
      }

      @Override
      public double getCounts(int i) {
         return mChannels[i];
      }

      @Override
      public SpectrumProperties getProperties() {
         return mProperties;
      }

      public DataElementFormat() {
         // Set the default acquisition time stamp to now...
         mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, new Date());
      }

      /**
       * readDataElementFormat - reads the data in an emispec file pertaining to
       * the individual data elements.
       * 
       * @param ledis
       *           LEDataInputStream - ALTERS, the emispec file being read.
       * @throws IOException
       */
      private void readDataElementFormat(LEDataInputStream ledis) throws IOException {
         if (mDataTypeID == OneDimensional) {
            final double zero = ledis.readDouble();
            final double width = ledis.readDouble();
            setEnergyScale(zero, width);
            ledis.readInt(); // mCalibrationElement
            mDataType = ledis.readShort();
            mChannelCount = ledis.readInt();
            mChannels = new double[mChannelCount];

            for (int index = 0; index < mChannelCount; index++)
               switch (mDataType) {
                  case UnsignedOneByteInteger :
                     mChannels[index] = ledis.readUnsignedByte();
                     break;
                  case UnsignedTwoByteInteger :
                     mChannels[index] = ledis.readUnsignedShort();
                     break;
                  case UnsignedFourByteInteger :
                     mChannels[index] = ledis.readInt();
                     break;
                  case SignedOneByteInteger :
                     mChannels[index] = ledis.readByte();
                     break;
                  case SignedTwoByteInteger :
                     mChannels[index] = ledis.readShort();
                     break;
                  case SignedFourByteInteger :
                     mChannels[index] = ledis.readInt();
                     break;
                  case FourByteFloat :
                     mChannels[index] = ledis.readFloat();
                     break;
                  case EightByteFloat :
                     mChannels[index] = ledis.readDouble();
                     break;
                  case EightByteComplex :
                     throw new IOException("Unsupported data type: 8-byte complex.");
                  case SixteenByteComplex :
                     throw new IOException("Unsupported data type: 16-byte complex.");
                  default :
                     throw new IOException("Unrecognized data type.");
               }
         } else if (mDataTypeID == TwoDimensional)
            throw new IOException("Two Dimensional Data is not supported.");
      }
   }

   private class DataTagFormat {
      @SuppressWarnings("unused")
      private float mTime;
      @SuppressWarnings("unused")
      private double mPositionX, mPositionY;

      /**
       * readDataTagFormat - Reads the information about the time and/or
       * position when the data was taken.
       * 
       * @param ledis
       *           LEDataInputStream - ALTERS, the EMISPEC file being read.
       * @throws IOException
       */
      private void readDataTagFormat(LEDataInputStream ledis) throws IOException {
         if (mTagTypeID == TagIsTimeOnly) {
            if (ledis.readShort() != TagIsTimeOnly)
               throw new IOException("Conflicting Data Tag formats");
            else
               mTime = ledis.readFloat();
         } else if (mTagTypeID == TagIs2DwithTime) {
            if (ledis.readShort() != TagIs2DwithTime)
               throw new IOException("Conflicting Data Tag formats");
            else {
               mTime = ledis.readFloat();
               mPositionX = ledis.readDouble();
               mPositionY = ledis.readDouble();
            }
         } else
            throw new IOException("Unrecognized Data Tag");
      }

      public DataTagFormat() {

      }
   }

   /****************************************************************************
    * START OF MAIN CLASS *
    ***************************************************************************/
   /* Constants */
   // Verification values for correct file format
   private static final short LittleEndianCode = 0x4949;
   private static final short FileIDCode = 0x197;
   private static final short VersionNumber = 0x210;

   // Valid values for the DataTypeID
   private static final int OneDimensional = 0x4120;
   private static final int TwoDimensional = 0x4122;

   // Valid values for The TagTypeID
   private static final int TagIsTimeOnly = 0x4152;
   private static final int TagIs2DwithTime = 0x4142;

   // Data Type values
   private static final int UnsignedOneByteInteger = 1;
   private static final int UnsignedTwoByteInteger = 2;
   private static final int UnsignedFourByteInteger = 3;
   private static final int SignedOneByteInteger = 4;
   private static final int SignedTwoByteInteger = 5;
   private static final int SignedFourByteInteger = 6;
   private static final int FourByteFloat = 7;
   private static final int EightByteFloat = 8;
   private static final int EightByteComplex = 9;
   private static final int SixteenByteComplex = 10;

   /* Other member variables */
   private int mDataTypeID;
   private int mTagTypeID;
   private int mTotalNumberElements;
   private int mValidNumberElements;
   // private int mOffsetArrayOffset;
   private int mNumberDimensions;
   // private int mCalibrationElement;

   private DimensionArrayFormat[] DimensionArray;
   private DataElementFormat[] DataArray;
   private DataTagFormat[] DataTagArray;
   private int mChannel_Count;
   private double[] mSummed_Channel_Array;
   private final SpectrumProperties mProperties = new SpectrumProperties();
   private String mFileName;

   @Override
   public int getChannelCount() {
      return mChannel_Count;
   }

   @Override
   public double getCounts(int i) {
      return mSummed_Channel_Array[i];
   }

   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   /**
    * setFilename - set the name of the file associated with this spectrum.
    * 
    * @param fn
    *           String
    */
   public void setFilename(String fn) {
      mFileName = fn;
   }

   /**
    * isInstanceOf - Does the specified input stream represent an instance of an
    * EMISPEC file?
    * 
    * @param is
    *           InputStream
    * @return boolean
    */
   public static boolean isInstanceOf(InputStream is) {
      boolean res = false;
      try {
         try {
            try (final LEDataInputStream ledis = new LEDataInputStream(is)) {
               res = (ledis.readShort() == LittleEndianCode);
               if (res)
                  res = (ledis.readShort() == FileIDCode);
               if (res)
                  res = (ledis.readShort() == VersionNumber);
            }
         } finally {
            is.close(); // force closure to ensure it is not reused...
         }
      } catch (final IOException ex) {
         res = false;
      }
      return res;
   }

   /**
    * read - Reads a standard EMISPEC file and stores the spectrum data. Reads
    * six parts: (1) the header (2) the dimension array (3) the data offset
    * array (4) the tag offset array (5) the spectrum data (6) the data tag.
    * 
    * @param is
    *           File - PRESERVES, an input stream linked to the EMISPEC file
    *           that is to be read.
    * @throws IOException
    */
   public void read(InputStream is) throws IOException {
      LEDataInputStream ledis = new LEDataInputStream(is);
      int[] mData_Offset_Array, mTag_Offset_Array;
      mFileName = "Unknown";
      readHeaderFormat(ledis);

      DimensionArray = new DimensionArrayFormat[mNumberDimensions];
      for (int index = 0; index < mNumberDimensions; index++) {
         DimensionArray[index] = new DimensionArrayFormat();
         DimensionArray[index].readDimensionArrayFormat(ledis);
      }
      mData_Offset_Array = new int[mTotalNumberElements];
      mTag_Offset_Array = new int[mTotalNumberElements];

      readDataOffsetArrayFormat(ledis, mData_Offset_Array);
      readTagOffsetArrayFormat(ledis, mTag_Offset_Array);
      DataArray = new DataElementFormat[mTotalNumberElements];
      DataTagArray = new DataTagFormat[mTotalNumberElements];

      for (int index = 0; index < mTotalNumberElements; index++) {
         // Because reset is not supported, I have to close the data file and
         // reopen to reset it
         ledis.close();
         is.reset();
         ledis = new LEDataInputStream(is);

         ledis.skipBytes(mData_Offset_Array[index]);
         // Initialize the individual element
         DataArray[index] = new DataElementFormat();
         DataArray[index].readDataElementFormat(ledis);
      }
      mChannel_Count = DataArray[DataArray.length - 1].getChannelCount();
      sumMyChannelArray();
      for (int index = 0; index < mTag_Offset_Array.length; index++) {
         ledis.close();
         is.reset();
         ledis = new LEDataInputStream(is);

         ledis.skipBytes(mTag_Offset_Array[index]);

         DataTagArray[index] = new DataTagFormat();
         DataTagArray[index].readDataTagFormat(ledis);
      }
      ledis.close();
   }

   /**
    * readHeaderFormat - Reads the header information of an EMISPEC file.
    * 
    * @param ledis
    *           LEDataInputStream - ALTERS, the EMISPEC file that is being read.
    * @throws IOException
    */
   private void readHeaderFormat(LEDataInputStream ledis) throws IOException {
      if (ledis.readShort() != LittleEndianCode)
         throw new IOException("This file does not appear to be a valid EMISPEC file.");

      if (ledis.readShort() != FileIDCode)
         throw new IOException("This EMISPEC's file ID id not recognized as valid.");

      if (ledis.readShort() != VersionNumber)
         throw new IOException("This EMISPEC file is not the correct version.");

      mDataTypeID = ledis.readInt();
      if (!((mDataTypeID != OneDimensional) || (mDataTypeID != TwoDimensional)))
         throw new IOException("The Data Type ID for this EMISPEC file is not valid.");

      mTagTypeID = ledis.readInt();
      if (!((mTagTypeID != TagIsTimeOnly) || (mTagTypeID != TagIs2DwithTime)))
         throw new IOException("The Tag Type ID for this EMISPEC file is not valid");

      mTotalNumberElements = ledis.readInt();
      mValidNumberElements = ledis.readInt();
      if (mValidNumberElements > mTotalNumberElements)
         throw new IOException("# of valid elements greater than # of elements in array");

      ledis.readInt(); // mOffsetArrayOffset
      mNumberDimensions = ledis.readInt();
   }

   /**
    * readDataOffsetArrayFormat - Reads all the offset values corresponding to
    * each of the data points in the EMISPEC file.
    * 
    * @param ledis
    *           LEDataInputStream - ALTERS, the EMISPEC file being read.
    * @param My_Data_Offset_Array
    *           int[] - PRODUCES, holds the values of the data offset given by
    *           the file.
    * @throws IOException
    */
   private void readDataOffsetArrayFormat(LEDataInputStream ledis, int[] My_Data_Offset_Array) throws IOException {
      for (int index = 0; index < mTotalNumberElements; index++)
         My_Data_Offset_Array[index] = ledis.readInt();
   }

   /**
    * readTagOffsetArrayFormat - Reads all the tag offset values corresponding
    * to the tag information in the EMISPEC file.
    * 
    * @param ledis
    *           LEDataInputStream - ALTERS, the EMISPEC file being read.
    * @param My_Tag_Offset_Array
    *           int[] - PRODUCES, holds the values of the tag offset given by
    *           the file.
    * @throws IOException
    */
   private void readTagOffsetArrayFormat(LEDataInputStream ledis, int[] My_Tag_Offset_Array) throws IOException {
      for (int index = 0; index < mTotalNumberElements; index++)
         My_Tag_Offset_Array[index] = ledis.readInt();
   }

   /**
    * sumMyChannelArray - Takes the values from the data elements and sums them
    * together to create one spectrum.
    */
   private void sumMyChannelArray() {
      mSummed_Channel_Array = new double[mChannel_Count];
      for (int index = 0; index < mChannel_Count; index++)
         for (final DataElementFormat element : DataArray)
            mSummed_Channel_Array[index] = mSummed_Channel_Array[index] + element.getCounts(index);
   }

   /**
    * toString - overloads the default toString() operation. Returns the
    * filename of the EMISPEC file.
    * 
    * @return String
    */
   @Override
   public String toString() {
      return mFileName;
   }

   public EMISPECFile() {
      super();
   }

   public EMISPECFile(InputStream is) throws IOException {
      super();
      read(is);
   }

}

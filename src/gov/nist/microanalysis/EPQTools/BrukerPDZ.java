package gov.nist.microanalysis.EPQTools;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.TimeZone;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * Reads the Bruker file format associated with hand-held XRF units.
 * Reverse engineered by inspection of the binary file format.
 * 
 * @author nritchie
 *
 */
public class BrukerPDZ extends BaseSpectrum {
	
	private SpectrumProperties mProperties = new SpectrumProperties();
	private int[] mCounts;
	
	private final short readShort(DataInputStream dis) throws IOException {
		return ByteBuffer.allocate(Short.BYTES)
	            .order(ByteOrder.BIG_ENDIAN).putShort(dis.readShort())
	            .order(ByteOrder.LITTLE_ENDIAN).getShort(0);
	}
	
	private final int readInt(DataInputStream dis) throws IOException {
		return ByteBuffer.allocate(Integer.BYTES)
	            .order(ByteOrder.BIG_ENDIAN).putInt(dis.readInt())
	            .order(ByteOrder.LITTLE_ENDIAN).getInt(0);
	}
	
	private final double readDouble(DataInputStream dis) throws IOException {
		return ByteBuffer.allocate(Double.BYTES)
	            .order(ByteOrder.BIG_ENDIAN).putDouble(dis.readDouble())
	            .order(ByteOrder.LITTLE_ENDIAN).getDouble(0);
	}
	
	private final double readFloat(DataInputStream dis) throws IOException {
		return ByteBuffer.allocate(Float.BYTES)
	            .order(ByteOrder.BIG_ENDIAN).putFloat(dis.readFloat())
	            .order(ByteOrder.LITTLE_ENDIAN).getFloat(0);
	}
	
	BrukerPDZ(InputStream ins) throws IOException{
		DataInputStream dis = new DataInputStream(ins);
		dis.skip(6); // 6:
		final int nCh=readShort(dis);
		dis.skip(50-(6+Short.BYTES)); // 50:
		final double eVperCh = readDouble(dis);
		dis.skip(114 - (50+Double.BYTES));
		StringBuffer filter = new StringBuffer();
		for(int i=0;i<4;++i) {
			final int z = readShort(dis);
			final int th = readShort(dis);
			if(z>0) {
				if(i>0)
					filter.append(",");
				filter.append("("+Element.byAtomicNumber(z).toAbbrev()+","+th+".0e-6)");
			}
		}
		dis.skip(146 - (114+8*Short.BYTES));
		final int yr = readShort(dis);
		final int mon = readShort(dis);
		final int tz = readShort(dis);
		final int day = readShort(dis);
		final int hr = readShort(dis);
		final int min = readShort(dis);
		final int sec = readShort(dis);
		dis.skip(162 - (146 + 7*Short.BYTES));
		final double e0 = readFloat(dis);
		final double pc = readFloat(dis);
		dis.skip(342 - (162 + 2*Float.BYTES));
		final double rt = readFloat(dis);
		@SuppressWarnings("unused")
		final double dt = readFloat(dis);
		@SuppressWarnings("unused")
		final double xx = readFloat(dis);
		final double lt = readFloat(dis);
		dis.skip(358 - (342+4*Float.BYTES));
		mCounts = new int[nCh];
		for(int i=0;i<nCh;++i)
			mCounts[i]=readInt(dis);
		SpectrumProperties props = mProperties;
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"+(tz>=0?"+":"")+Integer.toString(tz)+":00"));
		c.set(yr,mon-1,day,hr,min,sec);
		props.setTimestampProperty(SpectrumProperties.AcquisitionTime, c.getTime());
		props.setNumericProperty(SpectrumProperties.LiveTime, lt);
		props.setNumericProperty(SpectrumProperties.RealTime, rt);
		props.setNumericProperty(SpectrumProperties.XRFSourceVoltage, e0);
		props.setNumericProperty(SpectrumProperties.XRFTubeCurrent, pc);
		props.setTextProperty(SpectrumProperties.XRFFilter, filter.toString());
		props.setNumericProperty(SpectrumProperties.EnergyOffset, 0.0);
		props.setNumericProperty(SpectrumProperties.EnergyScale, eVperCh);
	}

	@Override
	public int getChannelCount() {
		return mCounts.length;
	}

	@Override
	public double getCounts(int i) {
		return mCounts[i];
	}

	@Override
	public SpectrumProperties getProperties() {
		return mProperties;
	}
	
	public static boolean isInstanceOf(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		boolean res;
		try {
			try {
				res = (dis.readByte()==1) && (dis.readByte()==1) && (dis.readByte()==23) && (dis.readByte()==0);
			}
			finally {
				is.close();
			}
		} catch (IOException e) {
			res = false;
		}
		return res;
	}


}

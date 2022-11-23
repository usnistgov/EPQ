package gov.nist.microanalysis.EPQTools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

public class OxfordSPTFile  extends BaseSpectrum {

	private final SpectrumProperties mProperties = new SpectrumProperties();
	private double[] mData;

	public static boolean isInstanceOf(InputStream is) {
		final InputStreamReader isr = new InputStreamReader(is);
		final BufferedReader br = new BufferedReader(isr);
		String first;
		try {
			first = br.readLine();
			if ((first == null) || (!first.startsWith("﻿")))
				return false;
			final String second = br.readLine();
			return second != null && first.startsWith("﻿Acquired: ") && second.startsWith("Collimator: ");
		} catch (final IOException e) {
			return false;
		}
	}

	public OxfordSPTFile(String filename, boolean raw) throws FileNotFoundException, IOException, EPQException {
		this(new FileInputStream(filename), raw);
	}

	public OxfordSPTFile(InputStream is, boolean raw) throws IOException, EPQException {
		mProperties.setNumericProperty(SpectrumProperties.EnergyOffset, 0.0);
		mProperties.setNumericProperty(SpectrumProperties.EnergyScale, 10.0);

		final InputStreamReader isr = new InputStreamReader(is);
		final BufferedReader br = new BufferedReader(isr);
		String line = br.readLine();
		if((line!=null) && line.startsWith("﻿")) {
			line = line.substring(3);
		}
		double liveTime=Double.NaN, deadTime = Double.NaN;
		while(line!=null) {
			line=line.trim();
			if(line.length()==0)
				break;
			try {
			String[] items = line.split("\\:\\s");
			if(items.length>=2) {
				if(items[0].equals("Acquired")) {
					try {
						SimpleDateFormat df = new SimpleDateFormat("MM/dd/YYYY hh:mm:ss a"); // 12/21/2020 1:14:28 PM
						Calendar cal = new GregorianCalendar();
						cal.setTime(df.parse(items[1]));
						mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, cal.getTime());
					} catch (ParseException e) {
						// Ignore
					}					
				} else if(items[0].equals("Collimator")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Secondary Filter")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Time")){
					liveTime=Double.parseDouble(items[1]);
					mProperties.setNumericProperty(SpectrumProperties.LiveTime, liveTime);
					if(!Double.isNaN(deadTime))
						mProperties.setNumericProperty(SpectrumProperties.RealTime, liveTime*(1.0+deadTime));
				} else if(items[0].equals("Dead Time")){
					final String[] ss = items[1].split("\\%");
					deadTime = 0.01*Double.parseDouble(ss[0]);
					if(!Double.isNaN(liveTime))
						mProperties.setNumericProperty(SpectrumProperties.RealTime, liveTime*(1.0+deadTime));
				} else if(items[0].equals("BinsToProcess")){
					mData=new double[Integer.parseInt(items[1])];
				} else if(items[0].equals("Center")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("FWHM")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Correction Slope")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Correction Offset")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Shift Slope")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Shift Offset")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Tube Voltage")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Tube Current")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Process Time")){
					System.out.println(items[0]+": "+items[1]);
				} else if(items[0].equals("Incident Angle, rads")){
					System.out.println(items[0]+": "+items[1]);
				} else
					System.out.println(items[0]+": "+items[1]);
			}
			}
			catch(Throwable th) {
				
			}
			line = br.readLine();
		}
		line = br.readLine();
		if((line!=null) && line.equals("Raw\tCorrected")) {
			try {
				int i = 0;
				for (String str = br.readLine(); (str!=null) && (str.length() > 0)
						&& (i < mData.length); str = br.readLine()) {
					String[] items = str.split("\\t");
					if (items.length > 1)
						mData[i] = Double.parseDouble(items[raw?0:1]);
					++i;
				}
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}
		
		

	@Override
	public int getChannelCount() {
		return mData.length;
	}

	@Override
	public double getCounts(int i) {
		return mData[i];
	}

	@Override
	public SpectrumProperties getProperties() {
		return mProperties;
	}
}

package gov.nist.microanalysis.EPQTools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;

/**
 * <p>
 * An implementation of the ISpectrumData interface for spectra loaded from the
 * standard ASPEX TIFF image/spectrum file.
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

public class ASPEXSpectrum extends BaseSpectrum {

	// ASPEX custom tag types
	public static final short SPECTRAL_DATA = (short) 0x8352;
	public static final short SPECTRAL_XRES = (short) 0x8353;
	public static final short SPECTRAL_XOFF = (short) 0x8354;
	public static final short SPECTRAL_YRES = (short) 0x8355;
	public static final short SPECTRAL_YOFF = (short) 0x8356;

	public static final short IMAGE_DESCRIPTION = 270; // ASCII
	public static final short SOFTWARE = 305; // ASCII

	/*
	 * Calibration offset accounts for the Perception Consoles autocalibration
	 * mechanism's habit of calibrating one channel too high
	 */
	private double[] mChannels;
	private final Map<Element, Number> mComp = new TreeMap<Element, Number>();
	private final SpectrumProperties mProperties = new SpectrumProperties();

	private transient StageCoordinate mStagePosition;

	// A list of known tags that are currently ignored
	private static String[] mIgnoredList = { "dead_percent", "type4et", "composition", "peak_label", "pixel_size",
			"display_mag", "afa", "field#", "magfield#", "x_abs", "y_abs", "x_cg", "y_cg", "x_feret", "y_feret", "dmax",
			"dmin", "dperp", "aspect", "area", "perimeter", "orientation", "mag_index", "action", "first_elem",
			"second_elem", "third_elem", "fourth_elem", "first_conc", "second_conc", "third_conc", "fourth_conc",
			"first_pct", "second_pct", "third_pct", "fourth_pct", "video", "counts", "type(4et)#", "density",
			"psem_class", "sample_description", "sample_group", "instrument", "prepmethod_name", "prepsample_name",
			"analysis_name", "rasterbox", "about", "location0", "user_masthead", "void_area", "void_count",
			"edge_roughness", "rms_video", "roundness", "formfactor", "ecd", "skel", "hull_area", "hull_perim",
			"part_attrib", "x_dac", "y_dac" };

	private final transient NumberFormat mParser = NumberFormat.getInstance(Locale.US);
	private static String notAnASPEX = "This file does not appear to be an ASPEX-style spectrum file.";
	private static final double CALIBRATION_OFFSET = 0.0; // -10.0;

	private List<TIFFImageFileDir> mImages;

	public ASPEXSpectrum(File file, boolean withImage) throws EPQException, FileNotFoundException, IOException {
		this(file);
		if (withImage) {
			final double mag = mProperties.getNumericWithDefault(SpectrumProperties.Magnification, Double.NaN);
			double microFov = Double.NaN, macroFov = Double.NaN;
			if (!Double.isNaN(mag)) {
				final double zoom = mProperties.getNumericWithDefault(SpectrumProperties.MagnificationZoom, 1.0);
				macroFov = (1.0e-6 * (3.5 * 25.4 * 1000.0)) / mag;
				microFov = macroFov / zoom; // meters
			}
			final StageCoordinate stgPos = (StageCoordinate) mProperties
					.getObjectWithDefault(SpectrumProperties.StagePosition, null);
			if (!Double.isNaN(mag)) {
				final Iterator<ImageReader> irs = ImageIO.getImageReadersByFormatName("tiff");
				final ImageReader ir = irs.next();
				try (final FileImageInputStream fiis = new FileImageInputStream(file)) {
					ir.setInput(fiis);
					final int nImgs = ir.getNumImages(true);
					if (nImgs > 0) {
						final BufferedImage bi = ir.read(0);
						final ScaledImage scImg = new ScaledImage(bi, microFov,
								(microFov * bi.getHeight()) / bi.getWidth(), stgPos, "SE");
						getProperties().setImageProperty(SpectrumProperties.MicroImage, scImg);
					}
					if (nImgs > 1) {
						final BufferedImage bi = ir.read(1);
						final ScaledImage scImg = new ScaledImage(bi, macroFov,
								(macroFov * bi.getHeight()) / bi.getWidth(), stgPos, "BSE");
						getProperties().setImageProperty(SpectrumProperties.MacroImage, scImg);
					}
				}
			}
		}
	}

	public ASPEXSpectrum(File file) throws EPQException {
		super();
		{
			try {
				mImages = TIFFImageFileDir.readIFD(file);
				setEnergyScale(CALIBRATION_OFFSET, 10.0);
				final TIFFImageFileDir dir = mImages.get(0);
				double yRes = 1.0, yOff = 0.0;
				{
					TIFFImageFileDir.Field field = dir.getField(SPECTRAL_XRES);
					if (field != null)
						try {
							setEnergyScale(getZeroOffset(), mParser.parse(field.getAsString()).doubleValue());
						} catch (final ParseException ex) {
							// ignore
						}
					field = dir.getField(SPECTRAL_XOFF);
					if (field != null)
						try {
							setEnergyScale(mParser.parse(field.getAsString()).doubleValue() + CALIBRATION_OFFSET,
									getChannelWidth());
						} catch (final ParseException ex) {
							// ignore
						}
					field = dir.getField(SPECTRAL_YRES);
					if (field != null)
						try {
							yRes = mParser.parse(field.getAsString()).doubleValue();
						} catch (final ParseException ex) {
							// ignore
						}
					field = dir.getField(SPECTRAL_YOFF);
					if (field != null)
						try {
							yOff = mParser.parse(field.getAsString()).doubleValue();
						} catch (final ParseException ex) {
							// ignore
						}
				}
				{ // Get channel data...
					final TIFFImageFileDir.Field field = dir.getField(SPECTRAL_DATA);
					if (field == null)
						throw new EPQException(notAnASPEX);
					mChannels = field.getAsDoubleArray();
					for (int i = mChannels.length - 1; i >= 0; --i)
						mChannels[i] = (yRes * mChannels[i]) + yOff;
				}
				{ // Load properties
					final TIFFImageFileDir.Field field = dir.getField(IMAGE_DESCRIPTION);
					if (field != null)
						parseImageDescription(field.getAsString());
				}
				{ // Get software tag
					final TIFFImageFileDir.Field field = dir.getField(SOFTWARE);
					if (field != null)
						getProperties().setTextProperty(SpectrumProperties.Software, field.getAsString());
				}
				SpectrumUtils.rename(this, file.getName());
				getProperties().setTextProperty(SpectrumProperties.SourceFile, file.getCanonicalPath());
			} catch (final Throwable ex1) {
				System.err.println("Throwable: " + ex1.toString());
				throw new EPQException(ex1);
			}
		}
	}

	private void parseImageDescription(String imgDesc) {
		final String[] items = imgDesc.split("\n");
		mStagePosition = new StageCoordinate();
		for (final String item : items)
			parseImageDescItem(item);
		final SpectrumProperties props = getProperties();
		if (mComp.size() > 0) {
			final Composition comp = new Composition();
			final Element[] elms = new Element[mComp.size()];
			final double[] mf = new double[mComp.size()];
			int j = 0;
			for (final Element elm : mComp.keySet()) {
				elms[j] = elm;
				mf[j] = mComp.get(elm).doubleValue();
				++j;
			}
			comp.defineByMoleFraction(elms, mf);
			comp.setOptimalRepresentation(Composition.Representation.WEIGHT_PCT);
			if (comp.getElementCount() == 1)
				comp.setName("Pure " + comp.getElementSet().iterator().next().toAbbrev());
			props.setCompositionProperty(SpectrumProperties.StandardComposition, comp);
		}
		if (!getProperties().isDefined(SpectrumProperties.Elevation))
			getProperties().setNumericProperty(SpectrumProperties.Elevation, 37.0);
		getProperties().setObjectProperty(SpectrumProperties.StagePosition, mStagePosition);
		String comment = getProperties().getTextWithDefault(SpectrumProperties.SpectrumComment, null);
		if ((comment != null) && (comment.length() > 1))
			SpectrumUtils.rename(this, comment);
		mStagePosition = null;
	}

	private double parseDuration(String dur) {
		int st = dur.lastIndexOf(':');
		double res = 0.0;
		try {
			if (st == -1)
				res = mParser.parse(dur).doubleValue();
			else { // '##:##:##"
				res = mParser.parse(dur.substring(st + 1)).doubleValue();
				final int end = st;
				st = dur.lastIndexOf(':', end - 1);
				if (st == -1)
					st = 0;
				res += 60.0 * mParser.parse(dur.substring(st + 1, end)).doubleValue();
				if (st == 0)
					return res;
				res += 3600.0 * mParser.parse(dur.substring(0, st)).doubleValue();
			}
		} catch (final Exception ex) {
			System.err.println("Error parsing: " + dur);
		}
		return res;
	}

	private void parseImageDescItem(String idi) {
		final int eq = idi.indexOf('=');
		if (eq != -1)
			try {
				final String tag = ((idi.substring(0, eq)).trim()).toLowerCase(Locale.US);
				final String val = idi.substring(eq + 1).trim();
				final SpectrumProperties sp = getProperties();
				if (tag.equals("live_time"))
					sp.setNumericProperty(SpectrumProperties.LiveTime, parseDuration(val));
				else if (tag.equals("acquisition_time"))
					sp.setNumericProperty(SpectrumProperties.RealTime, parseDuration(val));
				else if (tag.equals("beam_current") || tag.equals("probe_current"))
					sp.setNumericProperty(SpectrumProperties.ProbeCurrent, mParser.parse(val).doubleValue());
				else if (tag.equals("element_percent")) {
					final int comma = val.indexOf(',');
					if (comma != -1) {
						final Element el = Element.byName(val.substring(0, comma).trim());
						final Number pct = mParser.parse(val.substring(comma + 1).trim());
						if (el.isValid())
							mComp.put(el, pct);
					}
				} else if (tag.equals("mag"))
					sp.setNumericProperty(SpectrumProperties.Magnification, mParser.parse(val).doubleValue());
				else if (tag.equals("zoom"))
					sp.setNumericProperty(SpectrumProperties.MagnificationZoom, mParser.parse(val).doubleValue());
				else if (tag.equals("stage_x"))
					mStagePosition.set(Axis.X, mParser.parse(val).doubleValue());
				else if (tag.equals("stage_y"))
					mStagePosition.set(Axis.Y, mParser.parse(val).doubleValue());
				else if (tag.equals("stage_z"))
					mStagePosition.set(Axis.Z, mParser.parse(val).doubleValue());
				else if (tag.equals("stage_r"))
					mStagePosition.set(Axis.R, mParser.parse(val).doubleValue());
				else if (tag.equals("stage_t"))
					mStagePosition.set(Axis.T, mParser.parse(val).doubleValue());
				else if (tag.equals("stage_b"))
					mStagePosition.set(Axis.B, mParser.parse(val).doubleValue());
				else if (tag.equals("spot_size"))
					sp.setNumericProperty(SpectrumProperties.SpotSize, mParser.parse(val).doubleValue());
				else if (tag.equals("accelerating_voltage"))
					sp.setNumericProperty(SpectrumProperties.BeamEnergy, mParser.parse(val).doubleValue());
				else if (tag.equals("beam_energy"))
					sp.setNumericProperty(SpectrumProperties.BeamEnergy, mParser.parse(val).doubleValue());
				else if (tag.equals("working_distance"))
					sp.setNumericProperty(SpectrumProperties.WorkingDistance, mParser.parse(val).doubleValue());
				else if (tag.equals("analysis_date")) { // in format "#0/#0/0000"
					int st = 0, end = val.indexOf('/');
					int mon, day, year, hour, min, sec;
					if (end == -1)
						throw new ParseException("Error parsing date: ", st);
					mon = Integer.parseInt(val.substring(st, end));
					st = end + 1;
					end = val.indexOf('/', st);
					if (end == -1)
						throw new ParseException("Error parsing date: ", st);
					day = Integer.parseInt(val.substring(st, end));
					st = end + 1;
					year = Integer.parseInt(val.substring(st));
					if (end == -1)
						throw new ParseException("Error parsing date: ", st);
					if (year < 100)
						year += (year < 80 ? 1900 : 2000);
					final Calendar c = Calendar.getInstance();
					// Restore analysis_time if available
					c.setTime(sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new Date(0)));
					hour = c.get(Calendar.HOUR_OF_DAY);
					min = c.get(Calendar.MINUTE);
					sec = c.get(Calendar.SECOND);
					c.set(year, mon - 1, day, hour, min, sec);
					sp.setTimestampProperty(SpectrumProperties.AcquisitionTime, c.getTime());
				} else if (tag.equals("analysis_time")) { // in format "#0:00:00
					// AM|PM"
					int st = 0, end = val.indexOf(":");
					int mon, day, year, hour, min, sec;
					if (end == -1)
						throw new ParseException("Error parsing time: ", st);
					hour = Integer.parseInt(val.substring(st, end));
					if (val.toUpperCase(Locale.US).indexOf("PM") != -1)
						hour += 12;
					st = end + 1;
					end = val.indexOf(':', st);
					if (end == -1)
						throw new ParseException("Error parsing time: ", st);
					min = Integer.parseInt(val.substring(st, end).trim());
					sec = Integer.parseInt(val.substring(end + 1, end + 3).trim());

					final Calendar c = Calendar.getInstance();
					// Restore analysis_time if available
					c.setTime(sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new Date(0)));
					mon = c.get(Calendar.MONTH);
					day = c.get(Calendar.DAY_OF_MONTH);
					year = c.get(Calendar.YEAR);
					c.set(year, mon, day, hour, min, sec);
					sp.setTimestampProperty(SpectrumProperties.AcquisitionTime, c.getTime());
				} else if (tag.equals("sample_number")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.SampleId, val);
				} else if (tag.equals("client_number")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.ClientsSampleID, val);
				} else if (tag.equals("client_name")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.ClientName, val);
				} else if (tag.equals("project_number")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.ProjectName, val);
				} else if (tag.equals("comment")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.SpectrumComment, val);
				} else if (tag.equals("caption")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.SpecimenDesc,
								val + " " + sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, ""));
				} else if (tag.equals("beam_x"))
					sp.setNumericProperty(SpectrumProperties.BeamSpotX, mParser.parse(val).doubleValue());
				else if (tag.equals("beam_y"))
					sp.setNumericProperty(SpectrumProperties.BeamSpotY, mParser.parse(val).doubleValue());
				else if (tag.equals("operator")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.InstrumentOperator, val);
				} else if (tag.equals("description")) {
					if (val.length() > 0)
						sp.setTextProperty(SpectrumProperties.SpecimenDesc,
								val + " " + sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, ""));
				} else if (tag.equals("part#")) {
					if (val.length() > 0) {
						final String def = sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, "");
						sp.setTextProperty(SpectrumProperties.SpecimenDesc, def + " #[" + val + "]");
					}
				} else if (tag.equals("dave"))
					sp.setNumericProperty(SpectrumProperties.AFA_DAvg, mParser.parse(val).doubleValue());
				else if (tag.equals("take_off_angle") || tag.equals("detector_tilt")) {
					// The take-off angle has always been spurious (57.0 when it
					// actually is 37.0)
					final double toa = mParser.parse(val).doubleValue();
					sp.setNumericProperty(SpectrumProperties.Elevation, toa == 57.0 ? 37.0 : toa);
				} else {
					boolean found = false;
					for (final String element : mIgnoredList)
						if (element.equals(tag)) {
							found = true;
							break;
						}
					if (!found)
						System.err.println("Unknown tag: " + tag + "=" + val);
				}
			} catch (final Exception ex) {
				System.err.println(ex.toString());
			}
	}

	/**
	 * getChannelCount
	 * 
	 * @return int
	 */
	@Override
	public int getChannelCount() {
		return mChannels.length;
	}

	/**
	 * getCounts
	 * 
	 * @param i int
	 * @return double
	 */
	@Override
	public double getCounts(int i) {
		return mChannels[i];
	}

	/**
	 * getProperties
	 * 
	 * @return SpectrumProperties
	 */
	@Override
	public SpectrumProperties getProperties() {
		return mProperties;
	}

	/**
	 * isInstanceOf - Does this InputStream look like it is likely to be an
	 * ASPEXSpectrum
	 * 
	 * @param is
	 * @return boolean
	 */
	public static boolean isInstanceOf(InputStream is) {
		boolean res;
		try {
			final byte[] b = new byte[4];
			is.read(b, 0, b.length);
			is.close(); // force closure to ensure it is not reused...
			res = Arrays.equals(b, TIFFImageFileDir.LITTLE_MAGIC);
		} catch (final IOException ex) {
			res = false;
		}
		return res;
	}

	static public String readImageDescription(File file, int imageIndex) throws EPQException, IOException {
		try (final FileInputStream fis = new FileInputStream(file)) {
			final FileChannel fc = fis.getChannel();
			final ByteBuffer bb = fc.map(MapMode.READ_ONLY, 0, fc.size());
			{
				final byte[] b = new byte[4];
				bb.get(b);
				if (Arrays.equals(b, TIFFImageFileDir.LITTLE_MAGIC))
					bb.order(ByteOrder.LITTLE_ENDIAN);
				// else if(Arrays.equals(b, TIFFImageFileDir.BIG_MAGIC))
				// bb.order(ByteOrder.BIG_ENDIAN);
				else
					throw new EPQException(notAnASPEX);
			}
			final int offset = bb.getInt();
			TIFFImageFileDir ifd = null;
			for (int i = 0; (i <= imageIndex) && (offset != 0); ++i)
				ifd = new TIFFImageFileDir(bb, offset);
			final TIFFImageFileDir.Field tf = ifd.getField(IMAGE_DESCRIPTION);
			return tf != null ? tf.getAsString() : "";
		}
	}

}

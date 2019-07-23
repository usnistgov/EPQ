package gov.nist.microanalysis.EPQTools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

public class BrukerSPX
   extends
   BaseSpectrum {

   transient ArrayList<String> tCurrentPath = null;

   private final SpectrumProperties mProperties = new SpectrumProperties();
   private double[] mData;

   static abstract private class BaseParser {
      private final SpectrumProperties.PropertyId mPid;

      protected BaseParser(SpectrumProperties.PropertyId pid) {
         mPid = pid;
      }

      private SpectrumProperties.PropertyId getPID() {
         return mPid;
      }

      abstract public Object parse(String str);
   }

   static private class Parser
      extends
      BaseParser {
      private final double mScale;

      Parser(SpectrumProperties.PropertyId pid, double scale) {
         super(pid);
         mScale = scale;
      }

      @Override
      public Object parse(String str) {
         return Double.valueOf(mScale * Double.parseDouble(str.trim()));
      }
   }

   static private class StringParser
      extends
      BaseParser {

      StringParser(SpectrumProperties.PropertyId pid) {
         super(pid);
      }

      @Override
      public Object parse(String str) {
         return str;
      }
   }

   static private class DateParser
      extends
      BaseParser {
      DateParser(SpectrumProperties.PropertyId pid) {
         super(pid);
      }

      @Override
      public Object parse(String str) {
         final Calendar c = Calendar.getInstance();
         c.set(2010, 11, 29);
         // assume MM:DD:YY
         final String[] item = str.split("\\x2E");
         final int month = Integer.parseInt(item[1]);
         final int day = Integer.parseInt(item[0]);
         final int year = Integer.parseInt(item[2]);
         c.clear();
         c.set(year, month - 1, day);
         return c.getTime();
      }
   }

   static private class TimeParser
      extends
      BaseParser {
      TimeParser(SpectrumProperties.PropertyId pid) {
         super(pid);
      }

      @Override
      public Object parse(String str) {
         final String[] item = str.split(":");
         final int hours = Integer.parseInt(item[0]);
         final int minutes = Integer.parseInt(item[1]);
         final int seconds = Integer.parseInt(item[2]);
         final Calendar c = Calendar.getInstance();
         c.clear();
         c.set(1970, 0, 1, hours, minutes, seconds);
         return c.getTime();
      }
   }

   static final private Map<String, BaseParser> mParsers = initSpectrumProperties();

   static private Map<String, BaseParser> initSpectrumProperties() {
      final HashMap<String, BaseParser> res = new HashMap<String, BaseParser>();
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/RealTime", new Parser(SpectrumProperties.RealTime, 0.001));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/LifeTime", new Parser(SpectrumProperties.LiveTime, 0.001));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/DeadTime", new Parser(SpectrumProperties.DeadPercent, 1.0));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/PrimaryEnergy", new Parser(SpectrumProperties.BeamEnergy, 1.0));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/ElevationAngle", new Parser(SpectrumProperties.Elevation, 1.0));
      res.put("/TRTSpectrum/ClassInstance/ClassInstance/CalibAbs", new Parser(SpectrumProperties.EnergyOffset, 1000.0));
      res.put("/TRTSpectrum/ClassInstance/ClassInstance/CalibLin", new Parser(SpectrumProperties.EnergyScale, 1000.0));
      res.put("/TRTSpectrum/ClassInstance/ClassInstance/Date", new DateParser(SpectrumProperties.AcquisitionTime));
      res.put("/TRTSpectrum/ClassInstance/ClassInstance/Time", new TimeParser(SpectrumProperties.AcquisitionTime));
      res.put("/TRTSpectrum/ClassInstance/ChildClassInstances/ClassInstance/TRTChartConfigurationData/SeriesProperties/ClassInstance/Name", new StringParser(SpectrumProperties.SpectrumDisplayName));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/ShapingTime", new Parser(SpectrumProperties.PulseProcessTime, 1.0e-5));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/Type", new StringParser(SpectrumProperties.DetectorDescription));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/DetectorThickness", new Parser(SpectrumProperties.DetectorThickness, 1.0));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/SiDeadLayerThickness", new Parser(SpectrumProperties.DeadLayer, 1.0));
      res.put("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/WindowType", new StringParser(SpectrumProperties.WindowType));
      return res;
   }

   public static boolean isInstanceOf(InputStream is) {
      final InputStreamReader isr = new InputStreamReader(is);
      final BufferedReader br = new BufferedReader(isr);
      String first;
      try {
         first = br.readLine();
         final String second = br.readLine();
         return (second != null) && first.startsWith("<?xml") && second.startsWith("<TRTSpectrum>");
      }
      catch(final IOException e) {
         return false;
      }
   }

   public BrukerSPX(String filename)
         throws FileNotFoundException,
         XmlPullParserException,
         IOException,
         EPQException {
      this(new FileInputStream(filename));
   }

   public BrukerSPX(InputStream is)
         throws XmlPullParserException,
         IOException,
         EPQException {
      final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
      factory.setNamespaceAware(true);
      final XmlPullParser xpp = factory.newPullParser();
      xpp.setInput(new InputStreamReader(is));
      tCurrentPath = new ArrayList<String>();
      for(int eventType = xpp.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xpp.next())
         if(eventType == XmlPullParser.START_DOCUMENT) {
            // System.out.println("Start document");
         } else if(eventType == XmlPullParser.END_DOCUMENT) {
            // System.out.println("End document");
         } else if(eventType == XmlPullParser.START_TAG)
            processStartElement(xpp);
         else if(eventType == XmlPullParser.END_TAG)
            processEndElement(xpp);
         else if(eventType == XmlPullParser.TEXT)
            try {
               processText(xpp);
            }
            catch(final Exception e) {
               e.printStackTrace();
            }
   }

   private void processStartElement(XmlPullParser xpp) {
      tCurrentPath.add(xpp.getName());
      if(xpp.getName().startsWith("Layer")) {
         final String path = getXMPPath();
         if(path.startsWith("/TRTSpectrum/ClassInstance/TRTHeaderedClass/ClassInstance/WindowLayers/Layer")) {
            int z = -1;
            double th = 0.0, af = 1.0;
            for(int i = 0; i < xpp.getAttributeCount(); ++i) {
               final String name = xpp.getAttributeName(i);
               final String val = xpp.getAttributeValue(i).trim();
               if(name.equals("Atom"))
                  z = Integer.parseInt(val);
               else if(name.equals("Thickness"))
                  th = Double.parseDouble(val);
               else if(name.equals("RelativeArea"))
                  af = Double.parseDouble(val);
            }
            if((z > 0) && (th > 0.0))
               switch(z) {
                  case Element.elmB:
                     mProperties.setNumericProperty(SpectrumProperties.BoronLayer, 1000.0 * th);
                     break;
                  case Element.elmC:
                     mProperties.setNumericProperty(SpectrumProperties.CarbonLayer, 1000.0 * th);
                     break;
                  case Element.elmN:
                     mProperties.setNumericProperty(SpectrumProperties.NitrogenLayer, 1000.0 * th);
                     break;
                  case Element.elmO:
                     mProperties.setNumericProperty(SpectrumProperties.OxygenLayer, 1000.0 * th);
                     break;
                  case Element.elmAl:
                     mProperties.setNumericProperty(SpectrumProperties.AluminumLayer, 1000.0 * th);
                     break;
                  case Element.elmSi:
                     mProperties.setNumericProperty(SpectrumProperties.SupportGridThickness, 0.001 * th);
                     mProperties.setNumericProperty(SpectrumProperties.WindowOpenArea, 100.0 * (1.0 - af));
                     break;
                  case Element.elmNi:
                     mProperties.setNumericProperty(SpectrumProperties.NickelLayer, 1000.0 * th);
                     break;
                  case Element.elmAu:
                     mProperties.setNumericProperty(SpectrumProperties.GoldLayer, 1000.0 * th);
                     break;
                  default:
                     System.out.println("Unexpected element: " + Element.byAtomicNumber(z) + " in Bruker SPX window.");
                     break;
               }
         }
      }
   }

   private void processEndElement(XmlPullParser xpp)
         throws EPQException {
      final String name = xpp.getName();
      if(!tCurrentPath.get(tCurrentPath.size() - 1).equals(name))
         throw new EPQException("Structural error in SPX file.");
      tCurrentPath.remove(tCurrentPath.size() - 1);
   }

   public void processText(XmlPullParser xpp)
         throws XmlPullParserException {
      final String str = xpp.getText();
      final String path = getXMPPath();
      final BaseParser bp = mParsers.get(path);
      if(bp != null) {
         final Object res = bp.parse(str);
         if(res instanceof Double)
            mProperties.setNumericProperty(bp.getPID(), ((Double) res).doubleValue());
         else if(res instanceof Date) {
            final Date rd = (Date) res;
            final Calendar c = Calendar.getInstance();
            if(mProperties.isDefined(SpectrumProperties.AcquisitionTime)) {
               final Date dt = mProperties.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, null);
               long date = dt.getTime() + rd.getTime();
               date += TimeZone.getDefault().getOffset(date);
               c.setTimeInMillis(date);
            } else
               c.setTimeInMillis(rd.getTime());
            mProperties.setTimestampProperty(bp.getPID(), c.getTime());
         } else if(res instanceof String)
            mProperties.setTextProperty(bp.getPID(), res.toString());
      } else if(path.equals("/TRTSpectrum/ClassInstance/Channels")) {
         int begin, end = -1;
         for(int i = 0; i < mData.length; ++i) {
            begin = end + 1;
            end = str.indexOf(",", begin);
            if(end == -1) {
               mData[i] = Double.parseDouble(str.substring(begin, str.length()).trim());
               break;
            } else
               mData[i] = Double.parseDouble(str.substring(begin, end).trim());
         }
      } else if(path.equals("/TRTSpectrum/ClassInstance/ClassInstance/ChannelCount")) {
         final int chCount = Integer.parseInt(str.trim());
         mData = new double[chCount];
      }
   }

   private String getXMPPath() {
      final StringBuffer sb = new StringBuffer();
      for(final String str : tCurrentPath) {
         sb.append("/");
         sb.append(str);
      }
      return sb.toString();
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

package gov.nist.microanalysis.EPQTools;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.mapper.Mapper;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.SampleShape;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.StandardsDatabase2;
import gov.nist.microanalysis.EPQLibrary.VectorSet;
import gov.nist.microanalysis.EPQLibrary.VectorSet.Vector;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.EPQLibrary.Detector.BasicSiLiLineshape;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.SiLiCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.XRayWindow;
import gov.nist.microanalysis.EPQLibrary.Detector.XRayWindow2;
import gov.nist.microanalysis.Utility.UncertainValue;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * A specialization of XStream defining aliases for common EPQ classes.
 * 
 * @author nicholas
 */
@SuppressWarnings("deprecation")
public class EPQXStream
   extends
   XStream {

   private static EPQXStream mInstance = null;

   public static EPQXStream getInstance() {
      if(mInstance == null)
         mInstance = new EPQXStream();
      return mInstance;
   }

   private void init() {
      XStream.setupDefaultSecurity(this); // to be removed after 1.5
      allowTypesByWildcard(new String[] {
         "gov.nist.microanalysis.**",
         "gov.nist.nanoscalemetrology.**",
         "org.python.**",
         "Jama.**"
      });
      setMode(XStream.ID_REFERENCES);
      alias("AtomicShell", AtomicShell.class);
      alias("BasicSiLiLineshape", BasicSiLiLineshape.class);
      alias("Composition", Composition.class);
      alias("DetectorProperties", DetectorProperties.class);
      alias("Element", Element.class);
      alias("Material", Material.class);
      alias("RegionOfInterest", RegionOfInterest.class);
      alias("SiLiCalibration", SiLiCalibration.class);
      alias("SpectrumProperties", SpectrumProperties.class);
      alias("StandardDatabase2", StandardsDatabase2.class);
      alias("StandardBlock2", StandardsDatabase2.StandardBlock2.class);
      alias("UncertainValue", UncertainValue.class);
      alias("UncertainValue2", UncertainValue2.class);
      alias("Vector", Vector.class);
      alias("VectorSet", VectorSet.class);
      alias("XRayTransition", XRayTransition.class);
      alias("XRayTransitionSet", XRayTransitionSet.class);
      alias("XRayWindow", XRayWindow.class);
      alias("XRayWindow2", XRayWindow2.class);
      alias("ThinFilmSample", SampleShape.ThinFilm.class);
      alias("BulkSample", SampleShape.Bulk.class);
   }

   private EPQXStream() {
      super();
      init();
   }

   /**
    * @param reflectionProvider
    */
   public EPQXStream(ReflectionProvider reflectionProvider) {
      super(reflectionProvider);
      init();
   }

   /**
    * @param hierarchicalStreamDriver
    */
   public EPQXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
      super(hierarchicalStreamDriver);
      init();
   }

   /**
    * @param reflectionProvider @param hierarchicalStreamDriver
    */
   public EPQXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver hierarchicalStreamDriver) {
      super(reflectionProvider, hierarchicalStreamDriver);
      init();
   }

   /**
    * @param reflectionProvider @param driver @param classLoader
    */
   public EPQXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver, ClassLoader classLoader) {
      super(reflectionProvider, driver, classLoader);
      init();
   }

   /**
    * @param reflectionProvider @param driver @param classLoader @param mapper
    */
   public EPQXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver, ClassLoader classLoader, Mapper mapper) {
      super(reflectionProvider, driver, classLoader, mapper);
      init();
   }

   /**
    * @param reflectionProvider @param driver @param classLoader @param
    *           mapper @param converterLookup @param converterRegistry
    */
   public EPQXStream(ReflectionProvider reflectionProvider, HierarchicalStreamDriver driver, ClassLoader classLoader, Mapper mapper, ConverterLookup converterLookup, ConverterRegistry converterRegistry) {
      super(reflectionProvider, driver, classLoader, mapper, converterLookup, converterRegistry);
      init();
   }

   /**
    * Generates an MD5 hash for an object by converting it to XML using XStream
    * and then mixing the current time/date with the XML representation to
    * generate a likely unique MD5 hash code. The MD5 can be used as a UUID
    * (128-bit likely unique ID) @param obj @return A hash code as a
    * String @throws NoSuchAlgorithmException
    */
   public static String toMD5(Object obj)
         throws NoSuchAlgorithmException {
      final MessageDigest m = MessageDigest.getInstance("MD5");
      final EPQXStream xs = EPQXStream.getInstance();
      final String xml = xs.toXML(obj);
      final DateFormat df = DateFormat.getDateTimeInstance();
      m.update(df.format(new Date(System.currentTimeMillis())).getBytes());
      m.update(xml.getBytes());
      String res = new BigInteger(1, m.digest()).toString(16);
      if(res.length() < 32)
         res = "00000000000000000000000000000000".substring(0, 32 - res.length()) + res;
      assert res.length() == 32;
      return toCanonicalUUID(res);
   }

   public static String generateGUID(Object obj) {
      try {
         return toMD5(obj);
      }
      catch(final NoSuchAlgorithmException e) {
         final String hex = Long.toHexString(System.currentTimeMillis()) + Double.toHexString((new Random()).nextLong());
         return "fa116636-ac2b-2f0d-" + ("0000000000000000".substring(0, 16 - hex.length()) + hex);
      }
   }

   /**
    * Converts a uuid to PostgreSQL's canonical form
    * XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX where X are lower case hexidecimal
    * digits. @param uuid @return A canonical UUID as a string
    */
   static public String toCanonicalUUID(String uuid) {
      uuid = uuid.replace("-", "").toLowerCase();
      assert uuid.length() <= 32;
      while(uuid.length() < 32)
         uuid = "0" + uuid;
      return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20)
            + "-" + uuid.substring(20, 32);
   }
}

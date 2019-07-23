package gov.nist.microanalysis.EPQDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.SwingWorker;

import org.apache.derby.drda.NetworkServerControl;

import com.thoughtworks.xstream.converters.ConversionException;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ParticleSignature;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQLibrary.Detector.ElectronProbe;
import gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector;
import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.EPQTools.SpectrumFile;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.UncertainValue2;

public class Session {

   /**
    * <p>
    * A class that provides basic information about a spectrum from the database
    * record and facilitates loading the spectrum when required.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   public class SpectrumSummary
      implements
      Comparable<SpectrumSummary> {
      private final int mID;
      private final String mDisplayName;
      private final Timestamp mTimestamp;
      private ISpectrumData mSpectrum;

      private SpectrumSummary(int id, String dispName, Timestamp timestamp) {
         mID = id;
         mDisplayName = dispName;
         mTimestamp = timestamp;
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo(SpectrumSummary o) {
         int res = 0;
         if(mID != o.mID) {
            res = mTimestamp.compareTo(o.mTimestamp);
            if(res == 0)
               res = mDisplayName.compareTo(o.mDisplayName);
            if(res == 0)
               res = (mID < o.mID ? -1 : 1);
         }
         return res;
      }

      public ISpectrumData load()
            throws SQLException, IOException, EPQException {
         // synchronize it to facilitate use in threaded applications
         synchronized(this) {
            if(mSpectrum == null)
               mSpectrum = readSpectrum(mID);
            return mSpectrum;
         }
      }

      @Override
      public String toString() {
         return mDisplayName;
      }

      public Timestamp getTimestamp() {
         return mTimestamp;
      }
   }

   public class AlreadyInDatabaseException
      extends
      Exception {

      private static final long serialVersionUID = -297714303436448772L;

      private AlreadyInDatabaseException(String name) {
         super(name + " is already in " + mDbName + ".");
      }
   }

   public enum ElementDataTypes {
      STANDARD_COMPOSITION, MEASURED_COMPOSITION, UNCERTAINTY_MEASURE, PARTICLE_SIGNATURE
   }

   private final String EMBEDDED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
   private final String CLIENT_DRIVER = "org.apache.derby.jdbc.ClientDriver";
   private final String CONN_EMBEDDED_BASE = "jdbc:derby:";
   private final String CONN_CLIENT_BASE = "jdbc:derby://localhost:1527/";
   private final String mDbName;
   private Connection mConnection;
   private TreeMap<String, Integer> mPeople;
   private TreeMap<Integer, String> mInvPeople;
   private TreeMap<String, Integer> mProjects;
   private TreeMap<Integer, String> mInvProjects;
   private HashMap<ElectronProbe, Integer> mElectronProbes;
   private HashMap<DetectorProperties, Integer> mDetectors;
   private HashMap<DetectorProperties, Integer> mActiveDetectors;
   private final HashMap<DetectorProperties, HashMap<DetectorCalibration, Integer>> mCalibrations;
   static private final boolean ENABLE_LW = false;

   private boolean mIsNew;

   /**
    * Constructs a Session
    * 
    * @param dbName
    */
   public Session(String dbName) {
      super();
      mDbName = dbName;
      mCalibrations = new HashMap<DetectorProperties, HashMap<DetectorCalibration, Integer>>();
      NetworkServerControl nsc = null;
      boolean startServer = false;
      boolean client = true;
      try {
         nsc = new NetworkServerControl();
         nsc.ping();
      }
      catch(final Exception ex) {
         startServer = true;
      }
      if(startServer)
         try {
            nsc.start(null);
            Class.forName(CLIENT_DRIVER);
            client = true;
         }
         catch(final java.lang.ClassNotFoundException cnfe) {
            System.err.print("ClassNotFoundException: ");
            System.err.println(cnfe.getMessage());
            System.out.println("\n    >>> Please check your CLASSPATH variable   <<<\n");
         }
         catch(final Exception e) {
            try {
               Class.forName(EMBEDDED_DRIVER);
               client = false;
            }
            catch(final java.lang.ClassNotFoundException cnfe) {
               System.err.print("ClassNotFoundException: ");
               System.err.println(cnfe.getMessage());
               System.out.println("\n    >>> Please check your CLASSPATH variable   <<<\n");
            }
         }
      while(mConnection == null)
         // Create (if needed) and connect to the database
         try {
            mConnection = DriverManager.getConnection((client ? CONN_CLIENT_BASE : CONN_EMBEDDED_BASE) + dbName);
            mIsNew = false;
         }
         catch(final SQLException se) {
            final String theError = se.getSQLState();
            if(theError.equals("08001")) {
               // The client database has failed; try the embedded...
               client = false;
               continue;
            } else if(theError.equals("XJ040"))
               throw new EPQFatalException("Is your home directory on a network share and are you running another instance of Graf on another computer?");
            else if(theError.equals("XJ004") || theError.equals("08004")) {
               // Database does not yet exist...
               try {
                  final String connectStr = (client ? CONN_CLIENT_BASE : CONN_EMBEDDED_BASE) + dbName + ";create=true";
                  mConnection = DriverManager.getConnection(connectStr);
               }
               catch(final SQLException e) {
                  e.printStackTrace();
                  throw new EPQFatalException("Unable to create database.");
               }
               try {
                  createDatabase();
                  mIsNew = true;
               }
               catch(final EPQException epq) {
                  client = false;
                  epq.printStackTrace();
                  throw new EPQFatalException("Unable to populate database.");
               }
            } else if(theError.equals("XJ040"))
               throw new EPQFatalException("Unable to connect to the database.\nYour firewall may be blocking TCP/IP port 1527 thereby forcing "
                     + "this application to use the database in embedded mode.\nThe embedded database only permits one simultaneous connection.");
            else
               throw new EPQFatalException(se);
         }
      if(mConnection != null) {
         try {
            final PreparedStatement ps = mConnection.prepareStatement("SELECT TABLEID FROM SYS.SYSTABLES WHERE TABLENAME='QC_ITEM'");
            try (final ResultSet rs = ps.executeQuery()) {
               if(!rs.next())
                  buildQCTables();
            }
         }
         catch(final Exception e) {
            throw new EPQFatalException(e);
         }
         if(ENABLE_LW)
            try {
               final PreparedStatement ps = mConnection.prepareStatement("SELECT TABLEID FROM SYS.SYSTABLES WHERE TABLENAME='LINEWEIGHT_DATA'");
               try (final ResultSet rs = ps.executeQuery()) {
                  if(!rs.next())
                     buildLineweightTables();
               }
            }
            catch(final Exception e) {
               throw new EPQFatalException(e);
            }
      }
   }

   public void addDetector(IXRayDetector xrd) {
      final DetectorProperties dp = xrd.getDetectorProperties();
      if(!dp.getProperties().isDefined(SpectrumProperties.DetectorGUID))
         dp.getProperties().setTextProperty(SpectrumProperties.DetectorGUID, EPQXStream.generateGUID(dp));
      addElectronProbe(dp.getOwner());
      addCalibration(dp, xrd.getCalibration());
   }

   public void addDetector(DetectorProperties xrd) {
      if(!getDetectors().contains(xrd)) {
         final String sql = "INSERT INTO DETECTOR (ID, CREATED, NAME, INSTRUMENT_KEY, XML_OBJ) VALUES (DEFAULT, CURRENT_TIMESTAMP, ?, ?, ?)";
         try {
            final PreparedStatement ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            final EPQXStream xs = EPQXStream.getInstance();
            ps.setString(1, xrd.toString());
            Integer i = getElectronProbes().get(xrd.getOwner());
            if(i == null) {
               addElectronProbe(xrd.getOwner());
               i = getElectronProbes().get(xrd.getOwner());
            }
            ps.setInt(2, i.intValue());
            final String xml = xs.toXML(xrd);
            ps.setString(3, xml);
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               if(rs.next()) {
                  final Integer idx = Integer.valueOf(rs.getInt(1));
                  mDetectors.put(xrd, idx);
                  mActiveDetectors.put(xrd, idx);
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Is the detector identified by the detector properties object active (true)
    * or disabled (false)?
    * 
    * @param dp
    * @return true if dp represents an enabled set of DetectorProperties
    */
   public boolean isEnabled(DetectorProperties dp) {
      getElectronProbes(); // to ensure mDetectors != null
      return mActiveDetectors.get(dp) != null;
   }

   public void setEnabled(DetectorProperties dp, boolean enabled) {
      if(isEnabled(dp) != enabled) {
         final int idx = findDetector(dp);
         assert idx != -1;
         getElectronProbes(); // to ensure mDetectors != null
         final String sql = "UPDATE DETECTOR SET RETIRED=" + (enabled ? "NULL" : "CURRENT_TIMESTAMP") + " WHERE ID=?";
         try {
            assert enabled ^ mActiveDetectors.containsValue(Integer.valueOf(idx));
            final PreparedStatement ps = mConnection.prepareStatement(sql);
            ps.setInt(1, idx);
            ps.executeUpdate();
            if(enabled)
               mActiveDetectors.put(dp, Integer.valueOf(idx));
            else
               mActiveDetectors.remove(dp);
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
   }

   public void addCalibration(DetectorProperties xrd, DetectorCalibration calib) {
      {
         final SpectrumProperties cp = calib.getProperties();
         if(!cp.isDefined(SpectrumProperties.CalibrationGUID))
            cp.setTextProperty(SpectrumProperties.CalibrationGUID, EPQXStream.generateGUID(calib));
      }
      final Map<DetectorCalibration, Integer> mdci = getCalibrationsInt(xrd);
      if(!mdci.containsKey(calib)) {
         {
            final SpectrumProperties cp = calib.getProperties();
            if(!cp.isDefined(SpectrumProperties.CalibrationGUID))
               cp.setTextProperty(SpectrumProperties.CalibrationGUID, EPQXStream.generateGUID(calib));
         }
         final String sql = "INSERT INTO CALIBRATION (ID, DETECTOR, START_DATE, XML_OBJ) VALUES (DEFAULT, ?, ?, ?)";
         PreparedStatement ps;
         try {
            ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int xrdIdx = findDetector(xrd);
            if(xrdIdx < 0) {
               addDetector(xrd);
               xrdIdx = findDetector(xrd);
               assert xrdIdx >= 0;
            }
            ps.setInt(1, xrdIdx);
            ps.setDate(2, new java.sql.Date(calib.getActiveDate().getTime()));
            ps.setString(3, EPQXStream.getInstance().toXML(calib));
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               if(rs.next())
                  mdci.put(calib, Integer.valueOf(rs.getInt(1)));
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Updates the name and XML definition of the 'old' detector to reflect the
    * new defintion in xrd.
    * 
    * @param old The previous definition
    * @param xrd The new defintion
    */
   public void updateDetector(DetectorProperties old, DetectorProperties xrd) {
      final String sql = "UPDATE DETECTOR SET NAME=?, XML_OBJ=? WHERE ID=?";
      try {
         final PreparedStatement ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
         final EPQXStream xs = EPQXStream.getInstance();
         final int oldIdx = findDetector(old);
         if(oldIdx >= 0) {
            if(!xrd.getProperties().isDefined(SpectrumProperties.DetectorGUID))
               xrd.getProperties().setTextProperty(SpectrumProperties.DetectorGUID, EPQXStream.generateGUID(xrd));
            ps.setString(1, xrd.toString());
            ps.setString(2, xs.toXML(xrd));
            ps.setInt(3, oldIdx);
            ps.executeUpdate();
            mDetectors.remove(old);
            mDetectors.put(xrd, Integer.valueOf(oldIdx));
            if(mActiveDetectors.containsKey(old)) {
               mActiveDetectors.remove(old);
               mActiveDetectors.put(xrd, Integer.valueOf(oldIdx));
            }
         } else
            addDetector(xrd);
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
   }

   public void updateInstrument(ElectronProbe ep) {
      final Integer idx = mElectronProbes.get(ep);
      if(idx != null)
         try {
            final String sql = "UPDATE ELECTRONPROBE SET NAME=?, XML_OBJ=? WHERE ID=?";
            final EPQXStream xs = EPQXStream.getInstance();
            final String xml = xs.toXML(ep.getProbeProperties());
            final PreparedStatement ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ep.toString());
            ps.setString(2, xml);
            ps.setInt(3, idx.intValue());
            ps.executeUpdate();
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      else
         addElectronProbe(ep);
   }

   private void addElectronProbe(ElectronProbe ep) {
      if(!getElectronProbes().containsKey(ep)) {
         final String sql = "INSERT INTO ELECTRONPROBE (ID, CREATED, NAME, XML_OBJ) VALUES (DEFAULT, CURRENT_TIMESTAMP, ?, ?)";
         try {
            final PreparedStatement ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            final EPQXStream xs = EPQXStream.getInstance();
            final String xml = xs.toXML(ep.getProbeProperties());
            ps.setString(1, ep.toString());
            ps.setString(2, xml);
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               if(rs.next()) {
                  getElectronProbes();
                  final int id = rs.getInt(1);
                  mElectronProbes.put(ep, Integer.valueOf(id));
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
   }

   private int addElementalDatum(Composition comp, Session.ElementDataTypes type, int uncertainty)
         throws SQLException {
      final Set<Element> elms = comp.getElementSet();
      final StringBuffer sql = new StringBuffer();
      final StringBuffer vals = new StringBuffer();
      sql.append("INSERT INTO ELEMENT_DATA (ID, COMP_TYPE, PRESENT, ENTERED_BY, UNCERTAINTY_KEY");
      vals.append(") VALUES (DEFAULT, ?, ?, ?, ?");
      for(int i = 0; (i < 5) && (i < elms.size()); ++i) {
         sql.append(", ELM");
         sql.append(i);
         vals.append(", ?");
      }
      for(final Element elm : elms) {
         sql.append(", ELM_");
         sql.append(elm.toAbbrev().toUpperCase());
         vals.append(", ?");
      }
      vals.append(" )");
      sql.append(vals);
      final PreparedStatement ps = mConnection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, type.ordinal());
      ps.setBytes(2, computePresent(elms));
      ps.setInt(3, findPerson(System.getProperty("user.name")));
      ps.setInt(4, uncertainty);
      int pos = 5;
      for(int i = 0; (i < 5) && (i < elms.size()); ++i, ++pos)
         ps.setInt(pos, comp.getNthElementByWeight(i).getAtomicNumber());
      for(final Element elm : elms) {
         ps.setFloat(pos, (float) comp.weightFraction(elm, false));
         ++pos;
      }
      ps.executeUpdate();
      try (final ResultSet rs = ps.getGeneratedKeys()) {
         return rs.next() ? rs.getInt(1) : -1;
      }
   }

   /**
    * Add a measurement value to the database.
    * 
    * @param comp
    * @return The index of the measurement (ELEMENT_DATA object) in the database
    * @throws SQLException
    */
   public int addMeasurement(Composition comp)
         throws SQLException {
      int u = -1;
      if(comp.isUncertain())
         u = addElementalDatum(comp, ElementDataTypes.UNCERTAINTY_MEASURE, -1);
      return addElementalDatum(comp, ElementDataTypes.MEASURED_COMPOSITION, u);
   }

   /**
    * Add a ParticleSignature to the database.
    * 
    * @param sig
    * @return The index of the ELEMENT_DATA record in the database
    * @throws SQLException
    */
   public int addParticleSignature(ParticleSignature sig)
         throws SQLException {
      final Set<Element> elms = sig.getUnstrippedElementSet();
      final StringBuffer sql = new StringBuffer();
      final StringBuffer vals = new StringBuffer();
      sql.append("INSERT INTO ELEMENT_DATA (ID, COMP_TYPE, PRESENT, ENTERED_BY, UNCERTAINTY_KEY");
      vals.append(") VALUES (DEFAULT, ?, ?, ?, ?");
      for(int i = 0; (i < 5) && (i < elms.size()); ++i) {
         sql.append(", ELM");
         sql.append(i);
         vals.append(", ?");
      }
      for(final Element elm : elms) {
         sql.append(", ELM_");
         sql.append(elm.toAbbrev().toUpperCase());
         vals.append(", ?");
      }
      vals.append(" )");
      sql.append(vals);
      final PreparedStatement ps = mConnection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, ElementDataTypes.PARTICLE_SIGNATURE.ordinal());
      ps.setBytes(2, computePresent(elms));
      ps.setInt(3, findPerson(System.getProperty("user.name")));
      ps.setInt(4, -1);
      int pos = 5;
      for(int i = 0; (i < 5) && (i < elms.size()); ++i, ++pos)
         ps.setInt(pos, sig.getNthElement(i + 1).getAtomicNumber());
      for(final Element elm : elms) {
         ps.setFloat(pos, (float) sig.get(elm));
         ++pos;
      }
      ps.executeUpdate();
      try (final ResultSet rs = ps.getGeneratedKeys()) {
         return rs.next() ? rs.getInt(1) : -1;
      }
   }

   public int addParticleSpectrum(File specFile, ParticleSignature signature, double beamEnergy, String project, DetectorProperties detector, DetectorCalibration calibration, String collectedBy, Date acqDate)
         throws EPQException, AlreadyInDatabaseException {
      try {
         byte[] digest;
         {
            try {
               final MessageDigest md = MessageDigest.getInstance("SHA");
               final BufferedInputStream is = new BufferedInputStream(new FileInputStream(specFile));
               try (final DigestInputStream dis = new DigestInputStream(is, md)) {
                  final byte[] buffer = new byte[1024];
                  while(dis.read(buffer) != -1)
                     ;
                  digest = md.digest();
                  assert md.getDigestLength() == 20 : "Message digest length is not 20 bytes!";
               }
            }
            catch(final NoSuchAlgorithmException e) {
               e.printStackTrace();
               digest = new byte[0];
            }
         }
         { // Search for replicas
            final String mdSel = "SELECT ID, DISPLAY_NAME FROM SPECTRUM WHERE DIGEST = ?";
            final PreparedStatement ps = mConnection.prepareStatement(mdSel);
            ps.setBytes(1, digest);
            try (final ResultSet rs = ps.executeQuery()) {
               if(rs.next())
                  throw new AlreadyInDatabaseException(specFile.getName());
            }
         }
         final int detIdx = mDetectors.get(detector).intValue();
         final int calIdx = getCalibrationsInt(detector).get(calibration).intValue();
         final int projIdx = mProjects.get(project).intValue();
         final int colByIdx = mPeople.get(collectedBy).intValue();
         final String sql = "INSERT INTO SPECTRUM (ID, DIGEST, SIGNATURE, BEAM_ENERGY, OPERATOR, DETECTOR, CALIBRATION, PROJECT, ACQUIRED, FILENAME, DISPLAY_NAME, SPECTRUM ) "
               + "VALUES ( DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
         final PreparedStatement ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
         ps.setBytes(1, digest);
         ps.setInt(2, addParticleSignature(signature));
         ps.setFloat(3, (float) beamEnergy);
         ps.setInt(4, colByIdx);
         ps.setInt(5, detIdx);
         ps.setInt(6, calIdx);
         ps.setInt(7, projIdx);
         ps.setTimestamp(8, new Timestamp(specFile.lastModified()));
         ps.setString(9, specFile.getName());
         final StringBuffer dispName = new StringBuffer();
         {
            final String pn = getInvProjects().get(projIdx);
            dispName.append(pn != null ? pn : "N/A");
            dispName.append(" - ");
            final int p = specFile.getName().lastIndexOf('.');
            dispName.append(specFile.getName().substring(0, p));
         }
         ps.setString(10, dispName.toString());
         try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(specFile))) {
            ps.setBlob(11, is, specFile.length()); // SPECTRUM_BLOB
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               return rs.next() ? rs.getInt(1) : -1;
            }
         }
      }
      catch(final AlreadyInDatabaseException aide) {
         throw aide;
      }
      catch(final Exception e) {
         throw new EPQException(e);
      }
   }

   /**
    * Adds a new PERSON record to the database.
    * 
    * @param name
    * @param comment
    */
   public void addPerson(String name, String comment) {
      final Map<String, Integer> people = getPeople();
      try {
         if(!people.containsKey(name)) {
            final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO PERSON (ID, NAME, COMMENT) VALUES (DEFAULT, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, comment);
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               if(rs.next()) {
                  final Integer id = Integer.valueOf(rs.getInt(1));
                  mPeople.put(name, id);
                  mInvPeople.put(id, name);
               }
            }
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
   }

   /**
    * Add a new project of the specified name (must be unique) associated with
    * the specified client and comment.
    * 
    * @param name
    * @param client
    * @param comment
    */
   public void addProject(String name, int client, String comment) {
      final Map<String, Integer> projects = getProjects();
      if(!projects.containsKey(name))
         try {
            final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO PROJECT (ID, NAME, CLIENT, COMMENT) VALUES (DEFAULT, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, client);
            ps.setString(3, comment);
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               if(rs.next()) {
                  final Integer id = Integer.valueOf(rs.getInt(1));
                  mProjects.put(name, id);
                  mInvProjects.put(id, name);
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
   }

   /**
    * Add a spectrum to the database. The method computes a digest of the
    * spectrum and checks to see whether the database already contains the
    * spectrum.
    * 
    * @param spec
    * @param force true -&gt; Add the spectrum regardless of whether the digest
    *           says it is already in the database
    * @return int
    * @throws FileNotFoundException
    * @throws IOException
    * @throws EPQException
    * @throws SQLException
    */
   public int addSpectrum(ISpectrumData spec, boolean force)
         throws FileNotFoundException, IOException, EPQException, SQLException, AlreadyInDatabaseException {
      final SpectrumProperties sp = spec.getProperties();
      if(sp.isDefined(SpectrumProperties.SpectrumDB))
         throw new EPQException("This spectrum is already in the database.");
      File file = null;
      {
         final String fn = sp.getTextWithDefault(SpectrumProperties.SourceFile, null);
         if(fn != null)
            file = new File(fn);
         if((file == null) || (!file.canRead())) {
            file = File.createTempFile("spectrum", ".emsa");
            file.deleteOnExit();
            try (final FileOutputStream os = new FileOutputStream(file)) {
               WriteSpectrumAsEMSA1_0.write(spec, os, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
            }
         }
      }
      StringReader diffReader = null;
      {
         final ISpectrumData onDisk = SpectrumFile.open(file)[0];
         final SpectrumProperties diff = SpectrumProperties.difference(spec.getProperties(), onDisk.getProperties());
         diff.setDetector(null);
         diffReader = new StringReader(EPQXStream.getInstance().toXML(diff));
      }
      byte[] digest;
      {
         try {
            final MessageDigest md = MessageDigest.getInstance("SHA");
            try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
               try (final DigestInputStream dis = new DigestInputStream(is, md)) {
                  final byte[] buffer = new byte[1024];
                  while(dis.read(buffer) != -1)
                     ;
                  digest = md.digest();
                  assert md.getDigestLength() == 20 : "Message digest length is not 20 bytes!";
               }
            }
         }
         catch(final NoSuchAlgorithmException e) {
            e.printStackTrace();
            digest = new byte[0];
         }
      }
      if(!force) { // Search for replicas
         final String mdSel = "SELECT ID, DISPLAY_NAME FROM SPECTRUM WHERE DIGEST = ?";
         final PreparedStatement ps = mConnection.prepareStatement(mdSel);
         ps.setBytes(1, digest);
         try (final ResultSet rs = ps.executeQuery()) {
            if(rs.next())
               throw new AlreadyInDatabaseException(spec.toString());
         }
      }
      {
         final String sql = "INSERT INTO SPECTRUM (ID, DIGEST, STD_COMP, MICRO_COMP, SIGNATURE, BEAM_ENERGY, OPERATOR, "
               + "DETECTOR, CALIBRATION, PROJECT, ACQUIRED, FILENAME, DISPLAY_NAME, EXTRA_PROPERTIES, SPECTRUM ) "
               + "VALUES ( DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
         final PreparedStatement ps = mConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
         ps.setBytes(1, digest); // DIGEST
         {
            final Composition std = sp.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
            int i = -1;
            if(std != null) {
               i = find(std, ElementDataTypes.STANDARD_COMPOSITION);
               if(i == -1)
                  i = this.addStandard(std);
            }
            ps.setInt(2, i);
         }
         {
            final Composition comp = sp.getCompositionWithDefault(SpectrumProperties.MicroanalyticalComposition, null);
            int i = -1;
            if(comp != null) {
               i = find(comp, ElementDataTypes.MEASURED_COMPOSITION);
               if(i == -1)
                  i = this.addMeasurement(comp);
            }
            ps.setInt(3, i);
         }
         {
            final ParticleSignature sig = sp.getParticleSignatureWithDefault(SpectrumProperties.ParticleSignature, null);
            int i = -1;
            if(sig != null) {
               i = find(sig);
               if(i == -1)
                  i = this.addParticleSignature(sig);
            }
            ps.setInt(4, i);
         }
         // Beam energy
         ps.setFloat(5, (float) sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, 0.0));
         // Operator
         ps.setInt(6, findPerson(sp.getTextWithDefault(SpectrumProperties.InstrumentOperator, "Unknown")));
         // Detector & Calibration
         {

            final IXRayDetector det = sp.getDetector();
            if(det != null) {
               final int detIdx = findDetector(det.getDetectorProperties());
               ps.setInt(7, detIdx);
               // Calibration
               final DetectorCalibration calib = det.getCalibration();
               final int calIdx = findCalibration(det.getDetectorProperties(), calib);
               if(calIdx == -1)
                  addCalibration(det.getDetectorProperties(), calib);
               ps.setInt(8, calIdx);
            } else {
               ps.setInt(7, -1);
               ps.setInt(8, -1);
            }
         }
         // Project
         {
            final String project = sp.getTextWithDefault(SpectrumProperties.ProjectName, null);
            final Integer pjIdx = project != null ? getProjects().get(project) : null;
            ps.setInt(9, pjIdx != null ? pjIdx.intValue() : findProject("None"));
         }
         // Acquired
         final java.util.Date acquired = sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new java.util.Date(System.currentTimeMillis()));
         ps.setDate(10, new java.sql.Date(acquired.getTime())); // Acquired
         ps.setString(11, file.getName()); // Filename
         ps.setString(12, spec.toString()); // DISPLAY_NAME
         ps.setClob(13, diffReader);
         try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            ps.setBlob(14, is, file.length()); // SPECTRUM_BLOB
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               return rs.next() ? rs.getInt(1) : -1;
            }
         }
      }
   }

   /**
    * Add or update the composition of a standard in the database.
    * 
    * @param comp
    * @return The index of the (ELEMENT_DATA) standard in the database.
    * @throws SQLException
    */
   public int addStandard(Composition comp)
         throws SQLException {
      int res = -1;
      // Does it already exist in the database?
      final PreparedStatement st = mConnection.prepareStatement("SELECT ELM_DATA, DENSITY FROM STANDARD WHERE NAME = ?");
      st.setMaxRows(1);
      st.setString(1, comp.getName());
      try (final ResultSet rs = st.executeQuery()) {
         final double NO_DENSITY = -1.0;
         final double density = (comp instanceof Material ? ((Material) comp).getDensity() : NO_DENSITY);
         if(rs.next()) {
            // A material of this name exists in database (update it??)
            res = rs.getInt(1);
            final Composition dbComp = (Composition) getElementalDatum(res);
            boolean update = (Math.abs(density - rs.getDouble(2)) > 0.001);
            if(!dbComp.almostEquals(comp, 1.0e-5)) {
               // Compositions are essentially not equal!
               int u = -1;
               if(comp.isUncertain())
                  u = addElementalDatum(comp, ElementDataTypes.UNCERTAINTY_MEASURE, -1);
               res = addElementalDatum(comp, ElementDataTypes.STANDARD_COMPOSITION, u);
               update = true;
            }
            if(update) {
               // Composition or density is different!
               final PreparedStatement us = mConnection.prepareStatement("UPDATE STANDARD SET ELM_DATA=?, DENSITY=? WHERE NAME=?");
               us.setInt(1, res);
               us.setDouble(2, density);
               us.setString(3, comp.getName());
               us.executeUpdate();
            }
         } else {
            int u = -1;
            if(comp.isUncertain())
               u = addElementalDatum(comp, ElementDataTypes.UNCERTAINTY_MEASURE, -1);
            res = addElementalDatum(comp, ElementDataTypes.STANDARD_COMPOSITION, u);
            final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO STANDARD (NAME, ELM_DATA, DENSITY) VALUES (?, ?, ?)");
            ps.setString(1, comp.toString());
            ps.setInt(2, res);
            ps.setDouble(3, density);
            ps.executeUpdate();
         }
      }
      return res;
   }

   private byte[] computePresent(Collection<Element> elms) {
      final byte[] c = new byte[((Element.elmCf - Element.elmH) + 7) / 8];
      for(int z = Element.elmH; z < Element.elmCf; ++z)
         if(elms.contains(Element.byAtomicNumber(z))) {
            final int zm1 = z - Element.elmH;
            c[zm1 / 8] = (byte) (c[zm1 / 8] + (1 << (zm1 % 8)));
         }
      return c;
   }

   private void buildQCTables()
         throws EPQException {
      final InputStream is = Session.class.getResourceAsStream("qc.sql");
      assert is != null : "Where is qc.sql?";
      executeSQL(is);

   }

   private void buildLineweightTables()
         throws EPQException {
      if(ENABLE_LW) {
         final InputStream is = Session.class.getResourceAsStream("lineweight.sql");
         assert is != null : "Where is lineweight.sql?";
         executeSQL(is);
      }
   }

   private void createDatabase()
         throws EPQException {
      final InputStream is = Session.class.getResourceAsStream("trixy.sql");
      assert is != null : "Where is trixy.sql?";
      executeSQL(is);
      buildQCTables();
      buildLineweightTables();
   }

   /**
    * @param is
    * @throws EPQException
    */
   private void executeSQL(final InputStream is)
         throws EPQException {
      try {
         final Reader isr = new InputStreamReader(is, "US-ASCII");
         assert isr != null;
         final BufferedReader br = new BufferedReader(isr);
         final StringBuffer sb = new StringBuffer();
         while(br.ready()) {
            String s = br.readLine();
            // strip comments
            if(s != null) {
               int p = s.indexOf("---");
               if(p >= 0)
                  s = s.substring(0, p);
               // Look for end-of-statement
               p = s.indexOf(';');
               if(p >= 0) {
                  sb.append(s.substring(0, p).trim());
                  // System.out.println(sb.toString());
                  final Statement st = mConnection.createStatement();
                  st.execute(sb.toString());
                  sb.setLength(0);
               } else {
                  sb.append(s.trim());
                  sb.append(" ");
               }
            }
         }
      }
      catch(final Exception e) {
         throw new EPQException(e);
      }
   }

   public void defaultInitialization()
         throws SQLException {
      addPerson(System.getProperty("user.name"), "Automatically added by the system based on the user who created the database.");
      final EDSDetector det = EDSDetector.createSiLiDetector(2048, 10.0, 132);
      addDetector(det);
      addStandard(MaterialFactory.createMaterial("K411"));
      addStandard(MaterialFactory.createMaterial("K412"));
      addMeasurement(MaterialFactory.createMaterial("K3189"));
      final Element[] elms = new Element[] {
         Element.Cu,
         Element.Mn,
         Element.Zn,
         Element.Fe
      };
      for(final Element elm : elms) {
         Composition comp;
         try {
            comp = MaterialFactory.createPureElement(elm);
            comp.setName(elm.toAbbrev() + " standard");
            addStandard(comp);
         }
         catch(final EPQException e) {
            e.printStackTrace();
         }
      }
   }

   private String edtColumn(ElementDataTypes edt) {
      switch(edt) {
         case STANDARD_COMPOSITION:
            return "STD_COMP";
         case MEASURED_COMPOSITION:
            return "MICRO_COMP";
         case PARTICLE_SIGNATURE:
            return "PARTICLE_SIGNATURE";
         default:
            throw new EPQFatalException("Not a valid type for edtColumn.");
      }
   }

   public int find(Composition comp, ElementDataTypes edt) {
      final StringBuffer sql = new StringBuffer("SELECT ID FROM ELEMENT_DATA WHERE COMP_TYPE = ? AND PRESENT = ?");
      final Set<Element> elms = comp.getElementSet();
      for(final Element elm : elms) {
         sql.append(" AND ELM_");
         sql.append(elm.toAbbrev().toUpperCase());
         sql.append(" = ?");
      }
      try {
         final PreparedStatement ps = mConnection.prepareStatement(sql.toString());
         ps.setInt(1, edt.ordinal());
         ps.setBytes(2, computePresent(elms));
         int pos = 3;
         for(final Element elm : elms) {
            ps.setFloat(pos, (float) comp.weightFraction(elm, false));
            ++pos;
         }
         try (final ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
      return -1;
   }

   /**
    * Find a ParticleSignature in the database (exact match)
    * 
    * @param sig
    * @return The index of the ELEMENT_DATA record in the database
    */
   public int find(ParticleSignature sig) {
      final StringBuffer sql = new StringBuffer("SELECT ID FROM ELEMENT_DATA WHERE COMP_TYPE = ? AND PRESENT = ?");
      final Set<Element> elms = sig.getUnstrippedElementSet();
      for(final Element elm : elms) {
         sql.append(" AND ELM_");
         sql.append(elm.toAbbrev().toUpperCase());
         sql.append(" = ?");
      }
      try {
         final PreparedStatement ps = mConnection.prepareStatement(sql.toString());
         ps.setInt(1, ElementDataTypes.PARTICLE_SIGNATURE.ordinal());
         ps.setBytes(2, computePresent(elms));
         int pos = 3;
         for(final Element elm : elms) {
            ps.setFloat(pos, (float) sig.get(elm));
            ++pos;
         }
         try (final ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
      return -1;
   }

   /**
    * Find all spectra within the fractional tolerance of the specified
    * ParticleSignature
    * 
    * @param sig - A ParticleSignature
    * @param tol - Tolerance 0.0001 to 0.9999
    * @param maxSpec - Maximum number of spectra to return
    * @return TreeSet&lt;SpectrumSummary&gt;
    */
   public TreeSet<SpectrumSummary> find(ParticleSignature sig, double tol, int maxSpec) {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final Set<Element> elms = sig.getUnstrippedElementSet();
      final boolean comp = true, present = false;
      final StringBuffer sql = new StringBuffer("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE SPECTRUM.SIGNATURE IN (SELECT ID");
      sql.append(" FROM ELEMENT_DATA WHERE COMP_TYPE = 3");
      if(present)
         sql.append(" AND PRESENT = ?");
      if(comp)
         for(final Element elm : elms) {
            final String elmStr = elm.toAbbrev().toUpperCase();
            sql.append(" AND (ELM_");
            sql.append(elmStr);
            sql.append(" >= ? AND ELM_");
            sql.append(elmStr);
            sql.append(" <= ?)");
         }
      sql.append(")");
      try {
         final PreparedStatement ps = mConnection.prepareStatement(sql.toString());
         ps.setMaxRows(maxSpec);
         int pos = 1;
         if(present)
            ps.setBytes(pos++, computePresent(elms));
         if(comp)
            for(final Element elm : elms) {
               final double delta = Math.max(sig.get(elm), 0.1) * tol;
               final double min = Math.max(0.0, sig.get(elm) - delta);
               final double max = Math.min(1.0, sig.get(elm) + delta);
               ps.setFloat(pos++, (float) min);
               ps.setFloat(pos++, (float) max);
            }
         try (final ResultSet rs = ps.executeQuery()) {
            for(int i = 0; (i < maxSpec) && rs.next();)
               try {
                  res.add(new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)));
                  ++i;
               }
               catch(final Exception e) {
                  e.printStackTrace();
               }
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
      return res;
   }

   /**
    * Finds the database record index associated with the specified person. If a
    * record doesn't exist one is created and the resulting record index
    * returned.
    * 
    * @param name
    * @return int - database record index ('ID')
    */
   public int findPerson(String name) {
      Integer res = getPeople().get(name);
      if(res == null) {
         addPerson(name, "");
         res = getPeople().get(name);
      }
      return res.intValue();
   }

   public int findProject(String name)
         throws EPQException {
      final Integer res = getProjects().get(name);
      if(res == null)
         throw new EPQException("No project of this name has been defined.");
      return res.intValue();
   }

   /**
    * Search for spectra by composition match with the specified tolerance.
    * 
    * @param comp
    * @param tol (1.0e-6 to 0.1)
    * @param det
    * @param eBeam
    * @param collectedBy
    * @param maxSpec
    * @return A sorted set of SpectrumSummary objects
    * @throws SQLException
    * @throws EPQException
    * @throws IOException
    * @throws FileNotFoundException
    */
   public TreeSet<SpectrumSummary> findSpectra(Composition comp, double tol, DetectorProperties det, double eBeam, String collectedBy, int maxSpec)
         throws SQLException, FileNotFoundException, IOException, EPQException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final StringBuffer sql = new StringBuffer();
      tol = Math.max(1.0e-6, Math.min(0.1, tol));
      sql.append("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRA WHERE ID=ELM_DATA.ID AND ");
      if(det != null) {
         final Integer detIdx = getDetectorsInt().get(det);
         if(detIdx != null) {
            sql.append("DETECTOR = ");
            sql.append(detIdx);
            sql.append(" AND ");
         }
      }
      if(eBeam != Double.NaN) {
         sql.append("BEAM_ENERGY > ");
         sql.append(Double.toString(0.99 * eBeam));
         sql.append(" AND ");
         sql.append("BEAM_ENERGY < ");
         sql.append(Double.toString(1.01 * eBeam));
         sql.append(" AND ");
      }
      if(collectedBy != null) {
         final Integer pIdx = getPeople().get(collectedBy);
         if(pIdx != null) {
            sql.append("OPERATOR = ");
            sql.append(pIdx);
         }
      }
      sql.append(" WHERE (");

      sql.append("SELECT ID FROM ELM_DATA WHERE COMP_TYPE = ");
      sql.append(ElementDataTypes.MEASURED_COMPOSITION.ordinal());
      for(final Element elm : comp.getElementSet()) {
         final double v = comp.weightFraction(elm, false);
         sql.append(" AND ELM_");
         sql.append(elm.toAbbrev());
         sql.append(">=");
         sql.append(Math.max(0.0, v - (Math.max(v, 0.1) * tol)));
         sql.append(" AND ELM_");
         sql.append(elm.toAbbrev());
         sql.append("<=");
         sql.append(Math.min(1.0, v + (Math.max(v, 0.1) * tol)));
      }
      sql.append(")");
      final Statement st = mConnection.createStatement();
      st.setMaxRows(maxSpec);
      try (final ResultSet rs = st.executeQuery(sql.toString())) {
         while(rs.next()) {
            final SpectrumSummary ss = new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3));
            res.add(ss);
         }
      }
      return res;

   }

   /**
    * <p>
    * Use the specified search string (<code>where</code>) to find a specified
    * type (<code>edt</code>) of spectrum in the database.
    * </p>
    * <b>Example</b>
    * <p>
    * <code>where = </code> "ELM_SI&gt;10 AND ELM_SI&lt;20 AND ELM_O&gt;20"
    * </p>
    * 
    * @param where A SQL string of
    * @param edt (may be <code>null</code>)
    * @param max
    * @return TreeSet&lt;SpectrumSummary&gt;
    * @throws SQLException
    */
   public TreeSet<SpectrumSummary> findSpectra(String where, ElementDataTypes edt, int max)
         throws SQLException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final Statement st = mConnection.createStatement();
      st.setMaxRows(max);
      final StringBuffer sql = new StringBuffer();
      sql.append("SELECT ID FROM ELEMENT_DATA WHERE ");
      if(edt != null) {
         sql.append("COMP_TYPE = ");
         sql.append(edt.ordinal());
         sql.append(" AND ");
      }
      sql.append(where);
      try (final ResultSet rs = st.executeQuery(sql.toString())) {
         while(rs.next() && (res.size() < max))
            res.addAll(getAssociatedSpectra(rs.getInt("ID"), edt, max - res.size()));
      }
      return res;
   }

   /**
    * Starts loading spectra from the database on a secondary thread. The load
    * process is carefully synchronized so that if another thread calls
    * <code>SpectrumSummary.load()</code> the thread will either return a
    * previously loaded instance of the spectrum (when available), halt and wait
    * for this thread to return it, or retrieve it itself.
    * 
    * @param ss
    * @return SwingWorker&lt;ArrayList&lt;ISpectrumData&gt;, ISpectrumData&gt;
    */
   public SwingWorker<ArrayList<ISpectrumData>, ISpectrumData> initiateLoad(Collection<SpectrumSummary> ss) {

      class LoadSpectra
         extends
         SwingWorker<ArrayList<ISpectrumData>, ISpectrumData> {
         private final Collection<SpectrumSummary> mSpectra;

         private LoadSpectra(Collection<SpectrumSummary> specs) {
            mSpectra = specs;
         }

         @Override
         protected ArrayList<ISpectrumData> doInBackground() {
            final ArrayList<ISpectrumData> res = new ArrayList<ISpectrumData>();
            for(final SpectrumSummary ss : mSpectra)
               try {
                  final ISpectrumData spec = ss.load();
                  if(spec != null)
                     res.add(spec);
                  publish(spec);
               }
               catch(final Exception e) {
                  e.printStackTrace();
               }
            return res;
         }
      }

      final LoadSpectra ls = new LoadSpectra(ss);
      ls.execute();
      return ls;
   }

   public ArrayList<ISpectrumData> loadSpectra(Collection<SpectrumSummary> specIds)
         throws SQLException, IOException, EPQException {
      final ArrayList<ISpectrumData> res = new ArrayList<ISpectrumData>();
      for(final SpectrumSummary ss : specIds) {
         final ISpectrumData spec = ss.load();
         if(spec != null)
            res.add(spec);
      }
      return res;
   }

   public TreeSet<SpectrumSummary> findStandards(String name, DetectorProperties det, double eBeam, String collectedBy, int maxSpec)
         throws SQLException, IOException, EPQException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final StringBuffer sql = new StringBuffer("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE ");
      if(det != null) {
         final Integer detIdx = getDetectorsInt().get(det);
         if(detIdx != null) {
            sql.append("DETECTOR = ");
            sql.append(detIdx);
            sql.append(" AND ");
         }
      }
      if(eBeam != Double.NaN) {
         sql.append("BEAM_ENERGY > ");
         sql.append(Double.toString(0.99 * eBeam));
         sql.append(" AND ");
         sql.append("BEAM_ENERGY < ");
         sql.append(Double.toString(1.01 * eBeam));
         sql.append(" AND ");
      }
      if(collectedBy != null) {
         final Integer pIdx = getPeople().get(collectedBy);
         if(pIdx != null) {
            sql.append("OPERATOR = ");
            sql.append(pIdx);
         }
      }
      final Integer stdIdx = getStandards().get(name);
      if(stdIdx != null) {
         sql.append(" AND STD_COMP = ");
         sql.append(stdIdx);
      }
      sql.append(" ORDER BY ACQUIRED");
      final Statement st = mConnection.createStatement();
      st.setMaxRows(maxSpec);
      try (final ResultSet rs = st.executeQuery(sql.toString())) {
         while(rs.next())
            res.add(new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)));
      }
      return res;
   }

   /**
    * Find standards for the specified detector, beam energy and element.
    * 
    * @param det
    * @param eBeam
    * @param elm
    * @return A set of SpectrumSummary objects
    * @throws SQLException
    * @throws IOException
    * @throws EPQException
    */
   public TreeSet<SpectrumSummary> findStandards(DetectorProperties det, double eBeam, Element elm)
         throws SQLException, IOException, EPQException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final Statement st = mConnection.createStatement();
      final StringBuffer sql = new StringBuffer();
      sql.append("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE STD_COMP IN (");
      sql.append("SELECT ID FROM ELEMENT_DATA WHERE COMP_TYPE = ");
      sql.append(ElementDataTypes.STANDARD_COMPOSITION.ordinal());
      sql.append(" AND ELM_");
      sql.append(elm.toAbbrev().toUpperCase());
      sql.append(" > 0.01) ");
      if(det != null) {
         final Integer detIdx = getDetectorsInt().get(det);
         if(detIdx != null) {
            sql.append("AND DETECTOR = ");
            sql.append(detIdx);
         }
      }
      if(eBeam != Double.NaN) {
         sql.append("AND BEAM_ENERGY > ");
         sql.append(Double.toString(0.99 * eBeam));
         sql.append(" AND ");
         sql.append("BEAM_ENERGY < ");
         sql.append(Double.toString(1.01 * eBeam));
      }
      try (final ResultSet rs = st.executeQuery(sql.toString())) {
         while(rs.next())
            res.add(new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)));
      }
      return res;
   }

   public TreeSet<SpectrumSummary> findReferences(DetectorProperties det, double eBeam, Collection<Element> elms)
         throws SQLException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final Statement st = mConnection.createStatement();
      final StringBuffer sql = new StringBuffer();
      sql.append("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE STD_COMP IN (");
      sql.append("SELECT ID FROM ELEMENT_DATA WHERE COMP_TYPE IN (");
      sql.append(ElementDataTypes.STANDARD_COMPOSITION.ordinal());
      sql.append(", ");
      sql.append(ElementDataTypes.MEASURED_COMPOSITION.ordinal());
      sql.append(")");
      for(final Element elm : elms) {
         sql.append(" AND ELM_");
         sql.append(elm.toAbbrev().toUpperCase());
         sql.append(" > 0.01) ");
      }
      if(det != null) {
         final Integer detIdx = getDetectorsInt().get(det);
         if(detIdx != null) {
            sql.append("AND DETECTOR = ");
            sql.append(detIdx);
         }
      }
      if(eBeam != Double.NaN) {
         sql.append("AND BEAM_ENERGY > ");
         sql.append(Double.toString(0.99 * eBeam));
         sql.append(" AND ");
         sql.append("BEAM_ENERGY < ");
         sql.append(Double.toString(1.01 * eBeam));
      }
      try (final ResultSet rs = st.executeQuery(sql.toString())) {
         while(rs.next())
            res.add(new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)));
      }
      return res;
   }

   private TreeSet<SpectrumSummary> getAssociatedSpectra(int elmDataId, ElementDataTypes edt, int max)
         throws SQLException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final DateFormat df = DateFormat.getDateTimeInstance();
      final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE "
            + edtColumn(edt) + " = ?");
      ps.setInt(1, elmDataId);
      try (final ResultSet specs = ps.executeQuery()) {
         while(specs.next()) {
            final StringBuffer sb = new StringBuffer();
            sb.append(specs.getString("DISPLAY_NAME"));
            sb.append(" - ");
            sb.append(df.format(specs.getTimestamp("ACQUIRED")));
            res.add(new SpectrumSummary(specs.getInt(1), specs.getString(2), specs.getTimestamp(3)));
         }
      }
      return res;
   }

   public String findClient(String project) {
      String res = null;
      try {
         final PreparedStatement ps = mConnection.prepareStatement("SELECT CLIENT FROM PROJECT WHERE NAME = ?");
         ps.setString(1, project);
         try (final ResultSet rs = ps.executeQuery()) {
            if(rs.next())
               res = getInvPeople().get(Integer.valueOf(rs.getInt(1)));
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
      return res;
   }

   public Connection getConnection() {
      return mConnection;
   }

   public String getDatabaseName() {
      return mDbName;
   }

   /**
    * Returns a list of the active (non-disabled) detectors.
    * 
    * @return Set&lt;DetectorProperties&gt;
    */
   public Set<DetectorProperties> getDetectors() {
      getElectronProbes();
      return Collections.unmodifiableSet(mActiveDetectors.keySet());
   }

   /**
    * Returns a list of the active and disabled detectors.
    * 
    * @return Set&lt;DetectorProperties&gt;
    */
   public Set<DetectorProperties> getAllDetectors() {
      getElectronProbes();
      return Collections.unmodifiableSet(mDetectors.keySet());
   }

   public Set<DetectorProperties> getDetectors(ElectronProbe ep) {
      final HashSet<DetectorProperties> res = new HashSet<DetectorProperties>();
      for(final DetectorProperties dp : getDetectors())
         if(dp.getOwner() == ep)
            res.add(dp);
      return res;
   }

   /**
    * Returns a list of EDS dectectors (DetectorProperties plus the latest
    * DetectorCalibration). Selecting <code>activeOnly==true</code> will only
    * return those which have not been disabled.
    * 
    * @param activeOnly
    * @return Set&lt;EDSDetector&gt;
    */
   public Set<EDSDetector> getEDSDetectors(boolean activeOnly) {
      final Set<EDSDetector> res = new HashSet<EDSDetector>();
      for(final DetectorProperties dp : activeOnly ? getDetectors() : getAllDetectors()) {
         final DetectorCalibration dc = getLatestCalibration(dp);
         if(dc instanceof EDSCalibration)
            res.add(EDSDetector.createDetector(dp, (EDSCalibration) dc));
      }
      return res;
   }

   public EDSDetector findDetector(String name) {
      final Set<EDSDetector> dets = getEDSDetectors(false);
      for(final EDSDetector det : dets)
         if(det.toString().startsWith(name))
            return det;
      return null;
   }

   public DetectorProperties findDetector(int detIdx) {
      getDetectors();
      for(final Map.Entry<DetectorProperties, Integer> me : mDetectors.entrySet())
         if(me.getValue().intValue() == detIdx)
            return me.getKey();
      return null;
   }

   public DetectorProperties findDetectorFromGUID(String guid) {
      getDetectors();
      for(final Map.Entry<DetectorProperties, Integer> me : mDetectors.entrySet()) {
         SpectrumProperties sp = me.getKey().getProperties();
         String detGuid = sp.getTextWithDefault(SpectrumProperties.DetectorGUID, "");
         if(detGuid.equals(guid))
            return me.getKey();
      }
      return null;
   }

   /**
    * Map an EDS detector into a detector index.
    * 
    * @param dp
    * @return Integer detector index (-1 if none)
    */
   public int findDetector(DetectorProperties dp) {
      getDetectors();
      final Integer i = mDetectors.get(dp);
      return i != null ? i.intValue() : -1;
   }

   public Set<EDSDetector> getCurrentEDSDetectors(ElectronProbe ep) {
      final Set<EDSDetector> res = new HashSet<EDSDetector>();
      for(final DetectorProperties dp : getDetectors(ep)) {
         final DetectorCalibration dc = getLatestCalibration(dp);
         if(dc instanceof EDSCalibration)
            res.add(EDSDetector.createDetector(dp, (EDSCalibration) dc));
      }
      return res;
   }

   public Set<ElectronProbe> getCurrentProbes() {
      final Set<EDSDetector> dets = getEDSDetectors(true);
      final HashSet<ElectronProbe> eps = new HashSet<ElectronProbe>();
      for(final EDSDetector det : dets)
         eps.add(det.getOwner());
      return eps;
   }

   public Map<DetectorProperties, Integer> getDetectorsInt() {
      getElectronProbes();
      return Collections.unmodifiableMap(mActiveDetectors);
   }

   /**
    * Returns a list of available (non-retired) instance of ElectronProbe along
    * with the associated DetectorProperties objects.
    * 
    * @return Map&lt;ElectronProbe, Integer&gt;
    */
   public synchronized Map<ElectronProbe, Integer> getElectronProbes() {
      if(mElectronProbes == null) {
         mElectronProbes = new HashMap<ElectronProbe, Integer>();
         mActiveDetectors = new HashMap<DetectorProperties, Integer>();
         mDetectors = new HashMap<DetectorProperties, Integer>();
         final TreeMap<Integer, ElectronProbe> tmp = new TreeMap<Integer, ElectronProbe>();
         try {
            final EPQXStream xs = EPQXStream.getInstance();
            {
               final Statement st = mConnection.createStatement();
               try (final ResultSet rs = st.executeQuery("SELECT * FROM ELECTRONPROBE")) {
                  while(rs.next())
                     try {
                        final Integer id = Integer.valueOf(rs.getInt("ID"));
                        final String xmlObj = rs.getString("XML_OBJ");
                        final SpectrumProperties sp = (SpectrumProperties) xs.fromXML(xmlObj);
                        final ElectronProbe ep = new ElectronProbe(sp);
                        mElectronProbes.put(ep, id);
                        tmp.put(id, ep);
                     }
                     catch(final ConversionException e) {
                        e.printStackTrace();
                     }
               }
            }
            {
               final Statement st = mConnection.createStatement();
               try (final ResultSet rs = st.executeQuery("SELECT * FROM DETECTOR")) {
                  while(rs.next())
                     try {
                        final Integer id = rs.getInt("ID");
                        final String xml = rs.getString("XML_OBJ");
                        final Timestamp retired = rs.getTimestamp("RETIRED");
                        final DetectorProperties det = (DetectorProperties) xs.fromXML(xml);
                        mDetectors.put(det, id);
                        if(retired == null)
                           mActiveDetectors.put(det, id);
                        final ElectronProbe ep = tmp.get(Integer.valueOf(rs.getInt("INSTRUMENT_KEY")));
                        if(ep != null)
                           det.setOwner(ep);
                     }
                     catch(final ConversionException e) {
                        e.printStackTrace();
                     }
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
      return Collections.unmodifiableMap(mElectronProbes);
   }

   /**
    * Returns the element datum associate with the specified index
    * 
    * @param idx
    * @return A Composition or ParticleSignature object
    */
   private Object getElementalDatum(int idx) {
      final String sql = "SELECT * FROM ELEMENT_DATA WHERE ID=?";
      try {
         final PreparedStatement ps = mConnection.prepareStatement(sql);
         ps.setInt(1, idx);
         try (final ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
               final int type = rs.getInt("COMP_TYPE");
               if((type == ElementDataTypes.MEASURED_COMPOSITION.ordinal())
                     || (type == ElementDataTypes.STANDARD_COMPOSITION.ordinal())) {
                  final Composition res = new Composition();
                  for(int z = Element.elmH; z <= Element.elmCf; ++z) {
                     final double v = rs.getFloat("ELM_" + Element.toAbbrev(z));
                     if(v != 0.0)
                        res.addElement(Element.byAtomicNumber(z), v);
                  }
                  return res;
               } else if(type == ElementDataTypes.PARTICLE_SIGNATURE.ordinal()) {
                  final ParticleSignature sig = new ParticleSignature();
                  for(int z = Element.elmH; z <= Element.elmCf; ++z) {
                     final double v = rs.getFloat("ELM_" + Element.toAbbrev(z));
                     if(v != 0.0)
                        sig.add(Element.byAtomicNumber(z), v);
                  }
                  return sig;
               }
            }
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
      return null;
   }

   /**
    * Returns a map between the names of the people currently understood by the
    * database and the database IDs.
    * 
    * @return Map&lt;String, Integer&gt;
    */
   public Map<String, Integer> getPeople() {
      if(mPeople == null) {
         mPeople = new TreeMap<String, Integer>();
         mInvPeople = new TreeMap<Integer, String>();
         try {
            final Statement st = mConnection.createStatement();
            try (final ResultSet rs = st.executeQuery("SELECT ID, NAME FROM PERSON")) {
               while(rs.next()) {
                  final int id = rs.getInt("ID");
                  final String name = rs.getString("NAME").trim();
                  mPeople.put(name, Integer.valueOf(id));
                  mInvPeople.put(Integer.valueOf(id), name);
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
      return Collections.unmodifiableMap(mPeople);
   }

   public Map<Integer, String> getInvPeople() {
      if(mPeople == null)
         getPeople();
      return mInvPeople;
   }

   /**
    * Returns a map of available project names to the associated database
    * indexes.
    * 
    * @return TreeMap&lt;String,Integer&gt;
    */
   public Map<String, Integer> getProjects() {
      if(mProjects == null) {
         mProjects = new TreeMap<String, Integer>();
         mInvProjects = new TreeMap<Integer, String>();
         try {
            final Statement st = mConnection.createStatement();
            try (final ResultSet rs = st.executeQuery("SELECT ID, NAME FROM PROJECT")) {
               while(rs.next()) {
                  final int id = rs.getInt("ID");
                  final String name = rs.getString("NAME").trim();
                  final Integer iv = Integer.valueOf(id);
                  mProjects.put(name, iv);
                  mInvProjects.put(iv, name);
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
      }
      return Collections.unmodifiableMap(mProjects);
   }

   public Map<Integer, String> getInvProjects() {
      if(mProjects == null)
         getProjects();
      return mInvProjects;
   }

   public TreeSet<SpectrumSummary> findSpectra(String project, DetectorProperties detector, DetectorCalibration calibration, String collectedBy, double beamEnergy, int maxSpectra) {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final Integer pj = getProjects().get(project);
      final Integer det = getDetectorsInt().get(detector);
      final Integer per = getPeople().get(collectedBy);
      final int cal = calibration != null ? findCalibration(detector, calibration) : -1;
      final StringBuffer sql = new StringBuffer();
      sql.append("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE ");
      boolean addAnd = false;
      if(pj != null) {
         sql.append("PROJECT = ");
         sql.append(pj);
         addAnd = true;
      }
      if(det != null) {
         if(addAnd)
            sql.append(" AND ");
         sql.append(" DETECTOR = ");
         sql.append(det);
         addAnd = true;
      }
      if(collectedBy != null) {
         if(addAnd)
            sql.append(" AND ");
         sql.append(" OPERATOR = ");
         sql.append(per);
         addAnd = true;
      }
      if(cal != -1) {
         if(addAnd)
            sql.append(" AND ");
         sql.append("CALIBRATION = ");
         sql.append(cal);
         addAnd = true;
      }
      if(!Double.isNaN(beamEnergy)) {
         if(addAnd)
            sql.append(" AND ");
         sql.append("BEAM_ENERGY = ?");
      }
      try {
         final PreparedStatement ps = mConnection.prepareStatement(sql.toString());
         if(!Double.isNaN(beamEnergy))
            ps.setFloat(1, (float) beamEnergy);
         try (final ResultSet rs = ps.executeQuery()) {
            for(int i = 0; rs.next() && (i < maxSpectra);)
               try {
                  res.add(new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)));
                  ++i;
               }
               catch(final Exception e) {
                  e.printStackTrace();
               }
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
      return res;
   }

   /**
    * Get the standard associated with the specified name.
    * 
    * @param name
    * @return Composition or Material object
    * @throws SQLException
    */
   public Composition findStandard(String name)
         throws SQLException {
      if(name.compareToIgnoreCase("unknown") != 0) {
         final PreparedStatement st = mConnection.prepareStatement("SELECT ELM_DATA, DENSITY FROM STANDARD WHERE NAME = ?");
         st.setMaxRows(1);
         st.setString(1, name);
         try (final ResultSet rs = st.executeQuery()) {
            if(rs.next()) {
               final Object res = getElementalDatum(rs.getInt(1));
               assert res instanceof Composition;
               Composition comp = (Composition) res;
               comp.setName(name);
               final double density = rs.getDouble(2);
               if(density > 0.0)
                  comp = new Material(comp, density);
               return comp;
            }
         }
      }
      return null;
   }

   public TreeSet<Composition> findAllStandards()
         throws SQLException {
      final TreeSet<Composition> resSet = new TreeSet<Composition>();
      final PreparedStatement st = mConnection.prepareStatement("SELECT NAME, ELM_DATA, DENSITY FROM STANDARD");
      try (final ResultSet rs = st.executeQuery()) {
         while(rs.next()) {
            final Object res = getElementalDatum(rs.getInt(2));
            assert res instanceof Composition;
            Composition comp = (Composition) res;
            comp.setName(rs.getString(1));
            final double density = rs.getDouble(3);
            if(density > 0.0)
               comp = new Material(comp, density);
            resSet.add(comp);
         }
      }
      return resSet;
   }

   /**
    * Returns a set of the names of standards ("named compositions")
    * 
    * @return TreeMap&lt;String&gt;
    * @throws SQLException
    */
   public TreeMap<String, Integer> getStandards()
         throws SQLException {
      final TreeMap<String, Integer> res = new TreeMap<String, Integer>();
      final Statement st = mConnection.createStatement();
      st.setMaxRows(0);
      try (final ResultSet rs = st.executeQuery("SELECT NAME, ELM_DATA FROM STANDARD")) {
         while(rs.next())
            res.put(rs.getString(1), Integer.valueOf(rs.getInt(2)));
      }
      return res;
   }

   public boolean isNew() {
      return mIsNew;
   }

   private ISpectrumData loadSpectrumFromResultSet(ResultSet rs)
         throws SQLException, IOException, FileNotFoundException, EPQException {
      ISpectrumData res;
      final int stdCompIdx = rs.getInt("STD_COMP");
      final int uCompIdx = rs.getInt("MICRO_COMP");
      final int sigIdx = rs.getInt("SIGNATURE");
      final double eBeam = rs.getDouble("BEAM_ENERGY");
      final int operator = rs.getInt("OPERATOR");
      final int detIdx = rs.getInt("DETECTOR");
      final int calIdx = rs.getInt("CALIBRATION");
      final int projIdx = rs.getInt("PROJECT");
      final Timestamp acquired = rs.getTimestamp("ACQUIRED");
      final String filename = rs.getString("FILENAME");
      final String dispName = rs.getString("DISPLAY_NAME");
      final int subIdx = 0; // rs.getInt(11);
      final Clob extraClob = rs.getClob("EXTRA_PROPERTIES");
      SpectrumProperties diff = null;
      try {
         if(extraClob != null)
            diff = (SpectrumProperties) EPQXStream.getInstance().fromXML(extraClob.getCharacterStream());
      }
      catch(final Exception e) {
         e.printStackTrace();
      }
      {
         try (final InputStream specBlob = rs.getBlob("SPECTRUM").getBinaryStream()) {
            String ext = ".tmp";
            if(filename.lastIndexOf(".") > 0)
               ext = filename.substring(filename.lastIndexOf("."));
            final File tmp = File.createTempFile("dbSpec", ext);
            try {
               {
                  try (final FileOutputStream fos = new FileOutputStream(tmp)) {
                     final byte[] buffer = new byte[1024];
                     for(int len = specBlob.read(buffer); len > 0; len = specBlob.read(buffer))
                        fos.write(buffer, 0, len);
                  }
               }
               res = SpectrumFile.open(tmp, subIdx);
            }
            finally {
               tmp.delete();
            }
         }
      }
      SpectrumProperties sp = res.getProperties();
      sp.setTextProperty(SpectrumProperties.SourceFile, filename);
      if(stdCompIdx != -1) {
         final Object obj = getElementalDatum(stdCompIdx);
         if(obj instanceof Composition)
            sp.setCompositionProperty(SpectrumProperties.StandardComposition, (Composition) obj);
      }
      if(uCompIdx != -1) {
         final Object obj = getElementalDatum(uCompIdx);
         if(obj instanceof Composition)
            sp.setCompositionProperty(SpectrumProperties.MicroanalyticalComposition, (Composition) obj);
      }
      if(sigIdx != -1) {
         final Object obj = getElementalDatum(sigIdx);
         if(obj instanceof ParticleSignature)
            sp.setParticleSignatureProperty(SpectrumProperties.ParticleSignature, (ParticleSignature) obj);
      }
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, eBeam);
      if(operator != -1)
         sp.setTextProperty(SpectrumProperties.InstrumentOperator, getInvPeople().get(operator));
      // Determine the correct DetectorProperties and DetectorCalibration
      DetectorProperties dp = null;
      DetectorCalibration dc = null;
      for(final Map.Entry<DetectorProperties, Integer> me : mDetectors.entrySet())
         if(me.getValue().intValue() == detIdx) {
            dp = me.getKey();
            for(final Map.Entry<DetectorCalibration, Integer> me2 : getCalibrationsInt(dp).entrySet())
               if(me2.getValue().intValue() == calIdx) {
                  dc = me2.getKey();
                  break;
               }
            break;
         }
      if(dc == null)
         dc = getBestCalibration(dp, acquired);
      if((dp != null) && (dc instanceof EDSCalibration)) {
         res = SpectrumUtils.applyEDSDetector(EDSDetector.createDetector(dp, (EDSCalibration) dc), res);
         sp = res.getProperties();
      }
      final String prj = getInvProjects().get(projIdx);
      sp.setTextProperty(SpectrumProperties.ProjectName, prj);
      sp.setTextProperty(SpectrumProperties.ClientName, findClient(prj));
      sp.setTimestampProperty(SpectrumProperties.AcquisitionTime, new java.util.Date(acquired.getTime()));
      sp.setTextProperty(SpectrumProperties.SpectrumDisplayName, dispName);
      if(diff != null)
         sp.addAll(diff);
      return res;
   }

   /**
    * Read the specified spectrum (by index) from the database...
    * 
    * @param id
    * @return A spectrum
    * @throws SQLException
    * @throws IOException
    * @throws EPQException
    */
   public ISpectrumData readSpectrum(int id)
         throws SQLException, IOException, EPQException {
      ISpectrumData res = null;
      final PreparedStatement ps = mConnection.prepareStatement("SELECT * FROM SPECTRUM WHERE ID = ?");
      ps.setInt(1, id);
      try (final ResultSet rs = ps.executeQuery()) {
         if(rs.next())
            res = loadSpectrumFromResultSet(rs);
      }
      return res;
   }

   /**
    * Read the list of spectra identified by database index and return them as a
    * set.
    * 
    * @param ids
    * @return TreeSet&lt;SpectrumSummary&lt;
    * @throws SQLException
    */
   public TreeSet<SpectrumSummary> readSpectra(int[] ids)
         throws SQLException {
      final TreeSet<SpectrumSummary> res = new TreeSet<SpectrumSummary>();
      final Statement st = mConnection.createStatement();
      st.setMaxRows(ids.length);
      final StringBuffer sb = new StringBuffer();
      sb.append("SELECT ID, DISPLAY_NAME, ACQUIRED FROM SPECTRUM WHERE ID IN (");
      for(int i = 0; i < ids.length; ++i) {
         if(i != 0)
            sb.append(", ");
         sb.append(ids[i]);
      }
      sb.append(")");
      try (final ResultSet rs = st.executeQuery(sb.toString())) {
         while(rs.next())
            try {
               res.add(new SpectrumSummary(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)));
            }
            catch(final Exception e) {
               e.printStackTrace();
            }
      }
      return res;
   }

   /**
    * Returns the DetectorCalibration objects associated with this detector
    * sorted in order of date.
    * 
    * @param dp
    * @return List&lt;DetectorCalibration&gt;
    */
   public List<DetectorCalibration> getCalibrations(DetectorProperties dp) {
      final Map<DetectorCalibration, Integer> dcs = getCalibrationsInt(dp);
      ArrayList<DetectorCalibration> cals = new ArrayList<DetectorCalibration>();
      for(final DetectorCalibration dc : dcs.keySet())
         cals.add(dc);
      Collections.sort(cals, new Comparator<DetectorCalibration>() {
         @Override
         public int compare(DetectorCalibration arg0, DetectorCalibration arg1) {
            return arg0.compareTo(arg1);
         }
      });
      return Collections.unmodifiableList(cals);
   }

   public DetectorCalibration getCalibrationFromGUID(DetectorProperties dp, String guid) {
      final Map<DetectorCalibration, Integer> dcs = getCalibrationsInt(dp);
      for(DetectorCalibration dc : dcs.keySet()) {
         final String calGuid = dc.getProperties().getTextWithDefault(SpectrumProperties.CalibrationGUID, "");
         if(calGuid.equals(guid))
            return dc;
      }
      return null;
   }

   /**
    * Returns the last calibration performed on the specified instrument.
    * 
    * @param dp
    * @return DetectorCalibration
    */
   public DetectorCalibration getMostRecentCalibration(DetectorProperties dp) {
      DetectorCalibration latest = null;
      final Map<DetectorCalibration, Integer> dcs = getCalibrationsInt(dp);
      for(final DetectorCalibration dc : dcs.keySet())
         if((latest == null) || (dc.getActiveDate().after(latest.getActiveDate())))
            latest = dc;
      return latest;
   }

   /**
    * Return the detector calibration which occurs the soonest after the
    * specified date. If there is no suitable calibration then the earliest
    * calibration is returned.
    * 
    * @param dp
    * @param date
    * @return DetectorCalibration
    */
   public DetectorCalibration getSuitableCalibration(DetectorProperties dp, Date date) {
      DetectorCalibration suitable = null;
      Date best = null;
      DetectorCalibration earliestDC = null;
      Date earliest = null;
      for(final Map.Entry<DetectorCalibration, Integer> me : getCalibrationsInt(dp).entrySet()) {
         final Date meDate = me.getKey().getActiveDate();
         if((meDate.before(date)) && ((best == null) || (best.before(meDate)))) {
            best = meDate;
            suitable = me.getKey();
         }
         if((earliest == null) || meDate.before(earliest)) {
            earliest = meDate;
            earliestDC = me.getKey();
         }
      }
      return suitable != null ? suitable : earliestDC;
   }

   /**
    * Returns the DetectorCalibration objects associated with this detector.
    * 
    * @param dp
    * @return Map&lt;DetectorCalibration, Integer&gt;
    */
   private Map<DetectorCalibration, Integer> getCalibrationsInt(DetectorProperties dp) {
      if(!mCalibrations.containsKey(dp)) {
         final HashMap<DetectorCalibration, Integer> dcs = new HashMap<DetectorCalibration, Integer>();
         try {
            int detId = findDetector(dp);
            if(detId == -1) {
               addDetector(dp);
               mConnection.commit();
               detId = findDetector(dp);
            }
            Statement st = mConnection.createStatement();
            try (final ResultSet rs = st.executeQuery("SELECT ID, XML_OBJ FROM CALIBRATION WHERE DETECTOR="
                  + Integer.toString(detId))) {
               while(rs.next()) {
                  Integer index = Integer.valueOf(rs.getInt(1));
                  // System.out.println(index);
                  if(!dcs.containsValue(index)) {
                     final String xml = rs.getString(2);
                     final DetectorCalibration dc = (DetectorCalibration) EPQXStream.getInstance().fromXML(xml);
                     { // Add the CalibrationHash tag
                        final SpectrumProperties cp = dc.getProperties();
                        if(!cp.isDefined(SpectrumProperties.CalibrationGUID))
                           cp.setTextProperty(SpectrumProperties.CalibrationGUID, EPQXStream.generateGUID(dc));
                     }
                     dcs.put(dc, index);
                  }
               }
            }
         }
         catch(final SQLException e) {
            e.printStackTrace();
         }
         mCalibrations.put(dp, dcs);
      }
      return mCalibrations.get(dp);
   }

   private int findCalibration(DetectorProperties dp, DetectorCalibration calib) {
      final Map<DetectorCalibration, Integer> mdci = getCalibrationsInt(dp);
      final Integer calIdx = mdci.get(calib);
      return calIdx != null ? calIdx.intValue() : -1;
   }

   /**
    * Returns the most recent calibration for the specified detector
    * 
    * @param dp
    * @return DetectorCalibration
    */
   private DetectorCalibration getLatestCalibration(DetectorProperties dp) {
      DetectorCalibration res = null;
      for(final DetectorCalibration dc : getCalibrations(dp))
         if((res == null) || (res.getActiveDate().before(dc.getActiveDate())))
            res = dc;
      return res;
   }

   /**
    * Returns the DetectorCalibration that occured prior to the specified Date
    * for the specified DetectorProperties.
    * 
    * @param dp
    * @param date
    * @return DetectorCalibration
    */
   private DetectorCalibration getBestCalibration(DetectorProperties dp, Date date) {
      DetectorCalibration res = null;
      for(final DetectorCalibration dc : getCalibrations(dp))
         if(((res == null) || res.getActiveDate().after(dc.getActiveDate())) && dc.getActiveDate().before(date))
            res = dc;
      return res;

   }

   @Override
   public String toString() {
      return mDbName;
   }

   private void deleteInstrument(int id) {
      try {
         final String sql = "DELETE FROM ELECTRONPROBE WHERE ID = ?";
         final PreparedStatement ps = mConnection.prepareStatement(sql);
         ps.setInt(1, id);
         ps.execute();
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
   }

   /**
    * Removes all evidence of the specified detector from the database. Use with
    * care!!!
    * 
    * @param id
    */
   public void deleteDetector(int id) {
      try {
         int instrument = Integer.MIN_VALUE;
         {
            final String sql = "SELECT INSTRUMENT_KEY FROM DETECTOR WHERE ID = ?";
            final PreparedStatement ps = mConnection.prepareStatement(sql);
            ps.setInt(1, id);
            try (final ResultSet rs = ps.executeQuery()) {
               if(rs.next())
                  instrument = rs.getInt(1);
            }
         }
         {
            final String sql = "DELETE FROM SPECTRUM WHERE DETECTOR = ?";
            final PreparedStatement ps = mConnection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.execute();
         }
         {
            final String sql = "DELETE FROM CALIBRATION WHERE DETECTOR = ?";
            final PreparedStatement ps = mConnection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.execute();
         }
         {
            final String sql = "DELETE FROM DETECTOR WHERE ID = ?";
            final PreparedStatement ps = mConnection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.execute();
         }
         {
            final String sql = "SELECT COUNT(ID) AS NumDets FROM DETECTOR WHERE INSTRUMENT_KEY = ?";
            final PreparedStatement ps = mConnection.prepareStatement(sql);
            ps.setInt(1, id);
            try (final ResultSet rs = ps.executeQuery()) {
               if(rs.next())
                  if(rs.getInt("NumDets") == 0)
                     deleteInstrument(instrument);
            }
         }
      }
      catch(final SQLException e) {
         e.printStackTrace();
      }
   }

   static final TreeMap<Integer, QCProject> mQCProjects = new TreeMap<Integer, QCProject>();
   static final String QC_PREFIX = "__QC__[";

   private static String toQCName(String comp) {
      return QC_PREFIX + comp + "]";
   }

   private static String fromQCName(String qcComp) {
      assert qcComp.startsWith(QC_PREFIX);
      assert qcComp.endsWith("]");
      return qcComp.substring(QC_PREFIX.length(), qcComp.length() - 1);
   }

   /**
    * Rounds the beam energy in keV to the nearest 0.1 keV.
    * 
    * @param beamE
    * @return beamE rounded to the nearest 0.1 keV
    */
   private static double roundBeamEnergy(double beamE) {
      return Math.round(10.0 * beamE) / 10.0; // round to the nearest 100 eV
   }

   /**
    * The QCProject class represents a set of associate quality control
    * measurements. The purpose is to facilitate tracking the performance of an
    * x-ray detector through time. A QC project contains a set of spectra and
    * the associated derived metrics. QC projects are associated with detectors
    * making measurements on a specified material at a specific beam energy.
    * 
    * @author nicholas
    */
   public class QCProject
      implements
      Comparable<QCProject> {
      final int mIndex;
      final int mDetectorIdx;
      final String mStandard; // "__QC__["+name+"]"
      final double mBeamEnergy;
      final QCNormalizeMode mMode;
      final double mNominalWD;
      final double mNominalI;

      transient EDSDetector mDetector;
      transient Composition mComposition;
      transient TreeSet<QCEntry> mEntries;
      transient TreeMap<String, Integer> mQCDatumIdx = new TreeMap<String, Integer>();

      private int getQCDatumIdx(String name)
            throws SQLException {
         Integer res = mQCDatumIdx.get(name);
         if(res == null) {
            {
               final PreparedStatement ps = mConnection.prepareStatement("SELECT ID FROM QC_DATUM WHERE NAME=? AND PROJECT=?");
               ps.setString(1, name);
               ps.setInt(2, mIndex);
               try (final ResultSet rs = ps.executeQuery()) {
                  if(rs.next())
                     res = Integer.valueOf(rs.getInt(1));
               }
            }
            if(res == null) {
               final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO QC_DATUM ( ID, NAME, PROJECT ) VALUES ( DEFAULT, ?, ? )", Statement.RETURN_GENERATED_KEYS);
               ps.setString(1, name);
               ps.setInt(2, mIndex);
               ps.executeUpdate();
               try (final ResultSet rs = ps.getGeneratedKeys()) {
                  res = Integer.valueOf(rs.next() ? rs.getInt(1) : -1);
               }
            }
            mQCDatumIdx.put(name, res);
         }
         return res.intValue();
      }

      private QCProject(EDSDetector det, String std, double beamE)
            throws SQLException,
            EPQException {
         final int detIdx = findDetector(det.getDetectorProperties());
         if(detIdx == -1)
            throw new EPQException("Unknown detector: " + det.getDetectorProperties().toString());
         final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, DETECTOR, STANDARD, BEAM_ENERGY, NORMALIZATION, NOMINAL_WD, NOMINAL_I FROM QC_PROJECT WHERE DETECTOR=? AND STANDARD=? AND BEAM_ENERGY=?");
         ps.setInt(1, detIdx);
         ps.setString(2, toQCName(std));
         ps.setDouble(3, roundBeamEnergy(beamE));
         ps.setFetchSize(1);
         try (final ResultSet rs = ps.executeQuery()) {
            if(!rs.next())
               throw new EPQException("No project found for " + det + " " + std + " " + beamE);
            mIndex = rs.getInt(1);
            mDetector = det;
            mDetectorIdx = rs.getInt(2);
            mStandard = rs.getString(3);
            mBeamEnergy = rs.getDouble(4);
            mMode = (rs.getInt(5) == 0 ? QCNormalizeMode.CURRENT : QCNormalizeMode.TOTAL_COUNTS);
            mNominalWD = rs.getDouble(6);
            mNominalI = rs.getDouble(7);
         }
      }

      private QCProject(int projectIdx)
            throws SQLException,
            EPQException {
         final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, DETECTOR, STANDARD, BEAM_ENERGY, NORMALIZATION, NOMINAL_WD, NOMINAL_I FROM QC_PROJECT WHERE ID=?");
         ps.setInt(1, projectIdx);
         try (final ResultSet rs = ps.executeQuery()) {
            if(!rs.next())
               throw new EPQException("Project " + projectIdx + " does not exist.");
            mIndex = rs.getInt(1);
            mDetectorIdx = rs.getInt(2);
            mStandard = rs.getString(3);
            mBeamEnergy = rs.getDouble(4);
            mMode = (rs.getInt(5) == 0 ? QCNormalizeMode.CURRENT : QCNormalizeMode.TOTAL_COUNTS);
            mNominalWD = rs.getDouble(6);
            mNominalI = rs.getDouble(7);
         }
      }

      private QCProject(ResultSet rs)
            throws SQLException {
         mIndex = rs.getInt(1);
         mDetectorIdx = rs.getInt(2);
         mStandard = rs.getString(3);
         mBeamEnergy = rs.getDouble(4);
         mMode = (rs.getInt(5) == 0 ? QCNormalizeMode.CURRENT : QCNormalizeMode.TOTAL_COUNTS);
         mNominalWD = rs.getDouble(6);
         mNominalI = rs.getDouble(7);
      }

      public EDSDetector getDetector() {
         if(mDetector == null) {
            final DetectorProperties dp = findDetector(mDetectorIdx);
            DetectorCalibration res = null;
            for(final DetectorCalibration dc : getCalibrations(dp))
               if((res == null) || res.getActiveDate().after(dc.getActiveDate()))
                  res = dc;
            mDetector = EDSDetector.createDetector(dp, (EDSCalibration) res);
         }
         return mDetector;
      }

      public Composition getStandard()
            throws SQLException {
         if(mComposition == null) {
            final Composition comp = findStandard(mStandard);
            comp.setName(fromQCName(mStandard));
            mComposition = comp;
         }
         return mComposition;
      }

      public int getIndex() {
         return mIndex;
      }

      public double getBeamEnergy() {
         return mBeamEnergy;
      }

      public boolean matchesBeamEnergy(double beamEnergy) {
         return roundBeamEnergy(beamEnergy) == roundBeamEnergy(mBeamEnergy);
      }

      public QCNormalizeMode getMode() {
         return mMode;
      }

      public double getNominalWD() {
         return mNominalWD;
      }

      public double getNominalI() {
         return mNominalI;
      }

      @Override
      public String toString() {
         final DetectorProperties dp = getDetector().getDetectorProperties();
         final NumberFormat df = new HalfUpFormat("0.0 keV");
         return fromQCName(mStandard) + " at " + df.format(mBeamEnergy) + " on " + dp.toString();
      }

      @Override
      public int compareTo(QCProject o) {
         return mIndex < o.mIndex ? -1 : (mIndex > o.mIndex ? 1 : 0);
      }

      /**
       * Returns a set containing all the data associated with this QC project.
       * 
       * @return Set&lt;QCEntry&gt;
       * @throws SQLException
       * @throws IOException
       * @throws EPQException
       */
      public TreeSet<QCEntry> getEntries()
            throws SQLException, IOException, EPQException {
         if(mEntries == null) {
            final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, PROJECT, CREATED, SPECTRUM FROM QC_ENTRY WHERE PROJECT=?");
            ps.setInt(1, getIndex());
            try (final ResultSet rs = ps.executeQuery()) {
               final TreeSet<QCEntry> res = new TreeSet<QCEntry>();
               while(rs.next())
                  res.add(new QCEntry(rs));
               mEntries = res;
            }
         }
         return mEntries;
      }

      /**
       * Returns a list of the QC items
       * 
       * @return Set&lt;QCEntry&gt;
       * @throws SQLException
       * @throws IOException
       * @throws EPQException
       */
      public Set<String> getItemNames()
            throws SQLException, IOException, EPQException {
         final TreeSet<String> res = new TreeSet<String>();
         final PreparedStatement ps = mConnection.prepareStatement("SELECT NAME FROM QC_DATUM WHERE PROJECT=?");
         ps.setInt(1, mIndex);
         try (final ResultSet rs = ps.executeQuery()) {
            while(rs.next())
               res.add(rs.getString(1));
         }
         return res;
      }

      /**
       * Returns a single entry from this QCProject by index. If you need many
       * entries getEntries() is a better bet.
       * 
       * @param qce
       * @return QCEntry
       * @throws SQLException
       * @throws IOException
       * @throws EPQException
       */
      public QCEntry getEntry(int qce)
            throws SQLException, IOException, EPQException {
         if(mEntries == null) {
            final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, PROJECT, CREATED, SPECTRUM FROM QC_ENTRY WHERE PROJECT=? AND ID=?");
            ps.setInt(1, getIndex());
            ps.setInt(2, qce);
            try (final ResultSet rs = ps.executeQuery()) {
               if(rs.next())
                  return new QCEntry(rs);
            }
         } else
            for(final QCEntry qe : mEntries)
               if(qe.getIndex() == qce)
                  return qe;
         return null;
      }

      /**
       * Returns a list of indexes to the QC entries for the specified
       * EDSDetector, Composition and beam energy. Does not read the entries
       * into memory.
       * 
       * @param projectIdx - Use getQCProject or findQCProjects to get this
       *           index
       * @return int[] An array of indexes of QC entries
       * @throws SQLException
       */
      private int[] getEntryIndexes()
            throws SQLException {
         final int[] res;
         if(mEntries != null) {
            res = new int[mEntries.size()];
            int i = 0;
            for(final QCEntry qce : mEntries)
               res[i++] = qce.getIndex();
         } else {
            final PreparedStatement ps = mConnection.prepareStatement("SELECT ID FROM QC_ENTRY WHERE PROJECT=? ORDER BY CREATED");
            ps.setInt(1, getIndex());
            try (final ResultSet rs = ps.executeQuery()) {
               final ArrayList<Integer> tmp = new ArrayList<Integer>();
               while(rs.next())
                  tmp.add(Integer.valueOf(rs.getInt(1)));
               res = new int[tmp.size()];
               for(int i = 0; i < res.length; ++i)
                  res[i] = tmp.get(i).intValue();
            }
         }
         return res;
      }

      /**
       * Computes the DescriptiveStatistics for the last (or first) n QCEntry
       * objects in this QCProject.
       * 
       * @param n The number of QCEntry objects to include
       * @param last True for last n entries and false for first n entries
       * @return TreeMap&lt;String,DescriptiveString&gt;
       * @throws SQLException
       * @throws IOException
       * @throws EPQException
       */
      public TreeMap<String, DescriptiveStatistics> getEntryStatistics(int n, boolean last)
            throws SQLException, IOException, EPQException {
         final Set<QCEntry> ent = last ? getEntries().descendingSet() : getEntries();
         final TreeMap<String, DescriptiveStatistics> res = new TreeMap<String, DescriptiveStatistics>();
         int i = 0;
         for(final QCEntry qc : ent) {
            if(i == n)
               break;
            for(final Map.Entry<String, UncertainValue2> me : qc.getData().entrySet()) {
               DescriptiveStatistics ds = res.get(me.getKey());
               if(ds == null) {
                  ds = new DescriptiveStatistics();
                  res.put(me.getKey(), ds);
               }
               ds.add(me.getValue().doubleValue());
            }
            ++i;
         }
         return res;
      }

      /**
       * Writes a summary of all the QC entries associated with this QC project
       * to the Writer specified. Normally this writer is a FileWriter(..) and
       * the output is in "UTF-8".
       * 
       * @param wr
       * @throws SQLException
       * @throws IOException
       * @throws EPQException
       */
      public void write(Writer wr)
            throws SQLException, IOException, EPQException {
         wr.append(toString() + "\n");
         final Set<QCEntry> entries = getEntries();
         final Set<String> columns = new TreeSet<String>();
         QCEntry last = null;
         for(final QCEntry qc : entries) {
            final Map<String, UncertainValue2> entry = qc.getData();
            for(final String name : entry.keySet())
               columns.add(name);
            last = qc;
         }
         final StringBuffer sb = new StringBuffer();
         final TreeMap<String, DescriptiveStatistics> stats = new TreeMap<String, DescriptiveStatistics>();
         sb.append("Timestamp\tIndex");
         for(final String column : columns) {
            sb.append("\t");
            sb.append(column);
            sb.append("\td");
            sb.append(column);
            stats.put(column, new DescriptiveStatistics());
         }
         sb.append("\n");
         wr.append(sb.toString());
         for(final QCEntry qc : entries) {
            sb.setLength(0);
            final Timestamp ts = qc.getTimestamp();
            sb.append(ts);
            sb.append("\t");
            sb.append(qc.getIndex());
            final Map<String, UncertainValue2> entry = qc.getData();
            for(final String column : columns) {
               final UncertainValue2 uv = entry.get(column);
               stats.get(column).add(uv.doubleValue());
               sb.append("\t");
               sb.append(uv.doubleValue());
               sb.append("\t");
               sb.append(uv.uncertainty());
            }
            sb.append("\n");
            wr.append(sb.toString());
         }
         final StringBuffer min = new StringBuffer();
         final StringBuffer max = new StringBuffer();
         final StringBuffer avg = new StringBuffer();
         final StringBuffer std = new StringBuffer();
         min.append("\tMinimum");
         max.append("\tMaximum");
         avg.append("\tAverage");
         std.append("\tStd. Dev.");
         for(final String column : columns) {
            final DescriptiveStatistics ds = stats.get(column);
            min.append("\t");
            min.append(ds.minimum());
            min.append("\t");
            max.append("\t");
            max.append(ds.maximum());
            max.append("\t");
            avg.append("\t");
            avg.append(ds.average());
            avg.append("\t");
            std.append("\t");
            std.append(ds.standardDeviation());
            std.append("\t");
         }
         min.append("\n");
         max.append("\n");
         avg.append("\n");
         std.append("\n");
         wr.append(min);
         wr.append(max);
         wr.append(avg);
         wr.append(std);
         if(last != null) { // Compare last to avg and stddev
            final Map<String, UncertainValue2> lastData = last.getData();
            sb.setLength(0);
            sb.append("Last (Delta & N-Sigma)\t");
            sb.append(last.getIndex());
            for(final String column : columns) {
               final double val = lastData.get(column).doubleValue();
               final DescriptiveStatistics ds = stats.get(column);
               sb.append("\t");
               sb.append(val - ds.average());
               sb.append("\t");
               sb.append((val - ds.average()) / ds.standardDeviation());
            }
            sb.append("\n");
            wr.append(sb.toString());
         }
      }

      /**
       * <p>
       * Add the appropriate records to the QC database. Use
       * <code>SpectrumFitter7.performQC(det, comp, spec, al)</code> to
       * construct the data item.
       * </p>
       * 
       * @param spec A QC spectrum
       * @param data The data items associated with spec
       * @throws SQLException
       * @throws EPQException
       * @throws AlreadyInDatabaseException
       * @throws IOException
       * @throws FileNotFoundException
       */
      public QCEntry addMeasurement(ISpectrumData spec, Map<String, UncertainValue2> data)
            throws SQLException, EPQException, FileNotFoundException, IOException, AlreadyInDatabaseException {
         final SpectrumProperties sp = spec.getProperties();
         final double beamE = roundBeamEnergy(sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN));
         if(beamE != mBeamEnergy)
            throw new EPQException("The beam energy on the measured QC spectrum does not match the QC project.");
         // Find the appropriate
         final Date dt = sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new Date(System.currentTimeMillis()));
         final Timestamp ts = new Timestamp(dt.getTime());
         int entryIdx = Integer.MAX_VALUE;
         {
            final int specIdx = addSpectrum(spec, true);
            final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO QC_ENTRY ( ID, CREATED, PROJECT, SPECTRUM ) VALUES ( DEFAULT, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, ts);
            ps.setInt(2, getIndex());
            ps.setInt(3, specIdx);
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys()) {
               if(rs.next())
                  entryIdx = rs.getInt(1);
            }
         }
         // Update the
         final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO QC_ITEM ( ID, ENTRY, DATUM, QC_VALUE, QC_UNC ) VALUES ( DEFAULT, ?, ?, ?, ? )");
         for(final Map.Entry<String, UncertainValue2> me : data.entrySet()) {
            final String name = me.getKey();
            final UncertainValue2 uv = me.getValue();
            ps.clearParameters();
            ps.setInt(1, entryIdx);
            ps.setInt(2, getQCDatumIdx(name));
            ps.setDouble(3, uv.doubleValue());
            ps.setDouble(4, uv.uncertainty());
            ps.executeUpdate();
         }
         final QCEntry qce = new QCEntry(entryIdx);
         if(mEntries != null)
            mEntries.add(qce);
         return qce;
      }

      public void setControlLimit(String name, UncertainValue2 uc)
            throws SQLException {
         final PreparedStatement ps = mConnection.prepareStatement("UPDATE QC_DATUM SET NOMINAL=?, TOLERANCE=? WHERE NAME=? AND PROJECT=?");
         ps.setDouble(1, uc.doubleValue());
         ps.setDouble(2, uc.uncertainty());
         ps.setString(3, name);
         ps.setInt(4, getIndex());
         ps.executeUpdate();
      }

      public UncertainValue2 getControlLimit(String name)
            throws SQLException {
         final PreparedStatement ps = mConnection.prepareStatement("SELECT NOMINAL, TOLERANCE FROM QC_DATUM WHERE NAME=? AND PROJECT=?");
         ps.setString(1, name);
         ps.setInt(1, getIndex());
         try (final ResultSet rs = ps.executeQuery()) {
            if(rs.next())
               return new UncertainValue2(rs.getDouble(1), name, rs.getDouble(2));
         }
         return null;
      }
   }

   public enum QCNormalizeMode {
      CURRENT("Current"), TOTAL_COUNTS("Total counts");

      final String mName;

      QCNormalizeMode(String name) {
         mName = name;
      }

      @Override
      public String toString() {
         return mName;
      }
   };

   /**
    * Create a new QCProject associated with the specified detector, standard
    * and beam energy.
    * 
    * @param det EDSDetector
    * @param std A material on which the spectra will be collected
    * @param beamE The beam energy at which the spectra will be collected
    * @param mode SpectrumFitter7.QCNormalization
    * @param nominalWD Working distance in mm
    * @param nominalI Probe current in nA
    * @return QCProject
    * @throws SQLException
    * @throws EPQException
    */
   public QCProject createQCProject(DetectorProperties det, Composition std, double beamE, QCNormalizeMode mode, double nominalWD, double nominalI)
         throws SQLException, EPQException {
      final String qcname = toQCName(std.getName());
      if(findStandard(qcname) == null) {
         final Composition qcStd = std.clone();
         qcStd.setName(qcname);
         addStandard(qcStd);
      }
      final PreparedStatement ps = mConnection.prepareStatement("INSERT INTO QC_PROJECT ( ID, DETECTOR, STANDARD, BEAM_ENERGY, NORMALIZATION, NOMINAL_WD, NOMINAL_I ) VALUES ( DEFAULT, ?, ?, ?, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, findDetector(det));
      ps.setString(2, qcname);
      ps.setDouble(3, roundBeamEnergy(beamE));
      ps.setInt(4, mode.ordinal());
      ps.setDouble(5, nominalWD);
      ps.setDouble(6, nominalI);
      ps.executeUpdate();
      try (final ResultSet rs = ps.getGeneratedKeys()) {
         if(rs.next()) {
            final int id = rs.getInt(1);
            final QCProject qcp = new QCProject(id);
            mQCProjects.put(id, qcp);
            return qcp;
         }
      }
      return null;
   }

   /**
    * Returns a set of DetectorProperties for detectors with associated QC
    * projects.
    * 
    * @return Set&lt;DetectorProperties&gt;
    * @throws SQLException
    */
   public Set<DetectorProperties> findDetectorsWithQCProjects()
         throws SQLException {
      final Set<DetectorProperties> res = new HashSet<DetectorProperties>();
      final PreparedStatement ps = mConnection.prepareStatement("SELECT DISTINCT DETECTOR FROM QC_PROJECT");
      try (final ResultSet rs = ps.executeQuery()) {
         while(rs.next())
            try {
               res.add(findDetector(rs.getInt(1)));
            }
            catch(final Exception e) {
               e.printStackTrace();
            }
      }
      return Collections.unmodifiableSet(res);
   }

   /**
    * Returns the QCProject associated with the specified index. If the index is
    * not associated with a QCProject then the method throws an EPQException.
    * 
    * @param projectIdx
    * @return QCProject
    * @throws SQLException
    * @throws EPQException
    */
   public QCProject getQCProject(int projectIdx)
         throws SQLException, EPQException {
      QCProject res = mQCProjects.get(Integer.valueOf(projectIdx));
      if(res == null) {
         res = new QCProject(projectIdx);
         mQCProjects.put(Integer.valueOf(projectIdx), res);
      }
      return res;
   }

   /**
    * Finds the first, existing QCProject associated with the specified
    * EDSDetector, standard and beam energy. If a QCProject does not exist then
    * the method throws an EPQException.
    * 
    * @param det EDSDetector
    * @param std The name of the standard
    * @param beamE The beam energy in keV
    * @return QCProject
    * @throws SQLException
    * @throws EPQException
    */
   public QCProject getQCProject(EDSDetector det, String std, double beamE)
         throws SQLException, EPQException {
      final int detIdx = findDetector(det.getDetectorProperties());
      beamE = roundBeamEnergy(beamE);
      for(final QCProject qcp : mQCProjects.values())
         if((qcp.mDetectorIdx == detIdx) && qcp.mStandard.equals(toQCName(std)) && (qcp.mBeamEnergy == beamE))
            return qcp;
      for(final QCProject qcp : findQCProjects(det.getDetectorProperties()))
         if((qcp.mDetectorIdx == detIdx) && qcp.mStandard.equals(toQCName(std)) && (qcp.mBeamEnergy == beamE))
            return qcp;
      final QCProject res = new QCProject(det, std, beamE);
      mQCProjects.put(res.getIndex(), res);
      return res;
   }

   /**
    * Returns a map of QC projects associate with the EDSDetector.
    * 
    * @param det
    * @return Map&lt;Integer,String&gt; Maps a project index into a user
    *         friendly description
    * @throws SQLException
    */
   public Set<QCProject> findQCProjects(DetectorProperties det)
         throws SQLException {
      final TreeSet<QCProject> res = new TreeSet<QCProject>();
      final int detIdx = findDetector(det);
      final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, DETECTOR, STANDARD, BEAM_ENERGY, NORMALIZATION, NOMINAL_WD, NOMINAL_I FROM QC_PROJECT WHERE DETECTOR=?");
      ps.setInt(1, detIdx);
      try (final ResultSet rs = ps.executeQuery()) {
         while(rs.next()) {
            final QCProject qcp = new QCProject(rs);
            res.add(qcp);
            mQCProjects.put(qcp.getIndex(), qcp);
         }
      }
      return res;
   }

   /**
    * Gets an EDSDetector with the same DetectorProperties as det but with the
    * earliest possible calibration.
    * 
    * @param det
    * @return EDSDetector
    */
   public EDSDetector getEarliestCalibrated(EDSDetector det) {
      final DetectorProperties dp = det.getDetectorProperties();
      DetectorCalibration res = null;
      for(final DetectorCalibration dc : getCalibrations(dp))
         if((res == null) || res.getActiveDate().after(dc.getActiveDate()))
            res = dc;
      return EDSDetector.createDetector(dp, (EDSCalibration) res);
   }

   /**
    * Used to remove unwanted QC entries from the database.
    * 
    * @param entryIdx
    * @throws SQLException
    */
   public void deleteQCEntry(int entryIdx)
         throws SQLException {
      {
         final PreparedStatement ps = mConnection.prepareStatement("DELETE FROM SPECTRUM WHERE QC_ENTRY.SPECTRUM=SPECTRUM.ID AND QC_ENTRY.PROJECT=?");
         ps.setInt(1, entryIdx);
         ps.executeUpdate();
      }
      {
         final PreparedStatement ps = mConnection.prepareStatement("DELETE FROM QC_ITEM WHERE ENTRY=?");
         ps.setInt(1, entryIdx);
         ps.executeUpdate();
      }
      {
         final PreparedStatement ps = mConnection.prepareStatement("DELETE FROM QC_ENTRY WHERE ID=?");
         ps.setInt(1, entryIdx);
         ps.executeUpdate();
      }
   }

   /**
    * Deletes all the associated entries, spectra and then the project from the
    * database.
    * 
    * @param projIdx
    * @throws SQLException
    * @throws EPQException
    */
   public void deleteQCProject(int projIdx)
         throws SQLException, EPQException {
      if(mQCProjects.containsKey(Integer.valueOf(projIdx)))
         mQCProjects.remove(Integer.valueOf(projIdx));
      final int[] es = getQCProject(projIdx).getEntryIndexes();
      for(final int e : es)
         deleteQCEntry(e);
      {
         final PreparedStatement ps = mConnection.prepareStatement("DELETE FROM QC_PROJECT WHERE ID=?");
         ps.setInt(1, projIdx);
         ps.executeUpdate();
      }
   }

   /**
    * Returns the metrics associated with a specific QC entry in the form of a
    * set of name, value pairs.
    * 
    * @param entryIdx
    * @return TreeMap&lt;String, UncertainValue&gt;
    * @throws SQLException
    */
   private Map<String, UncertainValue2> getQCEntryData(int entryIdx)
         throws SQLException {
      final TreeMap<String, UncertainValue2> res = new TreeMap<String, UncertainValue2>();
      final PreparedStatement ps = mConnection.prepareStatement("SELECT QC_DATUM.NAME, QC_ITEM.QC_VALUE, QC_ITEM.QC_UNC FROM QC_DATUM, QC_ITEM WHERE QC_DATUM.ID=QC_ITEM.DATUM AND QC_ITEM.ENTRY=?");
      ps.setInt(1, entryIdx);
      try (final ResultSet rs = ps.executeQuery()) {
         while(rs.next())
            res.put(rs.getString(1), new UncertainValue2(rs.getDouble(2), rs.getString(1), rs.getDouble(3)));
      }
      return Collections.unmodifiableMap(res);
   }

   /**
    * Represents an entry in the QC database associate with a single QC
    * measurement.
    * 
    * @author nicholas
    */
   public class QCEntry
      implements
      Comparable<QCEntry> {
      private final int mProject;
      private final Timestamp mCreated;
      private final int mId;
      private final int mSpecIdx;
      transient ISpectrumData mSpectrum = null;
      transient Map<String, UncertainValue2> mMeasurements = null;

      private QCEntry(int entryIdx)
            throws SQLException {
         final PreparedStatement ps = mConnection.prepareStatement("SELECT ID, PROJECT, CREATED, SPECTRUM FROM QC_ENTRY WHERE ID=?");
         ps.setInt(1, entryIdx);
         try (final ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
               mId = rs.getInt(1);
               mProject = rs.getInt(2);
               mCreated = rs.getTimestamp(3);
               mSpecIdx = rs.getInt(4);
            } else {
               mId = entryIdx;
               mProject = -1;
               mCreated = null;
               mSpecIdx = -1;
            }
         }
      }

      private QCEntry(ResultSet rs)
            throws SQLException {
         mId = rs.getInt(1);
         mProject = rs.getInt(2);
         mCreated = rs.getTimestamp(3);
         mSpecIdx = rs.getInt(4);
      }

      /**
       * Returns the spectrum associated with this QC measurement.
       * 
       * @return ISpectrumData
       * @throws SQLException
       * @throws IOException
       * @throws EPQException
       */
      public ISpectrumData getSpectrum()
            throws SQLException, IOException, EPQException {
         if(mSpectrum == null) {
            final ISpectrumData res = readSpectrum(mSpecIdx);
            SpectrumUtils.rename(res, "QC[" + getQCProject(mProject).toString() + "," + mCreated + "," + mId + "]");
            mSpectrum = res;
         }
         return mSpectrum;
      }

      @Override
      public String toString() {
         try {
            return "QC[" + getQCProject(mProject).toString() + "," + mCreated + "," + mId + "]";
         }
         catch(final Exception e) {
            return "QC[Project[" + mProject + "]," + mCreated + "," + mId + "]";
         }
      }

      /**
       * Returns the project with which this project is associated.
       * 
       * @return QCProject
       * @throws SQLException
       * @throws EPQException
       */
      public QCProject getProject()
            throws SQLException, EPQException {
         return getQCProject(mProject);
      }

      public int getIndex() {
         return mId;
      }

      /**
       * Returns the time at which this QCEntry was measured.
       * 
       * @return Timestamp
       */
      public Timestamp getTimestamp() {
         return mCreated;
      }

      /**
       * Returns a map containing the QC data metrics.
       * 
       * @return Map&lt;String,UncertainValue&gt;
       * @throws SQLException
       */
      public Map<String, UncertainValue2> getData()
            throws SQLException {
         if(mMeasurements == null)
            mMeasurements = getQCEntryData(mId);
         return mMeasurements;
      }

      @Override
      public int compareTo(QCEntry o) {
         int res = mCreated.compareTo(o.mCreated);
         if(res == 0)
            res = mId < o.mId ? -1 : (mId > o.mId ? 1 : 0);
         return res;
      }
   }

   private String[] csvSplitter(String str) {
      final ArrayList<String> res = new ArrayList<String>();
      final char[] ca = str.toCharArray();
      final StringBuffer sb = new StringBuffer();
      boolean skip = false;
      for(int i = 0; i < ca.length; ++i)
         if(ca[i] == '\"') {
            if(((i > 0) && (ca[i - 1] == '\"')))
               sb.append(ca[i]);
            skip = !skip;
         } else if(!skip) {
            if(ca[i] == ',') {
               res.add(sb.toString().trim());
               sb.setLength(0);
            } else
               sb.append(ca[i]);
         } else
            sb.append(ca[i]);
      return res.toArray(new String[res.size()]);
   }

   public void importMaterialCSV(File f)
         throws EPQException, SQLException {
      try {
         try (final FileInputStream fis = new FileInputStream(f)) {
            try (final Reader isr = new InputStreamReader(fis, "US-ASCII")) {
               try (final BufferedReader br = new BufferedReader(isr)) {
                  String str = br.readLine();
                  if((str == null) || (!str.trim().toLowerCase().startsWith("name,")))
                     throw new EPQException("This file does not apprear to be a materials CSV database");
                  for(str = br.readLine(); str != null; str = br.readLine()) {
                     double density = Double.NaN;
                     final String[] items = csvSplitter(str);
                     if(items[1] != "-")
                        try {
                           density = Double.parseDouble(items[1].trim());
                        }
                        catch(final NumberFormatException e) {
                           density = Double.NaN;
                           // Ignore it...
                        }
                     final Composition comp = new Composition();
                     final String name = (items[0].toUpperCase().replace("NBS GLASS ", "NIST ")).replace("K ", "K");
                     comp.setName(name);
                     for(int i = 2; i < items.length; ++i)
                        try {
                           final double wf = Double.parseDouble(items[i].trim());
                           if(wf > 0.0)
                              comp.addElement(Element.byAtomicNumber(i - 1), wf);
                        }
                        catch(final NumberFormatException e) {
                           // Ignore it...
                        }
                     if((findStandard(comp.getName()) == null) && (Math.abs(1.0 - comp.sumWeightFraction()) < 0.2))
                        addStandard(Double.isNaN(density) ? comp : new Material(comp, 1000.0 * density));
                     else
                        System.out.println("Not adding: " + comp.descriptiveString(false));
                  }
               }
            }
         }
      }
      catch(final IOException ex) {
         throw new EPQException(ex);
      }
   }

   public void exportMaterialCSV(File f)
         throws IOException, SQLException {
      try (final FileWriter fw = new FileWriter(f)) {
         final Set<String> names = getStandards().keySet();
         final StringBuffer sb = new StringBuffer();
         sb.append("Name, Density");
         for(int z = Element.elmH; z <= Element.elmPu; ++z) {
            sb.append(", ");
            sb.append(Element.byAtomicNumber(z).toAbbrev());
         }
         sb.append("\n");
         fw.write(sb.toString());
         for(final String name : names) {
            final Composition comp = findStandard(name);
            if(comp != null) {
               sb.setLength(0);
               sb.append(name);
               sb.append(", ");
               if(comp instanceof Material)
                  sb.append(Double.toString(0.001 * ((Material) comp).getDensity()));
               else
                  sb.append("\"-\"");
               for(int z = Element.elmH; z <= Element.elmPu; ++z) {
                  sb.append(", ");
                  sb.append(Double.toString(comp.weightFraction(Element.byAtomicNumber(z), false)));
               }
            }
            sb.append("\n");
         }
         fw.write(sb.toString());
      }
   }
};

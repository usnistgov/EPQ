package gov.nist.microanalysis.EPQTools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A mechanism for exporting spectra to a file which can be imported into
 * GnuPlot to produce a publication quality plot of EDS data.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class GnuplotFile {

   static private String mGnuPlotPath = readDefaultPath();
   static private String mDefaultTerminal = readDefaultTerminal();
   static private final String CHAR_SET = "US-ASCII";
   static private final String GNU_PLOT = "GnuPlot";
   static private final String GNU_PLOT_TERMINAL = "Terminal";
   private static final double STAGGER_X = 0.025;
   private static final double STAGGER_Y = 0.05;

   private final ArrayList<ISpectrumData> mSpectra;
   private final ArrayList<String> mTitles;
   private final TreeMap<KLMLine, String> mLines;
   private boolean mAutoX, mAutoY;
   private boolean mLogY;
   private double mXMin, mXMax;
   private double mYMin, mYMax;
   private String mTerminal;
   private boolean mStagger;
   private boolean mLabelYAxis;

   static private String readDefaultPath() {
      final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
      final String res = isWindows ? "C:\\Program Files\\gnuPlot\\bin\\wgnuplot.exe" : "/usr/bin/gnuplot";
      final Preferences pref = Preferences.userNodeForPackage(GnuplotFile.class);
      return pref.get(GNU_PLOT, res);
   }

   static private String readDefaultTerminal() {
      final String res = "eps";
      final Preferences pref = Preferences.userNodeForPackage(GnuplotFile.class);
      return pref.get(GNU_PLOT_TERMINAL, res);
   }

   /**
    * Is gnuPlot installed and correctly configured to permit generating gnuplot
    * output from within Trixy?
    * 
    * @return boolean
    */
   static public boolean generateSupported() {
      final File gnuPlot = new File(mGnuPlotPath);
      return gnuPlot.exists();
   }

   public GnuplotFile() {
      super();
      mSpectra = new ArrayList<ISpectrumData>();
      mTitles = new ArrayList<String>();
      mLines = new TreeMap<KLMLine, String>();
      mAutoX = true;
      mAutoY = true;
      mLogY = false;
      mTerminal = "";
      mStagger = false;
      mLabelYAxis = true;
   }

   public void addSpectrum(ISpectrumData spec) {
      mTitles.add(spec.toString());
      mSpectra.add(spec);
   }

   public void addSpectrum(ISpectrumData spec, String title) {
      mTitles.add(title);
      mSpectra.add(spec);
   }

   private double scale(double x, boolean up) {
      if (x < 1.0e-6)
         return x;
      final double den = Math.pow(10.0, Math.floor(Math.log10(x)));
      if (up)
         return (den * Math.ceil((10.0 * x) / den)) / 10.0;
      else
         return (den * Math.floor((10.0 * x) / den)) / 10.0;
   }

   private double transformY(double ii, double maxY, double minY, int i) {
      if (mStagger) {
         if (mLogY) {
            final double k = Math.pow(10.0, (Math.log10(maxY) - Math.log10(minY)) * STAGGER_Y);
            return ii * (1.0 + (i * k));
         } else
            return ii + (i * (maxY - minY) * STAGGER_Y);
      } else
         return ii;
   }

   private double transformX(double xx, double dx, int i) {
      return mStagger ? xx + (i * dx) : xx;
   }

   public void write(PrintWriter pw) throws IOException {
      final boolean isLatex = mTerminal.startsWith("latex");
      final NumberFormat nf = new HalfUpFormat("0.0");
      double minX = mXMin, maxX = mXMax;
      if (mAutoX) {
         minX = 0.0;
         maxX = 1.0e3; // eV
         for (final ISpectrumData spec : mSpectra) {
            final double max = SpectrumUtils.maxEnergyForChannel(spec, SpectrumUtils.maxChannel(spec));
            if (max > maxX)
               maxX = max;
         }
      }
      if (mStagger)
         maxX += (mSpectra.size() - 1) * (maxX - minX) * STAGGER_X;
      minX = scale(minX, false);
      maxX = scale(maxX, true);
      double minY = mYMin, maxY = mYMax;
      if (mAutoY) {
         minY = 0.0;
         maxY = 16.0;
         for (final ISpectrumData spec : mSpectra) {
            final double max = spec.getCounts(SpectrumUtils.maxChannel(spec));
            if (max > maxY)
               maxY = max;
         }
      }
      if (mStagger)
         maxY += (mSpectra.size() - 1) * (maxY - minY) * STAGGER_Y;
      minY = scale(minY, false);
      maxY = scale(maxY, true);
      // Trim KLM lines down
      final HashMap<double[], String> displayed = new HashMap<double[], String>();
      {
         final TreeSet<KLMLine> lines = new TreeSet<KLMLine>();
         lines.addAll(mLines.keySet());
         while (!lines.isEmpty()) {
            final TreeSet<KLMLine> fam = new TreeSet<KLMLine>();
            final KLMLine l = lines.first();
            for (final KLMLine klm : lines) {
               final AtomicShell ks = klm.getShell();
               final AtomicShell ls = l.getShell();
               if (ks.getElement().equals(ls.getElement()) && (ks.getFamily() == ls.getFamily()))
                  fam.add(klm);
            }
            KLMLine mi = fam.first();
            for (final KLMLine klm : fam)
               if (klm.getAmplitude() > mi.getAmplitude())
                  mi = klm;
            double maxI = 0.0;
            for (final ISpectrumData spec : mSpectra) {
               final int ch = SpectrumUtils.channelForEnergy(spec, FromSI.eV(mi.getEnergy()));
               for (int j = (ch - 5) >= 0 ? ch - 5 : 0; j < (ch + 5); j++)
                  if (j < spec.getChannelCount()) {
                     final double cc = spec.getCounts(j);
                     if (cc > maxI)
                        maxI = cc;
                  }
            }
            for (final KLMLine klm : fam)
               displayed.put(new double[]{maxI * (klm.getAmplitude() / mi.getAmplitude()), klm.getEnergy()}, mLines.get(klm));
            lines.removeAll(fam);
         }
      }
      pw.println("# Generated by the NIST EPQ Library");
      pw.println("# On " + DateFormat.getDateInstance().format(new Date()));
      pw.println("# By " + System.getProperty("user.name"));
      pw.println("# NOTE: TO SAVE FILES AS ENCAPSULATED POST SCRIPT FILES, OR AS OTHER FILE FORMATS,");
      pw.println("#     COMMENT OUT THE \"TERMINAL WINDOW\" SECTION, SET THE TERMINAL AS");
      pw.println("#     THE CHOSEN TYPE, AND SET THE OUTPUT.");
      if (!mTerminal.startsWith("post"))
         pw.println("# set terminal postscript enhanced \"Times-Roman\" 12");
      if (!mTerminal.startsWith("png"))
         pw.println("# set terminal png");
      if (!mTerminal.startsWith("window"))
         pw.println("# set terminal window");
      if (mTerminal.startsWith("latex"))
         pw.println("set size 1.2, 0.6");
      if (mTerminal.length() > 0)
         pw.println("set terminal " + mTerminal);
      if (mLogY) {
         pw.println("unset logscale x");
         pw.println("set logscale y");
         minY = 1.0;
      } else
         pw.println("unset logscale xy");
      // X Range
      pw.print("set xrange [");
      pw.print(nf.format(minX));
      pw.print(":");
      pw.print(nf.format(maxX));
      pw.println("]");
      // Y Range
      pw.print("set yrange [");
      pw.print(nf.format(minY));
      pw.print(":");
      pw.print(nf.format(maxY));
      pw.println("]");
      // Energy label
      if (mTerminal.equalsIgnoreCase("window")) {
         pw.println("set xlabel \"(Energy) eV\" font \"Arial,16\"");
         pw.println("set ylabel \"\" font \"Arial,16\"");
      } else {
         pw.println("set xlabel \"(Energy) eV\"");
         pw.println("set ylabel \"\"");
      }
      pw.println("set format x \"" + (isLatex ? "$%g$" : "%g") + "\"");
      pw.println("set format y \"" + (mLabelYAxis ? (isLatex ? "$%g$" : "%g") : "") + "\"");
      // Add klm labels
      int i = 1;
      pw.println("unset label");
      for (final Map.Entry<double[], String> klm : displayed.entrySet()) {
         // Add a label
         pw.println("# " + klm.getValue());
         pw.print("set label ");
         pw.print(i);
         if (mTerminal.startsWith("latex")) {
            pw.print(" \"{\\\\tiny ");
            pw.print(klm.getValue());
            pw.print("}\" at first ");
         } else {
            pw.print(" \"");
            pw.print(klm.getValue());
            pw.print("\" at first ");
         }
         final double[] value = klm.getKey();
         final double e = FromSI.eV(value[1]);
         pw.print(nf.format(e));
         pw.print(",");
         pw.print(nf.format(value[0] + ((maxY - minY) / 50)));
         pw.println(" center");
         ++i;
      }
      i = 0;
      // Plot spectra
      for (final String title : mTitles) {
         pw.print(i == 0 ? "plot " : ", ");
         pw.print("\"-\" with lines ");
         if (isLatex)
            pw.print("lt -1");
         pw.print(" title \"");
         pw.print(title);
         pw.print("\"");
         ++i;
      }
      // This is the klm lines
      if (displayed.size() > 0)
         pw.print(", \"-\" with impulses lt -1 notitle");
      pw.println();
      // Add the spectrum data...
      i = 0;
      for (final ISpectrumData spec : mSpectra) {
         pw.println("# " + spec.toString());
         final int minCh = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, minX));
         final int maxCh = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, maxX));
         for (int ch = minCh; ch < maxCh; ++ch) {
            pw.print(nf.format(transformX(SpectrumUtils.avgEnergyForChannel(spec, ch), (maxX - minX) * STAGGER_X, i)));
            pw.print(", ");
            pw.println(nf.format(transformY(spec.getCounts(ch), maxY, minY, i)));
         }
         pw.println("e");
         ++i;
      }
      // Add klm line positions
      if (displayed.size() > 0) {
         for (final Map.Entry<double[], String> klm : displayed.entrySet()) {
            final double[] value = klm.getKey();
            final double e = FromSI.eV(value[1]);
            if ((e > minX) && (e < maxX)) {
               pw.println("# " + klm.getValue());
               pw.print(nf.format(e));
               pw.print(", ");
               pw.println(nf.format(value[0]));
            }
         }
         pw.println("e");
      }
      pw.flush();
   }

   public void setAutoScaleX() {
      mAutoX = true;
   }

   public void setAutoScaleY() {
      mAutoY = true;
   }

   public void setXRange(double xMin, double xMax) {
      mAutoX = false;
      if (xMin > xMax) {
         final double tmp = xMin;
         xMin = xMax;
         xMax = tmp;
      }
      mXMin = xMin;
      mXMax = xMax;
   }

   public void setYRange(double yMin, double yMax) {
      mAutoY = false;
      if (yMin > yMax) {
         final double tmp = yMin;
         yMin = yMax;
         yMax = tmp;
      }
      mYMin = yMin;
      mYMax = yMax;
   }

   public void setLogCounts(boolean b) {
      mLogY = b;
   }

   public void addKLMLine(KLMLine line, String name) {
      mLines.put(line, name);
   }

   static public void setGnuPlotPath(String path) {
      if (!path.equals(mGnuPlotPath)) {
         final File gnuPlot = new File(path);
         if (gnuPlot.exists()) {
            mGnuPlotPath = path;
            final Preferences pref = Preferences.userNodeForPackage(GnuplotFile.class);
            pref.put(GNU_PLOT, mGnuPlotPath);
         }
      }
   }

   private String gnuize(String path) {
      final StringBuffer res = new StringBuffer();
      int begin = 0;
      int end = path.indexOf("\\", 0);
      while (end >= 0) {
         res.append(path.substring(begin, end));
         res.append("/");
         begin = end + 1;
         end = path.indexOf("\\", begin);
      }
      res.append(path.substring(begin));
      return res.toString();
   }

   /**
    * Generate a file by generating a temporary GNUPlot script then executing
    * the script using the static path to GnuPlot.
    * 
    * @param file
    *           A file into which to generate the output
    * @param terminal
    *           A string containing the identifier of one of the GNUPlot
    *           supported terminal devices.
    * @throws IOException
    */
   public void generate(File file, String terminal) throws EPQException, IOException {
      mTerminal = terminal;
      final File newFile = File.createTempFile("gnuPlot", ".plt");
      {
         try (final PrintWriter pw = new PrintWriter(newFile, CHAR_SET)) {
            if (!terminal.equalsIgnoreCase("window"))
               pw.println("set output \'" + gnuize(file.getCanonicalPath()) + "\'");
            write(pw);
            pw.println();
            pw.flush();
         }
      }

      final File gnuPlot = new File(mGnuPlotPath);
      if (gnuPlot.exists()) {
         final String[] cmds = new String[]{gnuPlot.getCanonicalPath(), newFile.getCanonicalPath()};
         final Process p = Runtime.getRuntime().exec(cmds, null, gnuPlot.getParentFile());
         try {
            p.waitFor();
            newFile.deleteOnExit();
         } catch (final InterruptedException e) {
            throw new EPQException("Taking too long to process " + newFile.getAbsolutePath());
         }
         if (p.exitValue() != 0)
            throw new EPQException("Unable to generate output for " + newFile.getAbsolutePath());
      }
   }

   /**
    * Generate a file by generating a temporary GNUPlot script then executing
    * the script using the static path to GnuPlot.
    * 
    * @param file
    *           A file into which to generate the output
    * @throws IOException
    */
   public void generateLatex(File file) throws EPQException, IOException {
      generate(file, "latex");
   }

   /**
    * Generate a file by generating a temporary GNUPlot script then executing
    * the script using the static path to GnuPlot.
    * 
    * @param file
    *           A file into which to generate the output
    * @param options
    *           PNG options to apply following "set terminal PNG "
    * @throws IOException
    */
   public void generatePNG(File file, String options) throws EPQException, IOException {
      generate(file, "png " + options);
   }

   /**
    * Generate a file by generating a temporary GNUPlot script then executing
    * the script using the static path to GnuPlot.
    * 
    * @param file
    *           A file into which to generate the output
    * @param options
    *           PNG options to apply following "set terminal PNG "
    * @throws IOException
    */
   public void generatePostscript(File file, String options) throws EPQException, IOException {
      generate(file, "postscript " + options);
   }

   /**
    * Gets the current value assigned to terminal
    * 
    * @return Returns the terminal.
    */
   public String getTerminal() {
      return mTerminal;
   }

   /**
    * Sets the value assigned to terminal.
    * 
    * @param terminal
    *           The value to which to set terminal.
    */
   public void setTerminal(String terminal) {
      mTerminal = terminal;
   }

   /**
    * Determines whether the multiple spectra are drawn in a right diagonal
    * stairstep pattern.
    * 
    * @return Returns the stagger.
    */
   public boolean isStagger() {
      return mStagger;
   }

   /**
    * Determines whether the multiple spectra are drawn in a right diagonal
    * stairstep pattern.
    * 
    * @param stagger
    *           The value to which to set stagger.
    */
   public void setStagger(boolean stagger) {
      mStagger = stagger;
   }

   /**
    * Determines whether the counts axis is labeled.
    * 
    * @return boolean
    */
   public boolean labelYAxis() {
      return mLabelYAxis;
   }

   /**
    * Determines whether the counts axis is labeled.
    * 
    * @param labelYAxis
    */
   public void setLabelYAxis(boolean labelYAxis) {
      mLabelYAxis = labelYAxis;
   }

   /**
    * Return the user configured default terminal.
    * 
    * @return String
    */
   public static String getDefaultTerminal() {
      return mDefaultTerminal;
   }
}

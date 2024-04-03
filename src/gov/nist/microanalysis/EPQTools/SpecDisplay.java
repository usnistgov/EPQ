package gov.nist.microanalysis.EPQTools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQTools.KLMLine.LabelType;
import gov.nist.microanalysis.EPQTools.KLMLine.SumPeak;
import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.PrintUtilities;

/**
 * <p>
 * A reusable component for displaying EDS spectra and many common associated
 * labels
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

public class SpecDisplay extends JComponent {

   private static final String ENERGY_AXIS_LABEL_KEV = "Energy (keV)";
   private static final long serialVersionUID = 4388450008895887951L;

   public class Region implements Comparable<Region> {
      public double mLowEnergy;
      public double mHighEnergy;

      public Region(double low, double high) {
         mLowEnergy = (low < high ? low : high);
         mHighEnergy = (low < high ? high : low);
      }

      public boolean overlaps(Region r) {
         return ((mLowEnergy <= r.mHighEnergy) && (mHighEnergy >= r.mLowEnergy));
      }

      public void add(Region r) {
         if (r.mLowEnergy < mLowEnergy)
            mLowEnergy = r.mLowEnergy;
         if (r.mHighEnergy > mHighEnergy)
            mHighEnergy = r.mHighEnergy;
      }

      public boolean inside(double e) {
         return (e >= mLowEnergy) && (e <= mHighEnergy);
      }

      /**
       * Gets the current value assigned to lowEnergy
       * 
       * @return Returns the lowEnergy in eV.
       */
      public double getLowEnergy() {
         return mLowEnergy;
      }

      /**
       * Gets the current value assigned to highEnergy
       * 
       * @return Returns the highEnergy in eV.
       */
      public double getHighEnergy() {
         return mHighEnergy;
      }

      @Override
      public String toString() {
         return "[" + Integer.toString((int) Math.round(mLowEnergy)) + " eV, " + Integer.toString((int) Math.round(mHighEnergy)) + " eV]";
      }

      public String toTabbedString() {
         return "ROI (eV)\t" + Integer.toString((int) Math.round(mLowEnergy)) + "\t" + Integer.toString((int) Math.round(mHighEnergy));
      }

      @Override
      public int compareTo(Region o) {
         return Double.compare(mLowEnergy, o.mLowEnergy);
      }
   }

   public class Regions {
      public ArrayList<Region> mList;

      Regions() {
         super();
         mList = new ArrayList<Region>(8);
      }

      Regions(Regions src) {
         super();
         mList = new ArrayList<Region>(src.mList);
      }

      public void add(Region r) {
         for (int i = mList.size() - 1; i >= 0; --i)
            if (r.overlaps(mList.get(i))) {
               // Add the two regions together then remove the existing region
               r.add(mList.get(i));
               mList.remove(i);
            }
         // Doesn't overlap any so just add it...
         mList.add(r);
      }

      public boolean inRegion(double e) {
         for (int i = mList.size() - 1; i >= 0; --i)
            if (mList.get(i).inside(e))
               return true;
         return false;
      }

      public void clear() {
         mList.clear();
      }

      public int size() {
         return mList.size();
      }

      public Region get(int i) {
         return mList.get(i);
      }
   }

   private class DisplayProperties {
      private double mScale;
      private final int mColorIndex; // Index into mDataColor

      private DisplayProperties(double scale, int colorIndex) {
         mScale = scale;
         mColorIndex = colorIndex;
      }
   }

   /**
    * ENERGY_EPS is used to ensure that peak integrations work as the user
    * intuitively expects. If a ROI of 0 keV to 1.0 keV is selected, then the
    * natural peak integration algorithm would count channels [0,10), [10,20),
    * ..., [990,1000), [1000,1010). This suprises users who expect the last
    * channel to not be there. By subtracting off ENERGY_EPS we eliminate this
    * issue with the rare possibility of not including E+ENERGY_EPS/2 correctly.
    */
   private static final double ENERGY_EPS = 1.0e-8;
   /**
    * Contains instances of the ISpectrumData objects to be displayed
    */
   private final java.util.List<ISpectrumData> mData = new ArrayList<ISpectrumData>();

   private synchronized java.util.List<ISpectrumData> getData() {
      return new ArrayList<ISpectrumData>(mData);
   }

   /**
    * Maps ISpectrumData instances into DisplayProperies instances
    */
   private final Map<ISpectrumData, DisplayProperties> mProperties = new TreeMap<ISpectrumData, DisplayProperties>();
   /**
    * Instances of KLMLine
    */
   private final Collection<KLMLine> mLines = new ArrayList<KLMLine>();
   private final Collection<KLMLine> mTemporaryLines = new ArrayList<KLMLine>();
   private int[] mMaxHeight; // Used to scale the KLM line heights
   private double mLabelThreshold = 0.001;

   /**
    * Instances of Region
    */
   private final Regions mRegions = new Regions();
   /**
    * Optional region to use for vertical scaling (Region Integral)
    */
   private Regions mScalingRegions = null;
   /**
    * Define the axis limits
    */
   private double mEMin = 0.0, mEMax = 2.0e4;
   private double mVMin = 0.0, mVMax = 16.0;
   private static final double DEFAULT_VZOOM = 101.0;
   private double mZoom = DEFAULT_VZOOM;
   private final boolean mShowVAxisLabels = true;
   private final boolean mShowHAxisLabels = true;
   private boolean mShowImage = false;
   final static int MAX_IMAGE_SIZE = 256;
   final static int MIN_IMAGE_SIZE = 128;
   final static int PNG_SCALE = 4;
   final static double LOG10_MIN = 100.0;
   final static double LOG10_RANGE = 1.0e4;
   private final Color mKLMColor = Color.darkGray;
   private final Color mAxisColor = Color.darkGray;
   private final Color mAxisLabelColor = buildAxisLabelColor();
   private final Color mPlotBackColor = new Color(0xFC, 0xFC, 0xF2);
   private final Color mMinorGridColor = new Color(0xFF, 0xF0, 0xF0);
   private final Color mMajorGridColor = new Color(0xFF, 0xE0, 0xE0);
   private final Color mBackTextColor = new Color(0xC0, 0xC0, 0xC0);
   private Color[] mDataColor = getDefaultColors();
   private final int TRANSPARENCY = 128;

   public void setSpectrumColors(Color[] colors) {
      mDataColor = colors;
   }

   static private Color buildAxisLabelColor() {
      return UIManager.getLookAndFeelDefaults().getColor("controlText");
   }

   /**
    * Returns a list of colors as found in the File colors in CSV format "red,
    * green, blue" where each index is 0..255.
    *
    * @param colors
    * @return Color[]
    */
   public static Color[] loadColors(File colors) {
      Color[] res = null;
      try {
         if (colors.isFile()) {
            final CSVReader reader = new CSVReader.FileReader(colors, false);
            final double[][] csv = reader.getResource();
            final ArrayList<Color> tmp = new ArrayList<Color>();
            for (final double[] row : csv) {
               if (row.length >= 3) {
                  final Color c = new Color((int) Math.round(row[0]), (int) Math.round(row[1]), (int) Math.round(row[2]));
                  tmp.add(c);
               }
            }
            if (tmp.size() > 4)
               res = tmp.toArray(new Color[tmp.size()]);
         }
      } catch (Throwable th) {
         // ignore it...
      }
      return res;
   }

   private static Color[] getDefaultColors() {
      return new Color[]{new Color(255, 66, 14), new Color(0, 69, 134), new Color(87, 157, 28), new Color(126, 0, 33), new Color(131, 202, 255),
            new Color(49, 64, 4), new Color(174, 207, 0), new Color(75, 31, 111), new Color(255, 149, 14), new Color(197, 0, 11),
            new Color(0, 132, 209), new Color(255, 211, 32)};
   }

   private final Rectangle mPlotRect = new Rectangle();
   private final Rectangle mOrdRect = new Rectangle();

   private enum ActionMode {
      None, Drag, Shift, Cursor, Zoom
   };

   private ActionMode mActionMode = ActionMode.None;
   // For recording the position of the ordinate labels for drag zooming
   private int mDragStart = Integer.MIN_VALUE;
   private int mDragEnd = Integer.MIN_VALUE;
   // For dragging the display left or right
   private int mShiftStart = Integer.MIN_VALUE;
   private int mShiftEnd = Integer.MIN_VALUE;
   private double mShiftHMin = Double.MIN_VALUE;
   private double mShiftHMax = Double.MIN_VALUE;

   private JPopupMenu jPopupMenu_Region; // The popup menu to display over a
   // region
   /**
    * The popup menu to display except when over a region
    */
   private JPopupMenu jPopupMenu_Default;
   private final ButtonGroup jButtonGroup_Axis = new ButtonGroup();
   private final ButtonGroup jButtonGroup_Label = new ButtonGroup();
   private final ButtonGroup jButtonGroup_Scaling = new ButtonGroup();
   private final ButtonGroup jButtonGroup_AxisSimple = new ButtonGroup();

   private String mTempAnnotation = null;
   private String mUserAnnotation = null;
   private final javax.swing.Timer mTimer;

   // / Inter spectrum scaling
   public enum SCALING_MODE {
      COUNTS("Counts"), INTEGRAL("Fraction of integrate counts (%)"), MAX_PEAK("Fraction of highest peak (%)"), REGION_INTEGRAL(
            "Fraction of region integral (%)"), COUNTS_PER_NA_S("Count/(nA·s)"), COUNTS_PER_NA_S_EV("Counts/(nA·s·eV)");

      private final String mUserString;

      private SCALING_MODE(String str) {
         mUserString = str;
      }

      @Override
      public String toString() {
         return mUserString;
      }
   }

   private SCALING_MODE mCurrentScalingMode = SCALING_MODE.COUNTS;

   /**
    * axis type
    */
   public enum AXIS_MODE {
      LINEAR("Linear axis"), LOG("Log(base 10) axis"), SQRT("Square root axis");

      private final String mUserString;

      private AXIS_MODE(String str) {
         mUserString = str;
      }

      @Override
      public String toString() {
         return mUserString;
      }
   };

   private AXIS_MODE mVAxisType = AXIS_MODE.LINEAR;
   private boolean mDragEnabled = true;
   private int mCursorPosition = Integer.MIN_VALUE;
   private int mVertScrollPos = Integer.MIN_VALUE;
   private static final Color CURSOR_COLOR = new Color(Color.white.getRGB() ^ Color.BLUE.getRGB());

   private static final int MIN_DRAG_DELTA = 2;
   /**
    * Staggering spectra
    */
   private int mStaggerOffset = 0;

   private KLMLine.LabelType mLabelType = KLMLine.LabelType.ELEMENT_ABBREV;

   private String mScaleText = mCurrentScalingMode.toString();

   /**
    * Combines KLM labels into a minimal set of compressed labels For example
    * "Fe Ka, Fe Kb, Ze La, Ze Lg" becomes "Fe K, Ze La+g"
    * 
    * @param label
    * @return String
    */
   private static String combineKLMLabels(String label) {
      final TreeSet<String> tss = new TreeSet<String>();
      // This step sorts the items and eliminates duplicates
      for (final String lb : label.split(","))
         tss.add(lb.trim());
      String hdr = null, prev = null;
      final StringBuffer acc = new StringBuffer();
      for (final String lb : tss) {
         if (hdr != null)
            if (lb.startsWith(hdr)) {
               final String xx = lb.substring(hdr.length()).trim();
               if (xx.length() > 0) {
                  acc.append("+");
                  acc.append(xx);
               }
            } else {
               hdr = null;
               prev = null;
            }
         if (hdr == null)
            if (prev == null) {
               prev = lb;
               if (acc.length() > 0)
                  acc.append(", ");
               acc.append(prev);
            } else {
               // Find the longest substring in common
               for (int i = 0; (i < prev.length()) && (i < lb.length()) && (prev.charAt(i) == lb.charAt(i)); ++i)
                  hdr = prev.substring(0, i + 1);
               if (hdr.length() > 0) {
                  // Something in common
                  acc.append("+");
                  acc.append(lb.substring(hdr.length()).trim());
               } else {
                  // Nothing in common
                  prev = lb;
                  if (acc.length() > 0)
                     acc.append(", ");
                  acc.append(prev);
               }
            }
      }
      String res = acc.toString();
      res = res.replace("K\u03B1+\u03B2", "K");
      res = res.replace("L\u002A+\u03B1+\u03B2+\u03B3", "L");
      res = res.replace("L\u002A+\u03B1+\u03B2", "L");
      res = res.replace("L\u002A+\u03B1+\u03B3", "L");
      res = res.replace("M\u002A+\u03B1+\u03B2+\u03B3", "M");
      res = res.replace("M\u002A+\u03B1+\u03B2", "M");
      res = res.replace("M\u002A+\u03B1+\u03B2+\u03B3", "M");
      return res;
   }

   protected void drawKLMs(Graphics2D gr, boolean label, boolean forSvg) {
      if (mLines.size() > 0) {
         gr.setColor(mKLMColor);
         drawLines(gr, mLabelType, mLines, forSvg);
      }
   }

   protected void drawTemporaryKLMs(Graphics2D gr) {
      if (mTemporaryLines.size() > 0) {
         gr.setColor(Color.white);
         gr.setXORMode(Color.green);
         gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
         gr.setStroke(new BasicStroke(2));
         drawLines(gr, LabelType.NONE, mTemporaryLines, false);
         gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         gr.setPaintMode();
      }
   }

   protected void drawKLMLine(Graphics2D gr, int xPos, int hgt, KLMLine.KLMLineType type, String str) {
      // Mark top of transitions with shapes, edges with family letter,
      final int offset = gr.getFontMetrics().stringWidth("X") / 3;
      final int blobHgt = 2 * offset;
      if ((hgt > blobHgt) && (type != KLMLine.KLMLineType.InvalidType)) {
         hgt += blobHgt;
         switch (type) {
            case KTransition :
               gr.drawOval(xPos - (blobHgt / 2), hgt - blobHgt, blobHgt, blobHgt);
               break;
            case LTransition :
               gr.drawRect(xPos - (blobHgt / 2), hgt - blobHgt, blobHgt, blobHgt);
               break;
            case MTransition : {
               final int[] xPts = {xPos, xPos - (blobHgt / 2), xPos + (blobHgt / 2)};
               final int[] yPts = {hgt, hgt - blobHgt, hgt - blobHgt};
               gr.drawPolygon(xPts, yPts, 3);
            }
               break;
            case KEdge :
            case LEdge :
            case MEdge :
            case NEdge :
               gr.fillOval(xPos - (blobHgt / 2), hgt - blobHgt, blobHgt, blobHgt);
               break;
            case NTransition : {
               final int[] xPts = {xPos, xPos - (blobHgt / 2), xPos + (blobHgt / 2)};
               final int[] yPts = {hgt, hgt - blobHgt, hgt - blobHgt};
               gr.drawPolygon(xPts, yPts, 3);
            }
               break;
            case EscapePeak :
            case SumPeak :
               gr.fillRect(xPos - (blobHgt / 2), hgt - blobHgt, blobHgt, blobHgt);
               break;
            case Satellite :
            case InvalidType :
               break;
         }
         gr.drawLine(xPos, hgt, xPos, mPlotRect.y + mPlotRect.height);
         hgt -= blobHgt;
      } else
         gr.drawLine(xPos, hgt, xPos, mPlotRect.y + mPlotRect.height);
      if (str.length() > 0) {
         final AffineTransform oldAt = gr.getTransform();
         try {
            final FontMetrics fm = gr.getFontMetrics();
            final Rectangle2D rect = fm.getStringBounds(str, gr);
            final int sw = (int) Math.round(rect.getWidth());
            int x, y;
            if (mPlotRect.y + sw + offset > hgt - 2 * offset) {
               y = mPlotRect.y + sw + offset;
               x = xPos - 3 * fm.getDescent();
            } else {
               y = hgt - 2 * offset;
               x = xPos;
            }
            gr.rotate(-Math.PI / 2, x, y);
            gr.translate(0.0, 0.2 * rect.getHeight());
            drawCallout(gr, x - offset, //
                  (int) Math.round(y - 0.2 * rect.getHeight() + rect.getY()), //
                  (int) Math.round(rect.getWidth() + 2.0 * offset), //
                  (int) Math.round(0.5 * rect.getHeight() - rect.getY()), offset, offset);
            gr.setColor(mKLMColor);
            gr.drawString(str, x, y);
         } finally {
            gr.setTransform(oldAt);
         }
      }
   }

   public Color makeTransparent(Color c, int a) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);

   }

   /**
    * @param energy
    *           in eV
    * @return The x-choordinate of associated with this energy
    */
   private int xx(double energy) {
      return mPlotRect.x + (int) (((energy - mEMin) * mPlotRect.width) / (mEMax - mEMin));
   }

   static List<EffectiveLine> buildEffectiveLines(Collection<KLMLine> lines, double tolerance) {
      final List<EffectiveLine> effs = new ArrayList<EffectiveLine>();
      final ArrayList<KLMLine> dupLines = new ArrayList<KLMLine>(lines);
      // Sort by element and amplitude
      dupLines.sort(new Comparator<KLMLine>() {
         @Override
         public int compare(KLMLine o1, KLMLine o2) {
            int res = Integer.compare(o1.getShell().getElement().getAtomicNumber(), o1.getShell().getElement().getAtomicNumber());
            return res != 0 ? res : -Double.compare(o1.mAmplitude, o2.mAmplitude);
         }
      });
      for (KLMLine line : dupLines) {
         boolean added = false;
         for (EffectiveLine eff : effs) {
            if (eff.add(line)) {
               added = true;
               break;
            }
         }
         if (!added)
            effs.add(new EffectiveLine(line, tolerance));
      }
      return effs;
   }

   static private class EffectiveLine {
      private final ArrayList<KLMLine> mLines;
      private double mAmplitude;
      private final double mTolerance;
      private double mWgtAvgCenter;

      private EffectiveLine(KLMLine klm, double tolerance) {
         mLines = new ArrayList<KLMLine>();
         mAmplitude = 0.0;
         mTolerance = tolerance;
         add(klm);
      }

      private boolean sameFamily(EffectiveLine eff) {
         if (!eff.mLines.get(0).getClass().equals(mLines.get(0).getClass()))
            return false;
         if (eff.mLines.get(0).getShell().getElement() != mLines.get(0).getShell().getElement())
            return false;
         return eff.mLines.get(0).getShell().getFamily() == mLines.get(0).getShell().getFamily();
      }

      private boolean add(KLMLine klm) {
         if (mLines.isEmpty() || memberOf(klm)) {
            assert !mLines.contains(klm);
            mLines.add(klm);
            mAmplitude += klm.mAmplitude;
            mWgtAvgCenter += klm.mAmplitude * klm.getEnergy();
            return true;
         }
         return false;
      }

      /**
       * Is this KLM of the same type, element and within the energy tolerance
       * of the KLM lines in this EffectiveLine collection?
       * 
       * @param klm
       * @return
       */
      public boolean memberOf(KLMLine klm) {
         final KLMLine primary = mLines.get(0);
         if (klm.getType() != primary.getType())
            return false;
         if (klm.getShell().getElement() != primary.getShell().getElement())
            return false;
         for (final KLMLine l : mLines)
            if (Math.abs(klm.getEnergy() - l.getEnergy()) < mTolerance)
               return true;
         return false;
      }

      /**
       * @return The best estimate energy in eV
       */
      public double getCenter() {
         return FromSI.eV(mWgtAvgCenter / mAmplitude);
      }

      private KLMLine.KLMLineType getType() {
         KLMLine.KLMLineType res = KLMLine.KLMLineType.InvalidType;
         for (final KLMLine l : mLines) {
            final KLMLine.KLMLineType lt = l.getType();
            if (lt.ordinal() > res.ordinal())
               res = lt;
         }
         return res;
      }

      public String toLabel(KLMLine.LabelType lt) {
         final StringBuffer sb = new StringBuffer();
         for (final KLMLine klm : mLines) {
            if (sb.length() > 0)
               sb.append(", ");
            sb.append(klm.toLabel(lt));
         }
         return combineKLMLabels(sb.toString());
      }
   }

   private void drawLines(Graphics2D gr, KLMLine.LabelType lt, Collection<KLMLine> lines, boolean forSvg) {
      final Shape oldClip = gr.getClip();
      try {
         gr.setClip(mPlotRect.x, mPlotRect.y, mPlotRect.width, mPlotRect.height);
         final Font oldFont = setLabelFont(gr, forSvg);
         try {
            final double tolerance = ToSI.eV((mEMax - mEMin) / 150);
            final List<EffectiveLine> effs = buildEffectiveLines(lines, tolerance);
            // Place the effective lines into families which are scaled
            // together.
            while (effs.size() > 0) {
               final EffectiveLine eff = effs.remove(0);
               final List<EffectiveLine> fam = new ArrayList<EffectiveLine>();
               fam.add(eff);
               final double xeffCenter = xx(eff.getCenter());
               EffectiveLine maxLine = (xeffCenter >= mPlotRect.x) && (xeffCenter < mMaxHeight.length + mPlotRect.x) ? eff : null;
               // Find all lines of the same element and family (K, L, M)
               for (int i = effs.size() - 1; i >= 0; i--) {
                  final EffectiveLine el = effs.get(i);
                  if (el.sameFamily(eff)) {
                     fam.add(el);
                     effs.remove(el);
                     final int xelCenter = xx(el.getCenter());
                     if ((xelCenter >= mPlotRect.x) && (xelCenter < mMaxHeight.length + mPlotRect.x))
                        if ((maxLine == null) || (el.mAmplitude > maxLine.mAmplitude))
                           maxLine = el;
                  }
               }
               if (maxLine != null) {
                  final int xmaxCenter = xx(maxLine.getCenter());
                  final double maxHgt = mMaxHeight[xmaxCenter - mPlotRect.x];
                  final double f = Math.max(0.05, ((mPlotRect.y + mPlotRect.height) - maxHgt) / mPlotRect.height);
                  for (final EffectiveLine feff : fam) {
                     final String label = feff.getType().isTransition()
                           ? (feff.mAmplitude > mLabelThreshold ? feff.toLabel(lt) : "")
                           : feff.toLabel(lt);
                     double scale = 1.0;
                     switch (feff.getType()) {
                        case KEdge :
                        case LEdge :
                        case MEdge :
                        case NEdge :
                           scale = 0.3;
                           break;
                        case EscapePeak :
                           scale = 1.0;
                           break;
                        case Satellite :
                           scale = 0.5;
                           break;
                        case SumPeak :
                           scale = 0.5;
                           break;
                        default :
                           scale = 1.0;
                           break;
                     }
                     final double hh = (mPlotRect.y + mPlotRect.height) - ((mPlotRect.height * f * scale * feff.mAmplitude) / maxLine.mAmplitude);
                     drawKLMLine(gr, xx(feff.getCenter()), (int) Math.round(hh), feff.getType(), label);
                  }
               }
            }
         } finally {
            gr.setFont(oldFont);
         }
      } finally

      {
         gr.setClip(oldClip);
      }
   }

   private Font setLabelFont(Graphics gr, boolean forSvg) {
      final Font oldFont = gr.getFont();
      if (mLabelType == LabelType.LARGE_ELEMENT)
         gr.setFont(new Font(oldFont.getFamily(), Font.PLAIN, forSvg ? 18 * PNG_SCALE : 15));
      else
         gr.setFont(new Font(oldFont.getFamily(), Font.PLAIN, forSvg ? 16 * PNG_SCALE : 12));
      return oldFont;
   }

   public void handleKLMActionEvent(KLMActionEvent ae) {
      final List<KLMLine> lines = ae.getLines();
      switch (ae.getAction()) {
         case ADD_LINES :
            addKLMs(lines);
            break;
         case CLEAR_ALL :
            mLines.clear();
            break;
         case REMOVE_LINES :
            removeKLMs(lines);
            break;
      }
      if (mPlotRect != null)
         repaint(mPlotRect);
   }

   public void addKLMs(Collection<KLMLine> lines) {
      for (final KLMLine line : lines) {
         boolean contains = false;
         for (final KLMLine ll : mLines)
            if (ll.equals(line)) {
               contains = true;
               break;
            }
         if (!contains)
            mLines.add(line);
      }
      if (mPlotRect != null)
         repaint(mPlotRect);
   }

   public void clearKLMs() {
      if (mLines.size() > 0) {
         mLines.clear();
         if (mPlotRect != null)
            repaint(mPlotRect);
      }
   }

   public void addKLM(KLMLine line) {
      for (final KLMLine ll : mLines)
         if (ll.equals(line))
            return;
      mLines.add(line);
      if (mPlotRect != null)
         repaint(mPlotRect);
   }

   public void removeKLM(KLMLine line) {
      final ArrayList<KLMLine> removeMe = new ArrayList<KLMLine>();
      for (final KLMLine kl : mLines)
         if (kl.isAssociated(line))
            removeMe.add(kl);
      if (removeMe.size() > 0) {
         for (final KLMLine rm : removeMe)
            mLines.remove(rm);
         if (mPlotRect != null)
            repaint(mPlotRect);
      }
   }

   public void removeKLMs(Collection<KLMLine> lines) {
      final ArrayList<KLMLine> removeMe = new ArrayList<KLMLine>();
      for (final KLMLine line : lines)
         for (final KLMLine kl : mLines)
            if (kl.isAssociated(line))
               removeMe.add(kl);
      if (removeMe.size() > 0) {
         for (final KLMLine rm : removeMe)
            mLines.remove(rm);
         if (mPlotRect != null)
            repaint(mPlotRect);
      }
   }

   /**
    * Set the temporary KLM lines to the specified sorted list of KLMLine
    * objects.
    * 
    * @param lines
    *           Collection&lt;KLMLine&gt;
    */
   public void setTemporaryKLMs(Collection<KLMLine> lines) {
      final Graphics2D gr = (Graphics2D) getGraphics();
      if (gr != null) {
         drawTemporaryKLMs(gr);
         mTemporaryLines.clear();
         mTemporaryLines.addAll(lines);
         drawTemporaryKLMs(gr);
      }
   }

   protected void drawRegions(Graphics gr) {
      gr.setColor(Color.yellow);
      for (final Region r : mRegions.mList) {
         int x0, x1;
         x0 = xx(r.getLowEnergy());
         x1 = xx(r.getHighEnergy());
         gr.fillRect(x0, mPlotRect.y, x1 - x0, mPlotRect.y + mPlotRect.height);
      }
      gr.setColor(Color.green);
   }

   protected double optimalStepSize(double min, double max, int labelDim, int totalDim) {
      int nLabels = totalDim / (5 * labelDim);
      if (nLabels < 2)
         nLabels = 2;
      final double nominalStep = 1000.0 * (max - min) / nLabels;
      long trialStep = Math.round(Math.pow(10.0, (int) (Math.log10(nominalStep)))); // log10
      if ((5 * trialStep) < nominalStep)
         trialStep *= 5;
      else if ((2 * trialStep) < nominalStep)
         trialStep *= 2;
      return 0.001 * trialStep;
   }

   private void drawHiresImage(Graphics2D g, final int wXtra, final int hXtra, final int cWidth, final int cHeight) {
      g.setBackground(Color.white);
      g.translate(wXtra, hXtra);
      final Font font = getGraphics().getFont();
      g.setFont(new Font(font.getFamily(), font.getStyle(), PNG_SCALE * font.getSize()));
      g.setStroke(new BasicStroke(PNG_SCALE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
      paintComponent(g, true);
   }

   private Dimension saveAsImage(File outfile, String formatName) throws EPQException {
      final int wXtra = (PNG_SCALE * getWidth()) / 20;
      final int hXtra = (PNG_SCALE * getHeight()) / 20;
      final int cWidth = ((PNG_SCALE * getWidth()) + (2 * wXtra));
      final int cHeight = ((PNG_SCALE * getHeight()) + (2 * hXtra));
      final BufferedImage bi = new BufferedImage(cWidth, cHeight,
            formatName.equalsIgnoreCase("BMP") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics2D g = bi.createGraphics();
      drawHiresImage(g, wXtra, hXtra, cWidth, cHeight);
      try {
         ImageIO.write(bi, formatName, outfile);
      } catch (final IOException e) {
         ErrorDialog.createErrorMessage(SpecDisplay.this, "Error writing spectrum to " + formatName + " file.", e);
      }
      return new Dimension(cWidth, cHeight);
   }

   @Override
   protected void paintComponent(Graphics g) {
      this.paintComponent(g, false);
   }

   private String formatAxisLabel(double nn) {
      switch (mCurrentScalingMode) {
         case INTEGRAL :
         case REGION_INTEGRAL :
            return new HalfUpFormat("0.0##", true).format(nn);
         default :
            if(nn<1.0)
               return new HalfUpFormat("0.0", true).format(nn);
            else
               return new HalfUpFormat("#,##0", true).format(nn);
      }
   }

   private void drawCallout(Graphics gr, int x, int y, int w, int h, int dx, int dy) {
      gr.setColor(makeTransparent(mPlotBackColor, TRANSPARENCY));
      gr.fillRoundRect(x, y, w, h, dx, dy);
      gr.setColor(makeTransparent(mMajorGridColor, TRANSPARENCY));
      gr.drawRoundRect(x, y, w, h, dx, dy);
   }

   protected void paintComponent(Graphics g, boolean forPng) {
      int t0, l0, w0, h0;
      final Graphics2D dup = (Graphics2D) g;
      dup.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      final Color oldColor = dup.getColor();
      final FontMetrics fm = dup.getFontMetrics();
      dup.setColor(mPlotBackColor);
      boolean showVAxis = mShowVAxisLabels;
      final List<ISpectrumData> data = getData();
      showVAxis = true;

      final int scale = (forPng ? PNG_SCALE : 1);
      final int canvasWidth = scale * getWidth();
      final int canvasHeight = scale * getHeight();
      if (showVAxis) {
         final int gutterWidth = (2 * scale) + fm.stringWidth(formatAxisLabel(mVMax) + "=iXX");
         l0 = canvasWidth > (gutterWidth + 4) ? gutterWidth + 2 : 0;
         w0 = (canvasWidth - (l0 + 4)) > 40 ? canvasWidth - (l0 + 4) : 40;
      } else {
         l0 = 40;
         w0 = canvasWidth - (l0 + 5);
      }
      t0 = 2;
      if (mShowHAxisLabels)
         h0 = (canvasHeight - (t0 + 4)) > (2 * fm.getHeight()) ? canvasHeight - (t0 + (2 * fm.getHeight())) : fm.getHeight();
      else
         h0 = canvasHeight - 5;
      mPlotRect.setBounds(l0, t0, w0, h0);
      mOrdRect.setBounds(0, t0, l0, h0);
      if ((mMaxHeight == null) || (mMaxHeight.length != w0))
         mMaxHeight = new int[w0];
      Arrays.fill(mMaxHeight, mPlotRect.y + mPlotRect.height);

      // Draw the background...
      dup.setColor(mAxisColor);
      dup.drawRect(l0 - 1, t0 - 1, w0 + 2, h0 + 2);
      final Color bkg = mPlotBackColor;
      dup.setColor(bkg);
      dup.fillRect(l0, t0, w0, h0);
      final Color alc = forPng ? mAxisColor : mAxisLabelColor;
      dup.setColor(alc);
      final int MIN_GRID_SCALE = 5;
      {
         if (mShowHAxisLabels) {
            final double step = optimalStepSize(mEMin, mEMax, fm.stringWidth("XXX"), mPlotRect.width) / MIN_GRID_SCALE;
            final double min = ((int) ((mEMin / step) + 0.999)) * step;
            final double max = ((int) (mEMax / step)) * step;
            final int digits = Math.min(4, Math.max(0, 3 - Math.min(3, (int)Math.floor(Math.log10(step)))));
            final NumberFormat fmt =  new DecimalFormat(digits==0 ? "0" : "0.0000".substring(0, digits + 2));
            for (double m = min; m <= (max + 0.001); m += MIN_GRID_SCALE * step) {
               final String str = fmt.format(0.001 * m);
               final int pos = (int) (l0 + ((w0 * (m - mEMin)) / (mEMax - mEMin)));
               dup.drawLine(pos, (t0 + h0) - scale, pos, t0 + h0 + (5 * scale));
               dup.drawString(str, pos - (fm.stringWidth(str) / 2), canvasHeight - fm.getHeight());
            }
            dup.drawString(ENERGY_AXIS_LABEL_KEV, (int) (mPlotRect.x + (0.5 * (mPlotRect.width - fm.stringWidth(ENERGY_AXIS_LABEL_KEV)))),
                  canvasHeight - fm.getDescent());
            dup.setColor(mMinorGridColor);
            int i = 0;
            for (double m = min; m < (max + 0.001); m += step, ++i) {
               final int pos = (int) (l0 + ((w0 * (m - mEMin)) / (mEMax - mEMin)));
               if ((pos > l0) && (pos < (l0 + w0))) {
                  if ((i % MIN_GRID_SCALE) == 0)
                     dup.setColor(mMajorGridColor);
                  dup.drawLine(pos, t0 + scale, pos, (t0 + h0) - scale);
                  if ((i % MIN_GRID_SCALE) == 0)
                     dup.setColor(mMinorGridColor);
               }
            }
         }
      }
      // Draw the cross lines
      switch (mVAxisType) {
         case LINEAR : { // Draw the labels
            final double step = optimalStepSize(mVMin, mVMax, fm.getHeight(), mPlotRect.height) / MIN_GRID_SCALE; 
            final double min = ((int) (mVMin / step)) * step;
            final double max = ((int) (mVMax / step)) * step;
            dup.setColor(mMinorGridColor);
            int i = 0;
            for (double m = min; m <= (max + 0.001); m += step, ++i) {
               if ((i % MIN_GRID_SCALE) == 0)
                  dup.setColor(mMajorGridColor);
               final int pos = (int) Math.round(t0 + (h0 * (1.0 - ((m - mVMin) / (mVMax - mVMin)))));
               if ((pos > t0) && (pos < (t0 + h0)))
                  dup.drawLine(l0 + scale, pos, (l0 + w0) - scale, pos);
               if ((i % MIN_GRID_SCALE) == 0) {
                  if (showVAxis) {
                     dup.setColor(alc);
                     final String str = formatAxisLabel(m);
                     dup.drawString(str, l0 - fm.stringWidth(str) - (6 * scale), pos + (fm.getHeight() / 3));
                     dup.drawLine(l0 + scale, pos, l0 - (4 * scale), pos);
                  }
                  dup.setColor(mMinorGridColor);

               }
            }
            dup.setColor(alc);
            if (showVAxis) {
               final int x = fm.getHeight(), y = (int) (mPlotRect.y + (0.5 * (mPlotRect.height + fm.stringWidth(mScaleText))));
               final AffineTransform oldAf = dup.getTransform();
               dup.rotate(-0.5 * Math.PI, x, y);
               dup.drawString(mScaleText, x, y);
               dup.setTransform(oldAf);
            }
         }
            break;
         case LOG : {
            // Every power of 10.0
            dup.setColor(mMinorGridColor);
            final double vMax = (mVMax > LOG10_MIN ? mVMax : LOG10_MIN);
            final double vMin = Math.pow(10.0, (int) Math.max(Math.log10(vMax / LOG10_RANGE), 0.0));
            for (double v = vMin; v < vMax; v *= 10.0)
               for (int j = 1; (j < 10) && ((j * v) < vMax); ++j) {
                  if (j == 1)
                     dup.setColor(mMajorGridColor);
                  final int a = (mPlotRect.y + mPlotRect.height)
                        - (int) ((Math.log10((j * v) / vMin) * mPlotRect.height) / (Math.log10(vMax / vMin)));
                  if ((a > t0) && (a < (t0 + h0)))
                     dup.drawLine(l0, a, l0 + w0, a);
                  if (j == 1) {
                     if (showVAxis) {
                        dup.setColor(alc);
                        final String str = formatAxisLabel(v);
                        dup.drawString(str, l0 - fm.stringWidth(str) - 4, a + (fm.getHeight() / 3));
                     }
                     dup.setColor(mMinorGridColor);
                  }
               }
            dup.setColor(alc);
            if (showVAxis) {
               String vText = "Log(" + mScaleText + ")";
               final int x = fm.getHeight(), y = (int) (mPlotRect.y + (0.5 * (mPlotRect.height + fm.stringWidth(vText))));
               final AffineTransform oldAf = dup.getTransform();
               dup.rotate(-0.5 * Math.PI, x, y);
               dup.drawString(vText, x, y);
               dup.setTransform(oldAf);
            }
         }
            break;
         case SQRT : {
            dup.setColor(mMajorGridColor);
            final double vMax = (mVMax > 0 ? Math.sqrt(mVMax) : 4.0);
            final double vMin = mVMin > 0 ? Math.sqrt(mVMin) : 0.0;
            double v = 1.0;
            while (v < vMax) {
               if ((v > vMin) && (v > (vMax / 32.0))) {
                  final int a = (mPlotRect.y + mPlotRect.height) - (int) (((v - vMin) * mPlotRect.height) / (vMax - vMin));
                  if ((a > t0) && (a < (t0 + h0)))
                     dup.drawLine(l0, a, l0 + w0, a);
                  if (showVAxis && (v > (vMax / 8.0))) {
                     dup.setColor(alc);
                     final String str = formatAxisLabel(v * v);
                     dup.drawString(str, l0 - fm.stringWidth(str) - 4, a + (fm.getHeight() / 3));
                     dup.setColor(mMinorGridColor);
                  }
               }
               v *= Math.sqrt(2.0);
            }
            dup.setColor(alc);
            if (showVAxis) {
               String vText = "Sqrt(" + mScaleText + ")";
               final int x = fm.getHeight(), y = (int) (mPlotRect.y + (0.5 * (mPlotRect.height + fm.stringWidth(vText))));
               final AffineTransform oldAf = dup.getTransform();
               dup.rotate(-0.5 * Math.PI, x, y);
               dup.drawString(vText, x, y);
               dup.setTransform(oldAf);
            }
         }
            break;
      }
      dup.setColor(mBackTextColor);
      // Draw pixel dimensions on screen but not PNG
      if (!forPng)
         dup.drawString(Integer.toString(getWidth()) + " \u00D7 " + Integer.toString(getHeight()), l0 + fm.charWidth('j'), t0 + fm.getHeight());
      // Set up a clipping rectangle to simplify life...
      dup.clipRect(l0, t0, w0, h0);
      // Draw the selected regions
      drawRegions(dup);
      // Write the data labels
      int maxWidth = 0;
      {
         for (final ISpectrumData spec : data) {
            final int w = fm.stringWidth(spec.toString());
            if (w > maxWidth)
               maxWidth = w;
         }
         if (maxWidth > (canvasWidth / 2))
            maxWidth = canvasWidth / 2;
         maxWidth += 8 * scale;
      }
      final Rectangle clipRect = dup.getClipBounds();
      int pictPos = 24 * scale;
      try {
         int line = 0;
         final int dx = fm.charWidth('W');
         final int h = fm.getHeight();
         dup.setColor(makeTransparent(mPlotBackColor, TRANSPARENCY));
         final int xOff = (l0 + w0) - (maxWidth + (2 * dx) + (2 * scale));
         final int ww = maxWidth + (2 * scale) + ((3 * dx) / 2);
         final int yOff = t0 + (h / 2);
         final int hMax = Math.min((h * data.size()) + (h / 2), (((h0 - h) / h) * h) + (h / 2));
         drawCallout(dup, xOff - (dx / 2), yOff - (h / 4), ww, hMax, 5 * scale, 5 * scale);
         pictPos = yOff + hMax + (h / 2);
         dup.setClip(xOff, yOff, ww - (dx / 2) - (2 * scale), hMax - (h / 4));
         for (final ISpectrumData spec : data) {
            final DisplayProperties dp = mProperties.get(spec);
            if (dp != null) {
               if (((yOff + (h * (line + 1))) > (hMax - (h / 4))) && ((data.size() - line) > 1)) {
                  dup.setColor(mAxisColor);
                  dup.drawString("+ " + (data.size() - line) + " more...", xOff, yOff + (h * (line + 1)));
                  break;
               } else {
                  dup.setColor(mDataColor[dp.mColorIndex]);
                  dup.fillRect(xOff, yOff + (h * line) + (3 * scale), h - (2 * scale), h - (2 * scale));
                  dup.setColor(mAxisColor);
                  dup.drawString(spec.toString(), xOff + h, yOff + (h * (line + 1)));
               }
               ++line;
            }
         }
         final String annot = getTextAnnotation();
         if (annot != null) {
            int yy = yOff + h;
            final String[] lines = annot.split("\n");
            final int TAB_WIDTH = 8;
            int width = 0;
            for (final String ll : lines) {
               final String[] cols = ll.split("\t");
               final int w = (TAB_WIDTH * dx * (cols.length - 1)) + fm.stringWidth(cols[cols.length - 1]);
               if (w > width)
                  width = w;
            }
            final int xOffAnnot = Math.min(xOff - (width + (2 * dx)), l0 + (w0 / 2));
            final int hMaxAnnot = Math.min((h * lines.length) + (h / 2), (((h0 - h) / h) * h) + (h / 2));
            dup.setClip(Math.max(l0, xOffAnnot - (dx / 2)), yOff - (h / 4), width + dx + 1, hMaxAnnot + 1);
            drawCallout(dup, xOffAnnot - (dx / 2), yOff - (h / 4), width + dx, hMaxAnnot, 5 * scale, 5 * scale);
            dup.setColor(mAxisColor);
            dup.setClip(Math.max(l0, xOffAnnot), yOff, width, hMaxAnnot - (h / 4));
            for (final String ll : lines) {
               final String[] cols = ll.split("\t");
               int xx = xOffAnnot;
               for (final String col : cols) {
                  dup.drawString(col, xx, yy);
                  xx += TAB_WIDTH * dx;
               }
               yy += h;
               if (yy > (yOff + hMaxAnnot))
                  break;
            }
         }
      } finally {
         dup.setClip(clipRect);
      }
      if (mShowImage && (data.size() == 1) && (canvasHeight > 128))
         for (int j = data.size() - 1; j >= 0; --j) {
            final ISpectrumData spec = data.get(j);
            if (spec.getProperties().isDefined(SpectrumProperties.MicroImage))
               try {
                  final Image img = spec.getProperties().getImageProperty(SpectrumProperties.MicroImage);
                  if (img instanceof BufferedImage) {
                     final BufferedImage bi = (BufferedImage) img;
                     int height = bi.getHeight() * scale;
                     int width = bi.getWidth() * scale;
                     while ((height + (scale * 20)) > (canvasHeight - pictPos)) {
                        height /= 2;
                        width /= 2;
                     }
                     ScaledImage.draw(bi, dup, (l0 + w0) - (width + (scale * 8)), pictPos, width, height);
                  }
               } catch (final EPQException e) {
                  // Ignore it...
               }
         }

      // Draw the data (last to first...)
      {
         int idx = data.size() - 1;
         for (int j = data.size() - 1; j >= 0; --j) {
            final ISpectrumData spec = data.get(j);
            final DisplayProperties dp = mProperties.get(spec);
            if (dp != null) {
               dup.setColor(mDataColor[dp.mColorIndex]);
               drawSpectrum(dup, spec, dp.mScale, idx, forPng);
               --idx;
            }
         }
      }
      drawKLMs(dup, true, forPng);
      if (!forPng) {
         dup.setColor(oldColor);
         drawTemporaryKLMs(dup);
         drawCursor(dup);
      }
   }

   static protected int bounds(int v, int b1, int b2) {
      if (v < b1)
         return b1;
      if (v >= b2)
         return b2 - 1;
      return v;
   }

   protected void drawSpectrum(Graphics gr, ISpectrumData sd, double scale, int idx, boolean forSvg) {
      final double channelWidth = sd.getChannelWidth();
      final double chPerPixel = (mEMax - mEMin) / (channelWidth * mPlotRect.width);
      double vMax = mVMax, vMin = mVMin;
      switch (mVAxisType) {
         case LOG : {
            vMax = Math.log10(vMax > LOG10_MIN ? mVMax : LOG10_MIN);
            vMin = (int) Math.max(vMax - Math.log10(LOG10_RANGE), 0.0);
         }
            break;
         case SQRT : {
            vMax = vMax > 0 ? Math.sqrt(vMax) : 4.0;
            vMin = vMin > 0 ? Math.sqrt(vMax) : 0.0;
         }
            break;
         case LINEAR :
            break;
      }
      final int off = idx * mStaggerOffset;
      if (forSvg || (chPerPixel < 1.0)) { // Stair steps
         final int base_ch = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, mEMin));
         final int top_ch = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, mEMax) + 1);
         final int[][] pts = new int[2][(2 * (top_ch - base_ch)) + 2];
         int ptIdx = 0;
         final int yOrigin = (mPlotRect.y + mPlotRect.height) - off;
         for (int ch = base_ch; ch <= top_ch; ++ch) {
            double cx = scale * sd.getCounts(ch);
            switch (mVAxisType) {
               case LOG :
                  cx = cx > 0.0 ? Math.max(Math.log10(cx), vMin) : vMin;
                  break;
               case SQRT :
                  cx = cx > 0.0 ? Math.sqrt(cx) : 0.0;
                  break;
               case LINEAR :
                  break;
            }
            final int yOff = (int) (((cx - vMin) * mPlotRect.height) / (vMax - vMin));
            final int xMin = off + xx(Math.min(SpectrumUtils.minEnergyForChannel(sd, ch), mEMax));
            pts[0][ptIdx] = xMin;
            pts[1][ptIdx] = Math.max(yOrigin - yOff, -100);
            ++ptIdx;
            final int xMax = off + xx(Math.min(SpectrumUtils.maxEnergyForChannel(sd, ch), mEMax));
            pts[0][ptIdx] = xMax;
            pts[1][ptIdx] = Math.max(yOrigin - yOff, -100);
            ++ptIdx;
            // Set mMaxHeight[i] to the pixel coordinate of the highest spectrum
            // data position of all displayed spectra
            for (int i = Math.max(0, xMin - mPlotRect.x); i < Math.min(mMaxHeight.length, xMax - mPlotRect.x); ++i)
               mMaxHeight[i] = Math.min(yOrigin - yOff, mMaxHeight[i]);
         }
         if (ptIdx > 0)
            gr.drawPolyline(pts[0], pts[1], ptIdx);
      } else { // Connected vertical bars
         double base_ch = (mEMin - sd.getZeroOffset()) / channelWidth + 1;
         final int top_ch = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, mEMax));
         double low = Double.MAX_VALUE;
         double high = 0.0;
         for (int i = 0; i < mPlotRect.width; ++i) {
            int high_ch = (int) (base_ch + chPerPixel);
            if (high_ch > top_ch)
               high_ch = top_ch;
            if (base_ch < 0) {
               base_ch += chPerPixel;
               continue;
            }
            if ((base_ch >= top_ch) || (high_ch < 0))
               break;
            for (int j = (int) base_ch; j < high_ch; ++j) {
               double datum = scale * sd.getCounts(j);
               switch (mVAxisType) {
                  case LOG :
                     datum = datum > 0.0 ? Math.max(Math.log10(datum), vMin) : vMin;
                     break;
                  case SQRT :
                     datum = datum > 0.0 ? Math.sqrt(datum) : 0.0;
                     break;
                  case LINEAR :
                     break;
               }
               if (datum < low)
                  low = datum;
               if (datum > high)
                  high = datum;
            }
            base_ch += chPerPixel;
            {
               // Don't draw anything if out of range...
               final int highPos = (int) ((mPlotRect.y + mPlotRect.height) - (((high - vMin) * mPlotRect.height) / (vMax - vMin)));
               if ((low < vMax) && (high > vMin)) {
                  if (high > vMax)
                     high = vMax;
                  if (low < vMin)
                     low = vMin;
                  gr.drawLine(i + mPlotRect.x + off,
                        (int) ((mPlotRect.y + mPlotRect.height) - (((low - vMin) * mPlotRect.height) / (vMax - vMin))) - off, i + mPlotRect.x + off,
                        highPos - off);
               }
               mMaxHeight[i] = Math.min(highPos, mMaxHeight[i]);
            }
            {
               final double tmp = high;
               high = low;
               low = tmp;
            }
         }
      }
   }

   /**
    * autoScaleV - Autoscale the vertical axis to maxPercent of the range above
    * the zeroOffset.
    * 
    * @param maxPercent
    *           double
    */
   public void autoScaleV(double maxPercent) {
      double max = -Double.MAX_VALUE;
      final List<ISpectrumData> data = getData();
      for (final Object element : data) {
         final ISpectrumData sd = (ISpectrumData) element;
         final double scale = mProperties.get(sd).mScale;
         final int minCh = Math.max(0, SpectrumUtils.channelForEnergy(sd, 100.0));
         final double scMax = sd.getCounts(SpectrumUtils.maxChannel(sd, minCh, sd.getChannelCount())) * scale;
         if (scMax > max)
            max = scMax;
      }
      if (max == -Double.MAX_VALUE)
         max = (16.0 * 100.0) / maxPercent;
      if ((mVMin != 0.0) || (mVMax != max)) {
         mVMin = 0.0;
         mVMax = (max * maxPercent) / 100.0;
         if (mVMax < 1.0)
            mVMax = 1.0;
         repaint();
      }
   }

   public void rescaleV() {
      autoScaleV(mZoom);
   }

   /**
    * autoScaleH - Autoscale the horizontal axis to minPercent of the range
    * above the zeroOffset and maxPercent of the range above the zeroOffset.
    * 
    * @param minPercent
    *           double
    * @param maxPercent
    *           double
    */
   public void autoScaleH(double minPercent, double maxPercent) {
      assert (minPercent < maxPercent);
      double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
      final List<ISpectrumData> data = getData();
      for (int i = 0; i < data.size(); ++i) {
         final ISpectrumData sd = data.get(i);
         final double val = SpectrumUtils.minEnergyForChannel(sd, SpectrumUtils.largestNonZeroChannel(sd));
         if (val > max)
            max = val;
         if (sd.getZeroOffset() < min)
            min = sd.getZeroOffset();
      }
      if (min < 0.0)
         min = 0.0;
      if ((min == Double.MAX_VALUE) || (max == -Double.MAX_VALUE)) {
         min = 0.0;
         max = 2.0e4;
      }
      if (min == max)
         max = min + 1000.0;
      // Pick a pretty horizontal max
      final double scale = 1000.0;
      max = scale * Math.ceil(max / scale);
      setHRange(min + ((max - min) * (minPercent / 100.0)), min + ((max - min) * (maxPercent / 100.0)));
   }

   private void startShift(int x) {
      assert mShiftStart == Integer.MIN_VALUE;
      mActionMode = ActionMode.Shift;
      mShiftStart = x;
      mShiftHMin = mEMin;
      mShiftHMax = mEMax;
   }

   private void endShift(int x) {
      assert mActionMode == ActionMode.Shift;
      performShift(x);
      mActionMode = ActionMode.None;
      mShiftStart = Integer.MIN_VALUE;
      mShiftEnd = Integer.MIN_VALUE;
      mShiftHMin = Double.MIN_VALUE;
      mShiftHMax = Double.MIN_VALUE;
   }

   private boolean isShift() {
      return mActionMode == ActionMode.Shift;
   }

   private void performShift(int x) {
      assert mActionMode == ActionMode.Shift;
      mShiftEnd = x;
      final double delta = (((mShiftStart - mShiftEnd) * (mShiftHMax - mShiftHMin)) / mPlotRect.width);
      mEMin = mShiftHMin + delta;
      mEMax = mShiftHMax + delta;
      repaint();
   }

   private void startDrag(int x) {
      assert mActionMode == ActionMode.None;
      mActionMode = ActionMode.Drag;
      mDragStart = Integer.MIN_VALUE;
      mDragEnd = Integer.MIN_VALUE;
      if ((x >= mPlotRect.x) && (x <= (mPlotRect.x + mPlotRect.width)) && mDragEnabled)
         mDragStart = x;
   }

   private void cancelDrag() {
      assert mActionMode == ActionMode.Drag;
      mDragStart = Integer.MIN_VALUE;
      mDragEnd = Integer.MIN_VALUE;
      mActionMode = ActionMode.None;
   }

   private void endDrag(int x) {
      assert mActionMode == ActionMode.Drag;
      mActionMode = ActionMode.None;
      if ((mDragStart != Integer.MIN_VALUE) && (Math.abs(mDragStart - x) >= MIN_DRAG_DELTA)) {
         if (mDragEnd != Integer.MIN_VALUE) {
            final Graphics gr = getGraphics().create();
            gr.setXORMode(Color.green);
            gr.fillRect(mDragStart < mDragEnd ? mDragStart : mDragEnd, mPlotRect.y, Math.abs(mDragEnd - mDragStart), mPlotRect.height);

         }
         mDragEnd = x;
         if (mDragStart > mDragEnd) {
            final int tmp = mDragStart;
            mDragStart = mDragEnd;
            mDragEnd = tmp;
         }
         {
            double min = mEMin + (((mDragStart - mPlotRect.x) * (mEMax - mEMin)) / mPlotRect.width);
            double max = mEMin + (((mDragEnd - mPlotRect.x) * (mEMax - mEMin)) / mPlotRect.width);
            if (min < mEMin)
               min = mEMin;
            if (max > mEMax)
               max = mEMax;
            addRegion(min, max);
            final NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(0);
            String status = "E[low]  = " + nf.format(min) + " eV\nE[high] = " + nf.format(max) + " eV\nDelta   = " + nf.format(max - min) + " eV";
            final List<ISpectrumData> data = getData();
            for (final ISpectrumData spec : data) {
               final long cx = Math
                     .round(SpectrumUtils.sumCounts(spec, SpectrumUtils.channelForEnergy(spec, min), SpectrumUtils.channelForEnergy(spec, max) + 1));
               status += "\n" + Long.toString(cx) + "\t" + spec.toString();
            }
            setTemporaryAnnotation(status);
         }
      }
      mDragStart = Integer.MIN_VALUE;
      mDragEnd = Integer.MIN_VALUE;
   }

   /**
    * Add a highlighted region from the specified min energy to max energy in eV
    * 
    * @param min
    *           Energy in eV
    * @param max
    *           Energy in eV
    */
   public void addRegion(double min, double max) {
      mRegions.add(new Region(min, max));
      repaint();
   }

   public void clearRegions() {
      if (mRegions.size() > 0) {
         mRegions.clear();
         repaint();
      }
   }

   private void performDrag(int x) {
      assert mActionMode == ActionMode.Drag;
      if ((mDragStart != Integer.MIN_VALUE) && (x != mDragStart) && (x != mDragEnd)) {
         final Graphics gr = getGraphics().create();
         gr.setXORMode(Color.yellow);
         if (mDragEnd != Integer.MIN_VALUE)
            gr.fillRect(mDragStart < mDragEnd ? mDragStart : mDragEnd, mPlotRect.y, Math.abs(mDragEnd - mDragStart), mPlotRect.height);
         mDragEnd = Math2.bound(x, mPlotRect.x, mPlotRect.x + mPlotRect.width + 1);
         gr.fillRect(mDragStart < mDragEnd ? mDragStart : mDragEnd, mPlotRect.y, Math.abs(mDragEnd - mDragStart), mPlotRect.height);
      }
   }

   private void displayCursor(int x) {
      assert mActionMode == ActionMode.Cursor;
      if ((x > mPlotRect.x) && (x < (mPlotRect.x + mPlotRect.width))) {
         final Graphics gr = getGraphics().create();
         gr.setXORMode(CURSOR_COLOR);
         if (mCursorPosition != Integer.MIN_VALUE)
            gr.drawLine(mCursorPosition, mPlotRect.y, mCursorPosition, mPlotRect.y + mPlotRect.height);
         mCursorPosition = x;
         // Collect spectrum info
         final double e = mEMin + (((mCursorPosition - mPlotRect.x) * (mEMax - mEMin)) / mPlotRect.width);
         final NumberFormat nf = NumberFormat.getInstance();
         final StringBuffer sb = new StringBuffer();
         nf.setMaximumFractionDigits(0);
         sb.append(nf.format(e));
         sb.append(" eV");
         final List<ISpectrumData> data = getData();
         for (final ISpectrumData sd : data) {
            final int ch = SpectrumUtils.channelForEnergy(sd, e);
            if ((ch >= 0) && (ch < sd.getChannelCount())) {
               sb.append("\n");
               sb.append(nf.format(sd.getCounts(ch)));
               sb.append("\t");
               sb.append(sd.toString());
            }
         }
         setTemporaryAnnotation(sb.toString());
         gr.drawLine(mCursorPosition, mPlotRect.y, mCursorPosition, mPlotRect.y + mPlotRect.height);
      }
   }

   private void drawCursor(Graphics gr) {
      if (mCursorPosition != Integer.MIN_VALUE) {
         gr.setXORMode(CURSOR_COLOR);
         gr.drawLine(mCursorPosition, mPlotRect.y, mCursorPosition, mPlotRect.y + mPlotRect.height);
      }
   }

   private void hideCursor() {
      drawCursor(getGraphics());
      setTemporaryAnnotation(null);
      mCursorPosition = Integer.MIN_VALUE;
      mActionMode = ActionMode.None;
   }

   private void doPopup(int x, int y) {
      final double e = mEMin + (((x - mPlotRect.x) * (mEMax - mEMin)) / mPlotRect.width);
      if (mRegions.inRegion(e))
         doRegionPopup(x, y);
      else
         doDefaultPopup(x, y);
   }

   private void doRegionPopup(int x, int y) {
      if (jPopupMenu_Region != null)
         jPopupMenu_Region.show(this, x, y);
   }

   private void doDefaultPopup(int x, int y) {
      if (jPopupMenu_Default != null)
         jPopupMenu_Default.show(this, x, y);
   }

   /**
    * setRegionMenu - Set the menu to use when the user right clicks over a user
    * defined region.
    * 
    * @param pm
    *           PopupMenu
    */
   public void setRegionMenu(JPopupMenu pm) {
      if (jPopupMenu_Region != null)
         remove(jPopupMenu_Region);
      jPopupMenu_Region = pm;
      if (jPopupMenu_Region != null)
         add(jPopupMenu_Region);
   }

   /**
    * setDefaultMenu - Set the popup menu to use when the user right clicks over
    * any position on the spectrum control except over a user defined region.
    * 
    * @param pm
    *           PopupMenu
    */
   public void setDefaultMenu(JPopupMenu pm) {
      if (jPopupMenu_Default != null)
         remove(jPopupMenu_Default);
      jPopupMenu_Default = pm;
      if (jPopupMenu_Default != null)
         add(jPopupMenu_Default);
   }

   /**
    * zoomInBy - Zoom the vertical axis by the specified factor. d&gt;1.0 is
    * zoom in and d&lt;1.0 is zoom out.
    * 
    * @param d
    *           double
    */
   public void zoomInBy(double d) {
      mZoom /= d;
      Preferences.userNodeForPackage(SpecDisplay.class).putDouble("Zoom", mZoom);
      mVMax = ((mVMax - mVMin) / d) + mVMin;
      if((getSpectrumScalingMode()==SCALING_MODE.COUNTS) &&(mVMax<8.0))
         mVMax=8.0;
      repaint();
   }

   public GnuplotFile createGnuplot() {
      final GnuplotFile gpf = new GnuplotFile();
      boolean showAxisScale = true;
      final List<ISpectrumData> data = getData();
      for (final ISpectrumData spec : data) {
         final double sc = mProperties.get(spec).mScale;
         gpf.addSpectrum(sc == 1.0 ? spec : SpectrumUtils.scale(sc, spec), spec.toString());
         showAxisScale &= (sc == 1.0);
      }
      gpf.setLabelYAxis(showAxisScale);
      gpf.setStagger(mStaggerOffset != 0);
      for (final KLMLine klm : mLines)
         gpf.addKLMLine(klm, klm.toLabel(mLabelType));
      gpf.setXRange(mEMin, mEMax);
      gpf.setYRange(mVMin, mVMax);
      gpf.setLogCounts(mVAxisType.equals(AXIS_MODE.LOG));
      return gpf;

   }

   /**
    * Returns a simple pop-up menu suitable for use as a display menu
    * 
    * @return JPopupMenu
    */
   public JPopupMenu getSimpleMenu() {
      final JPopupMenu pum = new JPopupMenu();
      pum.add(new JMenuItem_Axis(jButtonGroup_AxisSimple, SpecDisplay.AXIS_MODE.LINEAR));
      pum.add(new JMenuItem_Axis(jButtonGroup_AxisSimple, SpecDisplay.AXIS_MODE.LOG));
      pum.add(new JMenuItem_Zoom(0.0));
      pum.add(new JMenuItem_Zoom(1.0));
      return pum;
   }

   public class JMenuItem_Axis extends JCheckBoxMenuItem {
      private static final long serialVersionUID = 6627853387207913400L;
      final private AXIS_MODE mMode;
      final private ButtonGroup mGroup;

      public JMenuItem_Axis(ButtonGroup bg, AXIS_MODE mode) {
         super(mode.toString());
         mMode = mode;
         if (bg != null) {
            mGroup = bg;
            bg.add(this);
         } else
            mGroup = null;
         if (SpecDisplay.this.mVAxisType == mode)
            setSelected(true);
         addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 0x1;

            @Override
            public void actionPerformed(ActionEvent e) {
               setAxisScalingMode(mMode);
               if (mGroup != null)
                  mGroup.setSelected(JMenuItem_Axis.this.getModel(), true);
               Preferences.userNodeForPackage(SpecDisplay.class).put("Ordinate", mMode.name());
            }
         });
      }
   }

   static private String getZoomMenuItemText(double factor) {
      if (factor == 0.0)
         return "Zoom to region";
      else if (factor == 1.0)
         return "Zoom to all";
      else {
         final NumberFormat nf = new HalfUpFormat("0.#");
         return factor > 1.0 ? "Zoom in " + nf.format(factor) + " \u00D7" : "Zoom out " + nf.format(1.0 / factor) + " \u00D7";
      }
   }

   /**
    * <p>
    * A helper class for building menu items associated with zooming.
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
   public class JMenuItem_Zoom extends JMenuItem {
      private static final long serialVersionUID = 6627853387207913400L;
      final private double mFactor;

      /**
       * Constructs a JMenuItem_Zoom
       * 
       * @param factor
       *           0.0 is zoom to region, 1.0 is zoom to all, 2.0 is zoom in by
       *           2, 0.5 is zoom out by 2.0 etc.
       */
      public JMenuItem_Zoom(double factor) {
         super(getZoomMenuItemText(factor));
         mFactor = factor;
         addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 0x1;

            @Override
            public void actionPerformed(ActionEvent e) {
               if (mFactor == 0.0)
                  zoomToRegion();
               else if (mFactor == 1.0)
                  zoomToAll();
               else
                  zoomInBy(mFactor);
            }
         });
      }
   }

   /**
    * <p>
    * A helper class for building menu items associated with SCALING_MODE.
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
   public class JMenuItem_Compare extends JCheckBoxMenuItem {
      private static final long serialVersionUID = 6627853387207913400L;
      final private SCALING_MODE mMode;
      final private ButtonGroup mGroup;

      public JMenuItem_Compare(SCALING_MODE mode) {
         this(null, mode);
      }

      public JMenuItem_Compare(ButtonGroup bg, SCALING_MODE mode) {
         super(mode.toString());
         mMode = mode;
         if (bg != null) {
            mGroup = bg;
            bg.add(this);
         } else
            mGroup = null;
         if (SpecDisplay.this.mCurrentScalingMode == mode)
            setSelected(true);
         addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 0x1;

            @Override
            public void actionPerformed(ActionEvent e) {
               setSpectrumScalingMode(mMode);
               if (mGroup != null)
                  mGroup.setSelected(JMenuItem_Compare.this.getModel(), true);
               Preferences.userNodeForPackage(SpecDisplay.class).put("Scaling",
                     mMode != SCALING_MODE.REGION_INTEGRAL ? mMode.name() : SCALING_MODE.COUNTS.name());
            }
         });
      }
   }

   /**
    * <p>
    * A helper class for building menu items associated with LABEL_TYPE.
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
   public class JMenuItem_Label extends JCheckBoxMenuItem {
      private static final long serialVersionUID = 6627853387207913400L;
      final private LabelType mMode;
      final private ButtonGroup mGroup;

      public JMenuItem_Label(LabelType mode) {
         this(null, mode);
      }

      public JMenuItem_Label(ButtonGroup bg, LabelType mode) {
         super(mode.toString());
         mMode = mode;
         if (bg != null) {
            mGroup = bg;
            bg.add(this);
         } else
            mGroup = null;
         if (SpecDisplay.this.mLabelType == mode)
            setSelected(true);
         addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 0x1;

            @Override
            public void actionPerformed(ActionEvent e) {
               setKLMLabelType(mMode);
               if (mGroup != null)
                  mGroup.setSelected(JMenuItem_Label.this.getModel(), true);
               Preferences.userNodeForPackage(SpecDisplay.class).put(LabelType.class.getName(), mMode.name());
               setSelected(true);
            }
         });
      }
   }

   private class LabelAllLinesAction extends AbstractAction {

      private static final long serialVersionUID = 2433596345205113603L;

      private LabelAllLinesAction() {
         super("Label (almost) all lines?");
      }

      @Override
      public void actionPerformed(ActionEvent arg0) {
         if (((JCheckBoxMenuItem) arg0.getSource()).isSelected())
            setLabelThreshold(0.001);
         else
            setLabelThreshold(0.01);
         invalidate();
      }
   }

   private void setLabelThreshold(double val) {
      val = Math2.bound(val, 0.0, 1.0);
      if (mLabelThreshold != val) {
         mLabelThreshold = val;
         repaint();
      }
   }

   /**
    * getDefaultMenu - Get a default implementation of the spectrum display
    * popup menu.
    * 
    * @return JPopupMenu
    */
   public JPopupMenu getDefaultMenu() {
      final JPopupMenu menu = new JPopupMenu("Spectrum Menu");
      {
         final JMenu copyMenu = new JMenu("Copy");
         JMenuItem mi = new JMenuItem("As Bitmap");
         mi.setMnemonic('C');
         mi.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -6141937337251943212L;

            @Override
            public void actionPerformed(ActionEvent e) {
               copyBitmapToClipboard();
            }
         });
         copyMenu.add(mi);

         mi = new JMenuItem("Marked Elements");
         mi.setMnemonic('E');

         mi.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 7120219561040977936L;

            @Override
            public void actionPerformed(ActionEvent e) {
               final Set<Element> elms = getMarkedElements();
               final StringSelection ss = new StringSelection(elms.toString());
               getToolkit().getSystemClipboard().setContents(ss, ss);
            }
         });
         copyMenu.add(mi);

         mi = new JMenuItem("Status Text");
         mi.setMnemonic('S');

         mi.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 2109235954490679248L;

            @Override
            public void actionPerformed(ActionEvent e) {
               final String annot = getTextAnnotation();
               if (annot != null) {
                  final StringSelection ss = new StringSelection(annot);
                  getToolkit().getSystemClipboard().setContents(ss, ss);
               }
            }
         });
         copyMenu.add(mi);

         menu.add(copyMenu);
      }

      {
         final JMenu saveMenu = new JMenu("Save");
         JMenuItem mi = new JMenuItem("As displayed");
         mi.setMnemonic('B');
         mi.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 7593122492070594609L;

            @Override
            public void actionPerformed(ActionEvent e) {
               try {
                  final String kBitmapDir = "Bitmap directory";
                  final Preferences userPref = Preferences.userNodeForPackage(SpecDisplay.class);
                  final JFileChooser jfc = new JFileChooser(userPref.get(kBitmapDir, System.getProperty("user.home")));
                  jfc.setAcceptAllFileFilterUsed(false);
                  jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"jpg",}, "JPEG Image"));
                  jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"bmp",}, "Windows Bitmap"));
                  jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"tif",}, "Tagged Image File Format"));
                  final SimpleFileFilter png = new SimpleFileFilter(new String[]{"png",}, "Portable Network Graphic");
                  jfc.addChoosableFileFilter(png);
                  jfc.setFileFilter(png);
                  final int option = jfc.showSaveDialog(SpecDisplay.this);
                  if (option == JFileChooser.APPROVE_OPTION) {
                     final SimpleFileFilter sff = (SimpleFileFilter) jfc.getFileFilter();
                     File f = jfc.getSelectedFile();
                     final String ext = sff.getExtension(0);
                     if (!f.getName().endsWith(ext))
                        f = new File(f.getParent(), f.getName() + "." + ext);
                     saveAs(f, ext);
                     userPref.put(kBitmapDir, f.getParent());
                  }
               } catch (final EPQException e1) {
                  ErrorDialog.createErrorMessage(SpecDisplay.this, "Error saving file", e1);
               }
            }
         });
         saveMenu.add(mi);

         if (GnuplotFile.generateSupported()) {
            mi = new JMenuItem("As LaTeX script");
            mi.addActionListener(new AbstractAction() {

               private static final long serialVersionUID = 1L;

               @Override
               public void actionPerformed(ActionEvent e) {
                  try {
                     final String kBitmapDir = "Latex directory";
                     final Preferences userPref = Preferences.userNodeForPackage(SpecDisplay.class);
                     final JFileChooser jfc = new JFileChooser(userPref.get(kBitmapDir, System.getProperty("user.home")));
                     jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"tex",}, "LaTex file"));
                     jfc.setAcceptAllFileFilterUsed(false);
                     final int option = jfc.showSaveDialog(SpecDisplay.this);
                     if (option == JFileChooser.APPROVE_OPTION) {
                        final File f = jfc.getSelectedFile();
                        final GnuplotFile gpf = createGnuplot();
                        gpf.generateLatex(f);
                        userPref.put(kBitmapDir, f.getParent());
                     }
                  } catch (final Exception e1) {
                     ErrorDialog.createErrorMessage(SpecDisplay.this, "Error saving file", e1);
                  }
               }
            });
            saveMenu.add(mi);

            mi = new JMenuItem("As Encapsulated Postscript");
            mi.addActionListener(new AbstractAction() {

               private static final long serialVersionUID = 4L;

               @Override
               public void actionPerformed(ActionEvent e) {
                  try {
                     final String kBitmapDir = "PNG directory";
                     final Preferences userPref = Preferences.userNodeForPackage(SpecDisplay.class);
                     final JFileChooser jfc = new JFileChooser(userPref.get(kBitmapDir, System.getProperty("user.home")));
                     jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"eps",}, "Encapsulated Postscript"));
                     jfc.setAcceptAllFileFilterUsed(false);
                     final int option = jfc.showSaveDialog(SpecDisplay.this);
                     if (option == JFileChooser.APPROVE_OPTION) {
                        final File f = jfc.getSelectedFile();
                        final GnuplotFile gpf = createGnuplot();
                        final NumberFormat df = new HalfUpFormat("0.00");
                        final String yy = df.format((6.0 * SpecDisplay.this.getHeight()) / SpecDisplay.this.getWidth());
                        gpf.generatePostscript(f, "eps enhanced color solid lw 2 size 6.0in, " + yy + "in \"Times-Roman\" 16");
                        userPref.put(kBitmapDir, f.getParent());
                     }
                  } catch (final Exception e1) {
                     ErrorDialog.createErrorMessage(SpecDisplay.this, "Error saving file", e1);
                  }
               }
            });
            saveMenu.add(mi);
         }

         mi = new JMenuItem("As gnuPlot script");
         mi.addActionListener(new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
               try {
                  final String kBitmapDir = "GnuPlot directory";
                  final Preferences userPref = Preferences.userNodeForPackage(SpecDisplay.class);
                  final JFileChooser jfc = new JFileChooser(userPref.get(kBitmapDir, System.getProperty("user.home")));
                  jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"plt",}, "gnuPlot script"));
                  jfc.setAcceptAllFileFilterUsed(false);
                  final int option = jfc.showSaveDialog(SpecDisplay.this);
                  if (option == JFileChooser.APPROVE_OPTION) {
                     final File f = jfc.getSelectedFile();
                     final GnuplotFile gpf = createGnuplot();
                     gpf.setTerminal("window");
                     final PrintWriter pw = new PrintWriter(f, "US-ASCII");
                     gpf.write(pw);
                     pw.close();
                     userPref.put(kBitmapDir, f.getParent());
                  }
               } catch (final Exception e1) {
                  ErrorDialog.createErrorMessage(SpecDisplay.this, "Error saving file", e1);
               }

            }
         });
         saveMenu.add(mi);

         menu.add(saveMenu);
      }

      JMenuItem mi = new JMenuItem("Print");
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x1;

         @Override
         public void actionPerformed(ActionEvent e) {
            PrintUtilities.printComponent(SpecDisplay.this);
         }
      });
      menu.add(mi);

      menu.addSeparator();
      menu.add(new JMenuItem_Zoom(1.0));
      menu.add(new JMenuItem_Zoom(2.0));
      menu.add(new JMenuItem_Zoom(5.0));
      menu.add(new JMenuItem_Zoom(0.5));
      menu.add(new JMenuItem_Zoom(0.2));
      menu.addSeparator();

      final JMenu scalingMenu = new JMenu("Ordinate Scale");
      scalingMenu.add(new JMenuItem_Axis(jButtonGroup_Axis, AXIS_MODE.LINEAR));
      scalingMenu.add(new JMenuItem_Axis(jButtonGroup_Axis, AXIS_MODE.LOG));
      scalingMenu.add(new JMenuItem_Axis(jButtonGroup_Axis, AXIS_MODE.SQRT));
      menu.add(scalingMenu);

      final JMenu compareMenu = new JMenu("Spectrum Comparison");

      compareMenu.add(new JMenuItem_Compare(jButtonGroup_Scaling, SCALING_MODE.COUNTS));
      compareMenu.add(new JMenuItem_Compare(jButtonGroup_Scaling, SCALING_MODE.COUNTS_PER_NA_S));
      compareMenu.add(new JMenuItem_Compare(jButtonGroup_Scaling, SCALING_MODE.COUNTS_PER_NA_S_EV));
      compareMenu.add(new JMenuItem_Compare(jButtonGroup_Scaling, SCALING_MODE.MAX_PEAK));
      compareMenu.add(new JMenuItem_Compare(jButtonGroup_Scaling, SCALING_MODE.INTEGRAL));
      compareMenu.add(new JMenuItem_Compare(jButtonGroup_Scaling, SCALING_MODE.REGION_INTEGRAL));
      setButtonGroupSelection(jButtonGroup_Scaling, mCurrentScalingMode.toString());
      compareMenu.addSeparator();

      {
         final JCheckBoxMenuItem cmi = new JCheckBoxMenuItem("Stagger spectra");
         cmi.setState(getStaggerOffset() != 0);
         cmi.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
               final JCheckBoxMenuItem cmi = (JCheckBoxMenuItem) e.getSource();
               setStaggerOffset(cmi.getState() ? 10 : 0);
               Preferences.userNodeForPackage(SpecDisplay.class).putInt("Stagger", getStaggerOffset());
            }
         });
         compareMenu.add(cmi);
      }
      menu.add(compareMenu);

      final JMenu klmMenu = new JMenu("KLM Labels");
      {
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.ELEMENT));
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.ELEMENT_ABBREV));
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.LARGE_ELEMENT));
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.SIEGBAHN));
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.IUPAC));
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.FAMILY));
         klmMenu.add(new JMenuItem_Label(jButtonGroup_Label, LabelType.NONE));
         klmMenu.addSeparator();
         final JCheckBoxMenuItem jCheckBoxMenuItem = new JCheckBoxMenuItem(new LabelAllLinesAction());
         jCheckBoxMenuItem.setSelected(mLabelThreshold < 0.01);
         klmMenu.add(jCheckBoxMenuItem);
      }

      menu.add(klmMenu);

      menu.addSeparator();

      mi = new JMenuItem("Create an ROI...");
      mi.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            ROIDialog rd;
            Container c;
            for (c = getParent(); !((c instanceof Dialog) || (c instanceof Frame)); c = c.getParent());
            if (c instanceof Dialog)
               rd = new ROIDialog((Dialog) c, "Create an ROI", true);
            else if (c instanceof Frame)
               rd = new ROIDialog((Frame) c, "Create an ROI", true);
            else
               rd = new ROIDialog((Frame) null, "Create an ROI", true);
            {
               double min = 0.0, max = 10000.0;
               final List<ISpectrumData> data = getData();
               for (final ISpectrumData spec : data) {
                  if (spec.getZeroOffset() < min)
                     min = spec.getZeroOffset();
                  if (SpectrumUtils.maxEnergyForChannel(spec, spec.getChannelCount()) > max)
                     max = SpectrumUtils.maxEnergyForChannel(spec, spec.getChannelCount() - 1);
               }
               rd.setLimits(1000.0 * Math.floor(min / 1000.0), 1000.0 * Math.ceil(max / 1000.0));
            }
            rd.setLocationRelativeTo(c);
            rd.setVisible(true);
            if (rd.isOk()) {
               final double[] range = rd.getRange();
               if (rd.isZoom())
                  zoomTo(range[0], range[1]);
               else
                  addRegion(range[0], range[1]);
            }
         }
      });
      menu.add(mi);

      return menu;
   }

   /**
    * getDefaultRegionMenu - Get the default popup menu to display when the user
    * selects a popup menu from over a region.
    * 
    * @return PopupMenu
    */
   public JPopupMenu getDefaultRegionMenu() {
      final JPopupMenu menu = new JPopupMenu("Region Menu");
      JMenuItem mi = new JMenuItem("Clear regions");
      mi.setMnemonic('R');
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x1;

         @Override
         public void actionPerformed(ActionEvent e) {
            clearRegionList();
         }
      });
      menu.add(mi);

      mi = new JMenuItem("Integrate peak (background corrected)");
      mi.setMnemonic('S');
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x1;

         @Override
         public void actionPerformed(ActionEvent e) {
            backgroundCorrectedPeakIntegration();
         }
      });
      menu.add(mi);

      mi = new JMenuItem("Integrate peak");
      mi.setMnemonic('P');
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x1;

         @Override
         public void actionPerformed(ActionEvent e) {
            peakIntegration();
         }
      });
      menu.add(mi);

      mi = new JMenuItem("Three ROI integration");
      mi.setMnemonic('3');
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x1;

         @Override
         public void actionPerformed(ActionEvent e) {
            threeROIpeakIntegration();
         }
      });
      menu.add(mi);

      menu.add(new JMenuItem_Zoom(0.0));
      menu.add(new JMenuItem_Zoom(1.0));

      mi = new JMenuItem("Copy Region(s) To Clipboard");
      mi.setMnemonic('C');
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x1;

         @Override
         public void actionPerformed(ActionEvent e) {
            copyRegionsToClipboard();
         }
      });
      menu.add(mi);

      mi = new JMenuItem("ID Sum Peak");
      mi.setMnemonic('I');
      mi.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 1L;

         @Override
         public void actionPerformed(ActionEvent e) {
            final JPopupMenu pu = new JPopupMenu();
            if (mRegions.size() == 1) {
               final Set<SumPeak> sums = idSumPeak();
               if (sums != null) {
                  final JMenuItem none = new JMenuItem("None");
                  pu.add(none);
                  for (final SumPeak sum : sums)
                     pu.add(new SumMenuItem(sum));
               }
            } else {
               final JMenuItem none = new JMenuItem("Please, select only one region.");
               pu.add(none);
            }
            final Rectangle r = SpecDisplay.this.getBounds();
            pu.show(SpecDisplay.this, r.x + (r.width / 4), r.y + (r.height / 2));
         }

      });
      menu.add(mi);

      return menu;
   }

   /**
    * zoomToRegion - Zoom the horizontal display to contain all the currently
    * selected regions.
    */
   public void zoomToRegion() {
      if (!mRegions.mList.isEmpty()) {
         double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
         for (final Region r : mRegions.mList) {
            if (r.getLowEnergy() < min)
               min = r.getLowEnergy();
            if (r.getHighEnergy() > max)
               max = r.getHighEnergy();
         }
         if ((max - min) < 1.0) {
            max = ((max + min) / 2) + 0.5;
            min = max - 1.0;
         }
         mRegions.mList.clear();
         zoomTo(min, max);
      }
   }

   public void zoomTo(double low, double high) {
      mEMin = low;
      mEMax = high;
      repaint();
   }

   /**
    * clearRegionList - Clear all user defined regions (not ROIs).
    */
   public void clearRegionList() {
      mRegions.mList.clear();
      repaint();
   }

   /**
    * sumEventsInRegions - Sum all the events in the selected regions.
    */
   public void backgroundCorrectedPeakIntegration() {
      final String header = "Peak-Bkgd\tUncertainty\tS-to-N\tSpectrum";
      String res = "";
      final NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(1);
      nf.setGroupingUsed(false);
      for (final Region r : mRegions.mList) {
         if (res.length() > 0)
            res += "\n";
         res += r.toTabbedString() + "\n" + header;
         final List<ISpectrumData> data = getData();
         for (final ISpectrumData sd : data) {
            final double[] bci = SpectrumUtils.backgroundCorrectedIntegral(sd, r.getLowEnergy(), r.getHighEnergy() - ENERGY_EPS);
            final double err = bci[1];
            final double sum = bci[2];
            final double bkg = bci[3];
            res += "\n" + nf.format(sum - bkg) + "\t" + nf.format(err) + "\t" + nf.format((sum - bkg) / err) + "\t" + sd.toString();
         }
      }
      setTemporaryAnnotation(res);
   }

   public void peakIntegration() {
      final String header = "Total\tUncertainty\tChannels\tSpectrum";
      String res = "";
      final HalfUpFormat nf = new HalfUpFormat("#,##0");
      nf.setGroupingUsed(true);
      for (final Region r : mRegions.mList) {
         if (res.length() > 0)
            res += "\n";
         res += r.toTabbedString() + "\n" + header;
         final List<ISpectrumData> data = getData();
         for (final ISpectrumData sd : data) {
            final int minCh = SpectrumUtils.channelForEnergy(sd, r.getLowEnergy() + ENERGY_EPS);
            final int maxCh = SpectrumUtils.channelForEnergy(sd, r.getHighEnergy() - ENERGY_EPS) + 1;
            final double sum = SpectrumUtils.sumCounts(sd, minCh, maxCh);
            res += "\n" + nf.format(sum) + "\t" + nf.format(Math.sqrt(sum)) + "\t" + Integer.toString(maxCh - minCh) + "\t" + sd.toString();
         }
      }
      setTemporaryAnnotation(res);
   }

   public void threeROIpeakIntegration() {
      if (mRegions.size() == 3) {
         String res = "Peak-Bkgd\tUncertainty\tS-to-N\tSpectrum";
         final HalfUpFormat nf = new HalfUpFormat("#,##0");
         nf.setGroupingUsed(true);
         final ArrayList<Region> rois = new ArrayList<Region>(mRegions.mList);
         Collections.sort(rois);
         final Region low = rois.get(0), onPeak = rois.get(1), high = rois.get(2);
         for (final ISpectrumData sd : getData()) {
            final double[] tmp = SpectrumUtils.backgroundCorrectedIntegral(sd, onPeak.getLowEnergy(), onPeak.getHighEnergy(), low.getLowEnergy(),
                  low.getHighEnergy(), high.getLowEnergy(), high.getHighEnergy());
            res += "\n" + nf.format(tmp[0]) + "\t" + nf.format(tmp[1]) + "\t" + nf.format(tmp[0] / tmp[1]) + "\t" + sd.toString();
         }
         setTemporaryAnnotation(res);
      } else
         ErrorDialog.createErrorMessage(this, "Three region integration", "Please highlight three and only three regions.",
               "Three region integration requires you to highlight a low energy\nbackground window, an on-peak window and a high energy background window.");
   }

   public void zoomToAll() {
      mZoom = DEFAULT_VZOOM;
      Preferences.userNodeForPackage(SpecDisplay.class).putDouble("Zoom", mZoom);
      autoScaleV(mZoom);
      autoScaleH(0.0, 100.0);
   }

   // This procedure takes spectral data from a selected region and attaches
   // it to the system clipboard.
   public void copyRegionsToClipboard() {
      // Find the system clipboard
      final Clipboard clip = getToolkit().getSystemClipboard();
      // Turn the data from the selected spectrum into a string
      // and then turn the string into a transferable object
      final StringSelection ss = new StringSelection(writeSpectralDataToString());
      // Attach the data to the clipboard
      clip.setContents(ss, ss);
   }

   private class SumMenuItem extends JMenuItem {
      private static final long serialVersionUID = -1428067652623276359L;

      private final SumPeak mSumPeak;

      public SumMenuItem(SumPeak sp) {
         super(sp.toString());
         mSumPeak = sp;
         addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               SpecDisplay.this.addKLM(mSumPeak);
               clearRegions();
            }
         });
      }
   }

   public Set<SumPeak> idSumPeak() {
      final int[] lines = new int[]{XRayTransition.KA1, XRayTransition.KB1, XRayTransition.LA1, XRayTransition.MA1};
      final Region r = mRegions.get(0);
      final double low = ToSI.eV(r.getLowEnergy());
      final double high = ToSI.eV(r.getHighEnergy());
      return KLMLine.SumPeak.suggestSumPeaks(getMarkedElements(), low, high, 2, lines);
   }

   // This class is used to hold an image while on the clipboard.
   private static class TransferableImage implements Transferable {
      private final Image mImage;

      public TransferableImage(Image img) {
         mImage = img;
      }

      // Returns supported flavors
      @Override
      public DataFlavor[] getTransferDataFlavors() {
         return new DataFlavor[]{DataFlavor.imageFlavor};
      }

      // Returns true if flavor is supported
      @Override
      public boolean isDataFlavorSupported(DataFlavor flavor) {
         return DataFlavor.imageFlavor.equals(flavor);
      }

      // Returns image
      @Override
      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
         if (!DataFlavor.imageFlavor.equals(flavor))
            throw new UnsupportedFlavorException(flavor);
         return mImage;
      }
   }

   /**
    * copyBitmapToClipboard - Copy the control display as a image to the
    * clipboard.
    */
   public void copyBitmapToClipboard() {
      final int wXtra = (PNG_SCALE * getWidth()) / 20;
      final int hXtra = (PNG_SCALE * getHeight()) / 20;
      final int cWidth = ((PNG_SCALE * getWidth()) + (2 * wXtra));
      final int cHeight = ((PNG_SCALE * getHeight()) + (2 * hXtra));
      final BufferedImage bi = new BufferedImage(cWidth, cHeight, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics2D g = bi.createGraphics();
      drawHiresImage(g, wXtra, hXtra, cWidth, cHeight);
      Clipboard clp = getToolkit().getSystemClipboard();
      if (clp != null)
         clp.setContents(new TransferableImage(bi), null);
      else
         System.err.println("The clipboard is not available!");
   }

   /**
    * Save the current spectrum display bitmap in the specified file with the
    * specified format ("tiff", "jpeg", "png" etc.)
    * 
    * @param outfile
    * @param formatName
    * @throws EPQException
    */
   public Dimension saveAs(File outfile, String formatName) throws EPQException {
      return saveAsImage(outfile, formatName);
   }

   // This procedure obtains spectral data and then returns it in a
   // comma delimited string
   public String writeSpectralDataToString() {
      // Determines if all the channels have the same width.
      final StringBuffer specData = new StringBuffer();
      if (allVisableSpectrumAreTheSameWidth())
         writeDataInMultipleColumns(specData);
      else
         writeDataInThreeColumns(specData);
      // Return a string version of the processed data
      return specData.toString();
   }

   // This function writes the data in two columns, seperated by a tab. Column
   // one is the energy and column two is the count
   public void writeDataInThreeColumns(StringBuffer AllSpectrumData) {
      // Initializes the list of spectra to the beginning
      final List<ISpectrumData> data = getData();
      for (final ISpectrumData sd : data) {
         // Initializes the list of highlighted regions to the beginning
         // Declares a stringbuffer to hold the data for each spectrum
         final StringBuffer IndividualSpectrumData = new StringBuffer();
         IndividualSpectrumData.append(sd.toString());
         // cycles through each region
         for (final Region r : mRegions.mList) {
            // Finds the start (left side) of the highlighted region
            final int min = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, r.getLowEnergy()));
            // Finds the end (right side) of the highlighted region
            final int max = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, r.getHighEnergy() - ENERGY_EPS));
            // for each data point in the region, write out the energy and
            // the count at that specific point
            for (int i = min; i < max; i++)
               IndividualSpectrumData.append(
                     SpectrumUtils.minEnergyForChannel(sd, i) + '\t' + SpectrumUtils.maxEnergyForChannel(sd, i) + '\t' + sd.getCounts(i) + '\n');
         }
         // Adds one set of complete spectrum data to the larger stringbuffer
         AllSpectrumData.append(IndividualSpectrumData);
      }
   }

   // This procedure will write out the data in multiple columns, depending on
   // how many spectra there are. The format is lowE, highE, Spectrum #1,
   // Spectrum #2, etc. seperated by tabs
   public void writeDataInMultipleColumns(StringBuffer AllSpectrumData) {
      assert (allVisableSpectrumAreTheSameWidth());
      // Initializes the list of regions to the beginning
      for (final Region r : mRegions.mList) {
         // Initializes the list of spectra to the beginning
         final List<ISpectrumData> data = getData();
         final ISpectrumData sd0 = data.get(0);
         // Finds the start (left side) of the highlighted region
         final int min = SpectrumUtils.bound(sd0, SpectrumUtils.channelForEnergy(sd0, r.getLowEnergy()));
         // Finds the end (right side) of the highlighted region
         final int max = SpectrumUtils.bound(sd0, SpectrumUtils.channelForEnergy(sd0, r.getHighEnergy() - ENERGY_EPS));
         // For each channel
         for (int i = min; i < max; i++) {
            // Re-initialize the list of spectra to the beginning
            AllSpectrumData.append(SpectrumUtils.minEnergyForChannel(sd0, i) + "\t" + SpectrumUtils.maxEnergyForChannel(sd0, i) + "\t");
            for (final Object element : data) {
               final ISpectrumData sd = (ISpectrumData) element;
               // Add the current spectrum's count to the stringbuffer
               AllSpectrumData.append("\t" + sd.getCounts(i));
            }
            AllSpectrumData.append('\n');
         }
      }
   }

   /**
    * areAllVisableSpectrumTheSameWidth - This procedure checks if all the
    * visible spectra are the same width. It returns true if they are all the
    * same width, false if at least one is of a different width
    * 
    * @return boolean
    */
   public boolean allVisableSpectrumAreTheSameWidth() {
      final List<ISpectrumData> data = getData();
      // Initialize the list of spectra to the beginning
      final Iterator<ISpectrumData> i0 = data.iterator();
      if (i0.hasNext()) {
         final ISpectrumData sd = i0.next();
         while (i0.hasNext()) {
            final ISpectrumData next = i0.next();
            if ((next.getChannelCount() != sd.getChannelCount()) || (next.getChannelWidth() != sd.getChannelWidth())
                  || (next.getZeroOffset() != sd.getZeroOffset()))
               return false;
         }
      }
      return true;
   }

   /**
    * SpecDisplay - Constructs a SpecDisplay object.
    */

   public SpecDisplay() {
      super();
      setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
      {
         // Restore previous preferences
         final Preferences userPref = Preferences.userNodeForPackage(SpecDisplay.class);
         try {
            mLabelType = LabelType.valueOf(userPref.get(LabelType.class.getName(), LabelType.ELEMENT_ABBREV.name()));
         } catch (final IllegalArgumentException e2) {
            mLabelType = LabelType.ELEMENT_ABBREV;
         }
         setStaggerOffset(userPref.getInt("Stagger", 0));
         try {
            setSpectrumScalingMode(SCALING_MODE.valueOf(userPref.get("Scaling", SCALING_MODE.COUNTS.name())));
         } catch (final IllegalArgumentException e1) {
            setSpectrumScalingMode(SCALING_MODE.COUNTS);
         }
         try {
            setAxisScalingMode(AXIS_MODE.valueOf(userPref.get("Ordinate", AXIS_MODE.LINEAR.name())));
         } catch (final IllegalArgumentException e1) {
            setAxisScalingMode(AXIS_MODE.LINEAR);
         }
         mZoom = userPref.getDouble("Zoom", DEFAULT_VZOOM);
      }
      // Inherited
      setPreferredSize(new Dimension(161, 100));
      setDoubleBuffered(false);
      // Add event handlers
      this.addMouseWheelListener(new MouseWheelListener() {

         @Override
         public void mouseWheelMoved(MouseWheelEvent e) {
            final int rot = e.getWheelRotation();
            zoomInBy(rot > 0 ? 1.0 + (0.2 * rot) : 1.0 / (1.0 - (0.2 * rot)));
         }
      });

      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            final int xx = me.getX();
            final int yy = me.getY();
            if (me.isPopupTrigger())
               doPopup(xx, yy);
            else if ((me.getButton() == MouseEvent.BUTTON1) && (mActionMode == ActionMode.None)) {
               if ((mCursorPosition == Integer.MIN_VALUE) && (mVertScrollPos == Integer.MIN_VALUE))
                  if (mOrdRect.contains(xx, yy))
                     startVerticalZoom(yy);
                  else if (mPlotRect.contains(xx, yy)) {
                     if (me.isControlDown())
                        startShift(xx);
                     else
                        startDrag(xx);
                  }
            }
         }

         public void startVerticalZoom(int y) {
            assert (mCursorPosition == Integer.MIN_VALUE);
            mActionMode = ActionMode.Zoom;
            mVertScrollPos = y;
         }

         public void performVerticalZoom(int y) {
            assert (mCursorPosition == Integer.MIN_VALUE);
            assert mActionMode == ActionMode.Zoom;
            final double delta = y - mVertScrollPos;
            if (delta > 0)
               zoomInBy(Math.max(0.01, 1.0 - (delta / mOrdRect.height)));
            else
               zoomInBy(mOrdRect.height / (mOrdRect.height - Math.min(mOrdRect.height - 2, -delta)));
            mVertScrollPos = Integer.MIN_VALUE;
            mActionMode = ActionMode.None;
         }

         @Override
         public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger())
               doPopup(me.getX(), me.getY());
            else if (me.getButton() == MouseEvent.BUTTON1) {
               switch (mActionMode) {
                  case Cursor :
                     if (me.getClickCount() == 2)
                        hideCursor();
                     else
                        displayCursor(me.getX());
                     break;
                  case Drag :
                     if (me.getClickCount() == 2) {
                        cancelDrag();
                        mActionMode = ActionMode.Cursor;
                        displayCursor(me.getX());
                     } else
                        endDrag(me.getX());
                     break;
                  case Shift :
                     endShift(me.getX());
                     break;
                  case Zoom :
                     performVerticalZoom(me.getY());
                     break;
                  case None :
                     if (me.getClickCount() == 2) {
                        mActionMode = ActionMode.Cursor;
                        displayCursor(me.getX());
                     }
                     break;
               }
            }
         }

      });

      this.addMouseMotionListener(new MouseMotionAdapter() {
         @Override
         public void mouseDragged(MouseEvent me) {
            if (isShift()) {
               performShift(me.getX());
            } else {
               if (mDragStart != Integer.MIN_VALUE)
                  performDrag(me.getX());

            }
         }
      });
      mTimer = new javax.swing.Timer(10000, new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (mTempAnnotation != null) {
               mTempAnnotation = null;
               repaint();
               mTimer.stop();
            }
         }
      });
      mTimer.stop();
   }

   /**
    * addSpectrum - Add a spectrum to the display list using the specified scale
    * and color index.
    * 
    * @param sd
    *           ISpectrumData
    * @param colorIndex
    *           int
    * @param scaling
    *           double
    */
   public synchronized void addSpectrum(ISpectrumData sd, int colorIndex, double scaling) {
      mProperties.put(sd, new DisplayProperties(scaling, colorIndex % mDataColor.length));
      mData.add(sd);
      repaint();
   }

   public Color[] getSpectrumColors(Collection<ISpectrumData> specs) {
      Color[] res = new Color[specs.size()];
      int i = 0;
      for (ISpectrumData spec : specs)
         if (mProperties.containsKey(spec))
            res[i++] = mDataColor[mProperties.get(spec).mColorIndex % mDataColor.length];
         else
            res[i++] = Color.darkGray;
      return res;
   }

   /**
    * addSpectrum - Add the specified ISpectrumData to the display control.
    * 
    * @param sd
    *           ISpectrumData - The spectrum to add to the display list.
    */
   public synchronized void addSpectrum(ISpectrumData sd) {
      if (!mData.contains(sd)) {
         int colorIndex = 0;
         if (mData.size() > 0) {
            // Find the lowest color index with the smallest number of uses
            final int[] uses = new int[mDataColor.length];
            for (final Object element : mData) {
               final DisplayProperties dp = mProperties.get(element);
               if (dp != null)
                  for (int j = 0; j < mDataColor.length; ++j)
                     if (j == dp.mColorIndex)
                        ++uses[j];
            }
            int min = uses[0];
            for (int i = 1; i < uses.length; ++i)
               if (uses[i] < min) {
                  min = uses[i];
                  colorIndex = i;
               }
         }
         addSpectrum(sd, colorIndex, computeScale(sd));
      }
   }

   /**
    * removeSpectrum - Remove the spectrum defined by the specified
    * ISpectrumData from the display list.
    * 
    * @param sd
    *           ISpectrumData - The spectrum to remove.
    */
   public synchronized void removeSpectrum(ISpectrumData sd) {
      boolean repaint = false;
      if (mData.contains(sd)) {
         // If we remove the first spectrum rescale
         if ((mData.get(0) == sd) && (mData.size() > 1)) {
            final DisplayProperties dp1 = mProperties.get(mData.get(1));
            assert (dp1 != null);
            for (int i = 1; i < mData.size(); ++i) {
               final DisplayProperties dp = mProperties.get(mData.get(i));
               assert (dp != null);
               dp.mScale /= dp1.mScale;
            }
         }
         mProperties.remove(sd);
         mData.remove(sd);
         repaint = true;
      }
      if (repaint)
         repaint();
   }

   /**
    * clearAllSpectra - Remove all spectra from the control display.
    */
   public synchronized void clearAllSpectra() {
      mData.clear();
      mProperties.clear();
      repaint();
   }

   /**
    * Returns the number of spectra currently attached to this SpecDisplay
    * object. Some may be hidden.
    * 
    * @return int
    */
   public synchronized int spectrumCount() {
      return mData.size();
   }

   /**
    * SetHRange - Set the horizontal range that is displayed within the plot
    * area of the control.
    * 
    * @param min
    *           double - The minimum value in eV.
    * @param max
    *           double - The maximum value in eV.
    */
   public void setHRange(double min, double max) {
      if ((mEMin != min) || (mEMax != max)) {
         mEMin = min;
         mEMax = max;
         repaint();
      }
   }

   /**
    * setSpectrumScalingMode - Sets the mode by which spectra are scaled
    * relative to one another. (Valid modes are listed in ScalingModes)
    * 
    * @param mode
    *           SCALING_MODE
    */
   public void setSpectrumScalingMode(SCALING_MODE mode) {
      if ((mCurrentScalingMode != mode) || (mode == SCALING_MODE.REGION_INTEGRAL)) {
         mCurrentScalingMode = mode;
         setButtonGroupSelection(jButtonGroup_Scaling, mode.toString());
         if (mCurrentScalingMode == SCALING_MODE.REGION_INTEGRAL)
            if (mRegions.mList.size() > 0)
               mScalingRegions = new Regions(mRegions);
            else
               mCurrentScalingMode = SCALING_MODE.INTEGRAL;
         if (mCurrentScalingMode == SCALING_MODE.REGION_INTEGRAL) {
            final StringBuffer sb = new StringBuffer();
            boolean first = true;
            for (final Region r : mScalingRegions.mList) {
               sb.append(first ? "Fraction of integral " : ", ");
               sb.append(r.toString());
               first = false;
            }
            sb.append(" (%)");
            mScaleText = sb.toString();
         } else
            mScaleText = mCurrentScalingMode.toString();
         // Now perform the rescaling immediately...
         for (final Object element : getData()) {
            final ISpectrumData sd = (ISpectrumData) element;
            final DisplayProperties dp = mProperties.get(sd);
            assert dp != null;
            dp.mScale = computeScale(sd);
         }
         rescaleV();
         repaint();
      }
   }

   /**
    * Compute the scale factor dependent upon the current scaling mode.
    * 
    * @param sd0
    * @param sd
    * @return double
    */
   private double computeScale(ISpectrumData sd) {
      // Default to 1.0 for sd==sd0 or mCurrentScalingMode==SCALING_NONE
      switch (mCurrentScalingMode) {
         case COUNTS_PER_NA_S_EV : {
            final SpectrumProperties sp = sd.getProperties();
            final double tmp = SpectrumUtils.getAverageFaradayCurrent(sp, 1.0) * sp.getNumericWithDefault(SpectrumProperties.LiveTime, 60.0)
                  * sd.getChannelWidth();
            return 1.0 / tmp;
         }
         case COUNTS_PER_NA_S : {
            final SpectrumProperties sp = sd.getProperties();
            final double dose = SpectrumUtils.getAverageFaradayCurrent(sp, 1.0) * sp.getNumericWithDefault(SpectrumProperties.LiveTime, 60.0);
            return 1.0 / dose;
         }
         case INTEGRAL : {
            final double tc = SpectrumUtils.totalCounts(sd, true);
            return tc > 0.0 ? 100.0 / tc : 1.0;
         }
         case MAX_PEAK : {
            final int MIN_CHANNEL = 20;
            final double c = sd.getCounts(SpectrumUtils.maxChannel(sd, MIN_CHANNEL, sd.getChannelCount()));
            return c > 0.0 ? 100.0 / c : 100.0;
         }
         case REGION_INTEGRAL : {
            if (mScalingRegions != null) {
               double c = 0.0;
               for (final Region r : mScalingRegions.mList) {
                  c += SpectrumUtils.integrate(sd, r.getLowEnergy(), r.getHighEnergy());
               }
               return c > 0.0 ? 100.0 / c : 100.0;
            } else
               return 100.0;
         }
         default :
            return 1.0;
      }
   }

   /**
    * getSpectrumScalingMode - Returns the mode by which spectra are scaled
    * relative to one another
    * 
    * @return SCALING_MODE
    */
   public SCALING_MODE getSpectrumScalingMode() {
      return mCurrentScalingMode;
   }

   private void setButtonGroupSelection(ButtonGroup bg, String text) {
      if (bg != null) {
         final Enumeration<AbstractButton> e = bg.getElements();
         if (e.hasMoreElements())
            for (AbstractButton btn = e.nextElement(); e.hasMoreElements(); btn = e.nextElement())
               if (btn.getText().equals(text)) {
                  btn.setSelected(true);
                  break;
               }
      }
   }

   /**
    * setAxisScalingMode - set the vertical axis scaling mode to one of the
    * values in AxisModes.
    * 
    * @param mode
    *           AXIS_MODES
    */
   public void setAxisScalingMode(AXIS_MODE mode) {
      if (mVAxisType != mode) {
         mVAxisType = mode;
         setButtonGroupSelection(jButtonGroup_Axis, mode.toString());
         setButtonGroupSelection(jButtonGroup_AxisSimple, mode.toString());
         repaint();
      }
   }

   /**
    * getAxisScalingMode - Returns the current vertical axis scaling mode.
    * 
    * @return AXIS_MODES
    */
   public AXIS_MODE getAxisScalingMode() {
      return mVAxisType;
   }

   /**
    * Is it possible to drag regions on the spectrum window?
    * 
    * @return Returns the dragEnabled.
    */
   public boolean isDragEnabled() {
      return mDragEnabled;
   }

   /**
    * Specifies whether it is possible to drag regions on the spectum window.
    * 
    * @param dragEnabled
    *           The value to which to set dragEnabled.
    */
   public void setDragEnabled(boolean dragEnabled) {
      if (mDragEnabled != dragEnabled) {
         if (mActionMode == ActionMode.Drag)
            cancelDrag();
         mDragEnabled = dragEnabled;
      }
   }

   /**
    * Should an image display within the spectrum control when an image is
    * available and only one spectrum is visible?
    * 
    * @return boolean
    */
   public boolean getShowImage() {
      return mShowImage;
   }

   /**
    * Should an image display within the spectrum control when an image is
    * available and only one spectrum is visible? Default is false
    * 
    * @param showImage
    *           true to show an image
    */
   public void setShowImage(boolean showImage) {
      if (mShowImage != showImage) {
         mShowImage = showImage;
         repaint();
      }
   }

   /**
    * Gets the current value assigned to staggerOffset
    * 
    * @return Returns the staggerOffset.
    */
   public int getStaggerOffset() {
      return mStaggerOffset;
   }

   /**
    * Sets the value assigned to staggerOffset.
    * 
    * @param staggerOffset
    *           The value to which to set staggerOffset.
    */
   public void setStaggerOffset(int staggerOffset) {
      if (mStaggerOffset != staggerOffset) {
         mStaggerOffset = staggerOffset;
         repaint();
      }
   }

   /**
    * Specify the type of label to use on KLM lines (One of LONG_LABEL,
    * SHORT_LABEL or LARGE_LABEL)
    * 
    * @param lt
    */
   public void setKLMLabelType(LabelType lt) {
      if (mLabelType != lt) {
         mLabelType = lt;
         setButtonGroupSelection(jButtonGroup_Label, mLabelType.toString());
         repaint();
      }
   }

   /**
    * Exchange one spectrum in the display for another.
    * 
    * @param old
    * @param replacement
    */
   public synchronized void replaceSpectrum(ISpectrumData old, ISpectrumData replacement) {
      if (mData.contains(old) && mProperties.containsKey(old)) {
         mData.set(mData.indexOf(old), replacement);
         mProperties.put(replacement, mProperties.get(old));
         mProperties.remove(old);
      }
   }

   /**
    * Trim the specified spectrum according to the currently defined region of
    * interests.
    * 
    * @param spec
    * @return ISpectrumData
    */
   public ISpectrumData trimSpectrum(ISpectrumData spec) {
      if (mRegions.mList.size() > 0) {
         for (final Region reg : mRegions.mList)
            spec = SpectrumUtils.trimSpectrum(spec, reg.getLowEnergy(), reg.getHighEnergy() - ENERGY_EPS);
         return spec;
      } else
         return spec;
   }

   public Regions getRegions() {
      return mRegions;
   }

   private void setTemporaryAnnotation(String textAnnotation) {
      mTempAnnotation = textAnnotation;
      repaint();
      mTimer.stop();
      if (mTempAnnotation != null)
         mTimer.start();
   }

   public String getTextAnnotation() {
      return mTempAnnotation != null ? mTempAnnotation : mUserAnnotation;
   }

   public void setTextAnnotation(String textAnnotation) {
      if ((mUserAnnotation == null) || (!mUserAnnotation.equals(textAnnotation))) {
         mUserAnnotation = textAnnotation;
         repaint();
      }
   }

   public Set<Element> getMarkedElements() {
      final Set<Element> elms = new TreeSet<Element>();
      for (final KLMLine klm : mLines)
         elms.add(klm.getShell().getElement());
      return elms;
   }

}
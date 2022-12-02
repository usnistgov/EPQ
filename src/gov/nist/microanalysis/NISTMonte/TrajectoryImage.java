package gov.nist.microanalysis.NISTMonte;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Creates images of the electron trajectories.
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

final public class TrajectoryImage extends BufferedImage implements ActionListener {

   private double mXMin, mXScale;
   private double mYMin, mYScale;
   private int mMaxTrajectories = 100;
   private int mTrajectoryCount = 0;
   private int startTrajectory = 0;
   private boolean mLabel = true;

   // private double[] mPrevPosition = new double[3];
   private final Color mBackground = new Color(0xFF, 0xFF, 0xFF, 0x0);
   private final Map<Material, Color> mMaterialMap = new TreeMap<Material, Color>();
   private int mNextColor = 0;

   private static final Color[] mColors = {Color.green, Color.blue, Color.red, Color.orange, Color.yellow, Color.pink, Color.magenta, Color.cyan,
         Color.black, Color.gray};
   private static final Color BACKSCATTER_COLOR = Color.darkGray;
   private static final Color TEXT_COLOR = Color.gray;

   transient Graphics2D mGraphics;

   private void updateText() {
      if (mLabel) {
         mGraphics.setColor(TEXT_COLOR);
         mGraphics.setFont(new Font("sanserif", Font.PLAIN, (10 * this.getWidth()) / 256));
         final NumberFormat nf = NumberFormat.getInstance();
         nf.setMaximumFractionDigits(2);
         double[] fov = getFOV();
         final String tmp = nf.format(1.0e6 * fov[0]) + " \u00B5m \u00D7 " + nf.format(1.0e6 * fov[1]) + " \u00B5m";
         mGraphics.drawString(tmp, getWidth() - (mGraphics.getFontMetrics().stringWidth(tmp) + 2), mGraphics.getFontMetrics().getHeight());
      }
   }

   public double[] getFOV() {
      return new double[]{getWidth() / mXScale, getHeight() / mYScale};
   }

   public boolean getLabel() {
      return mLabel;
   }

   public void setLabel(boolean label) {
      mLabel = label;
   }

   /**
    * assignColor - Assigns a color to a Material. If you require consistent
    * coloration of materials between instances of MCSS_TrajectoryImage you can
    * preregister all the relevant Material instances using this function.
    * Otherwise colors will be assigned on a first imact basis and are likely to
    * change between simulations.
    * 
    * @param mat
    *           Material
    * @return Color
    */
   public Color assignColor(Material mat) {
      Color res = mMaterialMap.get(mat);
      if (res == null) {
         res = mColors[(mNextColor++) % mColors.length];
         mMaterialMap.put(mat, res);
      }
      return res;
   }

   /**
    * drawLine - Draws a trajectory line from p0 to p1
    * 
    * @param p0
    *           double[] - Initial point
    * @param p1
    *           double[] - Final point
    */
   private void drawLine(Graphics gr, double[] p0, double[] p1, Material mat) {
      final double k = 0.9 * (1.0 - Math.abs(Math2.normalize(Math2.minus(p0, p1))[2]));
      final Color base = assignColor(mat);
      final Color newC = new Color((int) (base.getRed() + (k * (0xFF - base.getRed()))), (int) (base.getGreen() + (k * (0xFF - base.getGreen()))),
            (int) (base.getBlue() + (k * (0xFF - base.getBlue()))));
      gr.setColor(newC);
      gr.drawLine((int) (mXScale * (p0[0] - mXMin)), (int) (mYScale * (p0[2] - mYMin)), (int) (mXScale * (p1[0] - mXMin)),
            (int) (mYScale * (p1[2] - mYMin)));
   }

   private void drawBS(Graphics gr, double[] p0, double[] p1) {
      gr.setColor(BACKSCATTER_COLOR);
      gr.drawLine((int) (mXScale * (p0[0] - mXMin)), (int) (mYScale * (p0[2] - mYMin)), (int) (mXScale * (p1[0] - mXMin)),
            (int) (mYScale * (p1[2] - mYMin)));
   }

   /**
    * MCSS_TrajectoryImage - Creates a MCSS_TrajectoryImage instance with the
    * standard grey-scale palette of the specified size (width and height). The
    * scale determines the size of the volume represented by the image.
    * 
    * @param width
    *           int
    * @param height
    *           int
    * @param scale
    *           double - a typical scale might be 4.0e-6 meters
    */
   public TrajectoryImage(int width, int height, double scale, boolean label) {
      super(width, height, TYPE_4BYTE_ABGR);
      mXMin = -scale / 2.0;
      mXScale = width / scale;
      mYMin = -scale / 10.0;
      mYScale = height / scale;
      mGraphics = createGraphics();
      mGraphics.setBackground(new Color(0xFF, 0xFF, 0xFF, 0x0));
      mGraphics.clearRect(0, 0, getWidth(), getHeight());
      mMaterialMap.put(Material.Null, BACKSCATTER_COLOR);
      mLabel = label;
      updateText();
   }

   public TrajectoryImage(int width, int height, double scale) {
      this(width, height, scale, true);
   }

   /**
    * setXRange - Sets the range of x values that will display on the image. min
    * is left and max is right.
    * 
    * @param min
    *           double
    * @param max
    *           double
    */
   public void setXRange(double min, double max) {
      mXMin = min;
      mXScale = getWidth() / (max - min);
      clear(true);
   }

   /**
    * setYRange - Sets the range of y-values that will display on the image. min
    * is top and max is bottom.
    * 
    * @param min
    *           double
    * @param max
    *           double
    */
   public void setYRange(double min, double max) {
      mYMin = min;
      mYScale = getHeight() / (max - min);
      clear(true);
   }

   public void clear(boolean text) {
      mGraphics.setBackground(mBackground);
      mGraphics.clearRect(0, 0, getWidth(), getHeight());
      if (text)
         updateText();
   }

   /**
    * setMaxTrajectories - Sets the maximum number of trajectories to add to
    * this image.
    * 
    * @param max
    *           int
    */
   public void setMaxTrajectories(int max) {
      mMaxTrajectories = max;
   }

   /**
    * setStartTrajectory - Sets the number of the first trajectory to be
    * included in this image. The default is 0. If this parameter is s and the
    * max parameter--use setMaxTrajectories()--is m, then trajectories from s to
    * m+s-1 are included in the image.
    * 
    * @param startTrajectory
    *           int
    */
   public void setStartTrajectory(int startTrajectory) {
      this.startTrajectory = startTrajectory;
   }

   /**
    * dumpToFile - Dump this MCSS_TrajectoryImage to a file in the specified
    * directory.
    * 
    * @param dest
    *           String - The destination directory
    * @throws FileNotFoundException
    * @throws IOException
    */
   public void dumpToFile(String dest) throws FileNotFoundException, IOException {
      new File(dest).mkdirs();
      dump(new File(dest, "trajectory.png"));
   }

   public void dump(File dest) throws IOException {
      final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
      if (writers.hasNext()) {
         final ImageWriter writer = writers.next();
         final ImageOutputStream ios = ImageIO.createImageOutputStream(dest);
         writer.setOutput(ios);
         writer.write(this);
         ios.close();
      }
   }

   public void drawSphere(double[] center, double radius, Material mat) {
      final Color baseColor = assignColor(mat);
      final Color drawColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 96);
      mGraphics.setColor(drawColor);
      mGraphics.drawOval((int) Math.round(mXScale * (center[0] - mXMin)), (int) Math.round(mYScale * (center[2] - mYMin)),
            (int) Math.round(mXScale * radius), (int) Math.round(mYScale * radius));
   }

   public void drawBlock(double[] dims, double[] center, Material mat) {
      final Color baseColor = assignColor(mat);
      final Color drawColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 96);
      mGraphics.setColor(drawColor);
      final int x0 = (int) Math.round(mXScale * (center[0] - (0.5 * dims[0])));
      final int y0 = (int) Math.round(mXScale * (center[2] - (0.5 * dims[2])));
      mGraphics.drawRect(x0, y0, (int) Math.round(mXScale * dims[0]), (int) Math.round(mYScale * dims[2]));
   }

   /**
    * actionPerformed - Implements actionPerformed for the ActionListener
    * interface.
    * 
    * @param e
    *           ActionEvent
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      if (mTrajectoryCount < (startTrajectory + mMaxTrajectories)) {
         assert (e.getSource() instanceof MonteCarloSS);
         final MonteCarloSS mcss = (MonteCarloSS) e.getSource();
         switch (e.getID()) {
            case MonteCarloSS.TrajectoryStartEvent :
               break;
            case MonteCarloSS.TrajectoryEndEvent :
               ++mTrajectoryCount;
               break;
            case MonteCarloSS.ScatterEvent :
            case MonteCarloSS.NonScatterEvent : {
               if (mTrajectoryCount >= startTrajectory) {
                  final Electron el = mcss.getElectron();
                  final double[] pos = el.getPosition();
                  if (el.getPreviousRegion() != null)
                     drawLine(mGraphics, el.getPrevPosition(), pos, el.getCurrentRegion().getMaterial());
               }
               break;
            }
            case MonteCarloSS.BackscatterEvent : {
               if (mTrajectoryCount >= startTrajectory) {
                  final Electron el = mcss.getElectron();
                  drawBS(mGraphics, el.getPrevPosition(), el.getPosition());
               }
               break;
            }
         }
      }
   }
}

package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JComponent;

/**
 * <p>
 * A control that displays the trajectory of one or more electrons. Initially it
 * will make no attempt to display the shapes of the material.
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

public class TrajectoryDisplay extends JComponent {
   protected class Step {
      private double[] mP0 = new double[3];
      private double[] mP1 = new double[3];
      private final int mMaterialIndex;

      Step(double[] p0, double[] p1, int mat) {
         mP0 = p0.clone();
         mP1 = p1.clone();
         mMaterialIndex = mat;
      }

      double[] getPoint(int i) {
         return i == 0 ? mP0 : mP1;
      }

      int getMaterial() {
         return mMaterialIndex;
      }
   }

   protected class Point {
      private double[] mP0 = new double[3];
      private final int mMaterialIndex;

      Point(double[] p0, int mat) {
         mP0 = p0.clone();
         mMaterialIndex = mat;
      }
   }

   private static final long serialVersionUID = 0x1;
   private final ArrayList<Step> mSteps = new ArrayList<Step>(); // A list of
   // Step
   // objects
   private final ArrayList<Point> mPoints = new ArrayList<Point>(); // A list
   // of
   // Point objects
   private double mX0, mY0, mX1; // The extent of the canvas (after transform).
   // mY1 is determined by display rectangle
   private final double[] mTransformed = new double[2];
   static private Color[] mMaterialColors = {Color.black, Color.blue, Color.green, Color.magenta, Color.orange, Color.red};

   public TrajectoryDisplay() {
      try {
         jbInit();
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   private void TransformPoint(double[] p0) {
      mTransformed[0] = p0[0];
      mTransformed[1] = p0[2];
   }

   public void addStep(double[] p0, double[] p1, int mat) {
      final Step st = new Step(p0, p1, mat);
      mSteps.add(st);
      drawStep(st);
   }

   public void clearAllSteps() {
      mSteps.clear();
      repaint();
   }

   public void addPoint(double[] p0, int mat) {
      final Point pt = new Point(p0, mat);
      mPoints.add(pt);
      drawPoint(pt);
   }

   public void clearAllPoints() {
      mPoints.clear();
      repaint();
   }

   @Override
   protected void paintComponent(Graphics g) {
      final Graphics dup = g.create();
      dup.setColor(getBackground());
      dup.fillRect(0, 0, getWidth(), getHeight());
      double sc = Math.log(mX1 - mX0) / 2.302585509299;
      if (Math.pow(10.0, sc - Math.ceil(sc)) < 1.0)
         sc -= 1.0;
      final double div = Math.pow(10.0, Math.round(sc));
      final double pixSize = (mX1 - mX0) / getWidth();
      dup.setColor(Color.lightGray);
      {
         final NumberFormat nf = NumberFormat.getNumberInstance();
         nf.setMaximumFractionDigits(3);
         nf.setMinimumFractionDigits(2);
         dup.setFont(new Font("SansSerif", Font.PLAIN, 12));
         double zero = Math.ceil(mX0 / div) * div;
         for (int m = (int) Math.round(((mX1 - zero) + (div / 2.0)) / div); m >= 0; --m) {
            final double v = (m * div) + zero;
            final int x = (int) ((v - mX0) / pixSize);
            dup.drawLine(x, 0, x, getHeight());
            dup.drawString(nf.format(v / 1e-6) + " \u00B5m", x + 2, getHeight());
         }
         zero = Math.ceil(mY0 / div) * div;
         for (int m = (int) Math.round((((pixSize * getHeight()) - zero) + (div / 2)) / div); m >= 0; --m) {
            final double v = (m * div) + zero;
            final int y = (int) ((v - mY0) / pixSize);
            dup.drawLine(0, y, getWidth(), y);
            dup.drawString(nf.format(v / 1e-6) + " \u00B5m", 2, y);
         }
      }
      dup.setColor(getForeground());
      for (final Step st : mSteps) {
         TransformPoint(st.getPoint(0));
         final int x0 = (int) ((mTransformed[0] - mX0) / pixSize);
         final int y0 = (int) ((mTransformed[1] - mY0) / pixSize);
         TransformPoint(st.getPoint(1));
         final int x1 = (int) ((mTransformed[0] - mX0) / pixSize);
         final int y1 = (int) ((mTransformed[1] - mY0) / pixSize);
         dup.setColor(mMaterialColors[st.getMaterial() % mMaterialColors.length]);
         dup.drawLine(x0, y0, x1, y1);
      }
      dup.setColor(getForeground());
      for (final Point pt : mPoints) {
         TransformPoint(pt.mP0);
         final int x0 = (int) ((mTransformed[0] - mX0) / pixSize);
         final int y0 = (int) ((mTransformed[1] - mY0) / pixSize);
         dup.setColor(mMaterialColors[pt.mMaterialIndex % mMaterialColors.length]);
         dup.fillRect(x0, y0, 1, 1);
      }
   }

   private void drawStep(Step st) {
      final double pixSize = (mX1 - mX0) / getWidth();
      if (getGraphics() != null) {
         final Graphics dup = getGraphics().create();
         dup.setColor(getForeground());
         TransformPoint(st.getPoint(0));
         final int x0 = (int) ((mTransformed[0] - mX0) / pixSize);
         final int y0 = (int) ((mTransformed[1] - mY0) / pixSize);
         TransformPoint(st.getPoint(1));
         final int x1 = (int) ((mTransformed[0] - mX0) / pixSize);
         final int y1 = (int) ((mTransformed[1] - mY0) / pixSize);
         dup.setColor(mMaterialColors[st.getMaterial() % mMaterialColors.length]);
         dup.drawLine(x0, y0, x1, y1);
      }
   }

   private void drawPoint(Point pt) {
      final double pixSize = (mX1 - mX0) / getWidth();
      if (getGraphics() != null) {
         final Graphics dup = getGraphics().create();
         dup.setColor(getForeground());
         TransformPoint(pt.mP0);
         final int x0 = (int) ((mTransformed[0] - mX0) / pixSize);
         final int y0 = (int) ((mTransformed[1] - mY0) / pixSize);
         dup.setColor(mMaterialColors[pt.mMaterialIndex % mMaterialColors.length]);
         dup.fillRect(x0, y0, 1, 1);
      }
   }

   public void setViewPort(double x0, double y0, double x1) {
      if ((x0 != mX0) || (y0 != mY0) || (x1 != mX1)) {
         mX0 = x0;
         mY0 = y0;
         mX1 = x1;
         repaint();
      }
   }

   private void jbInit() throws Exception {
      this.setBackground(Color.white);
   }
}

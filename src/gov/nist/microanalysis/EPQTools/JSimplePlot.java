package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.Utility.HalfUpFormat;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * JSimplePlot is a very basic 2-D plotting system. It can draw a series of data
 * sets with a handful of different shaped markers which can be connected by
 * lines.
 * 
 * @author nicholas
 */
public class JSimplePlot
   extends BufferedImage {

   private int sPrevShape = 0;

   private Shape nextShape() {
      final Shape[] vals = Shape.values();
      sPrevShape = ((sPrevShape + 1) >= vals.length ? 0 : sPrevShape + 1);
      return vals[sPrevShape];
   }

   private final static Color[] DEFAULT_COLORS = {
      Color.blue,
      Color.cyan,
      Color.gray,
      Color.green,
      Color.darkGray,
      Color.magenta,
      Color.orange,
      Color.red,
      Color.yellow
   };

   private int sPrevColor = -1;

   private Color nextColor() {
      sPrevColor = ((sPrevColor + 1) >= DEFAULT_COLORS.length ? 0 : sPrevColor + 1);
      return DEFAULT_COLORS[sPrevColor];
   }

   private final Color mPlotBackColor = new Color(0xFC, 0xFC, 0xF2);
   // private final Color mMinorGridColor = new Color(0xFF, 0xF0, 0xF0);
   private final Color mMajorGridColor = new Color(0xFF, 0xE0, 0xE0);

   public enum Shape {
      None {
         @Override
         void draw(int x, int y, int size, Graphics2D gr) {
            return;
         }
      },
      Square {
         @Override
         void draw(int x, int y, int size, Graphics2D gr) {
            gr.fillRect(x - (size / 2), y - (size / 2), size, size);
         }
      },
      Triangle {
         @Override
         void draw(int x, int y, int size, Graphics2D gr) {
            final int[] xx = {
               0,
               size / 3,
               -size / 3
            };
            final int[] yy = {
               -size / 2,
               size / 3,
               size / 3
            };
            gr.fillPolygon(xx, yy, 3);
         }
      },
      InvertedTriangle {
         @Override
         void draw(int x, int y, int size, Graphics2D gr) {
            final int[] xx = {
               0,
               size / 3,
               -size / 3
            };
            final int[] yy = {
               size / 2,
               -size / 3,
               -size / 3
            };
            gr.fillPolygon(xx, yy, 3);
         }
      },
      Circle {
         @Override
         void draw(int x, int y, int size, Graphics2D gr) {
            gr.fillArc(x, y, size, size, 0, 360);
         }
      },
      Diamond {
         @Override
         void draw(int x, int y, int size, Graphics2D gr) {
            final int[] xx = {
               0,
               size / 2,
               0,
               -size / 2
            };
            final int[] yy = {
               -size / 2,
               0,
               size / 2,
               0
            };
            gr.fillPolygon(xx, yy, 4);
         }
      };

      abstract void draw(int x, int y, int size, Graphics2D gr);
   };

   public static class Marker {
      private final Shape mShape;
      private final int mSize;
      private final Color mColor;

      public Marker() {
         mShape = Shape.None;
         mSize = 1;
         mColor = Color.black;
      }

      public Marker(Shape shape, int size, Color color) {
         mShape = shape;
         mSize = size;
         mColor = color;
      }

      void draw(int x, int y, Graphics2D gr) {
         gr.setColor(mColor);
         mShape.draw(x, y, mSize, gr);
      }

   }

   private class DataSet {
      final private String mName;
      final private double[] mXValues;
      final private double[] mYValues;
      private final Marker mMarker;
      private final Color mLineColor;

      private DataSet(double[] x, double[] y, String name, Marker marker, Color lineColor) {
         mXValues = x.clone();
         mYValues = y.clone();
         mMarker = marker;
         mLineColor = lineColor;
         mName = name;
      }

      private void draw(Graphics2D gr) {
         int prevX = Integer.MIN_VALUE, prevY = Integer.MIN_VALUE;
         for(int i = 0; i < mXValues.length; ++i) {
            final double x = mXValues[i], y = mYValues[i];
            final int xi = (int) Math.round(mPlotArea.x + (((x - mBounds.x) * mPlotArea.width) / mBounds.width));
            final int yi = (int) Math.round((mPlotArea.y + mPlotArea.height)
                  - (((y - mBounds.y) * mPlotArea.height) / mBounds.height));
            if((prevX != Integer.MIN_VALUE) && (mLineColor != null)) {
               gr.setColor(mLineColor);
               gr.drawLine(prevX, prevY, xi, yi);
            }
            mMarker.draw(xi, yi, gr);
            prevX = xi;
            prevY = yi;
         }
      }
   }

   private final ArrayList<DataSet> mData = new ArrayList<DataSet>();
   private Rectangle2D.Double mBounds;
   private final NumberFormat mXFormat = new HalfUpFormat("#,##0.0");
   private final NumberFormat mYFormat = new HalfUpFormat("#,##0.0");
   private String mXLabel;
   private String mYLabel;

   private Rectangle mPlotArea;

   /**
    * Creates a RGB bitmap with the specified width and height to contain the
    * plot.
    * 
    * @param width
    * @param height
    */
   public JSimplePlot(int width, int height) {
      super(width, height, BufferedImage.TYPE_3BYTE_BGR);
   }

   /**
    * Add a named data set with the specified marker and line color.
    * 
    * @param x
    * @param y
    * @param name
    * @param marker
    * @param lineColor
    */
   public void addDataSet(double[] x, double[] y, String name, Marker marker, Color lineColor) {
      mData.add(new DataSet(x, y, name, marker, lineColor));
      draw();
   }

   /**
    * Add an unnamed data set using the specified points.
    * 
    * @param x
    * @param y
    */
   public void addDataSet(double[] x, double[] y) {
      final Color cc = nextColor();
      addDataSet(x, y, null, new Marker(nextShape(), 4, cc), cc);
   }

   /**
    * Add a data set using the specified points and name.
    * 
    * @param x
    * @param y
    * @param name
    */
   public void addDataSet(double[] x, double[] y, String name) {
      final Color cc = nextColor();
      addDataSet(x, y, name, new Marker(nextShape(), 4, cc), cc);
   }

   /**
    * Remove all DataSet objects from this plot.
    */
   public void clear() {
      mData.clear();
      draw();
   }

   /**
    * Sets the bounds of the plot area.
    * 
    * @param minX
    * @param minY
    * @param maxX
    * @param maxY
    */
   public void setBounds(double minX, double minY, double maxX, double maxY) {
      mBounds = new Rectangle2D.Double(Math.min(minX, maxX), Math.min(minY, maxY), Math.abs(maxX - minX), Math.abs(maxY - minY));
      draw();
   }

   /**
    * Sets both the x and y labels (null to clear)
    * 
    * @param xLabel
    * @param yLabel
    */
   public void setLabels(String xLabel, String yLabel) {
      mXLabel = xLabel;
      mYLabel = yLabel;
      draw();
   }

   /**
    * Auto-scales the x axis. Matches the x-axis bounds to the maximum bounds of
    * the current data sets.
    */
   public void autoXScale() {
      double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
      for(final DataSet ds : mData)
         for(final double x : ds.mXValues) {
            if(x < minX)
               minX = x;
            if(x > maxX)
               maxX = x;
         }
      if(minX == Double.MAX_VALUE) {
         minX = 0.0;
         maxX = 10.0;
      }
      if(mBounds == null)
         mBounds = new Rectangle2D.Double(0.0, 0.0, 10.0, 10.0);
      mBounds.x = minX;
      mBounds.width = Math.max(maxX - minX, 1.0);
      draw();
   }

   /**
    * Auto-scales the y axis. Matches the y-axis bounds to the maximum bounds of
    * the current data sets plus a little extra.
    * 
    * @param extra How much extra to add (0.0 to ~1.0, nominal 0.1)
    */
   public void autoYScale(double extra) {
      double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
      for(final DataSet ds : mData)
         for(final double y : ds.mYValues) {
            if(y < minY)
               minY = y;
            if(y > maxY)
               maxY = y;
         }
      if(minY == Double.MAX_VALUE) {
         minY = extra * (10.0 / (1.0 + (2.0 * extra)));
         maxY = (10.0 / (1.0 + (2.0 * extra))) + minY;
      }
      if(mBounds == null)
         mBounds = new Rectangle2D.Double(0.0, 0.0, 10.0, 10.0);
      mBounds.y = minY - (extra * Math.max(maxY - minY, 1.0));
      mBounds.height = (1.0 + (2.0 * extra)) * Math.max(maxY - minY, 1.0);
      draw();
   }

   /**
    * Auto scale both the x and y axes.
    */
   public void autoScale() {
      autoXScale();
      autoYScale(0.1);
   }

   /**
    * Returns the bounds of the plot area.
    * 
    * @return Rectangle2D.Double
    */
   public Rectangle2D.Double getBounds() {
      return (Rectangle2D.Double) mBounds.clone();
   }

   private double axisMagic(double val) {
      final double lv = Math.log10(val);
      final double rlv = Math.pow(10.0, Math.round(lv) - 1.0);
      return rlv;
   }

   /**
    * Calculates the point on the plot corresponding to the specified
    * coordinates. Only works after draw() has been called.
    * 
    * @param x
    * @param y
    * @return Point
    */
   public Point coordinate(double x, double y) {
      final int ix = (int) Math.round(mPlotArea.x + (((x - mBounds.x) * mPlotArea.width) / mBounds.width));
      final int iy = (int) Math.round((mPlotArea.y + mPlotArea.height)
            + (((y - mBounds.y) * mPlotArea.height) / mBounds.height));
      return new Point(ix, iy);
   }

   private void draw() {
      final Graphics2D gr = createGraphics();
      gr.setColor(Color.white);
      gr.fillRect(0, 0, getWidth(), getHeight());
      if(mData.size() == 0)
         return;
      if(mBounds == null)
         autoScale();
      final double xUnits = axisMagic(mBounds.width);
      final FontMetrics fm = gr.getFontMetrics();
      final double xMin = ((int) ((mBounds.x + (0.999 * xUnits)) / xUnits)) * xUnits;
      int wx = 0, hx = 0;
      final ArrayList<Double> xValues = new ArrayList<Double>();
      final ArrayList<String> xLabels = new ArrayList<String>();
      for(double x = xMin; x < (mBounds.x + mBounds.width + xUnits); x += xUnits) {
         xValues.add(x);
         xLabels.add(mXFormat.format(x));
      }
      do {
         for(final String str : xLabels) {
            final Rectangle r = fm.getStringBounds(str, gr).getBounds();
            wx += r.width;
            hx = Math.max(r.height, hx);
         }
         if(wx > (getWidth() / 2))
            for(int i = xLabels.size() - 2; i >= 0; i -= 2) {
               xLabels.remove(i);
               xValues.remove(i);
            }
      } while((wx > (getWidth() / 2)) && (xLabels.size() > 2));

      final double yUnits = axisMagic(mBounds.height);
      final double yMin = ((int) ((mBounds.x + (0.999 * yUnits)) / yUnits)) * yUnits;
      int wy = 0, hy = 0;
      final ArrayList<Double> yValues = new ArrayList<Double>();
      final ArrayList<String> yLabels = new ArrayList<String>();
      for(double y = yMin; y < (mBounds.y + mBounds.height); y += yUnits) {
         yValues.add(y);
         yLabels.add(mYFormat.format(y));
      }
      do {
         for(final String str : yLabels) {
            final Rectangle r = fm.getStringBounds(str, gr).getBounds();
            wy = Math.max(r.width, wy);
            hy += r.height;
         }
         if(hy > (getHeight() / 2))
            for(int i = yLabels.size() - 2; i >= 0; i -= 2) {
               yValues.remove(i);
               yLabels.remove(i);
            }
      } while((hy > (getHeight() / 2)) && (yLabels.size() > 2));

      final int ly = (mYLabel != null ? (3 * fm.getHeight()) / 2 : 0);

      mPlotArea = new Rectangle(((12 * wy) / 10) + ly, hx / 10, //
      getWidth() - ((13 * wy) / 10) - ly, //
      getHeight() - ((13 * hx) / 10) - (mXLabel == null ? 0 : fm.getHeight()));
      gr.setColor(mPlotBackColor);
      gr.fillRect(mPlotArea.x, mPlotArea.y, mPlotArea.width, mPlotArea.height);

      for(int i = 0; i < xLabels.size(); ++i) {
         final String str = xLabels.get(i);
         final Rectangle r = fm.getStringBounds(str, gr).getBounds();
         final double val = xValues.get(i).doubleValue();
         final int py = mPlotArea.y + mPlotArea.height + r.height; // getHeight()
                                                                   // -
         // r.height / 10;
         final int px = (int) Math.round(((val - mBounds.x) * mPlotArea.width) / mBounds.width) + mPlotArea.x;
         gr.setColor(Color.black);
         gr.drawString(str, px - (r.width / 2), py);
         gr.setColor(mMajorGridColor);
         gr.drawLine(px, mPlotArea.y, px, mPlotArea.y + mPlotArea.height);
      }
      if(mXLabel != null) {
         gr.setColor(Color.black);
         gr.drawString(mXLabel, mPlotArea.x + ((mPlotArea.width - fm.stringWidth(mXLabel)) / 2), getHeight() - fm.getDescent());
      }
      for(int i = 0; i < yLabels.size(); ++i) {
         final String str = yLabels.get(i);
         final Rectangle r = fm.getStringBounds(str, gr).getBounds();
         final double val = yValues.get(i).doubleValue();
         final int px = mPlotArea.x - (r.width + (wy / 10));
         final int py = (mPlotArea.y + mPlotArea.height)
               - (int) Math.round(((val - mBounds.y) * mPlotArea.height) / mBounds.height);
         gr.setColor(Color.black);
         gr.drawString(str, px, py + (r.height / 3));
         gr.setColor(mMajorGridColor);
         gr.drawLine(mPlotArea.x, py, mPlotArea.x + mPlotArea.width, py);
      }
      gr.setColor(Color.black);
      if(mYLabel != null) {
         final AffineTransform before = gr.getTransform();
         final int xx = fm.getHeight();
         final int yy = mPlotArea.y + ((mPlotArea.height + fm.stringWidth(mYLabel)) / 2);
         gr.rotate(-0.5 * Math.PI, xx, yy);
         gr.drawString(mYLabel, xx, yy);
         gr.setTransform(before);
      }
      gr.drawRect(mPlotArea.x, mPlotArea.y, mPlotArea.width, mPlotArea.height);
      setClip(gr, true);
      { // Draw the legend
         int nw = 0;
         for(final DataSet ds : mData)
            if(ds.mName != null)
               nw = Math.max(fm.stringWidth(ds.mName), nw);
         int ny = mPlotArea.y;
         nw = (mPlotArea.x + mPlotArea.width) - (nw + 4);
         for(final DataSet ds : mData)
            if(ds.mName != null) {
               ny += fm.getHeight();
               gr.setColor(ds.mMarker.mColor);
               gr.drawString(ds.mName, nw, ny);
            }
      }
      // Draw the data
      for(final DataSet ds : mData)
         ds.draw(gr);
   }

   /**
    * If limit then set the clip area to the plot window else set it to the full
    * area.
    * 
    * @param gr Graphics2D
    * @param limit boolean
    */
   public void setClip(Graphics2D gr, boolean limit) {
      if(limit)
         gr.setClip(mPlotArea.x, mPlotArea.y, mPlotArea.width, mPlotArea.height);
      else
         gr.setClip(0, 0, getWidth(), getHeight());
   }
}
package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComponent;

/**
 * @author nicholas
 */
public class JStripSpectrumDisplay
   extends JComponent {

   private static final long serialVersionUID = -5820085805013539098L;

   final private ArrayList<ISpectrumData> mSpectra;
   private int mMaxSpectra = 1000;
   private int mStripHeight = 10;
   private int mBorder = 1;
   private double mMinEnergy = 0.0;
   private double mMaxEnergy = 10.0e3;
   transient private ArrayList<BufferedImage> mStrips;

   /**
    * @return the maxSpectra
    */
   public int getMaxSpectra() {
      return mMaxSpectra;
   }

   /**
    * @param maxSpectra the maxSpectra to set
    */
   public void setMaxSpectra(int maxSpectra) {
      if(mMaxSpectra != maxSpectra) {
         mMaxSpectra = maxSpectra;
         while(mSpectra.size() > mMaxSpectra) {
            mSpectra.remove(0);
            mStrips.remove(0);
         }
         updateBounds();
      }
   }

   /**
    * @return the stripHeight
    */
   public int getStripHeight() {
      return mStripHeight;
   }

   /**
    * @param stripHeight the stripHeight to set
    */
   public void setStripHeight(int stripHeight) {
      if(mStripHeight != stripHeight) {
         mStripHeight = stripHeight;
         updateBounds();
      }
   }

   /**
    * @return the border
    */
   public int getBorderThickness() {
      return mBorder;
   }

   /**
    * @param border the border to set
    */
   public void setBorderThickness(int border) {
      if(border < 0)
         border = 0;
      if(mBorder != border) {
         mBorder = border;
         updateBounds();
      }
   }

   /**
    * @return the minEnergy
    */
   public double getMinEnergy() {
      return mMinEnergy;
   }

   /**
    * @param minEnergy the minEnergy to set
    */
   public void setMinEnergy(double minEnergy) {
      if(mMinEnergy != minEnergy) {
         mMinEnergy = minEnergy;
         updateStrips();
      }
   }

   /**
    * @return the maxEnergy
    */
   public double getMaxEnergy() {
      return mMaxEnergy;
   }

   /**
    * @param maxEnergy the maxEnergy to set
    */
   public void setMaxEnergy(double maxEnergy) {
      if(mMaxEnergy != maxEnergy) {
         mMaxEnergy = maxEnergy;
         updateStrips();
      }
   }

   public JStripSpectrumDisplay() {
      mSpectra = new ArrayList<ISpectrumData>();
      mStrips = new ArrayList<BufferedImage>();
      setBackground(Color.white);
      // To cause the tool tip to display();
      setToolTipText("temp");
   }

   @Override
   protected void paintComponent(Graphics gr) {
      gr.setColor(getBackground());
      gr.fillRect(0, 0, getWidth(), getHeight());
      int h = mBorder;
      final Rectangle clip = gr.getClipBounds();
      for(int i = mSpectra.size() - 1; i >= 0; --i) {
         if((((h + mStripHeight) >= clip.y) && (h <= (clip.y + clip.height))))
            gr.drawImage(mStrips.get(i), 0, h, getWidth(), mStripHeight, null);
         h += mStripHeight + mBorder;
      }
   }

   @Override
   public String getToolTipText(MouseEvent me) {
      final int i = (me.getY() / (mStripHeight + mBorder));
      if(i < mSpectra.size()) {
         final ISpectrumData spec = mSpectra.get(i);
         return spec.toString();
      } else
         return "?";
   }

   /**
    * @see java.awt.Component#setBounds(java.awt.Rectangle)
    */
   @Override
   public void setBounds(Rectangle rect) {
      this.setBounds(rect.x, rect.y, rect.width, rect.height);
   }

   /**
    * Set the display bounds
    * 
    * @param x
    * @param y
    * @param width
    * @param height
    * @see java.awt.Component#setBounds(int, int, int, int)
    */
   @Override
   public void setBounds(int x, int y, int width, int height) {
      if(getBounds().width != width)
         updateStrips();
      super.setBounds(x, y, width, height);
      setPreferredSize(new Dimension(width, height));
   }

   public void setRange(double eMin, double eMax) {
      if((eMin != mMinEnergy) || (eMax != mMaxEnergy)) {
         mMinEnergy = eMin;
         mMaxEnergy = eMax;
         updateStrips();
         repaint();
      }
   }

   private void updateStrips() {
      mStrips.clear();
      for(final ISpectrumData spec : mSpectra) {
         final BufferedImage bi = SpectrumUtils.toStrip(spec, mMinEnergy, mMaxEnergy, getWidth(), 1);
         mStrips.add(bi);
      }
   }

   private void updateBounds() {
      final Rectangle r = getBounds();
      final int height = mSpectra.size() * (mStripHeight + mBorder);
      super.setBounds(r.x, r.y, r.width, height);
      setPreferredSize(new Dimension(r.width, height));
   }

   public void addSpectrum(ISpectrumData spec) {
      if(mSpectra.contains(spec))
         mSpectra.remove(spec);
      while(mSpectra.size() >= mMaxSpectra) {
         mSpectra.remove(0);
         mStrips.remove(0);
      }
      final BufferedImage bi = SpectrumUtils.toStrip(spec, 0.0, 10.0e3, getWidth(), 1);
      mStrips.add(bi);
      mSpectra.add(spec);
      updateBounds();
      repaint();
   }

   public void addSpectra(Collection<ISpectrumData> specs) {
      for(final ISpectrumData spec : specs)
         addSpectrum(spec);
   }

   public void removeSpectrum(ISpectrumData spec) {
      final int pos = mSpectra.indexOf(spec);
      if(pos != -1) {
         mStrips.remove(pos);
         mSpectra.remove(pos);
         updateBounds();
      }
   }

   public void clearSpectra() {
      mSpectra.clear();
      updateStrips();
      updateBounds();
   }
}

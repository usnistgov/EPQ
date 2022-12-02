package gov.nist.microanalysis.Utility;

/*
 * Copied from this tutorial:
 * http://www.apl.jhu.edu/~hall/java/Swing-Tutorial/Swing-Tutorial-Printing.html
 * And also from a post on the forums at java.swing.com. My apologies that I do
 * not have a link to that post, by my hat goes off to the poster because he/she
 * figured out the sticky problem of paging properly when printing a Swing
 * component.
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.RepaintManager;

public class PrintUtilities implements Printable {

   private static final int DEFAULT_BORDER = 36;
   private final Component mComponentToBePrinted;
   // Border in units of 1/72"
   private int mLeft = DEFAULT_BORDER;
   private int mTop = DEFAULT_BORDER;
   private int mRight = DEFAULT_BORDER;
   private int mBottom = DEFAULT_BORDER;

   public static void printComponent(Component c) {
      new PrintUtilities(c).print();
   }

   public PrintUtilities(Component componentToBePrinted) {
      this.mComponentToBePrinted = componentToBePrinted;
   }

   public void print() {
      final PrinterJob printJob = PrinterJob.getPrinterJob();
      printJob.setPrintable(this);
      if (printJob.printDialog())
         try {
            printJob.print();
         } catch (final PrinterException pe) {
            System.out.println("Error printing: " + pe);
         }
   }

   public void setBorders(int left, int top, int right, int bottom) {
      mLeft = Math.max(0, left);
      mTop = Math.max(0, top);
      mRight = Math.max(0, right);
      mBottom = Math.max(0, bottom);
   }

   @Override
   public int print(Graphics g, PageFormat pf, int pageIndex) {
      int response = NO_SUCH_PAGE;
      final Graphics2D g2 = (Graphics2D) g;
      // for faster printing, turn off double buffering
      disableDoubleBuffering(mComponentToBePrinted);
      final Dimension d = mComponentToBePrinted.getSize(); // get size of
      // document
      final double componentWidth = d.width; // width in pixels
      final double componentHeight = d.height; // height in pixels
      final Paper p = pf.getPaper();
      p.setImageableArea(mLeft, mTop, pf.getWidth() - (mLeft + mRight), pf.getHeight() - (mBottom + mTop));
      pf.setPaper(p);
      final double pageHeight = pf.getImageableHeight();
      final double pageWidth = pf.getImageableWidth();
      if ((pageHeight > 72.0) && (pageWidth > 72.0)) {
         final double hScale = pageWidth / componentWidth;
         final int totalNumPages = (int) Math.ceil((hScale * componentHeight) / pageHeight);
         // make sure not print empty pages
         if (pageIndex < totalNumPages) {
            // shift Graphic to line up with beginning of print-imageable region
            g2.translate(mLeft, mTop - (pageIndex * pageHeight));
            // scale the page so the width fits...
            g2.scale(hScale, hScale);
            // Clip to visible portion
            g2.setClip(0, (int) ((pageIndex * pageHeight) / hScale), (int) (pageWidth / hScale), (int) (pageHeight / hScale));
            mComponentToBePrinted.paint(g2); // repaint the page for printing
            response = Printable.PAGE_EXISTS;
         }
      }
      enableDoubleBuffering(mComponentToBePrinted);
      return response;
   }

   public static void disableDoubleBuffering(Component c) {
      final RepaintManager currentManager = RepaintManager.currentManager(c);
      currentManager.setDoubleBufferingEnabled(false);
   }

   public static void enableDoubleBuffering(Component c) {
      final RepaintManager currentManager = RepaintManager.currentManager(c);
      currentManager.setDoubleBufferingEnabled(true);
   }
}
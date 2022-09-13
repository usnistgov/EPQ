/**
 * gov.nist.nanoscalemetrology.JMONSEL.CoarseFineRasterScanGenerator Created by:
 * John Villarrubia Date: Sep 14, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * A ScanGenerator for a combined coarse/fine raster. This kind of scan provides
 * a good compromise between speed and accuracy in some charging simulations.
 * The problem with an ordinary raster-scan is that the pixels are so close to
 * each other that pixel i+1 can't be properly simulated until an FEA has been
 * performed to account for charges deposited during pixel i. However, doing an
 * FEA after each pixel is very time-consuming.
 * </p>
 * <p>
 * This scan generator divides the scanned area into ncx * ncy coarse (i.e.,
 * large) pixels. Each coarse pixel is subdivided into nx * ny fine pixels.
 * Raster scans of coarse pixels (rastered over the image) and fine pixels
 * (rastered over each coarse pixel) are then interleaved in such a way that the
 * first fine pixel in each coarse pixel is done, then the second fine pixel in
 * each coarse one, etc. In this way, successive pixels are relatively far apart
 * (a coarse instead of fine pixel size). It is a better approximation to ignore
 * the new charge distribution from these more distant neighbors than to ignore
 * it from the nearer neighbors of the usual raster-scan. If sizes are chosen
 * appropriately, more electrons can be run between FEA.
 * </p>
 * <p>
 * X and Y scans are along the coordinate axes. The
 * CoarseFineRasterScanGenerator is characterized by parameters: <br>
 * x0 - initial x position <br>
 * y0 - initial y position <br>
 * z - z coordinate <br>
 * deltaX - pixel size in x direction <br>
 * deltaY - pixel size in y direction <br>
 * ncX - number of coarse pixels per line <br>
 * ncY - number of coarse lines per frame <br>
 * nfX - number of fine pixels per line in a coarse pixel <br>
 * nfY - number of fine lines in a coarse pixel <br>
 * t0 - time at the initial position <br>
 * pixelDwell - time between coarse pixels along the x direction <br>
 * retraceTime - time between completion of the last coarse pixel of a line and
 * start of the first coarse pixel of the next line <br>
 * frameSettleTime - time between completion of the last line of a coarse pass
 * through a frame and the start of the first line of the next coarse pass <br>
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */

public class CoarseFineRasterScanGenerator
   extends ScanGenerator {

   private final double x0;
   private final double y0;
   private final double z;
   private final double deltaX;
   private final double deltaY;
   private final int ncX;
   private final int nfX;
   private final double t0;
   private final double pixelDwell;

   private final int pixelsPerFrame; // fine pixels in a frame
   private final int coarsePixelsPerPass; // coarse pixels in a frame
   private final double totalLineTime; // time for ncX coarse pixels + retrace
   private final double passTime; // time for each pass through coarse pixels
   private final double totalFrameTime; //
   private final double coarseXSize;
   private final double coarseYSize;

   /**
    * Constructs a CoarseFineRasterScanGenerator with the supplied parameters.
    *
    * @param x0
    * @param y0
    * @param z
    * @param deltaX
    * @param deltaY
    * @param ncX
    * @param ncY
    * @param nfX
    * @param nfY
    * @param t0
    * @param pixelDwell
    * @param retraceTime
    * @param passSettleTime
    */
   public CoarseFineRasterScanGenerator(double x0, double y0, double z, double deltaX, double deltaY, int ncX, int ncY, int nfX, int nfY, double t0, double pixelDwell, double retraceTime, double passSettleTime) {
      super();

      this.x0 = x0;
      this.y0 = y0;
      this.z = z;
      this.deltaX = deltaX;
      this.deltaY = deltaY;
      this.ncX = ncX;
      this.nfX = nfX;
      this.t0 = t0;
      this.pixelDwell = pixelDwell;
      coarsePixelsPerPass = ncX * ncY;
      final int pixelsPerCoarsePixel = nfX * nfY;
      pixelsPerFrame = coarsePixelsPerPass * pixelsPerCoarsePixel;
      totalLineTime = (ncX * pixelDwell) + retraceTime;
      passTime = passSettleTime + (ncY * totalLineTime);
      totalFrameTime = passTime * pixelsPerCoarsePixel;
      coarseXSize = nfX * deltaX;
      coarseYSize = nfY * deltaY;
   }

   /**
    * @param i
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScanGenerator#get(int)
    */
   @Override
   public double[] get(int i) {
      if(i < 0)
         return new double[] {
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN
         };
      final int frameNum = i / pixelsPerFrame;
      final int iframe = i % pixelsPerFrame;
      final int passNum = iframe / coarsePixelsPerPass;
      final int ipass = iframe % coarsePixelsPerPass;
      final int ic = ipass % ncX;
      final int jc = ipass / ncX;
      final int ifine = passNum % nfX;
      final int jfine = passNum / nfX;
      final double[] temp = new double[] {
         x0 + (ic * coarseXSize) + (ifine * deltaX),
         y0 + (jc * coarseYSize) + (jfine * deltaY),
         z,
         t0 + (ic * pixelDwell) + (jc * totalLineTime) + (passNum * passTime) + (frameNum * totalFrameTime)
      };
      return temp;
   }

}

/**
 * gov.nist.nanoscalemetrology.JMONSEL.SimpleRasterScanGenerator Created by:
 * John Villarrubia Date: Sep 14, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * A raster-scan generator with x and y scans along the coordinate axes. The
 * SimpleRasterScanGenerator's is characterized by parameters: <br>
 * x0 - initial x position <br>
 * y0 - initial y position <br>
 * z - z coordinate <br>
 * deltaX - pixel size in x direction <br>
 * deltaY - pixel size in y direction <br>
 * nx - number of pixels per line <br>
 * ny - number of lines per frame <br>
 * t0 - time at the initial position <br>
 * pixelDwell - time between pixels along the x direction <br>
 * retraceTime - time between completion of the last pixel of a line and start
 * of the first pixel of the next line <br>
 * frameSettleTime - time between completion of the last line of a frame and the
 * start of the first line of the next frame <br>
 * </p>
 * <p>
 * Let ix = (i%nx) be the current pixel number, and <br>
 * iy = i/nx % ny be the current line number, and<br>
 * if = i/(nx*ny) be the current frame number. Then<br>
 * The ith x position is x = x0+ix*deltaX.<br>
 * The ith y position is y = y0+iy*deltaY.<br>
 * The ith z position is z.<br>
 * The time of the ith point is t = t0 + ix*pixelDwell +
 * iy*(nx*pixelDwell+retraceTime) + if*(frameSettleTime + ny*(retraceTime +
 * nx*dwellTime))
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
public class SimpleRasterScanGenerator
   extends ScanGenerator {

   private final double x0;
   private final double y0;
   private final double deltaX;
   private final double deltaY;
   private final double z;
   private final double t0;
   private final double pixelDwell;

   private final int nx;
   private final int ny;

   private final double totalLineTime;
   private final double totalFrameTime;

   /**
    * Constructs a SimpleRasterScanGenerator with the supplied parameters.
    *
    * @param x0
    * @param y0
    * @param z
    * @param deltaX
    * @param deltaY
    * @param nx
    * @param ny
    * @param t0
    * @param pixelDwell
    * @param retraceTime
    * @param frameSettleTime
    */
   public SimpleRasterScanGenerator(double x0, double y0, double z, double deltaX, double deltaY, int nx, int ny, double t0, double pixelDwell, double retraceTime, double frameSettleTime) {
      super();
      this.x0 = x0;
      this.y0 = y0;
      this.deltaX = deltaX;
      this.deltaY = deltaY;
      this.z = z;
      this.t0 = t0;
      this.pixelDwell = pixelDwell;
      this.nx = nx;
      this.ny = ny;
      totalLineTime = (nx * pixelDwell) + retraceTime;
      totalFrameTime = frameSettleTime + (ny * totalLineTime);
   }

   /**
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
      final int ix = i % nx;
      final int iy = (i / nx) % ny;
      final int iframe = i / (nx * ny);
      return new double[] {
         x0 + (ix * deltaX),
         y0 + (iy * deltaY),
         z,
         t0 + (ix * pixelDwell) + (iy * totalLineTime) + (iframe * totalFrameTime)
      };
   }

}

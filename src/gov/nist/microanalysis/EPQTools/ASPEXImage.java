/**
 * 
 */
package gov.nist.microanalysis.EPQTools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;

/**
 * @author nritchie
 *
 */
public class ASPEXImage {

   public static BufferedImage[] read(File fn)
         throws IOException, EPQException {
      List<TIFFImageFileDir> images = TIFFImageFileDir.readIFD(fn);
      final TIFFImageFileDir dir = images.get(0);
      final TIFFImageFileDir.Field field = dir
            .getField(ASPEXSpectrum.IMAGE_DESCRIPTION);
      double macroFov = Double.NaN, microFov = Double.NaN; // meters
      StageCoordinate stgPos = new StageCoordinate();
      if (field != null) {
         double mag = Double.NaN, zoom = Double.NaN;
         String[] items = field.getAsString().split("\n");
         for (String item : items) {
            String[] kv = item.split("=", 1);
            if (kv[0].equals("mag"))
               mag = Double.parseDouble(kv[1]);
            else if (kv[0].equals("zoom"))
               zoom = Double.parseDouble(kv[1]);
            else if (kv[0].startsWith("stage_")) {
               try {
                  final double val = Double.parseDouble(kv[1]);
                  if (kv[0].equals("stage_x"))
                     stgPos.set(Axis.X, val);
                  else if (kv[0].equals("stage_y"))
                     stgPos.set(Axis.Y, val);
                  else if (kv[0].equals("stage_z"))
                     stgPos.set(Axis.Z, val);
                  else if (kv[0].equals("stage_r"))
                     stgPos.set(Axis.R, val);
                  else if (kv[0].equals("stage_t"))
                     stgPos.set(Axis.T, val);
                  else if (kv[0].equals("stage_b"))
                     stgPos.set(Axis.B, val);
               } catch (NumberFormatException e) {
                  e.printStackTrace();
               }
            }
         }
         macroFov = (1.0e-6 * (3.5 * 25.4 * 1000.0)) / mag;
         microFov = macroFov / zoom; // meters
      }
      final Iterator<ImageReader> irs = ImageIO
            .getImageReadersByFormatName("tiff");
      final ImageReader ir = irs.next();
      try (final FileImageInputStream fiis = new FileImageInputStream(fn)) {
         ir.setInput(fiis);
         final int nImgs = Math.min(2, ir.getNumImages(true));
         BufferedImage[] res = new BufferedImage[nImgs];
         if (nImgs > 0) {
            res[0] = ir.read(0);
            if (!Double.isNaN(microFov))
               res[0] = new ScaledImage(res[0], microFov,
                     (microFov * res[0].getHeight()) / res[0].getWidth(),
                     stgPos, "SE");
         }
         if (nImgs > 1) {
            res[1] = ir.read(1);
            if (!Double.isNaN(macroFov))
               res[1] = new ScaledImage(res[1], macroFov,
                     (macroFov * res[1].getHeight()) / res[1].getWidth(),
                     stgPos, "BSE");
         }
         return res;
      }
   }
}

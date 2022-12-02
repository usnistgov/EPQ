package gov.nist.microanalysis.Utility;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * <p>
 * A mechanism for copying HTML to the clipboard in a manner that will be
 * recognized by other programs as HTML.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class HtmlSelection implements Transferable {

   private static ArrayList<DataFlavor> mHTMLFlavors = new ArrayList<DataFlavor>();

   static {
      try {
         mHTMLFlavors.add(new DataFlavor("text/html;class=java.lang.String"));
         mHTMLFlavors.add(new DataFlavor("text/html;class=java.io.Reader"));
         mHTMLFlavors.add(new DataFlavor("text/html;charset=unicode;class=java.io.InputStream"));
      } catch (final ClassNotFoundException ex) {
         ex.printStackTrace();
      }

   }

   private final String mHTML;

   public HtmlSelection(final String html) {
      mHTML = html;
   }

   @Override
   public DataFlavor[] getTransferDataFlavors() {
      return mHTMLFlavors.toArray(new DataFlavor[mHTMLFlavors.size()]);
   }

   @Override
   public boolean isDataFlavorSupported(final DataFlavor flavor) {
      return mHTMLFlavors.contains(flavor);

   }

   @Override
   public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {

      if (String.class.equals(flavor.getRepresentationClass()))
         return mHTML;
      else if (Reader.class.equals(flavor.getRepresentationClass()))
         return new StringReader(mHTML);
      else if (InputStream.class.equals(flavor.getRepresentationClass())) {
         final ByteBuffer bb = Charset.defaultCharset().encode(mHTML);
         return new ByteArrayInputStream(bb.array());
      }
      throw new UnsupportedFlavorException(flavor);
   }

}

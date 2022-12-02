/**
 * gov.nist.microanalysis.Trixy.HTMLList Created by: nritchie Date: Jun 7, 2007
 */
package gov.nist.microanalysis.Utility;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.swing.text.html.HTML;

/**
 * <p>
 * A utility for creating lists of items in HTML.
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
public class HTMLList {

   private final ArrayList<String> mItems = new ArrayList<String>();
   private String mHeader;
   private String mErrorMsg;

   public HTMLList() {
      super();
   }

   public void setHeader(String header) {
      mHeader = header;
   }

   public void setError(String err) {
      mErrorMsg = err;
   }

   public int size() {
      return mItems.size();
   }

   public void add(String item) {
      mItems.add(item);
   }

   public void addError(String item) {
      mItems.add("<font color = \"red\">ERROR: " + item + "</font>");
   }

   public void addWarning(String item) {
      mItems.add("<font color = \"yellow\">ERROR: " + item + "</font>");
   }

   @Override
   public String toString() {
      if (mItems.size() > 0) {
         final StringWriter sw = new StringWriter();
         final PrintWriter pw = new PrintWriter(sw);
         if (mHeader != null) {
            pw.print("<h2>");
            pw.print(mHeader);
            pw.print("</h2>");
         }
         pw.println("<ul>");
         for (final String str : mItems) {
            pw.print(" <li>");
            pw.print(str);
            pw.println("</li>");
         }
         pw.println("</ul>");
         if (mErrorMsg != null) {
            pw.print("<p><font color=\"red\">");
            pw.print(mErrorMsg);
            pw.print("</font></p>");
         }
         return sw.toString();
      } else
         return "";
   }

   public HTML.Tag getTag() {
      if (mItems.size() > 0) {
         if (mHeader != null)
            return HTML.Tag.H2;
         else
            return HTML.Tag.UL;
      } else
         return HTML.Tag.COMMENT;
   }

}

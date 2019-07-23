/**
 * gov.nist.microanalysis.EPQDatabase.MineralStandardDatabase Created by:
 * nritchie Date: Jan 5, 2015
 */
package gov.nist.microanalysis.EPQDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;

/**
 * <p>
 * Description
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
public class MineralStandardDatabase {

   private String[] mHeader1;
   private String[] mHeader2;
   private List<String[]> mItems;

   private String trim(String str) {
      String tmp = str.trim();
      if(tmp.charAt(0) == '\"')
         tmp = tmp.substring(1, tmp.length());
      if(tmp.charAt(tmp.length() - 1) == '\"')
         tmp = tmp.substring(0, tmp.length() - 1);
      return tmp.trim();

   }

   private String[] split(String str) {
      ArrayList<String> res = new ArrayList<String>();
      int prev = 0;
      for(int i = 0; i < str.length(); ++i) {
         char thisChar = str.charAt(i);
         if(thisChar == '\"') {
            prev = i + 1;
            for(i = i + 1; i < str.length(); ++i) {
               thisChar = str.charAt(i);
               if(thisChar == '\\')
                  continue;
               if(thisChar == '\"')
                  break;
            }
         } else if(thisChar == ',') {
            res.add(i > prev ? trim(str.substring(prev, i)) : "");
            prev = i + 1;
         }
      }
      if(prev < str.length())
         res.add(trim(str.substring(prev)));
      return res.toArray(new String[res.size()]);

   }

   private void readDatabase()
         throws IOException {
      try (InputStream is = MineralStandardDatabase.class.getResourceAsStream("MineralStandards.csv")) {
         final InputStreamReader isr = new InputStreamReader(is, Charset.forName("Windows-1252"));
         final BufferedReader br = new BufferedReader(isr, 2048);
         mHeader1 = split(br.readLine());
         mHeader2 = split(br.readLine());
         mItems = new ArrayList<String[]>();
         while(br.ready()) {
            final String[] tmp = split(br.readLine());
            if(tmp.length > 1)
               mItems.add(tmp);
         }
      }
   }

   /**
    * Constructs a MineralStandardDatabase
    * 
    * @throws IOException
    */
   public MineralStandardDatabase()
         throws IOException {
      readDatabase();
   }

   public int size() {
      return mItems.size();
   }

   public int getColumnCount() {
      return mHeader1.length;
   }

   public String getHeaderItem(int col) {
      return mHeader1[col];
   }

   public String getHeaderItem2(int col) {
      return mHeader2[col];
   }

   public String get(int row, int col) {
      final String[] data = mItems.get(row);
      return (col >= 0) && (col < data.length) ? data[col] : "";
   }

   public Composition getComposition(int row) {
      Composition res = new Composition();
      final String[] data = mItems.get(row);
      for(int hdr = 8; (hdr < mHeader1.length) && (hdr < data.length); ++hdr) {
         final String item = data[hdr];
         if(item.length() > 0) {
            try {
               final double val = Double.parseDouble(item);
               final Element elm = Element.byName(mHeader1[hdr]);
               if(!elm.equals(Element.None))
                  res.addElement(elm, val);
               else
                  System.out.print("Unknown element: " + mHeader1[hdr]);
            }
            catch(NumberFormatException e) {
               System.out.print("Unable to parse C(" + mHeader1[hdr] + ") = " + item);
               e.printStackTrace();
            }
         }
      }
      return res;

   }

}

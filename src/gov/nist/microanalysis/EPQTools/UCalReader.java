package gov.nist.microanalysis.EPQTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class UCalReader {

   static final short FLAG = 0x0;
   static final int MAX = 0x200000;
   static final int ALL = Integer.MAX_VALUE;

   private UCalReader(File file, boolean headerOnly, int event) throws IOException, FileNotFoundException {
      try (final FileInputStream fis = new FileInputStream(file)) {
         final BufferedReader bis = new BufferedReader(new InputStreamReader(fis, "US-ASCII"));
         String str = "";
         do {
            str = bis.readLine();
            if (str == null)
               throw new IOException();
            System.out.println(str);
         } while (!str.equals("#End of Header"));
         if (!headerOnly) {
            try (final LEDataInputStream isr = new LEDataInputStream(fis)) {
               long i = 0, min = Integer.MAX_VALUE, cx = 0;
               FileWriter fw = null;
               try {
                  while (fis.available() > 0) {
                     final int x = isr.readShort();
                     if (x == FLAG) {
                        final int xx = isr.readInt();
                        if (min == Integer.MAX_VALUE)
                           min = xx;
                        if (fw != null) {
                           fw.close();
                           fw = null;
                        }
                        ++cx;
                        if (cx > event)
                           break;
                        if ((cx % 100) == 0)
                           System.out.print('.');
                        if ((cx % 8000) == 0)
                           System.out.println();
                        i = 0;
                     } else {
                        if ((fw == null) && ((event == ALL) || (cx == event))) {
                           final String fn = event == ALL ? "events.csv" : ("event_" + Integer.toString(event) + ".csv");
                           final File ef = new File(file.getParentFile(), fn);
                           fw = new FileWriter(ef);
                        }
                        if (fw != null) {
                           fw.append(Long.toString(i));
                           fw.append(", ");
                           fw.append(Integer.toString(x));
                           fw.append("\n");
                        }
                        ++i;
                     }
                  }
               } finally {
                  if (fw != null)
                     fw.close();
               }
            }
         }
      }
   }

   public static void main(String[] args) {
      try {
         if (args.length > 0) {
            boolean headerOnly = false;
            int event = ALL;
            for (final String arg : args) {
               if (arg.equals("-h"))
                  headerOnly = true;
               if (arg.startsWith("-n:"))
                  event = Integer.parseInt(arg.substring(3).trim());
            }
            new UCalReader(new File(args[args.length - 1]), headerOnly, event);
         } else
            System.out.println("UCalReader [-h] [-n:###] datafile");
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }
}

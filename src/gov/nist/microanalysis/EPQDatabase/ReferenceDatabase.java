package gov.nist.microanalysis.EPQDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;

public class ReferenceDatabase {

   private static ReferenceDatabase mInstance;
   private final Map<Element, List<Composition>> mData;

   /**
    * Returns the singleton instance... The first call to this method should
    * probably hand the Session. Subsequent calls don't need to...
    * 
    * @param session May be null
    * @return ReferenceDatabase
    */
   public static ReferenceDatabase getInstance(Session session) {
      if(mInstance == null)
         synchronized(ReferenceDatabase.class) {
            mInstance = new ReferenceDatabase(session);
         }
      return mInstance;
   }

   /**
    * Returns a list of suggested references Composition suitable for the
    * Element elm. Returns an empty list for no suggestions.
    * 
    * @return Map&lt;Element, List&lt;Composition&gt;&gt;
    */
   public Map<Element, List<Composition>> getDatabase() {
      return mData;
   }

   /**
    * Returns a list of suggested references Composition suitable for the
    * Element elm. Returns an empty list for no suggestions.
    * 
    * @param elm
    * @return List&lt;Composition&gt;
    */
   public List<Composition> getSuggestions(Element elm) {
      final List<Composition> res = mData.get(elm);
      return res != null ? res : Collections.<Composition> emptyList();
   }

   private ReferenceDatabase(Session session) {
      final InputStream is = ReferenceDatabase.class.getResourceAsStream("references.csv");
      final Map<Element, List<Composition>> res = new TreeMap<Element, List<Composition>>();
      try (final InputStreamReader isr = new InputStreamReader(is, "UTF-8")) {
         try (final BufferedReader br = new BufferedReader(isr)) {
            boolean done = false;
            while(true)
               try {
                  final String line = br.readLine();
                  done = (line == null);
                  if(done)
                     break;
                  final String[] items = line.split(",");
                  final Element elm = Element.byName(items[0]);
                  final ArrayList<Composition> comps = new ArrayList<Composition>();
                  for(int i = 1; i < items.length; ++i)
                     try {
                        Composition comp = session != null ? session.findStandard(items[i].trim()) : null;
                        if(comp == null)
                           comp = MaterialFactory.createCompound(items[i]);
                        if(comp != null)
                           comps.add(comp);
                     }
                     catch(final Exception e) {
                        // / Just ignore it...
                     }
                  if(comps.size() > 0)
                     res.put(elm, Collections.unmodifiableList(comps));
               }
               catch(final IOException e) {
                  e.printStackTrace();
               }
         }
      }
      catch(final Exception e) {
         // Ok, it failed, well too bad!
         e.printStackTrace();
      }
      mData = Collections.unmodifiableMap(res);
   }
}

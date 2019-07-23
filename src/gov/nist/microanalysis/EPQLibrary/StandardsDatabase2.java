package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * These classes represent a minimalist standard block database. Each standard
 * block is defined a name and the materials it contains.
 * <p>
 * <p>
 * This simple structure facilitates
 * <ul>
 * <li>Searching the list of available materials</li>
 * <li>Identifying which block(s) contain the material</li>
 * <li>Specifying the minimal number of blocks necessary to perform an analysis
 * </li>
 * </ul>
 * 
 * @author nicholas
 */
public class StandardsDatabase2 {

   /**
    * Default tolerance for Composition equality
    */
   private static final double DEFAULT_TOL = 1.0e-4;

   /**
    * The user object representing a standard block.
    * 
    * @author nicholas
    */
   public class StandardBlock2
      implements
      Comparable<StandardBlock2> {
      private final String mName;
      private final List<String> mStandards;
      private int mPriority = 3;

      private transient boolean mModified = false;

      private StandardBlock2(String name) {
         mName = name;
         mStandards = new ArrayList<String>();
         mPriority = 3;
      }

      private StandardBlock2(String name, Collection<String> bd) {
         mName = name;
         mStandards = new ArrayList<String>(bd);
         mPriority = 3;
      }

      public boolean has(String name) {
         for(final String comp : mStandards)
            if(comp.equals(name))
               return true;
         return false;
      }

      /**
       * Add a standard defined by a Composition and a int[2] representing the
       * pixel coordinates of the standard on the block image.
       * 
       * @param comp
       */
      public void addStandard(Composition comp) {
         mStandards.add(comp.toString());
         Collections.sort(mStandards);
         addMaterial(comp);
         mModified = true;
      }

      public void removeStandard(String std) {
         for(final String cs : mStandards)
            if(cs.equals(std)) {
               mStandards.remove(cs);
               cullMaterial(std);
               mModified = true;
               return;
            }
      }

      /**
       * Returns a list of Composition objects associated with this
       * StandardBlock.
       * 
       * @return List&lt;Composition&gt;
       */
      public List<Composition> getStandards() {
         final ArrayList<Composition> res = new ArrayList<Composition>();
         for(final String comp : mStandards)
            res.add(findComposition(comp));
         return Collections.unmodifiableList(res);
      }

      /**
       * Counts the number of Compositions which can be satisfied by this
       * StandardBlock.
       * 
       * @param comps
       * @return int The number of Compositions (&lt;=comps.size())
       */
      public Set<Composition> satisfies(Collection<Composition> comps) {
         final TreeSet<Composition> res = new TreeSet<Composition>();
         for(final Composition comp : comps)
            if(mStandards.contains(comp.toString()))
               res.add(comp);
         return res;
      }

      @Override
      public String toString() {
         return mName;
      }

      @Override
      public int compareTo(StandardBlock2 o) {
         return mName.compareTo(o.mName);
      }

      @Override
      public StandardBlock2 clone() {
         final StandardBlock2 res = new StandardBlock2(mName, mStandards);
         res.mPriority = this.mPriority;
         return res;
      }

      /**
       * The priority is used to rank otherwise equivalent choices of standard
       * block.
       * 
       * @return Returns the priority.
       */
      public int getPriority() {
         return mPriority;
      }

      /**
       * The priority is used to rank otherwise equivalent choices of standard
       * block. The range is 1 to 5 with 5 being the most preferred block. The
       * default is 3.
       * 
       * @param priority The value to which to set priority
       */
      public void setPriority(int priority) {
         mPriority = Math2.bound(priority, 1, 6);
      }
   }

   private String mName;
   private final TreeSet<StandardBlock2> mBlocks;
   private HashSet<Composition> mMaterials;
   private transient boolean mModified = false;

   private void addMaterial(Composition comp) {
      for(final Composition mat : mMaterials)
         if(mat.getName().equals(comp.getName()))
            return;
      mMaterials.add(comp);
   }

   /**
    * Ensures there is just one definition per name.
    */
   private void normalizeMaterials() {
      final TreeMap<String, Composition> comps = new TreeMap<String, Composition>();
      for(final Composition comp : mMaterials)
         comps.put(comp.getName(), comp);
      mMaterials = new HashSet<Composition>(comps.values());
   }

   private Composition findComposition(String name) {
      for(final Composition comp : mMaterials)
         if(comp.getName().equals(name))
            return comp;
      assert false;
      return Material.Null;
   }

   public static StandardsDatabase2 fromXML(String str) {
      final EPQXStream xs = EPQXStream.getInstance();
      return (StandardsDatabase2) xs.fromXML(str);
   }

   public static StandardsDatabase2 read(File file) {
      final EPQXStream xs = EPQXStream.getInstance();
      final StandardsDatabase2 res = (StandardsDatabase2) xs.fromXML(file);
      res.normalizeMaterials();
      return res;
   }

   public static StandardsDatabase2 read(InputStream is) {
      final EPQXStream xs = EPQXStream.getInstance();
      final StandardsDatabase2 res = (StandardsDatabase2) xs.fromXML(is);
      res.normalizeMaterials();
      return res;
   }

   public void replace(Composition newComp) {
      for(final Iterator<Composition> ic = mMaterials.iterator(); ic.hasNext();) {
         final Composition comp = ic.next();
         if(comp.getName().equals(newComp.getName()))
            ic.remove();
      }
      mMaterials.add(newComp);
      mModified = true;
   }

   /**
    * Constructs a StandardsDatabase2 with the specified user friendly name.
    * 
    * @param name
    */
   public StandardsDatabase2(String name) {
      mName = name;
      mBlocks = new TreeSet<StandardBlock2>();
      mMaterials = new HashSet<Composition>();
   }

   /**
    * Has any part of this database been modified since it was created, cloned
    * or saved.
    * 
    * @return true if modified
    */
   public boolean isModified() {
      boolean res = mModified;
      for(final StandardBlock2 block : mBlocks) {
         if(res)
            break;
         res |= block.mModified;
      }
      return res;
   }

   /**
    * Create an exact copy of this StandardsDatabase2 object.
    * 
    * @return StandardsDatabase2
    * @see java.lang.Object#clone()
    */
   @Override
   public StandardsDatabase2 clone() {
      final StandardsDatabase2 res = new StandardsDatabase2(mName);
      for(final StandardBlock2 sb : mBlocks)
         res.mBlocks.add(sb.clone());
      res.mMaterials = new HashSet<Composition>(mMaterials);
      res.resetModified();
      return res;
   }

   /**
    * Write the StandardDatabase to a ZIP encoded file.
    * 
    * @param file
    * @throws IOException
    */
   public void write(File file)
         throws IOException {
      final EPQXStream xs = EPQXStream.getInstance();
      try (final FileOutputStream os = new FileOutputStream(file)) {
         xs.toXML(this, os);
      }
      resetModified();
   }

   private void resetModified() {
      mModified = false;
      for(final StandardBlock2 block : mBlocks)
         block.mModified = false;
   }

   /**
    * Remove unused materials from the database.
    * 
    * @param name
    */
   private void cullMaterial(String name) {
      if(find(name).size() == 0)
         for(final Iterator<Composition> ci = mMaterials.iterator(); ci.hasNext();) {
            final Composition comp = ci.next();
            if(comp.getName().equals(name))
               ci.remove();
         }
   }

   /**
    * Add a new StandardBlock to the database
    * 
    * @param name The name of the block (unique)
    * @return The new StandardBlock object
    */
   public StandardBlock2 addBlock(String name) {
      final StandardBlock2 sb = new StandardBlock2(name);
      mBlocks.add(sb);
      mModified = true;
      return sb;
   }

   /**
    * Removes the StandardBlock specified by name from the database. Also
    * removes unused Composition objects.
    * 
    * @param block
    */
   public void removeBlock(StandardBlock2 block) {
      mBlocks.remove(block);
      for(final String std : block.mStandards)
         cullMaterial(std);
      mModified = true;
   }

   /**
    * Returns the StandardBlock identified by the specified name.
    * 
    * @param name
    * @return StandardBlock
    */
   public StandardsDatabase2.StandardBlock2 getBlock(String name) {
      for(final StandardBlock2 blk : mBlocks)
         if(blk.mName.equals(name))
            return blk;
      return null;
   }

   /**
    * Returns the full collection of StandardBlock objects.
    * 
    * @return Collection&lt;StandardBlock&gt;
    */
   public Collection<StandardsDatabase2.StandardBlock2> getBlocks() {
      return Collections.unmodifiableCollection(mBlocks);
   }

   /**
    * Returns a set containing the names of the available StandardBlocks.
    * 
    * @return Set&lt;String&gt;
    */
   public Set<String> getBlockNames() {
      final TreeSet<String> names = new TreeSet<String>();
      for(final StandardBlock2 blk : mBlocks)
         names.add(blk.mName);
      return Collections.unmodifiableSet(names);
   }

   /**
    * Returns a list of all available Compositions
    * 
    * @return Set&lt;Composition&gt;
    */
   public Set<Composition> allCompositions() {
      return Collections.unmodifiableSet(mMaterials);
   }

   /**
    * Returns a list of the StandardBlocks on which the specified Composition is
    * found.
    * 
    * @param comp
    * @return List&lt;StandardBlock&gt;
    */
   public List<StandardBlock2> find(Composition comp) {
      comp = comp.asComposition();
      for(final Composition mat : mMaterials)
         if(mat.asComposition().almostEquals(comp, DEFAULT_TOL))
            return find(mat.getName());
      return Collections.unmodifiableList(new ArrayList<StandardsDatabase2.StandardBlock2>());
   }

   public Composition getComposition(String name) {
      for(final Composition mat : mMaterials)
         if(mat.getName().equals(name))
            return mat;
      return null;
   }

   /**
    * Returns a list of the StandardBlocks on which the specified Composition is
    * found.
    * 
    * @param name
    * @return List&lt;StandardBlock&gt;
    */
   public List<StandardBlock2> find(String name) {
      final TreeSet<StandardBlock2> res = new TreeSet<StandardsDatabase2.StandardBlock2>();
      for(final StandardBlock2 sb : mBlocks)
         if(sb.has(name))
            res.add(sb);
      return Collections.unmodifiableList(new ArrayList<StandardsDatabase2.StandardBlock2>(res));
   }

   private boolean isIncluded(String mat, Collection<StandardBlock2> exclude) {
      if(exclude == null)
         return true;
      for(final StandardBlock2 sb : find(mat))
         if(!exclude.contains(sb))
            return true;
      return false;
   }

   /**
    * Find Composition objects containing at least 'min' mass fraction of the
    * specified Element.
    * 
    * @param elm
    * @param min
    * @param exclude A collection of blocks to exclude from the search
    * @return List&lt;Composition&gt;
    */
   public List<Composition> findStandards(Element elm, double min, Collection<StandardBlock2> exclude) {
      final List<Composition> res = new ArrayList<Composition>();
      for(final Composition std : mMaterials)
         if((std.weightFraction(elm, false) > min) && isIncluded(std.toString(), exclude))
            res.add(std);

      class CompareComps
         implements
         Comparator<Composition> {
         Element mElement;

         CompareComps(Element elm) {
            mElement = elm;
         }

         @Override
         public int compare(Composition o1, Composition o2) {
            final double c1 = o1.weightFraction(mElement, false);
            final double c2 = o2.weightFraction(mElement, false);
            return c1 < c2 ? 1 : (c1 > c2 ? -1 : 0);
         };
      }
      Collections.sort(res, new CompareComps(elm));
      return Collections.unmodifiableList(res);
   }

   /**
    * Find Composition objects containing at least 'min' mass fraction of the
    * specified Element.
    * 
    * @param elm
    * @param min
    */
   public List<Composition> findStandards(Element elm, double min) {
      return findStandards(elm, min, new ArrayList<StandardBlock2>());
   }

   @Override
   public String toString() {
      return mName == null ? "unnamed" : mName;
   }

   /**
    * Provide a new user-friendly name for the database.
    * 
    * @param name
    */
   public void rename(String name) {
      if(!name.equals(mName)) {
         mName = name;
         mModified = true;
      }
   }

   /**
    * Suggest a minimal set of StandardBlock objects to satisfy the specified
    * list of required Compositions.
    * 
    * @param comps The Compositions
    * @param exclude A list of StandardBlocks to exclude.
    * @return A minimal Set of StandardBlock objects or null if none is possible
    */
   public List<StandardBlock2> suggestBlocks(Collection<Composition> comps, Collection<StandardBlock2> exclude) {
      exclude = (exclude == null ? new TreeSet<StandardBlock2>() : exclude);
      final TreeMap<StandardBlock2, Set<Composition>> useful = new TreeMap<StandardBlock2, Set<Composition>>();
      for(final StandardBlock2 blk : mBlocks)
         if(!exclude.contains(blk)) {
            final Set<Composition> sat = blk.satisfies(comps);
            if(sat.size() > 0)
               useful.put(blk, sat);
         }
      return Collections.unmodifiableList(new ArrayList<StandardBlock2>(helper(comps, useful)));
   }

   /**
    * Recursively find the smallest set of StandardBlock2 objects that provides
    * all the necessary Composition objects.
    * 
    * @param comps
    * @param useful
    * @return Set&lt;StandardBlock2&gt;
    */
   private Set<StandardBlock2> helper(Collection<Composition> comps, TreeMap<StandardBlock2, Set<Composition>> useful) {
      int min = Integer.MAX_VALUE;
      Set<StandardBlock2> bestRes = null;
      double bestScore = -Double.MAX_VALUE;
      for(final Map.Entry<StandardBlock2, Set<Composition>> me : useful.entrySet()) {
         // Which Compositions are unsatisfied?
         final TreeSet<Composition> remaining = new TreeSet<Composition>(comps);
         remaining.removeAll(me.getValue());
         if(remaining.size() < comps.size()) {
            final Set<StandardBlock2> res = new TreeSet<StandardBlock2>();
            // Start with this block
            res.add(me.getKey());
            if(remaining.size() > 0) {
               final TreeMap<StandardBlock2, Set<Composition>> remainingUseful = new TreeMap<StandardBlock2, Set<Composition>>(useful);
               remainingUseful.remove(me.getKey());
               final Set<StandardBlock2> other = helper(remaining, remainingUseful);
               if(other != null) {
                  // Fully satisfies 'remaining'
                  res.addAll(other);
                  if((res.size() < min) || ((res.size() == min) && (score(res) > bestScore))) {
                     bestScore = score(res);
                     bestRes = res;
                     min = bestRes.size();
                  }
               }
            } else if((res.size() < min) || ((res.size() == min) && (score(res) > bestScore))) {
               bestScore = score(res);
               bestRes = res;
               min = bestRes.size();
            }
         }
      }
      return bestRes != null ? bestRes : new TreeSet<StandardBlock2>();
   }

   private double score(Collection<StandardBlock2> blocks) {
      double res = 0;
      for(final StandardBlock2 block : blocks)
         res += block.getPriority();
      return res / blocks.size();
   }

   /**
    * Ensure that the specified Compositions is represented in the database
    * Session.
    * 
    * @param ses
    * @param def
    * @return Composition
    * @throws Exception
    */
   private static Composition validateComposition(Session ses, String def)
         throws Exception {
      if(def != null) {
         Composition res = ses.findStandard(def);
         res = MaterialFactory.createCompound(def);
         ses.addStandard(res);
         return res;
      } else
         return null;
   }

   public StandardBlock2 buildCommonStandardMaterials(Session ses)
         throws Exception {
      final StandardBlock2 res = new StandardBlock2("Common standards");
      res.addStandard(validateComposition(ses, "C"));
      res.addStandard(validateComposition(ses, "BN"));
      res.addStandard(validateComposition(ses, "Al2O3"));
      res.addStandard(validateComposition(ses, "MgO"));
      res.addStandard(validateComposition(ses, "CaF2"));
      res.addStandard(validateComposition(ses, "BaF2"));
      res.addStandard(validateComposition(ses, "Mg"));
      res.addStandard(validateComposition(ses, "Al"));
      res.addStandard(validateComposition(ses, "BaCO4"));
      res.addStandard(validateComposition(ses, "BN"));
      res.addStandard(validateComposition(ses, "BN"));
      return res;
   }
};

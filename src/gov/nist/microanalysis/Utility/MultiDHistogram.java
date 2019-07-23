package gov.nist.microanalysis.Utility;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class MultiDHistogram {

   public interface IBinning {
      /**
       * Computes the index of the bin into which this data point falls.
       * 
       * @param vals
       * @return int[] or null if not within range
       */
      public int[] compute(double[] vals);

      /**
       * Returns the lower and upper limits for the specified bin in the
       * specified dimension.
       * 
       * @param bin int
       * @param dim int the dimension index
       * @return double[2] -&gt; [ min, max ]
       */
      public double[] limits(int bin, int dim);

      /**
       * Returns the size of the multi-D histogram dimensions
       * 
       * @return int[]
       */
      public int[] getDimensions();

   }

   static public class LinearBins
      implements IBinning {

      private final int mDimCount;
      private final double mWidth;
      private final int mDimLength;

      public LinearBins(int dim, double width, int number) {
         mDimCount = dim;
         mWidth = width;
         mDimLength = number;
      }

      @Override
      public int[] compute(double[] vals) {
         final int[] res = new int[vals.length];
         for(int i = 0; i < vals.length; ++i)
            res[i] = Math2.bound((int) (vals[i] / mWidth), 0, mDimLength);
         return res;
      }

      @Override
      public double[] limits(int bin, int dim) {
         assert (bin >= 0) && (bin < mDimLength);
         assert (dim >= 0) && (dim < mDimCount);
         if((bin >= 0) && (bin < mDimLength) && (dim >= 0) && (dim < mDimCount))
            return new double[] {
               bin * mWidth,
               bin * (mWidth + 1)
            };
         else
            return null;
      }

      @Override
      public int[] getDimensions() {
         final int[] dims = new int[mDimCount];
         Arrays.fill(dims, mDimLength);
         return dims;
      }
   }

   // Number of dimensions
   private final IBinning mBinning;
   private final TreeSet<Bin> mData;
   private int mIslandCount;

   public static final int NO_ISLAND = Integer.MAX_VALUE;

   static public class Bin
      implements Comparable<Bin> {
      static private final int DEFAULT_CAPACITY = 10;
      /**
       * mIndex uniquely identifies the bin via integer indices
       */
      final private int[] mIndex;
      /**
       * The number of items in this bin
       */
      private int mSize;
      /**
       * The actual items
       */
      private int[] mItem;
      /**
       * Associates Bins with other bins. Two bins are in the same island if
       * their mIsland are equal. mIsland==NO_ISLAND is unassigned
       */
      private int mIsland;

      private final transient int mHash;

      /**
       * Constructs a Accumulator based on the specified index
       * 
       * @param index
       * @param itemIndex
       */
      private Bin(int[] index, int itemIndex) {
         mIndex = index;
         mItem = new int[DEFAULT_CAPACITY];
         mItem[0] = itemIndex;
         mSize = 1;
         mIsland = NO_ISLAND;
         mHash = 31 + Arrays.hashCode(mIndex);
      }

      /**
       * A copy constructor for Bin
       * 
       * @param vox
       */
      private Bin(Bin vox) {
         mIndex = vox.mIndex;
         mItem = vox.mItem.clone();
         mSize = vox.mSize;
         mIsland = vox.mIsland;
         mHash = 31 + Arrays.hashCode(mIndex);
      }

      public int getDimension() {
         return mIndex.length;
      }

      /**
       * Adds a count to this accumulator.
       * 
       * @param other
       */
      private void add(int item) {
         final int requiredCapacity = DEFAULT_CAPACITY * (((mSize + 1 + DEFAULT_CAPACITY) - 1) / DEFAULT_CAPACITY);
         assert requiredCapacity >= (mSize + 1);
         if(requiredCapacity > mItem.length)
            mItem = Arrays.copyOf(mItem, requiredCapacity);
         mItem[mSize] = item;
         ++mSize;
         assert mSize <= mItem.length;
      }

      /**
       * Are these two Bin objects adjacent to each other.
       * 
       * @param vox
       * @return true or false
       */
      public boolean adjacent(Bin vox) {
         return adjacent(vox, mIndex.length);
      }

      /**
       * Are these two Bin objects m-adjacent to each other. m-adjacent means
       * differ in bin index by one or less in all indexes and the number of
       * different indices is less than or equal to m.
       * 
       * @param vox
       * @return true or false
       */
      public boolean adjacent(Bin vox, int m) {
         assert mIndex.length == vox.mIndex.length;
         int dist = 0;
         for(int d = 0; d < mIndex.length; ++d) {
            final int delta = Math.abs(mIndex[d] - vox.mIndex[d]);
            if(delta > 1)
               return false;
            dist += delta;
         }
         return (dist > 0) && (dist <= m);
      }

      /**
       * The distance in index unit between this Bin and vox
       * 
       * @param vox
       * @return double
       */
      public double distance(Bin vox) {
         assert mIndex.length == vox.mIndex.length;
         double dist = 0.0;
         for(int d = 0; d < mIndex.length; ++d)
            dist += Math.abs(mIndex[d] - vox.mIndex[d]);
         return Math.sqrt(dist);
      }

      public int getIsland() {
         return mIsland;
      }

      /**
       * Does this Voxel contain the specified index item.
       * 
       * @param item
       * @return boolean
       */
      public boolean contains(int item) {
         for(int i = 0; i < mSize; ++i)
            if(mItem[i] == item)
               return true;
         return false;
      }

      /**
       * Returns an array containing the items inside this bin.
       * 
       * @return int[]
       */
      public int[] getItems() {
         return Arrays.copyOf(mItem, mSize);
      }

      /**
       * Returns the number of items in this bin
       * 
       * @return int
       */
      public int getCount() {
         return mSize;
      }

      @Override
      public String toString() {
         final StringBuffer res = new StringBuffer();
         for(final int element : mIndex) {
            res.append(element);
            res.append(", ");
         }
         res.append(mSize);
         return res.toString();
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo(Bin o) {
         assert mIndex.length == o.mIndex.length;
         for(int i = 0; i < mIndex.length; ++i)
            if(mIndex[i] < o.mIndex[i])
               return -1;
            else if(mIndex[i] > o.mIndex[i])
               return 1;
         return 0;
      }

      @Override
      public int hashCode() {
         return mHash;
      }

      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(getClass() != obj.getClass())
            return false;
         final Bin other = (Bin) obj;
         return Arrays.equals(mIndex, other.mIndex);
      }
   }

   /**
    * Constructs a MultiDHistogram object to contain the multi-histogram data.
    */
   public MultiDHistogram(IBinning binning) {
      mBinning = binning;
      mData = new TreeSet<Bin>();
      mIslandCount = 0;
   }

   /**
    * A copy constructor for MultiDHistogram
    * 
    * @param src
    */
   public MultiDHistogram(MultiDHistogram src) {
      this(src, 1);
   }

   /**
    * A copy constructor for MultiDHistogram that trims all histogram bins with
    * less than minMembers.
    * 
    * @param src
    * @param minMembers
    */
   public MultiDHistogram(MultiDHistogram src, int minMembers) {
      mBinning = src.mBinning;
      mData = new TreeSet<Bin>();
      mIslandCount = 0;
      for(final Bin vox : src.mData)
         if(vox.mSize >= minMembers)
            add(vox);
   }

   /**
    * Add a single datum to the multi-dimensional histogram. The first call to
    * <code>add(..)</code> effectively defines the dimensionality of the
    * histogram through the length of the <code>index</code> argument.
    * 
    * @param data
    * @param item Item index
    */
   public void add(double[] data, int item) {
      final int[] index = mBinning.compute(data);
      if(index != null) {
         final Bin newBin = new Bin(index, item);
         final Bin b = find(newBin);
         if(b != null)
            b.add(item);
         else
            addBin(newBin);
      }
   }

   /**
    * Add a copy of the specified Bin (presumably from another MultiDHistogram)
    * to this MultiDHistogram.
    * 
    * @param newBin
    */
   public void add(Bin newBin) {
      final Bin dup = new Bin(newBin);
      dup.mIsland = NO_ISLAND;
      addBin(dup);
   }

   /**
    * Find the bin with the same index as the specified index.
    * 
    * @param b
    * @return Bin
    */
   public Bin find(Bin b) {
      final Bin res = mData.floor(b);
      return (res != null) && (res.compareTo(b) == 0) ? res : null;
   }

   /**
    * Add this bin to the MultiDHistogram. Place it in the correct island.
    * 
    * @param newBin
    */
   private void addBin(Bin newBin) {
      assert newBin.mIsland == NO_ISLAND;
      assert islandCheck() : "Island indices are not contiguous";
      /*
       * Check if this bin is connected to another bin...
       */
      for(final Bin b1 : mData)
         if(b1.adjacent(newBin) && (newBin.mIsland != b1.mIsland))
            if(newBin.mIsland == NO_ISLAND) {
               newBin.mIsland = b1.mIsland;
               assert mIslandCount > newBin.mIsland;
            } else {
               /*
                * Merge the larger indexed island into the smaller. Shift island
                * indicies greater then larger down by one.
                */
               final int smaller = Math.min(newBin.mIsland, b1.mIsland);
               final int larger = Math.max(newBin.mIsland, b1.mIsland);
               assert larger > smaller;
               assert mIslandCount > larger;
               for(final Bin b2 : mData) {
                  if(b2.mIsland == larger)
                     b2.mIsland = smaller;
                  if(b2.mIsland > larger)
                     --b2.mIsland;
               }
               newBin.mIsland = smaller;
               --mIslandCount;
               assert mIslandCount > smaller;
            }
      if(newBin.mIsland == NO_ISLAND) {
         /*
          * Not in an existing island, create a new one.
          */
         newBin.mIsland = mIslandCount;
         ++mIslandCount;
      }
      assert newBin.mIsland < mIslandCount;
      mData.add(newBin);
      assert islandCheck() : "Island indices are not contiguous";
   }

   private boolean islandCheck() {
      final Boolean[] present = new Boolean[mIslandCount];
      Arrays.fill(present, false);
      for(final Bin b : mData)
         present[b.mIsland] = true;
      boolean res = true;
      for(final Boolean element : present)
         res &= element;
      return res;
   }

   /**
    * Returns a MultiDHistogram object containing the specified island of
    * contiguous bins.
    * 
    * @param ni
    * @return MultiDHistogram
    */
   public MultiDHistogram getIsland(int ni) {
      final MultiDHistogram res = new MultiDHistogram(mBinning);
      for(final Bin bin : mData)
         if(bin.mIsland == ni)
            res.add(bin);
      return res;
   }

   /**
    * Get the number of bins in the specified island
    * 
    * @param ni
    * @return Integer number of bins
    */
   public int getIslandSize(int ni) {
      int cx = 0;
      for(final Bin bin : mData)
         if(bin.mIsland == ni)
            ++cx;
      return cx;
   }

   /**
    * Returns the number of discontinuous islands present in the total
    * histogram.
    * 
    * @return int
    */
   public int getIslandCount() {
      assert checkIslandCount() == mIslandCount;
      return mIslandCount;
   }

   private int checkIslandCount() {
      int largest = -1;
      for(final Bin bin : mData)
         if(bin.mIsland > largest)
            largest = bin.mIsland;
      return largest + 1;
   }

   /**
    * Returns the total number of items in the histogram.
    * 
    * @return int
    */
   public int getTotalCount() {
      int total = 0;
      for(final Bin bin : mData)
         total += bin.getCount();
      return total;
   }

   /**
    * Returns the index of the island in which this item is found.
    * 
    * @param item
    * @return int
    */
   public int findItemsIsland(int item) {
      for(final Bin bin : mData)
         if(bin.contains(item))
            return bin.mIsland;
      return NO_ISLAND;
   }

   /**
    * Returns a sorted array (<code>Arrays.sort(...)</code>) of the items in the
    * specified island.
    * 
    * @param n
    * @return int[]
    */
   public int[] getItemsInIsland(int n) {
      assert n < mIslandCount;
      final int nn = getIslandSize(n);
      final int[] res = new int[nn];
      int cx = 0;
      for(final Bin bin : mData)
         if(bin.mIsland == n)
            for(int i = 0; i < bin.mSize; ++i, ++cx)
               res[cx] = bin.mItem[i];
      Arrays.sort(res);
      return res;
   }

   /**
    * Returns an array of the items which are located in the same or connected
    * adjacent bins to the specified starter item.
    * 
    * @param starter
    * @return int[]
    */
   public int[] connected(int starter) {
      Bin vox = null;
      for(final Bin datum : mData)
         if(datum.contains(starter)) {
            vox = datum;
            break;
         }
      final MultiDHistogram nbd = vox != null ? getIsland(vox.mIsland) : new MultiDHistogram(mBinning);
      int cx = 0;
      for(final Bin datum : nbd.mData)
         cx += datum.mSize;
      final int[] res = new int[cx];
      int i = 0;
      for(final Bin datum : nbd.mData)
         for(int j = 0; j < datum.mSize; ++j)
            res[i++] = datum.mItem[j];
      assert i == cx;
      Arrays.sort(res);
      return res;
   }

   /**
    * Returns an unmodifiable set of the Bin objects making up this
    * MultiDHistogram
    * 
    * @return Set&lt;Bin&gt;
    */
   public Set<Bin> getBins() {
      return Collections.unmodifiableSet(mData);
   }

   /**
    * Compute the chiSquared statistics to determine whether two histograms are
    * equal to within the specified confidenceLevel. Returns less than 1.0 if
    * the distributions are essentially equivalent or &gt;1 if they are
    * dissimilar.
    * 
    * @param mdh2
    * @param minSize Discard bins with less than this number of members in
    *           md1+md2
    * @param confidenceLevel Typically 0.683, 0.90, 0.954, 0.99, 0.9973 or
    *           0.9999
    * @return double
    */
   public double chiSquaredTest(MultiDHistogram mdh2, int minSize, double confidenceLevel) {
      double chi1 = 0.0, chi2 = 0.0;
      double n1 = 0.0, n2 = 0.0;
      int df = 0;
      final TreeSet<Bin> allBins = new TreeSet<Bin>();
      allBins.addAll(mData);
      allBins.addAll(mdh2.mData);
      for(final Bin ba : allBins) {
         final Bin b1 = mData.floor(ba);
         final Bin b2 = mdh2.mData.floor(ba);
         final double x1 = (b1 != null) && b1.equals(ba) ? b1.mSize : 0;
         final double x2 = (b2 != null) && b2.equals(ba) ? b2.mSize : 0;
         final double s = x1 + x2;
         if(s >= minSize) {
            n1 += x1;
            n2 += x2;
            chi1 += (x1 * x1) / s;
            chi2 += (x2 * x2) / s;
            ++df;
         }
      }
      final double ns = n1 + n2;
      chi1 -= (n1 * n1) / ns;
      chi2 -= (n2 * n2) / ns;
      final double k = (ns * ns) / (n1 * n2);
      chi1 *= k;
      chi2 *= k;
      assert Math.abs(chi1 - chi2) < 0.0001;
      return chi1 / Math2.chiSquaredConfidenceLevel(confidenceLevel, df);
   }

   /**
    * Returns the number of events in the specified histogram bin.
    * 
    * @param index
    * @return The count as an integer
    */
   public int getCount(int[] index) {
      final Bin bin = new Bin(index, 0);
      final Bin res = mData.floor(bin);
      return (res != null) && (res.compareTo(bin) == 0) ? res.getCount() : 0;
   }
}

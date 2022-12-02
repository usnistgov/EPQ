package gov.nist.microanalysis.Utility;

import java.util.Arrays;

/**
 * <p>
 * MemberSet is a class which records whether each member of a fixed number of
 * items is a member or not a member of the set. It provides methods to work
 * with identically sized sets of members.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class MemberSet {

   public static final int END_OF_MEMBERS = -1;
   public static final int INIT_NEXT_MEMBER = -1;

   static private final int QUANTA = 8; // Byte.SIZE
   static private byte[] MASKS = {(byte) (0x1 << 0), (byte) (0x1 << 1), (byte) (0x1 << 2), (byte) (0x1 << 3), (byte) (0x1 << 4), (byte) (0x1 << 5),
         (byte) (0x1 << 6), (byte) (0x1 << 7)};

   static private byte[] INV_MASKS = {(byte) (0xFF - (0x1 << 0)), (byte) (0xFF - (0x1 << 1)), (byte) (0xFF - (0x1 << 2)), (byte) (0xFF - (0x1 << 3)),
         (byte) (0xFF - (0x1 << 4)), (byte) (0xFF - (0x1 << 5)), (byte) (0xFF - (0x1 << 6)), (byte) (0xFF - (0x1 << 7))};

   private final byte[] mMembers;
   private final int mSize;

   /**
    * Constructs a membership set containing <code>size</code> items which are
    * initialized to <code>init</code>.
    */
   public MemberSet(int size, boolean init) {
      mSize = size;
      mMembers = new byte[(size + (QUANTA - 1)) / QUANTA];
      reset(init);
   }

   /**
    * Constructs a replica of the argument MemberSet.
    * 
    * @param src
    */
   public MemberSet(MemberSet src) {
      mSize = src.size();
      mMembers = new byte[(mSize + (QUANTA - 1)) / QUANTA];
      System.arraycopy(src.mMembers, 0, mMembers, 0, mSize);
   }

   /**
    * Do this MemberSet contain the same members as ms?
    * 
    * @param ms
    * @return true if they contain the same members; false otherwise.
    */
   public boolean equals(MemberSet ms) {
      if (ms == this)
         return true;
      for (int i = mMembers.length - 2; i >= 0; --i)
         if (mMembers[i] != ms.mMembers[i])
            return false;
      for (int i = QUANTA * (mMembers.length - 1); i < mSize; ++i)
         if (this.contains(i) != ms.contains(i))
            return false;
      return true;

   }

   /**
    * Is the idx'th item a member of the set?
    * 
    * @param idx
    * @return boolean
    */
   public boolean contains(int idx) {
      return (mMembers[idx / QUANTA] & MASKS[idx % QUANTA]) != 0;
   }

   /**
    * Add the idx-th item to the MemberSet
    * 
    * @param idx
    */
   public void add(int idx) {
      mMembers[idx / QUANTA] |= MASKS[idx % QUANTA];
   }

   /**
    * Remove the idx-th item from the MemberSet
    * 
    * @param idx
    */
   public void remove(int idx) {
      mMembers[idx / QUANTA] &= INV_MASKS[idx % QUANTA];
   }

   /**
    * Returns the number of potential members in the member set.
    * 
    * @return int
    */
   public int size() {
      return mSize;
   }

   /**
    * Include the items in <code>ms</code> in this MemberSet.
    * 
    * @param ms
    */
   public void include(MemberSet ms) {
      assert (ms.size() == size());
      for (int i = 0; i < mMembers.length; ++i)
         mMembers[i] |= ms.mMembers[i];
   }

   /**
    * Remove the items in <code>ms</code> from this MemberSet
    * 
    * @param ms
    */
   public void remove(MemberSet ms) {
      assert (ms.size() == size());
      for (int i = 0; i < mMembers.length; ++i)
         mMembers[i] &= (0xFF - ms.mMembers[i]);
   }

   /**
    * Include those items in both this Member set and <code>ms</code>.
    * 
    * @param ms
    */
   public void both(MemberSet ms) {
      assert (ms.size() == size());
      for (int i = 0; i < mMembers.length; ++i)
         mMembers[i] &= ms.mMembers[i];
   }

   /**
    * Include those item that are in neither this MemberSet nor <code>ms</code>.
    * 
    * @param ms
    */
   public void neither(MemberSet ms) {
      assert (ms.size() != size());
      for (int i = 0; i < mMembers.length; ++i)
         mMembers[i] = (byte) (~(ms.mMembers[i] | mMembers[i]));
   }

   /**
    * Returns the number of members.
    * 
    * @return int
    */
   public int getMemberCount() {
      int res = 0;
      for (int i = mMembers.length - 2; i >= 0; --i)
         res += Integer.bitCount(mMembers[i] & 0xFF);
      for (int i = QUANTA * (mSize / QUANTA); i < size(); ++i)
         if (contains(i))
            ++res;
      return res;
   }

   /**
    * Reset all items in the member set to the value specified by <code>b</code>
    * 
    * @param b
    */
   public void reset(boolean b) {
      Arrays.fill(mMembers, b ? (byte) 0xFF : 0x0);
   }

   public int nextMember(int i) {
      int j = i + 1;
      if ((j % QUANTA) != 0) {
         for (int k = j; k < ((QUANTA * (j / QUANTA)) + QUANTA); ++k)
            if (contains(k))
               return k;
         j = (QUANTA * (j / QUANTA)) + QUANTA;
      }
      for (int k = j / QUANTA; k < mMembers.length; ++k)
         if (mMembers[k] != 0) {
            final byte b = mMembers[k];
            for (int l = 0; l < MASKS.length; ++l)
               if ((b & MASKS[l]) != 0)
                  return (k * QUANTA) + l;
         }
      return END_OF_MEMBERS;
   }
}

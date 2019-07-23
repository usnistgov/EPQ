/**
 * <p>
 * Tests Utility.MemberSet
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
package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.MemberSet;
import junit.framework.TestCase;

public class MemberSetTest
   extends TestCase {

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#contains(int)}.
    */
   public void testContains() {
      final MemberSet ms = new MemberSet(20, false);
      for(int i = 0; i < ms.size(); i += 3)
         ms.add(i);
      for(int i = 0; i < ms.size(); i += 5)
         ms.add(i);
      for(int i = 0; i < ms.size(); ++i)
         assertTrue(ms.contains(i) == (((i % 3) == 0) || ((i % 5) == 0)));
   }

   /**
    * Test method for {@link gov.nist.microanalysis.Utility.MemberSet#add(int)}.
    */
   public void testAdd() {
      final MemberSet ms = new MemberSet(20, false);
      int count = 0;
      for(int i = 0; i < ms.size(); ++i) {
         final int x = (int) (Math.random() * ms.size());
         if(!ms.contains(x)) {
            count++;
            ms.add(x);
            assertTrue(ms.contains(x));
         }
      }
      assertTrue(ms.getMemberCount() == count);
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#remove(int)}.
    */
   public void testRemoveInt() {
      final MemberSet ms = new MemberSet(20, true);
      int count = 0;
      for(int i = 0; i < ms.size(); ++i) {
         final int x = (int) (Math.random() * ms.size());
         if(ms.contains(x)) {
            count++;
            ms.remove(x);
            assertTrue(!ms.contains(x));
         }
      }
      assertTrue(ms.getMemberCount() == (ms.size() - count));
   }

   /**
    * Test method for {@link gov.nist.microanalysis.Utility.MemberSet#size()}.
    */
   public void testSize() {
      final MemberSet ms = new MemberSet(20, true);
      assertTrue(ms.size() == 20);
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#include(gov.nist.microanalysis.Utility.MemberSet)}
    * .
    */
   public void testInclude() {
      final MemberSet ms1 = new MemberSet(20, false);
      final MemberSet ms2 = new MemberSet(20, false);
      for(int i = 0; i < ms1.size(); i += 3)
         ms1.add(i);
      for(int i = 0; i < ms2.size(); i += 5)
         ms2.add(i);
      ms1.include(ms2);
      for(int i = 0; i < ms1.size(); ++i)
         assertTrue(ms1.contains(i) == (((i % 3) == 0) || ((i % 5) == 0)));
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#remove(gov.nist.microanalysis.Utility.MemberSet)}
    * .
    */
   public void testRemoveMemberSet() {
      final MemberSet ms1 = new MemberSet(20, false);
      final MemberSet ms2 = new MemberSet(20, false);
      for(int i = 0; i < ms1.size(); i += 3)
         ms1.add(i);
      for(int i = 0; i < ms2.size(); i += 5)
         ms2.add(i);
      ms1.remove(ms2);
      for(int i = 0; i < ms1.size(); ++i)
         assertTrue(ms1.contains(i) == (((i % 3) == 0) && (!((i % 5) == 0))));
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#both(gov.nist.microanalysis.Utility.MemberSet)}
    * .
    */
   public void testBoth() {
      final MemberSet ms1 = new MemberSet(20, false);
      final MemberSet ms2 = new MemberSet(20, false);
      for(int i = 0; i < ms1.size(); i += 3)
         ms1.add(i);
      for(int i = 0; i < ms2.size(); i += 5)
         ms2.add(i);
      ms1.both(ms2);
      for(int i = 0; i < ms1.size(); ++i)
         assertTrue(ms1.contains(i) == (((i % 3) == 0) && ((i % 5) == 0)));
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#neither(gov.nist.microanalysis.Utility.MemberSet)}
    * .
    */
   public void testNeither() {
      final MemberSet ms1 = new MemberSet(20, false);
      final MemberSet ms2 = new MemberSet(20, false);
      for(int i = 0; i < ms1.size(); i += 3)
         ms1.add(i);
      for(int i = 0; i < ms2.size(); i += 5)
         ms2.add(i);
      ms1.neither(ms2);
      for(int i = 0; i < ms1.size(); ++i)
         assertTrue(ms1.contains(i) != (((i % 3) == 0) || ((i % 5) == 0)));
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#getMemberCount()}.
    */
   public void testGetMemberCount() {
      final MemberSet ms1 = new MemberSet(21, false);
      ms1.add(1);
      ms1.add(3);
      ms1.add(8);
      ms1.add(12);
      ms1.add(14);
      assertTrue(ms1.getMemberCount() == 5);
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.MemberSet#reset(boolean)}.
    */
   public void testReset() {
      final MemberSet ms1 = new MemberSet(21, true);
      ms1.reset(false);
      assertTrue(ms1.getMemberCount() == 0);
      ms1.reset(true);
      assertTrue(ms1.getMemberCount() == ms1.size());
   }

   public void testEquals() {
      MemberSet ms1 = new MemberSet(20, false);
      MemberSet ms2 = new MemberSet(20, false);
      assertTrue(ms1.equals(ms2));
      for(int i = 0; i < ms1.size(); i += 3)
         ms1.add(i);
      for(int i = 0; i < ms2.size(); i += 5)
         ms2.add(i);
      assertFalse(ms1.equals(ms2));
      ms1 = new MemberSet(20, true);
      ms2 = new MemberSet(20, true);
      assertTrue(ms1.equals(ms2));
      ms2.remove(19);
      assertFalse(ms1.equals(ms2));
   }

   public void testNextMember() {
      final MemberSet ms1 = new MemberSet(2023, false);
      for(int i = 0; i < ms1.size(); i += 3)
         ms1.add(i);
      int j = 0;
      for(int i = ms1.nextMember(-1); i != -1; i = ms1.nextMember(i)) {
         assertTrue(i == j);
         j += 3;
      }
      ms1.reset(false);
      for(int i = 0; i < ms1.size(); i += 21)
         ms1.add(i);
      j = 0;
      for(int i = ms1.nextMember(-1); i != -1; i = ms1.nextMember(i)) {
         assertTrue(i == j);
         j += 21;
      }
   }
}

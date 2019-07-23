/**
 * gov.nist.nanoscalemetrology.JMONSELutils.CacheLinkedHashMap Created by: John
 * Villarrubia Date: Nov 4, 2010
 */
package gov.nist.nanoscalemetrology.JMONSELutils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * Extends LinkedHashMap for use as a cache. The constructor allows a cache size
 * to be set. Once the capacity is reached each new entry causes the eldest
 * entry to be deleted. New methods putPromote() and putPromoteAll() are the
 * same as put() and putAll() except that if the new map entry is already in the
 * map, that entry is promoted to most recent. put() and putAll() remain
 * available (inherited from LinkedHashMap) with the default non-promoting
 * behavior.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author John Villarrubia
 * @version 1.0
 */
/*
 * TODO When I wrote this I overlooked that LinkedHasMap has a constructor that
 * allows to specify that the map order is according to access order (which is
 * what I think I want) instead of the default insertion order. If I take
 * advantage of that, it might make the delete and reinsert stuff I do below
 * unnecessary.
 */
public class CacheLinkedHashMap<K, V>
   extends LinkedHashMap<K, V> {

   private static final long serialVersionUID = 8414106666098859681L;
   private final int cacheSize;

   /**
    * Constructs a CacheLinkedHashMap with given maximum size.
    * 
    * @param cacheSize - maximum number of items to be held in this cache.
    */
   public CacheLinkedHashMap(int cacheSize) {
      super(); // Makes an empty LinkedHashMap.
      this.cacheSize = cacheSize;
   }

   @Override
   protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return size() > cacheSize;
   }

   /**
    * Associates the specified value with the specified key in this map. If this
    * mapping was already in the map, it is promoted to "most recent."
    * 
    * @param key
    * @param value
    * @return
    * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
    */
   public V putPromote(K key, V value) {
      remove(key); // To reset the order, remove if already present.
      return super.put(key, value);
   }

   public void putPromoteAll(Map<? extends K, ? extends V> m) {
      for(final K k : m.keySet())
         remove(k); // Remove all matching entries to preserve order
      putAll(m);
   }

   /**
    * Gets the size of this cache.
    * 
    * @return - the size of this cache
    */
   public int getCacheSize() {
      return cacheSize;
   }
}

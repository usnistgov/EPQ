package gov.nist.microanalysis.Utility;

/**
 * <p>
 * Used to delay the computation of a computationally expensive object until and
 * when necessary. Derived classes compute the object in the function compute().
 * </p>
 *
 * @author Nicholas
 * @version 1.0
 */
public abstract class LazyEvaluate<H> {

   private transient H mValue;

   /**
    * Constructs a LazyEvaluate
    */
   public LazyEvaluate() {
      mValue = null;
   }

   /**
    * Clears the cached value. If get() is called again, the cached value will
    * be recomputed via a call to compute().
    */
   public void reset() {
      synchronized (this) {
         mValue = null;
      }
   }

   public boolean evaluated() {
      return mValue != null;
   }
   
   @Override
   public boolean equals(Object val) {
      return evaluated() && (mValue==val);
      
   }

   /**
    * get() will return the cached value when one exists or call compute to
    * assign the cached value and then return it.
    *
    * @return H An instance of the object
    */
   public H get() {
      H res = mValue;
      if (res == null) {
         synchronized (this) {
            if (mValue == null)
               mValue = compute();
            assert mValue != null;
            res = mValue;
         }
      }
      return res;
   }

   /**
    * Implement this function to compute the value that will be returned by
    * get()
    *
    * @return H
    */
   abstract protected H compute();
}

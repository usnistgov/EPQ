package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Translate2D;

/**
 * @author nicholas
 */
public class StageCoordinate implements Cloneable {

   public static enum Axis {
      X("X", " mm", "0.000"), Y("Y", " mm", "0.000"), Z("Z", " mm", "0.000"), R("Rotate", "\u00B0", "0.00"), T("Tilt", "\u00B0",
            "0.00"), B("Bank", "\u00B0", "0.00");

      private final String mName;
      private final String mUnit;
      private final NumberFormat mFormat;

      private Axis(String name, String unit, String fmtStr) {
         mName = name;
         mUnit = unit;
         mFormat = new HalfUpFormat(fmtStr);
      }

      @Override
      public String toString() {
         return mName;
      }

      public static Axis fromString(String str) {
         for(final Axis axis : Axis.values())
            if(axis.mName.equals(str))
               return axis;
         return null;
      }

      public String unit() {
         return mUnit;
      }

      public String format(double d) {
         return mFormat.format(d);
      }
   }

   private final Map<Axis, Double> mData;

   public StageCoordinate() {
      mData = new TreeMap<Axis, Double>();
   }

   public StageCoordinate(StageCoordinate sc) {
      mData = new TreeMap<Axis, Double>(sc.mData);
   }

   public StageCoordinate(StageCoordinate sc, Translate2D t2d) {
      mData = new TreeMap<Axis, Double>(sc.mData);
      final double[] oldCoord = new double[] {
         mData.get(Axis.X),
         mData.get(Axis.Y)
      };
      final double[] newCoord = t2d.compute(oldCoord);
      mData.put(Axis.X, newCoord[0]);
      mData.put(Axis.Y, newCoord[1]);
   }

   public double[] getCoords() {
      final double[] res = new double[6];
      res[0] = get(Axis.X);
      res[1] = get(Axis.Y);
      res[2] = get(Axis.Z);
      res[3] = get(Axis.R);
      res[4] = get(Axis.T);
      res[5] = get(Axis.B);
      return res;
   }

   /**
    * Returns the position associated with the axis or NaN if there is no
    * position associated with the axis.
    * 
    * @param axis
    * @return A position or Double.NaN
    */
   public double get(Axis axis) {
      return mData.containsKey(axis) ? mData.get(axis) : Double.NaN;
   }

   /**
    * A set containing the Axis objects for which data is available.
    * 
    * @return Set&lt;Axis&gt;
    */
   public Set<Axis> axes() {
      return mData.keySet();
   }

   /**
    * Set the position for the specified axis to the specified value.
    * 
    * @param axis
    * @param val
    */
   public void set(Axis axis, double val) {
      mData.put(axis, val);
   }

   /**
    * Clears any data associated with the specified axis.
    * 
    * @param axis
    */
   public void clear(Axis axis) {
      mData.remove(axis);
   }

   @Override
   public String toString() {
      final StringBuffer res = new StringBuffer();
      for(final Axis axis : Axis.values()) {
         final Double v = mData.get(axis);
         if(v != null) {
            res.append(res.length() == 0 ? "{" : ",");
            res.append(axis.mName);
            res.append(":");
            res.append(axis.format(v));
         }
      }
      res.append("}");
      return res.toString();
   }

   public static StageCoordinate fromString(String str)
         throws EPQException {
      if((str.charAt(0) != '{') || (str.charAt(str.length() - 1) != '}'))
         throw new EPQException("The stage point string is not formatted correctly");
      final StageCoordinate sp = new StageCoordinate();
      final String[] items = str.substring(1, str.length() - 1).split(",");
      if(items.length == 0)
         throw new EPQException("The stage point string does not contain any points.");
      for(final String item : items) {
         final String[] line = item.split(":");
         if(line.length == 2)
            try {
               final Axis ax = Axis.fromString(line[0].trim());
               if(ax != null)
                  sp.set(ax, Double.parseDouble(line[1].trim()));
            }
            catch(final NumberFormatException e) {
               // Ignore it...
            }
      }
      return sp;
   }

   public int size() {
      return mData.size();
   }

   @Override
   public StageCoordinate clone() {
      final StageCoordinate res = new StageCoordinate();
      res.mData.putAll(mData);
      return res;
   }

   public boolean isPresent(Axis axis) {
      return mData.containsKey(axis);
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + ((mData == null) ? 0 : mData.hashCode());
      return result;
   }

   /*
    * (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(obj == null)
         return false;
      if(!(obj instanceof StageCoordinate))
         return false;
      final StageCoordinate other = (StageCoordinate) obj;
      if(mData == null) {
         if(other.mData != null)
            return false;
      } else if(!mData.equals(other.mData))
         return false;
      return true;
   }

   public StageCoordinate delta(StageCoordinate sp1) {
      final StageCoordinate res = new StageCoordinate();
      if(!equals(sp1))
         for(final Axis axis : Axis.values()) {
            final Double d0 = mData.get(axis);
            final Double d1 = sp1.mData.get(axis);
            if(d0 == d1)
               continue;
            if((d0 != null) && (d1 != null) && (!d0.equals(d1)))
               res.set(axis, d1);
         }
      return res;
   }
}

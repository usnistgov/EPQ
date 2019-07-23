package gov.nist.microanalysis.NISTMonte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Constructs an shape from the area inside both shape1 and shape2 or inside all
 * the shapes. The intersection could be null if the shapes don't intersect at
 * any region in space.
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
public class Intersection
   implements Shape, ITransform {

   private final List<Shape> mShapes;

   /**
    * Constructs a Intersection object from two shapes
    */
   public Intersection(Shape sh1, Shape sh2) {
      mShapes = new ArrayList<>();
      mShapes.add(sh1);
      mShapes.add(sh2);
   }

   /**
    * Constructs a Intersection object from many shapes
    */
   public Intersection(Shape[] shapes) {
      mShapes = new ArrayList<>();
      mShapes.addAll(Arrays.asList(shapes));
   }

   /**
    * Constructs a Intersection object from two shapes
    */
   protected Intersection() {
      mShapes = new ArrayList<>();
   }

   protected void add(Shape sh) {
      mShapes.add(sh);
   }

   /**
    * @param pos
    * @return
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      for(final Shape sh : mShapes)
         if(!sh.contains(pos))
            return false;
      return true;
   }

   /**
    * @param pos0
    * @param pos1
    * @return
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection(double[],
    *      double[])
    */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      double[] ares = new double[mShapes.size()];
      for(int i = 0; i < ares.length; ++i)
         ares[i] = mShapes.get(i).getFirstIntersection(pos0, pos1);
      double res = Double.MAX_VALUE;
      if(contains(pos0))
         res = Math2.min(ares);
      else {
         final double EPS = 1.0e-12;
         for(double tmp : ares)
            if((tmp < res) && contains(Math2.plus(pos0, Math2.multiply(tmp + EPS, Math2.minus(pos1, pos0)))))
               res = tmp;
      }
      assert res >= 0.0;
      return res;
   }

   // See ITransform for JavaDoc
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      for(final Shape t : getShapes())
         if(t instanceof ITransform)
            ((ITransform) t).rotate(pivot, phi, theta, psi);
   }

   // See ITransform for JavaDoc
   @Override
   public void translate(double[] distance) {
      for(final Shape t : getShapes())
         if(t instanceof ITransform)
            ((ITransform) t).translate(distance);
   }

   public List<Shape> getShapes() {
      return Collections.unmodifiableList(mShapes);
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Intersection[");
      boolean first = true;
      for(final Shape sh : mShapes) {
         if(!first)
            sb.append(",");
         sb.append(sh.toString());
         first = false;
      }
      sb.append("]");
      return sb.toString();
   }
}

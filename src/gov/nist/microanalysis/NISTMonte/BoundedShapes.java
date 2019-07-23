package gov.nist.microanalysis.NISTMonte;

import java.util.ArrayList;
import java.util.Collection;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * An optimization tool. Does not so much define a shape as wrap other shapes in
 * a manner that speeds the simulation of electron trajectories. Checks complex
 * shapes against a simple shape to optimize the contains(...) and
 * getFirstIntersection(...) functions. Used appropriately, BoundedShapes can
 * provide extreme speed optimizations for complex shapes that can be divided up
 * into discrete blocks containing multiple constituent Shape objects.
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
public class BoundedShapes
   implements Shape {

   private final ArrayList<Shape> mShapes;
   private final SimpleBlock mBounds;

   /**
    * Constructs a BoundedShape
    */
   public BoundedShapes(Shape shape, double[] corner0, double[] corner1) {
      mShapes = new ArrayList<Shape>();
      mShapes.add(shape);
      mBounds = new SimpleBlock(corner0, corner1);
   }

   /**
    * Constructs a BoundedShape
    */
   public BoundedShapes(Collection<Shape> shapes, double[] corner0, double[] corner1) {
      mShapes = new ArrayList<Shape>(shapes);
      mBounds = new SimpleBlock(corner0, corner1);
   }

   public BoundedShapes(Collection<BoundedShapes> shapes) {
      mShapes = new ArrayList<Shape>();
      final double[] c0 = Math2.v3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
      final double[] c1 = Math2.v3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
      for(BoundedShapes bs : shapes) {
         mShapes.addAll(bs.mShapes);
         for(int i = 0; i < 3; ++i) {
            c0[i] = Math.min(c0[i], bs.getCorner0()[i]);
            c1[i] = Math.max(c1[i], bs.getCorner1()[i]);
         }
      }
      mBounds = new SimpleBlock(c0, c1);

   }

   public double[] getCorner0() {
      return mBounds.getCorner0();
   }

   public double[] getCorner1() {
      return mBounds.getCorner1();
   }

   public static BoundedShapes boundedSphere(double[] center, double radius) {
      final double[] c0 = Math2.minus(center, Math2.multiply(radius, Math2.ONE));
      final double[] c1 = Math2.add(center, Math2.multiply(radius, Math2.ONE));
      return new BoundedShapes(new Sphere(center, radius), c0, c1);
   }

   /**
    * Wraps a simple TruncatedSphere object with flattened top and/or bottom
    * surface in a BoundedShapes object.
    *
    * @param center
    * @param radius
    * @param top
    * @param bottom
    * @return BoundedShapes
    */
   public static BoundedShapes boundedTruncatedSphere(double[] center, double radius, double top, double bottom) {
      final double[] c0 = Math2.add(center, Math2.v3(-radius, -radius, Math.max(-radius, top)));
      final double[] c1 = Math2.add(center, Math2.v3(radius, radius, Math.min(radius, bottom)));
      return new BoundedShapes(new TruncatedSphere(center, radius, top, bottom), c0, c1);
   }

   public BoundedShapes add(BoundedShapes bs0, BoundedShapes bs1) {
      ArrayList<BoundedShapes> shapes = new ArrayList<BoundedShapes>();
      shapes.add(bs0);
      shapes.add(bs1);
      return new BoundedShapes(shapes);
   }

   /**
    * @param pos
    * @return
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      if(mBounds.contains(pos))
         for(final Shape sh : mShapes)
            if(sh.contains(pos))
               return true;
      return false;
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
      final double test = mBounds.getFirstIntersection(pos0, pos1);
      if(mBounds.contains(pos0) || ((test >= 0.0) && (test <= 1.0))) {
         double min = Double.MAX_VALUE;
         for(final Shape sh : mShapes) {
            final double tmp = sh.getFirstIntersection(pos0, pos1);
            if(tmp < min)
               min = tmp;
         }
         return min;
      }
      return Double.MAX_VALUE;
   }

}

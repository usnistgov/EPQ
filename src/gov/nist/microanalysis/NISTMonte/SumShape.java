package gov.nist.microanalysis.NISTMonte;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * An implementation of the Shape interface for the union of the volume
 * encompassed by multiple Shape instances. While the constituent Shape
 * instances need not strictly overlap, there is little reason to use this class
 * if they don't. When a trajectory exits a Shape, the trajectory is assumed to
 * return to the Shape's parent Shape. This leads to incorrect default behavior
 * if two child Shape instances overlap. To handle this situation, use the
 * SumShape to represent the overlapping child Shapes as a single Shape. The
 * trajectory will remain inside the SumShape until it exits all the Shapes that
 * define the SumShape.<br>
 * Note: SumShape is not particularly efficient.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class SumShape
   implements MonteCarloSS.Shape, ITransform, TrajectoryVRML.IRender {
   // The list of Shape instances to union.
   private final ArrayList<MonteCarloSS.Shape> mShapes;

   /**
    * Creates a sum shape that represents the sum of an array of Shapes.
    * 
    * @param shapes Shape[]
    */
   public SumShape(MonteCarloSS.Shape[] shapes) {
      mShapes = new ArrayList<MonteCarloSS.Shape>();
      for(final MonteCarloSS.Shape shape : shapes)
         mShapes.add(shape);
   }

   /**
    * Create a sum shape that represents the sum of two shapes.
    * 
    * @param a Shape
    * @param b Shape
    */
   public SumShape(MonteCarloSS.Shape a, MonteCarloSS.Shape b) {
      this(new MonteCarloSS.Shape[] {
         a,
         b
      });
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      for(final MonteCarloSS.Shape shape : mShapes)
         if(shape.contains(pos))
            return true;
      return false;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection(double[],
    *      double[])
    */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      double u = Double.MAX_VALUE;
      if(contains(pos0)) {
         // Starting inside...
         double[] start = pos0.clone();
         do {
            double uInc = -1;
            double[] end = null;
            for(final MonteCarloSS.Shape shape : mShapes)
               if(shape.contains(start)) {
                  final double ui = shape.getFirstIntersection(start, pos1);
                  assert ui > 0.0;
                  if((ui != Double.MAX_VALUE) && (ui > uInc)) {
                     end = Math2.pointBetween(start, pos1, ui);
                     uInc = ui;
                     u = Math2.distance(end, pos0) / Math2.distance(pos1, pos0);
                     if(u > 1.0)
                        break;
                  }
               }
            if(end == null)
               break;
            // Bump the start point into the next Shape...
            start = Math2.plus(end, Math2.multiply(1.0e-14, Math2.normalize(Math2.minus(pos1, pos0))));
            // Repeat until we can take a full step or
            // the step can't be enlarged...
            u = Math2.distance(end, pos0) / Math2.distance(pos1, pos0);
         } while(u < 1.0);
      } else {
         // Starting outside so get the shortest distance to a boundary
         for(final MonteCarloSS.Shape shape : mShapes) {
            final double ui = shape.getFirstIntersection(pos0, pos1);
            if(ui < u)
               u = ui;
         }
      }
      return u;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#rotate(double[], double,
    *      double, double)
    */
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      for(final MonteCarloSS.Shape shape : mShapes) {
         if(!(shape instanceof ITransform))
            throw new EPQFatalException(shape.toString() + " does not support transformation.");
         ((ITransform) shape).rotate(pivot, phi, theta, psi);
      }
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#translate(double[])
    */
   @Override
   public void translate(double[] distance) {
      for(final MonteCarloSS.Shape shape : mShapes) {
         if(!(shape instanceof ITransform))
            throw new EPQFatalException(shape.toString() + " does not support transformation.");
         ((ITransform) shape).translate(distance);
      }
   }

   /**
    * Render the SumShape by rendering each of the sub-Shapes. If a sub-Shape
    * does not implement the interface TrajectoryVRML.IRender then it will be
    * missing from the rendered VRML world.
    * 
    * @param rc
    * @param wr
    * @throws IOException
    * @see gov.nist.microanalysis.NISTMonte.TrajectoryVRML.IRender#render(gov.nist.microanalysis.NISTMonte.TrajectoryVRML.RenderContext,
    *      java.io.Writer)
    */
   @Override
   public void render(TrajectoryVRML.RenderContext rc, Writer wr)
         throws IOException {
      for(final MonteCarloSS.Shape shape : mShapes)
         if(shape instanceof TrajectoryVRML.IRender)
            ((TrajectoryVRML.IRender) shape).render(rc, wr);
   }

   /**
    * Returns an immutable list of the Shapes which define this SumShape object.
    * 
    * @return Returns the shapes.
    */
   public List<MonteCarloSS.Shape> getShapes() {
      return Collections.unmodifiableList(mShapes);
   }

   @Override
   public String toString() {
      final StringBuffer res = new StringBuffer("Sum[");
      boolean first = true;
      for(final MonteCarloSS.Shape shape : mShapes) {
         if(!first)
            res.append(", ");
         res.append(shape.toString());
         first = false;
      }
      res.append("]");
      return res.toString();
   }

};

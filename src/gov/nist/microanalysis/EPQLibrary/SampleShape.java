package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Objects;

import gov.nist.microanalysis.NISTMonte.CylindricalShape;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.NISTMonte.MultiPlaneShape;
import gov.nist.microanalysis.NISTMonte.ShapeDifference;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

/**
 * The SampleShape abstract class represents the base class from which classes
 * describing various common sample shapes can be constructed. SampleShape
 * derived classes can be attached as SpectrumProperties using
 * setSampleShape(...) to inform the quant algorithm about the sample shape. Not
 * all (in fact few) quant algorithms can actually take advantage of this
 * information. Most simply ignore it and perform a bulk analysis. @author
 * Nicholas W. M. Ritchie
 */
abstract public class SampleShape {

   /**
    * <p> A unit normal pointing out from the surface of the sample. </p> <p>
    * Orientation = [0.0, 0.0, -1.0 ] (-Z axis) is perpendicular to the beam.
    * </p>
    */
   protected final double[] mOrientation;

   protected SampleShape() {
      this(Math2.MINUS_Z_AXIS);
   }

   protected SampleShape(double[] orient) {
      mOrientation = Math2.normalize(orient).clone();
   }

   /**
    * Returns a MonteCarloSS.Shape object with the beam incidence point at the
    * origin and the assuming the detector is in the direction pointed towards
    * by the x-axis. @return MonteCarloSS.Shape
    */
   abstract public Shape getShape();

   abstract public String getName();

   /**
    * Returns a unit vector pointing parallel to the nominal orientation of the
    * sample. @return double[3] - a unit vector.
    */
   public double[] getOrientation() {
      return mOrientation.clone();
   }

   /**
    * Returns the coordinates of two corners defining a bounding box which fully
    * contains the shape as double[6]. The shape is assumed to oriented towards
    * a detector on the positive X axis. The top point at the center of the
    * sample is defined as [0.0, 0.0, 0.0]. @return double[6]
    */
   abstract public double[] getBoundingBox();

   /**
    * Returns the volume of the particle in SI. @return double in m^3
    */
   abstract public double getVolume();

   /**
    * Returns the surface area exposed to the electron beam of the particle in
    * SI. @return double in m^3
    */
   abstract public double getArea();

   /**
    * Compares this shape with another for equality. @param ss @return true if
    * the sample shapes are equivalent, false otherwise
    */
   @Override
   abstract public boolean equals(Object ss);

   /**
    * <h2>RightRectangularPrism - Armstrong Green book #1</h2> <p> A rectangular
    * block with thee different dimensions. </p> @author nritchie
    */
   public static class RightRectangularPrism
      extends SampleShape {

      final private double mHeight;
      final private double mDepth;
      final private double mWidth;

      public double getHeight() {
         return mHeight;
      }

      public double getDepth() {
         return mDepth;
      }

      public double getWidth() {
         return mWidth;
      }

      /**
       * @param height Vertical height of rectangle @param depth Thickness of
       * the rectangle along the detector axis @param width Thickness of the
       * rectangle perpendicular to the detector axis
       */
      public RightRectangularPrism(double height, double depth, double width) {
         mHeight = height;
         mDepth = depth;
         mWidth = width;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -0.5 * mDepth,
            -0.5 * mWidth,
            0.0,
            0.5 * mDepth,
            0.5 * mWidth,
            mHeight
         };
      }

      @Override
      public Shape getShape() {
         final double[] dims = new double[] {
            mDepth,
            mWidth,
            mHeight
         };
         final double[] pt = Math2.multiply(0.5 * mHeight, Math2.Z_AXIS);
         return MultiPlaneShape.createBlock(dims, pt, 0.0, 0.0, 0.0);
      }

      @Override
      public String toString() {
         final NumberFormat nf = new HalfUpFormat("0.0 \u00B5m");
         return "Block[Depth=" + nf.format(1.0e6 * mDepth) + ",Width = " + nf.format(1.0e6 * mWidth) + ",Height="
               + nf.format(1.0e6 * mHeight) + "]";
      }

      @Override
      public String getName() {
         return "right rectangular prism";
      }

      @Override
      public double getVolume() {
         return mDepth * mWidth * mHeight;
      }

      @Override
      public double getArea() {
         return mDepth * mWidth;
      }

      /**
       * @return A hash code @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mDepth);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mHeight);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mWidth);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @param obj @return true iff equivalent @see
       * java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof RightRectangularPrism))
            return false;
         final RightRectangularPrism other = (RightRectangularPrism) obj;
         if(Double.doubleToLongBits(mDepth) != Double.doubleToLongBits(other.mDepth))
            return false;
         if(Double.doubleToLongBits(mHeight) != Double.doubleToLongBits(other.mHeight))
            return false;
         if(Double.doubleToLongBits(mWidth) != Double.doubleToLongBits(other.mWidth))
            return false;
         return true;
      }
   }

   /**
    * <h2>Tetragonal prism - Armstrong Green book #2</h2> <p> A rectangular
    * block with square top rotate by 45 degrees so that the diagonal points
    * towards the detector. </p> @author nritchie
    */
   public static class TetragonalPrism
      extends SampleShape {

      final private double mDiagonal;
      final private double mHeight;

      public double getDiagonal() {
         return mDiagonal;
      }

      public double getHeight() {
         return mHeight;
      }

      public TetragonalPrism(double dim, double height) {
         mDiagonal = dim;
         mHeight = height;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -0.5 * mDiagonal,
            -0.5 * mDiagonal,
            0.0,
            0.5 * mDiagonal,
            0.5 * mDiagonal,
            mHeight
         };
      }

      @Override
      public Shape getShape() {
         final double k = 1.0 / Math.sqrt(2.0);
         final double[] dims = new double[] {
            k * mDiagonal,
            k * mDiagonal,
            mHeight
         };
         return MultiPlaneShape.createBlock(dims, Math2.multiply(0.5 * mHeight, Math2.Z_AXIS), 0.25 * Math.PI, 0.0, 0.0);
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.0");
         return "Tetragonal Prism[Diagonal=" + df.format(1.0e6 * getDiagonal()) + " \u00B5m, Height="
               + df.format(1.0e6 * getHeight()) + " \u00B5m]";
      }

      @Override
      public String getName() {
         return "tetragonal prism";
      }

      @Override
      public double getVolume() {
         return 0.5 * mDiagonal * mDiagonal * mHeight;
      }

      @Override
      public double getArea() {
         return 0.5 * mDiagonal * mDiagonal;
      }

      /**
       * @return A hash code @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mDiagonal);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mHeight);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @param obj @return true iff equivalent @see
       * java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof TetragonalPrism))
            return false;
         final TetragonalPrism other = (TetragonalPrism) obj;
         if(Double.doubleToLongBits(mDiagonal) != Double.doubleToLongBits(other.mDiagonal))
            return false;
         if(Double.doubleToLongBits(mHeight) != Double.doubleToLongBits(other.mHeight))
            return false;
         return true;
      }
   }

   /**
    * <h2>Triangular prism - Armstrong Green book #3</h2> <p> A prism like you
    * might use to demonstrate that sunlight contains many different colors.
    * </p> @author nritchie
    */
   public static class TriangularPrism
      extends SampleShape {

      final private double mLength;
      final private double mHeight;

      public double getLength() {
         return mLength;
      }

      public double getHeight() {
         return mHeight;
      }

      public TriangularPrism(double length, double height) {
         mLength = length;
         mHeight = height;
      }

      @Override
      public Shape getShape() {
         final double sqrt2o2 = 1.0 / Math.sqrt(2.0);
         final MultiPlaneShape sh = new MultiPlaneShape();
         sh.addPlane(Math2.Z_AXIS, Math2.multiply(mHeight, Math2.Z_AXIS));
         sh.addPlane(new double[] {
            sqrt2o2,
            0.0,
            -sqrt2o2
         }, Math2.ORIGIN_3D);
         sh.addPlane(new double[] {
            -sqrt2o2,
            0.0,
            -sqrt2o2
         }, Math2.ORIGIN_3D);
         sh.addPlane(Math2.Y_AXIS, Math2.multiply(0.5 * mLength, Math2.Y_AXIS));
         sh.addPlane(Math2.MINUS_Y_AXIS, Math2.multiply(0.5 * mLength, Math2.MINUS_Y_AXIS));
         return sh;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -0.5 * mHeight,
            -0.5 * mLength,
            0.0,
            0.5 * mHeight,
            0.5 * mLength,
            mHeight
         };
      }

      @Override
      public String toString() {
         return "Triangular Prism[Height=" + (new HalfUpFormat("0.0")).format(1.0e6 * getHeight()) + " \u00B5m]";
      }

      @Override
      public String getName() {
         return "triangular prism";
      }

      @Override
      public double getVolume() {
         return 0.5 * mHeight * mHeight * mLength;
      }

      @Override
      public double getArea() {
         return mHeight * mLength;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mHeight);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mLength);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof TriangularPrism))
            return false;
         final TriangularPrism other = (TriangularPrism) obj;
         if(Double.doubleToLongBits(mHeight) != Double.doubleToLongBits(other.mHeight))
            return false;
         if(Double.doubleToLongBits(mLength) != Double.doubleToLongBits(other.mLength))
            return false;
         return true;
      }
   }

   /**
    * <h2>Square pyramind - Armstrong Green book #4</h2> <p> A pyramid like in
    * Egypt. Half as high as the base length. </p> @author nritchie
    */
   public static class SquarePyramid
      extends SampleShape {

      private final double mBaseLength;

      public double getBaseLength() {
         return mBaseLength;
      }

      public double getHeight() {
         return 0.5 * mBaseLength;
      }

      public SquarePyramid(double baseLength) {
         mBaseLength = baseLength;
      }

      @Override
      public Shape getShape() {
         final double sqrt2o2 = 1.0 / Math.sqrt(2.0);
         final double[][] normals = new double[][] {
            new double[] {
               sqrt2o2,
               0.0,
               -sqrt2o2
               },
            new double[] {
               0.0,
               sqrt2o2,
               -sqrt2o2
               },
            new double[] {
               -sqrt2o2,
               0.0,
               -sqrt2o2
               },
            new double[] {
               0.0,
               -sqrt2o2,
               -sqrt2o2
               }
         };
         final MultiPlaneShape sh = new MultiPlaneShape();
         for(int i = 0; i < 4; ++i)
            sh.addPlane(normals[i], Math2.ORIGIN_3D);
         sh.addPlane(Math2.Z_AXIS, Math2.multiply(0.5 * mBaseLength, Math2.Z_AXIS));
         return sh;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -0.5 * mBaseLength,
            -0.5 * mBaseLength,
            0.0,
            0.5 * mBaseLength,
            0.5 * mBaseLength,
            0.5 * mBaseLength,
         };
      }

      @Override
      public String toString() {
         return "Square Pyramid[Height=" + (new HalfUpFormat("0.0")).format(1.0e6 * getHeight()) + " \u00B5m]";
      }

      @Override
      public String getName() {
         return "square pyramid";
      }

      @Override
      public double getVolume() {
         return 0.25 * getHeight() * mBaseLength * mBaseLength;
      }

      @Override
      public double getArea() {
         return mBaseLength * mBaseLength;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mBaseLength);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof SquarePyramid))
            return false;
         final SquarePyramid other = (SquarePyramid) obj;
         if(Double.doubleToLongBits(mBaseLength) != Double.doubleToLongBits(other.mBaseLength))
            return false;
         return true;
      }
   }

   /**
    * <h2>Cylinder - Armstrong Green book #5</h2> <p> A cylinder oriented like
    * an upright column </p> @author nritchie
    */

   public static class Cylinder
      extends SampleShape {
      private final double mRadius;
      private final double mHeight;

      /**
       * Constructs a vertical cylinder with the specified radius and
       * depth @param radius @param depth
       */
      public Cylinder(double radius, double depth) {
         mRadius = radius;
         mHeight = depth;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -mRadius,
            -mRadius,
            0.0,
            mRadius,
            mRadius,
            mHeight
         };
      }

      @Override
      public Shape getShape() {
         return new CylindricalShape(Math2.ORIGIN_3D, Math2.multiply(mHeight, Math2.Z_AXIS), mRadius);
      }

      public double getRadius() {
         return mRadius;
      }

      public double getHeight() {
         return mHeight;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.0");
         final double sc = 1.0e6;
         return "Cylinder[d=" + df.format(sc * mHeight) + " \u00B5m,r=" + df.format(sc * mRadius) + " \u00B5m]";
      }

      @Override
      public String getName() {
         return "cylinder";
      }

      @Override
      public double getVolume() {
         return Math.PI * mRadius * mRadius * mHeight;
      }

      @Override
      public double getArea() {
         return Math.PI * mRadius * mRadius;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mHeight);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mRadius);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof Cylinder))
            return false;
         final Cylinder other = (Cylinder) obj;
         if(Double.doubleToLongBits(mHeight) != Double.doubleToLongBits(other.mHeight))
            return false;
         if(Double.doubleToLongBits(mRadius) != Double.doubleToLongBits(other.mRadius))
            return false;
         return true;
      }
   }

   /**
    * <h2>Fiber - Armstrong Green book #6</h2> <p> A cylinder on its side.
    * Cylindrical symmetry axis perpendicular to the detector axis. </p> @author
    * nritchie
    */
   public static class Fiber
      extends SampleShape {
      private final double mRadius;
      private final double mLength;

      @Override
      public int hashCode() {
         return Objects.hash(mLength, mRadius);
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         Fiber other = (Fiber) obj;
         return Double.doubleToLongBits(mLength) == Double
               .doubleToLongBits(other.mLength)
               && Double.doubleToLongBits(mRadius) == Double
                     .doubleToLongBits(other.mRadius);
      }

      public double getRadius() {
         return mRadius;
      }

      public double getLength() {
         return mLength;
      }

      /**
       * Constructs a horizontal cylinder with the specified radius and length.
       * The orientation of the fiber is perpendicular to both the beam axis and
       * the detector axis. @param radius @param length
       */
      public Fiber(double radius, double length) {
         mRadius = radius;
         mLength = length;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -mRadius,
            -0.5 * mLength,
            0.0,
            mRadius,
            0.5 * mLength,
            2.0 * mRadius
         };
      }

      @Override
      public Shape getShape() {
         final double[] pt0 = Math2.multiply(0.5 * mLength, Math2.Y_AXIS);
         final double[] pt1 = Math2.multiply(-0.5 * mLength, Math2.Y_AXIS);
         final double[] offset = Math2.multiply(mRadius, Math2.Z_AXIS);
         return new CylindricalShape(Math2.plus(pt0, offset), Math2.plus(pt1, offset), mRadius);
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.0");
         final double sc = 1.0e6;
         return "Fiber[d=" + df.format(sc * mLength) + " \u00B5m,r=" + df.format(sc * mRadius) + " \u00B5m]";
      }

      @Override
      public String getName() {
         return "fiber";
      }

      @Override
      public double getVolume() {
         return Math.PI * mRadius * mRadius * mLength;
      }

      @Override
      public double getArea() {
         return 2.0 * mRadius * mLength;
      }
   }

   /**
    * <h2>Hemisphere - Armstrong Green book #7</h2> <p> A hemisphere with
    * rounded side up </p> @author nritchie
    */
   public static class Hemisphere
      extends SampleShape {
      private final double mRadius;

      /**
       * Construct a hemisphere of the specified radius. @param radius
       */
      public Hemisphere(double radius) {
         mRadius = radius;
      }

      public double getRadius() {
         return mRadius;
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -mRadius,
            -mRadius,
            0.0,
            mRadius,
            mRadius,
            mRadius
         };
      }

      @Override
      public Shape getShape() {
         final double[] center = Math2.multiply(mRadius, Math2.Z_AXIS);
         final Shape primary = new gov.nist.microanalysis.NISTMonte.Sphere(center, mRadius);
         final Shape delta = MultiPlaneShape.createSubstrate(Math2.MINUS_Z_AXIS, center);
         return new ShapeDifference(primary, delta);
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.0");
         final double sc = 2.0e6;
         return "Hemisphere[D=" + df.format(sc * mRadius) + " \u00B5m]";
      }

      @Override
      public String getName() {
         return "hemisphere";
      }

      @Override
      public double getVolume() {
         return (2.0 / 3.0) * Math.PI * Math.pow(mRadius, 3.0);
      }

      @Override
      public double getArea() {
         return Math.PI * Math.pow(mRadius, 2.0);
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mRadius);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof Hemisphere))
            return false;
         final Hemisphere other = (Hemisphere) obj;
         if(Double.doubleToLongBits(mRadius) != Double.doubleToLongBits(other.mRadius))
            return false;
         return true;
      }
   }
   /**
    * <h2>Sphere - Armstrong Green book #8</h2> <p> A sphere </p> @author
    * nritchie
    */
   public static class Sphere
      extends SampleShape {
      private final double mRadius;

      /**
       * Create a spherical sample with the specified radius in meters. @param
       * radius
       */
      public Sphere(double radius) {
         mRadius = radius;
      }

      public double getRadius() {
         return mRadius;
      }

      @Override
      public Shape getShape() {
         return new gov.nist.microanalysis.NISTMonte.Sphere(Math2.multiply(mRadius, Math2.Z_AXIS), mRadius);
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -mRadius,
            -mRadius,
            0.0,
            mRadius,
            mRadius,
            2.0 * mRadius
         };
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.0");
         return "Sphere[r=" + df.format(1.0e6 * mRadius) + "\u00B5m]";
      }

      @Override
      public String getName() {
         return "sphere";
      }

      @Override
      public double getVolume() {
         return (4.0 / 3.0) * Math.PI * Math.pow(mRadius, 3.0);
      }

      @Override
      public double getArea() {
         return Math.PI * Math.pow(mRadius, 2.0);
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mRadius);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof Sphere))
            return false;
         final Sphere other = (Sphere) obj;
         if(Double.doubleToLongBits(mRadius) != Double.doubleToLongBits(other.mRadius))
            return false;
         return true;
      }
   }

   /**
    * <p> Bulk represents a bulk, flat polished sample (possibly tilted).
    * </p> @author nritchie
    */
   public static class Bulk
      extends SampleShape {

      @Override
      public boolean equals(Object ss) {
         return ss.getClass().equals(getClass());
      }

      /**
       * Create a bulk sample normal to the electron beam.
       */
      public Bulk() {
         this(Math2.MINUS_Z_AXIS);
      }

      /**
       * Create a tilted bulk sample. @param normal double[3] A surface normal
       * [0.0,0.0,-1.0] is perpendicular to the beam.
       */
      public Bulk(double[] normal) {
         super(normal);
      }

      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -1.0e-3,
            -1.0e-3,
            0.0,
            1.0e-3,
            1.0e-3,
            2.0e-3
         };
      }

      @Override
      public Shape getShape() {
         return MultiPlaneShape.createSubstrate(mOrientation, Math2.ORIGIN_3D);
      }

      @Override
      public String toString() {
         if(Math2.distance(mOrientation, Math2.MINUS_Z_AXIS) < 1.0e-6)
            return "Bulk";
         else
            return "Bulk[" + Math2.toString(mOrientation, new HalfUpFormat("0.00")) + "]";
      }

      @Override
      public String getName() {
         return "bulk";
      }

      @Override
      public double getVolume() {
         return Math.pow(2.0e-3, 3.0);
      }

      @Override
      public double getArea() {
         return Math.pow(2.0e-3, 2.0);
      }
   }

   static public class ThinFilm
      extends SampleShape {

      private final double mThickness;

      public ThinFilm(double[] normal, double thickness) {
         super(normal);
         mThickness = thickness;
      }

      /**
       * @return @see gov.nist.microanalysis.EPQLibrary.SampleShape#getShape()
       */
      @Override
      public Shape getShape() {
         return MultiPlaneShape.createFilm(mOrientation, Math2.ORIGIN_3D, mThickness);
      }

      /**
       * @return @see gov.nist.microanalysis.EPQLibrary.SampleShape#getName()
       */
      @Override
      public String getName() {
         return "thin film";
      }

      /**
       * @return @see
       * gov.nist.microanalysis.EPQLibrary.SampleShape#getBoundingBox()
       */
      @Override
      public double[] getBoundingBox() {
         return new double[] {
            -1.0e-3,
            -1.0e-3,
            0.0,
            1.0e-3,
            1.0e-3,
            mThickness
         };
      }

      /**
       * @return @see gov.nist.microanalysis.EPQLibrary.SampleShape#getVolume()
       */
      @Override
      public double getVolume() {
         return 2.0e-3 * 2.0e-3 * mThickness;
      }

      /**
       * @return @see gov.nist.microanalysis.EPQLibrary.SampleShape#getArea()
       */
      @Override
      public double getArea() {
         return 2.0e-3 * 2.0e-3;
      }

      public double getThickness() {
         return mThickness;
      }
      
      public int hashCode() {
         return Double.hashCode(mThickness);
      }

      /**
       * @param ss @return @see
       * gov.nist.microanalysis.EPQLibrary.SampleShape#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object ss) {
         if(!(ss instanceof ThinFilm))
            return false;
         final ThinFilm tf = (ThinFilm) ss;
         return Arrays.equals(mOrientation, tf.mOrientation) && (mThickness == tf.mThickness);
      }

      @Override
      public String toString() {
         final NumberFormat nf = new HalfUpFormat("#,##0 nm");
         return "ThinFilm[Thickness=" + nf.format(1.0e9 * mThickness) + ", Normal=" + Arrays.toString(mOrientation) + "]";
      }

   }

}

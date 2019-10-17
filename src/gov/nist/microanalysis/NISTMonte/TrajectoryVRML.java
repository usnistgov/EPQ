package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * TrajectoryVRML is an observer that watches an instance of NISTMonte for
 * events related to electron trajectory changes. It then outputs a VRML
 * description of the trajectory step. The VRML is then used to
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
public class TrajectoryVRML
   implements ActionListener {

   static public final double SCALE = ToSI.micrometer(1.0);

   /**
    * Each class (nominally a MonteCarloSS.Shape) is responsible for knowing how
    * to render itself as VRML. Not all Shape(s) need to implement IRender but
    * shapes that don't implement IRender will not be included in the final VRML
    * file.
    */
   public interface IRender {

      /**
       * render - Renders the object. Use the scale translation 1.0 micrometer
       * equals 1.0 VRML unit.
       * 
       * @param vra The RenderContext object
       * @param dest The Writer into which to output the results
       */
      public void render(RenderContext vra, Writer dest)
            throws IOException;
   }

   static private Color[] mColors = {
      Color.blue,
      Color.red,
      Color.green,
      Color.yellow,
      Color.magenta,
      Color.orange,
      Color.pink,
      Color.cyan,
      Color.white,
      Color.gray,
      Color.lightGray
   };

   /**
    * A set of functions that provide contextual information for rendering an
    * object as VRML.
    */
   public class RenderContext {
      private final Map<Material, Color> mMaterialToColor;
      private Material mCurrentMaterial;
      private double mTransparency = 0.6;
      private int mNextColor = 0;

      /**
       * Constructs a RenderContext
       */
      private RenderContext() {
         super();
         mMaterialToColor = new HashMap<Material, Color>();
         mNextColor = 0;
      }

      /**
       * setCurrentMaterial - Sets the current material. The current material
       * determines the current Color.
       * 
       * @param mat
       */
      public void setCurrentMaterial(Material mat) {
         mCurrentMaterial = mat;
      }

      public Material getCurrentMaterial() {
         return mCurrentMaterial;
      }

      /**
       * specifyColorForMaterial - Allows the user to specify a specific color
       * for a specific material rather than permitting the default assignment.
       * 
       * @param mat
       * @param col
       */
      public void specifyColorForMaterial(Material mat, Color col) {
         mMaterialToColor.put(mat, col);
      }

      /**
       * getTransparency - Get a number 0.0 to 1.0 to represent the transparency
       * (1.0 is transparent, 0.0 is opaque)
       * 
       * @return double
       */
      public double getTransparency() {
         return mTransparency;
      }

      /**
       * setTransparency - Set a number 0.0 to 1.0 to represent the transparency
       * 
       * @param tr 1.0 is transparent, 0.0 is opaque
       */
      public void setTransparency(double tr) {
         assert (tr >= 0.0);
         assert (tr <= 1.0);
         if((tr >= 0.0) && (tr <= 1.0))
            mTransparency = tr;
      }

      /**
       * getCurrentColor - Get the color associated with the current Material
       * (setCurrentMaterial).
       * 
       * @return Color
       */
      public Color getCurrentColor() {
         final NumberFormat nf = NumberFormat.getInstance(Locale.US);
         nf.setMaximumFractionDigits(2);
         nf.setGroupingUsed(false);
         if(mCurrentMaterial == null)
            return Color.white;
         Color color = mMaterialToColor.get(mCurrentMaterial);
         if(color == null) {
            color = mColors[mNextColor];
            mMaterialToColor.put(mCurrentMaterial, color);
            ++mNextColor;
            if(mNextColor == mColors.length)
               mNextColor = 0;
         }
         return color;
      }
   }

   private final Writer mWriter;
   private final RenderContext mContext;
   private int mTrajectoryCount = 0;
   private int mMaxTrajectories = 100;
   private boolean mDisplayXRayEvent = true;
   private boolean mDisplayBackscatter = false;
   private double mTrajectoryWidth = 0.0025;
   private double mMaxR = 1.0e-4;
   private boolean mShowIncident = false;
   private boolean mEmissive = false; // Diffuse or emissive color
   private MonteCarloSS mMonte = null;
   private boolean mInitialized = false;
   private final NumberFormat mOutputFormat;

   /**
    * Constructs a TrajectoryVRML object with the specified RenderContext that
    * outputs to the specified Writer.
    * 
    * @param mcss
    * @param wr
    */
   public TrajectoryVRML(MonteCarloSS mcss, Writer wr) {
      super();
      mWriter = wr;
      mContext = new RenderContext();
      mMonte = mcss;
      mOutputFormat = NumberFormat.getNumberInstance(Locale.US);
      mOutputFormat.setMaximumFractionDigits(5);
      mOutputFormat.setGroupingUsed(false);
      try {
         mWriter.append("#VRML V2.0 utf8\n");
         mWriter.append("#Generated by NISTMonte\n");
         mWriter.append("WorldInfo {\n");
         mWriter.append(" title \"NISTMonte Trajectory\"\n");
         mWriter.append(" info [\"See https://cstl.nist.gov/div837/837.02/epq/dtsa2/index.html for more information\"]\n");
         mWriter.append("}");
      }
      catch(final IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * setMaxTrajectories - Sets the maximum number of trajectories to add to
    * this image. Subsequent trajectories are ignored.
    * 
    * @param max int
    */
   public void setMaxTrajectories(int max) {
      mMaxTrajectories = max;
   }

   private void render(MonteCarloSS.RegionBase reg)
         throws IOException {
      final MonteCarloSS.Shape sh = reg.getShape();
      if(sh instanceof IRender) {
         mContext.setCurrentMaterial(reg.getMaterial());
         mWriter.append("\n");
         ((IRender) sh).render(mContext, mWriter);
      }
      for(final MonteCarloSS.RegionBase r : reg.getSubRegions())
         render(r);
   }

   /**
    * renderSample - A mechanism to render the sample geometry as VRML. Not all
    * Shapes are currently supported. Shapes with common Materials are drawn in
    * the same color.
    */
   public void renderSample()
         throws IOException {
      for(final MonteCarloSS.RegionBase r : mMonte.getChamber().getSubRegions())
         render(r);
   }

   /**
    * truncate - Updates pos0 and pos1 such that the chord between pos0 and pos1
    * always remains inside a circle of radius maxR
    * 
    * @param pos0 - Modified during the call
    * @param pos1 - Modified during the call
    * @param maxR
    * @return true if some part of the chord exists inside maxR, false
    *         otherwise.
    */
   private static boolean truncate(double[] pos0, double[] pos1, double maxR) {
      final double a = Math2.sqr(pos1[0] - pos0[0]) + Math2.sqr(pos1[1] - pos0[1]) + Math2.sqr(pos1[2] - pos0[2]);
      if(a == 0.0)
         return false;
      final double b = 2.0 * ((pos0[0] * (pos1[0] - pos0[0])) + (pos0[1] * (pos1[1] - pos0[1])) + (pos0[2] * (pos1[2] - pos0[2])));
      final double c = (Math2.sqr(pos0[0]) + Math2.sqr(pos0[1]) + Math2.sqr(pos0[2])) - (maxR * maxR);
      final double d = (b * b) - (4.0 * a * c);
      if(d <= 0)
         return false; // Intersects nowhere...
      double t0, t1;
      {
         final double sqrt_d = Math.sqrt(d);
         final double tp = (-b + sqrt_d) / (2.0 * a);
         final double tm = (-b - sqrt_d) / (2.0 * a);
         t0 = Math.min(tp, tm);
         t1 = Math.max(tp, tm);
      }
      if((t0 < 0.0) && (t1 > 1.0))
         return true; // Fully contained
      if((t0 > 1.0) || (t1 < 0.0))
         return false; // Intersects but chord not inside
      final double[] orgPos0 = pos0.clone();
      if(t0 > 0.0) {
         pos0[0] += (pos1[0] - pos0[0]) * t0;
         pos0[1] += (pos1[1] - pos0[1]) * t0;
         pos0[2] += (pos1[2] - pos0[2]) * t0;
      }
      if(t1 < 1.0) {
         pos1[0] = orgPos0[0] + ((pos1[0] - orgPos0[0]) * t1);
         pos1[1] = orgPos0[1] + ((pos1[1] - orgPos0[1]) * t1);
         pos1[2] = orgPos0[2] + ((pos1[2] - orgPos0[2]) * t1);
      }
      return true;
   }

   public void drawLine(double[] pos, double[] prevPos) {
      if(prevPos == null)
         return;
      if(truncate(prevPos, pos, mMaxR)) {
         assert Math2.magnitude(prevPos) <= (1.0001 * mMaxR) : Double.toString(Math2.magnitude(prevPos));
         assert Math2.magnitude(pos) <= (1.0001 * mMaxR) : Double.toString(Math2.magnitude(pos));
         try {
            if(!mInitialized) {
               mInitialized = true;
               mWriter.append("\n#Prototype for a trajectory segment\n");
               mWriter.append("PROTO Segment [\n");
               mWriter.append(" field SFRotation rot 1 0 0 0\n");
               mWriter.append(" field SFVec3f offset 0 0 0\n");
               mWriter.append(" field SFFloat length 0.1\n");
               mWriter.append(" field SFColor color 1 1 1\n");
               mWriter.append("]\n");
               mWriter.append("{\n");
               mWriter.append(" Transform {\n");
               mWriter.append("  rotation IS rot\n");
               mWriter.append("  translation IS offset\n");
               mWriter.append("  children [\n");
               mWriter.append("   Shape {\n");
               mWriter.append("    geometry Cylinder {\n");
               mWriter.append("     radius " + mOutputFormat.format(mTrajectoryWidth) + "\n");
               mWriter.append("     height IS length\n");
               mWriter.append("    }\n");
               mWriter.append("    appearance Appearance {\n");
               mWriter.append("     material Material {\n");
               if(mEmissive)
                  mWriter.append("      emissiveColor IS color\n");
               else
                  mWriter.append("      diffuseColor IS color\n");
               mWriter.append("     }\n");
               mWriter.append("    }\n");
               mWriter.append("   }\n");
               mWriter.append("  ]\n");
               mWriter.append(" }\n");
               mWriter.append("}");
            }
            String colorStr, rotStr, offsetStr, lengthStr;
            {
               final Color color = mContext.getCurrentColor();
               colorStr = mOutputFormat.format(color.getRed() / 255.0) + " " + mOutputFormat.format(color.getGreen() / 255.0)
                     + " " + mOutputFormat.format(color.getBlue() / 255.0);
            }
            // r is the cross product (1,0,0) x norm(d)
            {
               final double[] d = {
                  prevPos[0] - pos[0],
                  prevPos[1] - pos[1],
                  prevPos[2] - pos[2]
               };
               final double dm = Math2.magnitude(d);
               assert (dm > 0.0);
               lengthStr = mOutputFormat.format(dm / SCALE);
               final double[] r = {
                  d[2] / dm,
                  0.0,
                  -d[0] / dm
               };
               final double rm = Math2.magnitude(r);
               if(rm > 0.0) { // if rotation required...
                  double th = Math.asin(rm);
                  if(d[1] < 0.0)
                     th = Math.PI - th;
                  rotStr = mOutputFormat.format(r[0] / rm) + " " + mOutputFormat.format(r[1] / rm) + " "
                        + mOutputFormat.format(r[2] / rm) + " " + mOutputFormat.format(th);
               } else
                  rotStr = "1 0 0 0";
            }
            offsetStr = mOutputFormat.format((pos[0] + prevPos[0]) / (2.0 * SCALE)) + " "
                  + mOutputFormat.format((pos[1] + prevPos[1]) / (2.0 * SCALE)) + " "
                  + mOutputFormat.format((pos[2] + prevPos[2]) / (2.0 * SCALE));
            // All on one line...
            mWriter.append("\nSegment {");
            mWriter.append(" rot " + rotStr);
            mWriter.append(" offset " + offsetStr);
            mWriter.append(" length " + lengthStr);
            mWriter.append(" color " + colorStr);
            mWriter.append(" }");
         }
         catch(final IOException e) {
            // Don't do anything...
         }
      }
      // }
   }

   /**
    * actionPerformed - Implements actionPerformed for the ActionListener
    * interface.
    * 
    * @param e ActionEvent
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      if(mTrajectoryCount < mMaxTrajectories) {
         assert (e.getSource() instanceof MonteCarloSS);
         final MonteCarloSS mcss = (MonteCarloSS) e.getSource();
         switch(e.getID()) {
            case MonteCarloSS.TrajectoryStartEvent:
               break;
            case MonteCarloSS.TrajectoryEndEvent:
               ++mTrajectoryCount;
               break;
            case MonteCarloSS.NonScatterEvent:
            case MonteCarloSS.ScatterEvent: {
               if(mDisplayXRayEvent) {
                  final Electron el = mcss.getElectron();
                  final Material m = el.getCurrentRegion().getMaterial();
                  if(el.getPreviousRegion() != null) {
                     mContext.setCurrentMaterial(m);
                     drawLine(el.getPosition(), el.getPrevPosition());
                  }
               }
               break;
            }
            case MonteCarloSS.BackscatterEvent: {
               if(mDisplayBackscatter) {
                  final Electron el = mcss.getElectron();
                  mContext.setCurrentMaterial(el.getCurrentRegion().getMaterial());
                  drawLine(el.getPosition(), el.getPrevPosition());
               }
               break;
            }
         }
      }
   }

   /**
    * Determines whether backscattered electron tracks are displayed
    * 
    * @return true to display backscattered electrons
    */
   public boolean isDisplayBackscatter() {
      return mDisplayBackscatter;
   }

   /**
    * Determines whether backscattered electron tracks are displayed
    * 
    * @param displayBackscatter
    */
   public void setDisplayBackscatter(boolean displayBackscatter) {
      mDisplayBackscatter = displayBackscatter;
   }

   /**
    * Determines whether x-ray events (scattering events) are displayed
    * 
    * @return Returns true if xray events are to be displayed
    */
   public boolean isDisplayXRayEvent() {
      return mDisplayXRayEvent;
   }

   /**
    * Determines whether x-ray events (scattering events) are displayed
    * 
    * @param displayXRayEvent
    */
   public void setDisplayXRayEvent(boolean displayXRayEvent) {
      mDisplayXRayEvent = displayXRayEvent;
   }

   /**
    * Gets the current value assigned to maxTrajectories
    * 
    * @return Returns the maxTrajectories.
    */
   public int getMaxTrajectories() {
      return mMaxTrajectories;
   }

   /**
    * addView - A mechanism for adding viewports into the VRML output file.
    * 
    * @param name A name for the view
    * @param pos The position of the viewer
    * @param towards The place the viewer is looking
    */
   public void addView(String name, double[] pos, double[] towards)
         throws IOException {
      final double[] def = Math2.MINUS_Z_AXIS;
      final double[] delta = Math2.normalize(new double[] {
         towards[0] - pos[0],
         towards[1] - pos[1],
         towards[2] - pos[2]
      });
      final double[] axis = Math2.cross(def, delta);
      final NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setGroupingUsed(false);
      nf.setMaximumFractionDigits(5);
      mWriter.append("\nViewpoint {\n");
      mWriter.append(" fieldOfView 0.785398\n");
      mWriter.append(" position " + nf.format(pos[0] / SCALE) + " " + nf.format(pos[1] / SCALE) + " "
            + nf.format(pos[2] / SCALE) + "\n");
      final double axisLen = Math2.magnitude(axis);
      final double th = Math2.dot(def, delta) > 0 ? Math.asin(axisLen) : Math.PI - Math.asin(axisLen);
      if(th != 0.0)
         if(axisLen < 1.0e-4)
            mWriter.append(" orientation 1 0 0 3.1415926\n");
         else
            mWriter.append(" orientation " + nf.format(axis[0] / axisLen) + " " + nf.format(axis[1] / axisLen) + " "
                  + nf.format(axis[2] / axisLen) + " " + nf.format(th) + "\n");
      mWriter.append(" description \"" + name + "\"\n");
      mWriter.append("}");
   }

   /*
    * public void addCameraTrajectory(double[] pos0, double[] pos1, double[]
    * center, double duration) { assert (false); try { mWriter.write("\nDEF
    * CAMERATIMER TimeSensor {\n"); mWriter.write(" cycleInterval " +
    * Double.toString(duration) + "\n"); mWriter.write(" loop TRUE\n");
    * mWriter.write("}\n"); mWriter.write("DEF CAMERAPOSINT PositionInterpolator
    * {\n"); mWriter.write(" set_fraction CAMERATIMER\n"); mWriter.write(" key
    * [...]\n"); mWriter.write(" keyValue [...]\n"); mWriter.write("}\n");
    * mWriter.write("DEF CAMERAORINT OrientationInterpolator {\n");
    * mWriter.write(" set_fraction CAMERATIMER\n"); mWriter.write(" key
    * [...]\n"); mWriter.write(" keyValue [...]\n"); } catch(IOException e) {
    * e.printStackTrace(); } }
    */

   /**
    * Gets the current value assigned to the width of the cylinders in
    * micrometers
    * 
    * @return Returns the trajectoryWidth.
    */
   public double getTrajectoryWidth() {
      return ToSI.micrometer(mTrajectoryWidth);
   }

   /**
    * Sets the value assigned to the width of the trajectory cylinders in
    * micrometers. Nominally this is 0.0025 micrometers
    * 
    * @param trajectoryWidth The value to which to set trajectoryWidth.
    */
   public void setTrajectoryWidth(double trajectoryWidth) {
      mTrajectoryWidth = FromSI.micrometer(trajectoryWidth);
   }

   /**
    * Gets the current value assigned to the maximum radius to plot
    * 
    * @return Returns the maximum radius
    */
   public double getMaxRadius() {
      return mMaxR;
   }

   /**
    * Sets the value that determines the maximum radius to plot (nominlly 1.0e-4
    * meters)
    * 
    * @param maxR The value to which to set the maximum radius
    */
   public void setMaxRadius(double maxR) {
      mMaxR = maxR;
   }

   /**
    * Should we display the incident beam?
    * 
    * @return boolean.
    */
   public boolean isShowIncident() {
      return mShowIncident;
   }

   /**
    * Should we display the incident beam?
    * 
    * @param showIncident True to show the incident beam (default = false)
    */
   public void setShowIncident(boolean showIncident) {
      mShowIncident = showIncident;
   }

   /**
    * Determines whether the trajectory segments are drawn as an emissive or
    * diffuse color
    * 
    * @return Returns the emissive.
    */
   public boolean isEmissive() {
      return mEmissive;
   }

   /**
    * Determines whether the trajectory segments are drawn as an emissive or
    * diffuse color
    * 
    * @param emissive true for emissive
    */
   public void setEmissive(boolean emissive) {
      mEmissive = emissive;
   }

   /**
    * getRenderContext - Returns the instance of RenderContext associated with
    * this instance of TrajectoryVRML.
    * 
    * @return TrajectoryVRML.RenderContext
    */
   public RenderContext getRenderContext() {
      return mContext;
   }

   public void addScaleMarker(double diameter, double[] position)
         throws IOException {
      mWriter.append("Transform {\n");
      mWriter.append(" translation ");
      for(int i = 0; i < 3; ++i) {
         mWriter.append(mOutputFormat.format(position[i] / SCALE));
         mWriter.append(i < 2 ? " " : "\n");
      }
      mWriter.append(" children [\n");
      mWriter.append("  Shape {\n");
      mWriter.append("   geometry Sphere { radius " + mOutputFormat.format((0.5 * diameter) / SCALE) + "}\n");
      mWriter.append("   appearance Appearance {\n");
      mWriter.append("    material Material {\n");
      mWriter.append("     emissiveColor 1 1 1\n");
      mWriter.append("     transparency 0.8\n");
      mWriter.append("    }\n");
      mWriter.append("   }\n");
      mWriter.append("  }\n");
      mWriter.append(" ]\n");
      mWriter.append("}\n");

      final NumberFormat nf = new HalfUpFormat("0.00");
      mWriter.append("Transform {\n");
      mWriter.append(" translation ");
      for(int i = 0; i < 3; ++i) {
         mWriter.append(mOutputFormat.format(i == 2 ? (position[i] + (0.7 * diameter)) / SCALE : position[i] / SCALE));
         mWriter.append(i < 2 ? " " : "\n");
      }
      mWriter.append(" rotation -1 0 0 1.57\n");
      mWriter.append(" children [\n");
      mWriter.append("  Shape {\n");
      mWriter.append("   geometry Text {\n");
      mWriter.append("    string [\"" + nf.format(diameter / SCALE) + "\"]\n");
      mWriter.append("    fontStyle FontStyle {\n");
      mWriter.append("     size 0.1\n");
      mWriter.append("     horizontal TRUE\n");
      mWriter.append("     family [\"SANS\"]\n");
      mWriter.append("     justify \"MIDDLE\"\n");
      mWriter.append("    }\n");
      mWriter.append("    maxExtent " + mOutputFormat.format(diameter / SCALE) + "\n");
      mWriter.append("   }\n");
      mWriter.append("  }\n");
      mWriter.append(" ]\n");
      mWriter.append("}\n");
   }
}
package gov.nist.microanalysis.EPQLibrary.Detector;

import java.text.NumberFormat;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A factory for classes that implement the IXRayWindowProperties interface.
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

public class XRayWindowFactory {
   public static final String NO_WINDOW = "No window";
   public static final String UT_WINDOW = "Ultra-thin window";
   public static final String BE_WINDOW = "Be window";
   public static final String DIAMOND_WINDOW = "Diamond window";
   public static final String BN_WINDOW = "Boron Nitride";
   public static final String SI3N4_WINDOW = "Silicon Nitride";

   public static final String Beryllium5micron = "Beryllium (5 \u00B5m)";
   public static final String Beryllium8micron = "Beryllium (8 \u00B5m)";
   public static final String Beryllium12_5micron = "Beryllium (12 \u00B5m)";
   public static final String Beryllium25micron = "Beryllium (25 \u00B5m)";
   public static final String Moxtek_AP1_3 = "Moxtek AP 1.3";
   public static final String Moxtek_AP1_7 = "Moxtek AP 1.7";
   public static final String Moxtek_AP3_3 = "Moxtek AP 3.3 (manufacturer's table)";
   public static final String Moxtek_AP3_3_mod = "Moxtek AP 3.3 (modified table)";
   public static final String Moxtek_AP3_3_Model = "Moxtek AP 3.3 (model)";
   public static final String Moxtek_AP5 = "Moxtek AP 5 (manufacturer's table)";
   public static final String Custom_Table = "Custom Table";
   public static final String NoWindow = NO_WINDOW;

   public static final String Diamond0_45micron = "Diamond (0.45 \u00B5m)";
   public static final String BoronNitride0_25micron = "Boron Nitride (0.25 \u00B5m)";
   public static final String AMPTEK_C1 = "Amptek C1 Si\u00B3N\u2074";
   public static final String AMPTEK_C2 = "Amptek C2 Si\u00B3N\u2074";

   /**
    * WindowTypes - A list of the known window types.
    */
   public static final String[] WindowTypes = {
      Beryllium5micron,
      Beryllium8micron,
      Beryllium12_5micron,
      Beryllium25micron,
      Moxtek_AP1_3,
      Moxtek_AP1_7,
      Moxtek_AP3_3_Model,
      Moxtek_AP3_3,
      NoWindow,
      Diamond0_45micron,
      BoronNitride0_25micron,
      Moxtek_AP3_3_mod,
      AMPTEK_C1,
      AMPTEK_C2,
      Moxtek_AP5,
      Custom_Table
   };

   /**
    * Creates a simple Be window of the specified thickness
    * 
    * @param thickness Meters
    * @return XRayWindow
    */
   public static XRayWindow createBeWindow(double thickness) {
      try {
         final XRayWindow xrw = new XRayWindow(1.0);
         xrw.addLayer(MaterialFactory.createPureElement(Element.Be), thickness);
         final NumberFormat tf = new HalfUpFormat("0");
         xrw.setName(Element.Be.toString() + " (" + tf.format(thickness * 1.0e6) + " \u00B5m)");
         xrw.getProperties().setTextProperty(SpectrumProperties.WindowType, BE_WINDOW);
         return xrw;
      }
      catch(final EPQException e) {
         throw new EPQFatalException(e);
      }
   }

   /**
    * Creates a simple Diamond window of the specified thickness
    * 
    * @param thickness Meters
    * @return XRayWindow
    */
   public static XRayWindow createDiamondWindow(double thickness) {
      final XRayWindow xrw = new XRayWindow(1.0);
      final Material diamond = new Material(Element.C, ToSI.gPerCC(3.52));
      xrw.addLayer(diamond, thickness);
      final NumberFormat tf = new HalfUpFormat("0");
      xrw.setName("Diamond (" + tf.format(thickness * 1.0e6) + " \u00B5m)");
      xrw.getProperties().setTextProperty(SpectrumProperties.WindowType, DIAMOND_WINDOW);
      return xrw;
   }

   /**
    * Creates a simple BoronNitride window of the specified thickness
    * 
    * @param thickness Meters
    * @return XRayWindow
    */
   public static XRayWindow createBoronNitrideWindow(double thickness) {
      try {
         final XRayWindow xrw = new XRayWindow(1.0);
         final Material bn = MaterialFactory.createCompound("BN", ToSI.gPerCC(3.45));
         xrw.addLayer(bn, thickness);
         final NumberFormat tf = new HalfUpFormat("0");
         xrw.setName("Boron nitride(" + tf.format(thickness * 1.0e6) + " \u00B5m)");
         xrw.getProperties().setTextProperty(SpectrumProperties.WindowType, BN_WINDOW);
         return xrw;
      }
      catch(final EPQException e) {
         throw new EPQFatalException(e);
      }
   }

   /**
    * Create a generic Paralene ultra-thin window of the specified thickness.
    * 
    * @param thickness Window thickness (in meters)
    * @param alThickness Alumnium coating thickness (in meters)
    * @param openArea A fraction between (0.0, 1.0]
    * @return An XRayWindow
    */
   public static XRayWindow createGenericUTW(double thickness, double alThickness, double openArea) {
      try {
         assert openArea > 0;
         assert openArea <= 1.0;
         final XRayWindow xrw = new GridMountedWindow(XRayWindow.SI, 0.38e-3, openArea);
         xrw.addLayer(XRayWindow.PARYLENE, thickness);
         xrw.addLayer(MaterialFactory.createPureElement(Element.Al), alThickness);
         final NumberFormat df = new HalfUpFormat("0");
         xrw.setName("UTW(" + df.format(FromSI.angstrom(thickness)) + " /u00C5)");
         xrw.getProperties().setTextProperty(SpectrumProperties.WindowType, UT_WINDOW);
         return xrw;
      }
      catch(final EPQException e) {
         throw new EPQFatalException(e);
      }
   }

   /**
    * Create a generic Moxtek ultra-thin window of the specified thickness.
    * 
    * @param thickness Window thickness (in meters)
    * @param alThickness Alumnium coating thickness (in meters)
    * @param openArea A fraction between (0.0, 1.0]
    * @return An XRayWindow
    */
   public static XRayWindow createGenericMoxtek(double thickness, double alThickness, double openArea) {
      assert openArea > 0;
      assert openArea <= 1.0;
      final XRayWindow xrw = new GridMountedWindow(XRayWindow.SI, 0.38e-3, openArea);
      xrw.addLayer(XRayWindow.DEFAULT_MOXTEK, thickness);
      if(alThickness > 0.0)
         xrw.addLayer(XRayWindow.AL, alThickness);
      final NumberFormat df = new HalfUpFormat("0");
      xrw.setName("Moxtek(" + df.format(FromSI.angstrom(thickness)) + " /u00C5)");
      xrw.getProperties().setTextProperty(SpectrumProperties.WindowType, UT_WINDOW);
      return xrw;
   }

   public static XRayWindow createSi3N4Window(String name, double si3n4, double al, double openFrac) {
      final XRayWindow xrw = new GridMountedWindow(XRayWindow.SI, 0.15e-3, openFrac);
      xrw.addLayer(XRayWindow.SILICON_NITRIDE, si3n4);
      xrw.addLayer(XRayWindow.AL, al);
      xrw.setName(name);
      xrw.getProperties().setTextProperty(SpectrumProperties.WindowType, SI3N4_WINDOW);
      return xrw;

   }

   public static XRayWindow createNoWindow(double openArea) {
      final XRayWindow res = new XRayWindow(openArea);
      res.getProperties().setTextProperty(SpectrumProperties.WindowType, NO_WINDOW);
      res.setName(NO_WINDOW);
      return res;
   }

   public static IXRayWindowProperties createWindow(String window) {
      try {
         int type = -1;
         for(int i = 0; i < WindowTypes.length; ++i)
            if(WindowTypes[i].equals(window)) {
               type = i;
               break;
            }
         switch(type) {
            case 0: { // Beryllium5micron
               assert window.equals(Beryllium5micron);
               return createBeWindow(5.0e-6);
            }
            case 1: { // Beryllium8micron:
               assert window.equals(Beryllium8micron);
               return createBeWindow(8.0e-6);
            }
            case 2: { // Beryllium12_5micron
               assert window.equals(Beryllium12_5micron);
               return createBeWindow(12.0e-6);
            }
            case 3: { // Beryllium25micron
               assert window.equals(Beryllium25micron);
               return createBeWindow(25.0e-6);
            }
            case 4: { // Moxtek_AP1_3
               assert window.equals(Moxtek_AP1_3);
               final IXRayWindowProperties res = createGenericMoxtek(ToSI.angstrom(3000.0), ToSI.angstrom(400.0), 0.75);
               res.setName(window);
               return res;
            }
            case 5: { // Moxtek_AP1_7
               assert window.equals(Moxtek_AP1_7);
               final IXRayWindowProperties res = createGenericMoxtek(ToSI.angstrom(6000.0), ToSI.angstrom(800.0), 0.75);
               res.setName(window);
               return res;
            }
            case 6: { // Moxtek_AP3_3_Model
               assert window.equals(Moxtek_AP3_3_Model);
               final IXRayWindowProperties res = createGenericMoxtek(ToSI.angstrom(3000.0), ToSI.angstrom(400.0), 0.77);
               res.setName(window);
               return res;
            }
            case 7: { // Moxtek_AP3_3
               assert window.equals(Moxtek_AP3_3);
               final SpectrumProperties sp = new SpectrumProperties();
               sp.setNumericProperty(SpectrumProperties.MoxtekWindow, 3.300);
               sp.setNumericProperty(SpectrumProperties.AluminumWindow, 30);
               // I spoke with Eric at Moxtek. The correct thickness is as
               // quoted in their literature 0.38 mm or 380 microns.
               sp.setNumericProperty(SpectrumProperties.SupportGridThickness, 0.38); // mm
               sp.setNumericProperty(SpectrumProperties.WindowOpenArea, 77.0); // percent
               final IXRayWindowProperties res = new XRayWindow2("AP3_3.csv", ToSI.eV(5.0), sp);
               res.getProperties().setTextProperty(SpectrumProperties.WindowType, UT_WINDOW);
               res.setName(window);
               return res;
            }
            case 8: { // No window
               assert window.equals(NO_WINDOW);
               final IXRayWindowProperties res = createNoWindow(1.0);
               res.setName(window);
               return res;
            }
            case 9: { // Diamond0_45micron,
               assert window.equals(Diamond0_45micron);
               final IXRayWindowProperties res = createDiamondWindow(0.45e-6);
               res.setName(window);
               return res;
            }
            case 10: { // BoronNitride0_25micron,
               assert window.equals(BoronNitride0_25micron);
               final IXRayWindowProperties res = createBoronNitrideWindow(0.25e-6);
               res.setName(window);
               return res;
            }
            case 11: { // Moxtek_AP3_3 (my modified table)
               window.equals(Moxtek_AP3_3_mod);
               final SpectrumProperties sp = new SpectrumProperties();
               sp.setNumericProperty(SpectrumProperties.MoxtekWindow, 3.300);
               sp.setNumericProperty(SpectrumProperties.AluminumWindow, 30);
               // I spoke with Eric at Moxtek. The correct thickness is as
               // quoted in their literature 0.38 mm or 380 microns.
               sp.setNumericProperty(SpectrumProperties.SupportGridThickness, 0.38); // mm
               sp.setNumericProperty(SpectrumProperties.WindowOpenArea, 77.0); // percent
               final IXRayWindowProperties res = new XRayWindow2("AP3_3_mod.csv", ToSI.eV(5.0), sp);
               res.getProperties().setTextProperty(SpectrumProperties.WindowType, UT_WINDOW);
               res.setName(window);
               return res;
            }
            case 12: { // AMPTEK C1
               assert window.equals(AMPTEK_C1);
               final IXRayWindowProperties res = createSi3N4Window(AMPTEK_C1, 90.0e-9, 250.0e-9, 0.78);
               res.setName(window);
               return res;
            }
            case 13: { // AMPTEK C2
               assert window.equals(AMPTEK_C2);
               final IXRayWindowProperties res = createSi3N4Window(AMPTEK_C2, 40.0e-9, 30.0e-9, 0.78);
               res.setName(window);
               return res;
            }
            case 14: { // Moxtek_AP5
               assert window.equals(Moxtek_AP5);
               final SpectrumProperties sp = new SpectrumProperties();
               sp.setNumericProperty(SpectrumProperties.MoxtekWindow, 5.0);
               // sp.setNumericProperty(SpectrumProperties.AluminumWindow, 30);
               sp.setNumericProperty(SpectrumProperties.SupportGridThickness, 0.38); // mm
               sp.setNumericProperty(SpectrumProperties.WindowOpenArea, 78.0); // percent
               final IXRayWindowProperties res = new XRayWindow3("AP5.csv", sp);
               res.getProperties().setTextProperty(SpectrumProperties.WindowType, UT_WINDOW);
               res.setName(window);
               return res;
            }
            case 15: { // Custom_Table
               assert window.equals(Custom_Table);
               final SpectrumProperties sp = new SpectrumProperties();
               // sp.setNumericProperty(SpectrumProperties.MoxtekWindow, 5.0);
               // sp.setNumericProperty(SpectrumProperties.AluminumWindow, 30);
               // I spoke with Eric at Moxtek. The correct thickness is as
               // quoted in their literature 0.38 mm or 380 microns.
               final IXRayWindowProperties res = new XRayWindow3("custom.csv", sp);
               res.getProperties().setTextProperty(SpectrumProperties.WindowType, UT_WINDOW);
               res.setName(window);
               return res;
            }
            default:
               return null;
         }
      }
      catch(final EPQException e) {
         throw new EPQFatalException(e);
      }
   }
}

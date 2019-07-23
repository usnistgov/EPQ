package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * <p>
 * A model of a window mounted on a grid with a certain open area fraction. This
 * class is designed to simulate a standard silicon grid mounted polymer window
 * such as produced by Moxtek.
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
public class GridMountedWindow
   extends XRayWindow {

   private final Material mGridMaterial;
   private final double mGridThickness;

   public GridMountedWindow(Material gridMaterial, double thickness, double openArea) {
      super(openArea);
      mGridMaterial = gridMaterial;
      mGridThickness = thickness;
      getProperties().setNumericProperty(SpectrumProperties.SupportGridThickness, 1.0e3 * thickness);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayWindowProperties#transmission(double)
    */
   @Override
   public double transmission(double energy) {
      // Presumably the transmission of the grid area is the transmission of the
      // window plus the transmission of the grid.
      final double mt = AlgorithmUser.getDefaultMAC().compute(mGridMaterial, energy) * mGridThickness
            * mGridMaterial.getDensity();
      return (super.transmission(energy) / mOpenFraction) * (mOpenFraction + ((1.0 - mOpenFraction) * Math.exp(-mt)));
   }

}

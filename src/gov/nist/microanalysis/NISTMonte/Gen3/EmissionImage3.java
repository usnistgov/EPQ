package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.XRay;

/**
 * <p>
 * Creates a bitmap image showing the generation of detected x-rays as a
 * function of position. The thermal color scale shows white where the most
 * x-rays are generated and black where almost no or no x-rays are generated.
 * The left and bottom edge show accumulated transmitted phi-rho-z and phi-rho-x
 * (projected) curves.
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

final public class EmissionImage3 extends EmissionImageBase implements ActionListener {

   private final XRayTransition mTransition;

   public EmissionImage3(int width, int height, XRayTransition xrt) throws EPQException {
      super(width, height);
      mTransition = xrt;
      xrt.getEnergy();
   }

   @Override
   protected String getTitle() {
      return mTransition.toString();
   }

   /**
    * getTransition - Get the transition associated with this image.
    * 
    * @return XRayTransition
    */
   public XRayTransition getTransition() {
      return mTransition;
   }

   /**
    * actionPerformed - Implements actionPerformed for the ActionListener
    * interface.
    * 
    * @param e
    *           ActionEvent
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      final XRayTransport3 xrg = (XRayTransport3) e.getSource();
      switch (e.getID()) {
         case MonteCarloSS.TrajectoryEndEvent :
            ++mTrajectoryCount;
            break;
         case BaseXRayGeneration3.XRayGeneration : {
            if (mTrajectoryCount < mMaxTrajectories) {
               final XRay xr = xrg.getXRay(mTransition);
               if (xr != null) {
                  final double[] pos = xr.getGenerationPos();
                  final double ii = mEmission ? xr.getIntensity() : xr.getGenerated();
                  if (ii > 0.0)
                     setPixel(pos[0], pos[2], ii);
               }
               resetImage();
            }
            break;
         }
      }
   }

}

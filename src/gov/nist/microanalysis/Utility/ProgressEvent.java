package gov.nist.microanalysis.Utility;

import java.awt.event.ActionEvent;

/**
 * <p>
 * An event representing percentage progress towards a goal.
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
public class ProgressEvent
   extends ActionEvent {

   private static final long serialVersionUID = 7157720139752608198L;
   private final int mProgress;

   /**
    * Constructs a ProgressEvent
    * 
    * @param src
    * @param id
    * @param progress in percent
    */
   public ProgressEvent(Object src, int id, int progress) {
      super(src, id, Integer.toString(Math2.bound(progress, 0, 101)) + "%");
      mProgress = Math2.bound(progress, 0, 101);
   }

   public int getProgress() {
      return mProgress;
   }
}

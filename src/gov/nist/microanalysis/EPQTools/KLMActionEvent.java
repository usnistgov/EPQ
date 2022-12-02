/**
 * gov.nist.microanalysis.EPQTools.KLMActionEvent Created by: nritchie Date: Apr
 * 5, 2010
 */
package gov.nist.microanalysis.EPQTools;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Description
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
public class KLMActionEvent extends ActionEvent {

   private static final long serialVersionUID = 2074079796714116586L;

   public enum KLMAction {
      ADD_LINES, REMOVE_LINES, CLEAR_ALL
   };

   final KLMAction mAction;
   final ArrayList<KLMLine> mLines = new ArrayList<KLMLine>();

   /**
    * Constructs a KLMActionEvent
    * 
    * @param source
    * @param lines
    * @param action
    */
   public KLMActionEvent(Object source, Collection<KLMLine> lines, KLMAction action) {
      super(source, 0, "KLM Action", System.currentTimeMillis(), action.ordinal());
      mLines.addAll(lines);
      mAction = action;
   }

   public static KLMActionEvent clearAllEvent(Object source) {
      return new KLMActionEvent(source, new ArrayList<KLMLine>(), KLMAction.CLEAR_ALL);
   }

   public List<KLMLine> getLines() {
      return Collections.unmodifiableList(mLines);
   }

   public KLMAction getAction() {
      return mAction;
   }

}

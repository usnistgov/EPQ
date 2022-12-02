package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * Takes an QuantificationOutline object and returns a QuantificaitonPlan
 * detailing the measurements necessary to implement the outline.
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
abstract public class QuantificationOptimizer {

   protected final QuantificationOutline mOutline;

   protected QuantificationOptimizer(final QuantificationOutline qo) {
      mOutline = qo;
   }

   /**
    * Takes a {@link QuantificationOutline} and returns a
    * {@link QuantificationPlan} object describing how to optimally implement
    * the plan described in the {@link QuantificationOutline}.
    * 
    * @param unk
    *           An estimate of the composition of the unknown.
    * @return QuantificationPlan
    */
   abstract public QuantificationPlan compute(Composition unk) throws EPQException;
}

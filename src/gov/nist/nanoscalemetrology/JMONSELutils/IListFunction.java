/**
 * 
 */
package gov.nist.nanoscalemetrology.JMONSELutils;

/**
 * <p>
 * An IListFunction class is a class that represents an underlying function of N
 * variables (N >= 1), that accepts input in the form of an array of N doubles,
 * and that returns a double.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public interface IListFunction {

	/**
	 * The evaluateAt() method allows the inputs to be provided in the form of an
	 * array.
	 *
	 * @return
	 */
	public double evaluateAt(double[] inputs);

	/**
	 * Returns the number of input variables expected by the underlying function.
	 *
	 * @return
	 */
	public int nVariables();
}

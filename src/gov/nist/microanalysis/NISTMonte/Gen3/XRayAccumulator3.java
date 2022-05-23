package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.CharacteristicXRay;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.XRay;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A simple acccumulator for recording the generated and emitted intensities on
 * a list of lines.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W M Ritchie
 * @version 1.0
 */
final public class XRayAccumulator3
   implements
   ActionListener {

   private class Accumulator {
      final XRayTransition mTransition;
      double mGenerated;
      double mTransmitted;

      private Accumulator(XRayTransition xrt) {
         mTransition = xrt;
         mGenerated = 0.0;
         mTransmitted = 0.0;
      }
   }

   private final Map<XRayTransition, Accumulator> mAccumulators = new HashMap<XRayTransition, Accumulator>();
   private int mElectronCount;
   private int mEventCount;
   private final double mScale;
   private final String mType;
   private final static double I_NORM = 1.0e-6 / (4.0 * Math.PI);

   /**
    * XRayAccumulator - Create an accumulator to record the sum of the generated
    * and transmitted x-ray intensity on the Set of XRayTransition or
    * AtomicShell objects in lines.
    * 
    * @param lines Collection&lt;XRayTransition&gt;
    * @param type
    * @param dose in amps
    */
   public XRayAccumulator3(Collection<XRayTransition> lines, String type, double dose) {
      for(XRayTransition xrt : lines) {
         final Accumulator acc = new Accumulator(xrt);
         mAccumulators.put(xrt, acc);
      }
      mScale = computeScale(dose);
      mType = type;
      clear();
   }

   /**
    * XRayAccumulator - Create an accumulator to record the sum of the generated
    * and transmitted x-ray intensity on the Set of XRayTransition or
    * AtomicShell objects in lines. AtomicShells are translated into the
    * strongest available XRayTransition.
    * 
    * @param xrtss Set&lt;XRayTransitionSet&gt;
    * @param type
    * @param dose in amps
    */
   public XRayAccumulator3(Set<XRayTransitionSet> xrtss, String type, double dose) {
      for(final XRayTransitionSet xrts : xrtss)
    	  for(final XRayTransition xrt : xrts.getTransitions())
    		  mAccumulators.put(xrt, new Accumulator(xrt));
      mScale = computeScale(dose);
      mType = type;
      clear();
   }

   /**
    * getTransitions - Get an immutable list of the transitions that are being
    * accumulated.
    * 
    * @return List&lt;XRayTransition&gt;
    */
   public List<XRayTransition> getTransitions() {
      ArrayList<XRayTransition> res = new ArrayList<XRayTransition>();
      for(XRayTransition xrt : mAccumulators.keySet())
         res.add(xrt);
      return Collections.unmodifiableList(res);
   }

   /**
    * clear - Reset the accumulator to zero.
    */
   public void clear() {
      for(final Accumulator acc : mAccumulators.values()) {
         acc.mGenerated = 0.0;
         acc.mTransmitted = 0.0;
      }
      mElectronCount = 0;
      mEventCount = 0;
   }

   /**
    * Invoked when an action occurs. Responds to XRayEventListener events.
    * 
    * @param ae ActionEvent from an XRayTransport objec
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      final Object src = ae.getSource();
      switch(ae.getID()) {
         case BaseXRayGeneration3.XRayGeneration: {
            assert src instanceof XRayTransport3;
            final XRayTransport3 tran = (XRayTransport3) src;
            for(int i = tran.getEventCount() - 1; i >= 0; --i) {
               final XRay tr = tran.getXRay(i);
               if(tr instanceof CharacteristicXRay) {
                  final XRayTransition xrt = ((CharacteristicXRay) tr).getTransition();
                  final Accumulator acc = mAccumulators.get(xrt);
                  if(acc != null) {
                     acc.mGenerated += tr.getGenerated();
                     acc.mTransmitted += tr.getIntensity();
                  }
               }
            }
            ++mEventCount;
         }
            break;
         case MonteCarloSS.TrajectoryStartEvent:
            ++mElectronCount;
            break;
      }
   }

   /**
    * getEmitted - Get the transmitted intensity for the specified transition in
    * x-rays per millistetradian of detector solid angle.
    * 
    * @param xrt
    * @return double
    */
   public double getEmitted(XRayTransition xrt) {
      final Accumulator acc = mAccumulators.get(xrt);
      return acc != null ? I_NORM * mScale * acc.mTransmitted / mElectronCount : 0.0;
   }

   /**
    * Compute probe dose correct scaling
    *
    * @param probeDose in amp*s
    * @return A scaling factor for the data
    */
   private static double computeScale(double probeDose) {
      return probeDose / PhysicalConstants.ElectronCharge;
   }

   /**
    * getSumEmitted - Returns the sum of the emitted intensity for all
    * transitions.
    * 
    * @return double
    */
   public double getSumEmitted() {
      double res = 0.0;
      for(final Accumulator acc : mAccumulators.values())
         res += acc.mTransmitted;
      return I_NORM * mScale * res / mElectronCount;
   }

   /**
    * getSumGenerated - Returns the sum of the generated intensity for all
    * transitons.
    * 
    * @return double
    */
   public double getSumGenerated() {
      double res = 0.0;
      for(final Accumulator acc : mAccumulators.values())
         res += acc.mGenerated;
      return I_NORM * mScale * res / mElectronCount;
   }

   /**
    * getGenerated - Get the generated intensity for the specified transition in
    * x-rays per millistetradian of detector solid angle.
    * 
    * @param xrt
    * @return double
    */
   public double getGenerated(XRayTransition xrt) {
      final Accumulator acc = mAccumulators.get(xrt);
      return acc != null ? I_NORM * mScale * acc.mGenerated / mElectronCount : 0.0;
   }

   public boolean contains(XRayTransition xrt) {
      return mAccumulators.get(xrt) != null;
   }

   /**
    * dump - Output the resulting accumulation to a PrintWriter in tab seperated
    * values suitable to import into a spreadsheet.
    * 
    * @param pw PrintWriter
    */
   public void dump(PrintWriter pw) {
      pw.println("Type:\t" + mType);
      pw.print("# of electrons\t");
      pw.println(mElectronCount);
      pw.print("# of scattering events\t");
      pw.println(mEventCount);
      pw.println("Transition\tGenerated (1/mSr)\tTransmitted (1/mSr)");
      for(final XRayTransition xrt : mAccumulators.keySet()) {
         pw.print(xrt.toString());
         pw.print('\t');
         pw.print(getGenerated(xrt));
         pw.print('\t');
         pw.print(getEmitted(xrt));
         pw.println();
      }
      pw.flush();
   }

   public String toHTML() {
      final StringBuffer sb = new StringBuffer();
      final NumberFormat nf1 = new HalfUpFormat("#,##0.0");
      sb.append("<table class=\"leftalign\">");
      sb.append("<tr><th>Transition</th><th>Energy<br/>(eV)</th><th>Generated<br/>1/msR</th><th>Emitted<br/>1/msR</th><th>&nbsp;&nbsp;Ratio&nbsp;&nbsp;<br/>&nbsp;&nbsp;(&#37;)&nbsp;&nbsp;</th></tr>");
      TreeMap<XRayTransition, Accumulator> sm = new TreeMap<>(mAccumulators);
      for(final XRayTransition xrt : sm.keySet()) {
         sb.append("<tr><th>");
         sb.append(xrt.toString());
         sb.append("</th><td>");
         try {
			sb.append(nf1.format(xrt.getEnergy_eV()));
		} catch (EPQException e) {
			sb.append("? eV");
		}
         sb.append("</th><td>");
         sb.append(nf1.format(getGenerated(xrt)));
         sb.append("</td><td>");
         sb.append(nf1.format(getEmitted(xrt)));
         sb.append("</td><td>");
         if(getGenerated(xrt) > 0.0) {
            sb.append(nf1.format((100.0 * getEmitted(xrt)) / getGenerated(xrt)));
            sb.append("&#37;</td></tr>");
         } else
            sb.append("--</td></tr>");
      }
      sb.append("</table>");
      return sb.toString();
   }

   private Accumulator find(XRayTransition xrt) {
	   return mAccumulators.get(xrt);
   }

   public String compareAsHTML(XRayAccumulator3 xra2) {
      final StringBuffer sb = new StringBuffer();
      final NumberFormat nf1 = new HalfUpFormat("0.0000");
      sb.append("<table class=\"leftalign\">");
      sb.append("<tr><th>Transition</th><th>&nbsp;&nbsp;Generated&nbsp;&nbsp;<br>(ratio)</th><th>&nbsp;&nbsp;Emitted&nbsp;&nbsp;<br>(ratio)</th></tr>");
      for(final Accumulator acc : mAccumulators.values()) {
         final Accumulator acc2 = xra2.find(acc.mTransition);
         if((acc2 != null) && (acc.mGenerated > 0.0)) {
            sb.append("<tr><th>");
            sb.append(acc.mTransition.toString());
            sb.append("</th><td>");
            sb.append(nf1.format((xra2.mScale * acc2.mGenerated / xra2.mElectronCount)
                  / (mScale * acc.mGenerated / mElectronCount)));
            sb.append("</td><td>");
            sb.append(nf1.format((xra2.mScale * acc2.mTransmitted / xra2.mElectronCount)
                  / (mScale * acc.mTransmitted / mElectronCount)));
            sb.append("</td></tr>");
         }
      }
      sb.append("</table>");
      return sb.toString();
   }

   @Override
   public String toString() {
      return mType;
   }
}

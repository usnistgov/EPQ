package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * A set of various different algorithms and databases for the x-ray transition
 * energy.
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

abstract public class TransitionEnergy
   extends AlgorithmClass {

   protected TransitionEnergy(String name, String reference) {
      super("Transition Energy", name, reference);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the EdgeEnergy class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * compute - Compute the transition energy for the specified transition.
    * 
    * @param xrt XRayTransition
    * @return double in Joules
    * @throws EPQException
    */
   abstract public double compute(XRayTransition xrt)
         throws EPQException;

   /**
    * isSupported - Can this algorithm compute the transition energy for the
    * specified transition?
    * 
    * @param xrt XRayTransition
    * @return boolean
    */
   abstract public boolean isSupported(XRayTransition xrt);

   /**
    * <p>
    * Deslattes2005TransitionEnergy is based on the tabulation of x-ray
    * transition energies from
    * <code>http://units.nist.gov/PhysRefData/XrayTrans/Html/search.html</code>.
    * This tabulation includes elements from Ne to Fm (but nothing below Z=10).
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   public static class Deslattes2005TransitionEnergy
      extends TransitionEnergy {

      private static transient TreeMap<XRayTransition, Double> mEnergies;

      private double parseNumber(String text) {
         return text.length() > 0 ? Double.parseDouble(text) : Double.NaN;
      }

      private TreeMap<XRayTransition, Double> loadEnergies() {
         final TreeMap<XRayTransition, Double> res = new TreeMap<XRayTransition, Double>();
         try {
            try (final Reader isr = new InputStreamReader(getClass().getResourceAsStream("deslattes.csv"), "US-ASCII")) {
               final BufferedReader br = new BufferedReader(isr);
               String line = br.readLine();
               while(line != null) {
                  line = line.trim();
                  if(!line.startsWith("#")) {
                     final String items[] = line.split("\t");
                     final Element elm = Element.byName(items[0]);
                     assert !elm.equals(Element.None);
                     final String xrtName = items[2].trim();
                     if(!xrtName.endsWith("edge")) {
                        AtomicShell dest, src;
                        if(xrtName.charAt(0) == 'K') {
                           dest = new AtomicShell(elm, AtomicShell.K);
                           src = AtomicShell.parseString(elm.toAbbrev() + " " + xrtName.substring(1, 3));
                        } else {
                           dest = AtomicShell.parseString(elm.toAbbrev() + " " + xrtName.substring(0, 2));
                           src = AtomicShell.parseString(elm.toAbbrev() + " " + xrtName.substring(2, 4));
                        }
                        // assert dest.exists() : line + " dest: " + dest;
                        // assert src.exists() : line + " src: "+src;
                        final XRayTransition xrt = new XRayTransition(elm, src.getShell(), dest.getShell());
                        // assert xrt.exists();
                        final double theory = items.length > 3 ? parseNumber(items[3]) : Double.NaN;
                        final double direct = items.length > 5 ? parseNumber(items[5]) : Double.NaN;
                        final double combined = items.length > 7 ? parseNumber(items[7]) : Double.NaN;
                        final double vapor = items.length > 9 ? parseNumber(items[9]) : Double.NaN;
                        double e = direct;
                        if(Double.isNaN(e))
                           e = combined;
                        if(Double.isNaN(e))
                           e = vapor;
                        if(Double.isNaN(e))
                           e = theory;
                        if(!Double.isNaN(e))
                           res.put(xrt, Double.valueOf(ToSI.eV(e)));
                     }
                  }
                  line = br.readLine();
               }
               for(final Map.Entry<XRayTransition, Double> me : res.entrySet())
                  if(me.getKey().getElement().equals(Element.O))
                     System.out.println(me.getKey() + "\t" + me.getValue());
            }
         }
         catch(final Exception e) {
            e.printStackTrace();
         }
         return res;
      }

      public Deslattes2005TransitionEnergy() {
         super("Deslattes et al (2005)", "R.D. Deslattes, E.G. Kessler Jr., P. Indelicato, L. de Billy, E. Lindroth, "
               + "J. Anton, J.S. Coursey, D.J. Schwab, C. Chang, R. Sukumar, K. Olsen, and "
               + "R.A. Dragoset (2005), X-ray Transition Energies (version 1.2). [Online]"
               + "vailable: http://physics.nist.gov/XrayTrans [2009, May 22]. "
               + "National Institute of Standards and Technology, Gaithersburg, MD");
      }

      private final Double getXRT(XRayTransition xrt) {
         if(mEnergies == null)
            synchronized(this) {
               if(mEnergies == null)
                  mEnergies = loadEnergies();
            }
         return mEnergies.get(xrt);
      }

      @Override
      public double compute(XRayTransition xrt)
            throws EPQException {
         final Double res = getXRT(xrt);
         if(res == null)
            throw new EPQException("Line energies are not available for " + xrt.toString() + ".");
         return res.doubleValue();
      }

      @Override
      public boolean isSupported(XRayTransition xrt) {
         return getXRT(xrt) != null;
      }

      /**
       * R.D. Deslattes, E.G. Kessler Jr., P. Indelicato, L. de Billy, E.
       * Lindroth, J. Anton, J.S. Coursey, D.J. Schwab, C. Chang, R. Sukumar, K.
       * Olsen, and R.A. Dragoset (2005), X-ray Transition Energies (version
       * 1.2). [Online] Available: http://physics.nist.gov/XrayTrans [2009, May
       * 22]. National Institute of Standards and Technology, Gaithersburg, MD.
       */
   };

   public static final TransitionEnergy DESLATTES = new Deslattes2005TransitionEnergy();

   /**
    * Chandler2005 - Computes the transition energies from the edge energies
    * provided by "Chantler, C.T., Olsen, K., Dragoset, R.A., Kishore, A.R.,
    * Kotochigova, S.A., and Zucker, D.S. (2005), X-Ray Form Factor, Attenuation
    * and Scattering Tables (version 2.1). [Online] Available:
    * http://physics.nist.gov/ffast [10-Mar-2005]. National Institute of
    * Standards and Technology, Gaithersburg, MD. Originally published as
    * Chantler, C.T., J. Phys. Chem. Ref. Data 29(4), 597-1048 (2000); and
    * Chantler, C.T., J. Phys. Chem. Ref. Data 24, 71-643 (1995)." Supports
    * elements H to U and transitions to and from shells K to O5, P1 to P3.
    */
   public static class Chantler2005TransitionEnergy
      extends TransitionEnergy {
      public Chantler2005TransitionEnergy() {
         super("NIST-Chantler 2005", "Derived from edge energies taken from http://physics.nist.gov/ffast");
      }

      @Override
      public double compute(XRayTransition xrt)
            throws EPQException {
         final AtomicShell dest = xrt.getDestination();
         AtomicShell src = xrt.getSource();
         switch(src.getElement().getAtomicNumber()) {
         // LIII and LII don't exist
            case Element.elmLi:
            case Element.elmBe:
               if(src.getShell() == AtomicShell.LIII)
                  src = new AtomicShell(src.getElement(), AtomicShell.LI);
               if(src.getShell() == AtomicShell.LII)
                  src = new AtomicShell(src.getElement(), AtomicShell.LI);
               break;
            // LIII doesn't exist
            case Element.elmB:
            case Element.elmC:
               if(src.getShell() == AtomicShell.LIII)
                  src = new AtomicShell(src.getElement(), AtomicShell.LII);
               break;
         }
         assert EdgeEnergy.Chantler2005.compute(dest) >= EdgeEnergy.Chantler2005.compute(src);
         final double res = EdgeEnergy.Chantler2005.compute(dest) - EdgeEnergy.Chantler2005.compute(src);
         assert (res == 0.0) || ((res > ToSI.eV(0.1)) && (res < ToSI.eV(1.0e6)));
         return res;
      }

      @Override
      public boolean isSupported(XRayTransition xrt) {
         return EdgeEnergy.Chantler2005.isSupported(xrt.getSource())
               && EdgeEnergy.Chantler2005.isSupported(xrt.getDestination());
      }
   }

   public static final TransitionEnergy Chantler2005 = new Chantler2005TransitionEnergy();

   /**
    * DTSA - Extracts the transition energy from the DTSA database.
    */
   public static class DTSATransitionEnergy
      extends TransitionEnergy {
      public DTSATransitionEnergy() {
         super("DTSA", "From DTSA at http://www.cstl.nist.gov/div837/Division/outputs/DTSA/DTSA.htm");
      }

      private transient double[][] mEnergy; // Stored in Joules

      @Override
      public double compute(XRayTransition xrt)
            throws EPQException {
         // The static table mEnergy is loaded on demand from LineEnergies.csv
         if(mEnergy == null)
            loadEnergyTable();
         if(!XRayTransition.isWellKnown(xrt.getTransitionIndex()))
            throw new EPQException("Energy for the transition " + xrt.toString() + " is not available.");
         final Element el = xrt.getElement();
         final int anm1 = el.getAtomicNumber() - 1;
         if((anm1 < (Element.elmH - 1)) || (anm1 >= mEnergy.length) || (mEnergy[anm1] == null))
            throw new EPQException("Line energies are not available for " + el.toAbbrev() + ".");
         return xrt.getTransitionIndex() < mEnergy[anm1].length ? mEnergy[anm1][xrt.getTransitionIndex()] : 0.0;
      }

      @Override
      public boolean isSupported(XRayTransition xrt) {
         if(mEnergy == null)
            loadEnergyTable();
         final int z = xrt.getElement().getAtomicNumber();
         return XRayTransition.isWellKnown(xrt.getTransitionIndex()) && (z >= Element.elmH) && ((z - 1) < mEnergy.length)
               && (mEnergy[z - 1] != null);
      }

      private synchronized void loadEnergyTable() {
         try {
            if(mEnergy == null) {
               mEnergy = (new CSVReader.ResourceReader("LineEnergies.csv", true)).getResource(TransitionEnergy.class);
               for(int r = 0; r < mEnergy.length; ++r)
                  if((mEnergy[r] != null) && (mEnergy[r].length > 0))
                     for(int c = 0; c < mEnergy[r].length; ++c)
                        mEnergy[r][c] = ToSI.eV(mEnergy[r][c]);
            }
         }
         catch(final Exception ex) {
            throw new EPQFatalException("Fatal error while attempting to load the line energies data file.");
         }
      }
   }

   public static final TransitionEnergy DTSA = new DTSATransitionEnergy();

   public static class SuperSetTransitionEnergy
      extends TransitionEnergy {

      private final TransitionEnergy mPrimary;
      private final TransitionEnergy mSecondary;

      public SuperSetTransitionEnergy(TransitionEnergy primary, TransitionEnergy secondary) {
         super("Composite", primary.getName() + "(w. " + secondary.getName() + ")");
         mPrimary = primary;
         mSecondary = secondary;
      }

      public SuperSetTransitionEnergy() {
         this(Chantler2005, DTSA);
      }

      @Override
      public double compute(XRayTransition xrt)
            throws EPQException {
         if(mPrimary.isSupported(xrt))
            return mPrimary.compute(xrt);
         else
            return mSecondary.compute(xrt);
      }

      @Override
      public boolean isSupported(XRayTransition xrt) {
         return mPrimary.isSupported(xrt) || mSecondary.isSupported(xrt);
      }
   }

   public static final TransitionEnergy SuperSet = new SuperSetTransitionEnergy(Chantler2005, DTSA);
   public static final TransitionEnergy DeslattesSuperSet = new SuperSetTransitionEnergy(DESLATTES, DTSA);

   static private final AlgorithmClass[] mAllImplementations = {
      TransitionEnergy.Chantler2005,
      TransitionEnergy.DTSA,
      TransitionEnergy.SuperSet,
      TransitionEnergy.DeslattesSuperSet
   };
}

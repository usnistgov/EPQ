package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.AbsoluteIonizationCrossSection;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EdgeEnergy;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.TransitionProbabilities;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.Utility.Math2;

/**
 * A class which implements the physics for characteristic x-ray generation. The
 * class implements the ActionListener interface which is used to listen for
 * scattering or other events occuring on an attached instance of MonteCarloSS.
 * It calculates the (fractional) number of photons emitted during the last
 * trajectory path between the previous scatter point and this one. This class
 * only implements generation. You must use {@link XRayTransport3} to transport
 * the x-ray event to either the detector
 * 
 * @author nicholas
 */
final public class CharacteristicXRayGeneration3
   extends BaseXRayGeneration3
   implements ActionListener {

   /**
    * Use this static method instead of the constructor to create instances of
    * this class and initialize it with an instance of the MonteCarloSS class
    * and add an ActionListener to the MonteCarloSS instance.
    * 
    * @param mcss
    * @return CharacteristicXRayGeneration
    */
   public static CharacteristicXRayGeneration3 create(MonteCarloSS mcss) {
      final CharacteristicXRayGeneration3 res = new CharacteristicXRayGeneration3();
      res.initialize(mcss);
      mcss.addActionListener(res);
      return res;
   }

   protected CharacteristicXRayGeneration3() {
      super("Characteristic", "Default");
   }

   private static class XRayData
      implements Comparable<XRayData> {
      private final XRayTransition mTransition;
      private final double mEnergy;
      private final double mProbability;

      private XRayData(XRayTransition xrt, double prob)
            throws EPQException {
         mTransition = xrt;
         mEnergy = xrt.getEnergy();
         if(mEnergy == 0.0)
            throw new EPQException("Trying to create a transition with zero energy. " + mTransition.toString());
         mProbability = prob;
      }

      @Override
      public int compareTo(XRayData xrd) {
         return mTransition.compareTo(xrd.mTransition);
      }
   }

   private transient boolean mInitialized = false;
   private transient AbsoluteIonizationCrossSection mICX = null;
   private transient TransitionProbabilities mTP = null;

   private transient TreeMap<AtomicShell, TreeSet<XRayData>> mData = null;
   private transient int mTrajCount = 0;
   private transient Random mRandom = new Random();

   private final double mMinWeight = 0.001;
   private final int mMaxTrajectories = Integer.MAX_VALUE;

   public void initialize()
         throws EPQException {
      if(!mInitialized) {
         // Initialize the algorithms...
         mICX = (AbsoluteIonizationCrossSection) getAlgorithm(AbsoluteIonizationCrossSection.class);
         mTP = (TransitionProbabilities) getAlgorithm(TransitionProbabilities.class);
         final Set<Element> elms = mMonte.getElementSet();
         final Set<AtomicShell> ionizedShells = new TreeSet<AtomicShell>();
         // Determine all the ionized shells
         for(final Element elm : elms)
            for(int sh = AtomicShell.K; sh <= AtomicShell.MV; ++sh) {
               final double ee = AtomicShell.getEdgeEnergy(elm, sh);
               if((ee > 0.0) && (ee < mMonte.getBeamEnergy()))
                  ionizedShells.add(new AtomicShell(elm, sh));
            }
         mData = new TreeMap<AtomicShell, TreeSet<XRayData>>();
         // Determine which transitions result from which ionizations
         { // Associate transitions and weights with shells
            for(final AtomicShell shell : ionizedShells) {
               final TreeSet<XRayData> xrd = new TreeSet<XRayData>();
               for(final Map.Entry<XRayTransition, Double> me : mTP.getTransitions(shell, mMinWeight).entrySet())
                  try {
                     if(me.getKey().getEnergy() > 0.0)
                        xrd.add(new XRayData(me.getKey(), me.getValue().doubleValue()));
                  }
                  catch(final EPQException e) {
                     System.err.print("Unable to simulate " + me.getKey() + "\n");
                  }
               if(xrd.size() > 0)
                  mData.put(shell, xrd);
            }
         }
         mInitialized = true;
      }
   }

   /**
    * Returns an array of all XRayTransitions that can potentially be produced.
    * 
    * @return XRayTranstion[]
    * @throws EPQException
    */
   public XRayTransition[] getTransitions()
         throws EPQException {
      if(!mInitialized)
         initialize();
      final TreeSet<XRayTransition> res = new TreeSet<XRayTransition>();
      for(final TreeSet<XRayData> shellData : mData.values())
         for(final XRayData xrd : shellData)
            res.add(xrd.mTransition);
      return res.toArray(new XRayTransition[res.size()]);
   }

   /**
    * Handles x-ray events by computing the amount of characteristic x-rays
    * generated.
    * 
    * @param ae
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      if(mTrajCount > mMaxTrajectories)
         return;
      assert (ae.getSource() instanceof MonteCarloSS);
      assert (mMonte == (MonteCarloSS) ae.getSource());
      reset();
      switch(ae.getID()) {
         case MonteCarloSS.FirstTrajectoryEvent:
            try {
               initialize();
            }
            catch(final EPQException e1) {
               e1.printStackTrace();
            }
            ++mTrajCount;
            fireXRayListeners(ae.getID());
            break;
         case MonteCarloSS.NonScatterEvent:
         case MonteCarloSS.ScatterEvent: {
            assert mInitialized;
            // Get the index associated with this material
            final Electron e = mMonte.getElectron();
            final MonteCarloSS.RegionBase reg = e.getCurrentRegion();
            final Material mat = reg.getMaterial();
            /*
             * The way this model generates x-rays in this model is a little
             * screwy but much more efficient than the natural alternative. In
             * reality, x-rays are generated very infrequently
             * (one-in-a-thousand trajectories or some such). However it would
             * be computationally very in efficient to actually generate x-rays
             * this infrequently. Instead the model calculates probabilities of
             * x-rays being generated and assigns the probability to each line
             * segment. The probability is proportional to the inelastic
             * cross-section and the path length. Rather than always assign the
             * probability to the scattering point, the probability is assigned
             * to a randomized point somewhere between the start of this
             * particular step and the end.
             */
            final double pos[] = Math2.pointBetween(e.getPrevPosition(), e.getPosition(), mRandom.nextDouble());
            final double stepLen = e.stepLength();
            final double energy = e.getEnergy();
            if(mData.size() > 0) {
               for(final Map.Entry<AtomicShell, TreeSet<XRayData>> me : mData.entrySet()) {
                  final AtomicShell shell = me.getKey();
                  if(energy > shell.getEdgeEnergy()) {
                     final double density = mat.atomsPerCubicMeter(shell.getElement());
                     if(density > 0.0) {
                        final double iz = mICX.computeShell(shell, energy) * stepLen * density;
                        assert !Double.isInfinite(iz);
                        if(iz > 0.0)
                           for(final XRayData xrd : me.getValue())
                              addCharXRay(pos, xrd.mEnergy, iz * xrd.mProbability, iz * xrd.mProbability, xrd.mTransition);
                     }
                  }
               }
               fireXRayListeners();
            }
            break;
         }
         case MonteCarloSS.BeamEnergyChanged:
            try {
               mInitialized = false;
               initialize();
            }
            catch(final EPQException e) {
               e.printStackTrace();
            }
            fireXRayListeners(ae.getID());
            break;
         default:
            fireXRayListeners(ae.getID());
            break;
      }
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(AbsoluteIonizationCrossSection.class, AbsoluteIonizationCrossSection.BoteSalvat2008);
      addDefaultAlgorithm(EdgeEnergy.class, EdgeEnergy.Default);
      addDefaultAlgorithm(TransitionProbabilities.class, TransitionProbabilities.Default);
   }

}

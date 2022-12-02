package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.Collections;
import java.util.LinkedHashSet;

// import java.util.*;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.IMaterialScatterModel;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements the IMaterialScatterModel interface for a MONSEL-style secondary
 * electron generating material.
 * </p>
 * <p>
 * A scatter model such as this one must be defined for each material in the
 * sample. The scatter model consists of 3 parts: (1) A list of scattering
 * mechanisms (e.g., Mott elastic scattering, Moller SE production, Plasmon SE
 * production,...) that operate in the material. These scattering mechanisms may
 * discontinuously (i.e., at a scattering event) change the primary electron
 * energy and direction and they may create secondary electrons. Mechanisms are
 * added to the list using the addScatterMechanism() method. (2) A single
 * barrier scattering function, specified using the setBarrierSM() method. This
 * method models scattering at a material interface. (3) A single continuous
 * slowing down function, specified using the setCSD() method. This method
 * determines the energy loss of the primary electron within the material.
 * MONSEL_MaterialScatterModel class combines the various mechanisms to
 * determine overall scattering behavior, including free path, secondary
 * generation, etc.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class MONSEL_MaterialScatterModel implements Cloneable, IMaterialScatterModel {

   private final SEmaterial mat; // The material for which this is the scatter
   // model

   private double minEforTracking;

   // Following private variables are associated with scattering
   private double cached_eK = -1.; // Initialize to an impossible eK.

   private double[] cached_cumulativeScatterRate = null;

   private double totalScatterRate;

   private ScatterMechanism[] scatterArray = null;

   private int nscattermech;

   /*
    * The continuous slowing down algorithm to use for this material
    */
   private SlowingDownAlg csd = null;

   /*
    * The barrier scattering mechanism to use for this material
    */
   private BarrierScatterMechanism barrierSM = null;

   /*
    * A set of scatter mechanisms at work in this material. Use of a set type
    * instead of a list type is meant to discourage duplicate entries. (Each
    * scatter mechanism should appear only once. Use of a set collection
    * prevents the same object from being added multiple times. Note that
    * duplicates are still possible inasmuch as we must rely upon the user not
    * to add distinct objects that are nevertheless instances of the same
    * underlying mechanism.) LinkedHashSet is used rather than HashSet or
    * TreeSet because it guarantees iteration order to be the same as order of
    * insertion and because of better performance in iterations.
    */

   /*
    * TODO: Maintaining a separate LinkedHashSet and scatterArray is cumbersome.
    * I should probably convert this to an ArrayList (so I can use the
    * .get(index) method) and get rid of the scatterArray.
    */
   private final LinkedHashSet<ScatterMechanism> scatterSet = new LinkedHashSet<ScatterMechanism>();

   // private ArrayList<ScatterMechanism> scatterSet = new ArrayList();

   /**
    * <p>
    * MONSEL_MaterialScatterModel: This constructor makes a private copy of the
    * supplied SEmaterial. Changes to material properties after instantiation of
    * the MONSEL_MaterialScatterModel will not be reflected in the model.
    * </p>
    * <p>
    * The MONSEL_MaterialScatterModel is constructed with default continuous
    * slowing down and barrier scattering mechanisms that may be overridden
    * using the setters below. The default slowing down algorithm is ZeroCSD(),
    * i.e., no continuous slowing down. The default barrier scattering mechanism
    * is ExpQMBarrierSM(mat)--i.e., barrier scattering with barrier height equal
    * to mat.getEnergyCBbottom() and barrier width assumed large compared to the
    * electron wavelength. The default minimum energy for tracking is the
    * surface barrier height (difference between vacuum and conduction band
    * minimum).
    * </p>
    */
   public MONSEL_MaterialScatterModel(SEmaterial mat) {
      this.mat = mat.clone();
      csd = new ZeroCSD();
      barrierSM = new ExpQMBarrierSM(this.mat);
      /*
       * By default the minimum energy for tracking is negative infinity in
       * vacuum and the bottom of the conduction band in non-vacuum. This means
       * we never drop an electron from the simulation when it is in vacuum, but
       * rather wait for it to either enter a material or be detected.
       */
      if (mat.getElementCount() == 0)
         minEforTracking = Double.NEGATIVE_INFINITY;
      else {
         /*
          * Default to barrier height or 0. whichever is larger. (Barrier can be
          * negative in some negative electronic affinity materials, but in this
          * case we assume a negative kinetic energy, which means energy less
          * than the conduction band bottom, refers to a trapped state.
          */
         minEforTracking = -mat.getEnergyCBbottom();
         if (minEforTracking < 0.)
            minEforTracking = 0.;
      }
   }

   /**
    * Material getMaterial: Returns a clone of the material that is being
    * modeled.
    */
   @Override
   public Material getMaterial() {
      return mat.clone();
   }

   private void setCache(Electron pe) {
      // Remember kinetic energy for which the cache was created
      cached_eK = pe.getEnergy();
      /*
       * Algorithm: 1. Get scatter rate (1/(mean free path) for each mechanism
       * active in this material 2. From this, determine the total scatter rate.
       * 3. Cache the scatter rates for later use.
       */
      totalScatterRate = 0.;
      if (scatterArray == null)
         return;
      int index = 0;
      for (final ScatterMechanism mech : scatterArray) {
         totalScatterRate += mech.scatterRate(pe);
         // Cache cumulative scatterRate
         cached_cumulativeScatterRate[index++] = totalScatterRate;
      }
   }

   @Override
   public double randomMeanPathLength(Electron pe) {
      /*
       * Algorithm: 1. Randomly select a free path from the Poisson distribution
       * that has an mfp corresponding to 1 / total scatter rate. Optimization:
       * We cache the cumulative scatter rate for later use by
       * randomScatteringAngle()
       */
      /*
       * TODO Should I always setCache here, or should I do if eK != cached_eK?
       */
      setCache(pe);
      /*
       * Return at most the chamber diameter for the free path. This is long
       * enough to guarantee the electron hits the chamber wall from any
       * starting position, provided it doesn't hit something else first. A long
       * path will most likely be shortened to a boundary by a routine that
       * computes the boundary distance as a multiple of this free path. If the
       * free path is too long (or infinite) the multiple will be too small,
       * leading to numerical imprecision.
       */
      final double maxFreePath = 2. * MonteCarloSS.ChamberRadius;
      if (totalScatterRate != 0.) {
         final double freepath = -Math.log(Math2.rgen.nextDouble()) / totalScatterRate;
         return freepath > maxFreePath ? maxFreePath : freepath;
      }
      /*
       * I still on very rare occasions get situations where an electron gets
       * stuck in an infinite loop. E.g., after several days and millions of
       * electrons I had one that struck within 5.e-8 nm of a corner. To avoid
       * having these lock up an otherwise good simulation, just drop the
       * electron.
       */
      if (pe.getStepCount() > 1000000)
         pe.setTrajectoryComplete(true);
      return maxFreePath;
   }

   @Override
   public Electron scatter(Electron pe) {
      /*
       * 1. Determine which of the scatter mechanisms active in this material
       * caused this scatter event. (Randomly choose one, with probability
       * weighted by scatter rate.) 2. Obtain a scattering angle appropriate to
       * this mechanism
       */

      final double eK = pe.getPreviousEnergy();
      if (eK != cached_eK) {
         /*
          * scattering is based on energy at conclusion of previous step, but
          * scatterRate methods used to set the cache use the current energy.
          * This is inelegant, but for now I hold my nose and ...
          */
         final double eKsaved = pe.getEnergy();
         pe.setEnergy(eK);
         setCache(pe);
         pe.setEnergy(eKsaved);
      }

      /*
       * There is the possibility (e.g., vacuum) that no scatter mechanisms are
       * assigned to this material. In this case the totalScatterRate will be 0.
       * In this event we do nothing.
       */
      if (totalScatterRate == 0.)
         return null;

      // Find the scatter mechanism that produced this scattering event
      // Generate a random # between 0 and total cumulative scatter rate

      final double r = Math2.rgen.nextDouble() * totalScatterRate;
      int index = 0; // Index is first index

      // Increment index and mechanism until cumulative scatter rate exceeds r
      while (cached_cumulativeScatterRate[index] < r)
         index++;

      return scatterArray[index].scatter(pe);
   }

   @Override
   public Electron barrierScatter(Electron pe, RegionBase nextRegion) {
      return barrierSM.barrierScatter(pe, nextRegion);
   }

   public void setBarrierSM(BarrierScatterMechanism barrierSM) {
      /*
       * TODO This is not good. I'm just using a reference to the provided
       * barrierSM. The user could easily have initialized it for the wrong
       * material. How about I clone it instead and initialize it for this.mat.
       */
      this.barrierSM = barrierSM;
   }

   public BarrierScatterMechanism getBarrierSM() {
      return barrierSM;
   }

   /**
    * Specifies the slowing down algorithm operative in this material. The
    * default is ZeroCSD().
    *
    * @param csd
    *           - the slowing down algorithm to use
    */
   public void setCSD(SlowingDownAlg csd) {
      this.csd = csd;
      // TODO I should clone it first. User is likely to use this in more than
      // one place.
      this.csd.setMaterial(mat);
   }

   public SlowingDownAlg getCSD() {
      return csd;
   }

   @Override
   public double calculateEnergyLoss(double len, Electron pe) {
      return csd.compute(len, pe);
   }

   /**
    * Adds a scattering mechanism to the list of those operative in this
    * material.
    */
   public boolean addScatterMechanism(ScatterMechanism mech) throws CloneNotSupportedException {
      // final ScatterMechanism mechCopy = mech.clone(); // Make a copy of the
      // scatter
      // mechanism
      // mechCopy.setMaterial(mat); // Initialize the copy for this material
      // if(scatterSet.add(mechCopy)) {
      if (scatterSet.add(mech)) {
         scatterArray = scatterSet.toArray(new ScatterMechanism[1]);
         nscattermech = scatterArray.length;
         cached_cumulativeScatterRate = new double[nscattermech];
         cached_eK = -1.;// Force recompute of cache on next call
         return true;
      }
      return false;
   }

   /**
    * Returns an unmodifiable set of scattering mechanisms assigned to this
    * material.
    */
   public LinkedHashSet<ScatterMechanism> getScatterSet() {
      return (LinkedHashSet<ScatterMechanism>) Collections.unmodifiableSet(scatterSet);
      /*
       * TODO This may not be good enough. A caller can get the unmodifiable set
       * (a set of references to scattermechanisms) and call the init routine on
       * one or more of its elements with a different material than the one with
       * which it was originally initialized. This will change its internal
       * state. If I fix this I may not also need to override clone();
       */
   }

   /**
    * Removes, if present, the specified scattering mechanism from the list of
    * those operative in this material.
    */
   public boolean removeScatterMechanism(ScatterMechanism mech) {
      boolean removed = scatterSet.remove(mech);
      scatterArray = scatterSet.toArray(new ScatterMechanism[1]);
      nscattermech = scatterArray.length;
      cached_cumulativeScatterRate = new double[nscattermech];
      cached_eK = -1.; // Force recompute of cache on next call
      return removed;
   }

   @Override
   public double getMinEforTracking() {
      return minEforTracking;
   }

   /**
    * Sets the minimum energy for tracking electrons in this material scatter
    * model. By default, in solids this energy is set to
    * -mat.getEnergyCBbottom(), that is, such that its energy relative to the
    * vacuum is 0. Electrons with less energy than this cannot escape the sample
    * to an external detector. For vacuum the default is to set this energy to
    * -infinity. This means electrons in vacuum are never dropped from the
    * simulation; instead we wait for them to either enter a material or be
    * detected.
    *
    * @param minEforTracking
    * @see gov.nist.microanalysis.NISTMonte.IMaterialScatterModel#setMinEforTracking(double)
    */
   @Override
   public void setMinEforTracking(double minEforTracking) {
      this.minEforTracking = minEforTracking;
   }

}

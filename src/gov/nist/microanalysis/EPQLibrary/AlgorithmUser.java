package gov.nist.microanalysis.EPQLibrary;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * <p>
 * The AlgorithmUser class facilitates the dynamic interchange of client
 * algorithms. Algorithm using classes should specify which client algorithms
 * they use by default. This class provides a mechanism by which these default
 * client algorithms can be overidden. If an end user wishes to use a different
 * algorithm to implement a specific type of algorithm the user can supply a
 * global override Strategy. Now instead of using the default client algorithm,
 * all AlgorithmUser classes will use the ones specified in the global override
 * Strategy.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
abstract public class AlgorithmUser {

   /**
    * Each algorithm class should specify the default algorithms it uses in the
    * InitializeDefaultStrategy abstract method using setDefaultAlgorithm.
    */
   private Strategy mLocalOverride = null;
   /**
    * While algorithms are specified on a AlgorithmUser by user basis, overrides
    * are done on a global basis.
    */
   static private Strategy mGlobalOverride = null;

   private static EdgeEnergy sDefaultEdgeEnergy = null;
   private static TransitionEnergy sDefaultTransitionEnergy = null;
   private static MassAbsorptionCoefficient sDefaultMAC = null;
   private static FluorescenceYieldMean sDefaultFluorescenceYieldMean = null;
   private static FluorescenceYield sDefaultFluorescenceYield = null;
   private static BetheElectronEnergyLoss sDefaultBetheEnergyLoss = null;
   private static BremsstrahlungAngularDistribution sDefaultAngularDistribution = null;
   private static CorrectionAlgorithm sDefaultCorrectionAlgorithm = null;

   protected AlgorithmUser() {
      super();
      initializeDefaultStrategy();
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return EdgeEnergy
    */
   public static EdgeEnergy getDefaultEdgeEnergy() {
      return sDefaultEdgeEnergy == null ? EdgeEnergy.SuperSet : sDefaultEdgeEnergy;
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return CorrectionAlgorithm
    */
   public static CorrectionAlgorithm getDefaultCorrectionAlgorithm() {
      return sDefaultCorrectionAlgorithm == null ? CorrectionAlgorithm.XPPExtended : sDefaultCorrectionAlgorithm;
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return TransitionEnergy
    */
   public static TransitionEnergy getDefaultTransitionEnergy() {
      return sDefaultTransitionEnergy == null ? TransitionEnergy.SuperSet : sDefaultTransitionEnergy;
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return MassAbsorptionCoefficient
    */
   public static MassAbsorptionCoefficient getDefaultMAC() {
      return sDefaultMAC == null ? MassAbsorptionCoefficient.Chantler2005 : sDefaultMAC;
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return FluorescenceYieldMean
    */
   public static FluorescenceYieldMean getDefaultFluorescenceYieldMean() {
      return sDefaultFluorescenceYieldMean == null ? FluorescenceYieldMean.DefaultMean : sDefaultFluorescenceYieldMean;
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return FluorescenceYield
    */
   public static FluorescenceYield getDefaultFluorescenceYield() {
      return sDefaultFluorescenceYield == null ? FluorescenceYield.DefaultShell : sDefaultFluorescenceYield;
   }

   /**
    * This represents an optimized ways to access this common algorithm. It is
    * updated whenever the global strategy is updated.
    * 
    * @return BetheElectronEnergyLoss
    */
   public static BetheElectronEnergyLoss getDefaultBetheEnergyLoss() {
      return sDefaultBetheEnergyLoss == null ? BetheElectronEnergyLoss.JoyLuo1989 : sDefaultBetheEnergyLoss;
   }

   public static BremsstrahlungAngularDistribution getDefaultAngularDistribution() {
      return sDefaultAngularDistribution == null ? BremsstrahlungAngularDistribution.Acosta2002L : sDefaultAngularDistribution;
   }

   /**
    * getActiveStrategy - Returns a Strategy object containing a complete set of
    * the the AlgorithmClass objects on which this AlgorithmUser object depends.
    * 
    * @return Strategy
    */
   public Strategy getActiveStrategy() {
      final Strategy res = mLocalOverride != null ? (Strategy) mLocalOverride.clone() : new Strategy();
      if(mGlobalOverride != null)
         res.apply(mGlobalOverride);
      return res;
   }

   /**
    * getAlgorithm - Returns the specific algorithm associated with the base
    * class provided as an argument.
    * 
    * @param cls
    * @return AlgorithmClass
    */
   public AlgorithmClass getAlgorithm(Class<?> cls) {
      AlgorithmClass res = null;
      if(mGlobalOverride != null)
         res = mGlobalOverride.getAlgorithm(cls);
      if((res == null) && (mLocalOverride != null))
         res = mLocalOverride.getAlgorithm(cls);
      if(res == null) {
         if(cls == CorrectionAlgorithm.class)
            res = getDefaultCorrectionAlgorithm();
         else if(cls == BetheElectronEnergyLoss.class)
            res = getDefaultBetheEnergyLoss();
         else if(cls == BremsstrahlungAngularDistribution.class)
            res = getDefaultAngularDistribution();
         else if(cls == EdgeEnergy.class)
            res = getDefaultEdgeEnergy();
         else if(cls == FluorescenceYield.class)
            res = getDefaultFluorescenceYield();
         else if(cls == FluorescenceYieldMean.class)
            res = getDefaultFluorescenceYieldMean();
         else if(cls == MassAbsorptionCoefficient.class)
            res = getDefaultMAC();
         else if(cls == TransitionEnergy.class)
            res = getDefaultTransitionEnergy();
      }
      return res;
   }

   /**
    * getAlgorithm - Returns the specific algorithm associated with the base
    * class provided as an argument.
    * 
    * @param clsName
    * @return AlgorithmClass
    */
   public AlgorithmClass getAlgorithm(String clsName) {
      AlgorithmClass res = null;
      if(mGlobalOverride != null)
         res = mGlobalOverride.getAlgorithm(clsName);
      if((res == null) && (mLocalOverride != null))
         res = mLocalOverride.getAlgorithm(clsName);
      return res;
   }

   /**
    * applyGlobalOverride - Apply the specified Strategy as a gloabal overrride
    * for all AlgorithmClass objects.
    * 
    * @param strat
    */
   static public void applyGlobalOverride(Strategy strat) {
      if(strat != null) {
         mGlobalOverride = strat;
         sDefaultTransitionEnergy = (TransitionEnergy) strat.getAlgorithm(TransitionEnergy.class);
         sDefaultEdgeEnergy = (EdgeEnergy) strat.getAlgorithm(EdgeEnergy.class);
         sDefaultMAC = (MassAbsorptionCoefficient) strat.getAlgorithm(MassAbsorptionCoefficient.class);
         sDefaultFluorescenceYield = (FluorescenceYield) strat.getAlgorithm(FluorescenceYield.class);
         sDefaultFluorescenceYieldMean = (FluorescenceYieldMean) strat.getAlgorithm(FluorescenceYieldMean.class);
         sDefaultBetheEnergyLoss = (BetheElectronEnergyLoss) strat.getAlgorithm(BetheElectronEnergyLoss.class);
         sDefaultAngularDistribution = (BremsstrahlungAngularDistribution) strat.getAlgorithm(BremsstrahlungAngularDistribution.class);
         sDefaultCorrectionAlgorithm = (CorrectionAlgorithm) strat.getAlgorithm(CorrectionAlgorithm.class);
      } else
         clearGlobalOverride();
   }

   /**
    * Returns the current global algorithm strategy based on defaults and global
    * overrides.
    * 
    * @return Strategy
    */
   static public Strategy getGlobalStrategy() {
      final Strategy res = new Strategy();
      res.addAll(mGlobalOverride);
      res.addAlgorithm(TransitionEnergy.class, getDefaultTransitionEnergy());
      res.addAlgorithm(EdgeEnergy.class, getDefaultEdgeEnergy());
      res.addAlgorithm(MassAbsorptionCoefficient.class, getDefaultMAC());
      res.addAlgorithm(FluorescenceYield.class, getDefaultFluorescenceYield());
      res.addAlgorithm(FluorescenceYieldMean.class, getDefaultFluorescenceYieldMean());
      res.addAlgorithm(BetheElectronEnergyLoss.class, getDefaultBetheEnergyLoss());
      res.addAlgorithm(BremsstrahlungAngularDistribution.class, getDefaultAngularDistribution());
      res.addAlgorithm(CorrectionAlgorithm.class, getDefaultCorrectionAlgorithm());
      return res;
   }

   /**
    * clearGlobalOverride - Clear the global strategy override restoring each
    * algorithm to use its default algorithms.
    */
   static public void clearGlobalOverride() {
      mGlobalOverride = null;
      sDefaultTransitionEnergy = null;
      sDefaultEdgeEnergy = null;
      sDefaultMAC = null;
      sDefaultFluorescenceYield = null;
      sDefaultFluorescenceYieldMean = null;
      sDefaultBetheEnergyLoss = null;
      sDefaultAngularDistribution = null;
   }

   /**
    * addDefaultAlgorithm - Specify a default algorithm. (Use applyStrategy to
    * override)
    * 
    * @param cls
    * @param ac
    */
   protected void addDefaultAlgorithm(Class<?> cls, AlgorithmClass ac) {
      assert ac != null;
      if(mLocalOverride == null)
         mLocalOverride = new Strategy();
      mLocalOverride.addAlgorithm(cls, ac);
   }

   /**
    * Outputs a description of the current Strategy to the specified Writer.
    * 
    * @param wr
    * @throws IOException
    */
   public void documentStrategy(Writer wr)
         throws IOException {
      for(final String cls : mGlobalOverride.getStrategyMap().keySet()) {
         wr.write(getAlgorithm(cls).toString());
         wr.write('\n');
      }
   }

   private Strategy getEffectiveStrategyHelper() {
      final Strategy strat = new Strategy();
      if(mLocalOverride != null) {
         strat.addAll(mLocalOverride);
         final Collection<AlgorithmClass> algs = mLocalOverride.getAlgorithms();
         for(final AlgorithmClass ac : algs)
            strat.addAll(((AlgorithmUser) ac).getEffectiveStrategyHelper());
      }
      return strat;
   }

   /**
    * Returns a list of all algorithms on which this algorithm depends.
    * 
    * @return SortedSet&lt;AlgorithmClass&gt;
    */
   public Strategy getEffectiveStrategy() {
      final Strategy strat = getEffectiveStrategyHelper();
      strat.apply(mGlobalOverride);
      return strat;
   }

   /**
    * initializeDefaultStrategy - Implement this method in derived classes to
    * specify the contents (possibly null) of the default Strategy using the
    * addDefaultAlgorithm(Class,AlgorithmClass) method.
    */
   abstract protected void initializeDefaultStrategy();
}

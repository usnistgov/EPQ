package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.ToSI;

/**
 * <p>
 * An extension of the EPQLibrary Material class to include MONSEL-style
 * material properties relevant for secondary electron generation.
 * </p>
 * <p>
 * Various physical scattering models require different material properties.
 * Getters and setters allow storing and accessing this information. Elemental
 * composition and the density of the material are available from the parent
 * class. Additional properties made available by this extension include the
 * material work function, plasmon energy, the location of the conduction band
 * minimum, and electronic structure of the material in the form of a
 * representation of the density of states.
 * </p>
 * <p>
 * The density of states is represented by lists of discrete state energy
 * values, their corresponding kinetic energies, and their occupancies. The
 * energy values are stored as "binding" energies, i.e., positive values equal
 * to the distance of the specified state below the vacuum level, which is taken
 * as having energy = 0. Thus the binding energy is the negative of the total
 * energy associated with a state. The corresponding densities are the number of
 * electrons per cubic meter that occupy the specified state. A crude
 * approximation of an energy band might be a single state with energy in the
 * middle of the band and density reflecting ALL of the electrons in the band.
 * If desired, the approximation can be refined, possibly at the cost of slowing
 * the simulation, by dividing the density appropriately among a larger number
 * of energy states.
 * </p>
 * <p>
 * For some models, scattering cross sections depend upon the target electron's
 * kinetic energy and not simply upon its total energy. To facilitate these
 * models, kinetic energies are associated with each of the states. These
 * energies may be supplied by the user (e.g., based upon literature values or
 * the user's own model for the material). If they are not supplied by the user,
 * default values are supplied. For electrons in the conduction band (energy
 * above the conduction band minimum) the default algorithm assigns a kinetic
 * energy equal to the distance of the state above the band minimum. For
 * electrons at lower levels the default algorithm assigns a kinetic energy
 * equal to the distance of the state below the vacuum level. The virial theorem
 * provides the rationale for this latter choice. It is believed that this
 * choice should be accurate for core levels, which are localized and atom-like.
 * This choice is probably more dubious for lightly bound states, such as those
 * in the valence band, but as I am not aware of a general procedure that
 * applies to all materials this seems as reasonable a default procedure as any.
 * You, the user, are supposed to know your material. You are encouraged to
 * override the defaults if you have better information.
 * </p>
 * <p>
 * Note that the SEmaterial class's job is to make these material properties
 * available to models. The models themselves decide which properties to use and
 * how to use them.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class SEmaterial extends Material implements Cloneable {
   private static final long serialVersionUID = 0x42;

   // Additional material properties
   private double workfunction; // work function
   private double dielectricBreakdownField = Double.POSITIVE_INFINITY;
   private double epsr = 1.; // relative dielectric function
   private double eplasmon; // plasmon resonance energy
   private double energyCBbottom; // energy of conduction band bottom
   private double bandgap = 0.; // width of the bandgap
   private long version = 0L; // Updates each time the data change

   /*
    * The rate of SE generation depends upon the density of occupied states,
    * represented here by discrete binding energy/occupancy pairs. In the case
    * of materials with a continuous band of states, this discrete
    * representation must be regarded as an approximation, but it may be made as
    * close as we wish by dividing the band into as many discrete states as we
    * deem necessary. In many cases it may not be necessary to use very many. If
    * SE yield is the main concern, it may be adequate, and a good deal more
    * computationally efficient, to approximate a band by one or a very few
    * states of well-chosen binding energy(ies). The density of occupied
    * electronic states is here represented by 3 arrays. These are lists of
    * binding energies, kinetic energies, and densities of electrons at those
    * binding energies (in electrons/m^3). The total occupancy of all states
    * should sum to the actual number of target electrons per unit volume in the
    * material. Binding energies are positive for bound electrons and are
    * measured with respect to the vacuum. Attempting to assign a negative
    * binding energy or negative electron density will cause a runtime
    * EPQFatalException.
    */

   /*
    * bindingEnergy is an array of binding energies and electronDensity is the
    * corresponding # electrons/m^3 at each of these. kineticEnergy is the
    * corresponding kinetic energy of each.
    */
   private final ArrayList<Double> bindingEnergy = new ArrayList<Double>();
   private final ArrayList<Double> electronDensity = new ArrayList<Double>();
   private final ArrayList<Double> kineticEnergy = new ArrayList<Double>();
   private boolean userSetKE = false; // Flag = true when user sets a kinetic
   // energy.

   /*
    * The core energies are the minimum energies to excite electrons out of core
    * levels. For a metal, this is the energy relative to the Fermi level. They
    * are used by some ScatterMechanisms--e.g., TabulatedInelasticSM, which
    * assumes an energy transfer greater than one of these represents excitation
    * of a core electron. This may be redundant with bindingEnergy, above, but
    * coreEnergy states need not be accompanied by kinetic energies or
    * densities, since TabulatedInelasticSM doesn't need those. For now I'm
    * implementing this as a TreeSet. The TreeSet does not allow duplicate
    * energy levels. Such duplicates do appear in some references from some
    * materials. However, such duplicates have no effect on any of the
    * algorithms that use the coreEnergy so far. (I don't think they could,
    * unless they depended upon some additional property of the state, like a
    * quantum number, but these other properties are not presently stored
    * either.) Presently there is no effect of storing duplicate energies except
    * to expend time getting past them to the next unique entry; thus use of
    * TreeSet is all benefit and no loss. If in the future I do need to
    * implement an algorithm that requires duplicate entries I can change this
    * TreeSet declaration to SortedArrayList instead.
    */
   // private SortedArrayList<Double> coreEnergy = new
   // SortedArrayList<Double>();
   private TreeSet<Double> coreEnergy = new TreeSet<Double>();

   /**
    * SEmaterial - Constructs an SE material with parameters appropriate for
    * vacuum.
    */
   public SEmaterial() {
      super(0.);
      // Material properties appropriate to vacuum
      workfunction = 0.;
      energyCBbottom = 0.;
      eplasmon = 0.; // should correspond to infinite mean free path
   }

   /**
    * SEmaterial - Constructs an SE material of specified composition and
    * density, but with parameters otherwise appropriate for vacuum. For a
    * realistic material the constructor call must be followed by other methods
    * to set the work function, plasmon energy, energy of the conduction band
    * bottom, and density of occupied electronic states.
    *
    * @param comp
    *           - The composition of the material
    * @param density
    *           - The density of the material in kg/m^3
    */
   public SEmaterial(Composition comp, double density) {
      super(comp, density);
      workfunction = 0.;
      energyCBbottom = 0.;
      eplasmon = 0.; // should correspond to infinite mean free path
   }

   /**
    * SEmaterial - Constructs an SE material comprised of the specified list of
    * elements with specified weight fractions and density, but with parameters
    * otherwise appropriate for vacuum. For a realistic material the constructor
    * call must be followed by other methods to set the work function, plasmon
    * energy, energy of the conduction band bottom, and density of occupied
    * electronic states.
    *
    * @param elms
    *           - Element[] An array of elements that comprise this material
    * @param weightFracs
    *           - double[] The corresponding weight fractions
    * @param density
    *           - The density of the material in kg/m^3
    * @param name
    *           - String A name for this material.
    */
   public SEmaterial(Element[] elms, double[] weightFracs, double density, String name) {
      super(elms, weightFracs, density, name);
      workfunction = 0.;
      energyCBbottom = 0.;
      eplasmon = 0.; // should correspond to infinite mean free path
   }

   /**
    * SEmaterial - Constructs an SE material with the same composition and
    * density as mat, but with specifically secondary electron properties set as
    * appropriate for vacuum.
    *
    * @param mat
    *           - A material
    */
   public SEmaterial(Material mat) {
      super(mat, mat.getDensity());
      workfunction = 0.;
      energyCBbottom = 0.;
      eplasmon = 0.; // should correspond to infinite mean free path
   }

   /**
    * addBindingEnergy - Adds a binding energy, corresponding default kinetic
    * energy, and density to density of states. The binding energy is the
    * difference, vacuum energy minus electron's total energy. This number is
    * positive for bound electrons. The density is the number of electrons per
    * cubic meter of this material that have this binding energy.
    *
    * @param bindingEnergy
    *           - binding energy in Joules
    * @param density
    *           - # electrons/meter^3 in this material at this binding energy
    */
   public void addBindingEnergy(double bindingEnergy, double density) {
      if (bindingEnergy < 0.0)
         throw new EPQFatalException("Binding energies must be positive.");
      if (density < 0.0)
         throw new EPQFatalException("Electron density must be positive.");
      this.bindingEnergy.add(bindingEnergy);
      /*
       * Use default kinetic energy for this state. The default for the
       * conduction band is to use the difference between the energy and the
       * conduction band bottom. For other bands we use kinetic energy = binding
       * energy, a choice based upon the virial theorem. -- Or should I use
       * binding energy + CBbottom?
       */
      if (-bindingEnergy > energyCBbottom)
         this.kineticEnergy.add(-bindingEnergy - energyCBbottom);
      else
         this.kineticEnergy.add(bindingEnergy + energyCBbottom);
      electronDensity.add(density);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * addBindingEnergy - Adds a binding energy, corresponding kinetic energy,
    * and density to density of states. The binding energy is the difference,
    * vacuum energy minus electron's total energy. This number is positive for
    * bound electrons. The density is the number of electrons per cubic meter of
    * this material that has this binding energy.
    *
    * @param bindingEnergy
    *           - binding energy in Joules
    * @param kineticEnergy
    *           - binding energy in Joules
    * @param density
    *           - # electrons/meter^3 in this material at this binding energy
    */
   public void addBindingEnergy(double bindingEnergy, double kineticEnergy, double density) {
      if (bindingEnergy < 0.0)
         throw new EPQFatalException("Binding energies must be positive.");
      if (kineticEnergy < 0.0)
         throw new EPQFatalException("Kinetic energies must be positive.");
      if (density < 0.0)
         throw new EPQFatalException("Electron density must be positive.");
      this.bindingEnergy.add(bindingEnergy);
      this.kineticEnergy.add(kineticEnergy);
      userSetKE = true;
      electronDensity.add(density);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * addBindingEnergy-a slightly more efficient form of addBindingEnergy when
    * adding multiple binding energies and densities, because the
    * re-initialization of the scatter mechanisms is only done once at the end.
    *
    * @param bindingEnergy
    *           - binding energy in Joules
    * @param density
    *           - # electrons/meter^3 in this material at this binding energy
    */
   public void addBindingEnergy(List<Double> bindingEnergy, List<Double> density) {
      // Error checking
      if (bindingEnergy.size() != density.size())
         throw new EPQFatalException("Unequal # of binding energies and densities");
      for (final Double b : bindingEnergy)
         if (b < 0.0)
            throw new EPQFatalException("Binding energies must be positive.");
      for (final Double d : density)
         if (d < 0.0)
            throw new EPQFatalException("Electron density must be positive.");
      this.bindingEnergy.addAll(bindingEnergy);
      // Use default kinetic energy
      for (final Double b : bindingEnergy)
         if (-b > energyCBbottom)
            this.kineticEnergy.add(-b - energyCBbottom);
         else
            this.kineticEnergy.add(b + energyCBbottom);
      electronDensity.addAll(density);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * addBindingEnergy-a slightly more efficient form of addBindingEnergy when
    * adding multiple binding energies and densities, because the
    * re-initialization of the scatter mechanisms is only done once at the end.
    *
    * @param bindingEnergy
    *           - a list of binding energies in Joules
    * @param kineticEnergy
    *           - a list of corresponding kinetic energies in Joules
    * @param density
    *           - corresponding list of # electrons/meter^3 in this material at
    *           this binding energy
    */
   public void addBindingEnergy(List<Double> bindingEnergy, List<Double> kineticEnergy, List<Double> density) {
      // Error checking
      if ((bindingEnergy.size() != density.size()) || (kineticEnergy.size() != density.size()))
         throw new EPQFatalException("Lists of energies and densities must be equal length");
      for (final Double b : bindingEnergy)
         if (b < 0.0)
            throw new EPQFatalException("Binding energies must be positive.");
      for (final Double b : kineticEnergy)
         if (b < 0.0)
            throw new EPQFatalException("Kinetic energies must be positive.");
      for (final Double d : density)
         if (d < 0.0)
            throw new EPQFatalException("Electron density must be positive.");
      this.bindingEnergy.addAll(bindingEnergy);
      this.kineticEnergy.addAll(kineticEnergy);
      userSetKE = true;
      electronDensity.addAll(density);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * addCoreEnergy - Adds a core energy to the existing list. (Current values
    * are not cleared.) The core energies are the minimum energies to excite
    * electrons out of atomic core levels. That is, they are the energy
    * differences between core level and lowest unoccupied state (Fermi energy
    * For a metal, conduction band minimum for a non-metal).
    *
    * @param coreEnergy
    *           - binding energy in Joules
    */
   public void addCoreEnergy(double coreEnergy) {
      if (coreEnergy < 0.0)
         throw new EPQFatalException("Core energies must be positive.");
      this.coreEnergy.add(coreEnergy);

      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * addCoreEnergy -- Adds all core energies in a list. (Previous values are
    * not cleared.)
    *
    * @param coreEnergy
    *           - a list of core energies in Joules
    */
   public void addCoreEnergy(List<Double> coreEnergy) {
      // Error checking
      for (final Double cE : coreEnergy)
         if (cE < 0.0)
            throw new EPQFatalException("Core energies must be positive.");
      this.coreEnergy.addAll(coreEnergy);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   @Override
   public SEmaterial clone() {
      final SEmaterial res = new SEmaterial();
      res.replicate(this);
      return res;
   }

   /**
    * Returns a "version" number. This number starts at 0 and is incremented
    * each time the material is altered in any way (e.g., by changing a
    * parameter or adding or removing a scatter mechanism). A changed version
    * number alerts a client to refresh its cache.
    */
   /*
    * TODO -- I should consider replacing this clunky version mechanism with a
    * subject/observer pattern.
    */
   public long get_version() {
      return version;
   }

   /**
    * getBindingEnergyArray - Returns a copy of the array of binding energies.
    * Binding energies are not sorted. They may be in any order, but the binding
    * energy, kinetic energy, and electron density arrays will be in the same
    * order.
    *
    * @return Double[] - A copy of the array of binding energies
    */
   public Double[] getBindingEnergyArray() {
      return bindingEnergy.toArray(new Double[0]);
      // return bindingEnergy.toArray(new Double[bindingEnergy.size()]);
   }

   /**
    * getCoreEnergyArray - Returns a copy of the array of core energies. Core
    * energies are sorted in increasing order, and there are no duplicates.
    *
    * @return Double[] - A copy of the array of binding energies
    */
   public Double[] getCoreEnergyArray() {
      return coreEnergy.toArray(new Double[0]);
      // return bindingEnergy.toArray(new Double[bindingEnergy.size()]);
   }

   /**
    * getEFermi - Returns the material's Fermi energy. The Fermi energy is not
    * independent of other energies for the material. Relative to the vacuum,
    * EFermi = -workfunction. To re-reference it to the conduction band bottom
    * we need to subtract that value.
    *
    * @return double - The Fermi energy in Joules.
    */
   public double getEFermi() {
      return -energyCBbottom - workfunction;
   }

   /**
    * getElectronDensityArray - Returns an array containing the density of
    * electrons (electrons/m^3) at each of the various binding energies in this
    * material.
    *
    * @return Double[] - A copy of the array of electron densities
    *         (electrons/m^3).
    */
   public Double[] getElectronDensityArray() {
      return electronDensity.toArray(new Double[0]);
   }

   /**
    * See the definition of energyCBbottom at the setEnergyCBbottom() method.
    *
    * @return Returns the energyCBbottom.
    */
   public double getEnergyCBbottom() {
      return energyCBbottom;
   }

   /**
    * getEplasmon - Returns the plasmon energy.
    *
    * @return double - The plasmon energy in Joules.
    */
   public double getEplasmon() {
      return eplasmon;
   }

   /**
    * getKineticEnergyArray - Returns a copy of the array of kinetic energies.
    * Kinetic energies are not sorted. They may be in any order, but the binding
    * energy, kinetic energy, and electron density arrays will be in the same
    * order.
    *
    * @return Double[] - A copy of the array of kinetic energies
    */
   public Double[] getKineticEnergyArray() {
      return kineticEnergy.toArray(new Double[0]);
      // return bindingEnergy.toArray(new Double[bindingEnergy.size()]);
   }

   /*
    * At some future time I may want to create a setDefaultDoS() method. This
    * method would determine reasonable binding energy/electron density values
    * to be used for this material. For example, it could set the binding energy
    * equal to the work function and the electron density to the average number
    * of lightly bound electrons in atoms of this material. This can be
    * discovered by using the EPQLibrary's AtomicShell getEnergy() and
    * getGroundStateOccupancy() methods.
    */

   /**
    * getWorkfunction - Returns the work function.
    *
    * @return double - The work function in Joules.
    */
   public double getWorkfunction() {
      return workfunction;
   }

   /**
    * getBandgap - Returns the band gap in Joules.
    *
    * @return double - The band gap in Joules.
    */
   public double getBandgap() {
      return bandgap;
   }

   /**
    * gets the value of this material's relative dielectric constant, epsr =
    * eps/eps0.
    *
    * @return - the material's relative dielectric constant
    */
   public double getEpsr() {
      return epsr;
   }

   /**
    * @return - the value in volts/meter of the dielectric breakdown field
    */
   public double getDielectricBreakdownField() {
      return dielectricBreakdownField;
   }

   /**
    * removeBindingEnergy - Removes a binding energy, kinetic energy, and
    * density of states triplet from the existing list.
    *
    * @param index
    *           int - index of binding energy/density entry to remove
    */
   public void removeBindingEnergy(int index) {
      bindingEnergy.remove(index);
      kineticEnergy.remove(index);
      electronDensity.remove(index);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * removeCoreEnergy - Removes the specified core energy from the existing
    * list.
    *
    * @param energy
    *           Double - binding energy entry to remove
    */
   public void removeCoreEnergy(Double energy) {
      coreEnergy.remove(energy);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * removeCoreEnergy - Removes the core energy at the specified index from the
    * existing list. The index corresponds to the core energies in sorted order,
    * as returned by getCoreEnergyArray().
    *
    * @param index
    *           int - index of the binding energy entry to remove
    */
   public void removeCoreEnergy(int index) {
      Double energy = getCoreEnergyArray()[index];
      coreEnergy.remove(energy);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /*
    * Imitate Nicholas's clone procedure
    */
   protected void replicate(SEmaterial mat) {
      super.replicate(mat);
      workfunction = mat.getWorkfunction();
      energyCBbottom = mat.getEnergyCBbottom();
      eplasmon = mat.getEplasmon();
      bindingEnergy.addAll(mat.bindingEnergy);
      electronDensity.addAll(mat.electronDensity);
      kineticEnergy.addAll(mat.kineticEnergy);
      userSetKE = mat.userSetKE;
      coreEnergy.addAll(mat.coreEnergy);
      bandgap = mat.getBandgap();
      epsr = mat.getEpsr();
      dielectricBreakdownField = mat.getDielectricBreakdownField();
   }

   /**
    * setBindingEnergy -- Changes the energy associated with the state at a
    * given index to a new value. If default kinetic energy values have not been
    * overridden, the corresponding kinetic energy is set according to the
    * default algorithm.
    *
    * @param index
    *           - the index of the entry to change
    * @param energy
    *           - the new density in electrons/m^3
    */
   public void setBindingEnergy(int index, double energy) {
      if (energy < 0.)
         throw new EPQFatalException("Binding energies must be positive.");
      bindingEnergy.set(index, energy);
      if (!userSetKE)
         if (-energy > energyCBbottom)
            this.kineticEnergy.set(index, -energy - energyCBbottom);
         else
            this.kineticEnergy.set(index, energy + energyCBbottom);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * setCoreEnergy - Clears the list of core energy levels.
    */
   public void setCoreEnergy() {
      this.coreEnergy = new TreeSet<Double>();
   }

   /*
    * setCoreEnergy - Sets the core energy list to the supplied array. (Any
    * previous values are cleared.) @param coreEnergy - Double[] Energies of
    * core levels in Joules
    */
   public void setCoreEnergy(Double[] coreEnergy) {
      setCoreEnergy(Arrays.asList(coreEnergy));
   }

   /**
    * setCoreEnergy - Sets the core energy list to the supplied list. (Any
    * previous values are cleared.)
    *
    * @param coreEnergy
    *           - List of Energies of core levels in Joules
    */
   public void setCoreEnergy(List<Double> coreEnergy) {
      this.coreEnergy = new TreeSet<Double>();
      for (final double cE : coreEnergy) {
         if (cE < 0.0)
            throw new EPQFatalException("Core energies must be positive.");
         this.coreEnergy.add(cE);
      }
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * setElectronDensity -- Changes the density associated with the state at a
    * given index to a new value.
    *
    * @param index
    *           - the index of the entry to change
    * @param density
    *           - the new density in electrons/m^3
    */
   public void setElectronDensity(int index, double density) {
      if (density < 0.)
         throw new EPQFatalException("Electron density must be positive.");
      electronDensity.set(index, density);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * energyCBbottom is the energy of the conduction band bottom. The vacuum
    * energy is defined to be 0, so energyCBbottom should be negative.
    *
    * @param energyCBbottom
    *           The energyCBbottom to set.
    */
   public void setEnergyCBbottom(double energyCBbottom) {
      this.energyCBbottom = energyCBbottom;
      if (!userSetKE)
         setKEtoDefault();
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * bandgap is the width of an insulating or semiconducting material's
    * bandgap, in Joules. bandgap = 0 for conductors.
    *
    * @param bandgap
    *           The bandgap to set.
    */
   public void setBandgap(double bandgap) {
      this.bandgap = bandgap;
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * setEplasmon - Sets the material's plasmon energy.
    *
    * @param eplasmon
    *           double - plasmon energy in Joules
    */
   public void setEplasmon(double eplasmon) {
      this.eplasmon = eplasmon;
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /*
    * TODO A better approach to defining the core energy cutoff might be to find
    * the edge energy of the highest occupied orbital. This is by definition the
    * valence shell. There may possibly be other energies near this one (for
    * different angular momentum states) and then a gap. Requiring this gap to
    * be a minimum size might be more reliable than requiring the absolute
    * energy to be bigger than a certain size. Alternatively, maybe Nicholas's
    * utilities will tell me the principle quantum number, and I just want
    * energies for states with principle quantum numbers different from the
    * valence shell's number.
    */
   final double coreEnergyCutoff = ToSI.eV(20.);

   /**
    * setEstimatedCoreEnergy - Resets the list of core energy levels to
    * estimated values. The estimated values for an element are the atomic shell
    * edge energies for the shells with nonzero ground state occupancy. Shells
    * with energies less than a cutoff (specifiable, 20 eV default) are not
    * considered to be core states, and they are excluded. The edge energies
    * values are determined by calling EPQLibrary's AtomicShell.getEdgeEnergy().
    * For a composite material, the estimate is determined by merging the lists
    * energies from the constituent elements. Known limitations: (1) This
    * procedure does not take into account energy shifts that depend upon the
    * state of the material (e.g, atomic vs. solid). (2) All constituents of
    * composite materials are included, even those that might be present only in
    * trace amounts. Best accuracy is therefore achieved by looking up the
    * values in a good reference and setting them using one of the other
    * methods.
    */

   public void setEstimatedCoreEnergy() {
      setEstimatedCoreEnergy(coreEnergyCutoff);
   }

   public void setEstimatedCoreEnergy(double cutoff) {
      double shellenergy;
      setCoreEnergy(); // Clear any existing ones.
      final Set<Element> constituentElements = this.getElementSet();
      for (final Element el : constituentElements) {
         int i = 0;
         AtomicShell as = new AtomicShell(el, i);
         while ((shellenergy = as.getGroundStateOccupancy() > 0 ? as.getEdgeEnergy() : Double.NaN) > cutoff) {
            addCoreEnergy(shellenergy);
            i++;
            as = new AtomicShell(el, i);
         }
      }
   }

   /**
    * setKEtoDefault -- resets kinetic energies according to the default
    * algorithm. The default is for the kinetic energies associated with states
    * in the conduction band to be the difference between the electron energy
    * and the energy of the band bottom. For states below the conduction band
    * the kinetic energy by default is set equal to the distance of that state
    * below the vacuum level. This choice is motivated by the virial theorem. It
    * should be a good value for deep core levels. Its value for shallow states
    * (e.g., the valence band) is not established. It is probably as good a
    * guess as one can make absent specific band structure information. If such
    * information is available, the default should be overridden.
    */
   void setKEtoDefault() {
      for (int i = 0; i < kineticEnergy.size(); i++)
         if (-bindingEnergy.get(i) > energyCBbottom)
            kineticEnergy.set(i, -bindingEnergy.get(i) - energyCBbottom);
         else
            kineticEnergy.set(i, bindingEnergy.get(i) + energyCBbottom);
      userSetKE = false;
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * setKineticEnergy -- Changes the kinetic energy associated with the state
    * at a given index to a new value.
    *
    * @param index
    *           - the index of the entry to change
    * @param energy
    *           - the new density in electrons/m^3
    */
   public void setKineticEnergy(int index, double energy) {
      if (energy < 0.)
         throw new EPQFatalException("Kinetic energies must be positive.");
      kineticEnergy.set(index, energy);
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * setWorkfunction - Sets the material's work function. In JMONSEL the work
    * function is the positive difference in energy between vacuum and the
    * highest occupied state, whether in the conduction band or, if the
    * conduction band is empty, the valence band. States (such as trap states
    * within the band gap) that have insignificant density and so are unlikely
    * to be a source of secondary electrons may be ignored in making this
    * assignment.
    *
    * @param workfunction
    *           double - work function in Joules
    */
   public void setWorkfunction(double workfunction) {
      this.workfunction = workfunction;
      if (energyCBbottom > -workfunction)
         energyCBbottom = -workfunction;
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * Sets the value of the relative dielectric constant at 0 frequency, epsr =
    * eps/eps0, for this material.
    *
    * @param epsr
    */
   public void setEpsr(double epsr) {
      this.epsr = epsr;
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }

   /**
    * Sets the dielectricBreakdownField, in volts/meter, at which significant
    * conduction occurs in insulators. This is available for use by conduction
    * models. By default it is set to infinity (no charge redistribution
    * occurs).
    *
    * @param breakdownField
    *           - the magnitude of the electric field (volts/meter) for which
    *           there is significant conduction.
    */
   public void setDielectricBreakdownField(double breakdownField) {
      dielectricBreakdownField = breakdownField;
      version = (version == Long.MAX_VALUE) ? 0L : version + 1L;
   }
}

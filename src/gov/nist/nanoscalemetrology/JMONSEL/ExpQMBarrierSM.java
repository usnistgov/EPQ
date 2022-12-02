package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements quantum mechanical scattering from a finite-width barrier with
 * "exponential" shape. I.e., the form of the potential energy barrier is
 * assumed to be U(x) = deltaU/(1+Exp(-2*x/lambda)). This barrier function
 * represents a smooth s-shaped transition from U(x)=0 well to the left of the
 * barrier to U(x)=deltaU well to the right. lambda is a measure of the width of
 * the barrier. Roughly half (~46%) of the transition occurs over a distance
 * equal to lambda (from x = -lambda/2 to x = lambda/2), 90% over 3*lambda.
 * </p>
 * <p>
 * There is an analytical formula for the probability of barrier transmission
 * through such a barrier. (See, e.g., Landau &amp; Lifshitz, Quantum Mechanics,
 * 1958, pg. 75.) This class computes barrier transmission using that formula.
 * In the limit that lambda is "large," this probability approaches the
 * classical result. In the limit that lambda goes to 0, it approaches the
 * transmission probability for sharp barriers encountered in elementary quantum
 * mechanics texts. For most purposes the useful range of lambda values is from
 * 0 m to 1.E-9 m. Anything larger than this is essentially classical.
 * </p>
 * <p>
 * A barrier scattering mechanism is associated with each region of our sample,
 * but barrier transmission is a pair-wise phenomenon. That is, the barrier
 * height and width at an interface between materials A and B depends in
 * principle upon the properties of both materials. If the electron starts
 * inside material A, then it is material A's barrier scatter mechanism that
 * governs that particular scattering event. The present scatter mechanism
 * determines the barrier height by comparing the potential energies in the
 * materials on each side of the interface. It uses the barrier width associated
 * with material A.
 * </p>
 * <p>
 * This is therefore a somewhat general implementation of barrier scattering
 * that includes classical and sharp-barrier quantum mechanical scattering as
 * special cases. Two constructors are provided. Both constructors accept a
 * material as input (from which the barrier height is determined). One
 * constructor additionally allows specification of lambda. The other does not.
 * The constructor without lambda specification implements classical barrier
 * scattering (the large lambda limit). This method of implementing classical
 * barrier scattering is preferred to giving an explict but large value of
 * lambda because it uses a simpler limiting-case formula, and it avoids
 * possible numerical issues associated with large arguments. The constructor
 * with an explicit lambda specification implements quantum mechanical
 * scattering for a barrier of the specified width.
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined for the materials on both sides of the
 * interface: energyCBbottom.
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

/*
 * A classical model may be more applicable than one might think. The
 * significance of quantum mechanical effects for SE yield is mitigated by two
 * factors. First, the strength of the effect depends upon how quickly the
 * potential energy transitions between its levels in the two materials. An
 * infinitely sharp transition, which is easy to solve and produces the usual
 * quantum mechanical transmission formula, is unrealistic. As the barrier width
 * increases, the quantum mechanical result quickly approaches the classical
 * limit. By 1 nm, there is little discernible difference. Secondly, quantum
 * mechanical effects are energy dependent, and the electrons encountering the
 * barrier are distributed in energy. Averaging over the distribution suggests
 * at most a 10% yield difference between an infinitely sharp barrier and a
 * barrier wide enough to be in the classical limit.
 */

public class ExpQMBarrierSM implements BarrierScatterMechanism {

   /*
    * The 3 quantities: work function, Fermi energy, and potential energy are
    * related by work function + Fermi energy + potential energy = 0.
    */

   private double u0; // Barrier height of this material to vacuum.
   private double lambda; // Barrier width
   private boolean classical;
   /*
    * The general formula for barrier transmission includes a factor of
    * PI*lambda*Sqrt(m/2.)/hbar that multiplies the square roots of energies.
    * Once lambda is given, this factor is precomputed and stored here.
    */
   private double lambdaFactor;
   private Material mat;

   /**
    * ExpQMBarrierSM -- Constructs a barrier scatter mechanism for the special
    * case of a wide barrier (i.e., a classical barrier scatter mechanism).
    *
    * @param mat
    *           - The material for which to construct it. If the provided
    *           argument is an SEmaterial, the potential energy of an electron
    *           in this material will be determined from the material
    *           properties. Otherwise it is set to 0.
    */
   public ExpQMBarrierSM(Material mat) {
      super();
      // this.mat = mat;
      if (mat instanceof SEmaterial)
         u0 = ((SEmaterial) mat).getEnergyCBbottom();
      else
         u0 = 0.;
      classical = true;
   }

   /**
    * ExpQMBarrierSM -- Constructs a barrier scatter mechanism for a quantum
    * mechanical barrier of specified width.
    *
    * @param mat
    *           - The material for which to construct it. If the provided
    *           argument is an SEmaterial, the potential energy of an electron
    *           in this material will be determined from the material
    *           properties. Otherwise it is set to 0.
    * @param lambda
    *           - The width of the barrier in meters.
    */
   public ExpQMBarrierSM(Material mat, double lambda) {
      this(mat);
      classical = false;
      this.lambda = lambda;
      lambdaFactor = (Math.PI * lambda * Math.sqrt(PhysicalConstants.ElectronMass / 2.)) / PhysicalConstants.PlanckReduced;
   }

   /**
    * sharpBarrierT -- a private utility routine. It computes the transmission
    * probability for a sharp (lambda = 0) barrier as a function of a
    * dimensionless barrier height, reducedU. reducedU =
    * deltaU/(kE*cos(theta)^2) where deltaU is the barrier height, kE is the
    * kinetic energy of the electron, and theta is the angle its trajectory
    * makes with the barrier. The more general formula (below) attempts to
    * divide by 0 if lambda is 0, but its limit is well defined. This routine
    * implements that limit. The formula is Prob =
    * 4*sqrt(1-reducedU)/(1+sqrt(1-reducedU))^2.
    *
    * @param reducedU
    * @return
    */
   private double sharpBarrierT(double rootPerpE, double rootDiff) {
      final double ksum = rootPerpE + rootDiff;
      return (4. * rootPerpE * rootDiff) / (ksum * ksum);
   }

   /**
    * generalBarrierT -- a private utility routine. It computes the quantum
    * mechanical transmission probability for a barrier of nonzero width as a
    * function of the kinetic energy associated with the electron's motion
    * perpendicular to the barrier (kE*cos(theta)^2) and the barrier height,
    * deltaU.
    *
    * @param perpE
    * @param deltaU
    * @return
    */
   private double generalBarrierT(double rootPerpE, double rootDiff) {
      /*
       * In the small wavelength (large energy) limit, our sinh functions below
       * can overflow, but there is no need in this limit to go to the trouble
       * of computing them. Transmission in that limit is indistinguishable from
       * 1. We arbitrarily choose k1 > 50 as our definition of large k. With
       * this choice, approximating by 1 has error less than 5% for kE >
       * 1.0002*deltaE.
       */
      final double k1 = lambdaFactor * rootPerpE;
      if (k1 > 50.)
         return 1.;
      final double k2 = lambdaFactor * rootDiff;
      final double kplus = k1 + k2;
      final double kminus = k1 - k2;
      final double sinhPlus = Math.sinh(kplus);
      final double sinhMinus = Math.sinh(kminus);
      final double ratio = sinhMinus / sinhPlus;
      return 1. - (ratio * ratio);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.BarrierScatterMechanism#barrierScatter
    * (gov.nist.microanalysis.NISTMonte.Electron,
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase)
    */
   @Override
   public Electron barrierScatter(Electron pe, RegionBase nextRegion) {
      /*
       * FIND THE STEP HEIGHT OF THE BARRIER. To do this correctly both
       * materials need to be SEmaterials so we can get the relevant parameters.
       * The most common non-SE material that we will encounter is vacuum
       * because MonteCarloSS assigns this to the space outside the sample. For
       * vacuum the potential energy step by definition is simply the
       * -potentialU of the current material, so that's what we do. Other non-SE
       * materials are treated the same.
       */
      final Material nextmaterial = nextRegion.getMaterial();

      /*
       * We must discover which shape we have intersected. If nextRegion is not
       * a subregion of the current one, then our intersection is with a
       * boundary of the current region. If it IS a subregion, then our
       * intersection is with it or with one of its containing regions.
       */
      final RegionBase currentRegion = pe.getCurrentRegion();

      assert currentRegion != nextRegion;

      double deltaU;
      final Material currentMaterial = currentRegion.getMaterial();
      if (currentMaterial instanceof SEmaterial)
         deltaU = -((SEmaterial) currentMaterial).getEnergyCBbottom();
      else
         deltaU = 0.;

      assert deltaU == -u0;
      if (nextmaterial instanceof SEmaterial)
         deltaU += ((SEmaterial) nextmaterial).getEnergyCBbottom();

      if (deltaU != 0.) {
         @SuppressWarnings("unused")
         int dummy = 0;
      }

      /* FIND THE OUTWARD POINTING NORMAL AT THE BOUNDARY */
      double[] nb = null; // We'll store it here

      if (currentRegion.isContainingRegion(nextRegion)) {
         RegionBase struckRegion = nextRegion; // usually this is true
         /*
          * Sometimes we cross multiple boundaries at once. The while loop
          * checks and corrects for this.
          */
         while (struckRegion.getParent() != currentRegion)
            struckRegion = struckRegion.getParent();
         final Shape intersectedshape = struckRegion.getShape();
         if (intersectedshape instanceof NormalShape) {
            nb = ((NormalShape) intersectedshape).getPreviousNormal().clone();
            for (int i = 0; i < 3; i++)
               nb[i] *= -1;
         }
      } else {
         final Shape intersectedshape = currentRegion.getShape();
         if (intersectedshape instanceof NormalShape)
            nb = ((NormalShape) intersectedshape).getPreviousNormal();
      }

      // GET THE VECTOR IN THE ELECTRON'S DIRECTION OF MOTION
      final double theta0 = pe.getTheta();
      final double phi0 = pe.getPhi();
      final double sintheta0 = Math.sin(theta0);
      final double[] n0 = new double[]{sintheta0 * Math.cos(phi0), sintheta0 * Math.sin(phi0), Math.cos(theta0)};

      /*
       * If the intersected shape is not a NormalShape, we still haven't
       * initialized nb. We have no data in this case, so we must make do with
       * an arbitrary assignment: Let nb be the same as the electron direction.
       * This choice gives maximum transmission probability and no deflection of
       * the electron's path.
       */
      if (nb == null)
         nb = n0;

      /*
       * Let the angle of incidence be called alpha. Cos(alpha) is given by the
       * dot product
       */
      final double cosalpha = (n0[0] * nb[0]) + (n0[1] * nb[1]) + (n0[2] * nb[2]);

      if (cosalpha <= 0.) {
         /*
          * This case corresponds to the electron "hitting" the barrier while
          * moving away from it. I.e., it didn't really hit the barrier. This
          * can happen, e.g., if electric field alters the electron's direction
          * of motion. We give it a nudge away from the barrier towards the
          * inside
          */
         final double[] pos0 = pe.getPosition();
         pe.setPosition(new double[]{pos0[0] - (MonteCarloSS.SMALL_DISP * nb[0]), pos0[1] - (MonteCarloSS.SMALL_DISP * nb[1]),
               pos0[2] - (MonteCarloSS.SMALL_DISP * nb[2])});

         return null;
      }

      if (deltaU == 0.) {
         /*
          * This corresponds to no barrier. This is usually due to a
          * mathematical boundary with the same material on both sides. It
          * transmits, so we give it a nudge off of the barrier toward the
          * outside, update the electron's region, and return.
          */
         final double[] pos0 = pe.getPosition();
         pe.setPosition(new double[]{pos0[0] + (MonteCarloSS.SMALL_DISP * nb[0]), pos0[1] + (MonteCarloSS.SMALL_DISP * nb[1]),
               pos0[2] + (MonteCarloSS.SMALL_DISP * nb[2])});
         pe.setCurrentRegion(nextRegion);

         return null;
      }

      final double kE0 = pe.getEnergy();
      double perpE;
      if (kE0 <= 0.)
         perpE = 0.;
      else
         perpE = cosalpha * cosalpha * kE0;
      double rootPerpE = 0.;
      double rootDiff = 0.;

      /* DECIDE WHETHER IT TRANSMITS OR NOT */
      boolean transmits;
      if ((perpE == 0.) || (perpE <= deltaU))
         /*
          * Even if deltaU<0 (the electron is stepping downhill) the quantum
          * mechanical formula gives transmission = 0 when perpE = 0.
          */
         transmits = false;
      else {
         rootPerpE = Math.sqrt(perpE);
         rootDiff = Math.sqrt(perpE - deltaU);
         if (classical)
            transmits = true; // Since we already know perpE>deltaU
         else {
            double transmissionProb;
            if (lambda == 0.)
               transmissionProb = sharpBarrierT(rootPerpE, rootDiff);
            else
               transmissionProb = generalBarrierT(rootPerpE, rootDiff);

            final double r = Math2.rgen.nextDouble();
            transmits = r < transmissionProb;
         }
      }

      /*
       * COMPUTE FINAL DIRECTION AND ENERGY FOR EACH OF THE CASES: TRANSMISSION
       * OR REFLECTION
       */
      final double[] nf = new double[3]; // Direction vector after scattering
      if (transmits) { // Transmission
         final double factor = cosalpha * ((rootDiff / rootPerpE) - 1.);
         /*
          * Following is the numerator part of the 3 components of the new
          * direction vector. All components would need to be divided by the
          * same factor, for normalization, but we don't bother because we don't
          * need it, except for the z component.
          */
         for (int i = 0; i < 3; i++)
            nf[i] = n0[i] + (factor * nb[i]);
         /* Normalize the z component to use later computing theta. */
         // nf[2] /= Math.sqrt(1. + (2. * cosalpha + factor) * factor);
         nf[2] /= Math.sqrt((nf[0] * nf[0]) + (nf[1] * nf[1]) + (nf[2] * nf[2]));

         pe.setEnergy(kE0 - deltaU);
         pe.setCurrentRegion(nextRegion);
         /*
          * Round off error makes some electrons, particularly those with final
          * directions nearly parallel to the interface, have positions on the
          * wrong side of their boundary. To prevent this we displace the
          * electron position 1 pm normal to the boundary.
          */
         final double[] pos0 = pe.getPosition();

         pe.setPosition(new double[]{pos0[0] + (MonteCarloSS.SMALL_DISP * nb[0]), pos0[1] + (MonteCarloSS.SMALL_DISP * nb[1]),
               pos0[2] + (MonteCarloSS.SMALL_DISP * nb[2])});
         /*
          * TODO On rare occasions, such as when our electron is within
          * SMALL_DISP of a corner, the above displacement can move the electron
          * outside its current region.
          */
      } else { // Total internal reflection
         final double twocosalpha = 2. * cosalpha;
         for (int i = 0; i < 3; i++)
            nf[i] = n0[i] - (nb[i] * twocosalpha);

         final double[] pos0 = pe.getPosition();
         pe.setPosition(new double[]{pos0[0] - (MonteCarloSS.SMALL_DISP * nb[0]), pos0[1] - (MonteCarloSS.SMALL_DISP * nb[1]),
               pos0[2] - (MonteCarloSS.SMALL_DISP * nb[2])});
      }

      final double thetaf = Math.acos(nf[2]);
      final double phif = Math.atan2(nf[1], nf[0]);

      pe.setDirection(thetaf, phif);
      return null;
   }

   /**
    * @return - a string in the form "ExpQMBarrierSM(material,u0,lambda)", where
    *         material and lambda are the parameters supplied to the constuctor
    *         and u0 is the barrier height (to vacuum).
    */
   @Override
   public String toString() {
      return "ExpQMBarrierSM(" + mat.toString() + "," + Double.valueOf(u0).toString() + "," + Double.valueOf(lambda).toString() + ")";
   }
}

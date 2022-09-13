/**
 * gov.nist.nanoscalemetrology.JMONSEL.ChargingListener Created by: jvillar
 * Date: Nov 22, 2010
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince54Integrator;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MeshElementRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.DirichletConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.FloatingConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.IConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.NeumannConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.RCConnection;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh.Tetrahedron;

import Jama.Matrix;

/**
 * <p>
 * ChargingListener is used to simulate the effect of charging. It keeps an
 * account of charge accumulation or depletion within MeshedRegions,
 * periodically performs finite element analysis (FEA) to compute the resulting
 * electrostatic potentials and fields, and corrects electron trajectories and
 * energies accordingly.
 * </p>
 * <p>
 * The frequency of FEA solutions is determined by the user-supplied
 * solnInterval parameter. The ChargingListener maintains a count of
 * trajectories run since the last solution. When this count equals
 * solnInterval, a new FEA solution is computed. At any given time, the fields
 * are approximated by the values determined at the most recently completed
 * solution. Choice of solnInterval is a trade-off: Smaller values lead to more
 * frequent and hence more accurate solutions, but FEA solutions can be
 * time-consuming.
 * </p>
 * <p>
 * ChargingListener performs FEA solutions by using an IFEARunner. The
 * IFEArunner tells ChargingListener how to run finite element analysis when
 * needed. Typical implementations provide a number of user-settable parameters
 * that determine how the solution is to be performed. The parameters determine
 * such things as the type of solver or preconditioner, convergence criteria,
 * etc. The values of the parameters affect whether the FEA solution converges
 * and the amount of time for convergence. Parameters that permit convergence
 * under the widest possible set of circumstances can cause solutions to be
 * needlessly time-consuming. ChargingListener employs a set of defaults that
 * seem to provide a reasonable compromise between speed and generality. If your
 * problem fails to converge it may benefit from altered settings (e.g., higher
 * quality preconditioner). On the other hand, naturally well-conditioned
 * problems might run faster with altered settings (e.g., faster, low quality
 * preconditioner). Setters and getters for these parameters are provided by
 * ChargingListener. A description of the function of each parameter may be
 * found in the documentation for these methods. For additional details, see
 * Sparskit documentation available on the web.
 * </p>
 * <p>
 * In the constructor, set feaInitializationRequired = true if you need an FEA
 * solution to be computed at N = 0 (i.e., before any electron trajectories are
 * run). You would use this, for example, if your mesh has initial nonzero
 * charge densities in some regions or if it contains conducting regions held at
 * nonzero potentials and you have not already computed and imported the
 * solution for the resulting fields. If the sample is initially uncharged and
 * field-free (the state assumed when you import a mesh) then it is not
 * necessary to run an FEA at N = 0. In this case either set
 * feaInitializationRequired = false in the constructor or use the provided form
 * of the constructor for which this is the default setting.
 * </p>
 * <p>
 * ChargingListener can be set to log the charge &amp; potential state of the mesh
 * to a file each cycle. The user calls setCPlog(), supplying the path to a
 * destination folder. Log files are generated internally using the
 * mesh.exportChargeAndPotentials method (see this method documentation for
 * details). The files are called cp[n].dat, with n starting with 1 and
 * incremented each time a log file is written. Logging is off by default. After
 * it is turned on by a call to setCPlog() it may be turned off again by calling
 * the stopCPlog() method.
 * </p>
 * <p>
 * Since the user supplies the mesh upon which this calculation is based, it may
 * be useful to consider how approximations employed by JMONSEL depend upon the
 * size of mesh elements. The FEA uses a linear approximation for potentials
 * within tetrahedral mesh elements. This means the electric field is constant
 * within each element. Charges deposited within a mesh element are assumed to
 * be uniformly distributed. I.e., the charge density is constant within each
 * element. Trajectories are corrected using Euler's approximation assuming
 * constant acceleration. Cumulative errors in Euler's approximation scale
 * linearly with element size. The scale is set by the electron's kinetic
 * energy. Trajectories should be accurate if the mesh elements are chosen
 * everywhere small enough that the difference between the maximum and minimum
 * potential energy within the element is small compared to the electron's
 * kinetic energy. Of course, the electron's kinetic energy eventually tends to
 * 0, so trajectories can never be accurate at the end of the electron's range.
 * However, the error in the electron's stopping position (which determines the
 * charge distribution) then depends upon the electron's remaining range. This
 * is small when the electron's energy is small. A reasonable starting
 * hypothesis would be to keep mesh elements small enough so that potential
 * differences across a mesh element are less than 1 volt. To keep memory and
 * computation time within reasonable limits, it is generally necessary to use a
 * variable mesh element size. Obviously, the most critical places to have small
 * mesh elements will be in the neighborhood of charges, and within those
 * neighborhoods the most critical are those near features of interest.
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
public class ChargingListener
   implements ActionListener {

   /**
    * <p>
    * A class to represent the differential equations that describe currents in
    * RCConnections. The differential equations are int the form qdot[i] = -
    * deltaV[i]/(p*tau(deltaV[i])) where q[i] is the charge flowed on the ith RC
    * connection, qdot[i] is its time derivative, deltaV[i] is the potential
    * drop across the ith connection, tau(deltaV[i]) is the time constant
    * associated with this connection, and p is the reciprocal of a capacitance
    * associated with the connection.
    * </p>
    * <p>
    * deltaV is determined from a matrix equation of the form deltaV = Vprev +
    * coefficientsChargesFromBeam.Q + coefficientsChargesFromCurrent.q where
    * Vprev is the voltage across the connections at last FEA, Q are new charges
    * on the floating regions since the last FEA and q is the amount of charge
    * in the connections since the last FEA. The quantities are all vectors or
    * matrices and "." means matrix multiplication. q is a vector of length
    * equal to the number of connections (the dimension of the problem). Q is a
    * vector of length equal to the number of floating regions involved in the
    * connections. Since connections may involve 1 or 2 floating regions Q will
    * have length greater than or equal to q's length.
    * </p>
    * <p>
    * The calling routine need not (and cannot) supply all of the constants in
    * these equations. Some of them, e.g., the p constant in the denominator and
    * the matrices, are determined by FEA by the constructor. The voltages at
    * the last FEA are likewise determined internally. After construction, the
    * only remaining parameter that the calling routine must supply to set up
    * the equations before solving is the Q vector--the average charges on
    * floating regions not due to charge flows in the connections (e.g., charges
    * directly from the beam) that have appeared in the interval since the
    * previous FEA. The calling routine supplies this vector by calling setV0().
    * ConnectionCurrentsODE then uses these charges to determine the initial
    * voltages on the connections.
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
    * @author John Villarrubia
    * @version 1.0
    */
   /* My notes on the derivation of this class are in DischargeModel.nb */
   private class ConnectionCurrentsODE
      implements FirstOrderDifferentialEquations {

      // private MeshedRegion r;
      private final RCConnection[] connectionList;
      private final int dim;
      /*
       * The equation for how the voltages across the connections depend upon
       * charges is in the form Vconnections = Vprev +
       * coefficientsChargesFromBeam.Q + coefficientsChargesFromCurrent.q where
       * Vprev is the voltage across the connections at last FEA, Q are new
       * charges on the floating regions since the last FEA and q is the amount
       * of charge in the connections since the last FEA. The quantities are all
       * vectors or matrices and "." means matrix multiplication. q is a vector
       * of length equal to the number of connections (the dimension of the
       * problem). Q is a vector of length equal to the number of floating
       * regions involved in the connections. Since connections may involve 1 or
       * 2 floating regions Q will have length greater than or equal to q's
       * length.
       */
      private final Matrix coefficientsChargesFromBeam;
      private final Matrix coefficientsChargesFromCurrent;
      private Matrix v0;

      /*
       * Following will hold the FloatingConstraints referenced in our
       * connection list. Its a set to avoid duplicates. It's a LinkedHashSet
       * because this is the only standard set implementation that preserves
       * insertion order.
       */
      private final LinkedHashSet<FloatingConstraint> floatSet = new LinkedHashSet<FloatingConstraint>();

      /**
       * Constructs a ConnectionCurrentsODE with equilibration from the
       * connections defined for the supplied MeshedRegion.
       *
       * @param r - the meshed region associated with this
       *           ConnectionCurrentsODE.
       */
      public ConnectionCurrentsODE(MeshedRegion r) {
         this(r, true);
      }

      /**
       * <p>
       * Constructs with or without equilibration a ConnectionCurrentsODE from
       * the connections defined for the supplied MeshedRegion.
       * </p>
       * <p>
       * Equilibration refers to flowing charges in the connections so as to
       * make the starting voltage drops on all connections equal to 0, the
       * steady state condition. This is appropriate if we are simulating a case
       * in which the sample has had a time to equilibrate that is large
       * compared to the relaxation times on all the connections.
       * </p>
       *
       * @param r - the meshed region associated with this
       *           ConnectionCurrentsODE.
       * @param equilibrate - if true, enough charge is flowed in each
       *           connection to make its initial voltage drop = 0. Otherwise,
       *           the charge is left as given and the connections may start in
       *           nonequilibrium states.
       */
      public ConnectionCurrentsODE(MeshedRegion r, boolean equilibrate) {
         // this.r = r;
         connectionList = r.getConnectionList();
         dim = connectionList.length;

         /*
          * Add FloatingConstraints to our floatSet and construct source and
          * drain matrices. The matrix dimensions are initialized to the maximum
          * possible size. We'll truncate later, after we know what the true
          * size is.
          */
         Matrix sourceMatrix = new Matrix(dim, 2 * dim);
         Matrix drainMatrix = new Matrix(dim, 2 * dim);
         int constraintIndex = 0;

         for(int connectionIndex = 0; connectionIndex < dim; connectionIndex++) {
            final RCConnection c = connectionList[connectionIndex];
            final IConstraint source = c.getSourceConstraint();
            if(source instanceof FloatingConstraint) {
               final FloatingConstraint src = (FloatingConstraint) source;
               if(floatSet.add(src)) {
                  /*
                   * src was not already in our set. Its index is just the
                   * current constraintIndex.
                   */
                  sourceMatrix.set(connectionIndex, constraintIndex, 1.);
                  constraintIndex++;
               } else {
                  /*
                   * src was already in our set. We have to ascertain its index.
                   */
                  int srcIndex = 0;
                  for(final FloatingConstraint s : floatSet)
                     if(s != src)
                        srcIndex++;
                     else
                        break;
                  sourceMatrix.set(connectionIndex, srcIndex, 1.);
               }
            }
            final IConstraint drain = c.getDrainConstraint();
            if(drain instanceof FloatingConstraint) {
               final FloatingConstraint drn = (FloatingConstraint) drain;
               /*
                * In my notes drainMatrix always appears with - sign. Here, in
                * contrast to the notes, I include the sign in the definition so
                * I can add it instead of subtract it.
                */
               if(floatSet.add(drn)) {
                  /*
                   * drn was not already in our set. Its index is just the
                   * current constraintIndex.
                   */
                  drainMatrix.set(connectionIndex, constraintIndex, -1.);
                  constraintIndex++;
               } else {
                  /*
                   * drn was already in our set. We have to ascertain its index.
                   */
                  int drnIndex = 0;
                  for(final FloatingConstraint d : floatSet)
                     if(d != drn)
                        drnIndex++;
                     else
                        break;
                  drainMatrix.set(connectionIndex, drnIndex, -1.);
               }
            }
         }

         /* Truncate our matrices to the correct size */
         final int floatCount = floatSet.size();
         sourceMatrix = sourceMatrix.getMatrix(0, dim - 1, 0, floatCount - 1);
         drainMatrix = drainMatrix.getMatrix(0, dim - 1, 0, floatCount - 1);
         final Matrix coefficientsPotential = new Matrix(floatCount, floatCount);

         /*
          * Load coefficientsPotential with final potentials (after unit charge
          * flows)
          */
         final FloatingConstraint[] floatsArray = floatSet.toArray(new FloatingConstraint[0]);
         for(int i = 0; i < floatCount; i++) {
            final FloatingConstraint f = floatsArray[i];
            f.incrementCharge(1); // Add 1 charge
            feaRunner.runFEA(r);
            f.incrementCharge(-1); // Remove the 1 charge
            for(int j = 0; j < floatCount; j++)
               coefficientsPotential.set(i, j, floatsArray[j].getPotential());
         }
         /* Compute initial potentials (before charge flows) */
         feaRunner.runFEA(r);
         /*
          * Subtract initial potentials from final ones to obtain changes caused
          * by unit charge changes
          */
         for(int j = 0; j < floatCount; j++) {
            final double V0j = floatsArray[j].getPotential();
            for(int i = 0; i < floatCount; i++)
               coefficientsPotential.set(j, i, coefficientsPotential.get(j, i) - V0j);
         }

         /*
          * We're now in a position to calculate the matrices we need to define
          * the equations
          */
         coefficientsChargesFromBeam = (sourceMatrix.plus(drainMatrix)).times(coefficientsPotential);
         final Matrix transDiff = (sourceMatrix.transpose()).plus(drainMatrix.transpose());
         coefficientsChargesFromCurrent = coefficientsChargesFromBeam.times(transDiff);

         /* Flow charges to establish equilibrium if requested */
         if(equilibrate) {
            boolean connectionFlows = false;
            setV0(new double[floatsArray.length]);
            final Matrix soln = coefficientsChargesFromCurrent.solve(v0.times(-1.));
            for(int i = 0; i < dim; i++) {
               final double q = soln.get(i, 0);
               if(q != 0) {
                  final boolean newFlow = connectionList[i].flowCharge(q);
                  connectionFlows = connectionFlows || newFlow;
               }
            }
            /* If the above moved any charges, run FEA to take account of it */
            if(connectionFlows)
               feaRunner.runFEA(r);
         }
      }

      /*
       * In the following, t is the time, q the charge, and qdot the derivative
       * of charge.
       */
      @Override
      public void computeDerivatives(double t, double[] q, double[] qdot) {
         final Matrix qMatrix = new Matrix(q, dim);
         final Matrix connectionDeltaV = v(qMatrix); // initialize to numerator
         for(int i = 0; i < dim; i++) {
            /*
             * In the following lines, deltaV is the voltage drop across
             * connection i. recipC is effectively 1/capacitance for this
             * connection. Tau/capacitance is resistance of the connection.
             * -deltaV/resistance is the current. The minus sign is because
             * current flows so as to reduce deltaV to 0.
             */
            final double deltaV = connectionDeltaV.get(i, 0);
            final double recipC = coefficientsChargesFromCurrent.get(i, i);
            final double resistance = recipC * connectionList[i].getTau(deltaV);
            qdot[i] = -deltaV / resistance;
         }
      }

      @Override
      public int getDimension() {
         return dim;
      }

      /**
       * Gets the current value assigned to floatSet
       *
       * @return Returns an unmodifiable view of floatSet.
       */
      public Set<FloatingConstraint> getFloatSet() {
         return Collections.unmodifiableSet(floatSet);
      }

      /**
       * Solves the differential equation to determine how much charge flows in
       * each connection in the interval deltat, then updates the connections to
       * move this much charge.
       *
       * @param deltat - time interval
       * @throws MathException
       * @throws MathException
       */
      public void integrate(double deltat)
            throws MathException, MathException {
         /*
          * The following lines set these parameters for the integrator: Min
          * step size = 1.e-5*deltat, Max step size = deltat, Abs tolerance =
          * 1.e-5, Rel tolerance = 1.e-5, Max Evaluations = 10000.
          */
         /*
          * FYI: In tests with dim = 5 tolerance of 1.e-15 for rel and abs
          * required ~250 calls to computeDerivatives in order to converge. With
          * tolerances 1.e-5 it requires ~35. DormandPrince54Integrator required
          * about double the calls to computeDerivatives without noticeable
          * improvement in accuracy. Note that the number of calls to
          * computeDerivatives depends on deltat relative to the relaxation
          * time. As tau gets shorter, the number of calls must go up because q
          * has more curvature. With tolerances 1.e-5 and tau a factor of 8
          * smaller than above, the number of calls was ~150.
          */
         final FirstOrderIntegrator dp54 = new DormandPrince54Integrator(1.e-5 * deltat, deltat, 1.e-5, 1.e-5);
         dp54.setMaxEvaluations(10000);
         final double[] q = new double[dim]; /* q starts at 0 */

         /* Solve the ODE to compute q at t = deltat */
         dp54.integrate(this, 0., q, deltat, q);

         /* Flow the computed number of charges in each connection */
         for(int i = 0; i < dim; i++)
            connectionList[i].flowCharge(q[i]);

      }

      /**
       * Sets the offset voltages (the constant term) that will be used for the
       * ODE equations. The offset voltage is a vector, V0, that is equal to the
       * voltage drops across the connections at the last FEA plus an additional
       * drop that depends linearly on charges added to the regions subject to
       * FloatingConstraint in the intervening interval. The user supplies an
       * array of these charges (in units of e). This method computes and
       * includes the effect of those charges on the potentials.
       *
       * @param beamQ - an array of new charges on the regions subject to
       *           FloatingConstraint. I.e., beamQ[i] is the additional charge
       *           on the region corresponding to the i'th constraint in the
       *           list that can be obtained by calling getFloatSet().
       */
      public void setV0(double[] beamQ) {
         final int numFloats = floatSet.size();
         if(beamQ.length != numFloats)
            throw new EPQFatalException("beamQ (argument of setV0) was length " + beamQ.length + " but must be length "
                  + numFloats);
         final Matrix beamQvector = new Matrix(beamQ, numFloats);
         v0 = coefficientsChargesFromBeam.times(beamQvector);
         for(int i = 0; i < dim; i++)
            v0.set(i, 0, v0.get(i, 0) + connectionList[i].getDeltaV());
      }

      /**
       * Computes the voltage across the connections with additional charge q
       * moved from drain to source.
       *
       * @param q
       * @return
       */
      private Matrix v(Matrix q) {
         return v0.plus(coefficientsChargesFromCurrent.times(q));
      }

   }

   private final MonteCarloSS mMonte;

   private int solnInterval;
   /* # of trajectories run since last FEA solution */
   private int trajCount = 0;
   private long previousElectronID = Electron.getlastIdent();
   private long maxCascade = 0L;
   // private final String feaFolder;
   private boolean feaInitializationRequired = false;
   private IConductionModel conductionModel;
   private final TimeKeeper tk;
   private double previousFEAtime;
   private double chargeMultiplier = 1.;
   private boolean firstCall;
   private int logCPcount;
   private boolean chargesAccumulate = true;
   private boolean logCP = false;
   private IFEArunner feaRunner = null;
   private String feaFolder;
   private String cpDestFolder;

   private ConnectionCurrentsODE[] connectionODEArray;

   /**
    * Default model to be used if the user does not specify a conduction model.
    * This model flows no charges.
    */
   private static final IConductionModel NULL_ConductionModel = new IConductionModel() {
      @Override
      public boolean conductCharges(MeshedRegion meshedRegion, double deltat, IFEArunner feaRunner) {
         return false;
      }

      @Override
      public MeshElementRegion stopRegion(MeshElementRegion startRegion) {
         return startRegion;
      }
   };

   /**
    * Constructs a ChargingListener with feaInitializationRequired = false and
    * without conduction. * @param mcss - The MonteCarloSS instance to which
    * this listener is attached.
    *
    * @param solnInterval - The number of trajectories between FEA solutions.
    * @param feaRunner - An IFEArunner that provides a method to run FEA and
    *           (usually) methods to specify FEA parameters.
    */
   public ChargingListener(MonteCarloSS mcss, int solnInterval, IFEArunner feaRunner) {
      this(mcss, solnInterval, feaRunner, false);
   }

   /**
    * Constructs a ChargingListener without conduction.
    *
    * @param mcss - The MonteCarloSS instance to which this listener is
    *           attached.
    * @param solnInterval - The number of trajectories between FEA solutions.
    * @param feaRunner - An IFEArunner that provides a method to run FEA and
    *           (usually) methods to specify FEA parameters.
    * @param feaInitializationRequired - true if the first FEA should be run at
    *           n = 0, false if the first one should be run at n = solnInterval.
    */
   public ChargingListener(MonteCarloSS mcss, int solnInterval, IFEArunner feaRunner, boolean feaInitializationRequired) {
      this(mcss, solnInterval, feaRunner, feaInitializationRequired, NULL_ConductionModel);
   }

   /**
    * Constructs a ChargingListener.
    *
    * @param mcss - The MonteCarloSS instance to which this listener is
    *           attached.
    * @param solnInterval - The number of trajectories between FEA solutions.
    * @param feaRunner - An IFEArunner that provides a method to run FEA and
    *           (usually) methods to specify FEA parameters.
    * @param feaInitializationRequired - true if the first FEA should be run at
    *           n = 0, false if the first one should be run at n = solnInterval.
    * @param conductionModel - an IConductionModel if there is to be conduction,
    *           null if not.
    */

   public ChargingListener(MonteCarloSS mcss, int solnInterval, IFEArunner feaRunner, boolean feaInitializationRequired, IConductionModel conductionModel) {
      mMonte = mcss;
      this.solnInterval = solnInterval;
      this.feaInitializationRequired = feaInitializationRequired;
      if(conductionModel == null)
         this.conductionModel = NULL_ConductionModel;
      else
         this.conductionModel = conductionModel;
      tk = TimeKeeper.getTimeKeeper();
      firstCall = true;

      this.feaRunner = feaRunner;
   }

   /**
    * <p>
    * Constructs a ChargingListener with default feaRunner,
    * feaInitializationRequired = false and without conduction. The feaRunner
    * determines which solver to use and with which options. To make sure the
    * sample and its constraints have been completely defined, ChargingListener
    * delays the choice of solver until the first time actionPerformed is
    * called. At that point the default depends upon the constraints, as
    * follows:<br>
    * If there are any floating regions we use PETScFEAfunner(feaFolder) with
    * its defaults.<br>
    * Otherwise, if there are any Neumann boundary conditions, we use
    * SparskitFEArunner(feaFolder) with its defaults.<br>
    * Finally, if there are only Dirichlet boundary conditions, we use
    * SparskitFEArunner(feaFolder) with ILU0 preconditioner.
    * </p>
    * <p>
    * Unfortunately, there does not seem to be a single choice of solver and
    * options that is good under all conditions. The above defaults have been
    * found by experience to work well under most circumstances. However, if the
    * solver does not perform well on your particular problem, it may be
    * worthwhile to experiment.
    * </p>
    *
    * @param mcss - The MonteCarloSS instance to which this listener is
    *           attached.
    * @param solnInterval - The number of trajectories between FEA solutions.
    * @param feaFolder - The name of the scratch folder the default feaRunner
    *           should use for communication with the solver.
    * @param feaInitializationRequired - true if the first FEA should be run at
    *           n = 0, false if the first one should be run at n = solnInterval.
    */
   public ChargingListener(MonteCarloSS mcss, int solnInterval, String feaFolder, boolean feaInitializationRequired) {
      this(mcss, solnInterval, null);
      this.feaFolder = feaFolder;
      this.feaInitializationRequired = feaInitializationRequired;
   }

   /**
    * @param event
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent event) {
      final MonteCarloSS mcss = (MonteCarloSS) event.getSource();
      final int eventID = event.getID();
      final Electron el = mcss.getElectron();
      /*
       * There can be events, BeamEnergyChanged or FirstTrajectoryEvent, during
       * MonteCarloSS initialization, at which point there has not yet been an
       * electron. We don't do anything on these events anyway, so if the
       * electron is null, we just return.
       */
      if(el == null)
         return;

      final RegionBase cr = el.getCurrentRegion();

      /*
       * Store the electron's MeshElementRegion in region. If the electron is
       * not in the mesh, region = null.
       */
      MeshElementRegion region = null;
      if(cr instanceof MeshElementRegion)
         region = (MeshElementRegion) cr;

      final ArrayList<MeshedRegion> regionList = getMeshedRegions(mMonte.getChamber());
      final int numRegions = regionList.size();

      if(firstCall) {
         previousFEAtime = tk.getTime();
         if(feaRunner == null)
            feaRunner = defaultFEARunner(feaFolder);
         firstCall = false;

         /* Initializations of each MeshedRegion if necessary */
         connectionODEArray = new ConnectionCurrentsODE[regionList.size()];
         for(int i = 0; i < numRegions; i++) {
            final MeshedRegion r = regionList.get(i);
            final IConstraint[] constraints = r.getConstraintList();
            for(final IConstraint c : constraints)
               if(c instanceof FloatingConstraint)
                  ((FloatingConstraint) c).computeCharge();
            if(r.getConnectionList().length > 0)
               connectionODEArray[i] = new ConnectionCurrentsODE(r);
            else if(feaInitializationRequired) {
               feaRunner.runFEA(r);
               final boolean meshRefined = reInitAfterFEA(r, el, region);
               if(meshRefined && (region != null))
                  // Update region to possibly altered region due to refinement
                  region = (MeshElementRegion) el.getCurrentRegion();
               if(logCP) {
                  final String outname = cpDestFolder + File.separator + "cp0.dat";
                  try {
                     r.getMesh().exportChargeAndPotentials(outname);
                     if(meshRefined) {
                        /*
                         * It's an adaptive mesh and it has changed. We need to
                         * log the new mesh too, in order to be able to
                         * interpret the charge and potentials file we have just
                         * saved.
                         */
                        final String meshFileName = cpDestFolder + File.separator + "mesh0.msh";
                        ((IAdaptiveMesh) r.getMesh().getBasicMesh()).saveMesh(meshFileName);
                     }
                  }
                  catch(final Exception e) {
                     throw new EPQFatalException(e.getMessage());
                  }

               }
            }
         }
         feaInitializationRequired = false; // no longer required
      }

      /*
       * Keep count of how many trajectories we've run. Every solnInterval
       * trajectories, we get a new FEA resolution.
       */
      if(eventID == MonteCarloSS.TrajectoryStartEvent) {
         if(chargesAccumulate)
            trajCount++;
         /*
          * If the user hasn't defined maxCascade, initialize it to permit 1
          * eV/electron
          */
         if(maxCascade == 0)
            maxCascade = (long) (el.getEnergy() / (1. * PhysicalConstants.ElectronCharge));
         final long cascadeSize = el.getIdent() - previousElectronID;
         previousElectronID = el.getIdent();
         if((trajCount == solnInterval) || (cascadeSize > maxCascade)) {
            trajCount = 0; /* reset it for the next count */
            final double timenow = tk.getTime();
            for(int i = 0; i < numRegions; i++) {
               final MeshedRegion r = regionList.get(i);
               /* Do an FEA for new charge distribution */
               boolean meshRefined;
               try {
                  doFEAwithConnectionFlows(r, connectionODEArray[i], timenow - previousFEAtime);
                  meshRefined = reInitAfterFEA(r, el, region);
                  if(meshRefined && (region != null))
                     // Update region to possibly altered region due to
                     // refinement
                     region = (MeshElementRegion) el.getCurrentRegion();
               }
               catch(final Exception e) {
                  throw new EPQFatalException(e.getMessage());
               }

               /* Flow charges if conductionModel so dictates */
               final boolean feaNeeded = conductionModel.conductCharges(r, timenow - previousFEAtime, feaRunner);
               /* redo FEA if charges were moved */
               if(feaNeeded)
                  try {
                     feaRunner.runFEA(r);
                     meshRefined = meshRefined || reInitAfterFEA(r, el, region);
                     if(meshRefined && (region != null))
                        // Update region to possibly altered region due to
                        // refinement
                        region = (MeshElementRegion) el.getCurrentRegion();
                  }
                  catch(final Exception e) {
                     throw new EPQFatalException(e.getMessage());
                  }

               if(logCP) {
                  final String outname = cpDestFolder + File.separator + "cp" + logCPcount + ".dat";
                  try {
                     r.getMesh().exportChargeAndPotentials(outname);
                     if(meshRefined) {
                        /*
                         * It's an adaptive mesh and it has changed. We need to
                         * log the new mesh too, in order to be able to
                         * interpret the charge and potentials file we have just
                         * saved.
                         */
                        final String meshFileName = cpDestFolder + File.separator + "mesh" + logCPcount + ".msh";
                        ((IAdaptiveMesh) r.getMesh().getBasicMesh()).saveMesh(meshFileName);
                     }
                  }
                  catch(final Exception e) {
                     throw new EPQFatalException(e.getMessage());
                  }
                  logCPcount++;
               }
            }
            previousFEAtime = timenow;
         }
         return;
      }

      /*
       * Check whether the electron is in a MeshElementRegion. If not, no action
       * is performed. Unmeshed volumes are assumed to be field free.
       */
      if(region == null)
         return; // Electron is not in the mesh

      switch(eventID) {
         /*
          * When an electron comes to rest (TrajectoryEnd or EndSecondary) in a
          * region where the potential is not constrained, decrement the total
          * charge in its region by e.
          */
         case MonteCarloSS.TrajectoryEndEvent:
         case MonteCarloSS.EndSecondaryEvent:
            if(chargesAccumulate) {
               region = conductionModel.stopRegion(region);
               // region can be null if electron leaves the mesh
               if((region != null) && !region.isConstrained()) {
                  /*
                   * Note that the logic here relies on the fact, currently
                   * true, that the only constraint that can be applied to a
                   * region (i.e., a volume) is the Dirichlet type. For
                   * Dirichlet constraint, there is no charge build-up because
                   * whatever supplies the fixed potential is a source or sink
                   * of charge. This is not necessarily true for other
                   * constraints. E.g., a floating region has no source or sink
                   * of charge. At present, floating constraints are associated
                   * only with the *surfaces* of floating regions, not with
                   * volume elements--so this code is correct. But we have to be
                   * careful if that changes in the future.
                   */
                  final Tetrahedron shape = (Tetrahedron) region.getShape();
                  shape.decrementChargeNumber();
                  assert region.getMaterial().getElementCount() != 0 : "Electron stops in vacuum!";
               }
            }
            break;

         /*
          * When a SE starts in a region with unconstrained potential, increment
          * that region's total charge by e.
          */
         case MonteCarloSS.StartSecondaryEvent:
            if(chargesAccumulate)
               if(!region.isConstrained()) {
                  final Tetrahedron shape = (Tetrahedron) region.getShape();
                  shape.incrementChargeNumber();
                  assert region.getMaterial().getElementCount() != 0 : "Electron emission in vacuum!";
               }
            break;

         /*
          * Each step ends with a scatter or nonscatter event. At these, make
          * corrections to account for E-fields due to charging.
          */
         case MonteCarloSS.ScatterEvent:
         case MonteCarloSS.NonScatterEvent:
            /*
             * Check whether the electron is in a MeshElementRegion. If not, no
             * action is performed. Unmeshed volumes are assumed to be field
             * free.
             */
            final Tetrahedron shape = (Tetrahedron) region.getShape();
            correctTrajectory(el, shape);
            break;

      }
   }

   /**
    * Utility to correct the electron's last step in accordance with the fields
    * in its shape. The correction used is Euler's, wherein only terms up to
    * linear in the step size are retained. This approximation corrects only the
    * electron's energy and velocity at each step. Its present position is not
    * altered (although of course the change in velocity alters its future
    * positions).
    *
    * @param el - the electron
    * @param shape - the shape of the MeshElementRegion it occupies
    */
   private void correctTrajectory(Electron el, Tetrahedron shape) {
      /*
       * This correction is an approximation. When the electron energy is small
       * compared to the potential energy difference across its region it's
       * possible it will do some strange things. For example, it's possible
       * within the approximation for the electron's final kinetic energy to be
       * negative. This should only be possible when the electron's energy is
       * small so that it should be dropped. For it to be correctly dropped it
       * may be necessary for various scattering routines to be tolerant of a
       * negative energy.
       */
      final double kE = el.getEnergy();
      final double stepLen = el.stepLength();
      /*
       * Tiny steps can cause numerical problems in the next line, but
       * corrections are correspondingly tiny. We can just skip them.
       */
      if(stepLen < 1.e-11)
         return;
      final double scaledT;
      /*
       * For kE <= 0. scaleT = 0. This means the new electron direction is
       * determined solely by the electric field lines.
       */
      if(kE > 0.)
         scaledT = (2. * kE) / (PhysicalConstants.ElectronCharge * stepLen);
      else
         scaledT = 0.;
      final double theta = el.getTheta();
      final double sintheta = Math.sin(theta);
      final double phi = el.getPhi();
      final double[] direction = new double[] {
         Math.cos(phi) * sintheta,
         Math.sin(phi) * sintheta,
         Math.cos(theta)
      };
      final double[] scaledFinalVel = new double[3];
      final double[] eField = shape.getEField();
      double mag = 0.; // magnitude of scaledFinalVel
      for(int i = 0; i < 3; i++) {
         scaledFinalVel[i] = (scaledT * direction[i]) - eField[i];
         mag += scaledFinalVel[i] * scaledFinalVel[i];
      }
      mag = Math.sqrt(mag);

      /* Correct the electron's angles to the new direction */
      el.setDirection(Math.acos(scaledFinalVel[2] / mag), Math.atan2(scaledFinalVel[1], scaledFinalVel[0]));

      /* Correct its energy */
      final double initialPot = shape.getPotential(el.getPrevPosition());
      final double finalPot = shape.getPotential(el.getPosition());
      double finalE = kE + (PhysicalConstants.ElectronCharge * (finalPot - initialPot));
      /*
       * In rare cases electrons can get trapped in a potential energy minimum
       * in vacuum. (There's probably a way out, but we don't have time to wait
       * for it to be found.) Such traps are characterized by finalE < 0 (they
       * contain turning points), large electron step count, and the material is
       * vacuum. In such cases we give the electron a little boost.
       */
      if((kE < 0.) && (el.getStepCount() > 1000) && (el.getCurrentRegion().getMaterial().getElementCount() == 0))
         finalE += 0.1 * PhysicalConstants.ElectronCharge;
      el.setEnergy(finalE);
   }

   private IFEArunner defaultFEARunner(String feaFolder) {
      final ArrayList<MeshedRegion> regionList = getMeshedRegions(mMonte.getChamber());
      final int[] constraintCount = {
         0,
         0,
         0
      };
      for(final MeshedRegion r : regionList) {
         final HashMap<Long, IConstraint> constraintmap = r.getConstraintMap();
         final Collection<IConstraint> constraintList = constraintmap.values();
         for(final IConstraint c : constraintList)
            if(c instanceof DirichletConstraint)
               constraintCount[0]++;
            else if(c instanceof NeumannConstraint)
               constraintCount[1]++;
            else if(c instanceof FloatingConstraint)
               constraintCount[2]++;
      }

      if(constraintCount[2] > 0) {
         final PETScFEArunner runner = new PETScFEArunner(feaFolder);
         runner.setChargeMultiplier(chargeMultiplier);
         return runner;
      } else if(constraintCount[1] > 0) {
         final SparskitFEArunner runner = new SparskitFEArunner(feaFolder);
         runner.setChargeMultiplier(chargeMultiplier);
         return runner;
      } else {
         final SparskitFEArunner runner = new SparskitFEArunner(feaFolder);
         runner.setPreconditioner(6);
         runner.setChargeMultiplier(chargeMultiplier);
         return runner;
      }
   }

   private void doFEAwithConnectionFlows(MeshedRegion r, ConnectionCurrentsODE connectionODE, double deltat) {
      if(connectionODE != null) {
         /* Get the list of floating regions in these connections */
         final Set<FloatingConstraint> fset = connectionODE.getFloatSet();
         /* Find how much new charge from the beam on each one */
         final double[] newCharge = new double[fset.size()];
         int i = 0;
         for(final FloatingConstraint f : fset) {
            final int qold = f.getCharge();
            f.computeCharge();
            /*
             * New charge was 0 at previous FEA and is f.getCharge()-qold now.
             * The divide by 2 represents an average of excess charge during
             * this interval. This is crude, but since we don't have details of
             * when the charge arrived, it's the best we can do.
             */
            newCharge[i] = (f.getCharge() - qold) / 2.;
            i++;
         }
         connectionODE.setV0(newCharge);
         try {
            connectionODE.integrate(deltat);
         }
         catch(final MathException e) {
            throw new EPQFatalException("MathException " + e.getMessage());
         }
      } else {
         final IConstraint[] constraints = r.getConstraintList();
         for(final IConstraint c : constraints)
            if(c instanceof FloatingConstraint)
               ((FloatingConstraint) c).computeCharge();
      }
      feaRunner.runFEA(r);
      return;
   }

   /**
    * Gets the current value assigned to chargeMultiplier
    *
    * @return Returns the chargeMultiplier.
    */
   public double getChargeMultiplier() {
      return chargeMultiplier;
   }

   /**
    * Gets the current value assigned to conductionModel
    *
    * @return Returns the conductionModel.
    */
   public IConductionModel getConductionModel() {
      return conductionModel;
   }

   /**
    * Gets the current value assigned to feaRunner
    *
    * @return Returns the feaRunner.
    */
   public IFEArunner getFEARunner() {
      return feaRunner;
   }

   /**
    * Returns a list of all MeshedRegions in startRegion and recursively through
    * its subregions.
    *
    * @param startRegion
    * @return
    */
   private ArrayList<MeshedRegion> getMeshedRegions(RegionBase startRegion) {
      final ArrayList<MeshedRegion> result = new ArrayList<MeshedRegion>();
      if(startRegion instanceof MeshedRegion)
         result.add((MeshedRegion) startRegion);
      else {
         final List<RegionBase> subRegions = startRegion.getSubRegions();
         for(final RegionBase r : subRegions)
            result.addAll(getMeshedRegions(r));
      }
      return result;
   }

   /**
    * Gets the current value assigned to solnInterval
    *
    * @return Returns the solnInterval.
    */
   public int getSolnInterval() {
      return solnInterval;
   }

   /**
    * Gets the current value assigned to chargesAccumulate. chargesAccumulate =
    * true is the default and usual operating mode. In this mode, charge
    * bookkeeping is turned on. Changes of the total charge in mesh volume
    * elements are tracked. At the next finite element analysis, the potentials
    * and fields will reflect the new charge distribution. When
    * chargesAccumulate = false, electrons trajectories are affected by whatever
    * the fields are, but changes to the charge distribution are ignored. The
    * counter that records the number of trajectories since the last FEA is not
    * incremented when chargesAccumlate is false, and no new FEA is run. Setting
    * chargesAccumulate = false can be useful when it is desirable to run a
    * number of trajectories without changing the condition of the mesh, for
    * example to generate a trajectory plot or obtain good statistics for the
    * yield, energy spectrum, etc.
    *
    * @return Returns the chargesAccumulate.
    */
   public boolean isChargesAccumulate() {
      return chargesAccumulate;
   }

   /**
    * Gets the current value assigned to feaInitializationRequired
    *
    * @return Returns the feaInitializationRequired.
    */
   public boolean isFeaInitializationRequired() {
      return feaInitializationRequired;
   }

   /**
    * A utility to do some cleanup chores we must do every time we refine the
    * mesh. It checks whether the mesh was refined. If it was, it reinitializes
    * it. Then, if the electron is in the mesh it either updates the potentials
    * only (if the mesh wasn't refined) or else updates the electron's entire
    * current region (if the mesh was refined.)
    *
    * @param r
    * @param el
    * @param electronRegionInMesh
    * @return
    */
   private boolean reInitAfterFEA(MeshedRegion r, Electron el, MeshElementRegion electronRegionInMesh) {
      final boolean meshRefined = r.initializeIfNeeded();
      if(electronRegionInMesh != null)
         if(meshRefined)
            // Re-find the electron's region in case the tet it was in got
            // refined
            r.resetElectronRegion(el);
         else
            ((Tetrahedron) electronRegionInMesh.getShape()).updatePotentials();
      return meshRefined;
   }

   /**
    * Sets the value assigned to chargeMultiplier. chargeMultiplier is a factor
    * applied to trapped charges before performing finite element analysis. By
    * default, chargeMultiplier = 1. and each trapped electron is counted as a
    * full electron. Under some circumstances it is desirable to count each
    * trapped charge as less than a full charge. For example, to determine the
    * average potential produced by depositing one electron into a pixel, we
    * could deposit 1 electron, do an FEA, repeat 1000 times, and average the
    * results. Alternatively, we could run 1000 electrons, treat each charge as
    * having 1/1000 its usual value, and do a single FEA. To do the latter, set
    * chargeMultiplier = 1/1000. Electrons retain their usual charge when moving
    * in fields. The multiplier only affects how the sources of the field are
    * dealt with.
    *
    * @param chargeMultiplier The value to which to set chargeMultiplier.
    */
   public void setChargeMultiplier(double chargeMultiplier) {
      this.chargeMultiplier = chargeMultiplier;
      if(feaRunner != null)
         feaRunner.setChargeMultiplier(chargeMultiplier);
   }

   /**
    * Sets the value assigned to chargesAccumulate.
    *
    * @param chargesAccumulate The value to which to set chargesAccumulate.
    */
   public void setChargesAccumulate(boolean chargesAccumulate) {
      if(this.chargesAccumulate != chargesAccumulate)
         this.chargesAccumulate = chargesAccumulate;
   }

   /**
    * Sets the value assigned to conductionModel.
    *
    * @param conductionModel The value to which to set conductionModel.
    */
   public void setConductionModel(IConductionModel conductionModel) {
      if(this.conductionModel != conductionModel)
         this.conductionModel = conductionModel;
   }

   /**
    * Turns on logging. When logging is on, a file called cp[n].dat is written
    * to the destination folder designated by the input string. (The folder is
    * created if necessary.) Each time a file is written, n is incremented, so
    * the files are cp1.dat, cp2.dat, etc. A second optional argument can
    * specify that the count starts from some other positive integer value.
    * (This can be useful to avoid overwriting earlier log files when resuming
    * an interrupted simulation.) The log file is generated using the Mesh
    * class's exportChargeAndPotentials method. It is a record of nonzero
    * charges contained in volume elements of the mesh and the potentials on
    * nodes of the mesh. It can be subsequently imported with the Mesh class's
    * importChargeAndPotentials method to recreate the state of the mesh that is
    * stored in the log file.
    *
    * @param destFolder
    */
   public void setCPlog(String destFolder) {
      logCP = true;
      logCPcount = 1;
      cpDestFolder = destFolder;
      /* Create the destination folder if necessary. */
      final File dest = new File(destFolder);
      if(!dest.exists())
         dest.mkdirs();
   }

   public void setCPlog(String destFolder, int n0) {
      if(n0 < 0)
         throw new EPQFatalException("Illegal negative n0 in setCPlog.");
      setCPlog(destFolder);
      logCPcount = n0;
   }

   /**
    * Sets the value assigned to feaInitializationRequired.
    *
    * @param feaInitializationRequired The value to which to set
    *           feaInitializationRequired.
    */
   public void setFeaInitializationRequired(boolean feaInitializationRequired) {
      if(this.feaInitializationRequired != feaInitializationRequired)
         this.feaInitializationRequired = feaInitializationRequired;
   }

   /**
    * Sets the value assigned to feaRunner.
    *
    * @param feaRunner The value to which to set feaRunner.
    */
   public void setFEARunner(IFEArunner feaRunner) {
      if(this.feaRunner != feaRunner)
         this.feaRunner = feaRunner;
   }

   /**
    * Sets the current value assigned to solnInterval
    */
   public void setSolnInterval(int solnInterval) {
      this.solnInterval = solnInterval;
   }

   /**
    * Turns off logging.
    */
   public void stopCPlog() {
      logCP = false;
   }
}

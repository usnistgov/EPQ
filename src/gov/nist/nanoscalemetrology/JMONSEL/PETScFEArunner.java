/**
 * gov.nist.nanoscalemetrology.JMONSEL.PETScFEArunner Created by: John
 * Villarrubia Date: Jun 8, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.File;
import java.io.IOException;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;

/**
 * <p>
 * Runs finite element analysis (FEA) using GetDP compiled with the PETSc linear
 * algebra library. Provides getters and setters for PETSc parameters. Getters
 * and setters are provided for many PETSc options. Most of these have not been
 * tested with JMONSEL. Option names and their default values are documented
 * below in the getters and setters. For more details, see the PETSc
 * documentation. Options listed in the PETSc documentation for which there is
 * no corresponding setter provided here are set to their default values.
 * </p>
 * <p>
 * Convergence: The FEA problem is a linear equation of the form A x = b where A
 * is a matrix and x and b are vectors. At the kth iteration, the residual rk is
 * defined as rk = Norm(b-A xk), where xk is the kth iteration's estimate of x
 * and Norm is the l2-norm. The convergence test is rk &lt; max(rtol*b,atol).
 * The system is deemed to be diverging if rk &gt; divtol*Norm(b). rtol, atol,
 * and divtol (the relative, absolute, and divergence tolerances) are among the
 * parameters that can be set.
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
public class PETScFEArunner implements IFEArunner {

   private final String feaFolder;

   /*
    * Following are the PETSc defaults (not necessarily the same as OUR
    * defaults). If our desired value differs from PETSc default, we must
    * specify it on the command line.
    */
   private final int ksp_gmres_restart_default = 30;
   private final double ksp_richardson_scale_default = 1.0;
   private final boolean ksp_right_pc_default = false;
   private final double ksp_rtol_default = 1.e-12;
   private final double ksp_atol_default = 1.e-50;
   private final double ksp_divtol_default = 1.e5;
   private final int ksp_max_it_default = 100000;

   /* The values assigned below are OUR defaults */
   private int ksp_type = 3;
   private int ksp_gmres_restart = ksp_gmres_restart_default;
   private double ksp_richardson_scale = ksp_richardson_scale_default;
   private int pc_type = 6;
   private boolean ksp_right_pc = ksp_right_pc_default;
   private int pc_factor_levels = 1;
   private boolean ksp_monitor = true;
   private double ksp_rtol = 1.e-10;
   private double ksp_atol = ksp_atol_default;
   private double ksp_divtol = ksp_divtol_default;
   private int ksp_max_it = 4000;
   private boolean log_summary = false;

   private double chargeMultiplier = 1.;

   /**
    * Constructs a PETScFEArunner
    */
   public PETScFEArunner(String feaFolder) {
      this.feaFolder = feaFolder;
      /* Create the temporary folder. */
      (new File(feaFolder)).mkdirs();
   }

   /**
    * @param meshReg
    * @see gov.nist.nanoscalemetrology.JMONSEL.IFEArunner#runFEA(gov.nist.microanalysis.NISTMonte.MeshedRegion)
    */
   @Override
   public void runFEA(MeshedRegion meshReg) {
      // Write GetDP spec files
      final GetDPspec specWriter = new GetDPspec(feaFolder, meshReg);
      specWriter.setChargeMultiplier(chargeMultiplier);

      try {
         specWriter.writeFEAfiles();
      } catch (final IOException err) {
         throw new EPQFatalException(err.getMessage());
      }

      // Delete previous resolution if any
      final File resFile = new File(feaFolder, "sample.res");
      if (resFile.exists())
         resFile.delete();

      /* Assemble the command line */
      final StringBuilder commandLine = new StringBuilder("getdp sample "); // Initialize
      // Append options
      switch (ksp_type) {
         case 1 :
            commandLine.append("-ksp_type richardson");
            break;
         case 2 :
            commandLine.append("-ksp_type chebychev");
            break;
         case 3 :
            commandLine.append("-ksp_type cg");
            break;
         case 4 :
            commandLine.append("-ksp_type gmres");
            break;
         case 5 :
            commandLine.append("-ksp_type tcqmr");
            break;
         case 6 :
            commandLine.append("-ksp_type bcgs");
            break;
         case 7 :
            commandLine.append("-ksp_type cgs");
            break;
         case 8 :
            commandLine.append("-ksp_type tfqmr");
            break;
         case 9 :
            commandLine.append("-ksp_type cr");
            break;
         case 10 :
            commandLine.append("-ksp_type lsqr");
            break;
         case 11 :
            commandLine.append("-ksp_type bicg");
            break;
         default :
            throw new EPQFatalException("Illegal ksp_type");
      }

      switch (pc_type) {
         case 1 :
            commandLine.append(" -pc_type jacobi");
            break;
         case 2 :
            commandLine.append(" -pc_type bjacobi");
            break;
         case 3 :
            commandLine.append(" -pc_type sor");
            break;
         case 4 :
            commandLine.append(" -pc_type eisenstat");
            break;
         case 5 :
            commandLine.append(" -pc_type icc");
            break;
         case 6 :
            commandLine.append(" -pc_type ilu");
            break;
         case 7 :
            commandLine.append(" -pc_type asm");
            break;
         case 8 :
            commandLine.append(" -pc_type ksp");
            break;
         case 9 :
            commandLine.append(" -pc_type composite");
            break;
         case 10 :
            commandLine.append(" -pc_type lu");
            break;
         case 11 :
            commandLine.append(" -pc_type cholesky");
            break;
         case 12 :
            commandLine.append(" -pc_type none");
            break;
         default :
            throw new EPQFatalException("Illegal pc_type");
      }

      commandLine.append(" -pc_factor_levels " + pc_factor_levels);

      /*
       * For options with a PETSc default, if the assigned value = default we
       * can skip specifying it on the command line.
       */
      if (isKsp_right_pc())
         commandLine.append(" -ksp_right_pc");
      if (isKsp_monitor())
         commandLine.append(" -ksp_monitor");
      if (isLog_summary())
         commandLine.append(" -log_summary");
      if (ksp_rtol != ksp_rtol_default)
         commandLine.append(" -ksp_rtol " + ksp_rtol);
      if (ksp_atol != ksp_atol_default)
         commandLine.append(" -ksp_atol " + ksp_atol);
      if (ksp_divtol != ksp_divtol_default)
         commandLine.append(" -ksp_divtol " + ksp_divtol);

      if (ksp_gmres_restart != ksp_gmres_restart_default)
         commandLine.append(" -ksp_gmres_restart " + ksp_gmres_restart);
      if (ksp_richardson_scale != ksp_richardson_scale_default)
         commandLine.append(" -ksp_richardson_scale " + ksp_richardson_scale);
      if (ksp_max_it != ksp_max_it_default)
         commandLine.append(" -ksp_max_it " + ksp_max_it);

      // Append end of command line
      commandLine.append(" -solve EleSta_v");

      /*
       * Do the FEA
       */

      try {
         specWriter.runGetDP(commandLine.toString());
      } catch (final InterruptedException e) {
         throw new EPQFatalException(e.getMessage());
      }
   }

   /**
    * Gets the current value assigned to ksp_type
    *
    * @return Returns the ksp_type.
    */
   public int getKsp_type() {
      return ksp_type;
   }

   /**
    * Sets the value assigned to ksp_type. KSP is the Krylov subspace solver
    * employed by GetDP. Available types are
    * <p>
    * ksp_type: <br>
    * - 1 richardson <br>
    * - 2 chebychev <br>
    * - 3 cg <br>
    * - 4 gmres <br>
    * - 5 tcqmr <br>
    * - 6 bcgs <br>
    * - 7 cgs <br>
    * - 8 tfqmr <br>
    * - 9 cr <br>
    * - 10 lsqr <br>
    * - 11 bicg <br>
    * - default : 3
    * </p>
    *
    * @param ksp_type
    *           The value to which to set ksp_type.
    */
   public void setKsp_type(int ksp_type) {
      if (this.ksp_type != ksp_type)
         this.ksp_type = ksp_type;
   }

   /**
    * Gets the current value assigned to pc_type
    *
    * @return Returns the pc_type.
    */
   public int getPc_type() {
      return pc_type;
   }

   /**
    * Sets the value assigned to pc_type. The PC is the preconditioner used by
    * GetDP. Available types are
    * <p>
    * pc_type: <br>
    * - 1 jacobi <br>
    * - 2 bjacobi <br>
    * - 3 sor <br>
    * - 4 eisenstat <br>
    * - 5 icc <br>
    * - 6 ilu <br>
    * - 7 asm <br>
    * - 8 ksp <br>
    * - 9 composit <br>
    * - 10 lu <br>
    * - 11 cholesky <br>
    * - 12 none <br>
    * - default : 6
    * </p>
    *
    * @param pc_type
    *           The value to which to set pc_type.
    */
   public void setPc_type(int pc_type) {
      if (this.pc_type != pc_type)
         this.pc_type = pc_type;
   }

   /**
    * Gets the current value assigned to pc_factor_levels.
    *
    * @return Returns the pc_factor_levels.
    */
   public int getPc_factor_levels() {
      return pc_factor_levels;
   }

   /**
    * Sets the value assigned to pc_factor_levels. The value must be an integer
    * &gt;= 0. This is the number of fill levels to use in the preconditioner.
    * Higher fill provides better preconditioning but also more memory and more
    * time to compute. Default is 1.
    *
    * @param pc_factor_levels
    *           The value to which to set pc_factor_levels.
    */
   public void setPc_factor_levels(int pc_factor_levels) {
      if (this.pc_factor_levels != pc_factor_levels)
         this.pc_factor_levels = pc_factor_levels;
      if (pc_factor_levels < 0)
         pc_factor_levels = 0;
   }

   /**
    * Gets the current value assigned to ksp_monitor.
    *
    * @return Returns the ksp_monitor.
    */
   public boolean isKsp_monitor() {
      return ksp_monitor;
   }

   /**
    * Sets the value assigned to ksp_monitor. If true (the default) residual
    * norms after each iteration are printed to the out.txt file in the
    * temporary feaFolder. If false, they are not printed.
    *
    * @param ksp_monitor
    *           The value to which to set ksp_monitor.
    */
   public void setKsp_monitor(boolean ksp_monitor) {
      if (this.ksp_monitor != ksp_monitor)
         this.ksp_monitor = ksp_monitor;
   }

   /**
    * Gets the current value assigned to ksp_rtol.
    *
    * @return Returns the ksp_rtol.
    */
   public double getKsp_rtol() {
      return ksp_rtol;
   }

   /**
    * Sets the value assigned to ksp_rtol. Default is 1.e-5.
    *
    * @param ksp_rtol
    *           The value to which to set ksp_rtol.
    */
   public void setKsp_rtol(double ksp_rtol) {
      if (this.ksp_rtol != ksp_rtol)
         this.ksp_rtol = ksp_rtol;
   }

   /**
    * Gets the current value assigned to log_summary
    *
    * @return Returns the log_summary.
    */
   public boolean isLog_summary() {
      return log_summary;
   }

   /**
    * Sets the value assigned to log_summary. When true, a performance summary
    * is included in the out.txt file in feaFolder. Default is false.
    *
    * @param log_summary
    *           The value to which to set log_summary.
    */
   public void setLog_summary(boolean log_summary) {
      if (this.log_summary != log_summary)
         this.log_summary = log_summary;
   }

   /**
    * Gets the current value assigned to ksp_gmres_restart
    *
    * @return Returns the ksp_gmres_restart.
    */
   public int getKsp_gmres_restart() {
      return ksp_gmres_restart;
   }

   /**
    * Sets the value assigned to ksp_gmres_restart. This is the number of
    * iterations at which GMRES, FGMRES and LGMRES restart. Default is 30.
    * Ignored with non GMRES solvers.
    *
    * @param ksp_gmres_restart
    *           The value to which to set ksp_gmres_restart.
    */
   public void setKsp_gmres_restart(int ksp_gmres_restart) {
      if (this.ksp_gmres_restart != ksp_gmres_restart)
         this.ksp_gmres_restart = ksp_gmres_restart;
   }

   /**
    * Gets the current value assigned to ksp_richardson_scale
    *
    * @return Returns the ksp_richardson_scale.
    */
   public double getKsp_richardson_scale() {
      return ksp_richardson_scale;
   }

   /**
    * Sets the value assigned to ksp_richardson_scale. Default = 1. Ignored for
    * non-Richardson KSP types.
    *
    * @param ksp_richardson_scale
    *           The value to which to set ksp_richardson_scale.
    */
   public void setKsp_richardson_scale(double ksp_richardson_scale) {
      if (this.ksp_richardson_scale != ksp_richardson_scale)
         this.ksp_richardson_scale = ksp_richardson_scale;
   }

   /**
    * Gets the current value assigned to ksp_right_pc.
    *
    * @return Returns the ksp_right_pc.
    */
   public boolean isKsp_right_pc() {
      return ksp_right_pc;
   }

   /**
    * Sets the value assigned to ksp_right_pc. If true, a right preconditioner
    * is used. If false (default), preconditioning is from the left.
    *
    * @param ksp_right_pc
    *           The value to which to set ksp_right_pc.
    */
   public void setKsp_right_pc(boolean ksp_right_pc) {
      if (this.ksp_right_pc != ksp_right_pc)
         this.ksp_right_pc = ksp_right_pc;
   }

   /**
    * Gets the current value assigned to ksp_atol
    *
    * @return Returns the ksp_atol.
    */
   public double getKsp_atol() {
      return ksp_atol;
   }

   /**
    * Sets the value assigned to ksp_atol. Default is 1.e-50.
    *
    * @param ksp_atol
    *           The value to which to set ksp_atol.
    */
   public void setKsp_atol(double ksp_atol) {
      if (this.ksp_atol != ksp_atol)
         this.ksp_atol = ksp_atol;
   }

   /**
    * Gets the current value assigned to ksp_divtol.
    *
    * @return Returns the ksp_divtol.
    */
   public double getKsp_divtol() {
      return ksp_divtol;
   }

   /**
    * Sets the value assigned to ksp_divtol. Default is 1.e5.
    *
    * @param ksp_divtol
    *           The value to which to set ksp_divtol.
    */
   public void setKsp_divtol(double ksp_divtol) {
      if (this.ksp_divtol != ksp_divtol)
         this.ksp_divtol = ksp_divtol;
   }

   /**
    * Gets the current value assigned to ksp_max_it
    *
    * @return Returns the ksp_max_it.
    */
   public int getKsp_max_it() {
      return ksp_max_it;
   }

   /**
    * Sets the value assigned to ksp_max_it. This is the maximum number of
    * iterations of the ksp solver. Default is 4000.
    *
    * @param ksp_max_it
    *           The value to which to set ksp_max_it.
    */
   public void setKsp_max_it(int ksp_max_it) {
      if (this.ksp_max_it != ksp_max_it)
         this.ksp_max_it = ksp_max_it;
   }

   @Override
   public void setChargeMultiplier(double chargeMultiplier) {
      this.chargeMultiplier = chargeMultiplier;
   }

   @Override
   public double getChargeMultiplier() {
      return chargeMultiplier;
   }

}

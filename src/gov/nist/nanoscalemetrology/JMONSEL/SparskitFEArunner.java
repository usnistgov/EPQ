/**
 * gov.nist.nanoscalemetrology.JMONSEL.SparskitFEArunner Created by: John
 * Villarrubia Date: Jun 8, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;

/**
 * <p>
 * Runs finite element analysis (FEA) using GetDP compiled with the Sparskit
 * linear algebra library. Provides getters and setters for sparskit parameters.
 * Following is a list of parameters and their default values:
 * <p>
 * Matrix_Format 1<br>
 * Matrix_Printing 0<br>
 * Matrix_Storage 0<br>
 * Scaling 0<br>
 * Renumbering_Technique 1<br>
 * Preconditioner 2<br>
 * Preconditioner_Position 2<br>
 * Nb_Fill 10<br>
 * Permutation_Tolerance 0.05<br>
 * Dropping_Tolerance 0<br>
 * Diagonal_Compensation 0<br>
 * Re_Use_ILU 0<br>
 * Algorithm 8<br>
 * Krylov_Size 120<br>
 * IC_Acceleration 1<br>
 * Re_Use_LU 0<br>
 * Iterative_Improvement 0<br>
 * Nb_Iter_Max 4000<br>
 * Stopping_Test 1e-010<br>
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author JohnVillarrubia
 * @version 1.0
 */
public class SparskitFEArunner implements IFEArunner {

	private int matrixFormat = 1;
	private int matrixPrinting = 0;
	private int matrixStorage = 0;
	private int scaling = 0;
	private int renumberingTechnique = 1;
	private int preconditioner = 2;
	private int preconditionerPosition = 2;
	private int nbFill = 10;
	private double permutationTolerance = 0.05;
	private int droppingTolerance = 0;
	private int diagonalCompensation = 0;
	private int reuseILU = 0;
	private int algorithm = 8;
	private int krylovSize = 120;
	private int ICAcceleration = 1;
	private int reuseLU = 0;
	private int iterativeImprovement = 0;
	private int nbIterMax = 4000;
	private double stoppingTest = 1e-010;
	private final String feaFolder;

	private double chargeMultiplier = 1.;

	/**
	 * Constructs a SparskitFEArunner
	 */
	public SparskitFEArunner(String feaFolder) {
		this.feaFolder = feaFolder;
		/* Create the temporary folder. */
		(new File(feaFolder)).mkdirs();
	}

	/**
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

		// Write solver parameter file
		try {
			writeSolverPar();
		} catch (final IOException err) {
			throw new EPQFatalException("Exception attempting to write solver.par: " + err.getMessage());
		}

		// Delete previous resolution if any
		final File resFile = new File(feaFolder, "sample.res");
		if (resFile.exists())
			resFile.delete();

		/*
		 * Do the FEA
		 */

		final String command = "getdp_sparskit sample -pre EleSta_v -cal";
		try {
			specWriter.runGetDP(command);
		} catch (final InterruptedException e) {
			throw new EPQFatalException(e.getMessage());
		}

	}

	private void writeSolverPar() throws IOException {
		/* Open resource for input */
		BufferedReader in = null;
		BufferedWriter out = null;

		try {
			in = new BufferedReader(new InputStreamReader(Mesh.class.getResourceAsStream("solver.par")));
			out = new BufferedWriter(new FileWriter(feaFolder + File.separator + "solver.par"));

			/* Copy the comments from resource to output */
			String line = in.readLine();
			while (line != null) {
				out.write(line);
				out.newLine();
				line = in.readLine();
			}

			/* Append our chosen parameter values */
			out.write("            Matrix_Format            " + getMatrixFormat());
			out.newLine();
			out.write("          Matrix_Printing            " + getMatrixPrinting());
			out.newLine();
			out.write("           Matrix_Storage            " + getMatrixStorage());
			out.newLine();
			out.write("                  Scaling            " + getScaling());
			out.newLine();
			out.write("    Renumbering_Technique            " + getRenumberingTechnique());
			out.newLine();
			out.write("           Preconditioner            " + getPreconditioner());
			out.newLine();
			out.write("  Preconditioner_Position            " + getPreconditionerPosition());
			out.newLine();
			out.write("                  Nb_Fill            " + getNbFill());
			out.newLine();
			out.write("    Permutation_Tolerance            " + getPermutationTolerance());
			out.newLine();
			out.write("       Dropping_Tolerance            " + getDroppingTolerance());
			out.newLine();
			out.write("    Diagonal_Compensation            " + getDiagonalCompensation());
			out.newLine();
			out.write("               Re_Use_ILU            " + getReuseILU());
			out.newLine();
			out.write("                Algorithm            " + getAlgorithm());
			out.newLine();
			out.write("              Krylov_Size            " + getKrylovSize());
			out.newLine();
			out.write("          IC_Acceleration            " + getICAcceleration());
			out.newLine();
			out.write("                Re_Use_LU            " + getReuseLU());
			out.newLine();
			out.write("    Iterative_Improvement            " + getIterativeImprovement());
			out.newLine();
			out.write("              Nb_Iter_Max            " + getNbIterMax());
			out.newLine();
			out.write("            Stopping_Test            " + getStoppingTest());
			out.newLine();
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	/**
	 * Gets the current value assigned to matrixFormat
	 *
	 * @return Returns the matrixFormat.
	 */
	public int getMatrixFormat() {
		return matrixFormat;
	}

	/**
	 * Sets the value assigned to matrixFormat.
	 * <p>
	 * Matrix_Format (Integer): <br>
	 * - 1 Sparse <br>
	 * - 2 Full <br>
	 * - default : 1<br>
	 * </p>
	 *
	 * @param matrixFormat The value to which to set matrixFormat.
	 */
	public void setMatrixFormat(int matrixFormat) {
		if (this.matrixFormat != matrixFormat)
			this.matrixFormat = matrixFormat;
	}

	/**
	 * Gets the current value assigned to matrixPrinting
	 * <p>
	 * Matrix_Printing (Integer): Disk write ('fort.*') <br>
	 * - 1 matrix (csr) <br>
	 * - 2 preconditioner (msr) <br>
	 * - 3 both <br>
	 * - default : 0
	 * </p>
	 *
	 * @return Returns the matrixPrinting.
	 */
	public int getMatrixPrinting() {
		return matrixPrinting;
	}

	/**
	 * Sets the value assigned to matrixPrinting.
	 *
	 * @param matrixPrinting The value to which to set matrixPrinting.
	 */
	public void setMatrixPrinting(int matrixPrinting) {
		if (this.matrixPrinting != matrixPrinting)
			this.matrixPrinting = matrixPrinting;
	}

	/**
	 * Gets the current value assigned to matrixStorage
	 *
	 * @return Returns the matrixStorage.
	 */
	public int getMatrixStorage() {
		return matrixStorage;
	}

	/**
	 * Sets the value assigned to matrixStorage.
	 * <p>
	 * Matrix_Storage (Integer): Disk Write or Read in internal format <br>
	 * - 0 none <br>
	 * - 1 write matrix (sparse) <br>
	 * - 2 read matrix (sparse) <br>
	 * - default : 0
	 * </p>
	 *
	 * @param matrixStorage The value to which to set matrixStorage.
	 */
	public void setMatrixStorage(int matrixStorage) {
		if (this.matrixStorage != matrixStorage)
			this.matrixStorage = matrixStorage;
	}

	/**
	 * Gets the current value assigned to scaling
	 *
	 * @return Returns the scaling.
	 */
	public int getScaling() {
		return scaling;
	}

	/**
	 * Sets the value assigned to scaling.
	 * <p>
	 * Scaling (Integer): Scale system <br>
	 * - 0 no <br>
	 * - 1 on basis of diagonal elements (no loss of possible symmetry) <br>
	 * - 2 on basis of inf. norm of first rows and then columns (asymmetric) <br>
	 * - 3 on basis of norm 1 of first rows and then columns (asymmetric) <br>
	 * - 4 on basis of norm 2 of first rows and then columns (asymmetric) <br>
	 * - default : 0
	 * </p>
	 *
	 * @param scaling The value to which to set scaling.
	 */
	public void setScaling(int scaling) {
		if (this.scaling != scaling)
			this.scaling = scaling;
	}

	/**
	 * Gets the current value assigned to renumberingTechnique
	 *
	 * @return Returns the renumberingTechnique.
	 */
	public int getRenumberingTechnique() {
		return renumberingTechnique;
	}

	/**
	 * Sets the value assigned to renumberingTechnique.
	 * <p>
	 * Renumbering_Technique (Integer): <br>
	 * - 0 No renumbering <br>
	 * - 1 Reverse Cuthill-Mc Kee <br>
	 * - default : 1
	 * </p>
	 *
	 * @param renumberingTechnique The value to which to set renumberingTechnique.
	 */
	public void setRenumberingTechnique(int renumberingTechnique) {
		if (this.renumberingTechnique != renumberingTechnique)
			this.renumberingTechnique = renumberingTechnique;
	}

	/**
	 * Gets the current value assigned to preconditioner
	 *
	 * @return Returns the preconditioner.
	 */
	public int getPreconditioner() {
		return preconditioner;
	}

	/**
	 * Sets the value assigned to preconditioner.
	 * <p>
	 * Preconditioner (Integer): <br>
	 * - 0 NONE No Factorization <br>
	 * - 1 ILUT Incomplete LU factorization with dual truncation strategy <br>
	 * - 2 ILUTP ILUT with column pivoting <br>
	 * - 3 ILUD ILU with single dropping + diagonal compensation (~MILUT) <br>
	 * - 4 ILUDP ILUD with column pivoting <br>
	 * - 5 ILUK level-k ILU <br>
	 * - 6 ILU0 simple ILU(0) preconditioning <br>
	 * - 7 MILU0 MILU(0) preconditioning <br>
	 * - 8 DIAGONAL <br>
	 * - default : 2
	 * </p>
	 *
	 * @param preconditioner The value to which to set preconditioner.
	 */
	public void setPreconditioner(int preconditioner) {
		if (this.preconditioner != preconditioner)
			this.preconditioner = preconditioner;
	}

	/**
	 * Gets the current value assigned to preconditionerPosition
	 *
	 * @return Returns the preconditionerPosition.
	 */
	public int getPreconditionerPosition() {
		return preconditionerPosition;
	}

	/**
	 * Sets the value assigned to preconditionerPosition.
	 * <p>
	 * Preconditioner_Position (Integer): <br>
	 * - 0 No Preconditioner <br>
	 * - 1 Left Preconditioner <br>
	 * - 2 Right Preconditioner <br>
	 * - 3 Both Left and Right Preconditioner <br>
	 * - default : 2
	 * </p>
	 *
	 * @param preconditionerPosition The value to which to set
	 *                               preconditionerPosition.
	 */
	public void setPreconditionerPosition(int preconditionerPosition) {
		if (this.preconditionerPosition != preconditionerPosition)
			this.preconditionerPosition = preconditionerPosition;
	}

	/**
	 * Gets the current value assigned to nbFill
	 *
	 * @return Returns the nbFill.
	 */
	public int getNbFill() {
		return nbFill;
	}

	/**
	 * Sets the value assigned to nbFill.
	 * <p>
	 * Nb_Fill (Integer): <br>
	 * - ILUT/ILUTP : maximum number of elements per line of L and U (except
	 * diagonal element) <br>
	 * - ILUK : each element whose fill-in level is greater than NB_FILL is dropped.
	 * <br>
	 * - default : 10
	 * </p>
	 *
	 * @param nbFill The value to which to set nbFill.
	 */
	public void setNbFill(int nbFill) {
		if (this.nbFill != nbFill)
			this.nbFill = nbFill;
	}

	/**
	 * Gets the current value assigned to permutationTolerance
	 *
	 * @return Returns the permutationTolerance.
	 */
	public double getPermutationTolerance() {
		return permutationTolerance;
	}

	/**
	 * Sets the value assigned to permutationTolerance.
	 * <p>
	 * Permutation_Tolerance (Real): Tolerance for column permutation in
	 * ILUTP/ILUDP. At stage i, columns i and j are permuted if
	 * abs(a(i,j))*PERMUTATION_TOLERANCE &gt; abs(a(i,i)). <br>
	 * - 0 no permutations <br>
	 * - 0.001 -&gt; 0.1 classical <br>
	 * - default : 0.05
	 * </p>
	 *
	 * @param permutationTolerance The value to which to set permutationTolerance.
	 */
	public void setPermutationTolerance(double permutationTolerance) {
		if (this.permutationTolerance != permutationTolerance)
			this.permutationTolerance = permutationTolerance;
	}

	/**
	 * Gets the current value assigned to droppingTolerance
	 *
	 * @return Returns the droppingTolerance.
	 */
	public int getDroppingTolerance() {
		return droppingTolerance;
	}

	/**
	 * Sets the value assigned to droppingTolerance.
	 * <p>
	 * Dropping_Tolerance (Real): <br>
	 * - ILUT/ILUTP/ILUK: a(i,j) is dropped if abs(a(i,j)) &lt; DROPPING_TOLERANCE *
	 * abs(diagonal element in U). <br>
	 * - ILUD/ILUDP : a(i,j) is dropped if abs(a(i,j)) &lt; DROPPING_TOLERANCE *
	 * [weighted norm of line i]. Weighted norm = 1-norm / number of nonzero
	 * elements on the line. <br>
	 * - default : 0
	 * </p>
	 *
	 * @param droppingTolerance The value to which to set droppingTolerance.
	 */
	public void setDroppingTolerance(int droppingTolerance) {
		if (this.droppingTolerance != droppingTolerance)
			this.droppingTolerance = droppingTolerance;
	}

	/**
	 * Gets the current value assigned to diagonalCompensation
	 *
	 * @return Returns the diagonalCompensation.
	 */
	public int getDiagonalCompensation() {
		return diagonalCompensation;
	}

	/**
	 * Sets the value assigned to diagonalCompensation.
	 * <p>
	 * Diagonal_Compensation (Real): ILUD/ILUDP: the term 'DIAGONAL_COMPENSATION *
	 * (sum of all dropped elements of the line)' is added to the diagonal element
	 * in U <br>
	 * - 0 ~ ILU with threshold <br>
	 * - 1 ~ MILU with threshold. <br>
	 * - default : 0
	 * </p>
	 *
	 * @param diagonalCompensation The value to which to set diagonalCompensation.
	 */
	public void setDiagonalCompensation(int diagonalCompensation) {
		if (this.diagonalCompensation != diagonalCompensation)
			this.diagonalCompensation = diagonalCompensation;
	}

	/**
	 * Gets the current value assigned to reuseILU
	 *
	 * @return Returns the reuseILU.
	 */
	public int getReuseILU() {
		return reuseILU;
	}

	/**
	 * Sets the value assigned to reuseILU.
	 * <p>
	 * Re_Use_ILU (Integer): Reuse ILU decomposition (and renumbering if any) <br>
	 * - 0 no <br>
	 * - 1 yes <br>
	 * - default : 0
	 * </p>
	 *
	 * @param reuseILU The value to which to set reuseILU.
	 */
	public void setReuseILU(int reuseILU) {
		if (this.reuseILU != reuseILU)
			this.reuseILU = reuseILU;
	}

	/**
	 * Gets the current value assigned to algorithm
	 *
	 * @return Returns the algorithm.
	 */
	public int getAlgorithm() {
		return algorithm;
	}

	/**
	 * Sets the value assigned to algorithm.
	 * <p>
	 * Algorithm (Integer): <br>
	 * - 1 CG Conjugate Gradient <br>
	 * - 2 CGNR CG (Normal Residual equation) <br>
	 * - 3 BCG Bi-Conjugate Gradient <br>
	 * - 4 DBCG BCG with partial pivoting <br>
	 * - 5 BCGSTAB BCG stabilized <br>
	 * - 6 TFQMR Transpose-Free Quasi-Minimum Residual <br>
	 * - 7 FOM Full Orthogonalization Method <br>
	 * - 8 GMRES Generalized Minimum RESidual <br>
	 * - 9 FGMRES Flexible version of GMRES <br>
	 * - 10 DQGMRES Direct versions of GMRES <br>
	 * - 11 LU LU Factorization <br>
	 * - 12 PGMRES Alternative version of GMRES <br>
	 * - default : 8
	 * </p>
	 *
	 * @param algorithm The value to which to set algorithm.
	 */
	public void setAlgorithm(int algorithm) {
		if (this.algorithm != algorithm)
			this.algorithm = algorithm;
	}

	/**
	 * Gets the current value assigned to krylovSize
	 *
	 * @return Returns the krylovSize.
	 */
	public int getKrylovSize() {
		return krylovSize;
	}

	/**
	 * Sets the value assigned to krylovSize.
	 * <p>
	 * Krylov_Size (Integer): Krylov subspace size <br>
	 * - default : 120
	 * </p>
	 *
	 * @param krylovSize The value to which to set krylovSize.
	 */
	public void setKrylovSize(int krylovSize) {
		if (this.krylovSize != krylovSize)
			this.krylovSize = krylovSize;
	}

	/**
	 * Gets the current value assigned to iCAcceleration
	 *
	 * @return Returns the iCAcceleration.
	 */
	public int getICAcceleration() {
		return ICAcceleration;
	}

	/**
	 * <p>
	 * Sets the value assigned to iCAcceleration.
	 * </p>
	 * <p>
	 * IC_Acceleration (Real): IC accelerator <br>
	 * - default : 1
	 * </p>
	 *
	 * @param ICAcceleration The value to which to set ICAcceleration.
	 */
	public void setICAcceleration(int ICAcceleration) {
		if (this.ICAcceleration != ICAcceleration)
			this.ICAcceleration = ICAcceleration;
	}

	/**
	 * Gets the current value assigned to reuseLU
	 *
	 * @return Returns the reuseLU.
	 */
	public int getReuseLU() {
		return reuseLU;
	}

	/**
	 * Sets the value assigned to reuseLU.
	 * <p>
	 * Re_Use_LU (Integer): Reuse LU decomposition <br>
	 * - 0 no <br>
	 * - 1 yes <br>
	 * - default : 0
	 * </p>
	 *
	 * @param reuseLU The value to which to set reuseLU.
	 */
	public void setReuseLU(int reuseLU) {
		if (this.reuseLU != reuseLU)
			this.reuseLU = reuseLU;
	}

	/**
	 * Gets the current value assigned to iterativeImprovement
	 *
	 * @return Returns the iterativeImprovement.
	 */
	public int getIterativeImprovement() {
		return iterativeImprovement;
	}

	/**
	 * Sets the value assigned to iterativeImprovement.
	 * <p>
	 * Iterative_Improvement (Integer): Iterative improvement of the solution
	 * obtained by a LU <br>
	 * - default : 0
	 * </p>
	 *
	 * @param iterativeImprovement The value to which to set iterativeImprovement.
	 */
	public void setIterativeImprovement(int iterativeImprovement) {
		if (this.iterativeImprovement != iterativeImprovement)
			this.iterativeImprovement = iterativeImprovement;
	}

	/**
	 * Gets the current value assigned to nbIterMax
	 *
	 * @return Returns the nbIterMax.
	 */
	public int getNbIterMax() {
		return nbIterMax;
	}

	/**
	 * Sets the value assigned to nbIterMax.
	 * <p>
	 * Nb_Iter_Max (Integer): Maximum number of iterations <br>
	 * - default : 4000
	 * </p>
	 *
	 * @param nbIterMax The value to which to set nbIterMax.
	 */
	public void setNbIterMax(int nbIterMax) {
		if (this.nbIterMax != nbIterMax)
			this.nbIterMax = nbIterMax;
	}

	/**
	 * Gets the current value assigned to stoppingTest
	 *
	 * @return Returns the stoppingTest.
	 */
	public double getStoppingTest() {
		return stoppingTest;
	}

	/**
	 * Sets the value assigned to stoppingTest.
	 * <p>
	 * Stopping_Test (Real): Target relative residual <br>
	 * - default : 1e-010
	 * </p>
	 *
	 * @param stoppingTest The value to which to set stoppingTest.
	 */
	public void setStoppingTest(double stoppingTest) {
		if (this.stoppingTest != stoppingTest)
			this.stoppingTest = stoppingTest;
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

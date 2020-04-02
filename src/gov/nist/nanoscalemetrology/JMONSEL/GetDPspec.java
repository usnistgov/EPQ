/**
 * gov.nist.nanoscalemetrology.JMONSEL.GetDPspec Created by: jvillar Date: Jun
 * 8, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.IMaterialScatterModel;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.DirichletConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.FloatingConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.IConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.NeumannConstraint;

/**
 * <p>
 * Writes problem specification files needed by the GetDP solver and invokes the
 * solver to compute FEA solution. Users of JMONSEL do not ordinarily need to
 * access this class directly. Rather, it is a utility class employed by
 * IFEArunner instances that use GetDP as the solver. The IFEArunner instance's
 * runFEA() method ordinarily follows a sequence like this:
 * </p>
 * <p>
 * Construct a GetDPspec instance. Each instance has its own MeshedRegion and
 * folder for scratch files (used to communicate between JMONSEL and GetDP).<br>
 * Use the writeFEA() method to cause problem specification files to be written
 * to the scratch folder.
 * </p>
 * <p>
 * Write a solver options file if needed. (GetDP compiled with some linear
 * algebra libraries uses a text file to specify solver options. Other linear
 * algebra libraries communicate options via the command line.)
 * </p>
 * <p>
 * Construct the appropriate command line to invoke GetDP. (Include options in
 * the command if required.) Use the runGetDP() method to compute the FEA
 * solution and import it into JMONSEL.
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
public class GetDPspec {

	/**
	 * <p>
	 * StreamToFile is a utility class that is used by MeshedRegion to redirect
	 * output streams (stderr and stdout) to files. Each instance runs on its own
	 * thread to prevent MeshedRegion from hanging due to a full buffer.
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
	private class StreamToFile extends Thread {
		InputStream is;
		File outputFile;

		StreamToFile(InputStream is, File outputFile) {
			this.is = is;
			this.outputFile = outputFile;
		}

		@Override
		public void run() {
			PrintStream out = null;
			try {
				final InputStreamReader isr = new InputStreamReader(is);
				final BufferedReader br = new BufferedReader(isr);
				out = new PrintStream(outputFile);
				String line = null;
				while ((line = br.readLine()) != null)
					out.println(line);
			} catch (final IOException ioe) {
				throw new EPQFatalException("IOException: " + ioe.getMessage());
			} finally {
				if (out != null)
					out.close();
			}
		}
	}

	private final String feaFolder;
	private final MeshedRegion meshedRegion;

	private double chargeMultiplier = 1.;

	/**
	 * Constructs a GetDPspec
	 */
	public GetDPspec(String feaFolder, MeshedRegion meshedRegion) {
		super();
		this.feaFolder = feaFolder;
		this.meshedRegion = meshedRegion;
	}

	/**
	 * Gets the current value assigned to chargeMultiplier
	 *
	 * @return Returns the chargeMultiplier.
	 */
	public double getChargeMultiplier() {
		return chargeMultiplier;
	}

	private double[] readRes(String resolutionFileName, int numVals) throws FileNotFoundException {
		final Scanner s = new Scanner(new BufferedReader(new FileReader(resolutionFileName)));
		s.useLocale(Locale.US);
		try {
			/* First section is ResFormat */
			final String str = s.next();
			if (!str.equals("$ResFormat"))
				throw new EPQFatalException("1st token of resolution file was not $ResFormat");
			while (!s.next().equals("$Solution"))
				; // skip to beginning of data section
			s.nextLine(); // skip the rest of this line
			for (int i = 0; i < 4; i++)
				s.nextInt(); // read 4 integers
			final double[] result = new double[numVals];
			for (int i = 0; i < numVals; i++) {
				result[i] = s.nextDouble();
				s.nextLine(); // skip any remaining entries
			}
			s.close();
			return result;
		} finally {
			if (s != null)
				s.close();
		}
	}

	/**
	 * Reads FEA results produced by getdp. Getdp results are in two files. One is a
	 * preresolution (*.pre) file that provides a descriptive line for each equation
	 * in the FEA problem, both those that relate to nodes with unknown (to be
	 * solved for) potentials and those with fixed potentials. The other is a
	 * resolution (*.res) file that provides the solved values of potentials at the
	 * unknown nodes. This form of readResolution() can be used when the two files
	 * have extensions .pre and .res and are otherwise the same. The argument is the
	 * common stem.
	 *
	 * @param fileNameStem
	 */
	private void readResolution(String fileNameStem) throws FileNotFoundException {
		readResolution(fileNameStem + ".pre", fileNameStem + ".res");
	}

	/**
	 * Reads finite element analysis results produced by getdp. Getdp results are in
	 * two files. One is a .pre preresolution file that provides a descriptive line
	 * for each equation in the FEA problem, both those that relate to nodes with
	 * unknown (to be solved for) potentials and those with fixed potentials. The
	 * other is a .res resolution file that provides the solved values of potentials
	 * at the unknown nodes. The inputs are strings providing paths and names for
	 * each of these.
	 * </p>
	 * <p>
	 * Note that this method updates the solution values (potentials) for nodes of
	 * the mesh, but it does not update cached values (e.g., in any already
	 * instantiated Elements of the mesh) that depend on them. The calling routine
	 * is responsible for any such updates, as needed.
	 *
	 * @param preResolutionFileName
	 * @param resolutionFileName
	 */
	private void readResolution(String preResolutionFileName, String resolutionFileName) throws FileNotFoundException {
		final Mesh mesh = meshedRegion.getMesh();
		final Scanner s = new Scanner(new BufferedReader(new FileReader(preResolutionFileName)));
		s.useLocale(Locale.US);
		try {
			/* First section is Resolution */
			final String str = s.next();
			if (!str.equals("$Resolution"))
				throw new EPQFatalException("1st File token of preresolution file was not $Resolution");
			while (!s.next().equals("$DofData"))
				; // skip to beginning of data section
			for (int i = 0; i < 5; i++)
				s.nextLine(); // skip the rest of this line + next 4
			final int numEntities = s.nextInt();
			final int numEqn = s.nextInt(); // The number of unconstrained
											// potentials
			/* Read potentials. Note that potentials[] is 0-offset. */
			final double[] potentials = readRes(resolutionFileName, numEqn);
			/*
			 * read lines from pre file and interleave constrained and unconstrained values
			 * as it dictates.
			 */
			for (int i = 1; i <= numEntities; i++) {
				final int basisfun = s.nextInt();
				final int entitynum = s.nextInt();
				s.nextInt(); // unused "harmonic"
				final int type = s.nextInt();
				switch (basisfun) {
				case 1: // Single node basis functions
					switch (type) {
					case 1: // Unknown potential
						final int eqNum = s.nextInt();
						mesh.setNodePotential(entitynum, potentials[eqNum - 1]);
						s.nextLine(); // skip unused nnz
						break;
					case 2:
						mesh.setNodePotential(entitynum, s.nextDouble());
						s.nextLine(); // skip unused time-function-number
						break;
					default:
						throw new EPQFatalException(
								"Unexpected dof type = " + Integer.toString(type) + " in " + preResolutionFileName);
					}
					break;
				case 2: // grouped nodes basis function
					switch (type) {
					case 1: // Unknown potential
						final int eqNum = s.nextInt();
						double val = potentials[eqNum - 1]; // group voltage
						/*
						 * Assign val to all nodes of each element in the appropriate group
						 */

						for (int j = 1, noE = mesh.getNumberOfElements(); j <= noE; j++)
							if (mesh.getTags(j)[0] == entitynum) {
								final int[] nodes = mesh.getNodeIndices(j); // This
																			// element's
								// nodes
								for (final int node : nodes)
									mesh.setNodePotential(node, val);
							}
						s.nextLine(); // skip unused nnz
						break;
					case 3:
						s.nextInt(); // skip unused associated dof
						val = s.nextDouble();
						/*
						 * Assign val to all nodes of each element in the appropriate group
						 */
						for (int j = 1, noE = mesh.getNumberOfElements(); j <= noE; j++)
							if (mesh.getTags(j)[0] == entitynum) {
								final int[] nodes = mesh.getNodeIndices(j); // This
																			// element's
								// nodes
								for (final int node : nodes)
									mesh.setNodePotential(node, val);
							}
						s.nextLine(); // skip unused time-function-number
						break;
					default:
						throw new EPQFatalException(
								"Unexpected dof type = " + Integer.toString(type) + " in " + preResolutionFileName);
					}
					break;

				default: /*
							 * There can be others (e.g., associated global quantities like charge) but
							 * there's currently nothing to be done with them. We just read to the end of
							 * the line.
							 */
					s.nextLine();
					break;
				}
			}
		} finally {
			if (s != null)
				s.close();
		}

		/* After reading new potentials, update all existing tetrahedra */
		// updateAllPotentials();

		/*
		 * updateAllPotentials(); was a previous strategy, but it may be a net waste of
		 * time to update a very large cache of tetrahedra merely on the chance that
		 * some of them will be reused before the next update. Current strategy is to
		 * delete the cache, forcing it to start over. There is more overhead for each
		 * tet that we end up re-using (because we must instantiate it in addition to
		 * updating its potentials) but we'll do this to fewer tets.
		 */
		mesh.clearElementsCache();
	}

	/**
	 * Runs an instance of GetDP using specification files in feaFolder (the
	 * constructor argument). Waits for it to complete, then reads its resolution
	 * file. The command argument is the command line that is to be executed.
	 * Normally this is something like "getdp_sparskit sample -pre EleSta_v -cal",
	 * possibly with additional command line options. runGetDP is ordinarily called
	 * from an IFEArunner, which manages the user's desired FEA options and
	 * constructs the appropriate corresponding command line by which to invoke
	 * GetDP.
	 *
	 * @param command
	 * @throws InterruptedException
	 */
	public void runGetDP(String command) throws InterruptedException {
		// Execute it and check for errors
		final Runtime runtime = Runtime.getRuntime();
		Process child = null;
		try {
			child = runtime.exec(command, null, new File(feaFolder));

			// Handle errors
			final StreamToFile errorStreamHandler = new StreamToFile(child.getErrorStream(),
					new File(feaFolder, "errOut.txt"));

			// Handle output
			final StreamToFile outputStreamHandler = new StreamToFile(child.getInputStream(),
					new File(feaFolder, "out.txt"));

			// kick them off
			errorStreamHandler.start();
			outputStreamHandler.start();

		} catch (final Exception err) {
			throw new EPQFatalException("Error launching FEA: " + err.getMessage());
		}
		if (child.waitFor() != 0)
			throw new EPQFatalException("Abnormal termination of FEA");

		try {
			readResolution(feaFolder + File.separator + "sample");
			/*
			 * Make interior of any floating regions consistent with the surface.
			 */
			final IConstraint[] constraintList = meshedRegion.getConstraintList();
			for (final IConstraint con : constraintList)
				if (con instanceof FloatingConstraint)
					((FloatingConstraint) con).setVolumePotential();
		} catch (final FileNotFoundException err) {
			throw new EPQFatalException(err.getMessage());
		}
	}

	/**
	 * Sets the value assigned to chargeMultiplier. A charge of n in the mesh is
	 * interpreted as n*e*chargeMultiplier, where e is the absolute value of the
	 * electronic charge.
	 *
	 * @param chargeMultiplier The value to which to set chargeMultiplier.
	 */
	public void setChargeMultiplier(double chargeMultiplier) {
		this.chargeMultiplier = chargeMultiplier;
	}

	public void writeFEAfiles() throws IOException {

		try {
			writeResource("IntegrationLib.pro", "Integration_lib.pro");
		} catch (final Exception err) {
			throw new EPQFatalException(
					"Exception attempting to copy IntegrationLib.pro to Integration_lib.pro: " + err.getMessage());
		}

		try {
			writeResource("JacobianLib.pro", "Jacobian_lib.pro");
		} catch (final Exception err) {
			throw new EPQFatalException(
					"Exception attempting to copy JacobianLib.pro to Jacobian_lib.pro: " + err.getMessage());
		}

		try {
			writeMesh();
		} catch (final Exception err) {
			throw new EPQFatalException("Exception in writeMesh: " + err.getMessage());
		}
	}

	/**
	 * Writes a problem definition file and a corresponding modified mesh file to
	 * the designated destination folder. For a complete GetDP problem specification
	 * we use these files: sample.msh, sample.pro, EleSta_v.pro,
	 * Integration_Lib.pro, and Jacobian_Lib.pro. JMONSEL can use fixed forms for
	 * all of these except the first 3. These must be modified to specify such
	 * things as constraints or the values of dielectric constant and charge density
	 * in various regions. Some of these (constraints and dielectric constants) are
	 * typically fixed for a given simulation but may vary from one simulation to
	 * another. The charge density changes even within a simulation as charge
	 * accumulates or dissipates.
	 * </p>
	 * <p>
	 * Function values and constraints are specified in the samples.pro file, which
	 * therefore must be rewritten for each different FEA solution. This file
	 * references meshed regions via the corresponding element tag numbers, which
	 * generally need to be modified (e.g., when a previously uncharged element
	 * becomes charged). This function should be called immediately prior to each
	 * FEA solution to write sample.pro and sample.msh files that are appropriate to
	 * the current state of the mesh.
	 *
	 * @throws IOException
	 */
	private void writeMesh() throws IOException {

		final Mesh mesh = meshedRegion.getMesh();
		final HashMap<Long, IConstraint> constraintMap = meshedRegion.getConstraintMap();
		final HashMap<Long, IMaterialScatterModel> msmMap = meshedRegion.getMSMMap();

		BufferedWriter meshOut = null;
		/*
		 * baseMap will contain information needed for tag assignments. It maps epsr to
		 * a Long[3] = {base,minimum tag assigned so far,maximum tag assigned so far.}
		 * The algorithm assigns base = n*base1 where n=1 for the first epsr encounted,
		 * n=2 for the second, etc. Thus, materials with a given epsr are assigned tags
		 * in the range base to base+base1-1. tag = base is reserved for those with q =
		 * 0. The others are assigned consecutive integers as they are encountered.
		 */
		final HashMap<Double, Long[]> baseMap = new HashMap<Double, Long[]>();
		/*
		 * qMap stores the mapping between newly assigned tag value and the associated
		 * charge. This is needed when we write the sample.pro file.
		 */
		final HashMap<Long, Integer> qMap = new HashMap<Long, Integer>();
		/*
		 * epsqToTagMap stores the mapping from epsr and q to the corresponding tag. It
		 * gets checked to see if a tag has already been assigned for a particular
		 * combination.
		 */
		final HashMap<Double, HashMap<Integer, Long>> epsqToTagMap = new HashMap<Double, HashMap<Integer, Long>>();
		long base1;
		long lastBase;

		try {
			meshOut = new BufferedWriter(new FileWriter(feaFolder + File.separator + "sample.msh"));
			meshOut.write("$MeshFormat");
			meshOut.newLine();
			meshOut.write("2.1 0 8");
			meshOut.newLine();
			meshOut.write("$EndMeshFormat");
			meshOut.newLine();
			meshOut.write("$Nodes");
			meshOut.newLine();
			// The nodes
			final int numNodes = mesh.getNumberOfNodes();
			meshOut.write(Integer.toString(numNodes));
			meshOut.newLine();
			for (int i = 1; i <= numNodes; i++) {
				meshOut.write(Integer.toString(i));
				final double[] nodeCoords = mesh.getNodeCoordinates(i);
				for (int j = 0; j < 3; j++)
					meshOut.write(" " + nodeCoords[j]);
				meshOut.newLine();
			}
			meshOut.write("$EndNodes");
			meshOut.newLine();

			// Initializations for baseMap
			// Find largest constraint tag
			long maxTag = Long.MIN_VALUE;
			for (final Long tag : constraintMap.keySet())
				if (tag > maxTag)
					maxTag = tag;
			final int nElements = mesh.getNumberOfElements();
			if (nElements > maxTag)
				maxTag = nElements;
			// For the sake of using round numbers, get the next higher power of 10
			base1 = Math.round(Math.pow(10., Math.ceil(Math.log10(maxTag))));
			lastBase = 0;

			// The elements
			meshOut.write("$Elements");
			meshOut.newLine();

			meshOut.write(Integer.toString(nElements));
			meshOut.newLine();

			/*
			 * Floating constraints are associated with both surfaces and their contained
			 * volumes. The surface tag is in the constraintMap, but the volume tags are
			 * not, so we have to make a separate list.
			 */
			final HashSet<Long> constrainedVolumeTagSet = new HashSet<Long>();
			for (final IConstraint c : constraintMap.values())
				if (c instanceof FloatingConstraint)
					constrainedVolumeTagSet.add(((FloatingConstraint) c).getVolumeTag());

			for (int i = 1; i <= nElements; i++) {
				/* Element index */
				meshOut.write(Integer.toString(i));
				/* Element type */
				final int type = mesh.getElementType(i);
				meshOut.write(" " + type);
				/* # of tags */
				final int ntags = mesh.getNumberOfTags(i);
				meshOut.write(" " + ntags);
				/* the tags */
				final long[] tags = mesh.getTags(i);
				tags[ntags - 1] = tags[0]; // Save original assignment in the last
											// tag
				double epsr = 1.;
				int nq = 0;

				if (!constraintMap.containsKey(tags[0]) && mesh.isVolumeType(i)
						&& !constrainedVolumeTagSet.contains(tags[0])) {
					/*
					 * Do this block if this volume element is unconstrained
					 */
					final Material mat = msmMap.get(tags[0]).getMaterial();
					if (!(mat instanceof SEmaterial))
						throw new EPQFatalException("Mesh element " + i + " does not contain an SEmaterial.");

					epsr = ((SEmaterial) mat).getEpsr();
					nq = mesh.getChargeNumber(i);

					if (baseMap.containsKey(epsr)) {
						/*
						 * This epsr is already in the baseMap.
						 */

						final HashMap<Integer, Long> qToTagMap = epsqToTagMap.get(epsr);
						if (qToTagMap.containsKey(nq))
							tags[0] = qToTagMap.get(nq); // fetch previous assignment
						else { // new charge value
							/*
							 * Pull its tagInfo to decide what tag is next.
							 */
							final Long[] tagInfo = baseMap.get(epsr);
							if (nq == 0) {
								tagInfo[1] = tagInfo[0];
								tags[0] = tagInfo[0];
							} else {
								tagInfo[2]++;
								tags[0] = tagInfo[2];
							}
							qToTagMap.put(nq, tags[0]);
						}
					} else {
						/*
						 * This is the first time we've encountered this value of epsr. initialize its
						 * baseMap and epsqToTagMap entries in addition to assigning the tag.
						 */
						lastBase += base1;
						if (nq == 0)
							tags[0] = lastBase;
						else
							tags[0] = lastBase + 1L;
						baseMap.put(epsr, new Long[] { lastBase, tags[0], tags[0] });
						final HashMap<Integer, Long> qToTagMap = new HashMap<Integer, Long>();
						qToTagMap.put(nq, tags[0]);
						epsqToTagMap.put(epsr, qToTagMap);
					}
				}
				/*
				 * TODO I've represented tags internally as longs, so they are very unlikely to
				 * overflow. However, it is not AS unlikely that they will exceed
				 * Integer.MAX_VALUE, and it is not at all clear that GetDP, when it imports
				 * such values, will be able to read them. (It is possible it uses int instead
				 * of long.) At some point, for safety's sake, I should check for an overflow
				 * and revise the tag values downward if necessary.
				 */
				for (int j = 0; j < ntags; j++)
					meshOut.write(" " + tags[j]);
				if (nq != 0)
					qMap.put(tags[0], nq);

				/* the nodes */
				final int[] nodeIndices = mesh.getNodeIndices(i);
				for (final int nodeIndex : nodeIndices)
					meshOut.write(" " + nodeIndex);
				meshOut.newLine();
			}
			meshOut.write("$EndElements");
			meshOut.newLine();
		} finally {
			if (meshOut != null)
				meshOut.close();
		}

		/* Write the corresponding sample.pro file */
		BufferedReader in = null;
		BufferedWriter sampleOut = null;
		final Long[] constraintTags = constraintMap.keySet().toArray(new Long[0]);
		final ArrayList<String> constraintNames = new ArrayList<String>();
		final int[] constraintCount = { 0, 0, 0 };

		try {
			final InputStreamReader isr = new InputStreamReader(Mesh.class.getResourceAsStream("samplePart1.pro"));
			in = new BufferedReader(isr);
			sampleOut = new BufferedWriter(new FileWriter(feaFolder + "\\" + "sample.pro"));
			/* Transfer Part 1 */
			String line = in.readLine();
			while (line != null) {
				sampleOut.write(line);
				sampleOut.newLine();
				line = in.readLine();
			}
			in.close();
			/* Write custom parts */
			sampleOut.newLine();
			/*
			 * Write Group specification
			 */
			sampleOut.write("Group {");
			sampleOut.newLine();
			sampleOut.newLine();

			sampleOut.write("  /* Elementary Groups used for Function Defs */");
			sampleOut.newLine();
			final Double[] epsValues = baseMap.keySet().toArray(new Double[0]);
			final ArrayList<String> epsNames = new ArrayList<String>();
			final ArrayList<String> sourceNames = new ArrayList<String>();
			for (int i = 0; i < epsValues.length; i++) {
				final Long[] tagInfo = baseMap.get(epsValues[i]);
				final String epsName = "epsRegion" + i;
				epsNames.add(epsName);
				if (tagInfo[1].longValue() == tagInfo[2].longValue())
					sampleOut.write("  " + epsName + " = Region[" + tagInfo[1] + "];");
				else
					sampleOut.write("  " + epsName + " = Region[" + tagInfo[1] + ":" + tagInfo[2] + "];");
				sampleOut.newLine();
				if (tagInfo[2].longValue() > tagInfo[0].longValue()) { /*
																		 * Some with this epsr have q != 0.
																		 */
					final String sourceName = "epsRegion" + i + "Sources";
					sourceNames.add(sourceName);
					if (tagInfo[1].longValue() == tagInfo[2].longValue())
						sampleOut.write("  " + sourceName + " = Region[" + (tagInfo[0] + 1) + "];");
					else
						sampleOut.write("  " + sourceName + " = Region[" + (tagInfo[0] + 1) + ":" + tagInfo[2] + "];");
					sampleOut.newLine();
				}
			}
			sampleOut.newLine();

			sampleOut.write("  /* Elementary Groups used for Constraints */");
			sampleOut.newLine();
			for (int i = 0; i < constraintTags.length; i++) {
				final String constraintName = "ConstraintRegion" + i;
				constraintNames.add(constraintName);
				sampleOut.write("  " + constraintName + " = Region[" + constraintTags[i] + "];");
				sampleOut.newLine();
			}
			sampleOut.newLine();

			sampleOut.write("  /* Global Groups */");
			sampleOut.newLine();

			sampleOut.write("  SourceDomain = Region[{");
			boolean first = true;
			for (final String name : sourceNames) {
				if (!first)
					sampleOut.write(",");
				sampleOut.write(name);
				first = false;
			}
			sampleOut.write("}]; /* Regions with nonzero charge density */");
			sampleOut.newLine();

			sampleOut.write("  DirichletConstrained = Region[{");
			for (int i = 0, count = 0; i < constraintTags.length; i++) {
				final IConstraint c = constraintMap.get(constraintTags[i]);
				if (c instanceof DirichletConstraint) { // Dirichlet constraint
					if (count > 0)
						sampleOut.write(",");
					sampleOut.write(constraintNames.get(i));
					count++;
				}
			}
			sampleOut.write("}]; /* Regions with Dirichlet constraint */");
			sampleOut.newLine();

			sampleOut.write("  vonNeumannConstrained = Region[{");
			for (int i = 0, count = 0; i < constraintTags.length; i++) {
				final IConstraint c = constraintMap.get(constraintTags[i]);
				if (c instanceof NeumannConstraint) { // von Neumann constraint
					if (count > 0)
						sampleOut.write(",");
					sampleOut.write(constraintNames.get(i));
					count++;
				}
			}
			sampleOut.write("}]; /* Regions with Neumann constraint */");
			sampleOut.newLine();

			sampleOut.write("  FloatConstrained = Region[{");
			for (int i = 0, count = 0; i < constraintTags.length; i++) {
				final IConstraint c = constraintMap.get(constraintTags[i]);
				if (c instanceof FloatingConstraint) { // Floating type constraint
					if (count > 0)
						sampleOut.write(",");
					sampleOut.write(constraintNames.get(i));
					count++;
				}
			}
			sampleOut.write("}]; /* Floating conductor boundary Regions */");
			sampleOut.newLine();

			sampleOut.write("  ConstrainedRegionsAll = Region[{");
			first = true;
			for (final String name : constraintNames) {
				if (!first)
					sampleOut.write(",");
				sampleOut.write(name);
				first = false;
			}
			sampleOut.write("}]; /* All regions with constraints */");
			sampleOut.newLine();

			sampleOut.write("  DomainCC_Ele = Region[{");
			first = true;
			for (final String name : epsNames) {
				if (!first)
					sampleOut.write(",");
				sampleOut.write(name);
				first = false;
			}
			sampleOut.write("}]; /* Nonconducting regions */");
			sampleOut.newLine();

			sampleOut.write("  DomainC_Ele = Region[{");
			first = true;
			for (final String name : constraintNames) {
				if (!first)
					sampleOut.write(",");
				sampleOut.write(name);
				first = false;
			}
			sampleOut.write("}]; /* Constrained regions */");
			sampleOut.newLine();

			sampleOut.write("  Domain_Ele = Region[{DomainCC_Ele}];");
			sampleOut.newLine();

			sampleOut.newLine();
			sampleOut.write("}");
			sampleOut.newLine();
			/*
			 * Write Function specification
			 */
			sampleOut.newLine();
			sampleOut.newLine();
			sampleOut.write("Function {");
			sampleOut.newLine();

			sampleOut.newLine();
			sampleOut.write("  /* Piecewise definition of relative dielectric function */");
			sampleOut.newLine();
			for (int i = 0; i < epsNames.size(); i++) {
				sampleOut.write("  epsr[" + epsNames.get(i) + "] = " + epsValues[i] + ";");
				sampleOut.newLine();
			}
			sampleOut.write("  epsr[ConstrainedRegionsAll] = 1.0;");
			sampleOut.newLine();

			sampleOut.newLine();
			sampleOut.write("  /* Piecewise definition of q function */");
			sampleOut.newLine();
			for (final Entry<Long, Integer> entry : qMap.entrySet()) {
				sampleOut.write("  q[Region[" + entry.getKey() + "]] = " + entry.getValue() + ";");
				sampleOut.newLine();
			}

			sampleOut.newLine();
			sampleOut.write("}");
			sampleOut.newLine();

			/*
			 * Write Constraint specification
			 */
			sampleOut.newLine();
			sampleOut.newLine();
			sampleOut.write("Constraint {");
			sampleOut.newLine();

			sampleOut.newLine();
			sampleOut.write("  /* Dirichlet constraints */");
			sampleOut.newLine();
			sampleOut.write("  { Name ElectricScalarPotential; Type Assign;");
			sampleOut.newLine();
			sampleOut.write("    Case {");
			sampleOut.newLine();
			for (int i = 0; i < constraintTags.length; i++) {
				final IConstraint c = constraintMap.get(constraintTags[i]);
				if (c instanceof DirichletConstraint) {
					constraintCount[0] += 1;
					sampleOut.write("      { Region " + constraintNames.get(i) + "; Value "
							+ ((DirichletConstraint) c).getPotential() + ";}");
					sampleOut.newLine();
				} else if (c instanceof NeumannConstraint)
					constraintCount[1]++;
				else if (c instanceof FloatingConstraint)
					constraintCount[2]++;
			}
			sampleOut.write("    }"); // Close Case
			sampleOut.newLine();
			sampleOut.write("  }"); // Close ElectricScalarPotential
			sampleOut.newLine();

			if (constraintCount[2] > 0) { /*
											 * Add GlobalCharge terms if we have any floating constraints
											 */
				sampleOut.write("  { Name GlobalElectricCharge;");
				sampleOut.newLine();
				sampleOut.write("    Case {");
				sampleOut.newLine();
				for (int i = 0; i < constraintTags.length; i++) {
					final IConstraint c = constraintMap.get(constraintTags[i]);
					if (c instanceof FloatingConstraint) {
						/* Tally charge in this volume */
						final FloatingConstraint cF = (FloatingConstraint) c;
						// cF.computeCharge();
						sampleOut
								.write("      { Region " + constraintNames.get(i) + "; Value " + cF.getCharge() + ";}");
						sampleOut.newLine();
					}
				}
				sampleOut.write("    }"); // End Case
				sampleOut.newLine();
				sampleOut.write("  }"); // End GlobalElectricCharge
				sampleOut.newLine();
			}

			sampleOut.newLine();
			sampleOut.write("}"); // Close Constraint
			sampleOut.newLine();
			sampleOut.newLine();

			/*
			 * Write includes
			 */
			sampleOut.write("/* The formulation used and its tools, considered as being");
			sampleOut.newLine();
			sampleOut.write("   in a black box, can now be included */");
			sampleOut.newLine();
			sampleOut.newLine();
			sampleOut.write("Include \"Jacobian_Lib.pro\"");
			sampleOut.newLine();
			sampleOut.write("Include \"Integration_Lib.pro\"");
			sampleOut.newLine();
			sampleOut.write("Include \"EleSta_v.pro\"");
			sampleOut.newLine();
			sampleOut.newLine();

			/*
			 * Write PostOperation specification
			 */
			sampleOut.write("PostOperation {");
			sampleOut.newLine();
			sampleOut.write("  { Name Map; NameOfPostProcessing EleSta_v;");
			sampleOut.newLine();
			sampleOut.write("     Operation {");
			sampleOut.newLine();
			sampleOut.write("       Print [ v, OnElementsOf DomainCC_Ele, File \"Lines_v.pos\" ];");
			sampleOut.newLine();
			if (constraintCount[2] > 0) { /* Print global terms */
				sampleOut.write("       Print[Q, OnRegion FloatConstrained, Format Table];");
				sampleOut.newLine();
				sampleOut.write("       Print[V, OnRegion FloatConstrained, Format Table];");
				sampleOut.newLine();
			}
			sampleOut.write("     }"); // End Operation
			sampleOut.newLine();
			sampleOut.write("  }"); // End Map
			sampleOut.newLine();
			sampleOut.write("}"); // End PostOperation
			sampleOut.newLine();
		} finally {
			if (in != null)
				in.close();
			if (sampleOut != null)
				sampleOut.close();
		}

		/* Write the corresponding EleSta_v.pro file */

		BufferedWriter out = null;
		try {
			final InputStreamReader isr = new InputStreamReader(Mesh.class.getResourceAsStream("EleStaV.pro"));
			in = new BufferedReader(isr);
			out = new BufferedWriter(new FileWriter(feaFolder + "\\" + "EleSta_v.pro"));

			/* Transfer line by line until we get to the charge definition */
			boolean charge;
			String line;
			do {
				line = in.readLine();
				charge = line.contains("chargeUnit");
				if (!charge) {
					out.write(line);
					out.newLine();
				}
			} while (!charge);

			/* Write the chargeUnit value */
			out.write("   chargeUnit = " + Double.toString(chargeMultiplier * PhysicalConstants.ElectronCharge) + ";");
			out.newLine();

			/* Transfer line by line until we get to the FunctionSpace */
			boolean fspace;
			do {
				line = in.readLine();
				fspace = line.contains("FunctionSpace {");
				if (!fspace) {
					out.write(line);
					out.newLine();
				}
			} while (!fspace);

			/* Write the FunctionSpace */
			out.write("FunctionSpace { ");
			out.newLine();
			out.write("  { Name Hgrad_v_Ele; Type Form0; ");
			out.newLine();
			out.write("    BasisFunction {");
			out.newLine();
			out.write("      // v = v  s   ,  for all nodes");
			out.newLine();
			out.write("      //      n  n");
			out.newLine();
			out.write("      { Name sn; NameOfCoef vn; Function BF_Node;");
			out.newLine();
			if (constraintCount[2] == 0) {
				out.write("        Support Region[{Domain_Ele,DomainC_Ele}]; Entity NodesOf[ All ]; }");
				out.newLine();
				out.write("    }"); // End BasisFunction
				out.newLine();
				out.newLine();
				out.write("    Constraint {");
				out.newLine();
				out.write("      { NameOfCoef vn; EntityType NodesOf; NameOfConstraint ElectricScalarPotential; }");
			} else {
				out.write(
						"        Support Region[{Domain_Ele,DomainC_Ele}]; Entity NodesOf[ All , Not FloatConstrained ]; }");
				out.newLine();
				out.write("   { Name sf; NameOfCoef vfu; Function BF_GroupOfNodes;");
				out.newLine();
				out.write("        Support Domain_Ele; Entity GroupsOfNodesOf[ FloatConstrained ]; }");
				out.newLine();
				out.write("    }"); // End BasisFunction
				out.newLine();
				out.newLine();
				out.write(" GlobalQuantity {");
				out.newLine();
				out.write("    { Name GlobalElectricPotential ; Type AliasOf ; NameOfCoef vfu ; }");
				out.newLine();
				out.write("    { Name GlobalElectricCharge ; Type AssociatedWith ; NameOfCoef vfu ; }");
				out.newLine();
				out.write(" }"); // End GlobalQuantity
				out.newLine();
				out.newLine();
				out.write("    Constraint {");
				out.newLine();
				out.write("      { NameOfCoef vn; EntityType NodesOf; NameOfConstraint ElectricScalarPotential; }");
				out.newLine();
				out.write("      { NameOfCoef vfu; EntityType GroupsOfNodesOf; NameOfConstraint CFoo; }");
				out.newLine();
				out.write(
						"      { NameOfCoef GlobalElectricCharge; EntityType GroupsOfNodesOf; NameOfConstraint GlobalElectricCharge; }");
				out.newLine();
				out.write(
						"      { NameOfCoef GlobalElectricPotential; EntityType GroupsOfNodesOf; NameOfConstraint GlobalElectricPotential; }");
			}
			out.newLine();
			out.write("    }"); // End Constraint
			out.newLine();
			out.write("  }"); // End Name Hgrad_v_Ele
			out.newLine();
			out.write("}"); // End FunctionSpace
			out.newLine();
			out.newLine();

			/* Write the formulation */
			out.write("Formulation { ");
			out.newLine();
			out.write("  { Name Electrostatics_v; Type FemEquation;");
			out.newLine();
			out.write("    Quantity {");
			out.newLine();
			out.write("      { Name v; Type Local; NameOfSpace Hgrad_v_Ele; }");
			out.newLine();
			if (constraintCount[2] > 0) {
				out.write("      { Name Q; Type Global; NameOfSpace Hgrad_v_Ele [GlobalElectricCharge]; }");
				out.newLine();
				out.write("      { Name V; Type Global; NameOfSpace Hgrad_v_Ele [GlobalElectricPotential]; }");
				out.newLine();
			}
			out.write("    }"); // End Quantity
			out.newLine();
			out.write("    Equation {");
			out.newLine();
			out.write("      Galerkin { [ epsr[] * Dof{d v} , {d v} ]; In Domain_Ele; ");
			out.newLine();
			out.write("                 Jacobian Vol; Integration GradGrad; }");
			out.newLine();
			out.write("      Galerkin { [ -q[]*chargeUnit/eps0/ElementVol[] , {v} ]; In SourceDomain; ");
			out.newLine();
			out.write("                 Jacobian Vol; Integration GradGrad; }");
			out.newLine();
			/* Write von Neumann terms if any */
			for (int i = 0; i < constraintTags.length; i++) {
				final IConstraint c = constraintMap.get(constraintTags[i]);
				if (!(c instanceof NeumannConstraint))
					continue;
				out.write("      Galerkin { [ " + Double.toString(-((NeumannConstraint) c).getNormalE())
						+ " , {v} ]; In " + constraintNames.get(i) + "; ");
				out.newLine();
				out.write("                 Jacobian Sur; Integration GradGrad; }");
				out.newLine();
			}
			/* Write Global term if any */
			if (constraintCount[2] > 0) {
				out.write("   GlobalTerm { [ -Dof{Q}*chargeUnit/eps0, {V} ] ; In FloatConstrained ; }");
				out.newLine();
			}

			/* Close the Formulation */
			out.write("    }");
			out.newLine();
			out.write("  }");
			out.newLine();
			out.write("}");
			out.newLine();
			out.newLine();

			/* Write the Resolution */
			out.write("Resolution {");
			out.newLine();
			out.write("  { Name EleSta_v;");
			out.newLine();
			out.write("    System {");
			out.newLine();
			out.write("      { Name Sys_Ele; NameOfFormulation Electrostatics_v; }");
			out.newLine();
			out.write("    }"); // End System
			out.newLine();
			out.write("    Operation { ");
			out.newLine();
			out.write("      Generate[Sys_Ele]; Solve[Sys_Ele]; SaveSolution[Sys_Ele];");
			out.newLine();
			out.write("    }"); // End Operation
			out.newLine();
			out.write("  }"); // End Name EleSta_v
			out.newLine();
			out.write("}"); // End Resolution
			out.newLine();

			/* Write the PostProcessing */
			out.write("PostProcessing {");
			out.newLine();
			out.write("  { Name EleSta_v; NameOfFormulation Electrostatics_v;");
			out.newLine();
			out.write("    Quantity {");
			out.newLine();
			out.write("      { Name v; Value { Local { [ {v} ]; In DomainCC_Ele; Jacobian Vol; } } }");
			out.newLine();
			if (constraintCount[2] > 0) {
				out.write("      { Name Q; Value { Term { [ {Q} ]; In FloatConstrained; } } }");
				out.newLine();
				out.write("      { Name V; Value { Term { [ {V} ]; In FloatConstrained; } } }");
				out.newLine();
			}
			out.write("    }"); // End Quantity
			out.newLine();
			out.write("  }");
			out.newLine(); // End Name EleSta_v
			out.write("}");
			out.newLine(); // End PostProcessing
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	/**
	 * Copies the named resource from this package to the supplied destination
	 * folder. This is used to transfer GetDP problem specification files
	 * (EleSta_V.pro, Integration_Lib.pro, etc.) to a temporary folder from which
	 * GetDP can read them, do its FEA calculation, and write the results.
	 *
	 * @param resourceName
	 * @param destFileName
	 * @throws IOException
	 */
	private void writeResource(String resourceName, String destFileName) throws IOException {
		/* Open resource for input */
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			final InputStream ras = Mesh.class.getResourceAsStream(resourceName);
			if (ras == null)
				throw new EPQFatalException("Error opening resource " + resourceName + ". getResourceAsStream failed");
			in = new BufferedInputStream(ras);

			/* Open destination file for output */
			out = new BufferedOutputStream(new FileOutputStream(feaFolder + File.separator + destFileName));

			int b = in.read();
			while (b != -1) {
				out.write(b);
				b = in.read();
			}
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

}

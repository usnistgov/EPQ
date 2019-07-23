package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A class designed for running CITZAF analyses
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */
public class CITZAF {
   public static String FileLocation = "";

   public static final int BULK = 0;
   public static final int PARTICLE_OR_THIN_FILM = 1;

   /**
    * A mechanism for specifying K, L or M line.
    */
   public static class Line {
      private final int mLine;

      private Line(final int Line) {
         mLine = Line;
      }

      @Override
      public String toString() {
         if(this == KLINE)
            return "K";
         else if(this == LLINE)
            return "L";
         else if(this == MLINE)
            return "M";
         else
            throw new IllegalStateException("Unrecognized Line");
      }

      public int getCode() {
         return mLine;
      }

      public static final Line KLINE = new Line(1);
      public static final Line LLINE = new Line(2);
      public static final Line MLINE = new Line(3);
   }

   public static final int DO_NOT_USE_STAGE_COORDINATES = 0;
   public static final int USE_STAGE_COORDINATES = 1;

   public static final int DO_NOT_PRINTOUT_MACS = 0;
   public static final int PRINTOUT_MACS = 1;

   public static final int DO_NOT_PRINTOUT_ZAF = 0;
   public static final int PRINTOUT_ZAF = 1;

   public static final int MINOR_ELEMENTS_CONCENTRATION_IN_PPM = 0;
   public static final int MINOR_ELEMENTS_CONCENTRATION_IN_NEAREST_HUNDRETH = 1;

   /**
    * A list of available correction algorithms using the typesafe enum pattern
    */
   public static final class CITZAFCorrectionAlgorithm {
      private final int[] mAlgorithm;

      private CITZAFCorrectionAlgorithm(final int[] Algorithm) {
         mAlgorithm = Algorithm;
      }

      @Override
      public String toString() {
         if(this == ARMSTRONG_LOVE_SCOTT)
            return "Armstrong/Love Scott";
         else if(this == CONVENTIONAL_PHILIBERT_DUNCUMB_REED)
            return "Conventional Philibert/Duncumb-Reed";
         else if(this == HEINRICH_DUNCUMB_REED)
            return "Heinrich/Duncumb-Reed";
         else if(this == LOVE_SCOTT_I)
            return "Love-Scott I";
         else if(this == LOVE_SCOTT_II)
            return "Love-Scott II";
         else if(this == PACKWOOD_PHI_PZ)
            return "Packwood Phi(PZ)(EPQ-91)";
         else if(this == BASTIN_ORIGINAL_PHI_PZ)
            return "Bastin Original Phi(PZ)";
         else if(this == BASTIN_PROZA_PHI_PZ)
            return "Bastin Proza Phi(PZ)(EPQ-91)";
         else if(this == POUCHOU_AND_PICHOIR_FULL)
            return "Pouchou and Pichoir - Full";
         else if(this == POUCHOU_AND_PICHOIR_SIMPLIFIED)
            return "Pouchou and Pichoir - Simplified";
         else
            throw new IllegalStateException("Correction Algorithm is not a recognized algorithm");
      }

      public int[] getAlgorithm() {
         return mAlgorithm;
      }

      public static CITZAFCorrectionAlgorithm[] getAllImplimentations() {
         return new CITZAFCorrectionAlgorithm[] {
            ARMSTRONG_LOVE_SCOTT,
            CONVENTIONAL_PHILIBERT_DUNCUMB_REED,
            HEINRICH_DUNCUMB_REED,
            LOVE_SCOTT_I,
            LOVE_SCOTT_II,
            PACKWOOD_PHI_PZ,
            BASTIN_ORIGINAL_PHI_PZ,
            BASTIN_PROZA_PHI_PZ,
            POUCHOU_AND_PICHOIR_FULL,
            POUCHOU_AND_PICHOIR_SIMPLIFIED
         };
      }

      public static final CITZAFCorrectionAlgorithm ARMSTRONG_LOVE_SCOTT = new CITZAFCorrectionAlgorithm(new int[] {
         2,
         1,
         2,
         0,
         0,
         9,
         4,
         4
      });

      public static final CITZAFCorrectionAlgorithm CONVENTIONAL_PHILIBERT_DUNCUMB_REED = new CITZAFCorrectionAlgorithm(new int[] {
         1,
         2,
         5,
         0,
         0,
         1,
         1,
         1
      });

      public static final CITZAFCorrectionAlgorithm HEINRICH_DUNCUMB_REED = new CITZAFCorrectionAlgorithm(new int[] {
         1,
         1,
         5,
         0,
         0,
         2,
         1,
         2
      });

      public static final CITZAFCorrectionAlgorithm LOVE_SCOTT_I = new CITZAFCorrectionAlgorithm(new int[] {
         2,
         1,
         2,
         0,
         0,
         4,
         4,
         4
      });

      public static final CITZAFCorrectionAlgorithm LOVE_SCOTT_II = new CITZAFCorrectionAlgorithm(new int[] {
         2,
         1,
         2,
         0,
         0,
         6,
         4,
         4
      });

      public static final CITZAFCorrectionAlgorithm PACKWOOD_PHI_PZ = new CITZAFCorrectionAlgorithm(new int[] {
         1,
         5,
         7,
         0,
         0,
         14,
         6,
         0
      });

      public static final CITZAFCorrectionAlgorithm BASTIN_ORIGINAL_PHI_PZ = new CITZAFCorrectionAlgorithm(new int[] {
         2,
         3,
         2,
         0,
         0,
         10,
         6,
         0
      });

      public static final CITZAFCorrectionAlgorithm BASTIN_PROZA_PHI_PZ = new CITZAFCorrectionAlgorithm(new int[] {
         3,
         3,
         4,
         0,
         0,
         15,
         5,
         7
      });

      public static final CITZAFCorrectionAlgorithm POUCHOU_AND_PICHOIR_FULL = new CITZAFCorrectionAlgorithm(new int[] {
         3,
         3,
         4,
         0,
         0,
         12,
         5,
         7
      });

      public static final CITZAFCorrectionAlgorithm POUCHOU_AND_PICHOIR_SIMPLIFIED = new CITZAFCorrectionAlgorithm(new int[] {
         3,
         3,
         4,
         0,
         0,
         13,
         5,
         7
      });
   }

   // Backscatter Coefficients
   public static int BACKSCATTER_COEFFICIENT_OF_HEINRICH = 1;
   public static int BACKSCATTER_COEFFICIENT_OF_LOVE_SCOTT = 2;
   public static int BACKSCATTER_COEFFICIENT_OF_POUCHOU_AND_PICHOIR = 3;
   public static int BACKSCATTER_COEFFICIENT_OF_HUNGER_AND_KUCHLER = 4;
   public static int NO_BACKSCATTER_COEFFICIENT = 0;

   public static String[] BACKSCATTER_COEFFICIENTS = {
      "Heinrich",
      "Love/Scott",
      "Pouchou & Pichoir",
      "Hunger & Kuchler<A&W-mod>",
      "None"
   };

   // Mean Ionization Potentials
   public static int MEAN_IONIZATION_POTENTIAL_OF_BERGER_AND_SELTZER = 1;
   public static int MEAN_IONIZATION_POTENTIAL_OF_DUNCUMB_AND_DA_CASA = 2;
   public static int MEAN_IONIZATION_POTENTIAL_OF_RUSTE_AND_ZELLER = 3;
   public static int MEAN_IONIZATION_POTENTIAL_OF_SPRINGER = 4;
   public static int MEAN_IONIZATION_POTENTIAL_OF_WILSON = 5;
   public static int MEAN_IONIZATION_POTENTIAL_OF_HEINRICH = 6;
   public static int MEAN_IONIZATION_POTENTIAL_OF_BLOCH = 7;
   public static int MEAN_IONIZATION_POTENTIAL_OF_ARMSTRONG = 8;
   public static int MEAN_IONIZATION_POTENTIAL_OF_JOY = 9;

   public static String[] MEAN_IONIZATION_POTENTIALS = {
      "Berger/Seltzer",
      "Duncumb/Da Casa",
      "Ruste/Zeller",
      "Springer",
      "Wilson",
      "Heinrich",
      "Bloch<Love/Scott>",
      "Armstrong<Springer/Berger>",
      "Joy <Wilson/Berger>"
   };

   // Phi<0>'s
   public static int PHI_0_EQUATION_OF_REUTER = 1;
   public static int PHI_0_EQUATION_OF_LOVE_AND_SCOTT = 2;
   public static int PHI_0_EQUATION_OF_RIVEROS = 3;
   public static int PHI_0_EQUATION_OF_POUCHOU_AND_PICHOIR = 4;
   public static int PHI_0_EQUATION_OF_KARDUCK_AND_REHBACH = 5;
   public static int PHI_0_EQUATION_OF_AUGUST_AND_WERNISCH = 6;
   public static int PHI_0_EQUATION_OF_PACKWOOD = 7;
   public static int NO_PHI_0_EQUATION = 0;

   public static String[] PHI_O_EQUATIONS = {
      "Reuter",
      "Love/Scott",
      "Riveros",
      "Pouchou & Pichoir",
      "Karduck & Rehbach",
      "August & Wernisch",
      "Packwood",
      "None"
   };

   // Ionization Cross Sections
   public static int IONIZATION_CROSS_SECTION_OF_BETHE_AND_POWELL = 1;
   public static int IONIZATION_CROSS_SECTION_OF_BETHE_AND_BROWN = 2;
   public static int IONIZATION_CROSS_SECTION_OF_GREEN_AND_COSSLETT = 3;
   public static int IONIZATION_CROSS_SECTION_OF_HUTCHINS = 4;
   public static int IONIZATION_CROSS_SECTION_OF_FABRE_AND_POWELL = 5;
   public static int IONIZATION_CROSS_SECTION_OF_WORTHINGTON_AND_TOMLIN = 6;
   public static int IONIZATION_CROSS_SECTION_OF_GRYZINSKY_AND_TOMLIN = 7;
   public static int IONIZATION_CROSS_SECTION_OF_CASNATI = 8;
   public static int IONIZATION_CROSS_SECTION_OF_JACOBI = 9;
   public static int IONIZATION_CROSS_SECTION_OF_REZ = 10;
   public static int NO_IONIZATION_CROSS_SECTION = 0;

   public static String[] IONIZATION_CROSS_SECTIONS = {
      "Bethe<Powell>",
      "Bethe<Brown>",
      "Green & Cosslett",
      "Hutchins",
      "Fabre<Powell>",
      "Worthington & Tomlin",
      "Gryzinsky",
      "Casnati",
      "Jacobi",
      "Rez",
      "None"
   };

   // Electron Retardation Equations
   public static int ELECTRON_RETARDATION_EQUATION_OF_BETHE_AND_RAO_SAHIB_AND_WITTRY = 1;
   public static int ELECTRON_RETARDATION_EQUATION_OF_LOVE_AND_SCOTT = 2;
   public static int ELECTRON_RETARDATION_EQUATION_OF_JOY = 3;
   public static int NO_ELECTRON_RETARDATION_EQUATION = 0;

   public static String[] ELECTRON_RETARDATION_EQUATIONS = {
      "Bethe<Rao-Sahib/Wittry>",
      "Love & Scott",
      "Joy",
      "None"
   };

   // Absorption Corrections
   public static int ABSORPTION_CORRECTION_OF_PHILIBERT = 1;
   public static int ABSORPTION_CORRECTION_OF_HEINRICH_AN_CHEM = 2;
   public static int ABSORPTION_CORRECTION_OF_HEINRICH_1989_MAS = 3;
   public static int ABSORPTION_CORRECTION_OF_LOVE_AND_SCOTT_1983 = 4;
   public static int ABSORPTION_CORRECTION_OF_LOVE_AND_SCOTT_I_1985 = 5;
   public static int ABSORPTION_CORRECTION_OF_LOVE_AND_SCOTT_II_1985 = 6;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_PACKWOOD_AND_BROWN = 7;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_BASTIN_1984 = 8;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_ARMSTRONG = 9;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_BASTIN_1986 = 10;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_RIVEROS_1987 = 11;
   public static int ABSORPTION_CORRECTION_OF_POUCHOU_AND_PICHOIR_FULL_PAP = 12;
   public static int ABSORPTION_CORRECTION_OF_POUCHOU_AND_PICHOIR_SIMPLE_PAP = 13;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_PACKWOOD_EPQ_91 = 14;
   public static int PHI_PZ_ABSORPTION_CORRECTION_OF_BASTIN_PROZA = 15;

   public static String[] ABSORPTION_CORRECTIONS = {
      "Absorption correction of Philibert",
      "Absorption correction of Heinrich/An. chem",
      "Absorption correction of Heinrich/1989 MAS",
      "Absorption correction of Love/Scott - 1983",
      "Absorption correction of Love/Scott I - 1985",
      "Absorption correction of Love/Scott II - 1985",
      "PHI<PZ> Absorption correction of Packwood/Brown 1982/XRS",
      "PHI<PZ> Absorption correction of Bastin 1984/XRS",
      "PHI<PZ> Absorption correction of Armstrong<P/B> 1981/MAS",
      "PHI<PZ> Absorption correction of Bastin 1986/Scanning",
      "PHI<PZ> Absorption correction of Riveros 1987/XRS",
      "Absorption correction of Pouchou & Pichoir - Full PAP",
      "Absorption correction of Pouchou & Pichoir - Simple PAP",
      "PHI<PZ> Absorption correction of Packwood <EPQ-91>",
      "PHI<PZ> Absorption correction of Bastin PROZA <EPQ-91>"
   };

   // Atomic Number Stopping Power Corrections
   public static int STOPPING_POWER_CORRECTION_OF_DUNCUMB_AND_REED = 1;
   public static int STOPPING_POWER_CORRECTION_OF_PHILIBERT_AND_TIXIER = 2;
   public static int STOPPING_POWER_CORRECTION_BY_NUMERICAL_INTEGRATION = 3;
   public static int STOPPING_POWER_CORRECTION_OF_LOVE_AND_SCOTT = 4;
   public static int STOPPING_POWER_CORRECTION_OF_POUCHOU_AND_PICHOIR = 5;
   public static int ATOMIC_NUMBER_CORRECTION_OF_PHI_PZ_INTEGRATION = 6;

   public static String[] ATOMIC_NUMBER_STOPPING_POWERS_CORRECTIONS = {
      "Stopping power correction of Duncumb/Reed<Frame>",
      "Stopping power correction of Philibert & Tixier",
      "Stopping power correction by Numerical Integration",
      "Stopping power correction of Love & Scott",
      "Stopping power correction of Pouchou & Pichoir",
      "Atomic number correction of PHI<PZ> integration"
   };

   // Atomic Number Backscatter Corrections
   public static int BACKSCATTER_CORRECTION_OF_DUNCUMB_AND_REED_FRAME_I = 1;
   public static int BACKSCATTER_CORRECTION_OF_DUNCUMB_AND_REED_FRAME_II = 2;
   public static int BACKSCATTER_CORRECTION_OF_DUNCUMB_AND_REED_HEINRICH_MOD = 3;
   public static int BACKSCATTER_CORRECTION_OF_LOVE_AND_SCOTT = 4;
   public static int BACKSCATTER_CORRECTION_OF_MYKLEBUST_I = 5;
   public static int BACKSCATTER_CORRECTION_OF_MYKLEBUST_AND_FIORI = 6;
   public static int BACKSCATTER_CORRECTION_OF_POUCHOU_AND_PICHOIR = 7;
   public static int BACKSCATTER_CORRECTION_OF_AUGUST_AND_RAZKA_AND_WERNISCH = 8;
   public static int BACKSCATTER_CORRECTION_OF_SPRINGER = 9;
   public static int NO_BACKSCATTER_CORRECTION = 0;

   public static String[] BACKSCATTER_CORRECTIONS = {
      "Duncumb/Reed<Frame-I>",
      "Duncumb/Reed<Frame-II>",
      "Duncumb/Reed<Heinrich-Mod>",
      "Love & Scott",
      "Myklebust - I",
      "Myklebust & Fiori (not implemented yet)",
      "Pouchou & Pichoir",
      "August, Razka & Wernisch",
      "Springer",
      "None"
   };

   public static class MassAbsorptionCoefficient {
      private final int MACCode;

      private MassAbsorptionCoefficient(final int code) {
         MACCode = code;
      }

      @Override
      public String toString() {
         if(this == MASS_ABSORPTION_COEFFICIENTS_FROM_DATA_TABLES)
            return "MACs From Data Tables";
         else if(this == MASS_ABSORPTION_COEFFICIENTS_FROM_TABLES_AND_USER_ENTRY)
            return "MACs From Data Tables and User Entry";
         else if(this == MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_EQN)
            return "MACs From Heinrich ICXOM-11 Equations";
         else if(this == MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_AND_USER_ENTRY)
            return "MACs From Heinrich ICXOM-11 Equations and User Entry";
         else
            throw new IllegalStateException("MAC is not a currently recognized MAC");
      }

      public static MassAbsorptionCoefficient[] getAllImplimentations() {
         return new MassAbsorptionCoefficient[] {
            MASS_ABSORPTION_COEFFICIENTS_FROM_DATA_TABLES,
            MASS_ABSORPTION_COEFFICIENTS_FROM_TABLES_AND_USER_ENTRY,
            MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_EQN,
            MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_AND_USER_ENTRY
         };
      }

      public int getCode() {
         return MACCode;
      }

      public static final MassAbsorptionCoefficient MASS_ABSORPTION_COEFFICIENTS_FROM_DATA_TABLES = new MassAbsorptionCoefficient(0);
      public static final MassAbsorptionCoefficient MASS_ABSORPTION_COEFFICIENTS_FROM_TABLES_AND_USER_ENTRY = new MassAbsorptionCoefficient(1);
      public static final MassAbsorptionCoefficient MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_EQN = new MassAbsorptionCoefficient(2);
      public static final MassAbsorptionCoefficient MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_AND_USER_ENTRY = new MassAbsorptionCoefficient(3);

   }
   /**
    * Represents the various different particle shape models supported by
    * CITZAF.
    */
   public static class ParticleModel {
      private final int ParticleModelCode;

      private ParticleModel(final int ParticleModelCode) {
         this.ParticleModelCode = ParticleModelCode;
      }

      public int getCode() {
         return ParticleModelCode;
      }

      @Override
      public String toString() {
         if(this == PARTICLE_MODEL_OF_THIN_FILM_TPS)
            return "thin film, tps";
         else if(this == PARTICLE_MODEL_OF_RECTANGULAR_PRISM)
            return "rectangular prism";
         else if(this == PARTICLE_MODEL_OF_TETRAGONAL)
            return "tetragonal";
         else if(this == PARTICLE_MODEL_OF_TRIGONAL_PRISM)
            return "trigonal prism";

         else if(this == PARTICLE_MODEL_OF_SQUARE_PYRAMID)
            return "square pyramid";
         else if(this == PARTICLE_MODEL_OF_SIDE_SCATTER_MODIFIED_RECTANGULAR_PRISM)
            return "side-scattered modified rectangular prism";
         /*
          * else if (this == PARTICLE_MODEL_OF_THIN_FILM_TPS) return "cylinder
          * resting on a base"; else if (this ==
          * PARTICLE_MODEL_OF_THIN_FILM_TPS) return "cylinder resting on its
          * side"; else if (this == PARTICLE_MODEL_OF_THIN_FILM_TPS) return
          * "hemisphere resting on base"; else if (this ==
          * PARTICLE_MODEL_OF_THIN_FILM_TPS) return "sphere";
          */
         else
            throw new IllegalStateException("Particle Model not recognized");

      }

      public static ParticleModel[] getAllImplimentations() {
         return new ParticleModel[] {
            PARTICLE_MODEL_OF_THIN_FILM_TPS,
            PARTICLE_MODEL_OF_RECTANGULAR_PRISM,
            PARTICLE_MODEL_OF_TETRAGONAL,
            PARTICLE_MODEL_OF_TRIGONAL_PRISM,
            PARTICLE_MODEL_OF_SQUARE_PYRAMID,
            PARTICLE_MODEL_OF_SIDE_SCATTER_MODIFIED_RECTANGULAR_PRISM
         };
      }

      public static final ParticleModel PARTICLE_MODEL_OF_THIN_FILM_TPS = new ParticleModel(1);
      public static final ParticleModel PARTICLE_MODEL_OF_RECTANGULAR_PRISM = new ParticleModel(2);
      public static final ParticleModel PARTICLE_MODEL_OF_TETRAGONAL = new ParticleModel(3);
      public static final ParticleModel PARTICLE_MODEL_OF_TRIGONAL_PRISM = new ParticleModel(4);
      public static final ParticleModel PARTICLE_MODEL_OF_SQUARE_PYRAMID = new ParticleModel(5);
      public static final ParticleModel PARTICLE_MODEL_OF_SIDE_SCATTER_MODIFIED_RECTANGULAR_PRISM = new ParticleModel(6);
   }

   /**
    * A class for identifying MACs. This should probably be eliminated in favor
    * of XRayTransition alone.
    */
   public class MAC
      implements
      Comparable<MAC> {
      public Element elm;
      public XRayTransition xrt;

      public MAC(final Element elm, final XRayTransition xrt) {
         this.elm = elm;
         this.xrt = xrt;
      }

      @Override
      public int compareTo(final MAC mac) {
         if(elm.compareTo(mac.elm) != 0)
            return elm.compareTo(mac.elm);
         else
            return xrt.compareTo(mac.xrt);
      }
   }

   public static int NORMAL_OUTPUT_MODE = 0;
   public static int ELEMENT_WGT_FRAC_OUTPUT_MODE = 1;
   public static int KRATIO_OUTPUT_MODE = 2;

   public static int COMPOSITION_IN_ATOMIC_PROPORTIONS = 8;
   public static int COMPOSITION_IN_ELEMENT_WEIGHT_FRACTIONS = 9;

   private double mBeamEnergy;
   private double mTakeOffAngle;
   private Composition mComposition;
   private int mSampleType;
   private TreeMap<Element, String> mElementToLineMapWithComposition;
   private TreeMap<Element, String> mElementToLineMapWithKRatios;
   private TreeMap<Element, Composition> mStandardsMap;

   private CITZAFCorrectionAlgorithm mCorrectionAlgorithm;

   private MassAbsorptionCoefficient mMACAlgorithm;
   private TreeMap<MAC, Double> mMACs;
   private int mUseStageCoordinates;
   private double mXStageCoordinate;
   private double mYStageCoordinate;
   private double mDeadtimeCorrection;
   private TreeMap<Element, Double> mDriftFactorsMap;
   private String mTitle;
   private int mPrintoutMACs;
   private int mPrintoutZAF;
   private int mMinorElementsConcentration;
   private ParticleModel[] mParticleModels;
   private double[] mParticleDiameters;
   private double mParticleDensity;
   private double mThicknessFactor;
   private double mIntegrationStep;
   private KRatioSet mKRatios;
   private boolean mUsingKRatios;
   private int mCurrentOutputMode = -1;

   public CITZAF() {
      // initialize the values
      clear();
   }

   public void clear() {
      mBeamEnergy = 15.0;
      mTakeOffAngle = 40.0;
      mComposition = new Composition();
      mKRatios = new KRatioSet();
      mSampleType = BULK;
      mElementToLineMapWithComposition = new TreeMap<Element, String>();
      mElementToLineMapWithKRatios = new TreeMap<Element, String>();
      mStandardsMap = new TreeMap<Element, Composition>();
      mCorrectionAlgorithm = CITZAFCorrectionAlgorithm.ARMSTRONG_LOVE_SCOTT;

      mMACAlgorithm = MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_DATA_TABLES;
      mMACs = new TreeMap<MAC, Double>();
      mUseStageCoordinates = DO_NOT_USE_STAGE_COORDINATES;
      mXStageCoordinate = 0.0;
      mYStageCoordinate = 0.0;
      mDeadtimeCorrection = 0.0;
      mDriftFactorsMap = new TreeMap<Element, Double>();
      mTitle = "";
      mPrintoutMACs = DO_NOT_PRINTOUT_MACS;
      mPrintoutZAF = DO_NOT_PRINTOUT_ZAF;
      mMinorElementsConcentration = MINOR_ELEMENTS_CONCENTRATION_IN_NEAREST_HUNDRETH;
      mParticleModels = new ParticleModel[0];
      mParticleDiameters = new double[0];
      mParticleDensity = 1.0;
      mThicknessFactor = 1.0;
      mIntegrationStep = .00001;
      mUsingKRatios = true;
      mCurrentOutputMode = -1;
   }

   /**
    * getBeamEnergy - returns the current beam energy in keV
    * 
    * @return double
    */
   public double getBeamEnergy() {
      return mBeamEnergy;
   }

   /**
    * setBeamEnergy - Sets the beam energy to the value of beamEnergy. Throws an
    * IllegalArgumentException if beamEnergy &lt; 0.0
    * 
    * @param beamEnergy double
    */
   public void setBeamEnergy(final double beamEnergy) {
      if(beamEnergy <= 0.0)
         throw new IllegalArgumentException("Beam energy must be positive and non-zero");
      mBeamEnergy = beamEnergy;
   }

   /**
    * getTakeOffAngle - returns the current takeoff angle
    * 
    * @return double
    */
   public double getTakeOffAngle() {
      return mTakeOffAngle;
   }

   /**
    * setTakeOffAngle - Sets the takeoff angle to the value of TakeOffAngle.
    * Throws an IllegalArgumentException if TakeOffAngle &lt; 0.0
    * 
    * @param TakeOffAngle double
    */
   public void setTakeOffAngle(final double TakeOffAngle) {
      if(TakeOffAngle <= 0.0)
         throw new IllegalArgumentException("Take off angle must be positive or non-zero");
      mTakeOffAngle = TakeOffAngle;
   }

   /**
    * getComposition - returns the current composition of the sample.
    * 
    * @return Composition
    */
   public Composition getComposition() {
      return mComposition.clone();
   }

   /**
    * setComposition - Sets the current composition to the value of mat. NOTE:
    * This method will also reinitialize the standards, erasing all previously
    * stored information about them.
    * 
    * @param mat Composition
    */
   public void setComposition(final Composition mat) {
      if(mComposition == null)
         throw new IllegalArgumentException("Composition cannot be equal to null");
      mComposition = mat.clone();

      mStandardsMap = new TreeMap<Element, Composition>();
   }

   /**
    * getElementSet - returns the an unmodifiable set of the elements in the
    * sample
    * 
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getElementSet() {
      if(mUsingKRatios)
         return mKRatios.getElementSet();
      else
         return mComposition.getElementSet();
   }

   /**
    * getKRatios - returns the k-ratio set of the sample
    * 
    * @return KRatioSet
    */
   public KRatioSet getKRatios() {
      return (KRatioSet) mKRatios.clone();
   }

   /**
    * setKRatios - sets the sample to the value of KRatios. NOTE: This method
    * will also reinitialize the standards and will erase any previous
    * information about them. Throws an IllegalArgumentException if the
    * KRatioSet is null.
    * 
    * @param KRatios KRatioSet
    */
   public void setKRatios(final KRatioSet KRatios) {
      if(KRatios == null)
         throw new IllegalArgumentException("KRatios cannot be null");

      final Set<Element> TheirElementSet = KRatios.getElementSet();
      final Set<Element> MyElementSet = mKRatios.getElementSet();
      if(!(MyElementSet.containsAll(TheirElementSet)) || (MyElementSet.size() != TheirElementSet.size()))
         mStandardsMap = new TreeMap<Element, Composition>();
      mElementToLineMapWithKRatios = new TreeMap<Element, String>();
      for(final XRayTransitionSet xrts : KRatios.getTransitions())
         mElementToLineMapWithKRatios.put(xrts.getElement(), xrts.toString());
      mKRatios = (KRatioSet) KRatios.clone();
   }

   /**
    * getSampleType - returns the kind of sample being stored, either Bulk or
    * Particles/Thin Film
    * 
    * @return int
    */
   public int getSampleType() {
      return mSampleType;
   }

   /**
    * setSampleType - set the type of the current sample. Throws an
    * IllegalArgumentException if the sample type is not Bulk or Particles/Thin
    * Film
    * 
    * @param SampleType int
    */
   public void setSampleType(final int SampleType) {
      if((SampleType != BULK) && (SampleType != PARTICLE_OR_THIN_FILM))
         throw new IllegalArgumentException("SampleType must be either BULK or PARTICLE_OR_THIN_FILM");
      mSampleType = SampleType;
   }

   /**
    * getElementToLineMap - returns a mapping of the elements in the sample to
    * their corresponding spectral lines.
    * 
    * @return Map&lt;Element,String&gt;
    */
   public Map<Element, String> getElementToLineMap() {
      return mUsingKRatios ? new TreeMap<Element, String>(mElementToLineMapWithKRatios)
            : new TreeMap<Element, String>(mElementToLineMapWithComposition);
   }

   public String getLine(final Element elm) {
      return mUsingKRatios ? mElementToLineMapWithKRatios.get(elm) : mElementToLineMapWithComposition.get(elm);
   }

   public boolean hasLine(final Element elm) {
      return mUsingKRatios ? mElementToLineMapWithKRatios.containsKey(elm) : mElementToLineMapWithComposition.containsKey(elm);
   }

   /**
    * setElementToLineMap - set the mapping between the elements in the sample
    * and their corresponding lines. Throws an IllegalArgumentException if the
    * map is null.
    * 
    * @param ElementToLineMap Map&lt;Element,String&gt;
    */
   public void setElementToLineMap(final Map<Element, String> ElementToLineMap) {
      if(ElementToLineMap == null)
         throw new IllegalArgumentException("ElementToLineMap cannot be null");
      if(mUsingKRatios)
         mElementToLineMapWithKRatios = new TreeMap<Element, String>(ElementToLineMap);
      else
         mElementToLineMapWithComposition = new TreeMap<Element, String>(ElementToLineMap);
   }

   public void setLine(final Element elm, final Line line) {
      if(mUsingKRatios) {
         if(mElementToLineMapWithKRatios.containsKey(elm))
            mElementToLineMapWithKRatios.remove(elm);
         mElementToLineMapWithKRatios.put(elm, line.toString());
      } else {
         if(mElementToLineMapWithComposition.containsKey(elm))
            mElementToLineMapWithComposition.remove(elm);
         mElementToLineMapWithComposition.put(elm, line.toString());
      }
   }

   /**
    * getAStandard - returns an array of the standards in the sample.
    * 
    * @return Composition
    */
   public Composition getAStandard(final Element elm) {
      if(mStandardsMap.containsKey(elm))
         return mStandardsMap.get(elm).clone();
      else
         throw new IllegalArgumentException("no standard found for " + elm.toAbbrev());
   }

   /**
    * setAStandard - Sets an individual standard
    * 
    * @param elm Element
    * @param mat Composition
    * @throws IllegalArgumentException (mat == null)
    */
   public void setAStandard(final Element elm, final Composition mat) {
      if(!getElementSet().contains(elm))
         throw new IllegalArgumentException(elm.toAbbrev() + " is not in the sample");
      if(mat == null)
         throw new IllegalArgumentException("mat cannot be null");
      if(mat.getElementCount() == 0)
         mStandardsMap.remove(elm);
      else
         mStandardsMap.put(elm, mat);
   }

   public boolean hasStandard(final Element elm) {
      return mStandardsMap.containsKey(elm);
   }

   public void clearStandards() {
      mStandardsMap = new TreeMap<Element, Composition>();
   }

   /**
    * getCorrectionAlgorithm - returns the CITZAF correction algorithm
    * 
    * @return CITZAFCorrectionAlgorithm
    */
   public CITZAFCorrectionAlgorithm getCorrectionAlgorithm() {
      return mCorrectionAlgorithm;
   }

   /**
    * setCorrectionAlgorithm - set the CITZAF correction algorithm to be used by
    * the analysis program
    * 
    * @param algorithm String
    * @throws IllegalArgumentException (algorithm not in CITZAFAlgorithms)
    */
   public void setCorrectionAlgorithm(final String algorithm) {
      final CITZAFCorrectionAlgorithm[] allAlgorithms = CITZAFCorrectionAlgorithm.getAllImplimentations();
      int counter = 0;
      while((counter < allAlgorithms.length) && (!algorithm.equals(allAlgorithms[counter].toString())))
         counter++;
      if(counter < allAlgorithms.length)
         mCorrectionAlgorithm = allAlgorithms[counter];
      else
         throw new IllegalArgumentException("algorithm is not a recognized algorithm name");
   }

   /**
    * getMACAlgorithm - returns the Mass Absorption Coefficient algorithm being
    * used
    * 
    * @return MassAbsorptionCoefficient
    */
   public MassAbsorptionCoefficient getMACAlgorithm() {
      return mMACAlgorithm;
   }

   /**
    * setMACAlgorithm - set the Mass Absorption Coefficient to be used
    * 
    * @param MAC String
    * @throws IllegalArgumentException (MAC not in MACAlgorithms)
    */
   public void setMACAlgorithm(final String MAC) {
      final MassAbsorptionCoefficient[] allMACs = MassAbsorptionCoefficient.getAllImplimentations();
      int counter = 0;
      while((counter < allMACs.length) && (!MAC.matches(allMACs[counter].toString())))
         counter++;
      if(counter < allMACs.length)
         mMACAlgorithm = allMACs[counter];
      else
         throw new IllegalArgumentException("MAC is not a recognized name for a MAC algorithm");
   }

   /**
    * getMACs - returns the raw MAC data for the sample
    * 
    * @return double
    */
   public double getMAC(final Element elm, final XRayTransition xrt) {
      return mMACs.get(new MAC(elm, xrt));
   }

   /**
    * setMACs - set the raw MAC data for the sample
    * 
    * @param elm Element
    * @param xrt XRayTransition
    * @param MAC double
    */
   public void setMAC(final Element elm, final XRayTransition xrt, final double MAC) {
      mMACs.put(new MAC(elm, xrt), Double.valueOf(MAC));
   }

   public void clearMACs() {
      mMACs = new TreeMap<MAC, Double>();
   }

   public boolean hasMAC(final Element elm, final XRayTransition xrt) {
      return mMACs.containsKey(new MAC(elm, xrt));
   }

   /**
    * isUsingStageCoordinates - returns whether or not StageCoordinates will be
    * used in the analysis
    * 
    * @return boolean
    */
   public boolean isUsingStageCoordinates() {
      return mUseStageCoordinates == USE_STAGE_COORDINATES;
   }

   /**
    * setUseStageCoordinates - sets whether or not Stage Coordinates will be
    * used in the analysis
    * 
    * @param UseStageCoordinates int
    * @throws IllegalArgumentException ((UseStageCoordinates !=
    *            USE_STAGE_COORDINATES) &amp;&amp; (UseStageCoordinates !=
    *            DO_NOT_USE_STAGE_COORDINATES))
    */
   public void setUseStageCoordinates(final int UseStageCoordinates) {
      if((UseStageCoordinates != USE_STAGE_COORDINATES) && (UseStageCoordinates != DO_NOT_USE_STAGE_COORDINATES))
         throw new IllegalArgumentException("Unrecognized code for UseStageCoordinates");
      mUseStageCoordinates = UseStageCoordinates;
   }

   /**
    * getXStageCoordinate - returns the X stage coordinate
    * 
    * @return double
    */
   public double getXStageCoordinate() {
      return mXStageCoordinate;
   }

   /**
    * setXStageCoordinate - set the X stage coordinate
    * 
    * @param XStageCoordinate double
    */
   public void setXStageCoordinate(final double XStageCoordinate) {
      mXStageCoordinate = XStageCoordinate;
   }

   /**
    * getYStageCoordinate - returns the Y stage coordinate
    * 
    * @return double
    */
   public double getYStageCoordinate() {
      return mYStageCoordinate;
   }

   /**
    * setYStageCoordinate - set the Y stage coordinate
    * 
    * @param YStageCoordinate double
    */
   public void setYStageCoordinate(final double YStageCoordinate) {
      mYStageCoordinate = YStageCoordinate;
   }

   /**
    * getDeadtimeCorrection - returns the deadtime correction in microseconds
    * 
    * @return double
    */
   public double getDeadtimeCorrection() {
      return mDeadtimeCorrection;
   }

   /**
    * setDeadtimeCorrection - set the deadtime correction
    * 
    * @param DeadtimeCorrection double
    * @throws IllegalArgumentException (DeadtimeCorrection &lt; 0)
    */
   public void setDeadtimeCorrection(final double DeadtimeCorrection) {
      if(DeadtimeCorrection < 0.0)
         throw new IllegalArgumentException("DeadtimeCorrection must be positive");
      mDeadtimeCorrection = DeadtimeCorrection;
   }

   /**
    * getDriftFactors - returns an array of drift factors
    * 
    * @return double[]
    */
   public double getDriftFactor(final Element elm) {
      if(mDriftFactorsMap.containsKey(elm))
         return mDriftFactorsMap.get(elm);
      else
         throw new IllegalArgumentException(elm.toAbbrev() + " does not have a drift factor");
   }

   /**
    * setDriftFactors - set the drift factors for the analysis program
    * 
    * @param elm Element
    * @param DriftFactor double
    * @throws IllegalArgumentException (DriftFactors == null)
    */
   public void setDriftFactor(final Element elm, final double DriftFactor) {
      if(DriftFactor <= 0)
         throw new IllegalArgumentException("DriftFactor must be positive and nonzero");
      if(!getElementSet().contains(elm))
         throw new IllegalArgumentException(elm.toAbbrev() + " is not in the sample");
      mDriftFactorsMap.put(elm, Double.valueOf(DriftFactor));
   }

   public void clearDriftFactors() {
      mDriftFactorsMap = new TreeMap<Element, Double>();
   }

   public boolean hasDriftFactor(final Element elm) {
      return mDriftFactorsMap.containsKey(elm);
   }

   /**
    * getTitle - returns the title used in the analysis program
    * 
    * @return String
    */
   public String getTitle() {
      return mTitle;
   }

   /**
    * setTitle - set the title used in the analysis program
    * 
    * @param Title String
    */
   public void setTitle(final String Title) {
      mTitle = Title;
   }

   /**
    * getPrintoutMACs - returns the code for printing out MACs
    * 
    * @return int
    */
   public int getPrintoutMACs() {
      return mPrintoutMACs;
   }

   /**
    * setPrintoutMACs - sets the code for printing out MACs
    * 
    * @param PrintoutMACs int
    */
   public void setPrintoutMACs(final int PrintoutMACs) {
      if((PrintoutMACs == PRINTOUT_MACS) || (PrintoutMACs != DO_NOT_PRINTOUT_MACS))
         mPrintoutMACs = PrintoutMACs;
      else
         assert (false);
   }

   /**
    * getPrintoutZAF - returns the code for printing out ZAF correction in the
    * analysis program
    * 
    * @return int
    */
   public int getPrintoutZAF() {
      return mPrintoutZAF;
   }

   /**
    * setPrintoutZAF - sets the code for printing out ZAF corrections in the
    * analysis program
    * 
    * @param PrintoutZAF int
    * @throws IllegalArgumentException ((PrintoutZAF != PRINTOUT_ZAF) &amp;&amp;
    *            (PrintoutZAF != DO_NOT_PRINTOUT_ZAF))
    */
   public void setPrintoutZAF(final int PrintoutZAF) {
      if((PrintoutZAF != PRINTOUT_ZAF) && (PrintoutZAF != DO_NOT_PRINTOUT_ZAF))
         throw new IllegalArgumentException("Unrecognized code for PrintoutZAF");
      mPrintoutZAF = PrintoutZAF;
   }

   /**
    * getMinorElementsOutput - returns the code for printing out minor element's
    * concentrations
    * 
    * @return int
    */
   public int getMinorElementsOutput() {
      return mMinorElementsConcentration;
   }

   /**
    * setMinorElementsOutput - set the code for printing out minor element's
    * concentrations
    * 
    * @param MinorElementsConcentration int
    * @throws IllegalArgumentException ((MinorElementsConcentration !=
    *            MINOR_ELEMENTS_CONCENTRATION_IN_NEAREST_HUNDRETH) &amp;&amp;
    *            (MinorElementsConcentration !=
    *            MINOR_ELEMENTS_CONCENTRATION_IN_PPM))
    */
   public void setMinorElementsOutput(final int MinorElementsConcentration) {
      if((MinorElementsConcentration != MINOR_ELEMENTS_CONCENTRATION_IN_NEAREST_HUNDRETH)
            && (MinorElementsConcentration != MINOR_ELEMENTS_CONCENTRATION_IN_PPM))
         throw new IllegalArgumentException("MinorElementsConcentration not recognized");
      mMinorElementsConcentration = MinorElementsConcentration;
   }

   /**
    * getParticleModels - returns an array of the particle models used to
    * represent the sample
    * 
    * @return ParticleModel[]
    */
   public ParticleModel[] getParticleModels() {
      return mParticleModels;
   }

   /**
    * setParticleModels - sets the list of models being used to represent the
    * sample during analysis
    * 
    * @param models Vector
    * @throws IllegalArgumentException (x is an element of models &amp;&amp; x
    *            is not a Particle model)
    */
   public void setParticleModels(final ArrayList<String> models) {
      final ParticleModel[] modelArray = new ParticleModel[models.size()];
      final ParticleModel[] allModels = ParticleModel.getAllImplimentations();
      for(int index = 0; index < models.size(); index++) {
         final String currentModel = models.get(index).toString();
         int Counter = 0;
         while((Counter < allModels.length) && (!currentModel.matches(allModels[Counter].toString())))
            Counter++;
         if(Counter < allModels.length)
            modelArray[index] = allModels[Counter];
         else
            throw new IllegalArgumentException("Unrecognized model type");
      }
      mParticleModels = modelArray;
   }

   /**
    * getParticleDiameters - returns an array of the particle diameters
    * 
    * @return double[]
    */
   public double[] getParticleDiameters() {
      return mParticleDiameters;
   }

   /**
    * setParticleDiameters - sets the list of diameters that will represent the
    * sample during analysis
    * 
    * @param ParticleDiameters Vector
    * @throws IllegalArgumentException (x is an element of ParticleDiameters
    *            &amp;&amp; x &lt; 0)
    */
   public void setParticleDiameters(final ArrayList<Double> ParticleDiameters) {
      final double[] Diameters = new double[ParticleDiameters.size()];
      for(int index = 0; index < Diameters.length; index++) {
         final double value = ParticleDiameters.get(index);
         if(value < 0.0)
            throw new IllegalArgumentException("ParticleDiameters must be positive");
         Diameters[index] = value;
      }
      mParticleDiameters = Diameters;
   }

   /**
    * getParticleDensity - returns the density of the particle
    * 
    * @return double
    */
   public double getParticleDensity() {
      return mParticleDensity;
   }

   /**
    * setParticleDensity - sets the density of the particle
    * 
    * @param Density double
    * @throws IllegalArgumentException (Density &lt; 0)
    */
   public void setParticleDensity(final double Density) {
      if(Density < 0)
         throw new IllegalArgumentException("Density must be positive");
      mParticleDensity = Density;
   }

   /**
    * getThicknessFactor - returns the ratio of the particle's thickness to its
    * diameter
    * 
    * @return double
    */
   public double getThicknessFactor() {
      return mThicknessFactor;
   }

   /**
    * setThicknessFactor - sets the ratio of the particle's thickness to its
    * diameter
    * 
    * @param ThicknessFactor double
    * @throws IllegalArgumentException (ThicknessFactor &lt; 0)
    */
   public void setThicknessFactor(final double ThicknessFactor) {
      if(ThicknessFactor < 0.0)
         throw new IllegalArgumentException("ThicknessFactor must be positive");
      mThicknessFactor = ThicknessFactor;
   }

   /**
    * getNumericalIntegrationStep - returns the numerical integration step
    * 
    * @return double
    */
   public double getNumericalIntegrationStep() {
      return mIntegrationStep;
   }

   /**
    * setNumericalIntegrationStep - sets the numerical integration step
    * 
    * @param IntegrationStep double
    * @throws IllegalArgumentException (IntegrationStep &lt; 0)
    */
   public void setNumericalIntegrationStep(final double IntegrationStep) {
      if(IntegrationStep < 0.0)
         throw new IllegalArgumentException("IntegrationStep must be positive");
      mIntegrationStep = IntegrationStep;
   }

   /**
    * getUsingKRatios - returns true if the analysis program will run using
    * k-ratios to calculate composition
    * 
    * @return boolean
    */
   public boolean getUsingKRatios() {
      return mUsingKRatios;
   }

   /**
    * setUsingKRatios - sets whether or not the analysis program will use
    * k-ratios to calculate composition
    * 
    * @param usingKRatios boolean
    */
   public void setUsingKRatios(final boolean usingKRatios) {
      mUsingKRatios = usingKRatios;
   }

   /**
    * dumpToFile - writes the CITZAF information to the datain file (but it does
    * NOT run the analysis program)
    */
   public void dumpToFile() {
      writeDatainFile();
   }

   /**
    * dumpToFileAndRunCITZAF - writes the CITZAF information to the datain file
    * and runs the analysis program
    */
   public void dumpToFileAndRunCITZAF() {
      dumpToFile();
      chooseOutputOptions(0, true);
      runCITZAFProgram();
   }

   private void writeDatainFile() {
      final File file = new File(FileLocation + "datain");
      try {
         try (final OutputStream os = new FileOutputStream(file)) {
            try (final PrintWriter pw = new PrintWriter(os)) {
               pw.println(mPrintoutMACs + ", " + mPrintoutZAF + ", " + mSampleType);
               pw.print(mMACAlgorithm.getCode());
               final int[] algorithm = mCorrectionAlgorithm.getAlgorithm();
               for(final int element : algorithm)
                  pw.print(", " + element);
               pw.println();
               pw.println("\"" + mTitle + "\"");
               pw.println(mBeamEnergy + ", " + mTakeOffAngle);

               final Set<Element> MasterElementsSet = getMasterElementSet();
               final TreeMap<Element, Integer> MasterElementSetIndexMap = new TreeMap<Element, Integer>();
               pw.println(MasterElementsSet.size());
               for(final Element elm : MasterElementsSet) {
                  pw.print(elm.getAtomicNumber() + ", ");

                  MasterElementSetIndexMap.put(elm, Integer.valueOf(MasterElementSetIndexMap.size() + 1));

                  Line line = null;
                  if(getElementToLineMap().containsKey(elm)) {
                     final String t = getElementToLineMap().get(elm).toString();
                     if(t.contains(" K "))
                        line = Line.KLINE;
                     else if(t.contains(" LIII "))
                        line = Line.LLINE;
                     else if((t.contains(" MV ")))
                        line = Line.MLINE;
                     if(line != null)
                        pw.print(line.getCode() + ", ");
                  } else
                     pw.print(-1 + ", ");

                  // grav factor set to 0 for now
                  pw.println(0);
               }

               if((mMACAlgorithm == MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_TABLES_AND_USER_ENTRY)
                     || (mMACAlgorithm == MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_AND_USER_ENTRY)) {
                  for(final MAC mac : mMACs.keySet())
                     pw.println(MasterElementSetIndexMap.get(mac.elm) + "," + MasterElementSetIndexMap.get(mac.xrt.getElement())
                           + "," + mMACs.get(mac));
                  pw.println("0");
               }

               pw.println(mMinorElementsConcentration + ", " + mDeadtimeCorrection + ", " + mUseStageCoordinates);

               if(mStandardsMap.size() > 0)
                  for(final Element elm : mStandardsMap.keySet()) {
                     final Composition standard = mStandardsMap.get(elm);
                     if(standard.getElementCount() > 0) {
                        pw.print(COMPOSITION_IN_ELEMENT_WEIGHT_FRACTIONS + " " + MasterElementSetIndexMap.get(elm).toString()
                              + " \"" + standard.getName() + "\"");
                        for(final Element standardElm : MasterElementsSet) {
                           double Quantity = 0.0;
                           if(standard.containsElement(standardElm))
                              Quantity = standard.weightFraction(standardElm, false);
                           pw.print(" " + Quantity);

                        }
                        pw.println();
                     }
                  }
               if((!mUsingKRatios) && (mComposition.getElementCount() > 0)) {
                  pw.print("1 1 ");
                  for(final Element elm : mComposition.getElementSet()) {
                     pw.print(mComposition.weightFraction(elm, false));
                     pw.print(" ");
                  }
                  pw.println();
               }

               if(mDriftFactorsMap.size() > 0) {
                  pw.print("2 ");
                  for(final Element elm : getElementSet()) {
                     pw.print(mDriftFactorsMap.get(elm).toString());
                     pw.print(" ");
                  }
                  pw.println();
               }

               if((mUsingKRatios) && (mKRatios.size() > 0)) {
                  pw.print(" 3 , 1");
                  for(final Element elm : getElementSet()) {
                     final String family = mElementToLineMapWithKRatios.get(elm);
                     int shell = AtomicShell.NoShell;
                     if(family.contains(" K "))
                        shell = AtomicShell.K;
                     else if(family.contains(" LIII "))
                        shell = AtomicShell.LIII;
                     else if(family.contains(" MV "))
                        shell = AtomicShell.MV;
                     pw.print(" , " + mKRatios.getKRatio(new XRayTransitionSet(new AtomicShell(elm, shell))));
                     // pw.print(" , " + mat.weightPercent(elm, false));
                  }
                  if(mUseStageCoordinates == USE_STAGE_COORDINATES)
                     pw.print(" , " + mXStageCoordinate + " , " + mYStageCoordinate);
                  pw.println();
               }

               if(mSampleType == PARTICLE_OR_THIN_FILM) {
                  pw.print(" 13 , ");
                  pw.print(mParticleModels.length);
                  for(final ParticleModel particleModel : mParticleModels) {
                     pw.print(" , ");
                     pw.print(particleModel.getCode());
                  }
                  pw.println();

                  pw.print(" 14 , ");
                  pw.print(mParticleDiameters.length);
                  for(final double particleDiameter : mParticleDiameters) {
                     pw.print(" , ");
                     pw.print(particleDiameter);
                  }
                  pw.println();

                  pw.print(" 15 , ");
                  pw.println(mParticleDensity);

                  pw.print(" 16 , ");
                  pw.println(mThicknessFactor);

                  pw.print(" 17 , ");
                  pw.println(mIntegrationStep);
               }

               pw.print("12");
            }
         }
      }
      catch(final IOException ex) {
         ex.printStackTrace(System.err);
      }
   }

   private void chooseOutputOptions(final int PrintoutMode, final boolean normalize) {
      try {
         mCurrentOutputMode = PrintoutMode;
         final File file = new File(FileLocation + "INITZAF.DAT");
         try (final OutputStream os = new FileOutputStream(file)) {
            try (final PrintWriter pw = new PrintWriter(os)) {
               // pw.println("10 " + mPrintoutZAF + " 1 1 1 0");
               pw.print(" " + PrintoutMode);
               pw.println(" 0 " + (normalize ? "1" : "0") + " 0 1 0");
            }
         }
      }
      catch(final IOException ioex) {
         ioex.printStackTrace(System.err);
      }
   }

   private void runCITZAFProgram() {
      try {
         final Runtime rt = Runtime.getRuntime();
         final Process proc = rt.exec("cmd");
         try (final InputStream in = proc.getInputStream()) {

            final ConsoleInputReader cir = new ConsoleInputReader(in);
            final Thread th = new Thread(cir);
            th.start();
            try (final OutputStream out = proc.getOutputStream()) {
               out.write(("cd " + FileLocation + "\n").getBytes());
               out.write("citzaf1\n".getBytes());
               out.write("citzaf2 4\n".getBytes());
            }
            proc.waitFor();
         }
         // rt.exec("notepad c:\\" + FileLocation + "dataout");

      }
      catch(final InterruptedException ex2) {
         ex2.printStackTrace(System.err);
      }
      catch(final IOException ioex) {
         ioex.printStackTrace(System.err);
      }
   }

   private Set<Element> getMasterElementSet() {
      final Set<Element> MasterElementsSet = new TreeSet<Element>();
      if((mUsingKRatios) && (mKRatios.size() > 0))
         MasterElementsSet.addAll(mKRatios.getElementSet());
      else if(mComposition.getElementCount() > 0)
         MasterElementsSet.addAll(mComposition.getElementSet());
      for(final Element elm : mStandardsMap.keySet())
         MasterElementsSet.addAll(mStandardsMap.get(elm).getElementSet());
      return MasterElementsSet;
   }

   /**
    * getCondensedResults - will return a string with only the sample data on
    * it, in a condensed form.
    * 
    * @throws IOException - if the Dataout file is in the wrong format(due to an
    *            error in writing the file) or the method fails to read
    *            correctly, this exception will be thrown
    * @return String
    */
   public String getCondensedResults()
         throws IOException {
      if((mUsingKRatios) && (mCurrentOutputMode != KRATIO_OUTPUT_MODE)) {
         chooseOutputOptions(KRATIO_OUTPUT_MODE, true);
         runCITZAFProgram();
      } else if((!mUsingKRatios) && (mCurrentOutputMode != ELEMENT_WGT_FRAC_OUTPUT_MODE)) {
         chooseOutputOptions(ELEMENT_WGT_FRAC_OUTPUT_MODE, true);
         runCITZAFProgram();
      }

      final StringBuffer sb = new StringBuffer();
      final File file = new File(FileLocation + "dataout");
      try {
         try (final InputStream is = new FileInputStream(file)) {
            try (final Reader rd = new InputStreamReader(is, Charset.forName("US-ASCII"))) {
               try (final BufferedReader br = new BufferedReader(rd)) {

                  br.mark(300);
                  while(!br.readLine().trim().startsWith("SMP"))
                     br.mark(300);
                  br.reset();

                  // The file actually uses one line per sample, but
                  // since there is only one sample, just read the header
                  // and that one line
                  sb.append(br.readLine().trim());
                  sb.append('\n');
                  // omit the extra line
                  br.readLine();
                  sb.append(br.readLine().trim());
               }
            }
         }
      }
      catch(final NullPointerException npex) {
         throw new IOException("Error while reading the dataout file");
      }
      return sb.toString();
   }

   /**
    * getFullResults - reads the full results from the dataout file
    * 
    * @throws IOException - if the Dataout file is in the wrong format(due to an
    *            error in writing the file) or the method fails to read
    *            correctly, this exception will be thrown
    * @return String
    */
   public String getFullResults()
         throws IOException {
      if(mCurrentOutputMode != NORMAL_OUTPUT_MODE) {
         chooseOutputOptions(NORMAL_OUTPUT_MODE, true);
         runCITZAFProgram();
      }
      final StringBuffer sb = new StringBuffer();
      final int numElements = getMasterElementSet().size();
      int numElementsInSample;
      if(mUsingKRatios)
         numElementsInSample = mKRatios.size();
      else
         numElementsInSample = mComposition.getElementCount();
      final File file = new File(FileLocation + "dataout");
      try {
         try (final InputStream is = new FileInputStream(file)) {
            try (final Reader rd = new InputStreamReader(is, Charset.forName("US-ASCII"))) {
               try (final BufferedReader br = new BufferedReader(rd)) {

                  // Read MACs
                  if(mPrintoutMACs == PRINTOUT_MACS) {
                     br.mark(300);
                     while(!br.readLine().startsWith("Z-LINE"))
                        br.mark(300);
                     br.reset();
                     // read MAC header
                     sb.append(br.readLine());
                     sb.append('\n');

                     br.readLine();
                     for(int index = 0; index < (numElements * numElements); index++) {
                        sb.append(br.readLine());
                        sb.append('\n');
                     }
                     sb.append('\n');
                  }

                  if(mPrintoutZAF == PRINTOUT_ZAF) {
                     // Read ZAF Corrections
                     br.mark(300);
                     while(!br.readLine().startsWith("ELEMENT"))
                        br.mark(300);
                     br.reset();

                     // read ZAF header
                     sb.append(br.readLine());
                     sb.append('\n');

                     br.readLine();
                     for(int index = 0; index < numElements; index++) {
                        sb.append(br.readLine());
                        sb.append('\n');
                     }
                     sb.append('\n');
                     sb.append("************************************************\n");

                     // read standards
                     for(int index = 0; index < mStandardsMap.size(); index++) {
                        while(!br.readLine().startsWith("ELEMENT"))
                           br.mark(300);
                        br.reset();

                        // read standard header
                        sb.append(br.readLine());
                        sb.append('\n');

                        br.readLine();
                        String line = br.readLine();
                        while(!line.startsWith("-")) {
                           sb.append(line);
                           sb.append('\n');
                           line = br.readLine();
                        }
                        sb.append("************************************************\n");
                     }
                  }

                  // Read Sample Results
                  while(!br.readLine().startsWith("SAMPLE")) {
                  }

                  br.readLine();
                  if(mSampleType == PARTICLE_OR_THIN_FILM)
                     br.readLine();
                  // sb.append(br.readLine());
                  // sb.append('\n');
                  if(isUsingStageCoordinates())
                     br.readLine();
                  // sb.append(br.readLine());
                  // sb.append('\n');

                  br.readLine();

                  // read header
                  sb.append(br.readLine());
                  sb.append('\n');
                  br.readLine();

                  for(int index = 0; index < numElementsInSample; index++) {
                     sb.append(br.readLine());
                     sb.append('\n');
                  }
                  br.readLine();
                  sb.append(br.readLine());
                  sb.append('\n');
                  sb.append('\n');

                  if(mPrintoutZAF == PRINTOUT_ZAF) {
                     br.readLine();
                     br.readLine();

                     for(int index = 0; index < (2 + numElementsInSample); index++) {
                        sb.append(br.readLine());
                        sb.append('\n');
                     }
                  }
               }
            }
         }
      }
      catch(final NullPointerException npex) {
         throw new IOException("Error when reading dataout file");
      }
      return sb.toString();
   }

   public boolean isCITZAFSupported() {
      final String OS = System.getProperty("os.name");
      return (OS.startsWith("Windows"));
   }

   public String SummarizeInputs() {
      final StringBuffer sb = new StringBuffer();
      if((!getTitle().matches("Enter your sample title here")) && (!getTitle().matches("")))
         sb.append("\"" + getTitle() + "\"\n");
      sb.append(writeDate());
      sb.append("Beam Energy: ");
      sb.append(getBeamEnergy());
      sb.append(" keV\n");
      sb.append("Take Off Angle: ");
      sb.append(getTakeOffAngle());
      sb.append(" degrees\n");
      sb.append("Sample: ");
      if(getUsingKRatios()) {
         sb.append(formatKRatios(getKRatios()));
         sb.append('\n');
      } else {
         sb.append(formatComposition(getComposition()));
         sb.append('\n');
      }
      sb.append("Sample Type: ");
      if(getSampleType() == BULK)
         sb.append("Bulk");
      else {
         sb.append("Particle or Thin Film");
         sb.append('\n');
         sb.append("Particle Model(s): ");
         sb.append(formatParticleModelsText(getParticleModels()));
         sb.append('\n');
         sb.append("Particle Diameter(s): ");
         sb.append(formatParticleDiameterText(getParticleDiameters()));
         sb.append('\n');
         sb.append("Particle Density: ");
         sb.append(getParticleDensity());
         sb.append('\n');
         sb.append("Thickness Factor: ");
         sb.append(getThicknessFactor());
         sb.append('\n');
         sb.append("Integration Step: ");
         sb.append(getNumericalIntegrationStep());
      }
      sb.append('\n');

      if(mStandardsMap.size() > 0)
         for(final Element elm : getElementSet())
            if(hasStandard(elm)) {
               sb.append("Standard for ");
               sb.append(elm.toAbbrev());
               sb.append(": ");
               sb.append(formatComposition(getAStandard(elm)));
               sb.append('\n');
            }

      sb.append("Correction used: ");
      sb.append(getCorrectionAlgorithm().toString());
      sb.append('\n');

      sb.append("MAC used: ");
      sb.append(getMACAlgorithm().toString());
      sb.append('\n');

      if(isUsingStageCoordinates()) {
         sb.append("Stage Coordinates (x,y): (");
         sb.append(getXStageCoordinate());
         sb.append(",");
         sb.append(getYStageCoordinate());
         sb.append(")\n");
      }

      if(getDeadtimeCorrection() > 0.0) {
         sb.append("Deadtime Correction: ");
         sb.append(getDeadtimeCorrection());
         sb.append("(�sec)\n");
      }

      if(!areDriftFactorsAtDefault()) {
         sb.append("Drift Factors: ");
         for(final Iterator<Element> i = getElementSet().iterator(); i.hasNext();) {
            final Element elm = i.next();
            sb.append(elm.toAbbrev());
            sb.append("=");
            sb.append(getDriftFactor(elm));
            if(i.hasNext())
               sb.append(", ");
            else
               sb.append('\n');
         }
      }
      return sb.toString();
   }

   private String writeDate() {
      final Calendar cal = Calendar.getInstance();
      final String month = getMonth(cal.get(Calendar.MONTH));

      final String Day = getDay(cal.get(Calendar.DAY_OF_WEEK));

      String HourStr = Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
      if(HourStr.length() == 1)
         HourStr = "0" + HourStr;
      String MinuteStr = Integer.toString(cal.get(Calendar.MINUTE));
      if(MinuteStr.length() == 1)
         MinuteStr = "0" + MinuteStr;
      String SecStr = Integer.toString(cal.get(Calendar.SECOND));
      if(SecStr.length() == 1)
         SecStr = "0" + SecStr;
      final StringBuffer date = new StringBuffer();
      date.append("Time: ");
      date.append(HourStr);
      date.append(":");
      date.append(MinuteStr);
      date.append(":");
      date.append(SecStr);
      date.append(" on ");
      date.append(Day + ", ");
      date.append(month);
      date.append(" ");
      date.append(cal.get(Calendar.DATE));
      date.append(", ");
      date.append(cal.get(Calendar.YEAR));
      date.append("\n");
      return date.toString();
   }

   private String getMonth(final int month) {
      switch(month) {
         case Calendar.JANUARY:
            return "January";
         case Calendar.FEBRUARY:
            return "February";
         case Calendar.MARCH:
            return "March";
         case Calendar.APRIL:
            return "April";
         case Calendar.MAY:
            return "May";
         case Calendar.JUNE:
            return "June";
         case Calendar.JULY:
            return "July";
         case Calendar.AUGUST:
            return "August";
         case Calendar.SEPTEMBER:
            return "September";
         case Calendar.OCTOBER:
            return "October";
         case Calendar.NOVEMBER:
            return "November";
         case Calendar.DECEMBER:
            return "December";
         default:
            return "Month Unknown!";
      }
   }

   private String getDay(final int day) {
      switch(day) {
         case Calendar.SUNDAY:
            return "Sunday";
         case Calendar.MONDAY:
            return "Monday";
         case Calendar.TUESDAY:
            return "Tuesday";
         case Calendar.WEDNESDAY:
            return "Wednesday";
         case Calendar.THURSDAY:
            return "Thursday";
         case Calendar.FRIDAY:
            return "Friday";
         case Calendar.SATURDAY:
            return "Saturday";
         default:
            return "Unknown Day!";
      }
   }

   public String formatComposition(final Composition mat) {
      if(mat.getElementCount() == 0)
         return "NO SAMPLE SELECTED";
      else {
         final StringBuffer res = new StringBuffer();
         final NumberFormat nf = new HalfUpFormat("#.####,-#.####");
         for(final Element el : mat.getElementSet()) {
            res.append(nf.format(mat.weightFraction(el, true) * 100.0));
            res.append("% ");
            res.append(el.toAbbrev());
            res.append(", ");
         }
         final int p = res.lastIndexOf(", ");
         res.delete(p, p + 2);
         return res.toString();
      }
   }

   public String formatKRatios(final KRatioSet kRatios) {
      if(kRatios.size() > 0) {
         final StringBuffer res = new StringBuffer();
         final NumberFormat nf = new HalfUpFormat("0.####");
         boolean first = true;
         for(final XRayTransitionSet xrts : kRatios.keySet()) {
            if(!first)
               res.append(", ");
            first = false;
            res.append(xrts.toString());
            res.append(": ");
            res.append(nf.format(kRatios.getKRatio(xrts)));
         }
         return res.toString();
      } else
         return "NO K-RATIOS SELECTED";
   }

   public String formatParticleDiameterText(final double[] Diameters) {
      final StringBuffer StringRep = new StringBuffer();

      if(Diameters.length != 0) {
         StringRep.append(Diameters.length);
         StringRep.append(" diameters: ");
         for(int index = 0; index < Diameters.length; index++) {
            if(index != 0)
               StringRep.append(", ");
            StringRep.append(Diameters[index]);
            StringRep.append("�m");
         }
         return StringRep.toString();
      } else
         return "No Diameters Selected";
   }

   public String formatParticleModelsText(final ParticleModel[] pm) {
      final StringBuffer temp = new StringBuffer();
      if(pm.length > 0) {
         temp.append(pm.length);
         temp.append(" models: ");
         for(int index = 0; index < pm.length; index++) {
            if(index != 0)
               temp.append(", ");
            temp.append(pm[index].toString());
         }
         return temp.toString();
      } else
         return "No models selected";
   }

   private boolean areDriftFactorsAtDefault() {
      for(final Element elm : getElementSet())
         if(getDriftFactor(elm) != 1.0)
            return false;
      return true;
   }

   class ConsoleInputReader
      implements
      Runnable {
      InputStream in;

      ConsoleInputReader(final InputStream in) {
         this.in = in;
      }

      @Override
      public void run() {
         try {
            final BufferedReader bufferreader = new BufferedReader(new InputStreamReader(in));
            String line = bufferreader.readLine();
            while(line != null)
               // System.out.println(line);
               line = bufferreader.readLine();
         }
         catch(final Exception e) {
            e.printStackTrace();
         }
      }
   }
}

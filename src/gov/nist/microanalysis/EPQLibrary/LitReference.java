package gov.nist.microanalysis.EPQLibrary;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * <p>
 * The Reference class abstracts the idea of a literature reference.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
abstract public class LitReference {

   protected LitReference() {
   }

   abstract public String getShortForm();

   abstract public String getLongForm();

   /**
    * A class for representing human authors.
    */
   static public class Author {
      private final String mFirst;
      private final String mLast;
      private String mAffiliation;

      public Author(String first, String last, String affiliation) {
         mFirst = first.trim();
         mLast = last.trim();
         mAffiliation = affiliation.trim();
      }

      public Author(String first, String last) {
         mFirst = first.trim();
         mLast = last.trim();
      }

      public String getAuthor() {
         return mLast + " " + mFirst.substring(0, 1);
      }

      public String getAffiliation() {
         return mAffiliation;
      }

      public String getFirst() {
         return mFirst;
      }

      public String getLast() {
         return mLast;
      }

      static public String toString(Author[] authors) {
         final StringBuffer sb = new StringBuffer();
         for(int i = 0; i < authors.length; ++i) {
            if(i != 0)
               sb.append(i == (authors.length - 1) ? " & " : ", ");
            sb.append(authors[i].getAuthor());
         }
         return sb.toString();
      }

      static public String toAbbrev(Author[] authors) {
         if(authors.length > 1)
            return authors[0].mLast + " et al";
         else if(authors.length > 0)
            return authors[0].mLast;
         else
            return "";
      }
   }

   // Some prolific establishments
   static public final String ONERA = "Office National d'Etudes et de Recherche Aerospatiales";
   static public final String NIST = "National Institute of Standards & Technology";
   static public final String NBS = "National Bureau of Standards";
   static public final String LehighU = "Lehigh University";
   static public final String Eindhoven = "University of Technology, Eindhoven";

   // Some prolific authors
   static public final Author DNewbury = new Author("Dale", "Newbury", NIST);
   static public final Author KHeinrich = new Author("Kurt", "Heinrich", NIST);
   static public final Author JPouchou = new Author("Jean-Louis", "Pouchou", ONERA);
   static public final Author FPichoir = new Author("Franciose", "Pichoir", ONERA);
   static public final Author RCastaing = new Author("R", "Castaing", "Universit� de Paris-Sud");
   static public final Author RMyklebust = new Author("Robert", "Myklebust", NIST);
   static public final Author DBright = new Author("David", "Bright", NIST);
   static public final Author CFiori = new Author("Chuck", "Fiori", NBS);
   static public final Author JArmstrong = new Author("John", "Armstrong", "American University");
   static public final Author JSmall = new Author("John", "Small", NIST);
   static public final Author JGoldstein = new Author("Joseph", "Goldstein", LehighU);
   static public final Author DWilliams = new Author("Dave", "Williams", LehighU);
   static public final Author GBastin = new Author("G", "Bastin", Eindhoven);
   static public final Author HHeijligers = new Author("H", "Heijligers", Eindhoven);
   static public final Author RPackwood = new Author("Rod", "Packwood", "Metals Technology Laboratory");
   static public final Author CLyman = new Author("Charles", "Lyman", LehighU);
   static public final Author ELifshin = new Author("Eric", "Lifshin", "State University of New York at Albany");
   static public final Author PEchlin = new Author("Patrick", "Echlin", "Cambridge Analytical Microscopy, Ltd.");
   static public final Author LSawyer = new Author("Linda", "Sawyer", "Ticona, LLC");
   static public final Author DJoy = new Author("David", "Joy", "University of Tennessee");
   static public final Author JMichael = new Author("Joseph", "Michael", "Sandia National Laboratories");
   static public final Author PDuncumb = new Author("Peter", "Duncumb", "University of Cambridge");
   static public final Author PStatham = new Author("Peter", "Statham", "Oxford Instruments");
   static public final Author SReed = new Author("S. J.", "Reed", "");
   static public final Author JPhilibert = new Author("J.", "Philibert", "");
   static public final Author HYakowitz = new Author("Harvey", "Yakowitz", NBS);
   static public final Author JCriss = new Author("J.", "Criss", "");
   static public final Author LBirks = new Author("L. S.", "Birks", "");
   static public final Author TMcKinley = new Author("T. D.", "McKinley", "");
   static public final Author DWittry = new Author("D. B.", "Wittry", "");
   static public final Author GLove = new Author("G.", "Love", "");
   static public final Author VScott = new Author("V. D.", "Scott", "");
   static public final Author JDijkstra = new Author("J. M.", "Dijkstra", "");
   static public final Author Savitzky = new Author("A", "Savitzky");
   static public final Author Golay = new Author("M. J. E.", "Golay");
   static public final Author RFEdgerton = new Author("R. J.", "Edgerton");
   static public final Author Cullen = new Author("Dermott", "Cullen", "Lawrence Livermore National Laboratory");
   static public final Author CPowell = new Author("Cedric", "Powell", "N.I.S.T.");
   static public final Author FSalvat = new Author("Francesc", "Salvat", "Facultat de Física (ECM), Universitat de Barcelona, Diagonal 647, 08028 Barcelona, Spain");
   static public final Author LSabbatucci = new Author("Sabbatucci", "Lorenzo", "Department of Industrial Engineering (DIN), Laboratory of Montecuccolino, Alma Mater Studiorum University of Bologna, via dei Colli 16, 40136 Bologna, Italy");
   static public final Author AJablonski = new Author("A", "Jablonksi");
   static public final Author BHenke = new Author("B.L.", "Henke");
   static public final Author EGullikson = new Author("E.M.", "Gullikson");
   static public final Author JDavis = new Author("J.C.", "Davis");
   static public final Author CSwytThomas = new Author("C.", "Swyt-Thomas", "N.I.H.");
   static public final Author VanGrieken = new Author("Rene", "Van Grieken", "");
   static public final Author Markowicz = new Author("Andrezej", "Markowicz", "");

   /**
    * A class representing a Journal for use in References.
    */
   static public class Journal {
      private final String mName;
      private final String mPublisher;
      private final String mAbbreviation;

      public Journal(String name, String abbrev, String publisher) {
         mName = name;
         mAbbreviation = abbrev;
         mPublisher = publisher;
      }

      public String getName() {
         return mName;
      }

      public String getPublisher() {
         return mPublisher;
      }
   }

   // Commonly referenced journals
   static public final Journal MandM = new Journal("Microscopy and Microanalysis", "Microsc. Microanal. (USA)", "Cambridge University Press");
   static public final Journal Scanning = new Journal("Scanning", "Scanning (USA)", "Foundation for the Advancement of Medicine & Science");
   static public final Journal Ultramicroscopy = new Journal("Ultramicroscopy", "Ultramicroscopy (Netherlands)", "Elsevier Science");
   static public final Journal SIA = new Journal("Surface and Interface Analysis", "Surf. Interface Anal. (UK)", "John Wiley & Sons Ltd.");
   static public final Journal JPhysD = new Journal("Journal of Physics - D", "J. Phys. D.", "");
   static public final Journal XRaySpec = new Journal("X-Ray Spectrometry", "X-Ray Spectrom.", "John Wiley & Sons Ltd.");
   static public final Journal AnalChem = new Journal("Analytical Chemistry", "Anal Chem", "American Chemical Society");
   static public final Journal JApplPhys = new Journal("Journal of Applied Physics", "J. Appl. Phys", "American Physical Society");
   static public final Journal AtDatNucData = new Journal("Atomic Data and Nuclear Data Tables", "At. Dat. Nucl. Dat. Tables", "Academic Press");
   static public final Journal PhysRevA = new Journal("Physical Review A", "Phys. Rev. A", "American Physical Society");
   static public final Journal ApplPhysLett = new Journal("Applied Physics Letters", "Appl. Phys. Let.", "American Physical Society");
   static public final Journal RadPhys = new Journal("Radiation Physics and Chemistry", "Rad Phys Chem.","");

   /**
    * A type of Reference representing journal articles.
    */
   static public class JournalArticle
      extends LitReference {
      private String mTitle;
      private final Journal mJournal;
      private final String mVolume;
      private final String mPages;
      private final int mYear;
      private final Author[] mAuthors;

      public JournalArticle(String title, Journal journal, String vol, String pages, int year, Author[] authors) {
         super();
         mTitle = title;
         mJournal = journal;
         mVolume = vol;
         mPages = pages;
         mYear = year;
         mAuthors = authors;
      }

      public JournalArticle(Journal journal, String vol, String pages, int year, Author[] authors) {
         mJournal = journal;
         mVolume = vol;
         mPages = pages;
         mYear = year;
         mAuthors = authors;
      }

      @Override
      public String getShortForm() {
         return String.format("%s. %s %s p%s (%d)", new Object[] {
            Author.toAbbrev(mAuthors),
            mJournal.mAbbreviation,
            mVolume,
            mPages,
            Integer.valueOf(mYear)
         });
      }

      @Override
      public String getLongForm() {
         return String.format("%s. %s %s %s p%s (%d)", new Object[] {
            Author.toString(mAuthors),
            mTitle != null ? mTitle : "",
            mJournal.mAbbreviation,
            mVolume,
            mPages,
            Integer.valueOf(mYear)
         });
      }
   }

   static JournalArticle LoveScott1978 = new JournalArticle(LitReference.JPhysD, "11", "p 1369", 1978, new Author[] {
      LitReference.GLove,
      LitReference.VScott
   });

   static JournalArticle Proza96 = new JournalArticle(LitReference.XRaySpec, "27", "p 3-10", 1998, new Author[] {
      LitReference.GBastin,
      LitReference.JDijkstra,
      LitReference.HHeijligers
   });

   static JournalArticle Proza96Extended = new JournalArticle(LitReference.XRaySpec, "30", "p 382-387", 2001, new Author[] {
      LitReference.GBastin,
      new Author("P. J. T. L.", "Oberndorff", ""),
      LitReference.JDijkstra,
      LitReference.HHeijligers
   });

   static final LitReference JTA1982 = new LitReference.BookChapter(LitReference.MicrobeamAnalysis, "175-180", new Author[] {
      LitReference.JArmstrong
   });

   /**
    * A class for representing books.
    */
   static public class Book
      extends LitReference {
      private final String mTitle;
      private final String mPublisher;
      private final int mYear;
      private final Author[] mAuthors;

      public Book(String title, String publisher, int year, Author[] authors) {
         mTitle = title;
         mPublisher = publisher;
         mYear = year;
         mAuthors = authors;
      }

      @Override
      public String getShortForm() {
         return String.format("%s. %s %s (%d)", new Object[] {
            Author.toAbbrev(mAuthors),
            mTitle,
            mPublisher,
            Integer.valueOf(mYear)
         });
      }

      @Override
      public String getLongForm() {
         return String.format("%s. %s %s (%d)", new Object[] {
            Author.toString(mAuthors),
            mTitle,
            mPublisher,
            Integer.valueOf(mYear)
         });
      }
   }

   static public class Program
      extends LitReference {
      private final String mTitle;
      private final String mVersion;
      private final Author[] mAuthors;

      public Program(String title, String version, Author[] authors) {
         mTitle = title;
         mVersion = version;
         mAuthors = authors;
      }

      @Override
      public String getShortForm() {
         return String.format("%s. %s v. %s", new Object[] {
            Author.toAbbrev(mAuthors),
            mTitle,
            mVersion
         });
      }

      @Override
      public String getLongForm() {
         return String.format("%s. %s v. %s", new Object[] {
            Author.toString(mAuthors),
            mTitle,
            mVersion
         });
      }
   }

   static public final Program ENDLIB97_Relax = new LitReference.Program("RELAX", "ENDLIB97", new Author[] {
      Cullen
   });

   static public final Program DTSA = new LitReference.Program("DTSA", "3.0.1", new Author[] {
      CFiori,
      CSwytThomas,
      RMyklebust
   });

   static public final Book ElectronProbeQuant = new Book("Electron Probe Quantitation", "Plenum", 1991, new Author[] {
      KHeinrich,
      DNewbury
   });

   static public final Book Goldstein = new Book("Scanning Electron Microscopy and X-Ray Microanalysis - 3rd edition", "Kluwer Academic/Plenum", 2003, new Author[] {
      JGoldstein,
      CLyman,
      DNewbury,
      ELifshin,
      PEchlin,
      LSawyer,
      DJoy,
      JMichael
   });
   static public final Book QuantitativeElectronProbeMicroanalysis = new Book("Quantitative Electron Probe Microanalysis - NBS SP 298", "National Bureau of Standards", 1968, new Author[] {
      KHeinrich
   });
   static public final Book ElectronBeamXRayMicroanalysis = new Book("Electron Beam X-Ray Microanalysis", "Van Nostrand Reinhold Company", 1981, new Author[] {
      KHeinrich
   });
   static public final Book EnergyDispersiveXRaySpectrometery = new Book("Energy Dispersive X-Ray Spectrometery - NBS SP 604", "National Bureau of Standards", 1981, new Author[] {
      KHeinrich,
      DNewbury,
      RMyklebust,
      CFiori
   });
   static public final Book CharacterizationOfParticles = new Book("Characterization of Particles NBS SP 533", "National Bureau of Standards", 1978, new Author[] {
      KHeinrich
   });

   static public final Book FrameBook = new Book("FRAME: An On-Line Correction Procedure for Quantitative Electron Probe Micronanalysis, NBS Tech Note 796", "National Bureau of Standards", 1973, new Author[] {
      HYakowitz,
      RMyklebust,
      KHeinrich
   });

   static public final Book ElectronMicroprobe = new Book("The Electron Microprobe", "Wiley (New York)", 1966, new Author[] {
      TMcKinley,
      KHeinrich,
      DWittry,
   });

   static public final Book MicrobeamAnalysis = new Book("Microbeam Analysis", "San Francisco Press", 1982, new Author[] {
      KHeinrich
   });

   static public final Book HandbookOfXRaySpectrometry = new Book("Handbook of X-Ray Spectrometry", "Marcel Dekker", 2002, new Author[] {
      VanGrieken,
      Markowicz

   });

   static public LitReference.JournalArticle Henke1993 = new LitReference.JournalArticle("X-ray interactions: photoabsorption, scattering, transmission, and reflection at E=50-30000 eV, Z=1-92", LitReference.AtDatNucData, "54", "181-342", 1993, new LitReference.Author[] {
      LitReference.BHenke,
      LitReference.EGullikson,
      LitReference.JDavis
   });

   /**
    * A class for representing chapters within books.
    */
   static public class BookChapter
      extends LitReference {
      private final Book mBook;
      private String mPages;
      private final Author[] mAuthors;

      public BookChapter(Book book, String pages, Author[] authors) {
         mBook = book;
         mPages = pages;
         mAuthors = authors;
      }

      public BookChapter(Book book, Author[] authors) {
         mBook = book;
         mAuthors = authors;
      }

      public String getPages() {
         return mPages;
      }

      @Override
      public String getShortForm() {
         return String.format("%s in \"%s\" eds. %s %s (%d)", new Object[] {
            Author.toAbbrev(mAuthors),
            mBook.mTitle,
            Author.toAbbrev(mBook.mAuthors),
            mBook.mPublisher,
            Integer.valueOf(mBook.mYear)
         });
      }

      @Override
      public String getLongForm() {
         return String.format("%s in \"%s\" eds. %s %s (%d)", new Object[] {
            Author.toString(mAuthors),
            mBook.mTitle,
            Author.toString(mBook.mAuthors),
            mBook.mPublisher,
            Integer.valueOf(mBook.mYear)
         });
      }
   }

   static public BookChapter PAPinEPQ = new BookChapter(LitReference.ElectronProbeQuant, "p XXX-XXX", new Author[] {
      LitReference.JPouchou,
      LitReference.FPichoir
   });

   static public Date createDate(int yr, int month, int day) {
      final Calendar cal = Calendar.getInstance(Locale.US);
      cal.set(yr, month, 24);
      return cal.getTime();
   }

   /**
    * A reference to a WebSite URL.
    */
   static public class WebSite
      extends LitReference {
      private final String mUrl;
      private final String mTitle;
      private final Date mDate;
      private final Author[] mAuthors;

      public WebSite(String url, String title, Date date, Author[] authors) {
         mUrl = url;
         mTitle = title;
         mDate = date;
         mAuthors = authors;
      }

      @Override
      public String getShortForm() {
         return mUrl;
      }

      @Override
      public String getLongForm() {
         return Author.toString(mAuthors) + ". " + mTitle + "[" + mUrl + " on " + DateFormat.getInstance().format(mDate) + "]";
      }
   }
   /**
    * A reference which is simply a text string.
    */
   static public class CrudeReference
      extends LitReference {
      private final String mReference;

      public CrudeReference(String ref) {
         mReference = ref;
      }

      @Override
      public String getShortForm() {
         return mReference;
      }

      @Override
      public String getLongForm() {
         return mReference;
      }
   }

   static public final LitReference NullReference = new CrudeReference("-");
}

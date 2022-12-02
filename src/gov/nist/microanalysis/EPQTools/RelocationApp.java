package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.PrintUtilities;
import gov.nist.microanalysis.Utility.Translate2D;
import gov.nist.microanalysis.Utility.Translate2D.CalibrationPoint;

public class RelocationApp extends JFrame {

   private static final long serialVersionUID = -8936386448493361187L;
   private JTextPane resultPane;
   private JTextField tag_Field;
   private JTextField x_Field;
   private JTextField y_Field;
   private Translate2D mTranslate = new Translate2D();
   private Color backColor;
   private int mInRelocationTable = 0;
   private int mPointCount = 0;

   private final NumberFormat mParser = NumberFormat.getInstance();

   private static final String POSITION_HEIGHT = "Main window\\height";
   private static final String POSITION_WIDTH = "Main window\\width";
   private static final String POSITION_LEFT = "Main window\\left";
   private static final String POSITION_TOP = "Main window\\top";

   private void appendHTML(String html) {
      try {
         final HTMLDocument doc = (HTMLDocument) resultPane.getDocument();
         // Find the document body and insert just before this...
         javax.swing.text.Element body = null;
         {
            javax.swing.text.Element root = doc.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
               javax.swing.text.Element element = root.getElement(i);
               if (element.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.BODY) {
                  body = element;
                  break;
               }
            }
            assert body != null;
         }
         doc.insertBeforeEnd(body, html);
         resultPane.setCaretPosition(doc.getEndPosition().getOffset() - 1);
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   private void addToRelocatedTable(String html) {
      if (mInRelocationTable == 0) {
         final String header = "<P>" + "<TABLE>"
               + " <CAPTION ALIGN=BOTTOM><B>Table:</B> Relocated points (Computed values are in bold face.)</CAPTION>" + " <TR>" + "  <TH></TH>"
               + "  <TH>X<sub>0</sub></TH>" + "  <TH>Y<sub>0</sub></TH>" + "  <TH>X<sub>1</sub></TH>" + "  <TH>Y<sub>1</sub></TH>" + "</TR>"
               + "</TABLE>" + "</P>";
         appendHTML(header);
      }
      final HTMLEditorKit hek = (HTMLEditorKit) resultPane.getEditorKit();
      final HTMLDocument doc = (HTMLDocument) resultPane.getDocument();
      ++mInRelocationTable;
      try {
         hek.insertHTML(doc, doc.getLength() - 1, "<TR>" + html + "</TR>", 3, 0, HTML.Tag.TR);
         tag_Field.setText("Point " + Integer.toString(++mPointCount));
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   private RelocationApp() {
      super("Relocation Utility");
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
      // Set the size of the window...
      {
         final Preferences userPref = Preferences.userNodeForPackage(RelocationApp.class);
         final Rectangle bounds = getBounds();
         final int top = userPref.getInt(POSITION_TOP, (int) bounds.getX());
         final int left = userPref.getInt(POSITION_LEFT, (int) bounds.getY());
         final int pos_width = userPref.getInt(POSITION_WIDTH, (int) bounds.getWidth());
         final int pos_height = userPref.getInt(POSITION_HEIGHT, (int) bounds.getHeight());
         setBounds(top, left, pos_width, pos_height);
         setPreferredSize(new Dimension(pos_width, pos_height));
      }
   }

   /**
    * Actions to perform when exiting the program...
    */
   private void performAtExit() {
      final Preferences userPref = Preferences.userNodeForPackage(RelocationApp.class);
      final Rectangle bounds = getBounds();
      userPref.putInt(POSITION_WIDTH, (int) bounds.getWidth());
      userPref.putInt(POSITION_HEIGHT, (int) bounds.getHeight());
      userPref.putInt(POSITION_TOP, (int) bounds.getX());
      userPref.putInt(POSITION_LEFT, (int) bounds.getY());
   }

   private double parseField(JTextField field) {
      double res;
      try {
         res = mParser.parse(field.getText()).doubleValue();
         field.setBackground(backColor);
      } catch (final Exception ex) {
         field.setBackground(Color.pink);
         field.requestFocus();
         res = Double.NaN;
      }
      return res;
   }

   private String baseDoc() {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      pw.println("<html>");
      pw.println(" <head>");
      pw.println("  <title>Relocation Utility Report</title>");
      pw.println("  <style type=\"text/css\">");
      pw.println("body {");
      pw.println("   background-color: #FFFFFF;");
      pw.println("   font-family: Tahoma, Geneva, sans-serif;");
      pw.println("   font-size: small;");
      pw.println("   color:#000000");
      pw.println("}");
      pw.println("p {");
      pw.println("   background-color: #FFFFFF;");
      pw.println("   margin-top: 0pt;");
      pw.println("   margin-bottom: 0pt;");
      pw.println("}");

      pw.println("h1 {");
      pw.println("   font-size: 1.3em;");
      pw.println("   font-weight: 200;");
      pw.println("   font-variant: small-caps;");
      pw.println("   background-color: #FFFFFF;");
      pw.println("   color:#000000");
      pw.println("   margin-top: 0pt;");
      pw.println("   margin: 6pt 0 6pt 0;");
      pw.println("}");

      pw.println("h2 {");
      pw.println("   font-size: 1.1em;");
      pw.println("   font-weight: 200;");
      pw.println("   font-variant: small-caps;");
      pw.println("   background-color: #FFFFFF;");
      pw.println("   color:#000000");
      pw.println("   margin-top: 0pt;");
      pw.println("   margin: 6pt 0 6pt 0;");
      pw.println("}");

      pw.println("table {");
      pw.println("   table-layout: fixed;");
      pw.println("   border-style: solid;");
      pw.println("   border-color: gray;");
      pw.println("   background-color: white;");
      pw.println("   position: relative;");
      pw.println("   left: 0em;");
      pw.println("}");

      pw.println("tr {");
      pw.println("   border-style: none;");
      pw.println("   border-padding: 3px;");
      pw.println("}");

      pw.println("th {");
      pw.println("   border-width: 0px;");
      pw.println("   width: 20em;");
      pw.println("   padding: 5px;");
      pw.println("   border-style: inset;");
      pw.println("   border-color: gray;");
      pw.println("   background-color: white;");
      pw.println("}");

      pw.println("td {");
      pw.println("   border-width: 0px;");
      pw.println("   width: 20em;");
      pw.println("   padding: 5px;");
      pw.println("   border-style: inset;");
      pw.println("   border-color: gray;");
      pw.println("   background-color: white;");
      pw.println("}");
      pw.println("   </style>");

      pw.println(" </head>");
      pw.println(" <body>");
      pw.println("   <h1>Relocation Utility</h1>");
      pw.println("   <h2>Version 1.1</h2>");
      pw.println(" </body>");
      pw.println("</html>");
      return sw.toString();
   }

   private void setTranslate(final Translate2D res, final ArrayList<CalibrationPoint> calibrationPoints) {
      mTranslate = res;
      final NumberFormat nf = new HalfUpFormat("0.####");
      final NumberFormat nf2 = new HalfUpFormat("0.#");
      final StringBuffer html = new StringBuffer();
      html.append("<P>");
      html.append(" <TABLE>");
      html.append("  <TR><TH>Parameter</TH><TH>Value</TH></TR>");
      html.append("  <TR><TH>X<sub>offset</sub></TH><TD>");
      html.append(nf.format(res.getXOffset()));
      html.append("  </TD></TR>");
      html.append("  <TR><TH>Y<sub>offset</sub></TH><TD>");
      html.append(nf.format(res.getYOffset()));
      html.append("  </TD>");
      html.append("  <TR><TH>X<sub>scale</sub></TH><TD>");
      html.append(nf.format(res.getXScale()));
      html.append("  </TD></TR>");
      html.append("  <TR><TH>Y<sub>scale</sub></TH><TD>");
      html.append(nf.format(res.getYScale()));
      html.append("  </TD></TR>");
      html.append("  <TR><TH>Rotation</TH><TD>");
      html.append(nf2.format(Math.toDegrees(res.getRotation())));
      html.append("&deg;</TD></TR>");
      if (calibrationPoints != null) {
         html.append("  <TR><TH>Error</TH><TD>");
         html.append(nf.format(res.error(calibrationPoints)));
         html.append("  </TD></TR>");
      }
      html.append(" </TABLE>");
      html.append("</P>");
      html.append("<P><B>Table:</B> Transformation definition</P>");
      appendHTML(html.toString());
      mInRelocationTable = 0;
   }

   private void initialize() {
      final JMenuBar mb = new JMenuBar();
      final JMenu fileMenu = new JMenu("File");
      fileMenu.setMnemonic(KeyEvent.VK_F);

      final JMenuItem saveAs = new JMenuItem("Save Report As");
      saveAs.setMnemonic(KeyEvent.VK_A);
      saveAs.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            final String kSaveDir = "HTMLSave";
            final Preferences userPref = Preferences.userNodeForPackage(RelocationApp.class);
            final JFileChooser jfc = new JFileChooser(userPref.get(kSaveDir, System.getProperty("user.home")));
            jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"html",}, "HTML File"));
            final int option = jfc.showSaveDialog(RelocationApp.this);
            if (option == JFileChooser.APPROVE_OPTION) {
               final File res = jfc.getSelectedFile();
               try {
                  try (final FileOutputStream fos = new FileOutputStream(res)) {
                     final HTMLEditorKit hek = (HTMLEditorKit) resultPane.getEditorKit();
                     final HTMLDocument doc = (HTMLDocument) resultPane.getDocument();
                     hek.write(fos, doc, 0, doc.getLength());
                  }
               } catch (final Exception e1) {
                  ErrorDialog.createErrorMessage(RelocationApp.this, "Error saving HTML", e1);
               }
               userPref.put(kSaveDir, res.getParent());
            }
         }
      });
      fileMenu.add(saveAs);

      final JMenuItem saveTranslationAs = new JMenuItem("Save Translation As");
      saveTranslationAs.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final String kSaveDir = "HTMLSave";
            final Preferences userPref = Preferences.userNodeForPackage(RelocationApp.class);
            final JFileChooser jfc = new JFileChooser(userPref.get(kSaveDir, System.getProperty("user.home")));
            jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"xrel",}, "Relocation File"));
            final int option = jfc.showSaveDialog(RelocationApp.this);
            if (option == JFileChooser.APPROVE_OPTION) {
               final File res = jfc.getSelectedFile();
               try {
                  try (final FileOutputStream fos = new FileOutputStream(res)) {
                     EPQXStream xs = EPQXStream.getInstance();
                     xs.toXML(mTranslate, fos);
                  }
               } catch (final Exception e1) {
                  ErrorDialog.createErrorMessage(RelocationApp.this, "Error saving HTML", e1);
               }
               userPref.put(kSaveDir, res.getParent());
            }
         }
      });
      fileMenu.add(saveTranslationAs);

      final JMenuItem openTranslation = new JMenuItem("Open Translation");
      openTranslation.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final String kSaveDir = "HTMLSave";
            final Preferences userPref = Preferences.userNodeForPackage(RelocationApp.class);
            final JFileChooser jfc = new JFileChooser(userPref.get(kSaveDir, System.getProperty("user.home")));
            jfc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"xrel",}, "Relocation File"));
            final int option = jfc.showOpenDialog(RelocationApp.this);
            if (option == JFileChooser.APPROVE_OPTION) {
               final File res = jfc.getSelectedFile();
               try {
                  try (final FileInputStream fis = new FileInputStream(res)) {
                     EPQXStream xs = EPQXStream.getInstance();
                     Object obj = xs.fromXML(fis);
                     if (obj instanceof Translate2D)
                        setTranslate((Translate2D) obj, (ArrayList<CalibrationPoint>) null);
                  }
               } catch (final Exception e1) {
                  ErrorDialog.createErrorMessage(RelocationApp.this, "Error saving HTML", e1);
               }
               userPref.put(kSaveDir, res.getParent());
            }
         }
      });
      fileMenu.add(openTranslation);

      final JMenuItem importCsv = new JMenuItem("Import CSV");
      importCsv.setMnemonic(KeyEvent.VK_I);
      importCsv.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            importCSV();
         }

      });
      fileMenu.add(importCsv);

      final JMenuItem print = new JMenuItem("Print");
      print.setMnemonic(KeyEvent.VK_P);
      print.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            PrintUtilities.printComponent(resultPane);
         }
      });

      fileMenu.add(print);

      fileMenu.addSeparator();

      final JMenuItem exit = new JMenuItem("Exit");
      exit.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            performAtExit();
            System.exit(0);
         }
      });
      exit.setMnemonic(KeyEvent.VK_X);
      fileMenu.add(exit);

      mb.add(fileMenu);

      final JMenu configMenu = new JMenu("Edit");
      configMenu.setMnemonic(KeyEvent.VK_E);
      final JMenuItem rel = new JMenuItem("Configure relocation");
      rel.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final RelocationDialog dlg = new RelocationDialog(RelocationApp.this, true);
            dlg.setLocationRelativeTo(RelocationApp.this);
            dlg.setVisible(true);
            final Translate2D res = dlg.getResult();
            if (res != null)
               setTranslate(res, dlg.getCalibrationPoints());
         }

      });
      rel.setMnemonic(KeyEvent.VK_C);
      configMenu.add(rel);

      mb.add(configMenu);

      final JMenu helpMenu = new JMenu("Help");
      helpMenu.setMnemonic(KeyEvent.VK_H);
      final JMenuItem about = new JMenuItem("About");
      about.setMnemonic(KeyEvent.VK_A);
      about.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final StringBuffer html = new StringBuffer();
            html.append("<h1>About the <i>Relocation Utility...</i></h1>");

            html.append("<p align = left>A utility for relocating points on a 2D plane when the underlying coordinate system has "
                  + "been translated, rotated or the axes scaled. This utility can be used to relocated points when a sample has been "
                  + "removed and replaced within an instrument; or when points are recorded on one instrument and the sample is moved "
                  + "to another instrument.</p>");

            html.append("<p align = left>First, define a coordinate transform by relocating points from "
                  + "one coordinate system in a second coordinate system.  The original and relocated points are recorded in the "
                  + "<i>configure relocation</i> dialog (<u>E</u>dit><u>C</u>onfigure Relocation). "
                  + "One point can be used to define a simple translation, two points a "
                  + "a translation plus rotation and uniform scaling on both the x and y axes.  Three or more points define a transformation "
                  + "including translation, rotation and different scaling on the x and y axes.</p>");

            html.append("<p align = left>Once the transformation has been defined, it can be applied to new points by entering a name and "
                  + "x and y coordinates for the point in the edit boxes at the bottom of the main window.  Select the <i>Transform</i> button to "
                  + "transform from the original coordinate system to the new one; or <i>Inverse</i> to perform the transformation from the new "
                  + "coordinate system back to the old one.</p>");

            html.append("<p align=left>Sets of points may be import in batch from comma separated value text files.  The "
                  + "CSV file may have 2 or 3 columns.  The optional first column is a point label. The next column is the x-"
                  + "coordinate and the last column is the y-coordinate.</p>");

            html.append("<p align = left>The Relocation Utility is part of the <i>Electron Probe Quantification</i> library. "
                  + "The Electron Probe Quantification library is a collection of analytical and Monte "
                  + "Carlo routines for interpreting electron probe microanalytical data.  The Electron "
                  + "Probe Quantification library is been developed by Nicholas W. M. Ritchie (nicholas.ritchie@nist.gov) "
                  + "with help from Daniel Davis.  This utility has been extracted as a stand alone application because of its "
                  + "general utility.</p>");

            html.append("<p><table align=center>");
            html.append("<tr><th align=center><b>National Institute of Standards and Technology</b></th></tr>");
            html.append("<tr><td align=center>Materials Measurement Science Division</td></tr>");
            html.append("<tr><td align=center>100 Bureau Drive</td></tr>");
            html.append("<tr><td align=center>Gaithersburg, MD 20899</td></tr>");
            html.append("<tr><td align=center><i>nicholas.ritchie@nist.gov</i></td></tr>");
            html.append("</table></p>");

            html.append(
                  "<p>Pursuant to title 17 Section 105 of the United States Code, this software is not subject to copyright protection and is in the public domain.</p>");
            appendHTML(html.toString());
            mInRelocationTable = 0;
         }
      });
      helpMenu.add(about);
      mb.add(helpMenu);

      setJMenuBar(mb);

      resultPane = new JTextPane();
      resultPane.setContentType("text/html");
      resultPane.setText(baseDoc());
      resultPane.setEditable(false);
      resultPane.setBackground(Color.white);

      add(new JScrollPane(resultPane));

      {
         final FormLayout fl = new FormLayout("10dlu, pref, 4dlu, 45dlu, 10dlu, pref, 4dlu, 45dlu, 10dlu, pref, 4dlu, 45dlu, 4dlu, pref, 4dlu, pref",
               "pref");
         final PanelBuilder pb = new PanelBuilder(fl);
         final CellConstraints cc = new CellConstraints();
         final FocusListener fl1 = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
               final JTextField tf = (JTextField) e.getComponent();
               tf.selectAll();
            }
         };
         pb.addLabel("<HTML>Tag", cc.xy(2, 1));
         tag_Field = new JTextField();
         tag_Field.addFocusListener(fl1);
         pb.add(tag_Field, cc.xy(4, 1));
         tag_Field.setText("Point " + Integer.toString(++mPointCount));
         pb.addLabel("<HTML>X", cc.xy(6, 1));
         x_Field = new JTextField();
         x_Field.addFocusListener(fl1);
         backColor = x_Field.getBackground();
         pb.add(x_Field, cc.xy(8, 1));
         pb.addLabel("<HTML>Y", cc.xy(10, 1));
         y_Field = new JTextField();
         y_Field.addFocusListener(fl1);
         pb.add(y_Field, cc.xy(12, 1));
         final JButton transform = new JButton("Translate");
         transform.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               final double x0 = parseField(x_Field);
               final double y0 = parseField(y_Field);
               if (!(Double.isNaN(x0) || Double.isNaN(y0))) {
                  final double[] res = mTranslate.compute(new double[]{x0, y0});
                  final NumberFormat tf = new HalfUpFormat("0.####");
                  final String html = "<TH>" + tag_Field.getText() + "</TH><TD>" + x_Field.getText() + "</TD><TD>" + y_Field.getText()
                        + "</TD><TD><B>" + tf.format(res[0]) + "</B></TD><TD><B>" + tf.format(res[1]) + "</B></TD>";
                  addToRelocatedTable(html);
               }
               x_Field.requestFocus();
            }
         });
         pb.add(transform, cc.xy(14, 1));
         final JButton inverse = new JButton("Inverse");
         inverse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               final double x0 = parseField(x_Field);
               final double y0 = parseField(y_Field);
               if (!(Double.isNaN(x0) || Double.isNaN(y0))) {
                  final double[] res = mTranslate.inverse(new double[]{x0, y0});
                  final NumberFormat tf = new HalfUpFormat("0.####");
                  final String html = "<TH>" + tag_Field.getText() + "</TH><TD><B>" + tf.format(res[0]) + "</B></TD><TD><B>" + tf.format(res[1])
                        + "</B></TD><TD>" + x_Field.getText() + "</TD><TD>" + y_Field.getText() + "</TD>";
                  addToRelocatedTable(html);
               }
               x_Field.requestFocus();
            }
         });
         pb.add(inverse, cc.xy(16, 1));
         add(pb.getPanel(), BorderLayout.SOUTH);
      }
      setPreferredSize(new Dimension(900, 600));
   }

   // Overridden so we can exit when window is closed
   @Override
   protected void processWindowEvent(WindowEvent e) {
      super.processWindowEvent(e);
      if (e.getID() == WindowEvent.WINDOW_CLOSING) {
         performAtExit();
         System.exit(0);
      }
   }

   private void importCSV() {
      final JFileChooser fc = new JFileChooser() {
         private static final long serialVersionUID = -8441838196501709700L;

         @Override
         public void updateUI() {
            putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
            super.updateUI();
         }
      };

      fc.setAcceptAllFileFilterUsed(true);
      fc.addChoosableFileFilter(new SimpleFileFilter(new String[]{"csv", "txt"}, "Comma Separated Values"));
      fc.setMultiSelectionEnabled(true);
      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
         for (final File f : fc.getSelectedFiles())
            try {
               processCSVFile(f);
            } catch (final Exception e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
   }

   private String[] parseCSV(String str) {
      final ArrayList<String> strs = new ArrayList<String>();
      final StringBuffer sb = new StringBuffer(str.length());
      for (int i = 0; i < str.length(); ++i) {
         char c = str.charAt(i);
         if (c == ',') {
            strs.add(sb.toString().trim());
            sb.setLength(0);
            continue;
         }
         if (c == '\"') {
            for (++i; i < str.length(); ++i) {
               c = str.charAt(i);
               if (c == '\"')
                  break;
               else
                  sb.append(c);
            }
            continue;
         } else
            sb.append(c);
      }
      if (sb.length() > 0)
         strs.add(sb.toString().trim());
      return strs.toArray(new String[strs.size()]);
   }

   private void processCSVFile(File f) throws Exception {
      final FileReader fr = new FileReader(f);
      try (final BufferedReader br = new BufferedReader(fr)) {
         int cx = 0;
         final NumberFormat tf = new HalfUpFormat("0.####");
         mInRelocationTable = 0;
         appendHTML("<h2>Transforming points from a CSV file</h2>");
         appendHTML("<p><b>CSV file:</b> " + f.getCanonicalPath() + "</p>");
         while (br.ready()) {
            final String[] items = parseCSV(br.readLine());
            if (items.length >= 2) {
               int i = 0;
               String name = "Point " + Integer.toString(++cx);
               if (items.length == 3)
                  name = items[i++];
               final double x = Double.parseDouble(items[i]);
               final double y = Double.parseDouble(items[i + 1]);
               final double[] res = mTranslate.compute(new double[]{x, y});
               final String html = "<TH>" + name + "</TH><TD>" + items[i] + "</TD><TD>" + items[i + 1] + "</TD><TD><B>" + tf.format(res[0])
                     + "</B></TD><TD><B>" + tf.format(res[1]) + "</B></TD>";
               addToRelocatedTable(html);
            }
         }
      }
   }

   /**
    * Main method for the RelocationApp class.
    * 
    * @param args
    */
   public static void main(String[] args) {
      // Set these regardless (no harm in it)
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.laf.smallTabs", "true");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Relocation Utility");
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (final Exception e) {
         e.printStackTrace();
      }
      final JFrame frame = new RelocationApp();
      frame.pack();
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      frame.setVisible(true);
   }
}

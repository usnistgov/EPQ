package gov.nist.microanalysis.EPQDatabase;

import gov.nist.microanalysis.EPQDatabase.Session.SpectrumSummary;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQTools.SpecDisplay;
import gov.nist.microanalysis.Utility.SpectrumPropertiesTableModel;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * A dialog to permit selection of spectra from a list of spectra.
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
public class ResultDialog extends JDialog {
   private static final long serialVersionUID = 5066921759652675420L;
   private SpecDisplay jSpecDisplay_Main;
   private JTable jTable_Spectra;
   private JTable jTable_Properties;
   private JButton jButton_Ok;
   private JButton jButton_Cancel;

   private boolean mResult = false;
   private ArrayList<Session.SpectrumSummary> mSpectra;

   private void initialize() {
      final FormLayout fl = new FormLayout("200dlu, 5dlu, fill:max(250dlu;pref):grow(1.0)",
            "pref, 5dlu, 150dlu, 5dlu, pref, 5dlu, fill:max(100dlu;pref):grow(1.0), 25dlu");
      final PanelBuilder panel = new PanelBuilder(fl);
      final CellConstraints cc = new CellConstraints();
      panel.addSeparator("Spectra", cc.xy(1, 1));
      panel.addSeparator("Spectrum Properties", cc.xy(3, 1));
      jTable_Spectra = new JTable();
      jTable_Spectra.setForeground(SystemColor.textText);
      panel.add(new JScrollPane(jTable_Spectra), cc.xy(1, 3));
      final DefaultListSelectionModel lsm = new DefaultListSelectionModel();
      lsm.addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            jSpecDisplay_Main.clearAllSpectra();
            final Collection<ISpectrumData> specs = getSpectra();
            for (final ISpectrumData spec : specs)
               jSpecDisplay_Main.addSpectrum(spec);
            jTable_Properties.setModel(new SpectrumPropertiesTableModel(specs));
            jSpecDisplay_Main.autoScaleV(100.0);
         }
      });
      jTable_Spectra.setSelectionModel(lsm);

      jTable_Properties = new JTable();
      jTable_Properties.setForeground(SystemColor.textText);
      panel.add(new JScrollPane(jTable_Properties), cc.xy(3, 3));
      panel.addSeparator("Selected spectra", cc.xyw(1, 5, 3));
      jSpecDisplay_Main = new SpecDisplay();
      panel.add(jSpecDisplay_Main, cc.xyw(1, 7, 3));
      final JPanel pp = panel.getPanel();
      pp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      add(pp, BorderLayout.CENTER);

      jButton_Ok = new JButton("Ok");
      jButton_Ok.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mResult = true;
            setVisible(false);
         }
      });
      jButton_Cancel = new JButton("Cancel");
      jButton_Cancel.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mResult = false;
            setVisible(false);
         }
      });
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Ok, jButton_Cancel);
      final JPanel btns = bbb.build();
      btns.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      add(btns, BorderLayout.SOUTH);

      pack();
   }

   /**
    * Constructs a ResultDialog for allowing the user to select among search
    * result spectra.
    * 
    * @param owner
    * @param title
    * @param modal
    */
   public ResultDialog(Frame owner, String title, boolean modal) {
      super(owner, title, modal);
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public void setSingleSelect(boolean b) {
      jTable_Spectra.setSelectionMode(b ? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
   }

   /**
    * Constructs a ResultDialog for allowing the user to select among search
    * result spectra.
    * 
    * @param owner
    * @param title
    * @param modal
    */
   public ResultDialog(Dialog owner, String title, boolean modal) {
      super(owner, title, modal);
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   class SpectrumTableModel extends AbstractTableModel {

      private static final long serialVersionUID = -8877874828275180489L;

      private final String[] COLUMN_NAMES = new String[]{"Acquired", "Description"};

      SpectrumTableModel(Collection<Session.SpectrumSummary> specs) {
         mSpectra = new ArrayList<Session.SpectrumSummary>(specs);
      }

      /**
       * @see javax.swing.table.TableModel#getColumnCount()
       */
      @Override
      public int getColumnCount() {
         return 2;
      }

      /**
       * @see javax.swing.table.TableModel#getColumnName(int)
       */
      @Override
      public String getColumnName(int columnIndex) {
         return COLUMN_NAMES[columnIndex];
      }

      /**
       * @see javax.swing.table.TableModel#getRowCount()
       */
      @Override
      public int getRowCount() {
         return mSpectra.size();
      }

      /**
       * @see javax.swing.table.TableModel#getValueAt(int, int)
       */
      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         switch (columnIndex) {
            case 0 :
               return DateFormat.getDateInstance().format(mSpectra.get(rowIndex).getTimestamp());
            default :
               return mSpectra.get(rowIndex).toString();

         }
      }
   };

   public void setSpectra(Collection<SpectrumSummary> specs) {
      final SpectrumTableModel stm = new SpectrumTableModel(specs);
      jTable_Spectra.setModel(stm);
   }

   public boolean showDialog() {
      mResult = false;
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      setVisible(true);
      return (getSpectra().size() > 0) && mResult;
   }

   public ArrayList<ISpectrumData> getSpectra() {
      final int[] rows = jTable_Spectra.getSelectedRows();
      final ArrayList<ISpectrumData> specs = new ArrayList<ISpectrumData>();
      for (final int r : rows)
         try {
            if (jTable_Spectra.isRowSelected(r))
               specs.add(mSpectra.get(r).load());
         } catch (final Exception e1) {
            e1.printStackTrace();
         }
      return specs;
   }

   public static void main(String[] args) {
      try {
         // Set these regardless (no harm in it)
         System.setProperty("apple.laf.useScreenMenuBar", "true");
         System.setProperty("apple.laf.smallTabs", "true");
         System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Trixy");
         // Set up special look-and-feels
         final String laf = UIManager.getSystemLookAndFeelClassName();
         UIManager.setLookAndFeel(laf);
      } catch (final Exception e) {
         try {
            e.printStackTrace();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (final Exception e1) {
            e1.printStackTrace();
         }
      }
      JFrame.setDefaultLookAndFeelDecorated(false);
      final ResultDialog rs = new ResultDialog((Frame) null, "Select one or more spectra", true);
      final Session ses = new Session("C:\\Documents and Settings\\nritchie\\My Documents\\Trixy Reports\\Database");
      try {
         final Random r = new Random();
         final TreeSet<Session.SpectrumSummary> specs = ses.readSpectra(
               new int[]{r.nextInt(100), r.nextInt(100), r.nextInt(100), r.nextInt(100), r.nextInt(100), r.nextInt(100), r.nextInt(100)});
         ses.initiateLoad(specs);
         rs.setSpectra(specs);
         rs.setSingleSelect(false);
         if (rs.showDialog())
            for (final ISpectrumData spec : rs.getSpectra())
               System.out.println(spec.toString());
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

}

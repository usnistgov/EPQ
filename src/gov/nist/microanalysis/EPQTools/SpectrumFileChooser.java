/**
 * <p>
 * Title: gov.nist.microanalysis.EPQTools.SpectrumFileChooser.java
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
package gov.nist.microanalysis.EPQTools;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQTools.SpecDisplay.AXIS_MODE;

/**
 * A JFileChooser-like dialog for previewing and opening EDS spectra files.
 */
public class SpectrumFileChooser
   extends JDialog {

   static private final long serialVersionUID = 0xdddd653892L;
   private JFileChooser mFileChooser;
   private SpecDisplay mSpecDisplay;
   private int mResult = JFileChooser.ERROR_OPTION;
   private final JComboBox<ISpectrumData> mSelectSpectrum = new JComboBox<ISpectrumData>();
   private Map<File, ISpectrumData[]> mSelected = new TreeMap<File, ISpectrumData[]>();
   private JPopupMenu mSpecDisplay_Menu;

   private String mErrors = null;

   private Collection<KLMLine> getDefaultKLMs(Element elm) {
      final TreeSet<KLMLine> res = new TreeSet<KLMLine>();
      int last = XRayTransition.MZ2 + 1;
      if(elm.getAtomicNumber() < Element.elmB)
         last = XRayTransition.KA1;
      else if(elm.getAtomicNumber() < Element.elmCa)
         last = XRayTransition.L3N2;
      else if(elm.getAtomicNumber() < Element.elmBa)
         last = XRayTransition.M1N2;
      for(int tr = XRayTransition.KA1; tr < last; ++tr) {
         final XRayTransition xrt = new XRayTransition(elm, tr);
         try {
            if((xrt.getWeight(XRayTransition.NormalizeKLM) >= 0.01) && (xrt.getEnergy() > 0.0))
               res.add(new KLMLine.Transition(xrt));
         }
         catch(final EPQException e) {
            // Just ignore it...
         }
      }
      return res;
   }

   private void initialize() {
      {
         final JPanel contents = new JPanel(new FormLayout("6dlu, max(pref;450dlu), 6dlu", "6dlu, pref, 3dlu, pref, 0dlu, fill:150dlu, 6dlu, pref, 6dlu"));
         mFileChooser = new JFileChooser() {
            private static final long serialVersionUID = -8441838196501709700L;

            @Override
            public void updateUI() {
               final String ver = System.getProperty("java.version");
               boolean use = false;
               if(ver != null) {
                  int st = 0;
                  int end = ver.indexOf('.', st);
                  if((end > st) && (Integer.parseInt(ver.substring(st, end)) >= 1)) {
                     st = end + 1;
                     end = ver.indexOf('.', st);
                     if((end > st) && (Integer.parseInt(ver.substring(st, end)) >= 6)) {
                        st = end + 1;
                        end = ver.indexOf('_', st);
                        if((end > st) && (Integer.parseInt(ver.substring(st, end)) >= 0)) {
                           st = end + 1;
                           end = ver.indexOf('-', st);
                           if(end == -1)
                              end = ver.length();
                           use = (Integer.parseInt(ver.substring(st, end)) >= 4);
                        }
                     }
                  }
               }
               putClientProperty("FileChooser.useShellFolder", use ? Boolean.TRUE : Boolean.FALSE);
               super.updateUI();
            }
         };
         mFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
         mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
         mFileChooser.setBorder(null);

         final CellConstraints cc = new CellConstraints();
         contents.add(mFileChooser, cc.xy(2, 2));

         mSelectSpectrum.addActionListener(new AbstractAction() {
            static private final long serialVersionUID = 0x32131daef1231L;

            @Override
            public void actionPerformed(ActionEvent ae) {
               mSpecDisplay.clearAllSpectra();
               final ISpectrumData spec = (ISpectrumData) mSelectSpectrum.getSelectedItem();
               if(spec != null)
                  mSpecDisplay.addSpectrum(spec);
               mSpecDisplay.zoomToAll();
            }
         });
         mSelectSpectrum.setEnabled(false);
         {
            final JPanel ssp = new JPanel(new FormLayout("pref:grow, 90dlu", "pref"));
            ssp.add(mSelectSpectrum, cc.xy(2, 1));
            contents.add(ssp, cc.xy(2, 4));
         }

         mSpecDisplay = new SpecDisplay();
         mSpecDisplay.setAxisScalingMode(AXIS_MODE.LINEAR);
         mSpecDisplay.setDragEnabled(false);
         mSpecDisplay_Menu = mSpecDisplay.getSimpleMenu();
         mSpecDisplay.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
               if(e.isPopupTrigger())
                  mSpecDisplay_Menu.show(mSpecDisplay, e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
               if(e.isPopupTrigger())
                  mSpecDisplay_Menu.show(mSpecDisplay, e.getX(), e.getY());
            }
         });

         contents.add(mSpecDisplay, cc.xy(2, 6));

         {
            final JButton ok = new JButton("Open");
            ok.addActionListener(new AbstractAction() {
               static private final long serialVersionUID = 0xFFEEDDCC86612L;

               @Override
               public void actionPerformed(ActionEvent ae) {
                  mResult = JFileChooser.APPROVE_OPTION;
                  if(mErrors != null)
                     ErrorDialog.createErrorMessage(SpectrumFileChooser.this, "Errors", "One or more errors occured while opening the selected files.", mErrors);
                  mFileChooser.approveSelection();
                  setVisible(false);
               }

            });
            getRootPane().setDefaultButton(ok);

            final JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new AbstractAction() {
               static private final long serialVersionUID = 0xFEEDCC86612L;

               @Override
               public void actionPerformed(ActionEvent ae) {
                  mResult = JFileChooser.CANCEL_OPTION;
                  mFileChooser.cancelSelection();
                  setVisible(false);
               }

            });
            final ButtonBarBuilder bbb = new ButtonBarBuilder();
            bbb.addGlue();
            bbb.addButton(ok, cancel);
            contents.add(bbb.build(), cc.xy(2, 8));
         }

         mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
         mFileChooser.setMultiSelectionEnabled(true);
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "ser",
            "msa",
            "emsa",
            "dat",
            "sp0",
            "mca",
            "spd",
            "spx"
         }, "Spectrum file"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "ser",
            "msa"
         }, "EMISPEC file"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "emsa",
            "msa",
            "ems"
         }, "MSA standard file"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "psmsa",
            "lsmsa"
         }, "Noran MSA files"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "dat"
         }, "DTSA file"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "tif"
         }, "TIFF-style spectrum file"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "spc"
         }, "EDAX SPC spectrum file"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "zstd"
         }, "DTSA-II standard bundle"));
         mFileChooser.addChoosableFileFilter(new SimpleFileFilter(new String[] {
            "ser",
            "msa",
            "emsa",
            "dat",
            "tif",
            "sp0",
            "mca",
            "spd",
            "spc",
            "psmsa",
            "lsmsa",
            "spx",
            "zstd"
         }, "Common spectrum file"));
         mFileChooser.setAcceptAllFileFilterUsed(true);
         mFileChooser.setControlButtonsAreShown(false);
         mFileChooser.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent changeEvent) {
               final String changeName = changeEvent.getPropertyName();
               if(mFileChooser.isMultiSelectionEnabled()) {
                  if(changeName.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
                     final File[] files = mFileChooser.getSelectedFiles();
                     if(mSelected == null)
                        mSelected = new TreeMap<File, ISpectrumData[]>();
                     if((files != null) && (files.length > 0)) {
                        File newFile = null;
                        ISpectrumData[] newSpecs = null;
                        // Figure out which file was just added.
                        final Map<File, ISpectrumData[]> selections = new TreeMap<File, ISpectrumData[]>();
                        StringBuffer errors = null;
                        for(final File file : files) {
                           final ISpectrumData[] specs = mSelected.get(file);
                           if(specs == null)
                              try {
                                 newSpecs = SpectrumFile.open(file);
                                 if(newSpecs != null) {
                                    newFile = file;
                                    selections.put(newFile, newSpecs);
                                 }
                              }
                              catch(final EPQException ex) {
                                 if(errors == null)
                                    errors = new StringBuffer();
                                 else
                                    errors.append("\n");
                                 errors.append(ex.getMessage());
                                 newSpecs = null;
                                 newFile = null;
                              }
                           else
                              selections.put(file, specs);
                        }
                        mErrors = errors != null ? errors.toString() : null;
                        mSelected = selections;
                        mSpecDisplay.clearAllSpectra();
                        mSpecDisplay.clearKLMs();
                        if(!mSelected.isEmpty()) {
                           final StringBuffer sb = new StringBuffer();
                           for(final ISpectrumData[] specs : mSelected.values())
                              for(final ISpectrumData spec : specs)
                                 displaySpectrum(sb, spec);
                           mSpecDisplay.setTextAnnotation(sb.toString());
                        }
                        mSpecDisplay.zoomToAll();
                     }
                  }
               } else if(changeName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                  mSpecDisplay.clearAllSpectra();
                  mSpecDisplay.clearKLMs();
                  mSelected = new TreeMap<File, ISpectrumData[]>();
                  final File file = mFileChooser.getSelectedFile();
                  try {
                     final ISpectrumData[] sd = SpectrumFile.open(file);
                     if((sd != null) && (sd.length > 0)) {
                        mSelected.put(file, sd);
                        mSelectSpectrum.removeAllItems();
                        for(final ISpectrumData element : sd)
                           mSelectSpectrum.addItem(element);
                        mSelectSpectrum.setSelectedIndex(0);
                        final StringBuffer sb = new StringBuffer();
                        displaySpectrum(sb, (ISpectrumData) mSelectSpectrum.getSelectedItem());
                        mSpecDisplay.setTextAnnotation(sb.toString());
                     }
                  }
                  catch(final EPQException ex) {
                     // Ignore it..
                  }
                  mSpecDisplay.zoomToAll();
               }
            }

            private void displaySpectrum(StringBuffer sb, final ISpectrumData spec) {
               mSpecDisplay.addSpectrum(spec);
               final Composition comp = SpectrumUtils.getComposition(spec);
               if(comp != null) {
                  sb.append(spec + "\t" + comp + "\n");
                  for(final Element elm : comp.getElementSet())
                     if(comp.weightFraction(elm, true) > 0.01)
                        mSpecDisplay.addKLMs(getDefaultKLMs(elm));
               }
            }
         });
         getContentPane().add(contents);
         addKeyMaps();
      }
      setResizable(false);
   }

   private void addKeyMaps() {
      {
         final String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";
         final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
         mFileChooser.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escapeKey, CANCEL_ACTION_KEY);
         getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escapeKey, CANCEL_ACTION_KEY);
         final AbstractAction cancelAction = new AbstractAction() {
            private static final long serialVersionUID = 585668420885106696L;

            @Override
            public void actionPerformed(ActionEvent e) {
               mResult = JFileChooser.CANCEL_OPTION;
               setVisible(false);
            }
         };
         getRootPane().getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
         mFileChooser.getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
      }
      {
         final String ENTER_ACTION_KEY = "ENTER_ACTION_KEY";
         final KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
         final Object prevAction = mFileChooser.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(enterKey);
         mFileChooser.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enterKey, ENTER_ACTION_KEY);
         getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enterKey, ENTER_ACTION_KEY);
         class EnterAction
            extends AbstractAction {

            private final Object mOldAction;

            EnterAction(Object oldAction) {
               mOldAction = oldAction;
            }

            private static final long serialVersionUID = 585668420885106696L;

            @Override
            public void actionPerformed(ActionEvent e) {
               /*
                * There is a small problem with the logic here. Once one file
                * has been selected even if it isn't displayed in the text box,
                * it will still be listed as a selected file.
                */
               final File[] selectedFiles = mFileChooser.getSelectedFiles();
               if(selectedFiles.length > 0) {
                  mResult = JFileChooser.APPROVE_OPTION;
                  SpectrumFileChooser.this.setVisible(false);
               } else if(mOldAction != null)
                  mFileChooser.getActionMap().get(mOldAction).actionPerformed(e);
            }
         }
         getRootPane().getActionMap().put(ENTER_ACTION_KEY, new EnterAction(null));
         mFileChooser.getActionMap().put(ENTER_ACTION_KEY, new EnterAction(prevAction));
         mFileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               // This seems to be what it takes to capture a double-click on a
               // spectrum file to close the dialog.
               if(arg0.getActionCommand() == "ApproveSelection") {
                  final File[] selectedFiles = mFileChooser.getSelectedFiles();
                  if(selectedFiles.length > 0) {
                     mResult = JFileChooser.APPROVE_OPTION;
                     SpectrumFileChooser.this.setVisible(false);
                  }
               }
            }
         });

      }
   }

   /**
    * Constructs a SpectrumFileChooser
    * 
    * @param owner
    * @param title
    * @throws HeadlessException
    */
   public SpectrumFileChooser(Frame owner, String title)
         throws HeadlessException {
      super(owner, title);
      try {
         initialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SpectrumFileChooser
    * 
    * @param owner
    * @param title
    * @throws HeadlessException
    */
   public SpectrumFileChooser(Dialog owner, String title)
         throws HeadlessException {
      super(owner, title);
      try {
         initialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public JFileChooser getFileChooser() {
      return mFileChooser;
   }

   public int showOpenDialog() {
      mResult = JFileChooser.ERROR_OPTION;
      setModal(true);
      setVisible(true);
      return mResult;
   }

   public void setMultiSelectionEnabled(boolean b) {
      mFileChooser.setMultiSelectionEnabled(b);
      mSelectSpectrum.setEnabled(!b);
      mSelected = null;
   }

   public ISpectrumData[] getSpectra() {
      final ArrayList<ISpectrumData> res = new ArrayList<ISpectrumData>();
      if(mFileChooser.isMultiSelectionEnabled()) {
         if(mSelected != null)
            for(final ISpectrumData[] specs : mSelected.values())
               for(final ISpectrumData spec : specs)
                  res.add(spec);
      } else if(mSelectSpectrum.getSelectedItem() != null)
         res.add((ISpectrumData) mSelectSpectrum.getSelectedItem());
      // Sort by acquisition time or when unavailable display name
      Collections.sort(res, new Comparator<ISpectrumData>() {
         @Override
         public int compare(ISpectrumData o1, ISpectrumData o2) {
            final Date d1 = o1.getProperties().getTimestampWithDefault(SpectrumProperties.AcquisitionTime, null);
            final Date d2 = o2.getProperties().getTimestampWithDefault(SpectrumProperties.AcquisitionTime, null);
            if((d1 != null) && (d2 != null))
               return d1.compareTo(d2);
            else
               return o1.toString().compareTo(o2.toString());
         }
      });
      return res.toArray(new ISpectrumData[res.size()]);
   }
}

package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Translate2D;
import gov.nist.microanalysis.Utility.Translate2D.CalibrationPoint;

/**
 * <p>
 * A frame for insertion into a dialog or parent frame that contains controls
 * for configuring relocation using the Translate2D class.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
public class RelocationPanel extends JPanel {

   private static final long serialVersionUID = 4974010576171294006L;

   private final JTextField xTranslation_Field = new JTextField();
   private final JTextField yTranslation_Field = new JTextField();
   private final JTextField rotation_Field = new JTextField();
   private final JTextField xScale_Field = new JTextField();
   private final JTextField yScale_Field = new JTextField();
   private final JTextField error_Field = new JTextField();

   private final JTextField x0_Field = new JTextField();
   private final JTextField y0_Field = new JTextField();
   private final JTextField x1_Field = new JTextField();
   private final JTextField y1_Field = new JTextField();

   private final JLabel text_Label = new JLabel("Add relocated points to calibrate the transformation.");

   private final JButton add_Button = new JButton("Add");
   private final JButton remove_Button = new JButton("Remove");
   private final JButton clear_Button = new JButton("Clear");

   private final JTable points_Table = new JTable();

   private Color backgroundColor;

   private final NumberFormat mParser = NumberFormat.getInstance();

   private ArrayList<Translate2D.CalibrationPoint> mPoints = new ArrayList<Translate2D.CalibrationPoint>();

   private final Translate2D mTransform = new Translate2D();

   /**
    * Constructs a RelocationFrame
    * 
    * @throws HeadlessException
    */
   public RelocationPanel() throws HeadlessException {
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private void initialize() throws Exception {
      final Border border = BorderFactory.createCompoundBorder(SwingUtils.createDefaultBorder(), BorderFactory.createEmptyBorder(4, 4, 4, 4));
      JPanel transformPanel;

      backgroundColor = x0_Field.getBackground();
      {
         final FormLayout fl = new FormLayout("4dlu, right:pref, 4dlu, 40dlu",
               "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
         final PanelBuilder pb = new PanelBuilder(fl);
         final CellConstraints cc = new CellConstraints();
         pb.addSeparator("Translation", cc.xyw(1, 1, 4));
         pb.addLabel("X translation", cc.xy(2, 3));
         pb.add(xTranslation_Field, cc.xy(4, 3));
         pb.addLabel("Y translation", cc.xy(2, 5));
         pb.add(yTranslation_Field, cc.xy(4, 5));

         pb.addSeparator("Scale", cc.xyw(1, 7, 4));
         pb.addLabel("X scale", cc.xy(2, 9));
         pb.add(xScale_Field, cc.xy(4, 9));
         pb.addLabel("Y scale", cc.xy(2, 11));
         pb.add(yScale_Field, cc.xy(4, 11));

         pb.addSeparator("Rotation", cc.xyw(1, 13, 4));
         pb.addLabel("Rotation", cc.xy(2, 15));
         pb.add(rotation_Field, cc.xy(4, 15));

         pb.addSeparator("Error", cc.xyw(1, 17, 4));
         pb.addLabel("Error", cc.xy(2, 19));
         pb.add(error_Field, cc.xy(4, 19));

         xTranslation_Field.setEditable(false);
         yTranslation_Field.setEditable(false);
         xScale_Field.setEditable(false);
         yScale_Field.setEditable(false);
         rotation_Field.setEditable(false);
         error_Field.setEditable(false);

         transformPanel = pb.getPanel();
         transformPanel.setBorder(border);

      }
      final JPanel mainPanel = new JPanel();
      {
         JPanel pointsPanel;
         {
            final FormLayout fl = new FormLayout(
                  "4dlu, right:pref, 4dlu, 30dlu, 4dlu, right:pref, 4dlu, 30dlu, 10dlu, 4dlu, right:pref, 4dlu, 30dlu, 4dlu, right:pref, 4dlu, 30dlu",
                  "100dlu, 10dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
            final PanelBuilder pb = new PanelBuilder(fl);
            final CellConstraints cc = new CellConstraints();
            updateTable();
            final DefaultListSelectionModel lsm = new DefaultListSelectionModel();
            lsm.addListSelectionListener(new ListSelectionListener() {
               @Override
               public void valueChanged(ListSelectionEvent e) {
                  final int[] rows = points_Table.getSelectedRows();
                  if (rows.length == 1) {
                     final Translate2D.CalibrationPoint cp = mPoints.get(rows[0]);
                     final NumberFormat df = new HalfUpFormat("0.000");
                     x0_Field.setText(df.format(cp.getX0()));
                     y0_Field.setText(df.format(cp.getY0()));
                     x1_Field.setText(df.format(cp.getX1()));
                     y1_Field.setText(df.format(cp.getY1()));
                     x0_Field.requestFocus();
                     x0_Field.selectAll();
                  }
               }

            });
            points_Table.setSelectionModel(lsm);

            final JScrollPane scroll = new JScrollPane(points_Table);

            pb.add(scroll, cc.xyw(1, 1, 17));

            final FocusListener fl1 = new FocusListener() {
               @Override
               public void focusGained(FocusEvent e) {
                  final JTextField tf = (JTextField) e.getComponent();
                  tf.selectAll();
               }

               @Override
               public void focusLost(FocusEvent e) {
               }

            };

            pb.addSeparator("Original position", cc.xyw(1, 3, 8));
            pb.addLabel("<HTML>X<sub>0</sub>", cc.xy(2, 5));
            x0_Field.addFocusListener(fl1);
            pb.add(x0_Field, cc.xy(4, 5));
            pb.addLabel("<HTML>Y<sub>0</sub>", cc.xy(6, 5));
            y0_Field.addFocusListener(fl1);
            pb.add(y0_Field, cc.xy(8, 5));

            pb.addSeparator("Relocated position", cc.xyw(10, 3, 8));
            pb.addLabel("<HTML>X<sub>1</sub>", cc.xy(11, 5));
            x1_Field.addFocusListener(fl1);
            pb.add(x1_Field, cc.xy(13, 5));
            pb.addLabel("<HTML>Y<sub>1</sub>", cc.xy(15, 5));
            y1_Field.addFocusListener(fl1);
            pb.add(y1_Field, cc.xy(17, 5));

            pb.add(text_Label, cc.xyw(1, 7, 17));

            pointsPanel = pb.getPanel();
            pointsPanel.setBorder(border);
         }
         JPanel buttonPanel = new JPanel();
         {
            final FormLayout fl = new FormLayout("pref, 4dlu, pref, 20dlu, pref", "pref");
            final PanelBuilder pb = new PanelBuilder(fl);
            final CellConstraints cc = new CellConstraints();

            add_Button.setMnemonic(KeyEvent.VK_A);
            add_Button.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  addButtonAction();
               }
            });
            pb.add(add_Button, cc.xy(1, 1));
            remove_Button.setMnemonic(KeyEvent.VK_R);
            remove_Button.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  removeButtonAction();
               }
            });
            pb.add(remove_Button, cc.xy(3, 1));
            clear_Button.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  clearButtonAction();
               }
            });
            pb.add(clear_Button, cc.xy(5, 1));
            buttonPanel = pb.getPanel();
            buttonPanel.setBorder(border);
         }
         mainPanel.setLayout(new BorderLayout());
         mainPanel.add(pointsPanel, BorderLayout.CENTER);
         mainPanel.add(buttonPanel, BorderLayout.SOUTH);
      }
      setLayout(new BorderLayout());
      mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 2));
      add(mainPanel, BorderLayout.CENTER);
      transformPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 2, 4, 4), border));
      add(transformPanel, BorderLayout.EAST);
      x0_Field.requestFocus();
   }

   private double parseField(JTextField field) {
      double res;
      try {
         res = mParser.parse(field.getText()).doubleValue();
         field.setBackground(backgroundColor);
      } catch (final Exception ex) {
         text_Label.setText("Unable to interpret " + field.getText() + " as a number.");
         text_Label.setForeground(Color.red);
         field.setBackground(Color.pink);
         field.requestFocus();
         res = Double.NaN;
      }
      return res;

   }

   private void addButtonAction() {
      final double y1 = parseField(y1_Field);
      final double x1 = parseField(x1_Field);
      final double y0 = parseField(y0_Field);
      final double x0 = parseField(x0_Field);
      final boolean notOk = Double.isNaN(x0) || Double.isNaN(y0) || Double.isNaN(x1) || Double.isNaN(y1);
      if (!notOk) {
         final Translate2D.CalibrationPoint cp = Translate2D.createCalibrationPoint(x0, y0, x1, y1);
         boolean different = true;
         for (final Translate2D.CalibrationPoint cp2 : mPoints)
            if (!cp.different(cp2)) {
               different = false;
               break;
            }
         if (different) {
            mPoints.add(cp);
            updateTable();
            text_Label.setText("One point added.");
            text_Label.setForeground(SystemColor.controlText);
         } else {
            text_Label.setText("This point is not sufficiently different.");
            text_Label.setForeground(Color.red);
         }
         x0_Field.requestFocus();
      }

   }

   private void updateTable() {
      final DefaultTableModel tm = new DefaultTableModel(
            new String[]{"Number", "<HTML>X<sub>0</sub>", "<HTML>Y<sub>0</sub>", "<HTML>X<sub>1</sub>", "<HTML>Y<sub>1</sub>"}, 0);
      final NumberFormat nf = new HalfUpFormat("0.####");
      final NumberFormat nf2 = new HalfUpFormat("0.#");
      int i = 0;
      for (final Translate2D.CalibrationPoint cp : mPoints)
         tm.addRow(new Object[]{Integer.toString(++i), nf.format(cp.getX0()), nf.format(cp.getY0()), nf.format(cp.getX1()), nf.format(cp.getY1())});
      points_Table.setModel(tm);
      mTransform.calibrate(mPoints);
      xTranslation_Field.setText(nf.format(mTransform.getXOffset()));
      yTranslation_Field.setText(nf.format(mTransform.getYOffset()));
      rotation_Field.setText(nf2.format(Math.toDegrees(mTransform.getRotation())));
      xScale_Field.setText(nf.format(mTransform.getXScale()));
      yScale_Field.setText(nf.format(mTransform.getYScale()));
      error_Field.setText(nf.format(mTransform.error(mPoints)));
   }

   private void removeButtonAction() {
      final int[] ind = points_Table.getSelectedRows();
      ArrayList<CalibrationPoint> removeMe = new ArrayList<CalibrationPoint>();
      for (int i : ind)
         removeMe.add(mPoints.get(i));
      mPoints.removeAll(removeMe);
      text_Label.setText(ind.length > 1 ? Integer.toString(ind.length) + " points removed." : "One point removed.");
      text_Label.setForeground(SystemColor.controlText);
      updateTable();
   }

   private void clearButtonAction() {
      mPoints.clear();
      text_Label.setText("Point table cleared.");
      text_Label.setForeground(SystemColor.controlText);
      updateTable();
   }

   public Translate2D getTransformation() {
      return mTransform;
   }

   public ArrayList<Translate2D.CalibrationPoint> getCalibrationPoints() {
      return new ArrayList<Translate2D.CalibrationPoint>(mPoints);
   }

   public void setCalibrationPoints(Collection<Translate2D.CalibrationPoint> pts) {
      mPoints = new ArrayList<Translate2D.CalibrationPoint>(pts);
      updateTable();
   }
}

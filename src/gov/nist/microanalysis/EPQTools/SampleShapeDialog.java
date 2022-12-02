package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.SampleShape;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.HalfUpFormat;

import java.awt.Color;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A dialog for defining/editing a SampleShape object.
 * 
 * @author nicholas
 */
public class SampleShapeDialog extends JDialog {

   static private enum SHAPE {
      BULK, BLOCK, RECT_PRISM, CYLINDER, SPHERE, TRI_PRISM, FIBER, HEMISPHERE, SQR_PYRAMID
   };

   final static double DEFAULT_VALUE = 1.0e-6;
   final static double DEFAULT_DENSITY = 3.0; // g/cm^3

   private static final long serialVersionUID = 1364519836314648953L;
   private final JRadioButton jRadioButton_Bulk = new JRadioButton("Bulk");
   private final JRadioButton jRadioButton_Block = new JRadioButton("Rectangular Block");
   private final JRadioButton jRadioButton_RectPrism = new JRadioButton("Rotated Square Block");
   private final JRadioButton jRadioButton_Cylinder = new JRadioButton("Vertical Cylinder");
   private final JRadioButton jRadioButton_Sphere = new JRadioButton("Sphere");
   private final JRadioButton jRadioButton_TriPrism = new JRadioButton("Triangular Prism");
   private final JRadioButton jRadioButton_Fiber = new JRadioButton("Fiber");
   private final JRadioButton jRadioButton_Hemisphere = new JRadioButton("Hemisphere");
   private final JRadioButton jRadioButton_SquarePyramid = new JRadioButton("Square Pyramid");
   private final ButtonGroup jButtonGroup_Shapes = new ButtonGroup();

   private final JTextField[] jTextField_Dim = new JTextField[]{new JTextField(), new JTextField(), new JTextField()};
   private final JLabel[] jLabel_Dim = new JLabel[]{new JLabel("Dimension"), new JLabel("Depth"), new JLabel("Width")};
   private final JLabel[] jLabel_Unit = new JLabel[]{new JLabel("\u00B5m"), new JLabel("\u00B5m"), new JLabel("\u00B5m")};

   private final JLabel jLabel_Diagram = new JLabel();

   private final JLabel jLabel_Density = new JLabel("Density");
   private final JTextField jTextField_Density = new JTextField();
   private final JLabel jLabel_GPerCC = new JLabel("g/cm\u00B9");

   private boolean mResultOk = false;

   JButton jButton_Ok = new JButton("Ok");
   JButton jButton_Cancel = new JButton("Cancel");

   public void initialize() {
      final CellConstraints cc = new CellConstraints();
      final PanelBuilder thisPanel = new PanelBuilder(new FormLayout("pref, 5dlu, pref", "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref"));
      jButtonGroup_Shapes.add(jRadioButton_Bulk);
      jButtonGroup_Shapes.add(jRadioButton_Block);
      jButtonGroup_Shapes.add(jRadioButton_RectPrism);
      jButtonGroup_Shapes.add(jRadioButton_Cylinder);
      jButtonGroup_Shapes.add(jRadioButton_Sphere);
      jButtonGroup_Shapes.add(jRadioButton_TriPrism);
      jButtonGroup_Shapes.add(jRadioButton_Fiber);
      jButtonGroup_Shapes.add(jRadioButton_Hemisphere);
      jButtonGroup_Shapes.add(jRadioButton_SquarePyramid);
      final ItemListener il = new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {
            SHAPE sh;
            if (jRadioButton_RectPrism.isSelected())
               sh = SHAPE.RECT_PRISM;
            else if (jRadioButton_Cylinder.isSelected())
               sh = SHAPE.CYLINDER;
            else if (jRadioButton_Sphere.isSelected())
               sh = SHAPE.SPHERE;
            else if (jRadioButton_TriPrism.isSelected())
               sh = SHAPE.TRI_PRISM;
            else if (jRadioButton_Fiber.isSelected())
               sh = SHAPE.FIBER;
            else if (jRadioButton_Hemisphere.isSelected())
               sh = SHAPE.HEMISPHERE;
            else if (jRadioButton_SquarePyramid.isSelected())
               sh = SHAPE.SQR_PYRAMID;
            else if (jRadioButton_Block.isSelected())
               sh = SHAPE.BLOCK;
            else
               sh = SHAPE.BULK;
            setShape(sh);
         }
      };
      jRadioButton_Bulk.addItemListener(il);
      jRadioButton_Block.addItemListener(il);
      jRadioButton_RectPrism.addItemListener(il);
      jRadioButton_Cylinder.addItemListener(il);
      jRadioButton_Sphere.addItemListener(il);
      jRadioButton_TriPrism.addItemListener(il);
      jRadioButton_Fiber.addItemListener(il);
      jRadioButton_Hemisphere.addItemListener(il);
      jRadioButton_SquarePyramid.addItemListener(il);

      final JPanel btnPanel = new JPanel(
            new FormLayout("pref", "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"));
      btnPanel.setBorder(SwingUtils.createTitledBorder("Shapes"));
      btnPanel.add(jRadioButton_Bulk, cc.xy(1, 1));
      btnPanel.add(jRadioButton_Block, cc.xy(1, 3));
      btnPanel.add(jRadioButton_RectPrism, cc.xy(1, 5));
      btnPanel.add(jRadioButton_Cylinder, cc.xy(1, 7));
      btnPanel.add(jRadioButton_Sphere, cc.xy(1, 9));
      btnPanel.add(jRadioButton_TriPrism, cc.xy(1, 11));
      btnPanel.add(jRadioButton_Fiber, cc.xy(1, 13));
      btnPanel.add(jRadioButton_Hemisphere, cc.xy(1, 15));
      btnPanel.add(jRadioButton_SquarePyramid, cc.xy(1, 17));
      thisPanel.add(btnPanel, cc.xy(1, 1));

      final String fmtStr = "5dlu, right:45dlu, 5dlu, 50dlu, 2dlu, pref, 5dlu";
      {
         final FocusListener fl = new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
               int idx = -1;
               for (int i = 0; i < 3; ++i)
                  if (e.getComponent() == jTextField_Dim[i]) {
                     idx = i;
                     break;
                  }
               if (idx != -1)
                  validateDimension(idx);
            }

            @Override
            public void focusGained(FocusEvent e) {
               ((JTextField) e.getComponent()).selectAll();
            }
         };

         final PanelBuilder pb = new PanelBuilder(new FormLayout(fmtStr, "pref, 5dlu, pref, 5dlu, pref"));
         for (int i = 0; i < 3; ++i) {
            pb.add(jLabel_Dim[i], cc.xy(2, 1 + (2 * i)));
            pb.add(jTextField_Dim[i], cc.xy(4, 1 + (2 * i)));
            jTextField_Dim[i].setText("1.0");
            pb.add(jLabel_Unit[i], cc.xy(6, 1 + (2 * i)));
            jTextField_Dim[i].addFocusListener(fl);
         }
         final JPanel panel = pb.getPanel();
         panel.setBorder(SwingUtils.createTitledBorder("Dimensions"));
         thisPanel.add(panel, cc.xy(1, 3));
      }
      {
         final PanelBuilder pb = new PanelBuilder(new FormLayout(fmtStr, "pref"));
         pb.add(jLabel_Density, cc.xy(2, 1));
         pb.add(jTextField_Density, cc.xy(4, 1));
         jTextField_Density.setText("3.0");
         jTextField_Density.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
               validateDensity();
            }

            @Override
            public void focusGained(FocusEvent e) {
               ((JTextField) e.getComponent()).selectAll();
            }

         });
         pb.add(jLabel_GPerCC, cc.xy(6, 1));
         pb.getPanel().setBorder(SwingUtils.createTitledBorder("Nominal Density"));
         thisPanel.add(pb.getPanel(), cc.xy(1, 5));
      }

      jButton_Ok.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (validateResults()) {
               mResultOk = true;
               setVisible(false);
            }
         }
      });

      jButton_Cancel.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mResultOk = false;
            setVisible(false);
         }
      });

      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Ok, jButton_Cancel);
      thisPanel.add(bbb.build(), cc.xyw(1, 7, 3));

      final JPanel sh = new JPanel(new FormLayout("center:180dlu:grow", "center:pref:grow"));
      sh.setBorder(SwingUtils.createTitledBorder("Diagram"));
      sh.add(jLabel_Diagram, cc.xy(1, 1));
      thisPanel.add(sh, cc.xywh(3, 1, 1, 5));
      jLabel_Diagram.setIcon(getIcon(SHAPE.BLOCK));

      final JPanel panel = thisPanel.getPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      setContentPane(panel);
      pack();
      jRadioButton_Bulk.setSelected(true);
      setShape(SHAPE.BULK);
   }

   private Icon getIcon(SHAPE sh) {
      switch (sh) {
         case BULK :
            return null;
         case BLOCK :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/rectangularPrism.png"));
         case RECT_PRISM :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/tetragonalPrism.png"));
         case CYLINDER :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/cylinder.png"));
         case SPHERE :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/sphere.png"));
         case TRI_PRISM :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/triangularPrism.png"));
         case FIBER :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/fiber.png"));
         case HEMISPHERE :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/hemisphere.png"));
         case SQR_PYRAMID :
            return new ImageIcon(JWizardDialog.class.getResource("ClipArt/squarePyramid.png"));
      }
      return null;
   }

   private void updateLabel(int idx, String text, boolean visible) {
      jLabel_Dim[idx].setText(text);
      jLabel_Dim[idx].setEnabled(visible);
      jTextField_Dim[idx].setEnabled(visible);
      jLabel_Unit[idx].setEnabled(visible);
   }

   private void updateDensity(boolean enable) {
      jTextField_Density.setEnabled(enable);
   }

   private void setShape(SHAPE sh) {
      jLabel_Diagram.setIcon(getIcon(sh));
      switch (sh) {
         case BULK :
            updateLabel(0, "Depth", false);
            updateLabel(1, "Thickness", false);
            updateLabel(2, "Width", false);
            updateDensity(false);
            break;
         case BLOCK :
            updateLabel(0, "Depth", true);
            updateLabel(1, "Thickness", true);
            updateLabel(2, "Width", true);
            updateDensity(true);
            break;
         case RECT_PRISM :
            updateLabel(0, "Depth", true);
            updateLabel(1, "Thickness", true);
            updateLabel(2, "Width", false);
            updateDensity(true);
            break;
         case CYLINDER :
            updateLabel(0, "Diameter", true);
            updateLabel(1, "Height", true);
            updateLabel(2, "Width", false);
            updateDensity(true);
            break;
         case SPHERE :
         case HEMISPHERE :
            updateLabel(0, "Diameter", true);
            updateLabel(1, "Thickness", false);
            updateLabel(2, "Width", false);
            updateDensity(true);
            break;
         case TRI_PRISM :
            updateLabel(0, "Height", true);
            updateLabel(1, "Thickness", false);
            updateLabel(2, "Width", true);
            updateDensity(true);
            break;
         case FIBER :
            updateLabel(0, "Diameter", true);
            updateLabel(1, "Thickness", false);
            updateLabel(2, "Length", true);
            updateDensity(true);
            break;
         case SQR_PYRAMID :
            updateLabel(0, "Thickness", true);
            updateLabel(1, "Base", false);
            updateLabel(2, "Width", false);
            updateDensity(true);
            break;
      }
   }

   public SampleShapeDialog(Frame parent) {
      super(parent, "Particle Shape Dialog", true);
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public SampleShapeDialog(Window parent) {
      super(parent, "Particle Shape Dialog");
      try {
         setModal(true);
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private double getDimension(int i) {
      final JTextField tf = jTextField_Dim[i];
      double v = DEFAULT_VALUE;
      final NumberFormat nf = NumberFormat.getInstance();
      try {
         v = nf.parse(tf.getText().trim()).doubleValue() * 1.0e-6;
         tf.setBackground(SystemColor.window);
      } catch (final ParseException ex) {
         tf.setBackground(Color.pink);
         tf.requestFocus();
         tf.setText("1.0");
         throw new NumberFormatException();
      }
      return v;
   }

   private boolean validateDensity() {
      try {
         final NumberFormat nf = NumberFormat.getInstance();
         final double v = nf.parse(jTextField_Density.getText().trim()).doubleValue();
         jTextField_Density.setBackground(SystemColor.window);
         if ((v >= 0.01) && (v <= 20.0))
            return true;
      } catch (final ParseException e) {
         jTextField_Density.setText("3.0");
      }
      jTextField_Density.setBackground(Color.pink);
      jTextField_Density.requestFocus();
      jTextField_Density.selectAll();
      return false;
   }

   private boolean validateDimension(int i) {
      final JTextField tf = jTextField_Dim[i];
      try {
         final NumberFormat nf = NumberFormat.getInstance();
         final double v = nf.parse(tf.getText().trim()).doubleValue();
         tf.setBackground(SystemColor.window);
         if ((v >= 0.01) && (v <= 1000.0))
            return true;
      } catch (final ParseException e) {
         tf.setText("3.0");
      }
      tf.setBackground(Color.pink);
      tf.requestFocus();
      tf.selectAll();
      return false;
   }

   public boolean validateResults() {
      SHAPE sh = SHAPE.BULK;
      if (jRadioButton_Block.isSelected())
         sh = SHAPE.BLOCK;
      else if (jRadioButton_RectPrism.isSelected())
         sh = SHAPE.RECT_PRISM;
      else if (jRadioButton_Cylinder.isSelected())
         sh = SHAPE.CYLINDER;
      else if (jRadioButton_Sphere.isSelected())
         sh = SHAPE.SPHERE;
      else if (jRadioButton_TriPrism.isSelected())
         sh = SHAPE.TRI_PRISM;
      else if (jRadioButton_Fiber.isSelected())
         sh = SHAPE.FIBER;
      else if (jRadioButton_Hemisphere.isSelected())
         sh = SHAPE.HEMISPHERE;
      else if (jRadioButton_SquarePyramid.isSelected())
         sh = SHAPE.SQR_PYRAMID;
      else
         sh = SHAPE.BULK;
      switch (sh) {
         case BULK :
            return true;
         case BLOCK :
            return validateDimension(1) && validateDimension(0) && validateDimension(2) && validateDensity();
         case RECT_PRISM :
            return validateDimension(0) && validateDimension(1) && validateDensity();
         case CYLINDER :
            return validateDimension(0) && validateDimension(1) && validateDensity();
         case SPHERE :
            return validateDimension(0) && validateDensity();
         case TRI_PRISM :
            return validateDimension(2) && validateDimension(0) && validateDensity();
         case FIBER :
            return validateDimension(0) && validateDimension(2) && validateDensity();
         case HEMISPHERE :
            return validateDimension(0) && validateDensity();
         case SQR_PYRAMID :
            return validateDimension(0) && validateDensity();
      }
      return false;
   }

   public SampleShape getSampleShape() {
      if (mResultOk) {
         SHAPE sh = SHAPE.BULK;
         if (jRadioButton_Block.isSelected())
            sh = SHAPE.BLOCK;
         else if (jRadioButton_RectPrism.isSelected())
            sh = SHAPE.RECT_PRISM;
         else if (jRadioButton_Cylinder.isSelected())
            sh = SHAPE.CYLINDER;
         else if (jRadioButton_Sphere.isSelected())
            sh = SHAPE.SPHERE;
         else if (jRadioButton_TriPrism.isSelected())
            sh = SHAPE.TRI_PRISM;
         else if (jRadioButton_Fiber.isSelected())
            sh = SHAPE.FIBER;
         else if (jRadioButton_Hemisphere.isSelected())
            sh = SHAPE.HEMISPHERE;
         else if (jRadioButton_SquarePyramid.isSelected())
            sh = SHAPE.SQR_PYRAMID;
         else
            sh = SHAPE.BULK;
         switch (sh) {
            case BULK :
               return new SampleShape.Bulk();
            case BLOCK :
               return new SampleShape.RightRectangularPrism(getDimension(1), getDimension(0), getDimension(2));
            case RECT_PRISM :
               return new SampleShape.TetragonalPrism(getDimension(0), getDimension(1));
            case CYLINDER :
               return new SampleShape.Cylinder(0.5 * getDimension(0), getDimension(1));
            case SPHERE :
               return new SampleShape.Sphere(0.5 * getDimension(0));
            case TRI_PRISM :
               return new SampleShape.TriangularPrism(getDimension(2), getDimension(0));
            case FIBER :
               return new SampleShape.Fiber(0.5 * getDimension(0), getDimension(2));
            case HEMISPHERE :
               return new SampleShape.Hemisphere(0.5 * getDimension(0));
            case SQR_PYRAMID :
               return new SampleShape.SquarePyramid(2.0 * getDimension(0));
            default :
               return null;
         }
      } else
         return null;
   }

   private void setDimension(int i, double v) {
      final NumberFormat df = new HalfUpFormat("0.0");
      jTextField_Dim[i].setText(df.format(1.0e6 * v));
   }

   public void setSampleShape(SampleShape ss) {
      if ((ss == null) || (ss instanceof SampleShape.Bulk)) {
         setShape(SHAPE.BULK);
         jRadioButton_Bulk.setSelected(true);
      } else if (ss instanceof SampleShape.RightRectangularPrism) {
         final SampleShape.RightRectangularPrism shape = (SampleShape.RightRectangularPrism) ss;
         setDimension(0, shape.getDepth());
         setDimension(1, shape.getHeight());
         setDimension(2, shape.getWidth());
         setShape(SHAPE.BLOCK);
         jRadioButton_Block.setSelected(true);
      } else if (ss instanceof SampleShape.TetragonalPrism) {
         final SampleShape.TetragonalPrism shape = (SampleShape.TetragonalPrism) ss;
         setDimension(0, shape.getDiagonal());
         setDimension(1, shape.getHeight());
         setShape(SHAPE.RECT_PRISM);
         jRadioButton_RectPrism.setSelected(true);
      } else if (ss instanceof SampleShape.Cylinder) {
         final SampleShape.Cylinder shape = (SampleShape.Cylinder) ss;
         setDimension(0, 2.0 * shape.getRadius());
         setDimension(1, shape.getHeight());
         setShape(SHAPE.CYLINDER);
         jRadioButton_Cylinder.setSelected(true);
      } else if (ss instanceof SampleShape.Sphere) {
         final SampleShape.Sphere shape = (SampleShape.Sphere) ss;
         setDimension(0, 2.0 * shape.getRadius());
         setShape(SHAPE.SPHERE);
         jRadioButton_Sphere.setSelected(true);
      } else if (ss instanceof SampleShape.TriangularPrism) {
         final SampleShape.TriangularPrism shape = (SampleShape.TriangularPrism) ss;
         setDimension(0, shape.getHeight());
         setDimension(2, shape.getLength());
         setShape(SHAPE.TRI_PRISM);
         jRadioButton_TriPrism.setSelected(true);
      } else if (ss instanceof SampleShape.Fiber) {
         final SampleShape.Fiber shape = (SampleShape.Fiber) ss;
         setDimension(0, 2.0 * shape.getRadius());
         setDimension(2, shape.getLength());
         setShape(SHAPE.FIBER);
         jRadioButton_Fiber.setSelected(true);
      } else if (ss instanceof SampleShape.Hemisphere) {
         final SampleShape.Hemisphere shape = (SampleShape.Hemisphere) ss;
         setDimension(0, 2.0 * shape.getRadius());
         setShape(SHAPE.HEMISPHERE);
         jRadioButton_Hemisphere.setSelected(true);
      } else if (ss instanceof SampleShape.SquarePyramid) {
         final SampleShape.SquarePyramid shape = (SampleShape.SquarePyramid) ss;
         setDimension(0, shape.getHeight());
         setShape(SHAPE.SQR_PYRAMID);
         jRadioButton_SquarePyramid.setSelected(true);
      } else
         assert false;
   }

   public boolean isOk() {
      return mResultOk;
   }

   public double getDensity() {
      final JTextField tf = jTextField_Density;
      double v = DEFAULT_DENSITY;
      try {
         final NumberFormat nf = NumberFormat.getInstance();
         v = nf.parse(tf.getText().trim()).doubleValue();
         tf.setBackground(SystemColor.window);
      } catch (final ParseException ex) {
         tf.setBackground(Color.pink);
         tf.requestFocus();
         tf.setText("3.0");
      }
      return ToSI.gPerCC(v);
   }

   /**
    * @param den
    *           in SI (kg/m^3)
    */
   public void setDensity(double den) {
      final NumberFormat df = new HalfUpFormat("0.0");
      jTextField_Density.setText(df.format(FromSI.gPerCC(den)));
   }

   /**
    * Enable those shapes supported by the specified correction algorithm.
    * 
    * @param ca
    */
   public void enableShapes(CorrectionAlgorithm ca) {
      jRadioButton_Bulk.setEnabled(ca.supports(SampleShape.Bulk.class));
      jRadioButton_Block.setEnabled(ca.supports(SampleShape.RightRectangularPrism.class));
      jRadioButton_RectPrism.setEnabled(ca.supports(SampleShape.TetragonalPrism.class));
      jRadioButton_Cylinder.setEnabled(ca.supports(SampleShape.Cylinder.class));
      jRadioButton_Sphere.setEnabled(ca.supports(SampleShape.Sphere.class));
      jRadioButton_TriPrism.setEnabled(ca.supports(SampleShape.TriangularPrism.class));
      jRadioButton_Fiber.setEnabled(ca.supports(SampleShape.Fiber.class));
      jRadioButton_Hemisphere.setEnabled(ca.supports(SampleShape.Hemisphere.class));
      jRadioButton_SquarePyramid.setEnabled(ca.supports(SampleShape.SquarePyramid.class));
      if (!jButtonGroup_Shapes.getSelection().isEnabled()) {
         final Enumeration<AbstractButton> ea = jButtonGroup_Shapes.getElements();
         for (AbstractButton ab = ea.nextElement(); ea.hasMoreElements(); ab = ea.nextElement())
            if (ab.isEnabled()) {
               ab.setSelected(true);
               break;
            }
      }
   }
}

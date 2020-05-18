package gov.nist.microanalysis.EPQTools;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * A tool bar for common spectrum related tasks.
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
public class SpectrumToolBar
   extends JPanel {

   private static final long serialVersionUID = 2281785269926301222L;

   public static final int ZOOM_ALL = 0x1;
   public static final int ZOOM_REGION = 0x2;
   public static final int ZOOM_IN2 = 0x4;
   public static final int ZOOM_IN5 = 0x8;
   public static final int ZOOM_OUT2 = 0x10;
   public static final int ZOOM_OUT5 = 0x20;
   public static final int ZOOM_VALUES = 0x40;
   public static final int ALL = 0x7F;

   private final int mMode;
   private final SpecDisplay jSpecDisplay;

   private final Icon getIcon(String name) {
      final URL url = getClass().getResource("ClipArt/" + name);
      return new ImageIcon(url);
   }

   public SpectrumToolBar(SpecDisplay disp, int mode) {
      super();
      mMode = mode;
      jSpecDisplay = disp;
      initialize();
   }

   public SpectrumToolBar(SpecDisplay disp) {
      this(disp, ALL);
   }

   private JButton createButton(String icon, String toolTip) {
      final JButton btn = new JButton();
      btn.setContentAreaFilled(false);
      btn.setBorderPainted(false);
      btn.setFocusable(false);
      final Icon icn = getIcon(icon);
      btn.setIcon(icn);
      btn.setToolTipText(toolTip);
      btn.setPreferredSize(new Dimension(icn.getIconWidth(), icn.getIconHeight()));
      return btn;
   }

   private class ZoomButton
      extends JButton {

      private static final long serialVersionUID = -2413906973485907053L;
      private final int mUpper;

      ZoomButton(int r) {
         super(Integer.toString(r));
         setContentAreaFilled(false);
         setBorderPainted(false);
         setFocusable(false);
         setMargin(new Insets(0, 2, 0, 2));
         setToolTipText("Zoom to [0," + Integer.toString(r) + " keV]");
         mUpper = r;
         addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomTo(0.0, 1000.0 * mUpper);
            }

         });

      }
   }

   public void initialize() {
      final PanelBuilder pb = new PanelBuilder(new FormLayout("pref", "pref:grow, 1dlu, pref:grow, 1dlu, pref:grow, 1dlu, pref:grow, 1dlu, pref:grow, 1dlu, pref:grow, 1dlu, pref:grow"), this);
      final CellConstraints cc = new CellConstraints();
      removeAll();
      int pos = 1;
      if((mMode & ZOOM_ALL) != 0) {
         final JButton btn = createButton("all_sm.png", "Zoom to all");
         btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomToAll();
            }
         });
         pb.add(btn, cc.xy(1, pos));
         pos += 2;
      }
      if((mMode & ZOOM_IN5) != 0) {
         final JButton btn = createButton("up2_sm.png", "Zoom in by 5 \u00D7");
         btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomInBy(5.0);
            }
         });
         pb.add(btn, cc.xy(1, pos));
         pos += 2;
      }
      if((mMode & ZOOM_IN2) != 0) {
         final JButton btn = createButton("up1_sm.png", "Zoom in by 2 \u00D7");
         btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomInBy(2.0);
            }
         });
         pb.add(btn, cc.xy(1, pos));
         pos += 2;
      }
      if((mMode & ZOOM_REGION) != 0) {
         final JButton btn = createButton("zoom_sm.png", "Zoom to ROI");
         btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomToRegion();
            }
         });
         pb.add(btn, cc.xy(1, pos));
         pos += 2;
      }
      if((mMode & ZOOM_OUT2) != 0) {
         final JButton btn = createButton("down1_sm.png", "Zoom out by 2 \u00D7");
         btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomInBy(0.5);
            }
         });
         pb.add(btn, cc.xy(1, pos));
         pos += 2;
      }
      if((mMode & ZOOM_OUT5) != 0) {
         final JButton btn = createButton("down2_sm.png", "Zoom out by 5 \u00D7");
         btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
               jSpecDisplay.zoomInBy(0.2);
            }
         });
         pb.add(btn, cc.xy(1, pos));
         pos += 2;
      }
      if((mMode & ZOOM_VALUES) != 0) {
         final PanelBuilder pb2 = new PanelBuilder(new FormLayout("pref, 1dlu, pref, 1dlu, pref", "pref, 1dlu, pref"));
         pb2.add(new ZoomButton(5), cc.xy(1, 1));
         pb2.add(new ZoomButton(15), cc.xy(3, 1));
         pb2.add(new ZoomButton(30), cc.xy(5, 1));
         pb2.add(new ZoomButton(10), cc.xy(1, 3));
         pb2.add(new ZoomButton(20), cc.xy(3, 3));
         pb2.add(new ZoomButton(40), cc.xy(5, 3));
         pb.add(pb2.getPanel(), cc.xy(1, pos));
      }
   }
}

/**
 * <p>
 * Title: gov.nist.microanalysis.EPQTools.PreferenceEditor.java
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
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */
package gov.nist.microanalysis.EPQTools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * 
 */
public class PreferenceEditor
   extends JPanel {
   static final long serialVersionUID = 0x42;
   private final JTree mTree;
   private final DefaultMutableTreeNode mTop = new DefaultMutableTreeNode();
   private final JPanel mButtonPanel = new JPanel(new FormLayout("50dlu, 5dlu, 50dlu", "pref"));
   private final JButton mAddButton = new JButton("Add");
   private final JButton mRemoveButton = new JButton("Remove");
   private JPanel mContent = new JPanel();
   private final JPopupMenu mAddMenu = new JPopupMenu();
   private final TreeMap<String, JPanel> mContentMap = new TreeMap<String, JPanel>();
   private final CellConstraints cc = new CellConstraints();
   private final ArrayList<String> mNodeList = new ArrayList<String>();
   private final String mRootName;

   public PreferenceEditor(String rootName, JPanel content) {
      super(new FormLayout("50dlu, 5dlu, 50dlu, 5dlu, 60dlu, 60dlu, 60dlu, 38dlu", "pref, 30dlu, 30dlu, 30dlu, 25dlu"));

      final Border loweredbevel = SwingUtils.createDefaultBorder();

      mRootName = rootName;
      mTop.setUserObject(mRootName);
      mTree = new JTree(mTop);
      mTree.setBorder(loweredbevel);
      mTree.addTreeSelectionListener(new TreeSelectionListener() {
         @Override
         public void valueChanged(TreeSelectionEvent e) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) mTree.getLastSelectedPathComponent();
            System.out.println("You selected: " + node.toString());
            final JPanel panel = mContentMap.get(node.toString());
            remove(mContent);
            mContent = panel;
            add(mContent, cc.xywh(5, 1, 4, 4));

            updateUI();
         }
      });

      mButtonPanel.add(mAddButton, cc.xy(1, 1));
      mButtonPanel.add(mRemoveButton, cc.xy(3, 1));

      if(content != null)
         mContent = content;
      mContentMap.put(mRootName, content);

      mAddButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mAddMenu.show(mAddButton, mAddButton.getX(), mAddButton.getY());
         }
      });

      mRemoveButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) mTree.getLastSelectedPathComponent();
            node.removeFromParent();
            mTree.updateUI();
            mNodeList.remove(node.toString());
            final JPanel panel = mContentMap.get(mRootName);
            remove(mContent);
            mContent = panel;
            add(mContent, cc.xywh(5, 1, 4, 4));
            updateUI();
         }
      });

      add(mButtonPanel, cc.xyw(1, 1, 3));
      add(mTree, cc.xywh(1, 2, 3, 4));
      add(mContent, cc.xywh(5, 1, 4, 4));
   }

   public void addOption(String optionName, JPanel content) {
      final JMenuItem item = mAddMenu.add(optionName);
      mContentMap.put(optionName, content);
      item.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if(!mNodeList.contains(e.getActionCommand())) {
               final DefaultMutableTreeNode node = new DefaultMutableTreeNode(e.getActionCommand());
               mNodeList.add(e.getActionCommand());
               mTop.add(node);
               mTree.updateUI();
            }
         }
      });
   }

   public JPanel getContentPanel() {
      return mContent;
   }

   /**
    * main - for testing only
    * 
    * @param args
    */
   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch(final Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      JFrame.setDefaultLookAndFeelDecorated(false);

      final JFrame frame = new JFrame();
      frame.setTitle("Testing of PreferenceEditor");
      final JPanel mainPanel = new JPanel(new FormLayout("pref", "pref"));
      final CellConstraints cc = new CellConstraints();
      mainPanel.add(new JLabel("This is the main panel"), cc.xy(1, 1));
      final PreferenceEditor pe = new PreferenceEditor("Report", mainPanel);
      final Border loweredetched = SwingUtils.createDefaultBorder();
      mainPanel.setBorder(loweredetched);
      final JPanel headerPanel = new JPanel();
      headerPanel.setBorder(loweredetched);
      headerPanel.add(new JLabel("This is just a test"));
      pe.addOption("Header", headerPanel);

      frame.add(pe);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.setVisible(true);

   }

}

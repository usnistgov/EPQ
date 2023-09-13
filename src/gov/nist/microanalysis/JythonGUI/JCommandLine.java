package gov.nist.microanalysis.JythonGUI;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import gov.nist.microanalysis.JythonGUI.JCommandLine;

import javax.swing.JTextPane;

/**
 * <p>
 * A GUI based command line editor designed to provide a user mechanism to edit
 * commands to and results from a scripting language. (Designed with Jython in
 * mind.)
 * </p>
 * <p>
 * Not copyright: In the public domain
 * </p>
 *
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */
public class JCommandLine extends JTextPane {
   private static final long serialVersionUID = 0x1;
   private int mCmdOffset = -1;
   private PerformCommand mCmdProcessor;
   private String mCurrentCmd = null;
   private int mLastChar = 10;
   private ArrayList<String> mCmdBuffer = new ArrayList<>();
   private int mCmdIndex = -1;
   private StyleWriter mErrorWriter;
   static private final String COMMAND = "Command";
   static private final String PROMPT = "Prompt";
   static private final String INNER_ERROR = "InternalError";

   /**
    * <p>
    * Title: PerformCommand
    * </p>
    * <p>
    * Description: An interface for plugging in an interpreter into
    * JCommandLine. Implement the Do method to perform the user specified
    * command.
    * </p>
    *
    * @author Nicholas W. M. Ritchie
    * @version 1.0
    */
   public interface PerformCommand {
      void Do(String cmd);

      void executeScript(InputStream is);
   }

   /**
    * <p>
    * Title: JCommandLine.StyleWriter
    * </p>
    * <p>
    * Description: Implement a simple Writer that outputs the text to the
    * JCommandLine pane in a specified style....
    * </p>
    *
    * @author Nicholas W. M. Ritchie
    * @version 1.0
    */
   public class StyleWriter extends Writer {
      private String mStyle;
      private StringBuffer mBuffer = new StringBuffer();
      private boolean mClosed = false;

      // Constucted using JCommandLine.createStyleWriter
      private StyleWriter(String style) {
         mStyle = style;
      }

      // Writer.close();
      public void close() {
         mClosed = true;
      }

      // Writer.flush();
      public void flush() throws IOException {
         if (mBuffer.length() > 0) {
            try {
               String cmd = null;
               Document doc = getDocument();
               // Store the command line away
               if (mCmdOffset != Integer.MAX_VALUE) {
                  cmd = doc.getText(mCmdOffset, doc.getLength() - mCmdOffset);
               }
               doc.insertString(doc.getLength(), mBuffer.toString(), JCommandLine.this.getStyle(mStyle));
               int len = mBuffer.length();
               mLastChar = mBuffer.charAt(len - 1);
               mBuffer.delete(0, len);
               // restore it after displaying the text
               if (cmd != null) {
                  createCmdLine(cmd);
               } else {
                  JCommandLine.this.setCaretPosition(doc.getLength());
               }
            } catch (BadLocationException ex) {
               throw new IOException(ex.toString());
            }
         }
      }

      // Writer.write();
      public void write(char[] cbuf, int off, int len) throws IOException {
         if (!mClosed) {
            mBuffer.append(cbuf, off, len);
            if (mBuffer.length() > 1024) {
               flush();
            }
         }
      }

      /**
       * getStyle - Allows the user of the StyleWriter to modify the Style. Whe
       * the StyleWriter is constructed, the Style is just the default style for
       * the underlying JTextPane. However the various attributes can be readily
       * modified using the various StyleConstants static methods.
       *
       * @return Style
       */
      public Style getStyle() {
         return JCommandLine.this.getStyle(mStyle);
      }
   };

   /**
    * <p>
    * Title: DummyExcutive
    * </p>
    * <p>
    * Description: A trivial example of how to implement the PerformCommand
    * interface.
    * </p>
    */
   private class DummyExecutive implements PerformCommand {
      private StyleWriter mResultWriter;

      DummyExecutive() {
         mResultWriter = createStyleWriter(toString());
         StyleConstants.setItalic(mResultWriter.getStyle(), true);
      }

      public void Do(String cmd) {
         try {
            mResultWriter.write(cmd);
            mResultWriter.flush();
         } catch (IOException ex) {
         }
      }

      public void executeScript(InputStream is) {
         try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str = null;
            do {
               str = br.readLine();
               if (str != null) {
                  mResultWriter.write(str);
               }
            } while (str != null);
         } catch (IOException ex) {
         }
      }
   }

   private void writeException(String str) {
      Document doc = getDocument();
      try {
         doc.insertString(doc.getLength(), str, getStyle(INNER_ERROR));
      } catch (BadLocationException ex) {
      }
   }

   public void writeError(String str) {
      try {
         if (mLastChar != 10) {
            mErrorWriter.write("\n");
         }
         mErrorWriter.write(str);
         mErrorWriter.flush();
      } catch (IOException ex) {
      }
   }

   public void writeCommand(String str) {
      Document doc = getDocument();
      try {
         doc.remove(mCmdOffset, doc.getLength() - mCmdOffset);
         doc.insertString(mCmdOffset, str, getStyle(COMMAND));
         doc.insertString(mCmdOffset, "\n", getStyle(COMMAND));
      } catch (BadLocationException ex) {
      }
   }

   public void execute(InputStream is) {
      class CmdProcessStream implements Runnable {
         InputStream mStream;

         CmdProcessStream(InputStream is) {
            mStream = is;
         }

         public void run() {
            try {
               mCmdProcessor.executeScript(mStream);
               mStream.close();
            } catch (IOException ex) {
            }
         }
      }

      mCmdOffset = Integer.MAX_VALUE;
      if (mCmdProcessor != null) 
         (new CmdProcessStream(is)).run();
      createCmdLine("");
   }

   private void createCmdLine(String cmd) {
      try {
         StyledDocument doc = (StyledDocument) getDocument();
         int len = doc.getLength();
         doc.insertString(len, (mLastChar == 10 ? "" : "\n") + Integer.toString(mCmdBuffer.size() + 1) + ">", getStyle(PROMPT));
         mLastChar = 0;
         mCmdOffset = doc.getLength() + 1;
         doc.insertString(mCmdOffset - 1, " " + cmd, getStyle(COMMAND));
         this.setCaretPosition(mCmdOffset);
      } catch (BadLocationException ex) {
      }
   }

   public JCommandLine(StyledDocument sd) {
      this();
      this.setDocument(sd);
   }

   /**
    * JCommandLine - The default constructor for JCommandLine
    *
    * @throws HeadlessException
    */
   public JCommandLine() throws HeadlessException {
      super();
      Style cmd = addStyle(COMMAND, getStyle("default"));
      StyleConstants.setForeground(cmd, Color.blue);
      Style prmt = addStyle(PROMPT, getStyle("default"));
      StyleConstants.setForeground(prmt, Color.lightGray);
      Style err = addStyle(INNER_ERROR, getStyle("default"));
      StyleConstants.setForeground(err, Color.RED);
      mErrorWriter = createStyleWriter("__ERROR__");
      StyleConstants.setForeground(mErrorWriter.getStyle(), Color.RED);
      createCmdLine("");

      this.addKeyListener(new java.awt.event.KeyAdapter() {
         // I'll insert my own returns and customize the delete key
         public void keyPressed(KeyEvent e) {
            int ss = Math.min(getSelectionEnd(), getSelectionStart());
            int se = Math.max(getSelectionEnd(), getSelectionStart());
            switch (e.getKeyCode()) {
               case KeyEvent.VK_BACK_SPACE : {
                  if (se < mCmdOffset + 1) {
                     e.consume();
                     return;
                  }
                  if (ss < mCmdOffset) {
                     setSelectionStart(mCmdOffset);
                     setSelectionEnd(se);
                  }
                  break;
               }
               case KeyEvent.VK_DELETE : {
                  if (se < mCmdOffset) {
                     e.consume();
                     return;
                  }
                  if (ss < mCmdOffset) {
                     setSelectionStart(mCmdOffset);
                     setSelectionEnd(se);
                  }
                  break;
               }
               // Kill the enter key
               case KeyEvent.VK_ENTER :
                  e.consume();
                  break;
            }
         }

         // Handle the ESC, Ctrl-Up and Ctrl-Down keys
         public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_ESCAPE : {
                  Document doc = getDocument();
                  try {
                     doc.remove(mCmdOffset, doc.getLength() - mCmdOffset);
                     setCaretPosition(mCmdOffset);
                     e.consume();
                  } catch (BadLocationException ex) {
                  }
                  break;
               }
               case KeyEvent.VK_UP : {
                  if (e.isControlDown()) {
                     e.consume();
                     if (mCmdIndex > 0) {
                        Document doc = getDocument();
                        try {
                           doc.remove(mCmdOffset, doc.getLength() - mCmdOffset);
                           --mCmdIndex;
                           doc.insertString(mCmdOffset, (String) mCmdBuffer.get(mCmdIndex), getStyle(COMMAND));
                           setCaretPosition(doc.getLength());
                        } catch (BadLocationException ex1) {
                        }
                     }
                  }
                  break;
               }
               case KeyEvent.VK_DOWN : {
                  if (e.isControlDown()) {
                     e.consume();
                     if (mCmdIndex + 1 < mCmdBuffer.size()) {
                        Document doc = getDocument();
                        try {
                           doc.remove(mCmdOffset, doc.getLength() - mCmdOffset);
                           ++mCmdIndex;
                           doc.insertString(mCmdOffset, (String) mCmdBuffer.get(mCmdIndex), getStyle(COMMAND));
                           setCaretPosition(doc.getLength());
                        } catch (BadLocationException ex1) {
                        }
                     }
                  }
                  break;
               }
            }
         }

         // Disable keyboard input if we are not currently on the command
         // line...
         public void keyTyped(KeyEvent e) {
            int ss = Math.min(getSelectionEnd(), getSelectionStart());
            int se = Math.max(getSelectionEnd(), getSelectionStart());
            // Works inside the debugger but not outside...
            if ((se == mCmdOffset) && (e.getKeyChar() == KeyEvent.VK_BACK_SPACE)) {
               e.consume();
               return;
            }
            if (ss < mCmdOffset) {
               if (se < mCmdOffset) {
                  e.consume();
                  return;
               }
               setSelectionStart(mCmdOffset);
               setSelectionEnd(se);
            }
            switch (e.getKeyChar()) {
               case '\n' : {
                  try {
                     if (mCmdProcessor != null) {
                        Document doc = getDocument();
                        int len = doc.getLength();
                        String cmd = doc.getText(mCmdOffset, len - mCmdOffset);
                        doc.insertString(len, "\n", getStyle("COMMAND"));
                        mCmdBuffer.add(cmd);
                        mCmdIndex = mCmdBuffer.size();
                        mCmdOffset = Integer.MAX_VALUE; // To inhibit writing
                                                        // more text...
                        mLastChar = 10;
                        mCurrentCmd = cmd;
                        try {
                           mCmdProcessor.Do(mCurrentCmd);
                           mCurrentCmd = null;
                        } catch (Exception ex) {
                           writeException(e.toString());
                        }
                        createCmdLine("");
                        e.consume();
                     }
                  } catch (BadLocationException ex) {
                     writeException("Command line error: " + ex.toString());
                  }
                  break;
               }
            }
         }
      });
      // Debug stuff
      mCmdProcessor = new DummyExecutive();
   }

   /**
    * cut - Override cut to copy the entire selected region but only delete the
    * portion within the command line.
    */
   public void cut() {
      StringSelection ss = new StringSelection(getSelectedText());
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
      int st = getSelectionStart();
      int ed = getSelectionEnd();
      if (st > ed) {
         int t = st;
         st = ed;
         ed = t;
      }
      if (ed < mCmdOffset) {
         return;
      }
      if (st < mCmdOffset) {
         st = mCmdOffset;
      }
      setSelectionStart(st);
      setSelectionEnd(ed);
      replaceSelection("");
   }

   /**
    * addBanner - Add a banner string to the top of the JCommandLine panel.
    *
    * @param banner
    *           String
    */
   public void addBanner(String banner) {
      if (banner.charAt(banner.length() - 1) != '\n') {
         banner += "\n";
      }
      Document doc = getDocument();
      String bStyle = "BANNER";
      Style st = getStyle(bStyle);
      if (st == null) {
         st = this.addStyle(bStyle, getStyle("default"));
         StyleConstants.setBold(st, true);
      }
      try {
         doc.insertString(0, banner, st);
      } catch (BadLocationException ex) {
      }
      if (mCmdOffset != Integer.MAX_VALUE) {
         mCmdOffset += banner.length();
         setCaretPosition(mCmdOffset);
      }
   }

   /**
    * createStyleWriter - Create an instance of the StyleWriter class for
    * streaming text to this control.
    *
    * @param styleName
    *           String
    * @return StyleWriter
    */
   public StyleWriter createStyleWriter(String styleName) {
      addStyle(styleName, this.getStyle("default"));
      return new StyleWriter(styleName);
   }

   /**
    * setCommandExecutive - Set the object the will take resposibility for
    * processing the command line strings.
    *
    * @param pc
    *           PerformCommand
    */
   public void setCommandExecutive(PerformCommand pc) {
      mCmdProcessor = pc;
   }

   /**
    * getCommandExecutive - Returns an instance of the class that implements the
    * PerformCommand interface.
    *
    * @return PerformCommand
    */
   public PerformCommand getCommandExecutive() {
      return mCmdProcessor;
   }
}

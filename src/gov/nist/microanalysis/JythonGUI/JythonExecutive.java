package gov.nist.microanalysis.JythonGUI;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.swing.text.StyleConstants;

import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.InteractiveConsole;
import org.python.util.InteractiveInterpreter;

import gov.nist.microanalysis.JythonGUI.JCommandLine;

/**
 * <p>
 * A simple implementation of the JCommandLine.PerformCommand
 * interface for the Jython interpreter.
 * </p>
 * <p>
 * Not copyright: In the public domain * 
 * </p>
 *
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class JythonExecutive
   implements JCommandLine.PerformCommand {
   private JCommandLine mCmdLine;
   private InteractiveConsole mInterpreter;
   private JCommandLine.StyleWriter mErrorWriter;
   private JCommandLine.StyleWriter mOutputWriter;
   private JCommandLine.StyleWriter mScriptWriter;
   private boolean mDisplayScript = false;

   /**
    * JythonExecutive - Create a JythonExecutive associated with the specified
    * JCommandLine command line editor window.
    *
    * @param jCmdLine JCommandLine
    */
   public JythonExecutive(JCommandLine jCmdLine) {
      mCmdLine = jCmdLine;
      mCmdLine.setCommandExecutive(this);
      mErrorWriter = mCmdLine.createStyleWriter("IIError");
      StyleConstants.setForeground(mErrorWriter.getStyle(), Color.RED);
      StyleConstants.setFontFamily(mErrorWriter.getStyle(),"Courier");
      StyleConstants.setFontSize(mErrorWriter.getStyle(),12);
      mOutputWriter = mCmdLine.createStyleWriter("IIOut");
      StyleConstants.setItalic(mOutputWriter.getStyle(), true);
      StyleConstants.setFontFamily(mOutputWriter.getStyle(),"Courier");
      StyleConstants.setFontSize(mOutputWriter.getStyle(),12);
      mInterpreter = new InteractiveConsole();
      mInterpreter.setErr(mErrorWriter);
      mInterpreter.setOut(mOutputWriter);
      mInterpreter.set("PathSep",System.getProperty("file.separator"));
      mInterpreter.set("ScriptFile","?");
      setScriptSource(null);
      mInterpreter.set("stdOut",mOutputWriter);
      mInterpreter.set("stdErr",mErrorWriter);
      mCmdLine.addBanner(InteractiveConsole.getDefaultBanner());
      String str = System.getProperty("java.compiler");
      if(str != null) {
         mCmdLine.addBanner(str);
      }
      ByteArrayOutputStream bas = new ByteArrayOutputStream();
      PrintStream oldPs = System.err;
      System.setErr(new PrintStream(bas));
      { // Add any jars in the same directory as this jar to the Jython package
         // list
         String cp = System.getProperty("java.class.path").toLowerCase();
         if(cp != null) {
            int i = cp.toLowerCase().indexOf("\\jythongui.jar");
            if(i != -1) {
               cp = cp.substring(0, i); // up to start of
               i = cp.lastIndexOf(";");
               if(i != -1) {
                  cp = cp.substring(i + 1);
               }
               PySystemState.packageManager.addJarDir(cp, true);
            }
         }
      }
      String res = bas.toString();
      if(res.length() > 0) {
         mCmdLine.addBanner(res);
      }
      System.setErr(oldPs);
   }

   /**
    * Do - perform the action specified by the command. cmd is passed to the
    * InteractiveInterpreter. If a result is generated the result will be
    * displayed on the JCommandLine instance.
    *
    * @param cmd String
    */
   public void Do(String cmd) {
      try {
         mInterpreter.push(cmd);
         mOutputWriter.flush();
         mErrorWriter.flush();
      }
      catch(Exception ex) {
         try {
            mErrorWriter.write(ex.toString());
         }
         catch(IOException ioe) {
            System.err.print(ioe.toString());
         }
      }
   }

   public void setScriptSource(File file){
      mInterpreter.set("ScriptFile",file);
      if(file!=null){
         String name=file.getName();
         // Remove the extension...
         int p=name.lastIndexOf('.');
         if(p>0)
            name=name.substring(0,p);
         mInterpreter.set("DefaultOutput",file.getParent()+File.separator+name);
      } else
         mInterpreter.set("DefaultOutput",System.getProperty("user.home")+File.separator+"Jython");
   }

   public void executeScript(InputStream is) {
      long st = System.currentTimeMillis();
      try {
         if(mScriptWriter == null) {
            mScriptWriter = mCmdLine.createStyleWriter("script");
         }
         InputStreamReader isr = new InputStreamReader(is);
         BufferedReader br = new BufferedReader(isr);
         String str = br.readLine();
         StringBuffer script = new StringBuffer(4096);
         while(str != null) {
            if(mDisplayScript) {
               mScriptWriter.write(str + "\n");
               mScriptWriter.flush();
            }
            script.append(str + "\n");
            str = br.readLine();
         }
         mInterpreter.exec(script.toString());
         mInterpreter.cleanup();
      }
      catch(PyException pe) {
         try {
            if(pe.type.toString().indexOf("ThreadDeath") != -1) {
               mErrorWriter.write("The script was canceled.\n");
               mErrorWriter.flush();
            } else {
               mErrorWriter.write(pe.toString());
               mErrorWriter.flush();
            }
         }
         catch(IOException ioex) {
         }
      }
      catch(Exception ex) {
         try {
            mErrorWriter.write(ex.toString());
            mErrorWriter.flush();
         }
         catch(IOException ioex) {
         }
      }
      long delta = (System.currentTimeMillis() - st) / 100;
      try {
         int hrs = (int) (delta / 36000);
         int mins = (int) ((delta / 600) % 60);
         int secs = (int) ((delta / 10) % 60);
         int tenths = (int) (delta % 10);
         mScriptWriter.write("Execution time: " + Integer.toString(hrs) + (mins > 9 ? ":" : ":0") + Integer.toString(mins)
               + (secs > 9 ? ":" : ":0") + Integer.toString(secs) + "." + Integer.toString(tenths));
         mScriptWriter.flush();
      }
      catch(IOException ex1) {
      }
   }

   /**
    * getInteractiveInterpreter - Provides access to the InteractiveInterpreter.
    *
    * @return InteractiveInterpreter
    */
   public InteractiveInterpreter getInteractiveInterpreter() {
      return mInterpreter;
      
   }
   

}

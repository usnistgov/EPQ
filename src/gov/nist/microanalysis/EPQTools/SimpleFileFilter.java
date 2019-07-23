package gov.nist.microanalysis.EPQTools;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * <p>
 * A custom implementation of the FileChooser.FileFilter interface.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class SimpleFileFilter
   extends FileFilter {
   private final String[] mExtensions;
   private final String mDescription;

   public SimpleFileFilter(String[] exts, String desc) {
      mExtensions = new String[exts.length];
      for(int i = exts.length - 1; i >= 0; --i)
         mExtensions[i] = "." + exts[i].toLowerCase();
      mDescription = desc;
   }

   public SimpleFileFilter(String ext, String desc) {
      mExtensions = new String[1];
      mExtensions[0] = "." + ext.toLowerCase();
      mDescription = desc;
   }

   @Override
   public boolean accept(File f) {
      if(f.isDirectory())
         return true;
      final String name = f.getName().toLowerCase();
      for(int i = mExtensions.length - 1; i >= 0; --i)
         if(name.endsWith(mExtensions[i]))
            return true;
      return false;
   }

   @Override
   public String getDescription() {
      return mDescription;
   }

   public int getExtensionCount() {
      return mExtensions.length;
   }

   public String getExtension(int i) {
      return mExtensions[i].substring(1);
   }

   public File forceExtension(File f) {
      final String name = f.getName().toLowerCase();
      for(final String mExtension : mExtensions)
         if(name.endsWith(mExtension))
            return f;
      return new File(f.getParent(), f.getName() + mExtensions[0]);
   }
}

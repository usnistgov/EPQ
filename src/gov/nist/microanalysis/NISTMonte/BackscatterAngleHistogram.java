package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;

import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Histogram;

public class BackscatterAngleHistogram implements ActionListener {

   private Histogram mCurrent;
   private DescriptiveStatistics mDescCurr;
   private final Histogram mBackscattered = new Histogram(0.0, Math.PI, 180);
   private final Histogram mNotBackscattered = new Histogram(0.0, Math.PI, 180);
   private final Histogram mMaxBS = new Histogram(0.0, Math.PI, 180);
   private final Histogram mMaxNBS = new Histogram(0.0, Math.PI, 180);
   private final DescriptiveStatistics mDescBS = new DescriptiveStatistics();
   private final DescriptiveStatistics mDescNBS = new DescriptiveStatistics();
   private double mMaxAngle;
   private final double mMinEnergy;
   private boolean mQuickTerminate = false;

   public BackscatterAngleHistogram(double minEnergy, boolean quickTerminate) {
      assert minEnergy >= ToSI.eV(50.0);
      assert minEnergy < ToSI.keV(500.0);
      mMinEnergy = minEnergy;
      mQuickTerminate = quickTerminate;
   }
   
   public BackscatterAngleHistogram(double minEnergy) {
      this(minEnergy, false);
   }
   
   public DescriptiveStatistics getBackscatterDS() {
      return mDescBS;
   }

   public DescriptiveStatistics getNonBackscatterDS() {
      return mDescNBS;
   }


   @Override
   public void actionPerformed(ActionEvent ae) {
      MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
      final Electron el = mcss.getElectron();
      switch (ae.getID()) {
         case MonteCarloSS.TrajectoryStartEvent :
            mCurrent = new Histogram(0.0, Math.PI, 180);
            mDescCurr = new DescriptiveStatistics();
            mMaxAngle = 0.0;
            break;
         case MonteCarloSS.PostScatterEvent:
            if ((el.getEnergy() <= mMinEnergy) && (mCurrent != null)) {
               mNotBackscattered.merge(mCurrent);
               mMaxNBS.add(mMaxAngle);
               mDescNBS.merge(mDescCurr);
               mCurrent = null;
               mDescCurr = null;
               if(mQuickTerminate)
                  el.setTrajectoryComplete(true);
            } else if (mCurrent != null) {
               double sa = el.getScatteringAngle();
               assert !Double.isNaN(sa);
               mMaxAngle = Math.max(sa, mMaxAngle);
               mCurrent.add(sa);
               mDescCurr.add(sa);
            }
            break;
         case MonteCarloSS.BackscatterEvent :
            if (mCurrent != null) {
               double sa = el.getScatteringAngle();
               mMaxAngle = Math.max(sa, mMaxAngle);
               mCurrent.add(sa);
               assert !Double.isNaN(sa);
               mDescCurr.add(sa);
            }
            if ((el.getEnergy() > mMinEnergy) && (mCurrent != null)) {
               mBackscattered.merge(mCurrent);
               mDescBS.merge(mDescCurr);
               mMaxBS.add(mMaxAngle);
               mCurrent = null;
               mDescCurr = null;
               if(mQuickTerminate)
                  el.setTrajectoryComplete(true);
            }
            break;
         case MonteCarloSS.TrajectoryEndEvent :
            mCurrent = null;
            break;
         default :
            break;
      }
   }

   public void dump(PrintWriter pw) {
      pw.println("Bin,Min,Max,Backscatter,NotBackscatter,BS/NBS,Max_BS,Max_NBS");
      final double sc = (double) mNotBackscattered.totalCounts() / (double) mBackscattered.totalCounts();
      final double totBS = mMaxBS.totalCounts(), totNBS=mMaxNBS.totalCounts();
      for (int i = 0; i < mBackscattered.binCount(); ++i) {
         pw.print(mBackscattered.binName(i, "{0,number,#.##}"));
         pw.print(",");
         pw.print(i);
         pw.print(",");
         pw.print(i + 1);
         pw.print(",");
         pw.print(mBackscattered.counts(i));
         pw.print(",");
         pw.print(mNotBackscattered.counts(i));
         pw.print(",");
         if(mNotBackscattered.counts(i)>0)
            pw.print((sc * mBackscattered.counts(i)) / mNotBackscattered.counts(i));
         else
            pw.print("-");
         pw.print(",");
         pw.print(mMaxBS.counts(i)/totBS);
         pw.print(",");
         pw.print(mMaxNBS.counts(i)/totNBS);
         pw.println();
      }
   }
}

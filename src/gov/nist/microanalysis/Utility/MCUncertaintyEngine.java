package gov.nist.microanalysis.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

abstract public class MCUncertaintyEngine {

   private final ArrayList<UncertainValueMC> mResults;
   private final Number[] mArguments;

   public MCUncertaintyEngine(int iterations, Number[] arguments) {
      mResults = new ArrayList<UncertainValueMC>();
      mArguments = arguments;
      doIterations(iterations);
   }

   public void doIterations(int iterations) {
      for(int i = 0; i < iterations; i++) {
         Map<String, Double> rd = new TreeMap<String, Double>();
         UncertainValueMC[] args = new UncertainValueMC[mArguments.length];
         for(int j = 0; j < mArguments.length; ++j)
            args[j] = new UncertainValueMC(UncertainValue2.asUncertainValue2(mArguments[j]), rd);
         mResults.add(compute(args));
      }
   }

   public List<UncertainValueMC> getResults() {
      return mResults;
   }

   public DescriptiveStatistics getStatistics() {
      return DescriptiveStatistics.compute(mResults);
   }

   public double nominalValue() {
      return mResults.iterator().next().nominalValue();
   }

   public UncertainValue2 getResult() {
      final DescriptiveStatistics res = getStatistics();
      return new UncertainValue2(res.average(), res.standardDeviation());
   }

   abstract public UncertainValueMC compute(UncertainValueMC[] arguments);
}

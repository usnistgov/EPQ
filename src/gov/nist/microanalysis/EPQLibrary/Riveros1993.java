/**
 * 
 */
package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.Math2;

/**
 * The Riveros matrix correction algorithm can be used to perform the absorption
 * correction on the continuum.
 * 
 * 
 * @author nritchie
 *
 */
public class Riveros1993 {

   private final Composition mComposition;
   private final double mBeamEnergy;
   private final double mTakeOff;
   private final double mEtaM;
   
   private double alphaz(Element elm, double e0k, double eck) {
      final double z = elm.getAtomicNumber(), a = elm.getAtomicWeight();
      final double j = ToSI.keV(MeanIonizationPotential.Berger83.compute(elm));
      return (2.14e5 * Math.pow(z, 1.16)) / (a * Math.pow(e0k, 1.25))
            * Math.sqrt(Math.log(1.166 * e0k / j) / (e0k - eck));
   }

   private double betaz(Element elm, double e0k, double eck) {
      final double z = elm.getAtomicNumber(), a = elm.getAtomicWeight();
      return (1.1e5 * Math.pow(z, 1.5)) / ((e0k - eck) * a);
   }

   private double etam(Element elm, double e0k) {
      final double z = elm.getAtomicNumber();
      final double lz = Math.log(z);
      return (0.1904 + lz * (-0.2236 + lz * (0.1292 + lz * -0.01491)))
            * (2.167e-4 * z + 0.9987)
            * Math.pow(e0k, (0.1382 - 0.9211 / Math.sqrt(z)));

   }

   // Tests ok against NeXLCore
   public double etam(Composition comp, double e0k) {
      double res = 0.0;
      for (Element elm : comp.getElementSet())
         res += elasticFraction(elm, comp, e0k) * etam(elm, e0k);
      return res;
   }

   private double elasticXSec(double zz, double e0k) {
      final double alphaalpha = 3.4e-3 * Math.pow(zz, 0.67) / e0k, mc2 = 511.0;
      return 5.21e-21 * ((zz / e0k) * (zz / e0k))
            * (4 * Math.PI / (alphaalpha * (1.0 + alphaalpha)))
            * Math.pow(((e0k + mc2) / (e0k + 2.0 * mc2)), 2);
   }

   // Tests ok against NeXLCore
   public double elasticFraction(Element elm, Composition mat, double e0k) {
      double den = 0.0;
      for (Element el : mat.getElementSet()) {
         den += mat.atomicPercent(el) * elasticXSec(el.getAtomicNumber(), e0k);
      }
      return mat.atomicPercent(elm) * elasticXSec(elm.getAtomicNumber(), e0k)
            / den;
   }

   public Riveros1993(Composition comp, double e0, double takeOff) {
      // super("BremCorrection", "Riveros1993", LitReference.Riveros1993);
      mComposition = comp;
      mBeamEnergy = e0;
      mTakeOff = takeOff;
      mEtaM = etam(comp, FromSI.keV(e0));
   }

   public double chi(double e, double takeOffAngle, Composition comp) {
      assert (Math.abs(takeOffAngle) < ((Math.PI * 89.99) / 180.0));
      MassAbsorptionCoefficient mac = MassAbsorptionCoefficient.Default;
      return mac.compute(comp, e) / Math.sin(takeOffAngle);
   }

   public double compute(double e) {
      final double e0k = FromSI.keV(mBeamEnergy), eck = FromSI.keV(e), u0 = mBeamEnergy / e;
      assert u0 >= 1.0;
      final double phi0 = 1.0 + (mEtaM * u0 * Math.log(u0)) / (u0 - 1.0);
      final double gamma = (1.0 + mEtaM) * (u0 * Math.log(u0)) / (u0 - 1.0);
      double alpha = 0.0, beta = 0.0;
      for (Element elm : mComposition.getElementSet()) {
         final double mf = mComposition.weightFraction(elm, true);
         alpha += mf * alphaz(elm, e0k, eck);
         beta += mf * betaz(elm, e0k, eck);
      }
      assert alpha > 0.0;
      assert beta > 0.0;
      // Compute the matrix correction
      final double xm = MassAbsorptionCoefficient
            .toCmSqrPerGram(chi(e, mTakeOff, mComposition));
      final double ff = xm / (2.0 * alpha);
      final double gg = (beta + xm) / (2.0 * alpha);
      final double fchi = gg < 22.3
            ? (Math.sqrt(Math.PI)
                  * (Math.exp(ff * ff) * gamma * alpha * (1.0 - Math2.erf(ff))
                        - Math.exp(gg * gg) * (gamma - phi0) * alpha
                              * (1.0 - Math2.erf(gg))))
                  / (2.0 * alpha * alpha)
            : 0.0;
      final double f = gg < 22.3 ? (Math.sqrt(Math.PI)*(gamma - Math.exp(gg*gg)*(gamma - phi0)*Math2.erfc(gg)))/(2.0*alpha) : 1.0;
      return fchi / f;
   }
}

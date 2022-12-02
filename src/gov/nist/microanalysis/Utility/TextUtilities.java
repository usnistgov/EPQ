package gov.nist.microanalysis.Utility;

import java.util.Collection;

/**
 * <p>
 * Some utilities for handling text in pre 1.5 JDKs
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class TextUtilities {

   /**
    * toString - A replacement for the Arrays.toString available starting with
    * Java 5.
    * 
    * @param da
    *           double[]
    * @return String
    */
   public static final String toString(double[] da) {
      final StringBuffer sb = new StringBuffer((da.length * 15) + 4);
      sb.append("[");
      for (final double element : da)
         sb.append(element);
      sb.append("]");
      return sb.toString();
   }

   public static final String replace(String src, String before, String after) {
      if (src.indexOf(before) >= 0) {
         final StringBuffer sb = new StringBuffer(src);
         final int b = sb.indexOf(before);
         final int e = b + before.length();
         sb.replace(b, e, after);
         return sb.toString();
      }
      return src;
   }

   public static String wrapHTMLHeader(String name, String content, String css) {
      final String header = "<html>\n" + "<head>\n" + " <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n"
            + " <meta name=\"GENERATOR\" content=\"DTSA-II\">" + " <meta name=\"description\" content=\"" + name + "\">" + " <title>" + name
            + "</title>" + (css != null ? " <link href=\"" + css + "\" rel=\"stylesheet\" type=\"text/css\">\n" : "") + "</head>\n<body>\n";
      return header + content + "\n</body>\n</html>";
   }

   private static final String[][] mHTMLMap = {{"&amp;", "\u0026"}, {"&lt;", "\u003C"}, {"&gt;", "\u003E"}, {"&OElig;", "\u0152"},
         {"&oelig;", "\u0153"}, {"&Scaron;", "\u0160"}, {"&scaron;", "\u0161"}, {"&Yuml;", "\u0178"}, {"&circ;", "\u02C6"}, {"&tilde;", "\u02DC"},
         {"&ensp;", "\u2002"}, {"&emsp;", "\u2003"}, {"&thinsp;", "\u2009"}, {"&zwnj;", "\u200C"}, {"&zwj;", "\u200D"}, {"&lrm;", "\u200E"},
         {"&rlm;", "\u200F"}, {"&ndash;", "\u2013"}, {"&mdash;", "\u2014"}, {"&lsquo;", "\u2018"}, {"&rsquo;", "\u2019"}, {"&sbquo;", "\u201A"},
         {"&ldquo;", "\u201C"}, {"&rdquo;", "\u201D"}, {"&bdquo;", "\u201E"}, {"&dagger;", "\u2020"}, {"&Dagger;", "\u2021"}, {"&permil;", "\u2030"},
         {"&lsaquo;", "\u2039"}, {"&rsaquo;", "\u203A"}, {"&euro;", "\u20AC"}, {"&quot;", "\""}, {"&iexcl;", "\u00A1"}, {"&cent;", "\u00A2"},
         {"&pound;", "\u00A3"}, {"&curren;", "\u00A4"}, {"&yen;", "\u00A5"}, {"&brvbar;", "\u00A6"}, {"&sect;", "\u00A7"}, {"&uml;", "\u00A8"},
         {"&copy;", "\u00A9"}, {"&ordf;", "\u00AA"}, {"&laquo;", "\u00AB"}, {"&not;", "\u00AC"}, {"&shy;", "\u00AD"}, {"&reg;", "\u00AE"},
         {"&macr;", "\u00AF"}, {"&deg;", "00B0"}, {"&plusmn;", "\u00B1"}, {"&sup2;", "\u00B2"}, {"&sup3;", "\u00B3"}, {"&acute;", "\u00B4"},
         {"&micro;", "\u00B5"}, {"&para;", "\u00B6"}, {"&middot;", "\u00B7"}, {"&cedil;", "\u00B8"}, {"&sup1;", "\u00B9"}, {"&ordm;", "\u00BA"},
         {"&raquo;", "\u00BB"}, {"&frac14;", "\u00BC"}, {"&frac12;", "\u00BD"}, {"&frac34;", "\u00BE"}, {"&iquest;", "\u00BF"},
         {"&Agrave;", "\u00C0"}, {"&Aacute;", "\u00C1"}, {"&Acirc;", "\u00C2"}, {"&Atilde;", "\u00C3"}, {"&Auml;", "\u00C4"}, {"&Aring;", "\u00C5"},
         {"&AElig;", "\u00C6"}, {"&Ccedil;", "\u00C7"}, {"&Egrave;", "\u00C8"}, {"&Eacute;", "\u00C9"}, {"&Ecirc;", "\u00CA"}, {"&Euml;", "\u00CB"},
         {"&Igrave;", "\u00CC"}, {"&Iacute;", "\u00CD"}, {"&Icirc;", "\u00CE"}, {"&Iuml;", "\u00CF"}, {"&ETH;", "\u00D0"}, {"&Ntilde;", "\u00D1"},
         {"&Ograve;", "\u00D2"}, {"&Oacute;", "\u00D3"}, {"&Ocirc;", "\u00D4"}, {"&Otilde;", "\u00D5"}, {"&Ouml;", "\u00D6"}, {"&times;", "\u00D7"},
         {"&Oslash;", "\u00D8"}, {"&Ugrave;", "\u00D9"}, {"&Uacute;", "\u00DA"}, {"&Ucirc;", "\u00DB"}, {"&Uuml;", "\u00DC"}, {"&Yacute;", "\u00DD"},
         {"&THORN;", "\u00DE"}, {"&szlig;", "\u00DF"}, {"&agrave;", "\u00E0"}, {"&aacute;", "\u00E1"}, {"&acirc;", "\u00E2"}, {"&atilde;", "\u00E3"},
         {"&auml;", "\u00E4"}, {"&aring;", "\u00E5"}, {"&aelig;", "\u00E6"}, {"&ccedil;", "\u00E7"}, {"&egrave;", "\u00E8"}, {"&eacute;", "\u00E9"},
         {"&ecirc;", "\u00EA"}, {"&euml;", "\u00EB"}, {"&igrave;", "\u00EC"}, {"&iacute;", "\u00ED"}, {"&icirc;", "\u00EE"}, {"&iuml;", "\u00EF"},
         {"&eth;", "\u00F0"}, {"&ntilde;", "\u00F1"}, {"&ograve;", "\u00F2"}, {"&oacute;", "\u00F3"}, {"&ocirc;", "\u00F4"}, {"&otilde;", "\u00F5"},
         {"&ouml;", "\u00F6"}, {"&divide;", "\u00F7"}, {"&oslash;", "\u00F8"}, {"&ugrave;", "\u00F9"}, {"&uacute;", "\u00FA"}, {"&ucirc;", "\u00FB"},
         {"&uuml;", "\u00FC"}, {"&yacute;", "\u00FD"}, {"&thorn;", "\u00FE"}, {"&yuml;", "\u00FF"}, {"&fnof;", "\u0192"}, {"&Alpha;", "\u0391"},
         {"&Beta;", "\u0392"}, {"&Gamma;", "\u0393"}, {"&Delta;", "\u0394"}, {"&Epsilon;", "\u0395"}, {"&Zeta;", "\u0396"}, {"&Eta;", "\u0397"},
         {"&Theta;", "\u0398"}, {"&Iota;", "\u0399"}, {"&Kappa;", "\u039A"}, {"&Lambda;", "\u039B"}, {"&Mu;", "\u039C"}, {"&Nu;", "\u039D"},
         {"&Xi;", "\u039E"}, {"&Omicron;", "\u039F"}, {"&Pi;", "\u03A0"}, {"&Rho;", "\u03A1"}, {"&Sigma;", "\u03A3"}, {"&Tau;", "\u03A4"},
         {"&Upsilon;", "\u03A5"}, {"&Phi;", "\u03A6"}, {"&Chi;", "\u03A7"}, {"&Psi;", "\u03A8"}, {"&Omega;", "\u03A9"}, {"&alpha;", "\u03B1"},
         {"&beta;", "\u03B2"}, {"&gamma;", "\u03B3"}, {"&delta;", "\u03B4"}, {"&epsilon;", "\u03B5"}, {"&zeta;", "\u03B6"}, {"&eta;", "\u03B7"},
         {"&theta;", "\u03B8"}, {"&iota;", "\u03B9"}, {"&kappa;", "\u03BA"}, {"&lambda;", "\u03BB"}, {"&mu;", "\u03BC"}, {"&nu;", "\u03BD"},
         {"&xi;", "\u03BE"}, {"&omicron;", "\u03BF"}, {"&pi;", "\u03C0"}, {"&rho;", "\u03C1"}, {"&sigmaf;", "\u03C2"}, {"&sigma;", "\u03C3"},
         {"&tau;", "\u03C4"}, {"&upsilon;", "\u03C5"}, {"&phi;", "\u03C6"}, {"&chi;", "\u03C7"}, {"&psi;", "\u03C8"}, {"&omega;", "\u03C9"},
         {"&thetasym;", "\u03D1"}, {"&upsih;", "\u03D2"}, {"&piv;", "\u03D6"}, {"&bull;", "\u2022"}, {"&hellip;", "\u2026"}, {"&prime;", "\u2032"},
         {"&Prime;", "\u2033"}, {"&oline;", "\u203E"}, {"&frasl;", "\u2044"}, {"&weierp;", "\u2118"}, {"&image;", "\u2111"}, {"&real;", "\u211C"},
         {"&trade;", "\u2122"}, {"&alefsym;", "\u2135"}, {"&larr;", "\u2190"}, {"&uarr;", "\u2191"}, {"&rarr;", "\u2192"}, {"&darr;", "\u2193"},
         {"&harr;", "\u2194"}, {"&crarr;", "\u21B5"}, {"&lArr;", "\u21D0"}, {"&uArr;", "\u21D1"}, {"&rArr;", "\u21D2"}, {"&dArr;", "\u21D3"},
         {"&hArr;", "\u21D4"}, {"&forall;", "\u2200"}, {"&part;", "\u2202 "}, {"&exist;", "\u2203"}, {"&empty;", "\u2205"}, {"&nabla;", "\u2207"},
         {"&isin;", "\u2208"}, {"&notin;", "\u2209"}, {"&ni;", "\u220B"}, {"&prod;", "\u220F"}, {"&sum;", "\u2211"}, {"&minus;", "\u2212"},
         {"&lowast;", "\u2217"}, {"&radic;", "\u221A"}, {"&prop;", "\u221D"}, {"&infin;", "\u221E"}, {"&ang;", "\u2220"}, {"&and;", "\u2227"},
         {"&or;", "\u2228"}, {"&cap;", "\u2229"}, {"&cup;", "\u222A"}, {"&int;", "\u222B"}, {"&there4;", "\u2234"}, {"&sim;", "\u223C"},
         {"&cong;", "\u2245"}, {"&asymp;", "\u2248"}, {"&ne;", "\u2260"}, {"&equiv;", "\u2261"}, {"&le;", "\u2264"}, {"&ge;", "\u2265"},
         {"&sub;", "\u2282"}, {"&sup;", "\u2283"}, {"&nsub;", "\u2284"}, {"&sube;", "\u2286"}, {"&supe;", "\u2287"}, {"&oplus;", "\u2295"},
         {"&otimes;", "\u2297"}, {"&perp;", "\u22A5"}, {"&sdot;", "\u22C5"}, {"&lceil;", "\u2308"}, {"&rceil;", "\u2309"}, {"&lfloor;", "\u230A"},
         {"&rfloor;", "\u230B"}, {"&lang;", "\u2329"}, {"&rang;", "\u232A"}, {"&loz;", "\u25CA"}, {"&spades;", "\u2660"}, {"&clubs;", "\u2663"},
         {"&hearts;", "\u2665"}, {"&diams;", "\u2666"}};

   /**
    * Replaces all UNICODE characters with HTML character entity encodings with
    * the HTML character entity encoding. Use this function on the text portion
    * not on HTML containing tags.
    * 
    * @param html
    * @return String
    */
   public static final String normalizeHTML(String html) {
      final StringBuffer sb = new StringBuffer(html);
      for (final String[] element : mHTMLMap)
         for (int pos = sb.indexOf(element[1], 0); pos != -1; pos = sb.indexOf(element[1], pos + 1))
            sb.replace(pos, pos + 1, element[0]);
      return sb.toString();
   }

   /**
    * A simple method for removing egregous characters from strings.
    * 
    * @param str
    * @return String
    */
   public static String normalizeString(String str) {
      final StringBuffer sb = new StringBuffer(str);
      for (int i = sb.length() - 1; i >= 0; --i)
         switch (sb.charAt(i)) {
            case '\"' :
               sb.replace(i, i + 1, "&#34;");
               break;
            default :
               break;
         }
      return sb.toString();
   }

   public static String normalizeFilename(String name) {
      final char[] forbidden = {'<', '>', ':', '"', '/', '\\', '|', '?', '*'};
      for (char c = 1; c <= 32; c++)
         if (name.indexOf(c) != -1)
            name = name.replace(c, '_');
      for (char c : forbidden)
         if (name.indexOf(c) != -1)
            name = name.replace(c, '_');
      return name;
   }

   public static String toScientific(String str) {
      if (str.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) {
         final char[] superscripts = {'\u2070', '\u00B9', '\u00B2', '\u00B3', '\u2074', '\u2075', '\u2076', '\u2077', '\u2078', '\u2079',};
         final char minus = '\u207B';
         final StringBuffer sb = new StringBuffer();
         str = str.toLowerCase();
         final int ei = str.indexOf('e');
         assert ei != -1;
         sb.append(str.substring(0, ei));
         sb.append(" \u00D7 10");
         for (int i = ei + 1; i < str.length(); ++i) {
            final char c = str.charAt(i);
            if (c == '-')
               sb.append(minus);
            else if (Character.isDigit(c))
               sb.append(superscripts[Character.getNumericValue(c)]);
            else
               sb.append(c);
         }
         return sb.toString();
      } else
         return str;
   }

   public static String toScientificHTML(String str) {
      if (str.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) {
         final StringBuffer sb = new StringBuffer();
         str = str.toLowerCase();
         final int ei = str.indexOf('e');
         sb.append(str.substring(0, ei));
         sb.append(" &#215; 10<sup>");
         sb.append(str.substring(ei + 1));
         sb.append("</sup>");
         return sb.toString();
      } else
         return str;
   }

   public static <T> String toList(Collection<T> items) {
      final StringBuffer sb = new StringBuffer();
      final int sz = items.size();
      int i = 0;
      for (final T item : items) {
         if (i > 0)
            sb.append(i == (sz - 1) ? " & " : ", ");
         sb.append(item.toString());
         ++i;
      }
      return sb.toString();
   }

}

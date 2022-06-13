package org.basex.query.ft;

import org.basex.query.*;

/**
 * Full-text document and queries.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
abstract class FTData extends QueryTest {
  /** Test document. */
  protected static final String DOC =
      "<fttest>\n" +
      "  <co>\n" +
      "     <w>xml in the first sentence. second sentence. " +
      "third sentence. fourth sentence. fifth sentence.</w>\n" +
      "     <w>XML xml XmL</w>\n" +
      "     <w>we have xml databases</w>\n" +
      "     <w>XML DATABASES</w>\n" +
      "     <w>XML &amp; Databases</w>\n" +
      "  </co>\n" +
      "  <wc>\n" +
      "     <w>hello</w>\n" +
      "  </wc>\n" +
      "  <sc>\n" +
      "     <s>di\u00e4t-joghurt</s>\n" +
      "     <s>diat-joghurt</s>\n" +
      "  </sc>\n" +
      "  <at><b>B</b>ad one</at>\n" +
      "  <fti>adfas wordt. ook wel eens</fti>" +
      "  <fti>wordt ook wel een s</fti>" +
      "  <fti>adfad. wordt\nook wel.eens a</fti>" +
      "  <fti>adfad wordt. ook\nwel een s adf</fti>" +
      "  <fti>adfad wordt ook. wel een s</fti>" +
      "  <atr key='value'/>" +
      "  <w>the fifth sentence. fourth sentence. " +
      "third sentence. second sentence. first sentence.</w>\n" +
      "  <wld/>\n" +
      "  <wld>yeah</wld>\n" +
      "  <mix>A<sub/>B</mix>\n" +
      "  <mix>B<sub/>A</mix>\n" +
      "  <order>A B A</order>\n" +
      "</fttest>";

  static { create(DOC); }

  /** Test queries. */
  protected static final Object[][] QUERIES = {
      { "Simple 1", booleans(true), "'a' contains text 'a'" },
      { "Simple 2", booleans(true), "'a b' contains text 'b'" },
      { "Simple 3", booleans(false), "'abc' contains text 'b'" },
      { "Simple 4", nodes(22), "//b['true' contains text 'true']" },
      { "Simple 5", booleans(true), "//@key contains text 'value'" },
      { "Simple 6", strings("value"), "//@key[. contains text 'value']/string()" },
      { "Simple 7", booleans(false), "//@key contains text 'values'" },
      { "Simple 8", booleans(true), "number('100') + 23 contains text '123'" },
      { "Simple 9", booleans(true), "true() contains text 'true'" },
      { "Simple 10", booleans(true), "false() contains text 'false'" },
      { "Simple 11", booleans(false), "'text' contains text ''" },
      { "Simple 12", booleans(false), "'t' contains text ftnot { 't' } distance at most 0 words" },

      { "FT 1", nodes(14), "//w[text() contains text 'HELLO']" },
      { "FT 2", nodes(14), "//w[text() contains text 'hello']" },
      { "FT 3", nodes(14), "//w[text() contains text '    hello!...   ']" },
      { "FT 4", empty(), "//w[  text  (   )  contains text  '  anarmophism  '  ]  " },
      { "FT 5", empty(), "//w[text() contains text 'db']" },
      { "FT 6", nodes(42, 46), "//mix[text() contains text 'A']" },
      { "FT 7", nodes(14), "//w[text() contains text 'hello']['A' contains text 'A']" },
      { "FT 8", nodes(14), "//w[text() contains text 'hello']['X' contains text 'X' using fuzzy]" },
      { "FT 9", nodes(14), "//w[text() = 'hello' and 'X' contains text 'X']" },
      { "FT 10", nodes(14), "//w[text() = 'hello' and text() contains text 'hello']" },
      { "FT 11", empty(), "//wld[text() contains text '']" },
      { "FT 12", empty(), "//wld[text() contains text ' ']" },
      { "FT 13", nodes(40), "//*[text() contains text 'yeah']" },

      { "Preds 1", nodes(7, 9, 11),
        "//w[text() contains text 'xml'][text() contains text 'Databases']" },
      { "Preds 2", nodes(35),
        "//fttest[co/w contains text 'xml'][w contains text 'fifth']/atr" },
      { "Preds 3", nodes(1),
        "//fttest[*/text() contains text 'ook'][*/text() contains text 'een']" },
      { "Preds 4", nodes(1),
        "*[*/text() contains text 'ook'][*/text() contains text 'een']" },
      { "Preds 5", nodes(7),
        "//*[text() contains text 'have'][text() contains text 'xml']" },
      { "Preds 6", nodes(13),
        "//*[*/text() contains text 'hello'][*/text() = 'hello']" },
      { "Preds 7", nodes(7),
        "//w[text()[. contains text 'have xml'] contains text 'Databases']" },
      { "Preds 8", nodes(5),
        "/descendant::w[text() contains text 'xml'][2]" },
      { "Preds 9", nodes(3),
        "//w[text() contains text 'xml'][1]" },
      { "Preds 10", nodes(5),
        "//w[text() contains text 'xml'][2]" },
      { "Preds 11", nodes(14),
        "//wc/w[text() contains text 'hello']" },
      { "Preds 12", nodes(46),
        "//mix[text()[1] contains text 'B']" },

      { "AndOr 1", nodes(7, 9, 11),
        "//w[text() contains text 'xml' and text() contains text 'databases']" },
      { "AndOr 2", nodes(2),
        "//*[*/text() contains text 'have' and */text() contains text 'first']" },
      { "AndOr 3", nodes(25, 29),
        "//fti[text() contains text 'eens' or text() contains text 'a']" },
      { "AndOr 4", nodes(25, 29),
        "//fti[text() contains text 'eens' and text() contains text 'ook' or " +
        "text() contains text 'a']" },
      { "AndOr 5", nodes(25, 29),
        "//fti[text() contains text 'ook' and text() contains text 'eens' or " +
        "text() contains text 'a']" },
      { "AndOr 6", nodes(31),
        "//fti[text() contains text 'adf s' or text() contains text 's adf']" },
      { "AndOr 7", nodes(31),
        "//fti[contains(text(), 'adf') and text() contains text 'adf']" },
      { "AndOr 8", nodes(3),
        "//*[text() contains text 'sentence' and text() contains text 'xml']" },
      { "AndOr 9", nodes(42, 46),
        "//mix[text() contains text 'A'][text() contains text 'B']" },

      { "Phrase 1", nodes(7, 9, 11),
        "//w[text() contains text 'xml databases']" },
      { "Phrase 2", nodes(7, 9, 11),
        "//w[text() contains text 'xml &amp; databases']" },
      { "Phrase 3", nodes(7, 9, 11),
        "//w[text() contains text 'xml :) databases :|']" },
      { "Phrase 4", empty(),
        "//w[text() contains text 'xml db']" },
      { "Phrase 5", nodes(25, 29),
        "/fttest/fti[text() contains text 'wordt ook wel eens']" },

      { "FTDiacritics 1", nodes(17, 19),
        "//s[text() contains text 'diat']" },
      { "FTDiacritics 2", nodes(17, 19),
        "//s[text() contains text 'di\u00e4t joghurt' using diacritics insensitive]" },
      { "FTDiacritics 3", nodes(17),
        "//s[text() contains text 'di\u00e4t joghurt' using diacritics sensitive]" },

      { "FTDiacritics 4", booleans(true),
        "'\u0065\u0301\u00E9' contains text 'ee' using diacritics insensitive" },

      { "FTCaseOption 1", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml']" },
      { "FTCaseOption 2", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml' using case insensitive]" },
      { "FTCaseOption 3", nodes(11),
        "/fttest/co/w[text() contains text 'XML Databases' using case sensitive]" },
      { "FTCaseOption 4", nodes(9),
        "/fttest/co/w[text() contains text 'xml databases' using uppercase]" },
      { "FTCaseOption 5", nodes(3, 5, 7),
        "/fttest/co/w[text() contains text 'XML' using lowercase]" },
      { "FTCaseOption 6", nodes(5, 9, 11),
        "/fttest/co/w[text() contains text 'xml' using uppercase]" },
      { "FTCaseOption 7", nodes(7),
        "/fttest/co/w[text() contains text 'XML DATABASES' using lowercase]" },

      { "FTWildCard 1", nodes(14),
        "/fttest/wc/w[text() contains text '.ello' using wildcards]" },
      { "FTWildCard 2", nodes(14),
        "/fttest/wc/w[text() contains text 'hell.' using wildcards]" },
      { "FTWildCard 3", nodes(14),
        "/fttest/wc/w[text() contains text '.+llo' using wildcards]" },
      { "FTWildCard 4", nodes(14),
        "/fttest/wc/w[text() contains text 'hell.+' using wildcards]" },
      { "FTWildCard 5", nodes(14),
        "/fttest/wc/w[text() contains text '.*llo' using wildcards]" },
      { "FTWildCard 6", nodes(14),
        "/fttest/wc/w[text() contains text 'hel.*' using wildcards]" },
      { "FTWildCard 7", nodes(14),
        "/fttest/wc/w[text() contains text '.*' using wildcards]" },
      { "FTWildCard 8", nodes(14),
        "/fttest/wc/w[text() contains text '.+' using wildcards]" },
      { "FTWildCard 9", nodes(5, 9, 11),
        "/fttest/co/w[text() contains text 'X.+' using wildcards using case sensitive]" },
      { "FTWildCard 10", nodes(5, 9, 11),
        "/fttest/co/w[text() contains text 'x.+' using wildcards using uppercase]" },
      { "FTWildCard 11", nodes(40),
        "/fttest/wld[text() contains text '.*' using wildcards]" },
      { "FTWildCard 12", nodes(40),
        "/fttest/wld[text() contains text '.+' using wildcards]" },
      { "FTWildCard 13", nodes(14),
        "//w[text() contains text 'he.{1,2}o' using wildcards]" },
      { "FTWildCard 14", nodes(14),
        "//w[text() contains text 'h.+ll.+' using wildcards]" },
      { "FTWildCard 15", nodes(14),
        "//w[text() contains text 'h.\\llo' using wildcards]" },

      { "FTWildCard 16", booleans(false), "'a' contains text 'a.+' using wildcards" },
      { "FTWildCard 17", booleans(true), "'aa' contains text 'a.+' using wildcards" },
      { "FTWildCard 18", booleans(true), "'aaaa' contains text 'a.+' using wildcards" },
      { "FTWildCard 19", booleans(true), "'a' contains text 'a.*' using wildcards" },
      { "FTWildCard 20", booleans(true), "'aaaaaa' contains text 'a.*' using wildcards" },
      { "FTWildCard 21", booleans(false), "'a' contains text 'a.' using wildcards" },
      { "FTWildCard 22", booleans(true), "'aa' contains text 'a.' using wildcards" },
      { "FTWildCard 23", booleans(true), "'a' contains text 'a.{0,1}' using wildcards" },
      { "FTWildCard 24", booleans(true), "'aa' contains text 'a.{0,1}' using wildcards" },
      // parsing tests: should throw exception
      { "FTWildCard 25", "'aa' contains text 'a.{0, 1}' using wildcards" },
      { "FTWildCard 26", "'a' contains text 'a.{-1,1}' using wildcards" },
      { "FTWildCard 27", "'a' contains text 'a.{1}' using wildcards" },
      { "FTWildCard 28", "'a' contains text 'a.{1-5}' using wildcards" },
      { "FTWildCard 29", booleans(true), "'hi' contains text '\\h\\i' using wildcards" },
      // #660: combination of FTAnyAllOption and wildcards
      { "FTWildCard 30", booleans(true),
        "'a' contains text '.*' all words using wildcards" },

      { "FTFuzzy 1", nodes(7, 9, 11), "//*[text() contains text 'Database' using fuzzy]" },
      { "FTFuzzy 2", nodes(7, 9, 11), "//*[text() contains text 'Databaze' using fuzzy]" },
      { "FTFuzzy 3", empty(), "//*[text() contains text 'Databasing' using fuzzy]" },

      { "FTAnyAllOption 1", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml' any]" },
      { "FTAnyAllOption 2", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml' all]" },
      { "FTAnyAllOption 3", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml' any word]" },
      { "FTAnyAllOption 4", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml' all words]" },
      { "FTAnyAllOption 5", nodes(3, 5, 7, 9, 11),
        "/fttest/co/w[text() contains text 'xml' phrase]" },
      { "FTAnyAllOption 6", booleans(false),
        "'text' contains text { '' } any" },
      { "FTAnyAllOption 7", booleans(false),
        "'text' contains text { '' } all" },
      { "FTAnyAllOption 8", booleans(false),
        "'text' contains text { '' } all words" },
      { "FTAnyAllOption 9", booleans(false),
        "'text' contains text { '' } any word" },
      { "FTAnyAllOption 10", booleans(false),
        "'text' contains text { '' } phrase" },
      { "FTAnyAllOption 11", booleans(true),
        "'red balloon' contains text { 'red', '', 'balloon' } any" },
      { "FTAnyAllOption 12", booleans(false),
        "'red balloon' contains text { 'red', '', 'balloon' } all" },
      { "FTAnyAllOption 13", booleans(true),
        "'red balloon' contains text { 'red', '', 'balloon' } all words" },
      { "FTAnyAllOption 14", booleans(true),
        "'red balloon' contains text { 'red', '', 'balloon' } any word" },
      { "FTAnyAllOption 15", booleans(true),
        "'red balloon' contains text { 'red', '', 'balloon' } phrase" },

      { "FTTimes 1", nodes(7, 9, 11),
        "//w[text() contains text 'DaTaBaSeS' occurs exactly 1 times]" },
      { "FTTimes 2", nodes(5),
        "//w[text() contains text 'XmL' occurs exactly 3 times]" },
      { "FTTimes 3", nodes(14, 37),
        "//w[text() contains text 'xml' occurs exactly 0 times]" },
      { "FTTimes 4", nodes(5),
        "//w[text() contains text 'xml' occurs at least 3 times]" },
      { "FTTimes 5", nodes(3, 7, 9, 11, 14, 37),
        "//w[text() contains text 'XmL' occurs at most 2 times]" },
      { "FTTimes 6", nodes(3, 7, 9, 11, 14, 37),
        "//w[text() contains text 'XmL' occurs from 0 to 1 times]" },
      { "FTTimes 7", nodes(5),
        "//w[text() contains text 'xml xml' occurs at least 2 times]" },
      { "FTTimes 8", empty(),
        "//w[text() contains text 'xml xml' occurs at least 4 times]" },

      { "FTAnyAllTimes 1", booleans(true),
        "'a a a' contains text 'a a' occurs exactly 2 times" },
      { "FTAnyAllTimes 2", booleans(true),
        "'a a a' contains text 'a a' any occurs exactly 2 times" },
      { "FTAnyAllTimes 3", booleans(true),
        "'a a a' contains text 'a a' any word occurs exactly 3 times" },
      { "FTAnyAllTimes 4", booleans(true),
        "'a a a' contains text 'a a' all occurs exactly 2 times" },
      { "FTAnyAllTimes 5", booleans(true),
        "'a a a' contains text 'a a' all words occurs exactly 3 times" },
      { "FTAnyAllTimes 6", booleans(true),
        "'a a a' contains text 'a a' phrase occurs exactly 2 times" },
      { "FTAnyAllTimes 7", booleans(true),
        "'a b c' contains text '.' occurs exactly 3 times using wildcards" },
      { "FTAnyAllTimes 8", booleans(true),
        "'a b c' contains text '. .' occurs exactly 2 times using wildcards" },

      { "FTAndOr 1", nodes(7, 9, 11),
        "//w[text() contains text 'XmL' ftand 'Databases']" },
      { "FTAndOr 2", nodes(7, 9, 11, 14),
        "//w[text() contains text 'databases' ftor 'hello']" },
      { "FTAndOr 3", nodes(14),
        "//w[text() contains text 'HELLO' ftand 'hello']" },
      { "FTAndOr 4", nodes(7, 9, 11, 14),
        "//w[text() contains text 'xml' ftand 'databases' ftor 'hello' ]" },
      { "FTAndOr 5", nodes(7, 9, 11),
        "//w[text() contains text 'databases' ftand ('xml' ftor 'hello')]" },
      { "FTAndOr 6", nodes(31, 33),
        "//fti[text() contains text 'adfad' ftand 'wordt' ftand 'ook' " +
        "ftand 'wel' ftand 'een' ftand 's']" },
      { "FTAndOr 7", empty(),
        "//*[text() contains text 'databases' ftand 'db']" },
      { "FTAndOr 8", nodes(14),
        "//*[text() contains text 'hola' ftor 'hello']" },
      { "FTAndOr 9", empty(),
        "//*[text() contains text 'hola' ftand 'hello']" },
      { "FTAndOr 10", nodes(14),
        "//w[text() contains text 'HELLO' ftand ('hello' using stemming)]" },
      { "FTAndOr 11", nodes(14),
        "//w[text() contains text 'HELLO' ftand ('hello') using stemming]" },

      { "FTStemming 1", nodes(7, 9, 11),
        "//w[text() contains text 'xml database' using stemming]" },
      { "FTStemming 2", empty(),
        "//w[text() contains text 'xml database' using no stemming]" },
      { "FTStemming 3", nodes(7, 9, 11),
        "//w[text() contains text 'xml' ftand 'databasing' using stemming " +
        "using language 'en']" },
      { "FTStemming 4", nodes(7, 9, 11, 14),
        "//w[text() contains text 'hello' ftor 'database' using stemming]" },
      { "FTStemming 5", nodes(3, 5, 14, 37),
        "//w[text() contains text ftnot 'database' using stemming]" },
      { "FTStemming 6", booleans(true),
        "'base' contains text 'bases' using stemming" },
      { "FTStemming 7", booleans(true),
        "'bases' contains text ('base') using stemming" },
      { "FTStemming 8", booleans(true),
        "'base' contains text ('bases') using stemming" },
      { "FTStemming 9", booleans(true),
        "'base' contains text ('bases' using stemming) using no stemming" },
      { "FTStemming 10", booleans(true),
        "'\u043a\u043d\u0438\u0433\u0430' contains text '\u043a\u043d\u0438\u0433\u0438'" +
        " using stemming using language 'Russian'" },
      { "FTStemming 11", booleans(true),
        "'de' contains text 'de' using stemming using language 'pt'" },
      { "FTStemming 12", booleans(true),
        "'mice' contains text 'mouse' using stemming" },
      { "FTStemming 13", booleans(true),
        "'symposia' contains text 'symposium' using stemming" },
      { "FTStemming 14", booleans(true),
        "'men' contains text 'man' using stemming" },

      { "FTLanguage 1", nodes(14),
        "//*[text() contains text 'hello' using language 'en']" },
      { "FTLanguage 2", nodes(14),
        "//*[text() contains text 'hello' using language 'de']" },
      { "FTLanguage 3", // error
        "//*[text() contains text 'hello' using language 'jp']" },

      { "FTStopWords 1", nodes(7, 9, 11), "//*[text() contains text " +
        "'and databases' using stop words ('xml', 'and')]" },
      { "FTStopWords 2", nodes(7),
        "//*[text() contains text 'we use xml' using stop words ('use')]" },

      { "FTAtomization 1", nodes(21),
        "//at[. contains text 'bad one']" },
      { "FTAtomization 2", nodes(35),
        "//atr[@key contains text 'value']" },

      { "FTOrdered 1", nodes(7, 9, 11),
        "//w[. contains text 'databases' ordered]" },
      { "FTOrdered 2", nodes(7, 9, 11),
        "//w[. contains text 'xml' ftand 'databases' ordered]" },
      { "FTOrdered 3", empty(),
        "//w[. contains text 'databases' ftand 'xml' ordered]" },
      { "FTOrdered 4", booleans(true),
        "'A B' contains text ('A' ftand 'B' ordered)" },
      { "FTOrdered 5", booleans(true),
        "'A B' contains text ('A' ftand 'B') ftor ('C' ftand 'D') ordered" },
      { "FTOrdered 6", booleans(true),
        "'C D' contains text ('A' ftand 'B') ftor ('C' ftand 'D') ordered" },
      { "FTOrdered 7", booleans(true),
        "'A B C D' contains text ('B' ftand 'A' ordered) ftor " +
        "('C' ftand 'D' ordered) ordered" },
      { "FTOrdered 8", booleans(false),
        "'B A' contains text ('A' ftand ftnot 'B') ordered" },
      { "FTOrdered 9", booleans(true),
        "'A B' contains text 'B' ftor 'A' ordered" },
      { "FTOrdered 10", booleans(true),
        "'A B' contains text ('B' ftor 'A') ordered" },
      { "FTOrdered 11", booleans(true),
        "'A B C' contains text ('A' ftor 'C') ftand 'B' ordered" },
      { "FTOrdered 12", booleans(true),
        "//order contains text { 'A', 'B' } all ordered" },
      { "FTOrdered 13", booleans(true),
        "//order contains text { 'B', 'A' } all ordered" },
      { "FTOrdered 14", booleans(true),
        "//order contains text 'A B' all words ordered" },
      { "FTOrdered 15", booleans(true),
        "//order contains text 'B A' all words ordered" },

      { "FTDistance 1", nodes(3),
        "//w[text() contains text 'the' ftand 'fourth' " +
        "distance exactly 2 sentences]" },
      { "FTDistance 2", nodes(3, 37),
        "//w[. contains text 'first' ftand 'second' ftand 'third' " +
          "distance exactly 1 words]" },
      { "FTDistance 3", nodes(3, 37),
        "//w[. contains text 'first sentence' ftand 'third sentence' " +
        "distance exactly 2 words]" },
      { "FTDistance 4", nodes(3),
        "//w[. contains text 'the first sentence' ftand 'third sentence' " +
        "distance exactly 2 words]" },
      { "FTDistance 5", nodes(3, 37),
        "//w[. contains text 'sentence' ftand 'the' " +
        "distance exactly 1 words]" },
      { "FTDistance 6", nodes(3, 37),
        "//w[. contains text ('second' ftand 'third' window 3 words) " +
        "ftand 'sentence' distance exactly 0 words]" },
      { "FTDistance 7", nodes(3, 37),
        "//w[text() contains text ('second' ftand 'third' window 3 words) " +
        "ftand 'sentence' distance exactly 0 words ordered]" },
      { "FTDistance 8", nodes(37),
        "//w[. contains text 'third' ftand 'second' " +
        " ftand 'first' distance exactly 1 words ordered]" },
      { "FTDistance 9", nodes(3),
        "//w[. contains text 'first' ftand 'second' " +
        " ftand 'third' distance exactly 1 words ordered]" },
      { "FTDistance 10", booleans(true),
        "'a b' contains text 'a' ftand ('b') distance exactly 0 words" },
      { "FTDistance 11", booleans(true),
        "'a b' contains text ('a') ftand ('b') entire content" },

      { "FTWindow 1", nodes(3, 37),
        "//w[. contains text 'second' ftand 'fifth' window 7 words]" },
      { "FTWindow 2", nodes(3, 37),
        "//w[. contains text 'second sentence' ftand 'fifth sentence' " +
        "window 8 words]" },
      { "FTWindow 3", nodes(3, 37),
        "//w[. contains text 'third' ftand 'second' " +
        "ftand 'fifth' window 7 words]" },
      { "FTWindow 4", nodes(37),
        "//w[. contains text 'fifth' ftand 'third' " +
        "ftand 'second' ordered window 7 words]" },
      { "FTWindow 5", nodes(37),
        "//w[. contains text 'fifth' ftand 'third' " +
        "ftand 'second' window 7 words ordered]" },

      { "FTScope 1", nodes(27, 29, 33),
        "//fti[. contains text 'wordt ook' same sentence]" },
      { "FTScope 2", nodes(27, 29, 33),
        "//fti[text() contains text 'wordt' ftand 'ook' same sentence]" },
      { "FTScope 3", nodes(25, 31),
        "//fti[. contains text 'wordt' ftand 'ook' different sentence]" },
      { "FTScope 4", nodes(25, 27, 29, 33),
        "//fti[. contains text 'ook' ftand 'wel' same paragraph]" },
      { "FTScope 5", nodes(31),
        "//fti[. contains text 'ook' ftand 'wel' different paragraph]" },
      { "FTScope 6", booleans(true),
        "'a. a b' contains text ('a' ftand 'b') different sentence" },

      { "FTContent 1", nodes(3, 5, 9, 11),
        "//w[text() contains text 'xml' at start]" },
      { "FTContent 2", empty(),
        "//w[. contains text 'databases' at start]" },
      { "FTContent 3", nodes(9, 11),
        "//w[. contains text 'xml databases' at start]" },
      { "FTContent 4", nodes(9, 11),
        "//w[. contains text 'xml' ftand 'databases' ordered at start]" },
      { "FTContent 5", nodes(7, 9, 11),
        "//w[. contains text 'databases' at end]" },
      { "FTContent 6", nodes(7, 9, 11),
        "//w[. contains text 'xml databases' at end]" },
      { "FTContent 7", nodes(7, 9, 11),
        "//w[. contains text 'xml' ftand 'databases' at end]" },
      { "FTContent 8", empty(),
        "//w[. contains text 'have xml' at end]" },
      { "FTContent 9", nodes(14),
        "//w[text() contains text 'hello' entire content]" },
      { "FTContent 10", nodes(9, 11),
        "//w[. contains text 'xml databases' entire content]" },
      { "FTContent 11", booleans(true),
        "'a b c d' contains text 'a' ftand 'b' ftand 'c'" +
        " ftand 'd' entire content" },
      { "FTContent 12", booleans(true),
        "'a b c d' contains text 'd' ftand 'c' ftand 'b'" +
        " ftand 'a' entire content" },
      { "FTContent 13", booleans(true),
        "'a b c d' contains text 'a' ftand 'b' ftand 'c'" +
        " ftand 'd' entire content ordered" },
      { "FTContent 14", booleans(false),
        "'a b c d' contains text 'd' ftand 'c' ftand 'b'" +
        " ftand 'a' entire content ordered" },
      { "FTContent 15", booleans(true),
        "'a b c d' contains text 'a' ftand 'b' at start" },
      { "FTContent 16", booleans(true),
        "'a b c d' contains text 'a' ftand 'b' at start ordered" },
      { "FTContent 17", booleans(true),
        "'a b c d' contains text 'b' ftand 'a' at start" },
      { "FTContent 18", booleans(false),
        "'a b c d' contains text 'b' ftand 'a' at start ordered" },
      { "FTContent 19", booleans(true),
        "'a b c d' contains text 'c' ftand 'd' at end" },
      { "FTContent 20", booleans(true),
        "'a b c d' contains text 'c' ftand 'd' at end ordered" },
      { "FTContent 21", booleans(true),
        "'a b c d' contains text 'd' ftand 'c' at end" },
      { "FTContent 22", booleans(false),
        "'a b c d' contains text 'd' ftand 'c' at end ordered" },
      { "FTContent 23", booleans(true),
        "'a b c' contains text 'b c' ftand 'a' entire content" },
      { "FTContent 24", booleans(true),
        "'a b c' contains text 'a' ftand 'b c' entire content" },
      { "FTContent 25", booleans(true),
        "'a b c' contains text 'a b c' entire content" },
      { "FTContent 26", booleans(false),
        "'a b' contains text 'a' entire content" },
      { "FTContent 27", booleans(false),
        "'a b' contains text 'b' entire content" },

      { "FTMildNot 1", nodes(3, 5),
        "//w[text() contains text 'xml' not in 'xml databases']" },
      { "FTMildNot 2", nodes(14),
        "//w[text() contains text 'hello' not in 'xml']" },
      { "FTMildNot 3", booleans(false),
        "'a b' contains text 'a' not in 'a b'" },
      { "FTMildNot 4", booleans(false),
        "'a' contains text 'a' not in 'a'" },
      { "FTMildNot 5", booleans(true),
        "'a b' contains text 'a b' not in 'a'" },
      { "FTMildNot 6", booleans(true),
        "'a b a' contains text 'a' not in 'a b'" },
      { "FTMildNot 7", booleans(false),
        "'a b a b' contains text 'a' not in 'a b'" },
      { "FTMildNot 8",
        "'a' contains text 'a' not in ftnot 'a'" },
      { "FTMildNot 9", nodes(3, 5, 7, 9, 11),
        "//w[text() contains text 'xml' not in 'we have']" },

      { "FTUnaryNot 1", nodes(14, 37),
        "//w[text() contains text ftnot 'xml']" },
      { "FTUnaryNot 2", nodes(3, 5),
        "//w[text() contains text 'xml' ftand ftnot 'databases']" },
      { "FTUnaryNot 3", nodes(3, 5),
        "//w[text() contains text ftnot 'databases' ftand 'xml']" },
      { "FTUnaryNot 4", nodes(31),
        "//*[text() contains text 'adf' ftand ftnot 'xml']" },
      { "FTUnaryNot 5", nodes(3, 5, 9, 11),
        "//w[text() contains text 'xml' ftand ftnot 'databases' " +
        "using case sensitive]" },
      { "FTUnaryNot 6", nodes(7, 9, 11, 14, 37),
        "//w[text() contains text 'databases' ftor ftnot 'xml']" },
      { "FTUnaryNot 7", nodes(3, 5, 14, 37),
        "//w[text() contains text 'hello' ftor ftnot 'databases']" },
      { "FTUnaryNot 8", nodes(3, 5, 7, 9, 11, 14, 37),
        "//w[text() contains text ftnot 'bier']" },
      { "FTUnaryNot 9", nodes(3, 5, 7, 9, 11, 14, 37),
        "//w[text() contains text ftnot 'bier' ftand ftnot 'wein' ]" },
      { "FTUnaryNot 10", nodes(3, 5, 7, 9, 11, 14, 37),
        "//w[text() contains text ftnot 'bier' ftor ftnot 'wein' ]" },
      { "FTUnaryNot 11", nodes(14, 37),
        "//w[text() contains text ftnot 'xml' ftand ftnot 'databeses' ]" },
      { "FTUnaryNot 12", nodes(31),
        "//fti[text() contains text ftnot (ftnot 'adf') ]" },
      { "FTUnaryNot 13", nodes(31),
        "//fti[text() contains text 'adf' ftand ftnot (ftnot 'adf')]" },
      { "FTUnaryNot 14", nodes(31),
        "//fti[text() contains text 'adf' ftor ftnot (ftnot 'adf')]" },
      { "FTUnaryNot 15", nodes(25, 27, 29, 31, 33),
        "//fti[text() contains text 'adf' ftor ftnot 'adf']" },
    };
}

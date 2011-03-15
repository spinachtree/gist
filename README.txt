
title	Gist in Java

author	Peter Cashin

date	2011-03-15

This is a Java version of Gist, a PBNF grammar transform parser.

The doc directory contains general Gist information. For an introduction first see: GrammarsForProgrammers.

The examples directory contains some PBNF grammar-transform examples. These examples can be executed from a shell console, or the example.rb ruby script.

The package: org.spinachtree.gist is available in the gist.jar, and has only one public class, Gist. See javadoc/.

The make.rb script will re-compile and generate a new gist.jar, javadoc, etc..



Development Notes -------------------------

The Java version of Gist uses a kit-set-bootstrap approach that can be used to develop Gist for other programming languages, or to extend and modify this Java version.

Stage 1:

The Calculator example illustrates the intended usage, and is used as a first Test case. The Test class contains a hand translated version of the Calculate grammar rules (to run as Java Op objects).

Gist is implemented with two private classes: a Parser that executes the grammar operators and generates a syntax tree, and a Transform that walks the syntax tree (Span nodes) and calls the user's transform methods. 

The Parser needs the grammar rules translated into grammar operator Op objects (that implement a parse method).

Development starts with the Test class for the Calculator example (see the make script).

The Test first runs the Op grammar operators with a hand compiled version of the Calculate grammar, then tests that the parser can parse an example arith expression. Then it tests that the transform rules can be run. Incremental development testing.

Next the Boot class can be tested (with its main, see the make script). It follows the same pattern as the first examples but the grammar is a boot grammar for PBNF rules, and the transform methods generate the grammar operator Ops. The Boot should be able to parse its own rules, and transform them into grammar Op terms.

Finally the Gist class, which relies on both the Parser and Transform class, can be tested by running the Calculator example.

All the basic infrastructure is now working.


Stage 2:

The full grammar is defined in PBNF.java together with transform methods to translate them into Op objects.

The Op classes are extended to implement a full set of grammar operators.

The Gist class provides the user interface and creates the internal Parser and Transform components.  The initial bootstrap uses a Boot object that can parse the PBNF grammar, after that is tested the full bootstrap uses the PBNF transform to compile itself as a parser for the full PBNF grammar.

Full testing and example applications can now be run.



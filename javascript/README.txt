
title	Gist in JavaScript

author	Peter Cashin

date	2011-5-1

This is a JavaScript version of Gist, a PBNF grammar transform parser.

The gist/doc directory contains an introduction overview: GrammarsForProgrammers.

The file: spinachtree.gist.js is a stand-alone module (no other dependencies) that exports spinachtree.gist for Gist node.js applications: var spinachtree = require('./spinachtree.gist.js'); The same file can be used in a browser client using the global var spinachtree.gist

The examples directory contains some PBNF grammar-transform examples. These examples can also be viewed with the examples.js.html browser page. The node.js examples can be executed from a shell console: node.js examples/... .js  The browser client examples require a test jig .html page to run them.

The demo/ directory contains an interactive web page demo: just direct your browser to: demo.html



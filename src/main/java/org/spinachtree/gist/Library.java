package org.spinachtree.gist;

import java.util.*;
import java.net.*;
import java.io.*;

class Library {

	static Map<String,String> grammars = new HashMap<String,String>();
	
	static String get(String label) { return grammars.get(label); }
	static void put(String label, String grammar) { grammars.put(label,grammar); }
	
	static void load(String label,String... lines) {
		StringBuffer sb=new StringBuffer();
		for (String line: lines) sb.append(line).append("\n");
		grammars.put(label,sb.toString());
	}

	// define built-in grammars ........................................
	
        static { 
        load("gist.pragma", // default names: _ -> gist.pragma
                "TAB : 9  -- ASCII std char name ",
                "LF  : 10 -- line feed ",
                "VT  : 11 -- vertical tab ",
                "FF  : 12 -- form feed, new page ",
                "CR  : 13 -- carriage return ",
                "SP  : 32 -- space char ",
                "quot: 34 -- &quot; ",
                "amp : 38 -- &amp; ",
                "apos: 39 -- &apos; ",
                "lt  : 60 -- &lt; ",
                "gt  : 62 -- &gt; ",
                "bs  : 92 -- backslash, not historic BS backspace ",

                "-- Separators............. ",
                "NEL : 0x85   -- Newline, Unicode Cc, mainframe eol, see XML 1.1 ",
                "Zl  : 0x2028 -- Unicode Separator-line ",
                "Zp  : 0x2029 -- Unicode Separator-paragraph ",
                "Zs  : SP/0xa0/0x1680/0x180e/0x2000..200A/0x202F/0x205F/0x3000     ",

                "-- Extended Unicode Separators, pseudo categories................ ",
                "Zblank : TAB/Zs            -- in-line blank space................... ",
                "Zbreak : LF/CR/NEL/Zl      -- all line end chars, but see nl...      ",
                "Zgap   : TAB/VT/FF/Zp/Zbreak -- generic eol plus field separators    ",
                "Zall   : Zs/Zgap           -- all white space and gap separators     ",

                "-- Common standard useage............ ",
                "sp  : Zall  -- all white space, built-in: ~ : sp*  ",
                "nl  : CR (LF/NEL)? / LF / NEL / Zl -- $ line-break, see XML 1.1 ",
                "char: 8..10/13/0x20..7E/0x85/0xA0..D7FF/0xE000..FDCF/0xFDC0..FFFD/0x10000..10FFFD ",

                "-- Posix standard ASCII char-set names...... ",
                "ascii  : 0..127 ",
                "alnum  : alpha/digit ",
                "alpha  : 'A'..'Z'/'a'..'z' ",
                "blank  : TAB/SP -- in-line white space ",
                "cntrl  : 0..31/127 ",
                "digit  : '0'..'9' ",
                "graph  : print-SP -- black chars ",
                "lower  : 'a'..'z' ",
                "print  : ascii-cntrl ",
                "punct  : ascii-cntrl-alnum -- !@#$%^&*()_+=-`~{}|[]\\:\";'<>?,./ ",
                "space  : TAB/LF/VT/FF/CR/SP -- ascii white-space ",
                "upper  : 'A'..'Z' ", 
                "xdigit : '0'..'9'/'A'..'F'/'a'..'f' ",

                "-- Unicode char-sets........ ",
                "letter : nameStart  -- liberal, more than L_ Unicodes, see XML 1.1 ",
                "white  : Zblank -- in-line Separator-space chars ",
                "black  : char-cntrl-Zall -- graph, expanded to Unicode ",
                "text   : black/white -- print, expanded to Unicode ",

                "Alnum  : Alpha/Digit -- alnum, expanded to Unicode ",
                "Alpha  : letter -- alpha, expanded to Unicode ",
                "Blank  : white -- blank, expanded to Unicode in-line white space ",
                "Digit  : Nd -- digit expanded to Unicode ",
                "Graph  : black -- graph, expanded to Unicode ",
                "Print  : black/white -- print, expanded to Unicode ",
                "Space  : sp -- space expanded to Unicode white-space ",
 
                "-- names and tokens.......... ",
                "name      : nameStart nameChar*  -- XML 1.1 Name excluding ':'/'_' and '-'/'.' ",
                "token     : (nameChar/':'/'_'/'-'/'.')+  -- same as XML 1.1 Nmtoken ",
                "xmlName   : (nameStart/':'/'_') (nameChar/':'/'_'/'-'/'.')*  -- XML 1.1 Name ",
                "nameStart : alpha/0xC0..D6/0xD8..F6/0xF8..2FF/0x370..37D/0x37F..1FFF/0x200C..200D ",
                "   /0x2070..218F/0x2C00..2FEF /0x3001..D7FF/0xF900..FDCF/0xFDF0..FFFD/0x10000..EFFFF ",
                "nameChar  : nameStart/Nd/0xB7/0x0300..036F/0x203F..2040  -- XML 1.1 less :_-. ",

                "-- Unicode Numeric-digit ...... ",
                "Nd : '0'..'9'/0x660..669/0x6F0..6F9/0x7C0..7C9/0x966..96F/0x9E6..9EF/        ",
                      "0xA66..A6F/0xAE6..AEF/0xB66..B6F/0xBE6..BEF/0xC66..C6F/0xCE6..CEF/     ",
                      "0xD66..D6F/0xE50..E59/0xED0..ED9/0xF20..F29/0x1040..1049/0x1090..1099/ ",
                      "0x17E0..17E9/0x1810..1819/0x1946..194F/0x19D0..19D9/0x1B50..1B59/      ",
                      "0x1BB0..1BB9/0x1C40..1C49/0x1C50..1C59/0xA620..A629/0xA8D0..A8D9/      ",
                      "0xA900..A909/0xAA50..AA59/0xFF10..FF19/0x104A0..104A9/0x1D7CE..1D7FF   "
                );
        }
}



import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

import org.spinachtree.gist.*;

public class GistTest {
 
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("GistTest");
  }

  @Test
  public void dateGist() {
  		String dateGrammar=
  		"	Date  = year '-' month '-' day	\n"+
  		"	year  : d d d d					\n"+
  		"	month : d d?					\n"+
  		"	day   : d d?					\n"+
  		"	d     : '0'..'9'				\n";
  		
  		Gist dateGist=new Gist(dateGrammar);
  		Term date=dateGist.parse("2001-2-3");
		//System.out.println(date);
  		assertTrue(date.has("day","3"));
  		assertTrue(date.child("month").text().equals("2"));
  		assertTrue(date.child().isTag("year"));
  		assertTrue(date.child().next("day").isText("3"));
  }

	@Test
	public void repeatTest() {
	String repTest= 
	"	reps  = alpha digit* alpha+ digit*3 alpha*2.._ digit*3..5 0..100*  \n"+
	"	alpha : 'a'..'z'/'A'..'Z'         \n"+
	"	digit : '0'..'9'                  \n";

	Gist reps=new Gist(repTest);
	Term t=reps.parse("a123bcd123efgh123456789");
	assertTrue(t.isTag("reps"));
	
	}

	@Test
	public void charsTest() {
		Gist nums=new Gist(
		"	nat  = odd+ even*5 low* hi+            ",
		"	codes: '0'..'9'                        ",
		"	even : !'1' !'3' !('5'/'7'/'9') codes  ",
		"	odd  : !('6'/'2'/'0') !'8' !'4' codes  ",
		"	low  : !'6'..'~' codes                 ",
		"	hi   : !'0'..'5' codes                 ",
		"	mid  : !'0'..'4' !'6'..'9' codes       ",
		"	void : !0xE..FF hi                     ",
		"	char : !34 !92 0x20..10ffff            ");

		Term t=nums.parse("97135246800569");
		assertTrue(t.isTag("nat"));
	}

	@Test
	public void ebnfTest() {
		String ebnfTest=
		"	ebnf   = name {s op [s arg ['.' arg]] }  \n"+
		"	name : alpha {alpha/'0'..'9'}  \n"+
		"	op   : '+'/'-'/'*'		\n"+
		"	arg  : name			\n"+
		"	s    : ' '*			\n"+
		"	alpha: 'a'..'z'/'A'..'Z'       	\n";
	
		Gist ebnf=new Gist(ebnfTest);
		Term e=ebnf.parse("foo + bar -flim.flam");
		//System.out.println(e);
		assertTrue(e.isTag("ebnf"));
	}

	@Test
	public void leftTest() {
		String leftTest=
		"	Sub   = Sub '-' int / int  \n"+
		"	int   : '0'..'9'+       	\n";
	
		Gist left=new Gist(leftTest);
		Term sub=left.parse("5-3-2");
		assertTrue(sub.isTag("Sub"));
	}
		
	@Test
	public void exp1Test() {
		String exp1Test=
		"	Exp   = Sub / int     \n"+
		"	Sub   = Exp '-' int   \n"+
		"	int   : '0'..'9'+     \n";
	
		Gist exp1=new Gist(exp1Test);
		Term ex1=exp1.parse("5-3-2");
		assertTrue(ex1.isTag("Exp"));
	}

	@Test
	public void expTest() {
		String expTest=
		"	Exp   = Add / Sub / Item    \n"+
		"	Add   = Exp '+' Item        \n"+
		"	Sub   = Exp '-' Item        \n"+
		"	Item  = Mul / Div / Val     \n"+
		"	Mul   = Item '*' Val        \n"+
		"	Div   = Item '/' Val        \n"+
		"	Val   = int / '(' Exp ')'   \n"+
		"	int   : '0'..'9'+       	\n";
		
		Gist exp=new Gist(expTest);
		Term ex=exp.parse("1+2*(3-4/2+1)");
		assertTrue(ex.isTag("Exp"));
	}

	@Test
	public void ambigTest() {
		String ambigTest=
		"	Exp   = Add / Sub / Item    \n"+
		"	Add   = Exp '+' Exp        \n"+
		"	Sub   = Exp '-' Exp        \n"+
		"	Item  = Mul / Div / Val     \n"+
		"	Mul   = Item '*' Item        \n"+
		"	Div   = Item '/' Item        \n"+
		"	Val   = int / '(' Exp ')'   \n"+
		"	int   : '0'..'9'+       	\n";
		
		Gist ambig=new Gist(ambigTest);
		Term am=ambig.parse("(1+2)+3");
		assertTrue(am.isTag("Exp"));
		am=ambig.parse("1+2*(3-4/2+1)+5");
		assertTrue(am.isTag("Exp"));
	}

/*	@Test
	public void importTest() {
		String importTest=
		"	Imp   = letter Nd (letter/digit)* \n"+
		"	digit ->  gist.pragma#Nd        \n"+
		"	_     ->  gist.pragma	\n";
		
		Gist imp=new Gist(importTest);
		Term t=imp.parse("x42");
		//System.out.println(t);
		assertTrue(t.isTag("Imp"));
	}*/

	@Test
	public void importTest() {
		String importTest=
		"	Imp   = letter Nd (letter/digit)* \n"+
		"	digit =  gist.pragma.Nd        \n"+
		"	_     =  gist.pragma._         \n";

		Gist imp=new Gist(importTest);
		Term t=imp.parse("x42");
		//System.out.println(t);
		assertTrue(t.isTag("Imp"));
	}
	
}


package org.spinachtree.gist;

/*

Utility methods for working with Gist Parser Machine Code.....

*/

//import java.util.regex.*;
import java.util.*;

class CodePad {

	static final int SIZE=128;	
	
	int[] code;
	int loadAt;

	String[] ruleNames;
		
	CodePad(String[] ruleNames) {
		this.ruleNames=ruleNames;
		int ruleCount=ruleNames.length;
		int size=SIZE;
		if (ruleCount>=SIZE/2) size+=(ruleCount/SIZE+1)*SIZE;
		code=new int[size];
		loadAt=ruleCount;
	}
	
	CodePad(Parser parser) {
		this.code=parser.code;
		this.ruleNames=parser.ruleNames;
		if (ruleNames.length!=code[code.length-2]) System.out.println("CodePad? rules size...");
		loadAt=code[code.length-1];
		//System.out.printf("CodePad: len=%d loadAt=%d rn=%d \n",code.length,loadAt,ruleNames.length);
	}
	
	void load(int pc,int coda) { 
		if (pc<loadAt) code[pc]=coda;
		else System.exit(-1); // woops
	}
	
	int load(int x) {
		int pc=loadStart(1);
		code[loadAt++]=x;
		return pc;
 	}

	int load(int[] xs) { return load(xs,xs.length);}
	
	int load(int[] xs, int size) {
		int pc=loadStart(size);
		code[loadAt++]=size;
		for (int i=0;i<size;i++) code[loadAt++]=xs[i];
		return pc;
 	}

	int loadCopy(int[] xcode, int x) {
		int size=xcode[x++];
		int pc=loadStart(size);
		code[loadAt++]=size;
		for (int i=0;i<size;i++) code[loadAt++]=xcode[x+i];
		return pc;
	}
	
	int loadStart(int len) {
		int size=code.length;
		if (loadAt+len>=size) { // expand code space...
			int size1=size+SIZE;
			if (len>SIZE) size1+=(len/size)*SIZE;
			int[] code1=new int[size1];
			System.arraycopy(code,0,code1,0,code.length);
			code=code1;
		}
		return loadAt;
	}
	
	int[] code() { // return loaded code
		//code[loadStart]=loadAt-loadStart-1;
		int size=loadStart(2);
		code[code.length-2]=ruleNames.length;
		code[code.length-1]=size;
		return code;
	}

	// == dump code == debug inspection  ====================================================

	static void dump(Parser parser) {
		CodePad cp = new CodePad(parser);
		System.out.printf("Dump parser opcodes: ruleCount=%d size=%d of %d\n",cp.ruleNames.length,cp.loadAt,cp.code.length);
		for (int i=0; i<cp.loadAt; i++) cp.dumpLine(i);
	}

	void dumpLine(int i) {
		String label="";
		if (i<ruleNames.length) label=ruleNames[i];
		int opcode=code[i];
		System.out.printf("%12s%5d %s\n",label,i,opDump(opcode));
	}
		
	String opDump(int opcode) {	
		int op=opcode>>24;
		int arg=opcode&0xffffff;
		
		if (op==0) return String.format("%d",arg);
		if (op>Pam.opNames.length) return String.format("%d %-3d",op,arg); // undefined
		
		String label=String.format("%s %-3d",Pam.opNames[op],arg);
		if (op==Pam.REF||op==Pam.RUN||op==Pam.LST)
			label+=String.format(" %s",ruleNames[arg]);
		else if (op==Pam.CHS) {
			int n=code[arg], i=arg+1; // n=opcode count, i ptr to first
			int show=n;
			if (n>10) show=10;
			while (i<=arg+show) label+=String.format(" %d..%d",code[i++],code[i++]);
			if (n>show) label+=" ...";
		}
		else if (op==Pam.EXT) label+=String.format(" %d, %d, %d",(arg>>20)&0xf,(arg>>10)&0x3ff,arg&0x3ff);
		return label;
	} // opDump


	// // -- Span tree inspection ------------------------------------------------------------------
	// 
	// static String inspect(Span grit) { return inspect(grit,null,null); }
	// 
	// static String inspect(Span grit, Parser parser, String input) {
	// 	StringBuilder sb=new StringBuilder();
	// 	String[] names=parser.ruleNames;
	// 	tree(grit,"",sb,names,input);
	// 	return sb.toString();
	// }
	// 
	// static void tree(Span grit, String inset, StringBuilder sb, String[] names, String input) {
	// 	String name=tagName(grit.tag,names);
	// 	if (grit.tip==grit.top) { // leaf terminal
	// 		sb.append(inset).append(name).append(" ");
	// 		text(sb,input,grit.sot,grit.eot);
	// 		sb.append("\n");
	// 	} else {
	// 		sb.append(inset).append(name).append(" ");
	// 		Span[] kids=grit.children();
	// 		while (kids!=null && kids.length==1) {
	// 			Span kid=kids[0];
	// 			sb.append(tagName(kid.tag,names)).append(" ");
	// 			if (kid.tip==kid.top) {
	// 				text(sb,input,kid.sot,kid.eot);
	// 				kids=null;
	// 			} else kids=kid.children();
	// 		}
	// 		sb.append("\n");
	// 		if (kids!=null)
	// 			for (Span kid: kids) tree(kid,inset+"    ",sb,names,input);
	// 	}
	// }
	// 
	// static String tagName(int n, String[] names) {
	// 	if (names==null || n>=names.length || n<0) return String.format("%d",n);
	// 	return names[n];
	// }
	// 
	// static void text(StringBuilder sb, String input, int i, int j) {
	// 	if (input==null) sb.append(String.format("%d..%d",i,j));
	// 	else {
	// 		sb.append("\"");
	// 		for (int k=i;k<j;k++) {
	// 			int x=input.charAt(k);
	// 			if (x==92) sb.append("\\");
	// 			else if (x==34) sb.append("\"");
	// 			else if (x>31 && x<0XFF) sb.append((char)x);
	// 			else if (x==9) sb.append("\\t");
	// 			else if (x==10) sb.append("\\n");
	// 			else if (x==13) sb.append("\\r");
	// 			else sb.append("\\u"+Integer.toHexString(x));
	// 		}
	// 		sb.append("\"");
	// 	}
	// }

}
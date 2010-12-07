
package org.spinachtree.gist;

import java.util.*;

class Parser {

	Parser(String grammar, String[] ruleNames, int[] code) {
		this.grammar=grammar;
		this.ruleNames=ruleNames;
		this.nameIdx=indexMap(ruleNames);
		this.code=code;
		this.links=null;
	}

	Parser(String grammar, String[] ruleNames, Map<String,Integer> nameIdx, int[] code, Parser[] links) {
		this.grammar=grammar;
		this.ruleNames=ruleNames;
		this.nameIdx=nameIdx;
		this.code=code;
		this.links=links;	
	}

	String grammar;
	String[] ruleNames;
	Map<String,Integer> nameIdx;
	int[] code;
	Parser[] links; // external grammar parsers

	Parse parse(String src) {
		Pam p=pam(src);
		boolean result=p.parse(0);
		if (result) return new Parse(src,p.tree,p.max,this);
		Span fault=new Span(-1,0,p.max,Span.NIL,p.tree,null);
		return new Parse(src,fault,p.max,this);
	}
	
	Pam pam(String src) { 
		Pam[] ps=null;
		if (links!=null) {
			ps=new Pam[links.length];
			for (int i=1;i<links.length;i++) ps[i]=links[i].pam(src);
		}
		return new Pam(src,code,ps);
	}
	
	int[] code() { return code; }
	
	int tag(String name) {
		Integer id=nameIdx.get(name);
		if (id==null) return -1;
		return id.intValue();
	}
	
	String tagName(int tag) { if (tag<0) return "-fault"; else return ruleNames[tag&0x3ff]; }
	
	Map<String,Integer> indexMap(String[] names) {
		Map<String,Integer> map=new HashMap<String,Integer>();
		for (int i=0;i<names.length;i++) map.put(names[i],new Integer(i));
		return map;
	}
	

} // Parser
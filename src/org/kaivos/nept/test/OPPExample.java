package org.kaivos.nept.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.kaivos.nept.parser.OperatorLibrary;
import org.kaivos.nept.parser.OperatorPrecedenceParser;
import org.kaivos.nept.parser.TokenList;
import org.kaivos.nept.parser.TokenScanner;

/**
 * An example program demonstrating the usage of the operator precedence parser
 * 
 * @author Iikka Hauhio
 *
 */
public class OPPExample {

	/**
	 * The main method
	 * 
	 * @param args The arguments
	 * @throws IOException sometimes
	 */
	public static void main(String[] args) throws IOException {
		TokenScanner t = new TokenScanner()
			.addOperators("+-*/%()[]")
			.separateIdentifiersAndPunctuation(false)
			.addCommentRule("/*", "*/")
			.appendOnEOF("<EOF>");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while ((line = in.readLine()) != null) {
			Calculator c = new Calculator(t.tokenize(line, "<stdin>"));
			System.out.println(c.parse());
		}
	}
	
	/**
	 * The calculator
	 * 
	 * @author Iikka Hauhio
	 *
	 */
	public static class Calculator {
	
		private OperatorLibrary<Integer> library;
		private OperatorPrecedenceParser<Integer> opparser;
		private TokenList tokenlist;
		
		/**
		 * The constructor
		 * 
		 * @param tl TokenList
		 */
		public Calculator(TokenList tl) {
			this.tokenlist = tl;
			
			/* Declares the operator library – all operators use parsePrimary() as their RHS parser */
			library = new OperatorLibrary<>(() -> parsePrimary(tl));
			
			/* Declares the operators*/
			library.add("+", (a, b) -> a + b);
			library.add("-", (a, b) -> a - b);
			library.increaseLevel();
			library.add("*", (a, b) -> a * b);
			library.add("/", (a, b) -> a / b);
			
			/* Declares the OPP*/
			opparser = new OperatorPrecedenceParser<>(library);
		}
		
		/**
		 * Parses an expression from the token list
		 * 
		 * @return the value of the expression
		 */
		public int parse() {
			return parseExpression(tokenlist);
		}
		
		private int parseExpression(TokenList tl) {
			return opparser.parse(tl);
		}
		
		private int parsePrimary(TokenList tl) {
			if (tl.isNext("-"))
				return -parsePrimary(tl);
			if (tl.isNext("(")) {
				tl.accept("(");
				int i = parseExpression(tl);
				tl.accept(")");
				return i;
			}
			else return Integer.parseInt(tl.nextString());
		}
	
	}

}

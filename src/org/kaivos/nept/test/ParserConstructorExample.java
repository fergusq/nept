package org.kaivos.nept.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.kaivos.nept.parser.TokenScanner;
import org.kaivos.nept.parser.constructor.ParserConstructor;
import org.kaivos.nept.parser.constructor.ParserConstructor.PCParser;

/**
 * Parses expressions using ParserConstructor
 * 
 * @author Iikka Hauhio
 *
 */
public class ParserConstructorExample {

	/**
	 * @param args The arguments
	 * @throws IOException sometimes
	 */
	public static void main(String[] args) throws IOException {
		TokenScanner t = new TokenScanner()
			.addOperators("+-*/%()[]")
			.separateIdentifiersAndPunctuation(false)
			.appendOnEOF("<EOF>");
	
		ParserConstructor pc = new ParserConstructor();
		
		pc.node("Primary")
				.ACCEPT("(").NODE("expr", "Expression").ACCEPT(")")
				.OR().ACCEPT("-").NODE("neg", "Primary")
				.OR().PROCESS("number", Integer::parseInt);
		
		pc.node("Term")
				.NODE("operand", "Primary").MANY(pc.node().PACCEPT("op", "*", "/").NODE("operand", "Primary"));
		
		PCParser expression = pc.node("Expression")
				.NODE("operand", "Term").MANY(pc.node().PACCEPT("op", "+", "-").NODE("operand", "Term"));
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while ((line = in.readLine()) != null) {
			Map<String, List<Object>> map = expression.parse(t.tokenize(line, "<stdin>"));
			System.out.println(map);
		}
	}
	
}

package org.kaivos.nept.parser.test;

import static java.util.stream.Collectors.joining;

import org.junit.*;
import static org.junit.Assert.*;

import org.kaivos.nept.parser.TokenScanner;
import org.kaivos.nept.parser.TokenList;
import org.kaivos.nept.parser.ParsingException;

public class TokenScannerTest {

	String joinTokens(TokenList tl) {
		return tl.toList().stream()
			.map(token -> token.toString().replaceAll(",", ",,"))
			.collect(joining(", "));
	}
	
	@Test
	public void testOperators() {
		TokenScanner t = new TokenScanner()
			.separateIdentifiersAndPunctuation(false)
			.addOperators("+-")
			.appendOnEOF("<EOF>");
		assertEquals("a, +, b, a, -, b, a*b, a/b, *, +, -, /*, +, +, -, */, <EOF>",
			     joinTokens(t.tokenize("a+b a-b a*b a/b *+-/*+ +-*/", "<test>")));
	}
	
	@Test
	public void testQuote() {
		TokenScanner t = new TokenScanner()
			.addStringRule('"', '"', '\\')
			.addOperators("\\")
			.addEscapeCode('a', "abba")
			.appendOnEOF("<EOF>");
		assertEquals("\\, \", naakkaabbahaukka\"kotka, \", \\, <EOF>",
			     joinTokens(t.tokenize("\\\"naakka\\ahaukka\\\"kotka\"\\", "<test>")));
	}
	
	@Test
	public void testMulticharQuote() {
		TokenScanner t = new TokenScanner()
			.addStringRule("[[", "]]", (char) 0)
			.addOperators("[]")
			.appendOnEOF("<EOF>");
		assertEquals("[[, ab\nb\\a\" \ra[b]ba, ]], <EOF>",
			     joinTokens(t.tokenize("[[ab\nb\\a\" \ra[b]ba]]", "<test>")));
	}
	
	@Test
	public void testComment() {
		TokenScanner t = new TokenScanner()
			.addCommentRule("alku", "loppu")
			.appendOnEOF("<EOF>");
		assertEquals("abb, dabba, <EOF>",
			     joinTokens(t.tokenize("abbalkukanneloppudabba", "<test>")));
	}
	
	@Test(expected=ParsingException.class)
	public void testUnclosedComment() {
		TokenScanner t = new TokenScanner()
			.addCommentRule("alku", "loppu")
			.appendOnEOF("<EOF>");
	        t.tokenize("abbalkukanneladabba", "<test>");
	}
	
	@Test
	public void testSingleTokenComment() {
		TokenScanner t = new TokenScanner()
			.ignore("kommentti")
			.appendOnEOF("<EOF>");
		assertEquals("abba, dabba, <EOF>",
			     joinTokens(t.tokenize("abbakommenttidabba", "<test>")));
	}
	
}

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
	
}

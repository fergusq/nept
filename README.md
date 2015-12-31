nept
====

The New Parser Tools

## Overview

The New Parser Tools is a complete rewrite of the old parser tools (also avalable on github). Core utilities like `TokenScanner` and `TokenList` are simplified and made easier to use.

## Usage

### TokenScanner

The `TokenScanner` does not implement `TokenList` like in the old parser tools, but instead returns an instance of one, thus removing need to create a new scanner every time something is parsed.

A minimalistic scanner specifies all operator characters and other separators, which are then used to separate tokens.

```java
TokenScanner scanner = new TokenScanner()
			.addOperators("+-*/%()<>")
			.addOperatorRule("==")
			.addOperatorRule("!=")
			.addOperatorRule("<=")
			.addOperatorRule(">=")
			.appendOnEOF("<EOF>");
```

By default `TokenScanner` separates all punctuation characters from all other characters. If this is not needed, it can be turned of with `.separateIdentifiersAndPunctuation(false)`.

### TokenList

A token list object is redesigned to provide more methods, thus moving the responsibility of some token related tasks from the parser class to the token list.

The core methods of `TokenList` are still `.next()` and `.seek()`. New methods include `.isNext()`, `.nextString()`, `.seekString()` and `.accept()`.

```java
public Operation parseInstruction(TokenList tl) {
  if (tl.isNext("print")) {
    tl.accept("print");
    String var = tl.nextString();
    return () -> System.out.print(variables.get(var));
  }
  else if (tl.isNext("read")) {
    tl.accept("read");
    String var = tl.nextString();
    return () -> variables.put(var, System.in.read());
  }
  else {
    String command = tl.next();
    String var = tl.next();
    return () -> commands.get(command).consume(var);
  }
}
```

### OperatorPrecedeceParser

To make it easier to implement a short parser for a programming language, Nept includes an operator precedence parsing class. For complete example, see `OPPExample.java`.

```java
/* Declares the operator library – all operators use parsePrimary() as their RHS parser */
OperatorLibrary<Integer> library = new OperatorLibrary<>(tl -> parsePrimary(tl));
			
/* Declares the operators*/
library.add("+", (a, b) -> a + b);
library.add("-", (a, b) -> a - b);
library.increaseLevel();
library.add("*", (a, b) -> a * b);
library.add("/", (a, b) -> a / b);
			
/* Declares the OPP*/
OperatorPrecedenceParser<Integer> opparser = new OperatorPrecedenceParser<>(library);
```

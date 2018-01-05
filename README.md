jlox
====

`jlox` is a Java implementation of the tree-walk interpreter from the [Crafting Interpreters] book.

[Crafting Interpreters]: http://www.craftinginterpreters.com/

Code Organization
-----------------

The code is organized so that each chapter is on its own branch. At the end of each chapter, the branch merges with master. Any challenges that get done are coded on the chapter branch after the merge:

```text
^ * b44819c (chapter-4) Challenge 4.4 Nested Block Comments
| * 6bbaa10 Challenge 4.4 Block Comments
|/
*   2d553a0 (master) Chapter 4
|\  
| * 10e3b6b 4.7 Reserved Words and Identifiers
| * 3166716 4.6 Longer Lexemes
| * 3474e6d 4.5 Recognizing Lexemes
| * 17226fa 4.4 The Scanner Class
| * ffa1b07 4.2 Tokens and Lexemes
| * f704cb4 4.1 The Interpreter Framework
|/  
* 3893584 Set up initial Gradle project
```

Running
-------

Clone the project to a local directory. Building and running are done using [`just`](just):

```text
git clone https://github.com/ryanq/jlox
cd jlox
just build
./jlox script.lox
./jlox
```

[just]: https://github.com/casey/just

lox: bin
    java -cp bin/lox com.craftinginterpreters.lox.Lox

build: bin
    javac -d bin/lox src/com/craftinginterpreters/lox/*.java

clean:
    @rm -rf bin

bin:
    @mkdir -p bin/lox
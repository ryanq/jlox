package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, State>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    private enum State {
        DECLARED,
        DEFINED,
        USED
    }

    private class UnusedVariableError extends RuntimeException {
        final List<String> variables;

        UnusedVariableError(List<String> variables) {
            this.variables = variables;
        }
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        try {
            endScope();
        } catch (UnusedVariableError error) {
            Lox.error(1, "Unused variables: " + String.join(", ", error.variables));
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cannot return from top-level code.");
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == State.DECLARED) {
            Lox.error(expr.name, "Cannot read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        use(expr.name);
        return null;
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.parameters) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        try {
            endScope();
        } catch (UnusedVariableError error) {
            Lox.error(function.name, "Unused variables: " + String.join(", ", error.variables));
        }

        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, State>());
    }

    private void endScope() {
        Map<String, State> scope = scopes.pop();

        List<String> unused = new ArrayList<>();
        for (Entry<String, State> e : scope.entrySet()) {
            String name = e.getKey();
            State state = e.getValue();

            if (state != State.USED) {
                unused.add(name);
            }
        }

        if (!unused.isEmpty()) {
            throw new UnusedVariableError(unused);
        }
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, State> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Variable with this name already declared in this scope.");
        }

        scope.put(name.lexeme, State.DECLARED);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, State.DEFINED);
    }

    private void use(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, State.USED);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }

        // Not found. Assume it is global.
    }
}

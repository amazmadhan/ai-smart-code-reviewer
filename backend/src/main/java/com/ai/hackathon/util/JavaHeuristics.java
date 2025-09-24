package com.ai.hackathon.util;

import com.ai.hackathon.model.Issue;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.Modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaHeuristics {

    public static List<Issue> findIssuesWithLines(CompilationUnit cu, String source) {
        List<Issue> issues = new ArrayList<>();

        // Detect System.out.print* usages
        cu.findAll(MethodCallExpr.class).forEach(mc -> {
            if (mc.getScope().isPresent()) {
                String scope = mc.getScope().get().toString();
                String name = mc.getNameAsString();
                if (("System.out".equals(scope) || scope.endsWith("System.out")) &&
                        ("println".equals(name) || "print".equals(name))) {
                    int line = mc.getBegin().map(p -> p.line).orElse(-1);
                    issues.add(new Issue(line + 1, "Use of System.out.println — prefer a logging framework (SLF4J/Logback)."));
                }
            }
        });

        // Detect TODO/FIXME comments
        Pattern pTodo = Pattern.compile("TODO|FIXME");
        Matcher mTodo = pTodo.matcher(source);
        while (mTodo.find()) {
            int pos = mTodo.start();
            int line = source.substring(0, pos).split("\\R").length;
            issues.add(new Issue(line, "Found TODO/FIXME comment — address before production."));
        }

        // Detect hard-coded credentials
        Pattern pCred = Pattern.compile("(?i)(password\\s*=\\s*\".+?\"|secret\\s*=\\s*\".+?\"|API_KEY\\s*=\\s*\".+?\")");
        Matcher mCred = pCred.matcher(source);
        while (mCred.find()) {
            int pos = mCred.start();
            int line = source.substring(0, pos).split("\\R").length;
            issues.add(new Issue(line, "Possible hard-coded credential pattern — move secrets to config/secrets manager."));
        }

        // Detect broad catch of Exception
        Pattern pCatch = Pattern.compile("catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)", Pattern.CASE_INSENSITIVE);
        Matcher mCatch = pCatch.matcher(source);
        while (mCatch.find()) {
            int pos = mCatch.start();
            int line = source.substring(0, pos).split("\\R").length;
            issues.add(new Issue(line, "Broad catch of Exception — catch specific exceptions and avoid swallowing errors."));
        }

        // Detect possible SQL concatenation
        Pattern pSql = Pattern.compile("execute(Query|Update)\\s*\\(.*\\+.*\\)", Pattern.CASE_INSENSITIVE);
        Matcher mSql = pSql.matcher(source);
        while (mSql.find()) {
            int pos = mSql.start();
            int line = source.substring(0, pos).split("\\R").length;
            issues.add(new Issue(line, "Possible SQL string concatenation — use PreparedStatement to prevent SQL injection."));
        }

        // Detect long methods
        cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).forEach(md -> {
            int lines = md.toString().split("\\R").length;
            if (lines > 50) {
                int line = md.getBegin().map(p -> p.line).orElse(-1);
                issues.add(new Issue(line + 1, "Long method '" + md.getName() + "' (" + lines + " lines) — consider refactoring."));
            }
        });

        return issues;
    }

    public static int computePenaltyFromIssues(List<Issue> issues) {
        int penalty = 0;
        for (Issue iss : issues) {
            String s = iss.getMessage();
            if (s.contains("Long method")) penalty += 15;
            else if (s.contains("System.out.println")) penalty += 5;
            else if (s.contains("TODO") || s.contains("FIXME")) penalty += 8;
            else if (s.contains("hard-coded")) penalty += 30;
            else if (s.contains("Broad catch")) penalty += 10;
            else if (s.contains("SQL")) penalty += 25;
            else penalty += 5;
        }
        return Math.min(penalty, 100);
    }

    public static String applyAstRefactors(String source) {
        try {
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> pr = parser.parse(source);

            if (pr.isSuccessful() && pr.getResult().isPresent()) {
                CompilationUnit cu = pr.getResult().get();

                // 1. Replace System.out.println with logging
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodCallExpr n, Void arg) {
                        if (n.getScope().isPresent() &&
                                n.getScope().get().toString().equals("System.out") &&
                                n.getNameAsString().equals("println")) {

                            n.setScope(new NameExpr("logger"));
                            n.setName("info");
                        }
                        super.visit(n, arg);
                    }
                }, null);

                // 2. Remove TODO/FIXME comments
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(LineComment n, Void arg) {
                        String comment = n.getContent().toLowerCase();
                        if (comment.contains("todo") || comment.contains("fixme")) {
                            n.remove();
                        }
                        super.visit(n, arg);
                    }
                }, null);

                // 3. Replace hard-coded credentials with config
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(VariableDeclarator n, Void arg) {
                        if (n.getNameAsString().toLowerCase().contains("password") ||
                                n.getNameAsString().toLowerCase().contains("secret") ||
                                n.getNameAsString().toLowerCase().contains("key")) {

                            if (n.getInitializer().isPresent()) {
                                // Replace with environment variable or config
                                n.setInitializer(new MethodCallExpr(
                                        new NameExpr("System"), "getenv",
                                        new NodeList<>(new StringLiteralExpr("APP_PASSWORD"))
                                ));
                            }
                        }
                        super.visit(n, arg);
                    }
                }, null);

                // 4. Replace broad Exception catching with specific exceptions
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(CatchClause n, Void arg) {
                        if (n.getParameter().getType().toString().equals("Exception")) {
                            // Replace with ArithmeticException for division by zero
                            n.getParameter().setType("ArithmeticException");
                        }
                        super.visit(n, arg);
                    }
                }, null);

                // 5. Add necessary imports
                cu.addImport("org.slf4j.Logger");
                cu.addImport("org.slf4j.LoggerFactory");

                // 6. Add logger field if it doesn't exist
                boolean hasLogger = cu.getTypes().stream()
                        .anyMatch(type -> type.getFields().stream()
                                .anyMatch(field -> field.getVariable(0).getNameAsString().equals("logger")));

                if (!hasLogger && !cu.getTypes().isEmpty()) {
                    TypeDeclaration<?> mainClass = cu.getType(0);
                    FieldDeclaration loggerField = new FieldDeclaration()
                            .addModifier(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL)
                            .addVariable(new VariableDeclarator()
                                    .setType("Logger")
                                    .setName("logger")
                                    .setInitializer(new MethodCallExpr(
                                            new NameExpr("LoggerFactory"), "getLogger",
                                            new NodeList<>(new ClassExpr(new ClassOrInterfaceType(mainClass.getNameAsString())))
                                    )));

                    mainClass.getMembers().add(0, loggerField);
                }

                return cu.toString();
            }
        } catch (Exception e) {
            System.err.println("Error during AST refactoring: " + e.getMessage());
        }

        return source; // Return original if refactoring fails
    }
}

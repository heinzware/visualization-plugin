package de.heinzen.probplugin.visualization.loader.clazz;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 09.10.17
 */
public class InMemoryCompilerException extends Exception {

    private final String compilerMessage;

    public InMemoryCompilerException(String className, DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder sb = new StringBuilder("\nErrors during compilation:\n\n");
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                String source = "No source";
                if (diagnostic.getSource() != null) {
                    source = diagnostic.getSource().getName();
                }
                if (source.endsWith(className + ".java")) {
                    source = className + ".java";
                }
                sb.append("\tKind:\t\t\t\t").append(diagnostic.getKind()).append("\n")
                        .append("\tSource:\t\t\t").append(source).append("\n")
                        .append("\tCode:\t\t\t").append(diagnostic.getCode()).append("\n")
                        .append("\tMessage:\t\t\t")
                        .append(diagnostic.getMessage(null).replaceAll("\n", "\n\t\t\t\t\t")).append("\n")
                        .append("\tLine:\t\t\t\t").append(diagnostic.getLineNumber()).append("\n")
                        .append("\tPosition/Column:\t").append(diagnostic.getPosition()).append("/")
                        .append(diagnostic.getColumnNumber()).append("\n")
                        .append("\tStart-/Endposition:\t").append(diagnostic.getStartPosition()).append("/")
                        .append(diagnostic.getEndPosition()).append("\n\n");
            }
        }
        compilerMessage = sb.toString();
    }

    public String getCompilerMessage() {
        return compilerMessage;
    }
}

package de.heinzen.probplugin.visualization.loader.clazz;

import javax.tools.*;
import java.io.File;
import java.util.Arrays;

/**
 * Created by Christoph Heinzen on 27.04.17.
 */
public class InMemoryCompiler {

    public Class<?> compile(String className, File javaClassFile, String classpath, ClassLoader pluginClassloader) throws Exception{
        // create complete classpath
        classpath = (System.getProperty("java.class.path") + File.pathSeparator + classpath)
                .replaceAll("%20", " ");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        InMemoryClassloader classLoader = new InMemoryClassloader(pluginClassloader);

        try (JavaFileManager inMemoryFileManager = new InMemoryJavaFileManager(compiler, classLoader, diagnostics);
             StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {

            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(javaClassFile);

            //create compiler-task
            JavaCompiler.CompilationTask task = compiler.getTask(null, inMemoryFileManager,
                            diagnostics, Arrays.asList("-classpath", classpath), null, units);

            //compile and throw exception when an error occurs
            if (!task.call()) {
                throw new Exception(formatCompilerErrors(diagnostics));
            }

        }
        return Class.forName(className, true, classLoader);
    }

    private String formatCompilerErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder sb = new StringBuilder("\n\tErrors during compilation:\n\n\t\t");
        for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            sb.append("Kind: ").append(diagnostic.getKind()).append("\n\t\t")
                    .append("Source: ").append(diagnostic.getSource()).append("\n\t\t")
                    .append("Code and Message: ").append(diagnostic.getCode()).append(": ")
                    .append(diagnostic.getMessage(null)).append("\n\t\t")
                    .append("Line: ").append(diagnostic.getLineNumber()).append("\n\t\t")
                    .append("Position/Column: ").append(diagnostic.getPosition()).append("/")
                    .append(diagnostic.getColumnNumber()).append("\n\t\t")
                    .append("Startpostion/Endposition: ").append(diagnostic.getStartPosition()).append("/")
                    .append(diagnostic.getEndPosition()).append("\n\n\t\t");
        }
        return sb.toString();
    }

}

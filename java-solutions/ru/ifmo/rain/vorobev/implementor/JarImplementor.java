package ru.ifmo.rain.vorobev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * An implementation of {@link JarImpler}.
 * Making a default implementation of an interface given with {@link #implement} or {@link #implementJar}.
 *
 * @author Aleksandr Vorobev
 * @version 1.0
 */
public class JarImplementor implements JarImpler {

    /**
     * Default constructor.
     */
    public JarImplementor() {
    }

    /**
     * Index for argument name.
     */
    private static int index;

    /**
     * Main function. It takes the arguments from the input and checks if they are correct.
     * If there is an argument -jar then the program runs in JarImplementation mode [-jar] [interfaceName] [path]
     * else the program runs in implementation mode [interfaceName] [path]
     *
     * @param args list of arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.out.println("Two or three arguments were expected");
            return;
        }
        boolean isJar = args[0].equals("-jar");
        if (isJar && (args.length != 3 || args[2] == null)) {
            System.out.println("Three arguments were expected in jar implementation");
            return;
        }
        int classIndex = isJar ? 1 : 0;
        try {
            Class<?> token = Class.forName(args[classIndex]);
            if (isJar) {
                new JarImplementor().implementJar(token, Paths.get(args[classIndex + 1]));
            } else {
                new JarImplementor().implement(token, Paths.get(args[classIndex + 1]));
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Unable to find an interface");
        } catch (InvalidPathException e) {
            System.out.println("Invalid path to root");
        } catch (ImplerException e) {
            System.out.println("Unable to implement interface");
        }
    }

    /**
     * Makes a jar-file with implementation of interface at the given {@link Path}.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException which shows that input data is wrong.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path path = createDirectories(jarFile);
        implement(token, path);
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Compiler is null");
        }
        String[] args = {"-encoding", "UTF-8", "-cp",
                path.toString() + File.pathSeparator + getSuperPath(token).toString(),
                Path.of(path.toString(), getPathNameToImpl(token) + "Impl.java").toString()
        };
        int compileResult = compiler.run(null, null, null, args);
        if (compileResult != 0) {
            throw new ImplerException("Compile error with result:" + compileResult);
        }
        createJar(jarFile, path, token);
    }

    /**
     * Makes an implementation of given interface and puts it into class-file at given {@link Path}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException which shows that input data is wrong.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        int modifiers = token.getModifiers();
        if (Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Wrong token given");
        }
        String rootName = getPathNameToImpl(token) + "Impl.java";
        Path rootPath;
        try {
            rootPath = Paths.get(root.toString(), rootName);
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path to root");
        }
        if (rootPath.getParent() != null) {
            try {
                Files.createDirectories(rootPath.getParent());
            } catch (IOException e) {
                throw new ImplerException("Invalid path generated");
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(rootPath)) {
            writer.write(create(token));
        } catch (IOException e) {
            throw new ImplerException("Unable to write");
        }
    }

    /**
     * Creates a jar-file with implementation of interface at the given {@link Path}.
     *
     * @param jarFile directory to save jar-file
     * @param root directory with .class code
     * @param token type token to create implementation for.
     * @throws ImplerException which shows that creating jar is failed.
     */
    private static void createJar(Path jarFile, Path root, Class<?> token) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String implementationPath = getPathNameToImpl(token) + "Impl.class";
            stream.putNextEntry(new ZipEntry(implementationPath));
            Files.copy(Path.of(root.toString(), implementationPath), stream);
        } catch (IOException e) {
            throw new ImplerException("Can't create Jar", e);
        }
    }

    /**
     * Creates {@link Path} of implementation source code and create missing parent directories.
     *
     * @param path {@link Path} for implementation files.
     * @return {@link Path} where implementation must be created.
     * @throws ImplerException which shows that creating directories is failed.
     */
    private static Path createDirectories(Path path) throws ImplerException {
        Path parentPath = path.toAbsolutePath().normalize().getParent();
        try {
            Files.createDirectories(parentPath);
        } catch (IOException e) {
            throw new ImplerException("Unable to create directories");
        }
        try {
            return Files.createTempDirectory(parentPath, "impler-temp-dir");
        } catch (IOException e) {
            throw new ImplerException("Unable to create temp directories");
        }
    }

    /**
     * Finds classPath for token.
     *
     * @param token type token to create implementation for.
     * @return classPath for given token.
     * @throws ImplerException which shows that generation is failed.
     */
    private static Path getSuperPath(Class<?> token) throws ImplerException {
        try{
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new ImplerException("Syntax error");
        }
    }

    /**
     * Provides valid source code directories.
     *
     * @param token type token to create implementation for.
     * @return path to source code as {@link String}.
     */
    private static String getPathNameToImpl(Class<?> token) {
        return String.join("/", token.getPackageName().split("\\.")) +
                "/" + token.getSimpleName();
    }

    /**
     * Makes a {@link String} consisting of arguments passed, separated by a delimiter.
     *
     * @param delimiter separator symbol
     * @param args array of {@link String} to concatenate.
     * @return resulting {@link String}
     */
    private static String createLine(String delimiter, String... args) {
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            for (char c : s.toCharArray()) {
                sb.append(c < 128 ? Character.toString(c) : String.format("\\u%04x", (int) c));
            }
            sb.append(delimiter);
        }
        return sb.toString();
    }

    /**
     * Generates code.
     *
     * @param token type token to create implementation for.
     * @return resulting code.
     */
    private String create(Class<?> token) {
        return createLine(System.lineSeparator(),
                createPackageName(token), createClassName(token), createMethods(token), "}");
    }

    /**
     * Generates package name.
     *
     * @param token type token to create implementation for.
     * @return {@link String} with package name
     */
    private String createPackageName(Class<?> token) {
        Package packge = token.getPackage();
        return packge == null ? "" : createLine(" ", "package", packge.getName(), ";");
    }

    /**
     * Generates class name.
     *
     * @param token type token to create implementation for.
     * @return {@link String} with class name.
     */
    private String createClassName(Class<?> token) {
        return createLine(" ",
                Modifier.toString(token.getModifiers() &
                        ~Modifier.INTERFACE &
                        ~Modifier.ABSTRACT &
                        ~Modifier.PROTECTED &
                        ~Modifier.STATIC),
                "class", token.getSimpleName() + "Impl", "implements", token.getCanonicalName(), "{");
    }

    /**
     * Generates all methods.
     *
     * @param token type token to create implementation for.
     * @return {@link String} with all methods.
     */
    private String createMethods(Class<?> token) {
        return Arrays.stream(token.getMethods())
                .map(this::getMethodSignature)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Generates {@link Method}.
     *
     * @param method {@link Method} to generate.
     * @return {@link String} with {@link Method}.
     */
    private String getMethodSignature(Method method) {
        String modifiers = Modifier.toString(method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
        String returnType = method.getReturnType().getCanonicalName();
        String methodName = method.getName();
        String methodExceptions = method.getExceptionTypes().length == 0 ? "" : createLine(" ",
                "throws", Arrays.stream(method.getExceptionTypes())
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(",")));
        String methodBody = createLine(" ", "return",
                getType(method.getReturnType()), ";", System.lineSeparator(), "}");
        String methodFullName = createLine(" ", modifiers, returnType,
                methodName, "(", getArguments(method), ")", methodExceptions, "{");
        return createLine(System.lineSeparator(), methodFullName, methodBody);
    }

    /**
     * Generates all argumets of {@link Method} separated by comma.
     *
     * @param method {@link Method} of arguments.
     * @return {@link String} with argumets.
     */
    private String getArguments(Method method) {
        index = 0;
        return Arrays.stream(method.getParameterTypes())
                .map(type -> createLine(" ", type.getCanonicalName(), "argument" + index++))
                .collect(Collectors.joining(","));
    }

    /**
     * Makes source code of default value.
     *
     * @param classToken {@link Class} to find default value of.
     * @return {@link String} of default value.
     */
    private String getType(Class<?> classToken) {
        if (!classToken.isPrimitive()) {
            return "null";
        } else if (classToken.equals(boolean.class)) {
            return "false";
        } else if (classToken.equals(void.class)) {
            return "";
        } else {
            return "0";
        }
    }

}
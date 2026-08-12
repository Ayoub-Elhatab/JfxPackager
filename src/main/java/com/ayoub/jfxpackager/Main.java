package com.ayoub.jfxpackager;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Main {

    /**
     * CLI entry point. Parses flags into an ExeBuilder config and runs
     * the build. Prints usage and exits if required flags are missing.
     *
     * @param args CLI arguments in "--flag value" form
     * @throws IOException if the build fails
     * @throws InterruptedException if a build subprocess is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, String> opts = parseArgs(args);

        Path jarPath = opts.containsKey("jar") ? Paths.get(opts.get("jar")) : autoDetectJar();
        if (jarPath == null) {
            System.err.println("No --jar given and no jar found in target/. Build your project first.");
            printUsage();
            return;
        }

        String mainClass = opts.containsKey("main") ? opts.get("main") : autoDetectMainClass(jarPath);
        if (mainClass == null) {
            System.err.println("No --main given and no Main-Class found in the jar's manifest.");
            printUsage();
            return;
        }

        if (!opts.containsKey("name")) {
            printUsage();
            return;
        }

        JfxPackager builder = new JfxPackager()
                .mainJar(jarPath.toString())
                .mainClass(mainClass)
                .appName(opts.get("name"))
                .type(opts.getOrDefault("type", "exe"));

        if (opts.containsKey("version")) {
            builder.appVersion(opts.get("version"));
        }
        if (opts.containsKey("modules")) {
            builder.javafxModules(opts.get("modules").split(","));
        }
        if (opts.containsKey("out")){
            builder.outputDir(Paths.get(opts.get("out")));
        }
        if (opts.containsKey("icon")){
            builder.icon(Paths.get(opts.get("icon")));
        }
        if (opts.containsKey("shortcut")) {
            builder.winShortcut(Boolean.parseBoolean(opts.get("shortcut")));
        }
        if (opts.containsKey("menu")) {
            builder.winMenu(Boolean.parseBoolean(opts.get("menu")));
        }
        if (opts.containsKey("dir-chooser")) {
            builder.winDirChooser(Boolean.parseBoolean(opts.get("dir-chooser")));
        }

        System.out.println("Building " + opts.get("name") + " ...");
        builder.build();
        System.out.println("Done. Output in " + opts.getOrDefault("out", "dist"));
    }

    /**
     * Parses CLI args into a map of flag names to values, expecting
     * "--flag value" pairs (e.g. "--jar app.jar" -> {"jar": "app.jar"}).
     *
     * @param args raw CLI arguments
     * @return map of flag name (without "--") to its value
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                opts.put(args[i].substring(2), args[i + 1]);
            }
        }
        return opts;
    }

    /**
     * Scans target/ for a single usable jar, skipping sources/javadoc
     * jars, so --jar can be omitted for typical Maven project layouts.
     *
     * @return the detected jar path, or null if none or multiple were found
     */
    private static Path autoDetectJar() {
        Path targetDir = Paths.get("target");
        if (!Files.isDirectory(targetDir)) {
            return null;
        }

        try (var stream = Files.list(targetDir)) {
            List<Path> candidates = stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("sources"))
                    .filter(p -> !p.getFileName().toString().contains("javadoc"))
                    .toList();
            return candidates.size() == 1 ? candidates.get(0) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads the Main-Class attribute from a jar's manifest, so --main
     * can be omitted when the jar was already built with one set.
     *
     * @param jarPath path to the jar to inspect
     * @return the fully qualified main class, or null if not found
     */
    private static String autoDetectMainClass(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return null;
            }
            return manifest.getMainAttributes().getValue("Main-Class");
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Prints CLI usage instructions, including required/optional flags
     * and an example command. Shown when required arguments are missing.
     */
    private static void printUsage() {
        System.out.println("""
            Usage:
              java -jar jfxpackager.jar --jar <path> --main <fqcn> --name <appName> [options]

            Required:
                --name      App name for the output exe
            
            Auto-detected if omitted:
                --jar       Path to built JavaFX app jar (auto-detected from target/ if only one jar exists)
                --main      Fully qualified main class (auto-detected from the jar's manifest if set)

            Optional:
              --modules   Comma-separated JavaFX modules (default: javafx.controls)
              --version   App version (default: 1.0)
              --type      exe | msi | app-image (default: exe)
              --out       Output directory (default: dist)
              --icon      Path to a .ico file for the app icon
              --shortcut  true|false — create a desktop shortcut (default: false)
              --menu      true|false — add a Start Menu entry (default: false)
              --dir-chooser  true|false — let the installer pick install dir (default: false)

            Example:
                java -jar jfxpackager.jar --jar target/myapp.jar --main com.example.Main --name MyApp --modules javafx.controls,javafx.fxml --icon icons/app.ico --shortcut true --menu true --type exe
            """);
    }
}

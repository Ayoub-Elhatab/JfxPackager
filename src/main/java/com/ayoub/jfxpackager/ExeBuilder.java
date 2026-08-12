package com.ayoub.jfxpackager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ExeBuilder {

    private String mainJar;
    private String mainClass;
    private String appName;
    private String appVersion = "1.0";
    private List<String> javafxModules = new ArrayList<>(List.of("javafx.controls"));
    private Path jfxModsPath;      // where javafx jmods live (bundled/cached)
    private Path wixBinPath;       // where candle.exe/light.exe live (bundled/cached)
    private Path outputDir = Paths.get("dist");
    private Path workDir = Paths.get("build-tmp");
    private Path iconPath;
    private String type = "exe";   // exe, msi, app-image

    private boolean winShortcut = false;
    private boolean winMenu = false;
    private boolean winDirChooser = false;

    public ExeBuilder mainJar(String path) {
        this.mainJar = path;
        return this;
    }

    public ExeBuilder mainClass(String cls) {
        this.mainClass = cls;
        return this;
    }

    public ExeBuilder appName(String name) {
        this.appName = name; return this;
    }

    public ExeBuilder appVersion(String v) {
        this.appVersion = v; return this;
    }

    private boolean modulesExplicitlySet = false;

    public ExeBuilder javafxModules(String... mods) {
        if (!modulesExplicitlySet) { this.javafxModules.clear(); modulesExplicitlySet = true; }
        this.javafxModules.addAll(Arrays.asList(mods));
        return this;
    }

    public ExeBuilder jfxModsPath(Path p) {
        this.jfxModsPath = p;
        return this;
    }

    public ExeBuilder wixBinPath(Path p) {
        this.wixBinPath = p;
        return this;
    }

    public ExeBuilder outputDir(Path p) {
        this.outputDir = p;
        return this;
    }

    public ExeBuilder type(String t) {
        this.type = t;
        return this;
    }

    public ExeBuilder icon(Path p) {
        this.iconPath = p;
        return this;
    }

    public ExeBuilder winShortcut(boolean b) {
        this.winShortcut = b;
        return this;
    }

    public ExeBuilder winMenu(boolean b) {
        this.winMenu = b;
        return this;
    }

    public ExeBuilder winDirChooser(boolean b) {
        this.winDirChooser = b;
        return this;
    }

    /**
     * Builds the packaged app from start to finish: resolves required
     * dependencies (javafx-jmods, WiX), validates the config, then runs
     * jlink followed by jpackage to produce the final exe/msi/app-image.
     *
     * @throws IOException if any file or subprocess step fails
     * @throws InterruptedException if a subprocess is interrupted
     */
    public void build() throws IOException, InterruptedException {
        resolveDependencies();
        validate();

        Files.createDirectories(workDir);
        Files.createDirectories(outputDir);

        Path runtimeImage = workDir.resolve("runtime");
        jlink(runtimeImage);
        jpackage(runtimeImage);
    }

    /**
     * Auto-resolves required dependency paths that weren't explicitly
     * set: downloads/caches javafx-jmods always, and WiX only when
     * building an exe or msi (app-image doesn't need it).
     *
     * @throws IOException if a dependency download or extraction fails
     * @throws InterruptedException if a dependency download is interrupted
     */
    private void resolveDependencies() throws IOException, InterruptedException {
        DependencyManager deps = new DependencyManager();
        if (jfxModsPath == null) {
            jfxModsPath = deps.getJavaFxJmods();
        }

        if ((type.equals("msi") || type.equals("exe")) && wixBinPath == null) {
            wixBinPath = deps.getWixBinaries();
        }
    }

    /**
     * Checks that required config is present before building.
     * Fails fast with a clear message instead of letting jlink/jpackage
     * fail later with a cryptic subprocess error.
     *
     * @throws IllegalStateException if a required field or dependency is missing
     */
    private void validate() {
        if (mainJar == null || mainClass == null || appName == null)
            throw new IllegalStateException("mainJar, mainClass, appName are required");
        if (jfxModsPath == null || !Files.exists(jfxModsPath))
            throw new IllegalStateException("jfxModsPath missing — bundle/download javafx-jmods first");
        if (type.equals("msi") && (wixBinPath == null || !Files.exists(wixBinPath)))
            throw new IllegalStateException("wixBinPath missing — bundle/download WiX first for msi builds");
    }

    /**
     * Builds a minimal custom Java runtime image containing only the
     * modules the app needs (JavaFX + java.base), using jlink. This
     * trimmed runtime is what jpackage later bundles into the exe.
     *
     * @param outputImage where to write the generated runtime image
     */
    private void jlink(Path outputImage) throws IOException, InterruptedException {
        if(Files.exists(outputImage)) {
            deleteDir(outputImage);
        }

        List<String> cmd = new ArrayList<>(
                List.of(
                        "jlink",
                        "--module-path", jfxModsPath.toString() + File.pathSeparator + System.getProperty("java.home") + "/jmods",
                        "--add-modules", String.join(",", addJavaBase(javafxModules)),
                        "--output", outputImage.toString(),
                        "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2"
                )
        );
        run(cmd, null);
    }

    /**
     * Packages the app's jar and jlink runtime image into the final
     * output (exe, msi, or app-image) via jpackage, optionally with a
     * desktop shortcut, Start Menu entry, and install-dir chooser. For
     * exe/msi builds, temporarily adds the bundled WiX binaries to PATH
     * so jpackage can find candle/light without a system-wide WiX install.
     *
     * @param runtimeImage the jlink-built runtime to bundle into the app
     */
    private void jpackage(Path runtimeImage) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(
                List.of(
                        "jpackage",
                        "--type", type,
                        "--name", appName,
                        "--app-version", appVersion,
                        "--input", Paths.get(mainJar).getParent().toString(),
                        "--main-jar", Paths.get(mainJar).getFileName().toString(),
                        "--main-class", mainClass,
                        "--runtime-image", runtimeImage.toString(),
                        "--dest", outputDir.toString()
                )
        );

        if (iconPath != null) {
            cmd.add("--icon");
            cmd.add(iconPath.toString());
        }
        if (winShortcut){
            cmd.add("--win-shortcut");
        }
        if (winMenu){
            cmd.add("--win-menu");
        }
        if (winDirChooser){
            cmd.add("--win-dir-chooser");
        }

        Map<String, String> env = null;
        if (type.equals("msi") || type.equals("exe")) {
            env = new HashMap<>(System.getenv());
            // jpackage looks for candle/light on PATH; prepend our bundled WiX
            env.put("PATH", wixBinPath.toString() + File.pathSeparator + env.getOrDefault("PATH", ""));
        }
        run(cmd, env);

    }

    /**
     * Recursively deletes a directory and all its contents,
     * deepest files first. Logs (but doesn't throw) if a file
     * fails to delete, so cleanup continues regardless.
     *
     * @param dir directory to delete
     * @throws IOException if the directory tree cannot be walked
     */
    private void deleteDir(Path dir) throws IOException {
        try(var stream = Files.walk(dir)){
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try{
                    Files.delete(p);
                }catch(IOException e){
                    System.err.println("Could not delete " + p + ": " + e.getMessage());
                }
            });
        }
    }

    /**
     * Ensures java.base is included in the module list, since jlink
     * requires it explicitly even though every module depends on it.
     *
     * @param mods JavaFX/app modules to include
     * @return a new list with java.base added if it was missing
     */
    private List<String> addJavaBase(List<String> mods) {
        List<String> out = new ArrayList<>(mods);
        if (!out.contains("java.base")) out.add("java.base");
        return out;
    }

    /**
     * Runs an external command and waits for it to complete,
     * streaming its output live. Throws if the command exits
     * with a non-zero status.
     *
     * @param cmd the command and its arguments
     * @param env extra environment variables to add, or null for none
     */
    private void run(List<String> cmd, Map<String, String> env) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
        if(env != null) {
            pb.environment().putAll(env);
        }

        Process process = pb.start();
        int code = process.waitFor();
        if (code != 0){
            throw new RuntimeException("Command failed (" + code + "): " + String.join(" ", cmd));
        }
    }
}

package com.ayoub.jfxpackager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds all configuration for a packaging run (jar/main-class/name,
 * JavaFX modules, output type, icon, shortcuts, dependency paths, etc).
 * Pure data + fluent setters — the actual jlink/jpackage work lives
 * in {@link JfxPackager}.
 */
public class PackagerConfig {

    private String mainJar;
    private String mainClass;
    private String appName;
    private String appVersion = "1.0";
    private List<String> javafxModules = new ArrayList<>(List.of("javafx.controls"));
    private boolean modulesExplicitlySet = false;
    private Path jfxModsPath;      // where javafx jmods live (bundled/cached)
    private Path wixBinPath;       // where candle.exe/light.exe live (bundled/cached)
    private Path outputDir = Paths.get("dist");
    private Path workDir = Paths.get("build-tmp");
    private Path iconPath;
    private String type = "exe";   // exe, msi, app-image
    private String javafxVersion = "21.0.2";
    private boolean winShortcut = false;
    private boolean winMenu = false;
    private boolean winDirChooser = false;

    public PackagerConfig mainJar(String path) { this.mainJar = path; return this; }
    public PackagerConfig mainClass(String cls) { this.mainClass = cls; return this; }
    public PackagerConfig appName(String name) { this.appName = name; return this; }
    public PackagerConfig appVersion(String v) { this.appVersion = v; return this; }

    public PackagerConfig javafxModules(String... mods) {
        if (!modulesExplicitlySet) { this.javafxModules.clear(); modulesExplicitlySet = true; }
        this.javafxModules.addAll(Arrays.asList(mods));
        return this;
    }

    public PackagerConfig jfxModsPath(Path p) { this.jfxModsPath = p; return this; }
    public PackagerConfig wixBinPath(Path p) { this.wixBinPath = p; return this; }
    public PackagerConfig outputDir(Path p) { this.outputDir = p; return this; }
    public PackagerConfig type(String t) { this.type = t; return this; }
    public PackagerConfig icon(Path p) { this.iconPath = p; return this; }
    public PackagerConfig javafxVersion(String v) { this.javafxVersion = v; return this; }
    public PackagerConfig winShortcut(boolean b) { this.winShortcut = b; return this; }
    public PackagerConfig winMenu(boolean b) { this.winMenu = b; return this; }
    public PackagerConfig winDirChooser(boolean b) { this.winDirChooser = b; return this; }

    public String mainJar() { return mainJar; }
    public String mainClass() { return mainClass; }
    public String appName() { return appName; }
    public String appVersion() { return appVersion; }
    public List<String> javafxModules() { return javafxModules; }
    public Path jfxModsPath() { return jfxModsPath; }
    public Path wixBinPath() { return wixBinPath; }
    public Path outputDir() { return outputDir; }
    public Path workDir() { return workDir; }
    public Path iconPath() { return iconPath; }
    public String type() { return type; }
    public String javafxVersion() { return javafxVersion; }
    public boolean winShortcut() { return winShortcut; }
    public boolean winMenu() { return winMenu; }
    public boolean winDirChooser() { return winDirChooser; }
}
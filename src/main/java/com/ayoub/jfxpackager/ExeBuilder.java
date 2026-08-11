package com.ayoub.jfxpackager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public void build() {

    }
}

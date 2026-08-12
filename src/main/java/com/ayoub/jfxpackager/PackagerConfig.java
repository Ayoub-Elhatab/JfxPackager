package com.ayoub.jfxpackager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class PackagerConfig {

    private String mainJar;
    private String mainClass;
    private String appName;
    private String appVersion = "1.0";

    @Setter(AccessLevel.NONE)
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


    public PackagerConfig javafxModules(String... mods) {
        if (!modulesExplicitlySet) {
            this.javafxModules.clear();
            modulesExplicitlySet = true;
        }
        this.javafxModules.addAll(Arrays.asList(mods));
        return this;
    }

    public List<String> javafxModules() {
        return javafxModules;
    }
}
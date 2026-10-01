# jfxpackager

A small Java library + CLI that packages a JavaFX application into a native
Windows executable (`.exe`), Windows Installer package (`.msi`), or a plain
runnable app folder (`app-image`) — without you having to manually download
javafx-jmods, install WiX, or hand-type long `jlink`/`jpackage` commands
every time.

It wraps two steps :

1. **`jlink`** — builds a minimal custom Java runtime containing only the
   modules your app needs (JavaFX + `java.base`).
2. **`jpackage`** — bundles that runtime + your app's jar into the final
   `.exe`/`.msi`/app-image, using [WiX](https://wixtoolset.org/) under the
   hood for the installer formats.

Both javafx-jmods and WiX are downloaded automatically on first use and
cached locally (`~/.jfxpackager/cache/`) — no manual setup required after
that.

---

## Requirements

- **JDK 17+** on your machine, with `jlink` and `jpackage` available on
  `PATH` (both ship with the JDK itself since Java 14+).
- **Windows** — the current version only targets Windows (`exe`/`msi`/
  `app-image`). WiX is Windows-specific; Linux/macOS support isn't
  implemented yet.
- **Maven**, since jfxpackager is built and consumed as a Maven artifact.

---

## Building jfxpackager itself

```
mvn clean install
```

This compiles the library and installs it into your local Maven repo
(`~/.m2/repository/com/ayoub/jfxpackager/1.0/`), ready to be used as a
dependency in other projects.

---

## Using it in your JavaFX app

### 1. Add the dependency

**Local (default)** — after running `mvn clean install` in this project,
add the dependency in your JavaFX app's `pom.xml`:

```xml
<dependency>
    <groupId>com.ayoub</groupId>
    <artifactId>jfxpackager</artifactId>
    <version>1.0</version>
</dependency>
```

This pulls the jar from your local `~/.m2` repo — works only on the
machine where you ran `mvn install`.

**Via JitPack (optional)** — this repo is pushed to GitHub and
tagged (e.g. `v1.0.0`), any project can pull it directly from the tag
instead of relying on a local build:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Ayoub-Elhatab</groupId>
        <artifactId>jfxpackager</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

This is worth switching to once jfxpackager is used across more than one
project or machine — no need to re-clone/rebuild from source each time,
and it works the same way for anyone else with access to the repo.

### 2. Add the required plugins to your app's `pom.xml`

jfxpackager relies on a few things being true about your project's build —
these plugins make that happen:

```xml
<build>
    <plugins>

        <!-- Required: sets Main-Class in the jar's manifest so
             jfxpackager can auto-detect your app's entry point
             without needing --main on the command line. -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.4.1</version>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>com.example.MyApp</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>

        <!-- Optional but recommended: lets you run your JavaFX app
             directly during development with `mvn javafx:run`,
             handling the module path for you. Unrelated to packaging
             — just a dev convenience. -->
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>com.example.MyApp</mainClass>
            </configuration>
        </plugin>

        <!-- Required: this is how you actually invoke jfxpackager's
             CLI from inside your project via `mvn exec:java`. -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>com.ayoub.jfxpackager.Main</mainClass>
            </configuration>
        </plugin>

    </plugins>
</build>
```

**Why each one matters:**

- `maven-jar-plugin` — without a `Main-Class` manifest entry, jfxpackager's
  jar/main-class auto-detection has nothing to read, and you'll be forced
  to pass `--main` manually every time.
- `javafx-maven-plugin` — not required for packaging, but useful for
  running/testing your app during development.
- `exec-maven-plugin` — the actual bridge that lets you run jfxpackager's
  `Main` class from your project's own `mvn` command line.

### 3. Build your app's jar

```
mvn clean package
```

### 4. Run jfxpackager

```
mvn "exec:java" "-Dexec.mainClass=com.ayoub.jfxpackager.Main" "-Dexec.args=--name MyApp --modules javafx.controls,javafx.fxml --type exe"
```

Output lands in `dist/`.

---

## CLI flags

| Flag | Required? | Description |
|---|---|---|
| `--name` | Yes | App name used for the output exe/msi |
| `--jar` | No | Path to the built jar. Auto-detected from `target/` if exactly one jar exists there. |
| `--main` | No | Fully qualified main class. Auto-detected from the jar's manifest (`Main-Class`) if set via `maven-jar-plugin`. |
| `--modules` | No | Comma-separated JavaFX modules (default: `javafx.controls`) |
| `--version` | No | App version (default: `1.0`) |
| `--type` | No | `exe` \| `msi` \| `app-image` (default: `exe`) |
| `--out` | No | Output directory (default: `dist`) |
| `--icon` | No | Path to a `.ico` file for the app icon (Windows requires `.ico`, not `.png`) |
| `--javafx-version` | No | JavaFX version to package (default: `21.0.2`) |
| `--shortcut` | No | `true`/`false` — create a desktop shortcut |
| `--menu` | No | `true`/`false` — add a Start Menu entry |
| `--dir-chooser` | No | `true`/`false` — let the installer choose install directory |

### Example

```
mvn "exec:java" "-Dexec.mainClass=com.ayoub.jfxpackager.Main" "-Dexec.args=--jar target/myapp.jar --main com.example.Main --name MyApp --modules javafx.controls,javafx.fxml --icon icons/app.ico --shortcut true --menu true --type exe --javafx-version 21.0.2"
```

---

## Config file (`jfxpackager.properties`)

Instead of repeating long CLI flags every time, put a file named
`jfxpackager.properties` in your project's root directory:

```properties
name=MyApp
modules=javafx.controls,javafx.fxml
icon=icons/app.ico
type=exe
shortcut=true
menu=true
javafx-version=21.0.2
```

Then you can just run:

```
mvn "exec:java" "-Dexec.mainClass=com.ayoub.jfxpackager.Main"
```

Any flag you pass explicitly on the command line overrides the value from
the properties file.

---

## What gets cached, and where

First run downloads:

- **javafx-jmods** for the configured JavaFX version (~200MB) →
  `~/.jfxpackager/cache/javafx-jmods-<version>/`
- **WiX 3.11 binaries** → `~/.jfxpackager/cache/wix311/`

Both are reused on every subsequent build — nothing is re-downloaded unless
the cache folder is deleted or a different `--javafx-version` is requested.

---

## What's inside `build-tmp/` and `dist/`

- **`build-tmp/runtime/`** — the custom Java runtime image `jlink` builds
  (a mini JRE containing only the modules your app needs). This is an
  intermediate artifact consumed by `jpackage`; safe to delete after a
  build.
- **`dist/`** — the final output: your installable `.exe`/`.msi`, or the
  runnable app-image folder. This is what you actually ship.

---

## Project structure

```
com.ayoub.jfxpackager
├── Main.java              CLI entry point — parses args, loads config file,
│                           builds a PackagerConfig, runs the build
├── PackagerConfig.java     Holds all build configuration (jar, main class,
│                           modules, icon, output type, shortcuts, etc.)
├── JfxPackager.java        Executes the build: resolves dependencies,
│                           validates config, runs jlink then jpackage
└── DependencyManager.java  Downloads and caches javafx-jmods and WiX
```

---

## Known limitations

- **Windows only.** `exe`/`msi`/`app-image` types are all implemented
  against `jpackage` + WiX on Windows. macOS (`dmg`/`pkg`) and Linux
  (`deb`/`rpm`) aren't supported yet.
- **No progress reporting beyond raw console output.** `jlink`/`jpackage`
  output is streamed live to the console, but there's no structured
  progress bar or stage indicator.
- **WiX 3.11 specifically** — `jpackage` doesn't support WiX 4+ as of
  current JDK versions, so the bundled WiX is pinned to 3.11.

---

## Troubleshooting

**`AccessDeniedException` when writing to `dist/`** — usually a locked
file. Common culprits on Windows: OneDrive syncing the project folder,
a leftover `java.exe`/`candle.exe`/`light.exe` process, or Explorer/an
open installer window still holding a handle. Check with:

```
tasklist | findstr /i "java candle light onedrive msiexec"
```

kill whatever's listed, then retry. Longer term, keep build folders
(`target/`, `dist/`, `build-tmp/`) out of any OneDrive-synced path.

**Icon not applying / "specified icon file does not exist"** — the path
passed to `--icon` is relative to wherever you run the `mvn` command from
(your project root), not relative to the jfxpackager library.

---

## Author

Ayoub Elhatab — [LinkedIn](https://www.linkedin.com/in/ayoub-elhatab)

## License

MIT — see [LICENSE](LICENSE) for details.

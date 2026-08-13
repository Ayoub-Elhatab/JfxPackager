package com.ayoub.jfxpackager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and caches the external binaries jlink/jpackage need
 * but the JDK doesn't ship: javafx-jmods (per version) and WiX
 * binaries. Each is fetched once and reused from
 * ~/.jfxpackager/cache/ on subsequent calls.
 *
 * @author Ayoub Elhatab
 * LinkedIn: <a href="https://www.linkedin.com/in/ayoub-elhatab/">Ayoub Elhatab</a>
 */
public class DependencyManager {

    private static final Path CACHE_DIR = Paths.get(System.getProperty("user.home"), ".jfxpackager", "cache");

    private static final String WIX_URL = "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip";

    /**
     * Returns the local javafx-jmods folder for the given version,
     * downloading and extracting it on first use and reusing the
     * cached copy afterward. A ".complete" marker file confirms the
     * cache is fully extracted, so an interrupted download doesn't
     * leave a broken cache behind.
     *
     * @param version the JavaFX version to fetch, e.g. "21.0.2"
     * @return path to the folder containing the javafx .jmod files
     * @throws IOException if the download or extraction fails
     * @throws InterruptedException if the download is interrupted
     */
    public Path getJavaFxJmods(String version) throws IOException, InterruptedException {
        Path target = CACHE_DIR.resolve("javafx-jmods-" + version);
        Path marker = target.resolve(".complete");
        if (Files.exists(marker)) {
            return target;
        }
        Files.createDirectories(target);

        String jfxUrl = "https://download2.gluonhq.com/openjfx/" + version + "/openjfx-" + version + "_windows-x64_bin-jmods.zip";
        Path zip = CACHE_DIR.resolve("javafx-jmods.zip");
        download(jfxUrl, zip);
        unzip(zip, target, true); // strip top-level folder (jmods/*.jmod)

        Files.deleteIfExists(zip);
        Files.createFile(marker);

        return target;
    }

    /**
     * Returns the local WiX binaries folder, downloading and extracting
     * it on first use and reusing the cached copy afterward. A ".complete"
     * marker file confirms the cache is fully extracted, so an interrupted
     * download doesn't leave a broken cache behind.
     *
     * @return path to the folder containing candle.exe/light.exe
     * @throws IOException if the download or extraction fails
     * @throws InterruptedException if the download is interrupted
     */
    public Path getWixBinaries() throws IOException, InterruptedException {
        Path target = CACHE_DIR.resolve("wix311");
        Path marker = target.resolve(".complete");
        if (Files.exists(marker)) {
            return target;
        }
        Files.createDirectories(target);

        Path zip = CACHE_DIR.resolve("wix.zip");
        download(WIX_URL, zip);
        unzip(zip, target, false); // wix zip is flat, no stripping needed

        Files.deleteIfExists(zip);
        Files.createFile(marker);

        return target;
    }

    /**
     * Downloads a file from a URL directly to disk, following redirects
     * (needed since GitHub release links redirect to S3).
     *
     * @param url  the file to download
     * @param dest where to save it
     * @throws IOException if the download fails or returns a non-200 status
     * @throws InterruptedException if the download is interrupted
     */
    private void download(String url, Path dest) throws IOException, InterruptedException {
        System.out.println("Downloading " + url + " ...");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(dest));

        if(response.statusCode() != 200){
            throw new IOException("Download failed (" + response.statusCode() + "): " + url);
        }
    }

    /**
     * Extracts a zip file to a destination directory.
     * <p>
     * If stripTopLevel is true, removes the outermost folder some zips
     * wrap their contents in (e.g. "javafx-jmods-21/x.jmod" -> "x.jmod").
     * Guards against zip entries that try to write outside destDir.
     *
     * @param zipFile the zip archive to extract
     * @param destDir where to extract its contents
     * @param stripTopLevel whether to drop the zip's outermost folder
     * @throws IOException if extraction fails or an entry escapes destDir
     */
    private void unzip(Path zipFile, Path destDir, boolean stripTopLevel) throws IOException{
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (stripTopLevel) {
                    int slash = name.indexOf('/');
                    if (slash < 0) continue;
                    name = name.substring(slash + 1);
                    if (name.isEmpty()) continue;
                }
                Path outPath = destDir.resolve(name).normalize();
                if (!outPath.startsWith(destDir))
                    throw new IOException("Zip entry outside target dir: " + name);

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        zis.transferTo(os);
                    }
                }
            }
        }
    }
}

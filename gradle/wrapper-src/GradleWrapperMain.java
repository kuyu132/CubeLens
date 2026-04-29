package org.gradle.wrapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Minimal Gradle wrapper implementation to avoid needing a prebuilt gradle-wrapper.jar in this sandbox.
 * Reads gradle/wrapper/gradle-wrapper.properties, downloads the distribution, unzips it, then execs Gradle.
 */
public final class GradleWrapperMain {
  private static final String PROPS_PATH = "gradle/wrapper/gradle-wrapper.properties";

  public static void main(String[] args) throws Exception {
    Path projectDir = Paths.get("").toAbsolutePath();
    Path propsPath = projectDir.resolve(PROPS_PATH);
    if (!Files.exists(propsPath)) {
      System.err.println("Missing " + PROPS_PATH + " (run from project root)");
      System.exit(1);
      return;
    }

    Properties props = new Properties();
    try (InputStream in = new FileInputStream(propsPath.toFile())) {
      props.load(in);
    }
    String distributionUrl = Objects.toString(props.getProperty("distributionUrl"), "").trim();
    if (distributionUrl.isEmpty()) {
      System.err.println("distributionUrl missing in " + PROPS_PATH);
      System.exit(1);
      return;
    }

    Duration networkTimeout = parseTimeout(props.getProperty("networkTimeout"));
    Path gradleExe = ensureGradleDistribution(projectDir, distributionUrl, networkTimeout);
    int exit = execGradle(gradleExe, args);
    System.exit(exit);
  }

  private static Duration parseTimeout(String msStr) {
    if (msStr == null || msStr.isBlank()) return Duration.ofSeconds(10);
    try {
      long ms = Long.parseLong(msStr.trim());
      return Duration.ofMillis(ms);
    } catch (NumberFormatException e) {
      return Duration.ofSeconds(10);
    }
  }

  private static Path ensureGradleDistribution(Path projectDir, String url, Duration networkTimeout)
    throws Exception {
    URI uri = URI.create(url.replace("\\:", ":").replace("\\/", "/"));
    String fileName = Paths.get(uri.getPath()).getFileName().toString();
    if (!fileName.endsWith(".zip")) {
      throw new IllegalArgumentException("Unsupported distributionUrl (expected .zip): " + url);
    }
    String distName = fileName.substring(0, fileName.length() - 4);
    String urlHash = sha256Hex(url);

    Path gradleUserHome = resolveGradleUserHome();
    Path distDir = gradleUserHome.resolve("wrapper")
      .resolve("dists")
      .resolve(distName)
      .resolve(urlHash);
    Files.createDirectories(distDir);

    Path zipPath = distDir.resolve(fileName);
    if (!Files.exists(zipPath)) {
      download(uri, zipPath, networkTimeout);
    }

    Path extractedRoot = findGradleRootDir(distDir);
    if (extractedRoot == null) {
      unzip(zipPath, distDir);
      extractedRoot = findGradleRootDir(distDir);
    }

    if (extractedRoot == null) {
      throw new IllegalStateException("Failed to locate extracted Gradle distribution in " + distDir);
    }

    Path gradleExe = extractedRoot.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle");
    if (!Files.exists(gradleExe)) {
      throw new IllegalStateException("Missing gradle executable: " + gradleExe);
    }
    if (!isWindows()) {
      gradleExe.toFile().setExecutable(true);
    }
    return gradleExe;
  }

  private static Path resolveGradleUserHome() {
    String env = System.getenv("GRADLE_USER_HOME");
    if (env != null && !env.isBlank()) return Paths.get(env);
    return Paths.get(System.getProperty("user.home")).resolve(".gradle");
  }

  private static void download(URI uri, Path outPath, Duration networkTimeout) throws Exception {
    System.out.println("Downloading Gradle distribution...");
    System.out.println("  " + uri);

    Path tmp = outPath.resolveSibling(outPath.getFileName().toString() + ".part");
    Files.deleteIfExists(tmp);

    HttpClient client = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(networkTimeout)
      .build();

    HttpRequest request = HttpRequest.newBuilder(uri)
      .timeout(networkTimeout)
      .GET()
      .build();

    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Failed to download " + uri + " (HTTP " + response.statusCode() + ")");
    }

    long total = response.headers().firstValueAsLong("content-length").orElse(-1);
    try (InputStream in = new BufferedInputStream(response.body());
         OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {
      byte[] buf = new byte[1024 * 64];
      long done = 0;
      long lastPrint = 0;
      int read;
      while ((read = in.read(buf)) >= 0) {
        out.write(buf, 0, read);
        done += read;
        if (total > 0 && done - lastPrint > (1024L * 1024L * 5L)) {
          lastPrint = done;
          System.out.println("  downloaded " + (done / (1024 * 1024)) + " / " + (total / (1024 * 1024)) + " MB");
        }
      }
    }
    try {
      Files.move(tmp, outPath);
    } catch (FileAlreadyExistsException e) {
      Files.deleteIfExists(tmp);
    }
  }

  private static void unzip(Path zipPath, Path destDir) throws IOException {
    System.out.println("Unzipping Gradle distribution...");
    try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        Path out = destDir.resolve(entry.getName()).normalize();
        if (!out.startsWith(destDir)) {
          throw new IOException("Zip entry escapes target dir: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(out);
        } else {
          Files.createDirectories(out.getParent());
          try (OutputStream outStream = new BufferedOutputStream(Files.newOutputStream(out))) {
            zin.transferTo(outStream);
          }
          if (!isWindows() && out.getFileName().toString().equals("gradle")) {
            out.toFile().setExecutable(true);
          }
        }
      }
    }
  }

  private static Path findGradleRootDir(Path distDir) throws IOException {
    try (Stream<Path> s = Files.list(distDir)) {
      return s.filter(Files::isDirectory)
        .filter(p -> p.getFileName().toString().startsWith("gradle-"))
        .findFirst()
        .orElse(null);
    }
  }

  private static int execGradle(Path gradleExe, String[] args) throws IOException, InterruptedException {
    ProcessBuilder pb;
    if (isWindows()) {
      pb = new ProcessBuilder(gradleExe.toString());
    } else {
      pb = new ProcessBuilder(gradleExe.toString());
    }
    for (String arg : args) {
      pb.command().add(arg);
    }
    pb.inheritIO();
    Process p = pb.start();
    return p.waitFor();
  }

  private static String sha256Hex(String input) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}


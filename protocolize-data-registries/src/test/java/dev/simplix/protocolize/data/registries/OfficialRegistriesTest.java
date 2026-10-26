package dev.simplix.protocolize.data.registries;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplix.protocolize.api.util.ProtocolVersions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Verifies the committed registries/items JSON against the official Minecraft data generator.
 * <p>
 * For every release in {@link ProtocolVersions#OFFICIAL_RELEASES} the official server jar is downloaded from Mojang
 * (SHA-1 checked) and the data generator is run with the JDK that the release requires. Every committed file of the
 * protocol folder must equal the generated file. {@code extended.json} is skipped because the generator does not
 * produce it.
 * <p>
 * Properties:
 * <ul>
 *     <li>{@code official.versions}: optional comma separated protocol numbers or release ids to run</li>
 *     <li>{@code official.java.<major>}: JDK home for Java major version 8, 16, 17, 21 or 25 when the running JDK differs</li>
 *     <li>{@code official.refresh=true}: overwrite the committed files that differ with the official output instead of failing</li>
 * </ul>
 * Every run writes the exact JSON differences of each release to {@code target/official-verification/<release>.txt}
 * ({@code -} committed only, {@code +} official only, {@code ~} changed). HTTP requests are retried with backoff on
 * I/O errors (HTTP/2 GOAWAY), HTTP 429 and HTTP 5xx.
 * The test is excluded from the default build. Run it with {@code -Pofficial-verification}. Downloaded jars and
 * generated files are deleted after the run and are never committed or packaged.
 */
@RunWith(Parameterized.class)
public class OfficialRegistriesTest {

    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Path WORK_DIR = Paths.get("target", "official-data");
    private static final Path REGISTRIES_DIR = Paths.get(System.getProperty("basedir", "."), "src", "main", "resources", "registries");
    private static final boolean REFRESH = Boolean.getBoolean("official.refresh");
    private static final long DATAGEN_TIMEOUT_MINUTES = 15;
    private static final int HTTP_ATTEMPTS = 6;
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static JsonObject manifest;

    private final int protocol;
    private final String release;

    public OfficialRegistriesTest(int protocol, String release) {
        this.protocol = protocol;
        this.release = release;
    }

    @Parameterized.Parameters(name = "{1} (protocol {0})")
    public static Collection<Object[]> versions() {
        String filter = System.getProperty("official.versions", "").trim();
        Set<String> wanted = filter.isEmpty() ? null
            : Arrays.stream(filter.split(",")).map(String::trim).collect(Collectors.toSet());
        List<Object[]> versions = new ArrayList<>();
        ProtocolVersions.OFFICIAL_RELEASES.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
            .forEach(entry -> {
                String release = entry.getKey();
                int protocol = entry.getValue();
                if (wanted == null || wanted.contains(String.valueOf(protocol)) || wanted.contains(release)) {
                    versions.add(new Object[]{protocol, release});
                }
            });
        return versions;
    }

    @Test
    public void committedDataMatchesOfficialRelease() throws Exception {
        Path releaseDir = Files.createDirectories(WORK_DIR.resolve(release));
        JsonObject version = fetchVersionJson();
        JsonObject server = version.getAsJsonObject("downloads").getAsJsonObject("server");
        Path jar = releaseDir.resolve("server.jar");
        download(server.get("url").getAsString(), server.get("sha1").getAsString(), jar);
        assertDeclaredProtocol(jar);

        Path committedDir = REGISTRIES_DIR.resolve(String.valueOf(protocol));
        List<String> committedFiles = listCommittedFiles(committedDir);
        Assert.assertFalse("No committed data in " + committedDir, committedFiles.isEmpty());

        int javaMajor = version.has("javaVersion")
            ? version.getAsJsonObject("javaVersion").get("majorVersion").getAsInt() : 8;
        Path outDir = releaseDir.resolve("out");
        runDatagen(jar, javaMajor, releaseDir.resolve("run"), outDir);

        List<String> problems = new ArrayList<>();
        List<String> report = new ArrayList<>();
        Path generatedReports = outDir.resolve("reports");
        for (String file : committedFiles) {
            Path generated = generatedReports.resolve(file);
            Path committed = committedDir.resolve(file);
            if (!Files.isRegularFile(generated)) {
                problems.add(file + ": not generated by the official data generator");
                report.add("== " + file + ": not generated");
                report.add("   reports: " + listing(generatedReports));
                report.add("   datagen log tail:");
                report.add("    " + tail(releaseDir.resolve("run").resolve("datagen.log")));
                continue;
            }
            JsonElement officialJson = readJson(generated);
            JsonElement committedJson = readJson(committed);
            if (committedJson.equals(officialJson)) {
                continue;
            }
            List<String> differences = new ArrayList<>();
            describeDifferences("$", committedJson, officialJson, differences);
            report.add("== " + file + ": " + differences.size() + " difference(s) (- committed only, + official only, ~ changed)");
            report.addAll(differences);
            if (REFRESH) {
                Files.copy(generated, committed, StandardCopyOption.REPLACE_EXISTING);
                report.add("   refreshed from the official output");
            } else {
                problems.add(file + ": differs from the official " + release + " data");
            }
        }
        Path reportFile = Paths.get("target", "official-verification", release + ".txt");
        Files.createDirectories(reportFile.getParent());
        Files.write(reportFile, report.isEmpty()
            ? List.of(release + " (protocol " + protocol + "): no differences") : report);
        Assert.assertTrue(release + " (protocol " + protocol + ") does not match the official data, details in "
            + reportFile.toAbsolutePath() + ":\n  " + String.join("\n  ", problems), problems.isEmpty());
    }

    @AfterClass
    public static void deleteDownloads() throws Exception {
        if (Files.exists(WORK_DIR)) {
            try (Stream<Path> paths = Files.walk(WORK_DIR)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static List<String> listCommittedFiles(Path dir) throws Exception {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(path -> path.getFileName().toString())
                .filter(name -> !name.equals("extended.json"))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    private JsonObject fetchVersionJson() throws Exception {
        if (manifest == null) {
            manifest = getJson(MANIFEST_URL);
        }
        for (JsonElement entry : manifest.getAsJsonArray("versions")) {
            JsonObject object = entry.getAsJsonObject();
            if (object.get("id").getAsString().equals(release)) {
                return getJson(object.get("url").getAsString());
            }
        }
        throw new AssertionError(release + " is not listed in the official version manifest");
    }

    private static JsonObject getJson(String url) throws Exception {
        return JsonParser.parseString(get(url)).getAsJsonObject();
    }

    private static String get(String url) throws Exception {
        HttpResponse<String> response = sendWithRetry(HttpRequest.newBuilder(URI.create(url)).build(),
            HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("GET " + url + " returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static void download(String url, String expectedSha1, Path target) throws Exception {
        if (Files.exists(target) && sha1(target).equals(expectedSha1)) {
            return;
        }
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        HttpResponse<Path> response = sendWithRetry(HttpRequest.newBuilder(URI.create(url)).build(),
            HttpResponse.BodyHandlers.ofFile(partial, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("GET " + url + " returned HTTP " + response.statusCode());
        }
        Assert.assertEquals("SHA-1 of " + url, expectedSha1, sha1(partial));
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sha1(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Checks {@code protocol_version} of the server's {@code version.json}. Bundled (26.x) server jars keep the
     * version file inside the nested server jar. Jars without a version file fall back to
     * {@link #declaresProtocolNextToRelease}.
     */
    private void assertDeclaredProtocol(Path jar) throws Exception {
        OptionalInt declared = declaredProtocol(jar);
        if (declared.isPresent()) {
            Assert.assertEquals("protocol_version declared by the " + release + " server jar",
                protocol, declared.getAsInt());
            return;
        }
        Assert.assertTrue(release + ": no version.json and no protocol " + protocol + " next to the release name",
            declaresProtocolNextToRelease(jar, release, protocol));
    }

    /**
     * Targeted fallback for jars without {@code version.json}: only classes that contain the exact release-name
     * constant are inspected, and one of them must push the protocol number as a {@code sipush} operand.
     */
    private static boolean declaresProtocolNextToRelease(Path jar, String release, int protocol) throws Exception {
        byte[] name = release.getBytes(StandardCharsets.UTF_8);
        byte[] utf8Constant = ByteBuffer.allocate(3 + name.length)
            .put((byte) 1).putShort((short) name.length).put(name).array();
        byte[] sipush = {(byte) 0x11, (byte) (protocol >> 8), (byte) protocol};
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }
                if (indexOf(bytes, utf8Constant) >= 0 && indexOf(bytes, sipush) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static OptionalInt declaredProtocol(Path jar) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry direct = zip.getEntry("version.json");
            if (direct != null) {
                try (InputStream in = zip.getInputStream(direct)) {
                    return protocolOf(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            if (zip.getEntry("META-INF/versions.list") != null) {
                for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("META-INF/versions/") && entry.getName().endsWith(".jar")) {
                        try (ZipInputStream nested = new ZipInputStream(zip.getInputStream(entry))) {
                            for (ZipEntry inner = nested.getNextEntry(); inner != null; inner = nested.getNextEntry()) {
                                if (inner.getName().equals("version.json")) {
                                    return protocolOf(new String(nested.readAllBytes(), StandardCharsets.UTF_8));
                                }
                            }
                        }
                    }
                }
            }
        }
        return OptionalInt.empty();
    }

    private static OptionalInt protocolOf(String versionJson) {
        JsonObject object = JsonParser.parseString(versionJson).getAsJsonObject();
        return object.has("protocol_version")
            ? OptionalInt.of(object.get("protocol_version").getAsInt())
            : OptionalInt.empty();
    }

    private void runDatagen(Path jar, int javaMajor, Path runDir, Path outDir) throws Exception {
        Files.createDirectories(runDir);
        List<String> command = new ArrayList<>();
        command.add(javaExecutable(javaMajor).toString());
        if (hasEntry(jar, "META-INF/versions.list")) {
            command.addAll(List.of("-DbundlerMainClass=net.minecraft.data.Main", "-jar", jar.toAbsolutePath().toString()));
        } else {
            command.addAll(List.of("-cp", jar.toAbsolutePath().toString(), "net.minecraft.data.Main"));
        }
        command.addAll(List.of("--reports", "--output", outDir.toAbsolutePath().toString()));

        Path log = runDir.resolve("datagen.log");
        Process process = new ProcessBuilder(command)
            .directory(runDir.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
        if (!process.waitFor(DATAGEN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            Assert.fail(release + ": data generator timed out, see " + log);
        }
        if (process.exitValue() != 0) {
            List<String> lines = Files.readAllLines(log);
            String tail = String.join("\n", lines.subList(Math.max(0, lines.size() - 20), lines.size()));
            Assert.fail(release + ": data generator exited with " + process.exitValue() + "\n" + tail);
        }
    }

    private static Path javaExecutable(int javaMajor) {
        if (Runtime.version().feature() == javaMajor) {
            return javaIn(Paths.get(System.getProperty("java.home")));
        }
        String home = System.getProperty("official.java." + javaMajor);
        Assert.assertNotNull("Minecraft needs Java " + javaMajor + ": pass -Dofficial.java." + javaMajor + "=<JDK home>", home);
        return javaIn(Paths.get(home));
    }

    private static Path javaIn(Path home) {
        Path windows = home.resolve("bin").resolve("java.exe");
        return Files.exists(windows) ? windows : home.resolve("bin").resolve("java");
    }

    private static boolean hasEntry(Path jar, String name) throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.getEntry(name) != null;
        }
    }

    private static JsonElement readJson(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file));
    }

    /**
     * Retries I/O errors (HTTP/2 GOAWAY resets), HTTP 429 and HTTP 5xx with exponential backoff.
     */
    private static <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler)
        throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                HttpResponse<T> response = HTTP.send(request, handler);
                if ((response.statusCode() != 429 && response.statusCode() < 500) || attempt >= HTTP_ATTEMPTS) {
                    return response;
                }
                System.err.println("HTTP " + response.statusCode() + " from " + request.uri() + ", attempt " + attempt);
            } catch (IOException e) {
                if (attempt >= HTTP_ATTEMPTS) {
                    throw e;
                }
                System.err.println("I/O error from " + request.uri() + ", attempt " + attempt + ": " + e.getMessage());
            }
            Thread.sleep(2_000L * (1L << (attempt - 1)));
        }
    }

    /**
     * Lists the exact differences: {@code -} path only in the committed file, {@code +} path only in the official
     * output, {@code ~} value changed. Object keys are compared by name, array entries by index.
     */
    private static void describeDifferences(String path, JsonElement committed, JsonElement official, List<String> out) {
        if (committed.isJsonObject() && official.isJsonObject()) {
            JsonObject a = committed.getAsJsonObject();
            JsonObject b = official.getAsJsonObject();
            Set<String> keys = new TreeSet<>(a.keySet());
            keys.addAll(b.keySet());
            for (String key : keys) {
                String child = path + "." + key;
                if (!a.has(key)) {
                    out.add("+ " + child + ": " + b.get(key));
                } else if (!b.has(key)) {
                    out.add("- " + child + ": " + a.get(key));
                } else {
                    describeDifferences(child, a.get(key), b.get(key), out);
                }
            }
        } else if (committed.isJsonArray() && official.isJsonArray()) {
            JsonArray a = committed.getAsJsonArray();
            JsonArray b = official.getAsJsonArray();
            for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
                String child = path + "[" + i + "]";
                if (i >= a.size()) {
                    out.add("+ " + child + ": " + b.get(i));
                } else if (i >= b.size()) {
                    out.add("- " + child + ": " + a.get(i));
                } else {
                    describeDifferences(child, a.get(i), b.get(i), out);
                }
            }
        } else if (!committed.equals(official)) {
            out.add("~ " + path + ": committed " + committed + ", official " + official);
        }
    }

    private static String listing(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) {
            return "(missing)";
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(path -> path.getFileName().toString()).sorted().collect(Collectors.joining(", ", "[", "]"));
        }
    }

    private static String tail(Path log) throws Exception {
        if (!Files.isRegularFile(log)) {
            return "(no datagen log)";
        }
        List<String> lines = Files.readAllLines(log);
        return String.join("\n    ", lines.subList(Math.max(0, lines.size() - 20), lines.size()));
    }

}

package scripting.builder.jdk.download;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import utility.log.EngineLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

final class DiscoClient {
    private static final EngineLog Logger = new EngineLog(DiscoClient.class);
    private static final String BaseURL = "https://api.foojay.io/disco/v3.0";
    private static final String PackageType = "jdk";
    private static final long ConnectTimeoutSeconds = 20L;
    private static final long RequestTimeoutSeconds = 30L;
    private static final ObjectMapper Mapper = new ObjectMapper().rebuild().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final HttpClient Client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(ConnectTimeoutSeconds)).followRedirects(HttpClient.Redirect.NORMAL).build();

    private record PackagesResponse(List<DiscoPackage> result) {}
    private record InfoResponse(List<PackageInfo> result) {}

    private DiscoClient() {}

    /**
     * List tje JDK packages that can be downloaded directly for the given host platform and feature version.
     * @param distribution the foojay distribution token, such as {@code temurin}
     * @param os the foojay operating system token, either {@code windows}, {@code macos} or {@code linux}
     * @param arch the foojay architecture token, either {@code x64 or {@code aarch64}}
     * @param archiveType the archive format, either {@code zip} or {@code tar.gz}
     * @param version the required Java feature version
     * @return the matching packages or an empty list on any failure
     */
    static List<DiscoPackage> query(String distribution, String os, String arch, String archiveType, int version) {
        String url = String.format(
                "%s/packages?distribution=%s&package_type=%s&version=%d&latest=available&directly_downloadable=true&operating_system=%s&architecture=%s&archive_type=%s",
                BaseURL, distribution, PackageType, version, os, arch, archiveType);
        PackagesResponse response = get(url, PackagesResponse.class);
        return response == null || response.result == null ? List.of() : response.result;
    }

    /**
     * Resolve a package iid to its direct download detail.
     * @param id the package id from a {@link DiscoPackage}
     * @return the download detail, or null when it can't be resolved
     */
    static PackageInfo resolve(String id) {
        InfoResponse response = get(BaseURL + "/ids/" + id, InfoResponse.class);
        if (response == null || response.result == null || response.result.isEmpty()) return null;
        return response.result.getFirst();
    }

    /**
     * Open a GET stream for a resolved download URL, reusing the shared client with its redirect / TLS policy.
     * No request timeout is set so a large archive transfer is not aborted halfway. Connection timeout still applied.
     * @param url the direct download URL
     * @return the HTTP response with an {@link InputStream} body
     * @throws IOException if an IO error had occurred during receiving or the client had shutdown
     * @throws InterruptedException if the operation is interrupted
     */
    static HttpResponse<InputStream> openStream(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/octet-stream")
                .GET().build();
        return Client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private static <T> T get(String url, Class<T> type) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(RequestTimeoutSeconds))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = Client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) return Mapper.readValue(response.body(), type);
            Logger.warning(String.format("foojay request failed (HTTP %d): %s", response.statusCode(), url));
            return null;
        } catch (IOException e) {
            Logger.warning("foojay request error: " + e.getMessage());
            return null;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}

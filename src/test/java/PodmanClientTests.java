import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.TweakedDockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PodmanClientTests {

    public static final String PODMAN_SOCK = System.getenv().getOrDefault("DOCKER_URI", "unix:///run/user/1000/podman/podman.sock");

    @Test
    public void testZerodepPodmanIssue() throws InterruptedException {
        ZerodepDockerHttpClient client = new ZerodepDockerHttpClient.Builder().dockerHost(URI.create(PODMAN_SOCK)).maxConnections(Integer.MAX_VALUE).build();
        // podman idle timeout 10 seconds +1
        testRequestsWithDelay(client, 11000);
    }

    @Test
    public void testTweakedFailsIfStaleCheckGreaterThanPodmanIdleTimeoutAndDelay() throws InterruptedException {
        TimeValue validateAfterInactivity = TimeValue.ofSeconds(15);
        DockerHttpClient client = new TweakedDockerHttpClient(URI.create(PODMAN_SOCK), null, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, validateAfterInactivity, null);
        // podman idle timeout 10 seconds +1
        testRequestsWithDelay(client, 11000);
    }


    @Test
    public void testTweakedWithConnectionStaleCheckSucceedsWhenStaleCheckLessThanPodmanIdleTimeout() throws InterruptedException {
        TimeValue validateAfterInactivity = TimeValue.ofSeconds(8);
        DockerHttpClient client = new TweakedDockerHttpClient(URI.create(PODMAN_SOCK), null, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, validateAfterInactivity, null);
        // podman idle timeout 10 seconds +1
        testRequestsWithDelay(client, 11000);
    }

    @Test
    public void testTweakedWithConnectionKeepaliveLessThanPodmanIdleTimeout() throws InterruptedException {
        Duration keepalive = Duration.ofSeconds(8);
        DockerHttpClient client = new TweakedDockerHttpClient(URI.create(PODMAN_SOCK), null, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, null, keepalive);
        // podman idle timeout 10 seconds +1
        testRequestsWithDelay(client, 11000);
    }

    @Test
    public void testTweakedWithConnectionKeepaliveGreaterThanPodmanIdleTimeoutAndDelay() throws InterruptedException {
        Duration keepalive = Duration.ofSeconds(15);
        DockerHttpClient client = new TweakedDockerHttpClient(URI.create(PODMAN_SOCK), null, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, null, keepalive);
        // podman idle timeout 10 seconds +1
        testRequestsWithDelay(client, 11000);
    }

    private static void testRequestsWithDelay(DockerHttpClient client, int delayMillis) throws InterruptedException {
        sendAsyncVersionRequestsExpect200(client, 10);
        Thread.sleep(delayMillis);
        sendAsyncVersionRequestsExpect200(client, 1);
    }

    private static void sendAsyncVersionRequestsExpect200(DockerHttpClient client, int endExclusive) {
        var futures = IntStream.range(0, endExclusive).boxed().map(integer -> requestVersions(client)).toList();
        futures.forEach(responseCompletableFuture -> {
            try (DockerHttpClient.Response join = responseCompletableFuture.join()) {
                assertEquals(200, join.getStatusCode());
                join.getBody().readAllBytes();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    private static CompletableFuture<DockerHttpClient.Response> requestVersions(DockerHttpClient client) {
        return CompletableFuture.supplyAsync(() -> client.execute(DockerHttpClient.Request.builder().method(DockerHttpClient.Request.Method.GET).path("/version").build()));
    }
}

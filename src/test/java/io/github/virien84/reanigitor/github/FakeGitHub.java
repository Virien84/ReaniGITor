package io.github.virien84.reanigitor.github;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A tiny local HTTP server that answers like the GitHub REST API, for tests. */
public final class FakeGitHub implements AutoCloseable {

    record Reply(int status, String body, Map<String, String> headers) {
    }

    public record Request(String method, String pathAndQuery, String accept, String authorization) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new HashMap<>();
    public final List<Request> requests = new ArrayList<>();

    public FakeGitHub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String target = exchange.getRequestURI().toString();
            synchronized (this) {
                requests.add(new Request(exchange.getRequestMethod(), target,
                        exchange.getRequestHeaders().getFirst("Accept"),
                        exchange.getRequestHeaders().getFirst("Authorization")));
            }
            Reply reply = replies.getOrDefault(target, new Reply(404, "{\"message\":\"Not Found\"}", Map.of()));
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            reply.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            exchange.sendResponseHeaders(reply.status(), body.length == 0 ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    public FakeGitHub on(String pathAndQuery, int status, String body) {
        return on(pathAndQuery, status, body, Map.of());
    }

    public FakeGitHub on(String pathAndQuery, int status, String body, Map<String, String> headers) {
        replies.put(pathAndQuery, new Reply(status, body, headers));
        return this;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}

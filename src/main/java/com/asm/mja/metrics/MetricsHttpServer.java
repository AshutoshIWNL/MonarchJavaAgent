package com.asm.mja.metrics;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author ashut
 * @since 01-12-2025
 */

public class MetricsHttpServer {

    private static MetricsHttpServer instance;
    private HttpServer server;

    private MetricsHttpServer() {}

    public static synchronized MetricsHttpServer getInstance() {
        if (instance == null)
            instance = new MetricsHttpServer();
        return instance;
    }

    public void start(int port) {
        if (server != null) return; // already running

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/metrics", new PrometheusMetricsHandler());
            server.createContext("/metrics.json", new JsonMetricsHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Metrics endpoint running at http://127.0.0.1:" + port + "/metrics");
            System.out.println("Metrics JSON endpoint running at http://127.0.0.1:" + port + "/metrics.json");
        } catch (IOException e) {
            System.err.println("Failed to start Metrics HTTP server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** Handler: returns metrics in Prometheus/OpenMetrics format */
    static class PrometheusMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            boolean openMetrics = acceptsOpenMetrics(exchange);
            String payload = MetricsPrometheusSerializer.toPrometheus(
                MetricsSnapshot.getInstance(),
                openMetrics
            );
            if (openMetrics && !payload.contains("# EOF")) {
                payload = payload + "# EOF\n";
            }
            byte[] responseBytes = payload.getBytes(StandardCharsets.UTF_8);

            if (openMetrics) {
                exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/openmetrics-text; version=1.0.0; charset=utf-8"
                );
            } else {
                exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/plain; version=0.0.4; charset=utf-8"
                );
            }
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private boolean acceptsOpenMetrics(HttpExchange exchange) {
            String accept = exchange.getRequestHeaders().getFirst("Accept");
            return accept != null && accept.toLowerCase().contains("application/openmetrics-text");
        }
    }

    /** Handler: returns all metrics in JSON for compatibility */
    static class JsonMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String json = MetricsJsonSerializer.toJson(
                MetricsSnapshot.getInstance().getAllMetrics()
            );
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}

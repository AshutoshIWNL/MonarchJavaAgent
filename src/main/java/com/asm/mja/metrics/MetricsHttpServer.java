package com.asm.mja.metrics;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

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
            server.createContext("/metrics", new MetricsHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Metrics endpoint running at http://127.0.0.1:" + port + "/metrics");
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

    /** Handler: returns all metrics in JSON */
    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String json = MetricsJsonSerializer.toJson(
                    MetricsSnapshot.getInstance().getAllMetrics()
            );

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        }
    }
}
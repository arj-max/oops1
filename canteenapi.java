import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CanteenAPIService {

    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final String WEB_DIR = "./"; // All files in the same folder
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        startServer();
    }

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Serve static files (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    // ---------------- Static File Handler ----------------
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            File file = new File(WEB_DIR + path);
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                Files.copy(file.toPath(), exchange.getResponseBody());
            } else {
                File indexFile = new File(WEB_DIR + "/index.html");
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, indexFile.length());
                Files.copy(indexFile.toPath(), exchange.getResponseBody());
            }
            exchange.getResponseBody().close();
        }

        private String getContentType(String path) {
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }

    // ---------------- Register Handler ----------------
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
                return;
            }
            try {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = gson.fromJson(requestBody, Map.class);

                String name = params.get("name").trim();
                String email = params.get("email").trim().toLowerCase();
                String password = params.get("password").trim();

                // Hash password
                String hashedPassword = hashPassword(password);

                boolean success = User.register(name, email, hashedPassword);

                if (success) {
                    sendResponse(exchange, "{\"success\":true,\"message\":\"Registration successful\"}", 200);
                } else {
                    sendResponse(exchange, "{\"success\":false,\"error\":\"Email already exists\"}", 400);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, "{\"success\":false,\"error\":\"Server error\"}", 500);
            }
        }
    }

    // ---------------- Login Handler ----------------
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
                return;
            }
            try {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = gson.fromJson(requestBody, Map.class);

                String email = params.get("email").trim().toLowerCase();
                String password = params.get("password").trim();
                String hashedPassword = hashPassword(password);

                User user = User.login(email, hashedPassword);

                if (user != null) {
                    String response = gson.toJson(Map.of(
                            "success", true,
                            "user", Map.of(
                                    "id", user.id,
                                    "name", user.name,
                                    "email", user.email,
                                    "wallet", user.wallet
                            )
                    ));
                    sendResponse(exchange, response, 200);
                } else {
                    sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid credentials\"}", 401);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, "{\"success\":false,\"error\":\"Server error\"}", 500);
            }
        }
    }

    // ---------------- Utilities ----------------
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);
        return body.toString();
    }

    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

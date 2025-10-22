import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class CanteenAPIService {

    private static final int PORT = 8080;
    private static final String WEB_DIR = "./"; // All files in the same folder

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
        // Add other handlers: menu, orders, cart, payment, reviews

        server.setExecutor(null);
        server.start();
        System.out.println("Canteen Management System Server started on port " + PORT);
    }

    // Serve static files
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
                // fallback to index.html for SPA
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

    // ----------- Login Handler -----------
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = parseJson(requestBody);

                String email = params.get("email").trim().toLowerCase();
                String password = params.get("password").trim();

                User userService = new User();
                var result = userService.login(email, password);

                if (result.isPresent()) {
                    var user = result.get();
                    String response = String.format(
                        "{\"success\":true,\"user\":{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"wallet\":%.2f}}",
                        user.id, user.name, user.email, user.wallet
                    );
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

    // ----------- Register Handler -----------
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);
                Map<String, String> params = parseJson(requestBody);

                String name = params.get("name").trim();
                String email = params.get("email").trim().toLowerCase();
                String password = params.get("password").trim();

                User userService = new User();
                boolean success = userService.register(name, email, password);

                if (success) {
                    sendResponse(exchange, "{\"success\":true,\"message\":\"Registration successful\"}", 200);
                } else {
                    sendResponse(exchange, "{\"success\":false,\"error\":\"Registration failed\"}", 400);
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, "{\"success\":false,\"error\":\"Server error\"}", 500);
            }
        }
    }

    // ----------- Utilities -----------
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) body.append(line);
        return body.toString();
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> result = new HashMap<>();
        json = json.replaceAll("[{}\"]", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                result.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return result;
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

}

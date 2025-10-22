// CanteenAPIService.java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CanteenAPIService {
    private static final int PORT = 8080;
    private static final String WEB_DIR = "./"; // Current directory
    
    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Serve static files (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());
        
        // API endpoints
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/menu", new MenuHandler());
        server.createContext("/api/orders", new OrdersHandler());
        server.createContext("/api/cart", new CartHandler());
        server.createContext("/api/payment", new PaymentHandler());
        server.createContext("/api/reviews", new ReviewsHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("Canteen Management System Server started!");
        System.out.println("Web interface available at: http://localhost:" + PORT);
        System.out.println("API endpoints available at: http://localhost:" + PORT + "/api/");
    }
    
    // Handler for serving static files (HTML, CSS, JS)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Default to index.html
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            File file = new File(WEB_DIR + path);
            
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                
                Files.copy(file.toPath(), exchange.getResponseBody());
            } else {
                // File not found, serve index.html for SPA routing
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
    
    // API Handlers
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    Map<String, String> params = parseFormData(requestBody);
                    
                    String email = params.get("email");
                    String password = params.get("password");
                    
                    User userService = new User();
                    var result = userService.login(email, password);
                    
                    if (result.isPresent()) {
                        var user = result.get();
                        String response = String.format(
                            "{\"success\": true, \"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\", \"wallet\": %.2f}}",
                            user.id, user.name, user.email, user.wallet
                        );
                        sendResponse(exchange, response, 200);
                    } else {
                        sendResponse(exchange, "{\"success\": false, \"error\": \"Invalid credentials\"}", 401);
                    }
                } catch (Exception e) {
                    sendResponse(exchange, "{\"success\": false, \"error\": \"Server error\"}", 500);
                }
            } else {
                sendResponse(exchange, "{\"error\": \"Method not allowed\"}", 405);
            }
        }
    }
    
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    Map<String, String> params = parseFormData(requestBody);
                    
                    String name = params.get("name");
                    String email = params.get("email");
                    String password = params.get("password");
                    
                    User userService = new User();
                    boolean success = userService.register(name, email, password);
                    
                    if (success) {
                        sendResponse(exchange, "{\"success\": true, \"message\": \"Registration successful\"}", 200);
                    } else {
                        sendResponse(exchange, "{\"success\": false, \"error\": \"Registration failed\"}", 400);
                    }
                } catch (Exception e) {
                    sendResponse(exchange, "{\"success\": false, \"error\": \"Server error: " + e.getMessage() + "\"}", 500);
                }
            }
        }
    }
    
    static class MenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    Menu menuService = new Menu();
                    var items = menuService.listMenuItems();
                    
                    StringBuilder json = new StringBuilder("{\"success\": true, \"menuItems\": [");
                    for (int i = 0; i < items.size(); i++) {
                        var item = items.get(i);
                        if (i > 0) json.append(",");
                        json.append(String.format(
                            "{\"id\": %d, \"name\": \"%s\", \"description\": \"%s\", \"price\": %.2f, \"available\": %s, \"createdAt\": \"%s\"}",
                            item.id, escapeJson(item.name), escapeJson(item.description), 
                            item.price, item.available, item.createdAt
                        ));
                    }
                    json.append("]}");
                    
                    sendResponse(exchange, json.toString(), 200);
                } catch (Exception e) {
                    sendResponse(exchange, "{\"success\": false, \"error\": \"Failed to load menu\"}", 500);
                }
            }
        }
    }
    
    static class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String requestBody = readRequestBody(exchange);
                    Map<String, String> params = parseJson(requestBody);
                    
                    int userId = Integer.parseInt(params.get("userId"));
                    String timeSlot = params.get("timeSlot");
                    
                    // Parse items from JSON
                    Map<Integer, Integer> itemsMap = new HashMap<>();
                    // Implementation for parsing items...
                    
                    Order orderService = new Order();
                    int orderId = orderService.createOrder(userId, itemsMap, timeSlot);
                    
                    sendResponse(exchange, "{\"success\": true, \"orderId\": " + orderId + "}", 200);
                } catch (Exception e) {
                    sendResponse(exchange, "{\"success\": false, \"error\": \"Failed to create order\"}", 500);
                }
            }
        }
    }
    
    // Utility methods
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }
    
    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                    String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    params.put(key, value);
                } catch (UnsupportedEncodingException e) {
                    // ignore
                }
            }
        }
        return params;
    }
    
    private static Map<String, String> parseJson(String json) {
        // Simple JSON parser for basic needs
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
        
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}

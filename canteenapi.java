// Inside CanteenAPIService.java, replace LoginHandler and RegisterHandler with:

static class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, "{\"error\": \"Method not allowed\"}", 405);
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
                        "{\"success\": true, \"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\", \"wallet\": %.2f}}",
                        user.id, user.name, user.email, user.wallet
                );
                sendResponse(exchange, response, 200);
            } else {
                sendResponse(exchange, "{\"success\": false, \"error\": \"Invalid credentials\"}", 401);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, "{\"success\": false, \"error\": \"Server error\"}", 500);
        }
    }
}

static class RegisterHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, "{\"error\": \"Method not allowed\"}", 405);
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
                sendResponse(exchange, "{\"success\": true, \"message\": \"Registration successful\"}", 200);
            } else {
                sendResponse(exchange, "{\"success\": false, \"error\": \"Registration failed\"}", 400);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, "{\"success\": false, \"error\": \"Server error\"}", 500);
        }
    }
}

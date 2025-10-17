// Main.java
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Start the web server
            CanteenAPIService.startServer();
            
            // You can also run the console version simultaneously
            runConsoleDemo();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runConsoleDemo() {
        try {
            User userSvc = new User();
            Menu menuSvc = new Menu();
            Order orderSvc = new Order();
            Payment paymentSvc = new Payment();
            Review reviewSvc = new Review();
            OrderSummary summarySvc = new OrderSummary();

            System.out.println("=== Canteen Management System ===");
            System.out.println("Web Server: http://localhost:8080");
            System.out.println("Console demo running...\n");

            // Demo code from previous implementation...
            // (Your existing console demo code here)
            
        } catch (Exception e) {
            System.err.println("Console demo error: " + e.getMessage());
        }
    }
}
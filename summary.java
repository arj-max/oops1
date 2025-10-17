// OrderSummary.java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderSummary {

    // Generate summary for one order
    public String generateOrderSummary(int orderId) throws SQLException {
        StringBuilder summary = new StringBuilder();
        summary.append("Order ").append(orderId).append(" Summary:\n");
        summary.append("========================================\n");

        try (Connection c = DBConnection.getConnection()) {
            // Order details
            String orderSql = "SELECT u.name as user_name, o.time_slot, o.total_amount, o.status, o.created_at " +
                             "FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?";
            try (PreparedStatement orderStmt = c.prepareStatement(orderSql)) {
                orderStmt.setInt(1, orderId);
                try (ResultSet orderRs = orderStmt.executeQuery()) {
                    if (orderRs.next()) {
                        summary.append("User: ").append(orderRs.getString("user_name")).append("\n");
                        summary.append("Time Slot: ").append(orderRs.getString("time_slot")).append("\n");
                        summary.append("Total Amount: ₹").append(String.format("%.2f", orderRs.getDouble("total_amount"))).append("\n");
                        summary.append("Status: ").append(orderRs.getString("status")).append("\n");
                        summary.append("Order Date: ").append(orderRs.getTimestamp("created_at")).append("\n");
                    }
                }
            }

            // Items
            summary.append("\nItems:\n");
            summary.append("------\n");
            
            String itemsSql = "SELECT mi.name, oi.quantity, oi.price, (oi.quantity * oi.price) as item_total " +
                            "FROM order_items oi JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                            "WHERE oi.order_id = ?";
            try (PreparedStatement itemsStmt = c.prepareStatement(itemsSql)) {
                itemsStmt.setInt(1, orderId);
                try (ResultSet itemsRs = itemsStmt.executeQuery()) {
                    double total = 0;
                    while (itemsRs.next()) {
                        String itemName = itemsRs.getString("name");
                        int quantity = itemsRs.getInt("quantity");
                        double price = itemsRs.getDouble("price");
                        double itemTotal = itemsRs.getDouble("item_total");
                        total += itemTotal;
                        
                        summary.append("- ").append(itemName)
                              .append(" x").append(quantity)
                              .append(" @ ₹").append(String.format("%.2f", price))
                              .append(" = ₹").append(String.format("%.2f", itemTotal))
                              .append("\n");
                    }
                    summary.append("\nGrand Total: ₹").append(String.format("%.2f", total)).append("\n");
                }
            }
        }
        return summary.toString();
    }

    // Generate detailed order summary with HTML format
    public String generateOrderSummaryHTML(int orderId) throws SQLException {
        StringBuilder html = new StringBuilder();
        html.append("<div class='order-summary'>");
        html.append("<h3>Order #").append(orderId).append(" Summary</h3>");

        try (Connection c = DBConnection.getConnection()) {
            // Order details
            String orderSql = "SELECT u.name as user_name, o.time_slot, o.total_amount, o.status, o.created_at " +
                             "FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?";
            try (PreparedStatement orderStmt = c.prepareStatement(orderSql)) {
                orderStmt.setInt(1, orderId);
                try (ResultSet orderRs = orderStmt.executeQuery()) {
                    if (orderRs.next()) {
                        html.append("<div class='order-details'>");
                        html.append("<p><strong>User:</strong> ").append(orderRs.getString("user_name")).append("</p>");
                        html.append("<p><strong>Time Slot:</strong> ").append(orderRs.getString("time_slot")).append("</p>");
                        html.append("<p><strong>Total Amount:</strong> ₹").append(String.format("%.2f", orderRs.getDouble("total_amount"))).append("</p>");
                        html.append("<p><strong>Status:</strong> ").append(orderRs.getString("status")).append("</p>");
                        html.append("<p><strong>Order Date:</strong> ").append(orderRs.getTimestamp("created_at")).append("</p>");
                        html.append("</div>");
                    }
                }
            }

            // Items
            html.append("<div class='order-items'>");
            html.append("<h4>Items:</h4>");
            html.append("<table border='1' style='width:100%; border-collapse: collapse;'>");
            html.append("<tr><th>Item</th><th>Quantity</th><th>Price</th><th>Total</th></tr>");
            
            String itemsSql = "SELECT mi.name, oi.quantity, oi.price " +
                            "FROM order_items oi JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                            "WHERE oi.order_id = ?";
            try (PreparedStatement itemsStmt = c.prepareStatement(itemsSql)) {
                itemsStmt.setInt(1, orderId);
                try (ResultSet itemsRs = itemsStmt.executeQuery()) {
                    double total = 0;
                    while (itemsRs.next()) {
                        String itemName = itemsRs.getString("name");
                        int quantity = itemsRs.getInt("quantity");
                        double price = itemsRs.getDouble("price");
                        double itemTotal = price * quantity;
                        total += itemTotal;
                        
                        html.append("<tr>");
                        html.append("<td>").append(itemName).append("</td>");
                        html.append("<td>").append(quantity).append("</td>");
                        html.append("<td>₹").append(String.format("%.2f", price)).append("</td>");
                        html.append("<td>₹").append(String.format("%.2f", itemTotal)).append("</td>");
                        html.append("</tr>");
                    }
                    html.append("<tr style='font-weight: bold;'>");
                    html.append("<td colspan='3' style='text-align: right;'>Grand Total:</td>");
                    html.append("<td>₹").append(String.format("%.2f", total)).append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</table>");
            html.append("</div>");
            html.append("</div>");
        }
        return html.toString();
    }

    // Daily report
    public DailyReport generateDailyReport(String date) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*), SUM(total_amount) FROM orders WHERE DATE(created_at) = ? AND status != 'CANCELLED'")) {
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                double revenue = rs.getDouble(2);
                if (rs.wasNull()) revenue = 0.0;
                return new DailyReport(date, count, revenue);
            }
        }
        return new DailyReport(date, 0, 0);
    }

    // Monthly report
    public MonthlyReport generateMonthlyReport(int year, int month) throws SQLException {
        String datePattern = year + "-" + String.format("%02d", month) + "%";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*), SUM(total_amount) FROM orders WHERE created_at LIKE ? AND status != 'CANCELLED'")) {
            ps.setString(1, datePattern);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                double revenue = rs.getDouble(2);
                if (rs.wasNull()) revenue = 0.0;
                return new MonthlyReport(year, month, count, revenue);
            }
        }
        return new MonthlyReport(year, month, 0, 0);
    }

    // Get popular menu items
    public List<PopularItem> getPopularMenuItems(int limit) throws SQLException {
        List<PopularItem> popularItems = new ArrayList<>();
        String sql = "SELECT mi.id, mi.name, COUNT(oi.menu_item_id) as order_count, " +
                    "SUM(oi.quantity) as total_quantity " +
                    "FROM order_items oi " +
                    "JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                    "GROUP BY mi.id, mi.name " +
                    "ORDER BY total_quantity DESC " +
                    "LIMIT ?";
        
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    popularItems.add(new PopularItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("order_count"),
                        rs.getInt("total_quantity")
                    ));
                }
            }
        }
        return popularItems;
    }

    public static class DailyReport {
        public String date; 
        public int totalOrders; 
        public double revenue;
        public DailyReport(String d, int t, double r) {
            date = d; 
            totalOrders = t; 
            revenue = r;
        }
    }

    public static class MonthlyReport {
        public int year, month, totalOrders;
        public double revenue;
        public MonthlyReport(int y, int m, int t, double r) {
            year = y; 
            month = m; 
            totalOrders = t; 
            revenue = r;
        }
    }

    public static class PopularItem {
        public int id, orderCount, totalQuantity;
        public String name;
        public PopularItem(int id, String name, int orderCount, int totalQuantity) {
            this.id = id;
            this.name = name;
            this.orderCount = orderCount;
            this.totalQuantity = totalQuantity;
        }
    }
}
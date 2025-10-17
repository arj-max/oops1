// Payment.java
import java.sql.*;
import java.util.UUID;

public class Payment {

    public boolean processPayment(int orderId, double amount, String method) throws SQLException {
        String checkOrder = "SELECT total_amount, status FROM orders WHERE id = ?";
        String insertPayment = "INSERT INTO payments (order_id, amount, method, status, transaction_id) VALUES (?, ?, ?, ?, ?)";
        String updateOrder = "UPDATE orders SET status='PAID' WHERE id = ?";

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            
            // Check order exists and not already paid
            try (PreparedStatement ps = c.prepareStatement(checkOrder)) {
                ps.setInt(1, orderId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    c.rollback();
                    return false; // order not found
                }
                if ("PAID".equalsIgnoreCase(rs.getString("status"))) {
                    c.rollback();
                    return false; // already paid
                }
                if (Math.abs(rs.getDouble("total_amount") - amount) > 0.01) {
                    c.rollback();
                    return false; // amount mismatch
                }
            }

            // Insert payment
            try (PreparedStatement ps = c.prepareStatement(insertPayment)) {
                ps.setInt(1, orderId);
                ps.setDouble(2, amount);
                ps.setString(3, method);
                ps.setString(4, "SUCCESS");
                ps.setString(5, UUID.randomUUID().toString());
                ps.executeUpdate();
            }

            // Update order status
            try (PreparedStatement ps = c.prepareStatement(updateOrder)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            c.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
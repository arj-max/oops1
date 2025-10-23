import java.sql.*;
import java.security.MessageDigest;

public class User {

    public int id;
    public String name, email;
    public double wallet;

    public User() {} // default constructor

    private static final double DEFAULT_WALLET = 0.0;

    public static boolean register(String name, String email, String password) {
        String sql = "INSERT INTO users (name, email, password, wallet) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (c == null) return false;

            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, sha256(password.trim()));
            ps.setDouble(4, DEFAULT_WALLET);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            // e.g., duplicate email
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public static User login(String email, String password) {
        String sql = "SELECT id, name, email, wallet FROM users WHERE email=? AND password=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (c == null) return null;

            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, sha256(password.trim()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.id = rs.getInt("id");
                    u.name = rs.getString("name");
                    u.email = rs.getString("email");
                    u.wallet = rs.getDouble("wallet");
                    return u;
                }
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

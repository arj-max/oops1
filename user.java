// User.java
import java.sql.*;
import java.security.MessageDigest;
import java.util.Optional;

public class User {

    public static class UserModel {
        public int id; 
        public String name, email;
        public double wallet;
        
        public UserModel(int id, String name, String email, double wallet) {
            this.id = id; 
            this.name = name; 
            this.email = email; 
            this.wallet = wallet;
        }
    }

    public boolean register(String name, String email, String password) throws SQLException {
        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, sha256(password));
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<UserModel> login(String email, String password) throws SQLException {
        String sql = "SELECT id, name, email, wallet FROM users WHERE email=? AND password=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserModel(
                        rs.getInt("id"), 
                        rs.getString("name"),
                        rs.getString("email"), 
                        rs.getDouble("wallet")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    private String sha256(String input) {
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
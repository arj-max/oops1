// Menu.java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Menu {

    public static class MenuItem {
        public int id;
        public String name;
        public String description;
        public double price;
        public boolean available;
        public Timestamp createdAt;

        public MenuItem(int id, String name, String description, double price, boolean available, Timestamp createdAt) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.available = available;
            this.createdAt = createdAt;
        }
    }

    // Add a menu item
    public int addMenuItem(String name, String description, double price, boolean available) throws SQLException {
        String sql = "INSERT INTO menu_items (name, description, price, available) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setBoolean(4, available);
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) {
                if (g.next()) return g.getInt(1);
            }
            return -1;
        }
    }

    // Get menu item by ID
    public MenuItem getMenuItemById(int id) throws SQLException {
        String sql = "SELECT * FROM menu_items WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new MenuItem(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("price"),
                            rs.getBoolean("available"),
                            rs.getTimestamp("created_at")
                    );
                }
            }
        }
        return null;
    }

    // List all menu items
    public List<MenuItem> listMenuItems() throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_items ORDER BY name";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MenuItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getBoolean("available"),
                        rs.getTimestamp("created_at")
                ));
            }
        }
        return list;
    }

    // Update menu item
    public boolean updateMenuItem(int id, String name, String description, double price, boolean available) throws SQLException {
        String sql = "UPDATE menu_items SET name=?, description=?, price=?, available=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setBoolean(4, available);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Delete menu item by ID
    public boolean deleteMenuItem(int id) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Get available menu items only
    public List<MenuItem> getAvailableMenuItems() throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM menu_items WHERE available = true ORDER BY name";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MenuItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getBoolean("available"),
                        rs.getTimestamp("created_at")
                ));
            }
        }
        return list;
    }
}
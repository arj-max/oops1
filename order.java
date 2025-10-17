// Review.java
import java.sql.*;
import java.util.*;

public class Review {

    public static class ReviewModel {
        public int id, userId, menuItemId, rating;
        public String comment;
        public Timestamp createdAt;
        public String userName; // For displaying user name in reviews

        public ReviewModel(int id, int userId, int menuItemId, int rating, String comment, Timestamp createdAt) {
            this.id = id; 
            this.userId = userId; 
            this.menuItemId = menuItemId; 
            this.rating = rating; 
            this.comment = comment;
            this.createdAt = createdAt;
        }

        public ReviewModel(int id, int userId, int menuItemId, int rating, String comment, Timestamp createdAt, String userName) {
            this(id, userId, menuItemId, rating, comment, createdAt);
            this.userName = userName;
        }
    }

    // Add a new review
    public boolean addReview(int userId, int menuItemId, int rating, String comment) throws SQLException {
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        String sql = "INSERT INTO reviews (user_id, menu_item_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId); 
            ps.setInt(2, menuItemId); 
            ps.setInt(3, rating); 
            ps.setString(4, comment);
            return ps.executeUpdate() > 0;
        }
    }

    // Get all reviews for a menu item with user names
    public List<ReviewModel> getReviewsForMenuItem(int menuItemId) throws SQLException {
        String sql = "SELECT r.*, u.name as user_name FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.menu_item_id = ? ORDER BY r.created_at DESC";
        List<ReviewModel> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReviewModel(
                        rs.getInt("id"), 
                        rs.getInt("user_id"), 
                        rs.getInt("menu_item_id"),
                        rs.getInt("rating"), 
                        rs.getString("comment"),
                        rs.getTimestamp("created_at"),
                        rs.getString("user_name")
                    ));
                }
            }
        }
        return list;
    }

    // Get reviews by user
    public List<ReviewModel> getReviewsByUser(int userId) throws SQLException {
        String sql = "SELECT r.*, u.name as user_name FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.user_id = ? ORDER BY r.created_at DESC";
        List<ReviewModel> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReviewModel(
                        rs.getInt("id"), 
                        rs.getInt("user_id"), 
                        rs.getInt("menu_item_id"),
                        rs.getInt("rating"), 
                        rs.getString("comment"),
                        rs.getTimestamp("created_at"),
                        rs.getString("user_name")
                    ));
                }
            }
        }
        return list;
    }

    // Get review by ID
    public Optional<ReviewModel> getReviewById(int reviewId) throws SQLException {
        String sql = "SELECT r.*, u.name as user_name FROM reviews r JOIN users u ON r.user_id = u.id WHERE r.id = ?";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ReviewModel(
                        rs.getInt("id"), 
                        rs.getInt("user_id"), 
                        rs.getInt("menu_item_id"),
                        rs.getInt("rating"), 
                        rs.getString("comment"),
                        rs.getTimestamp("created_at"),
                        rs.getString("user_name")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    // Update a review
    public boolean updateReview(int reviewId, int rating, String comment) throws SQLException {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        String sql = "UPDATE reviews SET rating = ?, comment = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rating); 
            ps.setString(2, comment); 
            ps.setInt(3, reviewId);
            return ps.executeUpdate() > 0;
        }
    }

    // Delete a review
    public boolean deleteReview(int reviewId) throws SQLException {
        String sql = "DELETE FROM reviews WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() > 0;
        }
    }

    // Get average rating for a menu item
    public double getAverageRating(int menuItemId) throws SQLException {
        String sql = "SELECT AVG(rating) as avgRating FROM reviews WHERE menu_item_id = ?";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avgRating");
                    return rs.wasNull() ? 0.0 : avg;
                }
            }
        }
        return 0.0;
    }

    // Get rating count for a menu item
    public int getRatingCount(int menuItemId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM reviews WHERE menu_item_id = ?";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    // Get rating distribution for a menu item
    public Map<Integer, Integer> getRatingDistribution(int menuItemId) throws SQLException {
        Map<Integer, Integer> distribution = new HashMap<>();
        String sql = "SELECT rating, COUNT(*) as count FROM reviews WHERE menu_item_id = ? GROUP BY rating ORDER BY rating DESC";
        try (Connection c = DBConnection.getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    distribution.put(rs.getInt("rating"), rs.getInt("count"));
                }
            }
        }
        return distribution;
    }
}
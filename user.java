public boolean register(String name, String email, String password) {
    name = name.trim();
    email = email.trim().toLowerCase();
    password = password.trim();
    
    String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        
        if (c == null) {
            System.err.println("DB connection failed!");
            return false;
        }
        
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, sha256(password));
        
        boolean success = ps.executeUpdate() > 0;
        if(success) System.out.println("User registered: " + email);
        return success;
        
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

public Optional<UserModel> login(String email, String password) {
    email = email.trim().toLowerCase();
    password = password.trim();
    
    String sql = "SELECT id, name, email, wallet FROM users WHERE email=? AND password=?";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        
        if (c == null) {
            System.err.println("DB connection failed!");
            return Optional.empty();
        }
        
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
        
    } catch (SQLException e) {
        e.printStackTrace();
        return Optional.empty();
    }
}

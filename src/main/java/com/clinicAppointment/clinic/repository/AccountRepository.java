package com.clinicAppointment.clinic.repository;

import com.clinicAppointment.clinic.model.AccountModel;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    private void createSessionTable() {
        String sql = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT UNIQUE, " +
                "session_token TEXT, " +
                "expires_at DATETIME)";
        jdbcTemplate.execute(sql);
    }
    @PostConstruct
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "fname TEXT, " +
                "lname TEXT, " +
                "email TEXT UNIQUE, " +
                "password TEXT, " +
                "role TEXT DEFAULT 'user', " + // Add role column with default value 'user'
                "session_token TEXT)";
        jdbcTemplate.execute(sql);
    }

    public String getRoleByEmail(String email) {
        try {
            String sql = "SELECT role FROM accounts WHERE email = ?";
            return jdbcTemplate.queryForObject(sql, String.class, email);
        } catch (Exception e) {
            return "user";
        }
    }

    public void addAccount(String fname, String lname, String email, String password) {
        String sql = "INSERT INTO accounts (fname, lname, email, password, role) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, fname, lname, email, password, "user"); // Default role is 'user'
    }


    public boolean checkAccount(String email, String password) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE email = ? AND password = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, password);
        return count != null && count > 0;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0; // Returns true if email exists
    }

    public String generateSessionToken(String email) {
        String token = UUID.randomUUID().toString();
        String sql = "UPDATE accounts SET session_token = ? WHERE email = ?";
        jdbcTemplate.update(sql, token, email);
        return token;
    }

    public String getEmailBySession(String sessionToken) {
        try {
            String sql = "SELECT email FROM accounts WHERE session_token = ?";
            return jdbcTemplate.queryForObject(sql, String.class, sessionToken);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearSessionToken(String email) {
        String sql = "UPDATE accounts SET session_token = NULL WHERE email = ?";
        jdbcTemplate.update(sql, email);
    }


    public List<AccountModel> getAllAccounts() {
        String sql = "SELECT * FROM accounts";
        RowMapper<AccountModel> rowMapper = (rs, rowNum) 
                -> new AccountModel(rs.getInt("id"),
                rs.getString("fname"),
                rs.getString("lname"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"));
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void deleteAccount(int id) {
        String sql = "DELETE FROM accounts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }


}

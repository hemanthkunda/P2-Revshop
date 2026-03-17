import java.sql.*;

public class CheckDb {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        String url = "jdbc:oracle:thin:@localhost:1521:XE";
        String user = "revshop2";
        String pass = "12345";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
             
            System.out.println("--- All constraints on USERS table ---");
            ResultSet rs = stmt.executeQuery("SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE FROM USER_CONSTRAINTS WHERE TABLE_NAME = 'USERS'");
            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " + rs.getString(2));
            }
            
            System.out.println("--- All rows in USERS table ---");
            rs = stmt.executeQuery("SELECT ID, EMAIL FROM USERS");
            int count = 0;
            while (rs.next()) {
                System.out.println(rs.getLong("ID") + " | " + rs.getString("EMAIL"));
                count++;
            }
            System.out.println("Total rows: " + count);

            System.out.println("--- Trying to insert dummy row ---");
            try {
                stmt.executeUpdate("INSERT INTO USERS (ID, EMAIL, NAME, PASSWORD, ROLE, CREATED_AT, UPDATED_AT) VALUES (USERS_SEQ.NEXTVAL, 'dummy@test.com', 'Dummy', 'pass', 'BUYER', SYSDATE, SYSDATE)");
                System.out.println("Dummy insert succeeded.");
            } catch (Exception e) {
                System.out.println("Dummy insert failed: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

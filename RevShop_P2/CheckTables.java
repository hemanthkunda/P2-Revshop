import java.sql.*;

public class CheckTables {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        String url = "jdbc:oracle:thin:@localhost:1521:XE";
        String user = "revshop2";
        String pass = "12345";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
             
            System.out.println("--- All Tables in Schema ---");
            ResultSet rs = stmt.executeQuery("SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME");
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

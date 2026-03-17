import java.sql.*;

public class CheckDbProd {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        String url = "jdbc:oracle:thin:@localhost:1521:XE";
        String user = "revshop2";
        String pass = "12345";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
             
            System.out.println("--- Columns in PRODUCT table ---");
            ResultSet rs = stmt.executeQuery("SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'PRODUCT' ORDER BY COLUMN_ID");
            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " + rs.getString(2));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

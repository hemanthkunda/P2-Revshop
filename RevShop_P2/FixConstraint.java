import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FixConstraint {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        String url = "jdbc:oracle:thin:@localhost:1521:XE";
        String user = "revshop2";
        String pass = "12345";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             Statement stmtAlter = conn.createStatement()) {
             
            System.out.println("Finding existing constraints on PRODUCT.SELLER_ID...");
            ResultSet rs = stmt.executeQuery(
                "SELECT c.constraint_name " +
                "FROM user_constraints c " +
                "JOIN user_cons_columns a ON a.constraint_name = c.constraint_name " +
                "WHERE a.table_name = 'PRODUCT' AND a.column_name = 'SELLER_ID' AND c.constraint_type = 'R'"
            );
            
            List<String> constraints = new ArrayList<>();
            while (rs.next()) {
                constraints.add(rs.getString(1));
            }
            rs.close();
            
            for (String cName : constraints) {
                System.out.println("Found constraint: " + cName + " - dropping...");
                stmtAlter.execute("ALTER TABLE product DROP CONSTRAINT " + cName);
            }
            
            System.out.println("Adding new constraint to reference USERS(id)...");
            stmtAlter.execute("ALTER TABLE product ADD CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(id)");
            System.out.println("New constraint successfully added!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

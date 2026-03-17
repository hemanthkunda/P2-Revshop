import java.sql.*;

public class CheckConst {
    public static void main(String[] args) throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        String url = "jdbc:oracle:thin:@localhost:1521:XE";
        String user = "revshop2";
        String pass = "12345";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
             
            System.out.println("--- FK_PRODUCT_SELLER INFO ---");
            ResultSet rs = stmt.executeQuery(
                "SELECT c.constraint_name, c.table_name as child_table, " +
                "a.column_name as child_column, " +
                "c.r_constraint_name, " +
                "cr.table_name as parent_table " +
                "FROM user_constraints c " +
                "JOIN user_cons_columns a ON a.constraint_name = c.constraint_name " +
                "JOIN user_constraints cr ON c.r_constraint_name = cr.constraint_name " +
                "WHERE c.constraint_name = 'FK_PRODUCT_SELLER'"
            );
            while (rs.next()) {
                System.out.println("Child: " + rs.getString("child_table") + "." + rs.getString("child_column") + 
                                   " -> Parent Table: " + rs.getString("parent_table"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

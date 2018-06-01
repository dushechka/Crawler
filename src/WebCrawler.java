import java.sql.*;

public class WebCrawler {

    private static final String DB_ADRESS = "jdbc:mysql://localhost:3306/searchandratewords";
    private static final String USER_NAME = "crawler";
    private static final String PASSWORD = "123";

    public static void main(String[] args) throws Exception {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_ADRESS, USER_NAME, PASSWORD);
            if (conn == null) {
                System.out.println("No database connection!");
                System.exit(0);
            }
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM pages WHERE siteID=1");
            while (rs.next()) {
                System.out.println(rs.getRow() + ". " + rs.getString("URL")
                        + "\t" + rs.getString("foundDateTime"));
            }
            stmt.close();
        } catch (SQLException exc) {
            exc.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}

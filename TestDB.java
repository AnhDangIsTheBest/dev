public class TestDB {
    public static void main(String[] args) {
        try {
            var conn = DBConnection.getConnection();
            System.out.println("THANH CONG");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

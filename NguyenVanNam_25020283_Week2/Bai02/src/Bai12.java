public class Bai12 {
    private String id;
    private String name;
    private String email;
    private double gpa;
    public Bai12(){
        this.id = "2502xxxx";
        this.name = "UET_Student";
        this.email = "2502xxx@vnu.edu.vn";
        this.gpa = 0.0;
    }
    public Bai12(String id, String name){
        this.id = id;
        this.name = name;
        this.gpa = 0.0;
        this.email = id + "@vnu.edu.vn";
    }
    public Bai12(String id, String name, String email, double gpa){
        this.id = id;
        this.name = name;
        this.email = email;
        if (gpa < 0.0 || gpa > 4.0){
            System.out.println("GPA phai tu 0.0 den 4.0");
            this.gpa = 0.0;
        }
        else {
            this.gpa = gpa;
        }
    }
    public void displayInfo(){
        System.out.println("ID: " + this.id);
        System.out.println("Name: " + this.name);
        System.out.println("Email: " + this.email);
        System.out.println("GPA: " + this.gpa);
    }

    public static void main(String[] args){
       Bai12 student1 = new Bai12();
       Bai12 student2 = new Bai12("25020283", "Nguyen Van Nam");
       Bai12 student3 = new Bai12("25020283","Nguyen Van Nam", "25020283@vnu.edu.vn",5.0);
       System.out.println("hoc sinh 1");
       student1.displayInfo();
       System.out.println("hoc sinh 2");
       student2.displayInfo();
       System.out.println("hoc sinh 3");
       student3.displayInfo();

       
    }
    
}

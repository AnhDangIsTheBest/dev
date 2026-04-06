class MyDate{
    public int day,month,year;
    public MyDate(int day,int month,int year){
        this.day = day;
        this.month = month;
        this.year = year;
    }
    @Override
    public String toString(){
        return this.day +"/" + this.month +"/" +this.year;
    }
    public MyDate(MyDate other){
        this.day = other.day;
        this.month = other.month;
        this.year = other.year;
    }
}
class Employee{
    public String name;
    public MyDate birthday;
    public Employee(String name, MyDate birthday){
        this.name = name;
        this.birthday = birthday;
    }
    public Employee(Employee other){
        this.name = other.name;
        this.birthday = new MyDate(other.birthday);
    }
    public MyDate getBirthday(){
        return this.birthday;
    }


}
public class Bai14{
    public static void main(String[] args){
        MyDate person1 = new MyDate(1,1,2000);
        Employee em1 = new Employee("Nam",person1);
        Employee em2 = new Employee(em1);
        System.out.println(em2.birthday);
        em1.birthday.day = 2;
        em1.birthday.month = 2;
        em1.birthday.year = 2022;
        em2.name = "Khanh Linh";
        System.out.println(em1.name +" |Sinh ngay: " + em1.birthday);
        System.out.println(em2.name + " |Sinh ngay: " + em2.birthday);

        
    }
}
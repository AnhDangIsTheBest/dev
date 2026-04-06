class Person{
    private String name;
    private Person me;
    public Person(String name){
        this.name = name;
    }
    public void setMe(Person other){
        this.me = other;

    }
    public Person getMe(){
        return this.me;
    }
    public String getName(){
        return name;
    }

}
public class Bai18{
    public static void main(String[] args){
        Person p = new Person("Vnam");
        p.setMe(p);
        System.out.println("My Name: " + p.getMe().getName());
        p = null;
        System.out.println(p);
    }
}
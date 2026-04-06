public class Bai13 {
    int value;
    public Bai13(int value){
        this.value = value;
    }
    public int getValue(){
        return value;
    }
    public void setValue(int new_value){
        this.value = new_value;
    }
    

}
class swap{
    public static void swap(Bai13 n1, Bai13 n2){
        int temp = n1.value;
        n1.setValue(n2.value);
        n2.setValue(temp);
    }
    public static void main(String[] args){
        Bai13 n1 = new Bai13(5);
        Bai13 n2 = new Bai13(10);
        swap(n1,n2);
        System.out.println("gia tri cua n1: " + n1.getValue());
        System.out.println("gia tri cua n2: " + n2.getValue());
    }
}

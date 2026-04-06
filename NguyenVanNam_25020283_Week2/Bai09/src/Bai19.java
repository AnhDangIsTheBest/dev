import java.util.Scanner;
class Product2{
    private String name;
    private double price, discount;
    private int quantity;
    private static double totalRevenue;
    private static double taxRate = 0.1;
    public Product2(String name, double price, int quantity, double discount){
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.discount = discount;
    }
    public int getQuantity(){
        return this.quantity;
    }
    public static void updateTaxRate(double newRate){
        taxRate = newRate;
    }
    public double calculateFinalPrice(){
        return (price - discount) * (1 + taxRate);
    }
    public void updateDiscount(double newDiscount){
        this.discount = newDiscount;
    }
    public void sell(int amount){
        if  ( amount <= quantity){
            this.quantity -= amount;
            totalRevenue += amount * calculateFinalPrice();
            System.out.println("Thanh cong");
        }
        else{
            System.out.println("Khong du hang trong kho");
        }
    }
}
public class Bai19{
    public static void main(String[] args){
        Scanner sc =  new Scanner(System.in);
        System.out.println("Nhap name, price, quantity, discount(moi du lieu tren 1 dong)");
        String name1 = sc.nextLine();
        double price1 = sc.nextDouble();
        int quantity1 = sc.nextInt();
        double discount1 = sc.nextDouble();
        Product2 p1 = new Product2(name1,price1,quantity1,discount1);
        sc.nextLine();
        System.out.println("Nhap name, price, quantity, discount(moi du lieu tren 1 dong)");   
        String name2 = sc.next();
        double price2 = sc.nextDouble();
        int quantity2 = sc.nextInt();
        double discount2 = sc.nextDouble();
        Product2 p2 = new Product2(name2,price2,quantity2,discount2);
        System.out.print("Nhap so luong can mua p1: ");
        int amount1 = sc.nextInt();
        p1.sell(amount1);
        System.out.print("Nhap so luong can mua p2: ");
        int  amount2 = sc.nextInt();
        p2.sell(amount2);
        System.out.printf("Final Price p1: %.2f | Final Price p2: %.2f\n",
        p1.calculateFinalPrice(),p2.calculateFinalPrice());
        Product2.updateTaxRate(0.08);
        System.out.printf("Final Price p1: %.2f | Final Price p2: %.2f\n",
        p1.calculateFinalPrice(),p2.calculateFinalPrice());
    }  
} 

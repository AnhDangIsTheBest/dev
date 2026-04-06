class Product{
    public String id,name;
    public double price;
    public Product(String id, String name, double price){
        this.id = id;
        this.name = name;
        this.price = price;
    }
    public void setPrice(double new_price){
        this.price = new_price;
    }
    @Override
    public String toString() {
        return "ID: " + id + " / Name: " + name + " / Price: " + price + "$";
    }

}
class Inventory{
    public Product[] items;
    public Inventory(Product[] initialItems){
        this.items = initialItems;
    }
    public void disPlayInventory(){
        for (Product p : items){
            System.out.println(p);
        }
    }

}
public class Bai17{
    public static void main(String[] args){
        Product p1 = new Product("1", "Laptop", 1000);
        Product p2 = new Product("2", "Phone", 500);
        Product[] arr = {p1, p2};
        arr[0].setPrice(5000);
        Inventory kho = new Inventory(arr);
        kho.disPlayInventory();


    }
}
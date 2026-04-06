public class Bai15 {
    private String title, author;
    private double price;
    public Bai15(String title, String author, double price){
        this.title = title;
        this.author = author;
        this.price = price;
    }
    @Override
    public boolean equals(Object obj){
        if (this == obj){
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()){
            return false;
        }
        Bai15 other = (Bai15) obj;
        return Double.compare(other.price, price) == 0
               && title.equals(other.title) 
               && author.equals(other.author);

    }
    public static void main(String[] args){
        Bai15 tes1 = new Bai15("Toi yeu em","Aleksandr Sergeyevich Pushkin",10000);
        Bai15 tes2 =  new Bai15("Toi yeu em","Aleksandr Sergeyevich Pushkin",10000);
        System.out.print("So sanh ==: ");
        System.out.println(tes1 == tes2);
        System.out.print("So sanh equals: ");
        System.out.println(tes1.equals(tes2));
        System.out.println();
        System.out.print("== dung de so sanh di chi bo nho, khong override duoc, hay dung de kiem tra cung object\nequals dung de so sanh noi dung, override duoc,hay dung de so sanh doi tuong");
    }
}

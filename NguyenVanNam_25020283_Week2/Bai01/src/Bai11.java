
public class Bai11 {
    // BankAccount
    private String accountNumber;
    private double balance;
    private String ownerName;
    public Bai11(String accountNumber, String ownerName) {
        this.accountNumber = accountNumber;
        this.balance = 0.0;
        this.ownerName = ownerName;
    }
    public Bai11(String accountNumber, String ownerName, double balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        if (balance < 0){
            System.out.println("So du khong the am");
            this.balance = 0.0;
        }
        else{
            this.balance = balance;
        }
    
    }
    public void deposit(double amount){
        if (amount > 0){
            this.balance += amount;
            System.out.println("Da nap: " + amount + " VND");
        } else {
            System.out.println("So tien phai lon hon 0 ");
        }
    }
    public boolean withdraw(double amount){
        if (amount > this.balance){
            System.out.println("So du khong du de rut! ");
            return false;
        }
        else if (amount <= 0){
            System.out.println("So tien rut phai lon hon 0 ");
            return false;
        }
        else {
            this.balance -= amount;
            System.out.println("Da rut: " + amount + " VND");
            return true;
        }
    }
    public double getBalance() {
        return this.balance;
    }
    public static void main(String[] args ){
       Bai11 account = new Bai11("12040799141074", "Nguyen Van Nam", 100000.0);
       System.out.println(account.getBalance());
       account.deposit(-50000);
       account.withdraw(10000000);
       account.withdraw(15000); 
       System.out.println(account.getBalance());
       account.deposit(1000000);
       System.out.println(account.getBalance());
       
        }

    }

    



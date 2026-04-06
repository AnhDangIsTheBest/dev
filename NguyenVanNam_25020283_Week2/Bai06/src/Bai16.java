final class  Transaction {
    private String  transactionId, timestamp;
    public double amount;
    public Transaction(String transactionId, double amount,String timestamp){
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.amount = amount;
    }
    public String getTransactionId() { 
        return transactionId; 
     }
    public double getAmount() { 
        return amount;
     }
    public String getTimestamp() { 
        return timestamp;
     }
     @Override
    public String toString() {
        return String.format("[ID: %s | So tien: %.1f | Time: %s]", 
                             transactionId, amount, timestamp);
}
}
class Account{
    private String accountId;
    private double balance;
    private Transaction[] history;
    private int transactionCount = 0;
    public Account(String accountId, double balance) {
        this.accountId = accountId;
        this.balance = balance;
        this.transactionCount = 0;
        this.history = new Transaction[100];
    }
    public void addTransaction(Transaction t){
        history[transactionCount] = t;
        transactionCount += 1;
        balance += t.getAmount();
    }
    public Transaction[] gethistory(){
        Transaction[] copy_history = new Transaction[transactionCount];
        for (int i = 0; i < transactionCount;i++){
            copy_history[i] = new Transaction(history[i].getTransactionId(),history[i].getAmount(),history[i].getTimestamp());
        }
        return copy_history;
    }
    public void  displayHistory(){
        for (Transaction i: this.history){
            if (i == null){
                break;
            }
            else{
                System.out.println("ID_GD:" + i.getTransactionId() +" | amount:" + i.getAmount());
            }
        }
    }
    public void displayBalance() {
        System.out.println("Tai Khoan: " + accountId + " | So du: " + balance);
    }
}
public class Bai16{
    public static void main(String[] args){
        Account myAcc = new Account("25020283",100000);
        myAcc.addTransaction(new Transaction("GD01", 100000.0, "2026-03-02 10:01"));
        myAcc.addTransaction(new Transaction("GD02", 250000.0, "2026-03-02 11:30"));
        Transaction[] hackerView = myAcc.gethistory();
        hackerView[0].amount = 999999;
        hackerView[1] = null;
        myAcc.displayBalance(); // Số dư không thay đổi
        myAcc.displayHistory();
    }

}


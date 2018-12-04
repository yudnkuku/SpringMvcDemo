package demo;

import org.junit.Test;

public class SynThreadTest {

    @Test
    public void testAccount() {
        Account account = new Account("yuding", 1000);
        DrawMoneyRunnable drawMoneyRunnable = new DrawMoneyRunnable(account, 700);
        Thread thread1 = new Thread(drawMoneyRunnable);
        Thread thread2 = new Thread(drawMoneyRunnable);
        thread1.start();
        thread2.start();
    }

    class DrawMoneyRunnable implements Runnable {

        private Account account;

        private double drawMoney;

        public DrawMoneyRunnable(Account account, double drawMoney) {
            this.account = account;
            this.drawMoney = drawMoney;
        }

        @Override
        public void run() {
            if (account.getBalance() > drawMoney) {
                System.out.println("取钱成功，取出钱数为：" + drawMoney);
                double balance = account.getBalance() - drawMoney;
                account.setBalance(balance);
                System.out.println("当期账户余额为：" + balance);
            }
        }
    }

    class Account {
        private String accountNo;

        private double balance;

        public Account(String accountNo, double balance) {
            this.accountNo = accountNo;
            this.balance = balance;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public void setAccountNo(String accountNo) {
            this.accountNo = accountNo;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }
    }
}

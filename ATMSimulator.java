import java.util.*;

/*
 * ATM SIMULATOR - DSA Project Prototype
 * Data Structures:
 * HashMap, Stack, Queue, HashSet
 * Array + Greedy Algorithm
 */

public class ATMSimulator {

    static final int MAX_ATTEMPTS = 3;
    static final int DAILY_LIMIT = 20000;
    static final int[] NOTES = {500, 200, 100};

    static int txnCounter = 1;
    static int nextCard = 1004;

    static int[] cashStock = {20, 50, 100};

    static Map<String, Account> accounts = new HashMap<>();
    static Set<String> blockedCards = new HashSet<>();
    static Queue<String> waitingQueue = new ArrayDeque<>();

    // TRANSACTION CLASS
    static class Transaction {
        final int id;
        final String type;
        final int amount, balanceAfter;

        Transaction(String type, int amount, int balanceAfter) {
            this.id = txnCounter++;
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
        }

        public String toString() {
            return String.format(
                "#%d %-12s Rs. %6d | Balance: Rs. %d",
                id, type, amount, balanceAfter
            );
        }
    }

    // ACCOUNT CLASS
    static class Account {
        final String cardNo, name;
        String pin;
        int balance, withdrawnToday;

        final Stack<Transaction> history = new Stack<>();

        Account(String cardNo, String name, String pin, int balance) {
            this.cardNo = cardNo;
            this.name = name;
            this.pin = pin;
            this.balance = balance;
        }

        void record(String type, int amount) {
            history.push(new Transaction(type, amount, balance));
        }
    }

    // GREEDY CASH DISPENSING ALGORITHM
    static int[] dispense(int amount) {

        int[] used = new int[NOTES.length];
        int remaining = amount;

        for (int i = 0; i < NOTES.length; i++) {

            int n = Math.min(
                remaining / NOTES[i],
                cashStock[i]
            );

            used[i] = n;
            remaining -= n * NOTES[i];
        }

        return remaining == 0 ? used : null;
    }

    // INPUT HELPER
    static int readInt(Scanner sc, String prompt) {

        System.out.print(prompt);

        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // LOGIN SYSTEM
    static boolean login(Account acc, Scanner sc) {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            System.out.print("Enter PIN: ");

            String pin = sc.nextLine().trim();

            if (acc.pin.equals(pin)) {
                return true;
            }

            System.out.println(
                "Wrong PIN. Attempts left: " +
                (MAX_ATTEMPTS - attempt)
            );
        }

        blockedCards.add(acc.cardNo);

        System.out.println(
            "Card blocked after 3 wrong attempts. Contact the bank."
        );

        return false;
    }

    // WITHDRAW CASH
    static void withdraw(Account acc, Scanner sc) {

        int amt = readInt(
            sc, "Enter amount (multiple of 100): Rs. "
        );

        if (amt <= 0 || amt % 100 != 0) {
            System.out.println("Invalid amount.");
            return;
        }

        if (amt > acc.balance) {
            System.out.println("Insufficient balance.");
            return;
        }

        if (acc.withdrawnToday + amt > DAILY_LIMIT) {
            System.out.println(
                "Daily limit of Rs. " + DAILY_LIMIT + " exceeded."
            );
            return;
        }

        int[] used = dispense(amt);

        if (used == null) {
            System.out.println(
                "ATM cannot dispense this amount right now."
            );
            return;
        }

        for (int i = 0; i < NOTES.length; i++) {
            cashStock[i] -= used[i];
        }

        acc.balance -= amt;
        acc.withdrawnToday += amt;

        acc.record("WITHDRAW", amt);

        System.out.println("Please collect your cash:");

        for (int i = 0; i < NOTES.length; i++) {

            if (used[i] > 0) {
                System.out.println(
                    " Rs. " + NOTES[i] + " x " + used[i]
                );
            }
        }

        System.out.println(
            "Remaining balance: Rs. " + acc.balance
        );
    }

    // DEPOSIT CASH
    static void deposit(Account acc, Scanner sc) {

        int amt = readInt(
            sc, "Enter deposit amount: Rs. "
        );

        if (amt <= 0) {
            System.out.println("Invalid amount.");
            return;
        }

        acc.balance += amt;

        acc.record("DEPOSIT", amt);

        System.out.println(
            "Deposit successful. Balance: Rs. " + acc.balance
        );
    }

    // TRANSFER MONEY
    static void transfer(Account acc, Scanner sc) {

        System.out.print("Receiver card number: ");

        Account receiver = accounts.get(
            sc.nextLine().trim()
        );

        if (receiver == null || receiver == acc) {
            System.out.println("Invalid receiver account.");
            return;
        }

        int amt = readInt(
            sc, "Enter amount to transfer: Rs. "
        );

        if (amt <= 0 || amt > acc.balance) {
            System.out.println(
                "Invalid amount or insufficient balance."
            );
            return;
        }

        acc.balance -= amt;
        receiver.balance += amt;

        acc.record("TRANSFER OUT", amt);
        receiver.record("TRANSFER IN", amt);

        System.out.println(
            "Rs. " + amt + " transferred to " +
            receiver.name + "."
        );

        System.out.println(
            "Remaining balance: Rs. " + acc.balance
        );
    }

    // CHANGE PIN
    static void changePin(Account acc, Scanner sc) {

        System.out.print("Enter old PIN: ");

        if (!acc.pin.equals(sc.nextLine().trim())) {
            System.out.println("Incorrect old PIN.");
            return;
        }

        System.out.print("Enter new 4-digit PIN: ");

        String np = sc.nextLine().trim();

        if (!np.matches("\\d{4}")) {
            System.out.println(
                "PIN must be exactly 4 digits."
            );
            return;
        }

        acc.pin = np;

        System.out.println("PIN changed successfully.");
    }

    // MINI STATEMENT
    static void miniStatement(Account acc) {

        if (acc.history.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }

        System.out.println(
            "\n--- MINI STATEMENT (latest first) ---"
        );

        int shown = 0;

        for (
            int i = acc.history.size() - 1;
            i >= 0 && shown < 5;
            i--, shown++
        ) {
            System.out.println(acc.history.get(i));
        }
    }

    // CUSTOMER ATM SESSION
    static void session(Account acc, Scanner sc) {

        while (true) {

            System.out.println("\n--- ATM MENU ---");
            System.out.println("1. Check balance");
            System.out.println("2. Withdraw cash");
            System.out.println("3. Deposit cash");
            System.out.println("4. Transfer money");
            System.out.println("5. Change PIN");
            System.out.println("6. Mini statement (last 5)");
            System.out.println("0. Exit");

            System.out.print("Enter your choice: ");

            if (!sc.hasNextLine()) {
                return;
            }

            switch (sc.nextLine().trim()) {

                case "1":

                    System.out.println(
                        "Available balance: Rs. " + acc.balance
                    );

                    if (!acc.history.isEmpty()) {
                        System.out.println(
                            "Last transaction: " +
                            acc.history.peek()
                        );
                    }

                    break;

                case "2":
                    withdraw(acc, sc);
                    break;

                case "3":
                    deposit(acc, sc);
                    break;

                case "4":
                    transfer(acc, sc);
                    break;

                case "5":
                    changePin(acc, sc);
                    break;

                case "6":
                    miniStatement(acc);
                    break;

                case "0":

                    System.out.println(
                        "Please take your card. Thank you!"
                    );

                    return;

                default:
                    System.out.println(
                        "Invalid choice. Try again."
                    );
            }
        }
    }

    // JOIN ATM QUEUE
    static void joinQueue(Scanner sc) {

        System.out.print("Enter card number: ");

        String card = sc.nextLine().trim();

        if (!accounts.containsKey(card)) {
            System.out.println("Card not recognised.");
            return;
        }

        if (waitingQueue.contains(card)) {
            System.out.println(
                "This card is already in the queue."
            );
            return;
        }

        waitingQueue.add(card);

        System.out.println(
            "Card " + card +
            " added to queue. Position: " +
            waitingQueue.size()
        );
    }

    // VIEW WAITING QUEUE
    static void viewQueue() {

        if (waitingQueue.isEmpty()) {
            System.out.println("Queue is empty.");
            return;
        }

        System.out.println("\n--- WAITING QUEUE ---");

        int pos = 1;

        for (String card : waitingQueue) {

            System.out.println(
                pos++ + ". " + card +
                " (" + accounts.get(card).name + ")"
            );
        }
    }

    // SERVE NEXT CUSTOMER
    static void serveNext(Scanner sc) {

        String card = waitingQueue.poll();

        if (card == null) {
            System.out.println("No customer in queue.");
            return;
        }

        Account acc = accounts.get(card);

        System.out.println(
            "\nNow serving card " + card +
            " (" + acc.name + ")"
        );

        if (blockedCards.contains(card)) {
            System.out.println(
                "This card is blocked. Contact the bank."
            );
            return;
        }

        if (login(acc, sc)) {

            System.out.println(
                "Welcome, " + acc.name + "!"
            );

            session(acc, sc);
        }
    }

    // OPEN NEW ACCOUNT
    static void openAccount(Scanner sc) {

        System.out.print("Customer name: ");

        String name = sc.nextLine().trim();

        System.out.print("Set 4-digit PIN: ");

        String pin = sc.nextLine().trim();

        int open = readInt(
            sc, "Opening deposit: Rs. "
        );

        if (
            name.isEmpty() ||
            !pin.matches("\\d{4}") ||
            open < 0
        ) {

            System.out.println(
                "Invalid details. Account not created."
            );

            return;
        }

        String card = String.valueOf(nextCard++);

        accounts.put(
            card,
            new Account(card, name, pin, open)
        );

        System.out.println(
            "Account created. Your card number is " + card
        );
    }

    // UNBLOCK CARD
    static void unblockCard(Scanner sc) {

        System.out.print("Enter card number: ");

        String card = sc.nextLine().trim();

        if (blockedCards.remove(card)) {

            System.out.println(
                "Card " + card + " unblocked."
            );

        } else {

            System.out.println(
                "This card is not blocked."
            );
        }
    }

    // SAMPLE ACCOUNTS
    static void seedAccounts() {

        accounts.put(
            "1001",
            new Account("1001", "Aarav Sharma", "1234", 15000)
        );

        accounts.put(
            "1002",
            new Account("1002", "Neha Verma", "4321", 8000)
        );

        accounts.put(
            "1003",
            new Account("1003", "Rohan Patel", "1111", 5000)
        );
    }

    // MAIN METHOD
    public static void main(String[] args) {

        seedAccounts();

        Scanner sc = new Scanner(System.in);

        System.out.println("=====================================");
        System.out.println("            ATM SIMULATOR");
        System.out.println("=====================================");

        while (true) {

            System.out.println("\n1. Join queue (insert card)");
            System.out.println("2. Serve next customer");
            System.out.println("3. View waiting queue");
            System.out.println("4. Open a new account");
            System.out.println("5. Unblock a card (bank admin)");
            System.out.println("0. Exit");

            System.out.print("Enter your choice: ");

            if (!sc.hasNextLine()) {
                break;
            }

            switch (sc.nextLine().trim()) {

                case "1":
                    joinQueue(sc);
                    break;

                case "2":
                    serveNext(sc);
                    break;

                case "3":
                    viewQueue();
                    break;

                case "4":
                    openAccount(sc);
                    break;

                case "5":
                    unblockCard(sc);
                    break;

                case "0":

                    System.out.println(
                        "Thank you for using the ATM. Goodbye!"
                    );

                    sc.close();
                    return;

                default:

                    System.out.println(
                        "Invalid choice. Try again."
                    );
            }
        }

        sc.close();
    }
}
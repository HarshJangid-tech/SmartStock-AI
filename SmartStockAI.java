
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class Product {
    String name;
    int quantity;
    double price;
    int sold;

    Product(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.sold = sold;
    }
}

public class SmartStockAI {
    static Scanner sc = new Scanner(System.in);
    static ArrayList<Product> products = new ArrayList<>();

    static final String DATA_FILE = "smartstock_data.txt";
    static final int LOW_STOCK_LIMIT = 5;

    static double totalRevenue = 0;
    static int totalTransactions = 0;

    static int readInt(String message) {
        while (true) {
            System.out.print(message);
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                if (value >= 0) return value;
                System.out.println("Please enter zero or a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    static double readPrice(String message) {
        while (true) {
            System.out.print(message);
            try {
                double value = Double.parseDouble(sc.nextLine().trim());
                if (Double.isFinite(value) && value >= 0) return value;
            } catch (NumberFormatException e) {
                // Ask again.
            }
            System.out.println("Enter a valid non-negative price.");
        }
    }

    static String readName(String message) {
        while (true) {
            System.out.print(message);
            String name = sc.nextLine().trim();

            if (name.isEmpty()) {
                System.out.println("Product name cannot be empty.");
            } else if (name.contains("|")) {
                System.out.println("Please do not use the | character.");
            } else {
                return name;
            }
        }
    }

    static Product findProduct(String name) {
        for (Product p : products) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    static String money(double amount) {
        return String.format(Locale.US, "Rs. %.2f", amount);
    }

    static void saveData() {
        try (PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter(DATA_FILE)))) {

            out.println("META|" + totalRevenue + "|" + totalTransactions);

            for (Product p : products) {
                String encodedName = Base64.getEncoder().encodeToString(
                    p.name.getBytes(StandardCharsets.UTF_8));

                out.println("P|" + encodedName + "|" + p.quantity
                    + "|" + p.price + "|" + p.sold);
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not save inventory.");
        }
    }

    static void loadData() {
        File file = new File(DATA_FILE);

        if (!file.exists()) {
            addDemoProducts();
            saveData();
            return;
        }

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|", -1);

                try {
                    if (parts.length == 3 && parts[0].equals("META")) {
                        totalRevenue = Double.parseDouble(parts[1]);
                        totalTransactions = Integer.parseInt(parts[2]);
                    } else if (parts.length == 5 && parts[0].equals("P")) {
                        String name = new String(
                            Base64.getDecoder().decode(parts[1]),
                            StandardCharsets.UTF_8);

                        int quantity = Integer.parseInt(parts[2]);
                        double price = Double.parseDouble(parts[3]);
                        int sold = Integer.parseInt(parts[4]);

                        if (quantity >= 0 && sold >= 0
                                && Double.isFinite(price) && price >= 0
                                && findProduct(name) == null) {
                            Product p = new Product(name, quantity, price);
                            p.sold = sold;
                            products.add(p);
                        }
                    } else if (parts.length == 4) {
                        // Support the earlier prototype's data format.
                        String name = parts[0];
                        int quantity = Integer.parseInt(parts[1]);
                        double price = Double.parseDouble(parts[2]);
                        int sold = Integer.parseInt(parts[3]);

                        if (!name.isEmpty() && quantity >= 0 && sold >= 0
                                && Double.isFinite(price) && price >= 0
                                && findProduct(name) == null) {
                            Product p = new Product(name, quantity, price);
                            p.sold = sold;
                            products.add(p);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Skipped one invalid saved record.");
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read saved data.");
        }

        if (products.isEmpty()) {
            addDemoProducts();
        }

        saveData();
    }

    static void addDemoProducts() {
        products.add(new Product("Rice", 20, 60));
        products.add(new Product("Milk", 4, 30));
        products.add(new Product("Soap", 12, 40));
    }

    static void addProduct() {
        String name = readName("Product name: ");

        if (findProduct(name) != null) {
            System.out.println("This product already exists.");
            return;
        }

        int quantity = readInt("Opening stock: ");
        double price = readPrice("Price per unit: ");

        products.add(new Product(name, quantity, price));
        saveData();
        System.out.println("Product added and saved successfully!");
    }

    static void viewInventory() {
        System.out.println("\n========== INVENTORY ==========");

        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }

        System.out.printf("%-18s %10s %12s %14s%n",
                "PRODUCT", "QUANTITY", "PRICE", "STOCK VALUE");
        System.out.println("--------------------------------------------------------");

        for (Product p : products) {
            System.out.printf("%-18s %10d %12.2f %14.2f%n",
                    p.name, p.quantity, p.price, p.quantity * p.price);
        }
    }

    static void searchProduct() {
        String name = readName("Enter product name to search: ");
        Product p = findProduct(name);

        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        System.out.println("\nProduct: " + p.name);
        System.out.println("Available stock: " + p.quantity);
        System.out.println("Price per unit: " + money(p.price));
        System.out.println("Units sold (total): " + p.sold);
        System.out.println("Current stock value: "
                + money(p.quantity * p.price));
    }

    static void updateStock() {
        String name = readName("Product to restock: ");
        Product p = findProduct(name);

        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        int extra = readInt("Units to add: ");

        if (extra > Integer.MAX_VALUE - p.quantity) {
            System.out.println("Quantity is too large.");
            return;
        }

        p.quantity += extra;
        saveData();

        System.out.println("Stock updated! New quantity: " + p.quantity);
    }

    static void sellProduct() {
        String name = readName("Product sold: ");
        Product p = findProduct(name);

        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        int quantity = readInt("Units sold: ");

        if (quantity == 0) {
            System.out.println("Sale quantity must be greater than zero.");
            return;
        }

        if (quantity > p.quantity) {
            System.out.println("Not enough stock. Available: " + p.quantity);
            return;
        }

        double revenue = quantity * p.price;

        if (!Double.isFinite(revenue)
                || !Double.isFinite(totalRevenue + revenue)
                || quantity > Integer.MAX_VALUE - p.sold
                || totalTransactions == Integer.MAX_VALUE) {
            System.out.println("Sale is too large to record.");
            return;
        }

        p.quantity -= quantity;
        p.sold += quantity;
        totalRevenue += revenue;
        totalTransactions++;

        saveData();

        System.out.println("\nSale recorded successfully!");
        System.out.println("Product: " + p.name);
        System.out.println("Units sold: " + quantity);
        System.out.println("Sale amount: " + money(revenue));
        System.out.println("Remaining stock: " + p.quantity);
    }

    static void lowStock() {
        System.out.println("\n========== LOW-STOCK ALERTS ==========");
        boolean found = false;

        for (Product p : products) {
            if (p.quantity <= LOW_STOCK_LIMIT) {
                found = true;
                System.out.println("WARNING: " + p.name
                        + " has only " + p.quantity + " units left.");
            }
        }

        if (!found) {
            System.out.println("All products have sufficient stock.");
        }
    }

    static void stockInsights() {
        System.out.println("\n========== STOCK INSIGHTS ==========");

        if (products.isEmpty()) {
            System.out.println("Add products to see insights.");
            return;
        }

        for (Product p : products) {
            System.out.println("\nProduct: " + p.name);
            System.out.println("Current units: " + p.quantity);
            System.out.println("Total units sold: " + p.sold);

            if (p.quantity <= LOW_STOCK_LIMIT) {
                System.out.println("Suggestion: Restock soon.");
            } else if (p.sold >= 10 && p.quantity <= 10) {
                System.out.println("Suggestion: Monitor stock closely.");
            } else {
                System.out.println("Suggestion: Stock level looks okay.");
            }
        }

        System.out.println("\nThese are rule-based suggestions,");
        System.out.println("not predictions from a trained AI model.");
    }

    static void dashboard() {
        int units = 0;
        int lowCount = 0;
        double inventoryValue = 0;

        for (Product p : products) {
            units += p.quantity;
            inventoryValue += p.quantity * p.price;

            if (p.quantity <= LOW_STOCK_LIMIT) {
                lowCount++;
            }
        }

        System.out.println("\n========== SMARTSTOCK DASHBOARD ==========");
        System.out.println("Product types: " + products.size());
        System.out.println("Total units in stock: " + units);
        System.out.println("Low-stock products: " + lowCount);
        System.out.println("Inventory value: " + money(inventoryValue));
        System.out.println("Recorded sales: " + totalTransactions);
        System.out.println("Total sales revenue: " + money(totalRevenue));
        System.out.println("Data file: " + DATA_FILE);
    }

    static void showMenu() {
        System.out.println("\n========== SMARTSTOCK AI ==========");
        System.out.println("1. Add product");
        System.out.println("2. View inventory");
        System.out.println("3. Record a sale");
        System.out.println("4. Low-stock alerts");
        System.out.println("5. Stock insights");
        System.out.println("6. Dashboard");
        System.out.println("7. Search product");
        System.out.println("8. Restock product");
        System.out.println("0. Save and exit");
    }

    public static void main(String[] args) {
        loadData();

        System.out.println("\nWelcome to SmartStock AI!");
        System.out.println("Your inventory data is saved locally.");

        while (true) {
            showMenu();
            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1:
                    addProduct();
                    break;
                case 2:
                    viewInventory();
                    break;
                case 3:
                    sellProduct();
                    break;
                case 4:
                    lowStock();
                    break;
                case 5:
                    stockInsights();
                    break;
                case 6:
                    dashboard();
                    break;
                case 7:
                    searchProduct();
                    break;
                case 8:
                    updateStock();
                    break;
                case 0:
                    saveData();
                    System.out.println("Inventory saved. Goodbye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Choose an option from 0 to 8.");
            }
        }
    }
}

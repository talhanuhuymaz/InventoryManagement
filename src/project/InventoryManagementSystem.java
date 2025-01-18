package project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InventoryManagementSystem {

	private static final String DB_URL = "jdbc:sqlite:inventory.db";
	private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);
	private static final Color PRIMARY_COLOR = new Color(51, 153, 255);
    private static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            configureDatabaseConnection(conn);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Connection Failed: " + e.getMessage());
        }
        return conn;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                initializeDatabase();
                createMainWindow();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage());
            }
        });
    }

    private static void createMainWindow() {
        JFrame frame = new JFrame("Inventory Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new GridLayout(4, 1));

        JButton manageInventoryButton = new JButton("Manage Inventory");
        JButton manageSuppliersButton = new JButton("Manage Suppliers");
        JButton manageSalesButton = new JButton("Manage Sales");
        JButton viewReportButton = new JButton("View Sales Report");

        frame.add(manageInventoryButton);
        frame.add(manageSuppliersButton);
        frame.add(manageSalesButton);
        frame.add(viewReportButton);

        manageInventoryButton.addActionListener(e -> openInventoryWindow());
        manageSuppliersButton.addActionListener(e -> openSuppliersWindow());
        manageSalesButton.addActionListener(e -> openSalesWindow());
        viewReportButton.addActionListener(e -> viewSalesReport());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void initializeDatabase() {
        // Create tables with correct structure
        String createCardsTable = "CREATE TABLE IF NOT EXISTS Cards (" +
                "card_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "category TEXT," +
                "rarity TEXT," +
                "value REAL," +
                "quantity INTEGER" +
                ");";

        String createSuppliersTable = "CREATE TABLE IF NOT EXISTS Suppliers (" +
                "supplier_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "contact TEXT," +
                "email TEXT," +
                "phone TEXT" +
                ");";

        String createSalesTable = "CREATE TABLE IF NOT EXISTS Sales (" +
                "sale_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "card_id INTEGER," +
                "supplier_id INTEGER," +
                "quantity INTEGER," +
                "sale_date TEXT," +
                "sale_price REAL," +
                "FOREIGN KEY(card_id) REFERENCES Cards(card_id)," +
                "FOREIGN KEY(supplier_id) REFERENCES Suppliers(supplier_id)" +
                ");";

        String createSupplierPricesTable = "CREATE TABLE IF NOT EXISTS SupplierPrices (" +
                "price_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "supplier_id INTEGER," +
                "card_id INTEGER," +
                "price REAL," +
                "last_updated DATE," +
                "FOREIGN KEY(supplier_id) REFERENCES Suppliers(supplier_id)," +
                "FOREIGN KEY(card_id) REFERENCES Cards(card_id)" +
                ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Create tables if they don't exist
            stmt.execute(createCardsTable);
            stmt.execute(createSuppliersTable);
            stmt.execute(createSalesTable);
            stmt.execute(createSupplierPricesTable);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Failed to Initialize Database: " + e.getMessage());
        }
    }

    // Existing Inventory Management Methods
    private static void openInventoryWindow() {
        JFrame inventoryFrame = new JFrame("Manage Inventory");
        inventoryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        inventoryFrame.setSize(800, 600);

        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        model.addColumn("Card ID");
        model.addColumn("Name");
        model.addColumn("Category");
        model.addColumn("Rarity");
        model.addColumn("Value");
        model.addColumn("Quantity");

        loadInventoryData(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Card");
        JButton updateButton = new JButton("Update Card");
        JButton deleteButton = new JButton("Delete Card");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        inventoryFrame.add(panel);
        inventoryFrame.setVisible(true);

        addButton.addActionListener(e -> addCard(model));
        updateButton.addActionListener(e -> updateCard(table, model));
        deleteButton.addActionListener(e -> deleteCard(table, model));
        
        JButton comparePricesButton = new JButton("Compare Prices");
        buttonPanel.add(comparePricesButton);

        comparePricesButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(null, "Please select a card to compare prices.");
                return;
            }
            int cardId = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
            comparePrices(cardId);
        });
    }

    private static void loadInventoryData(DefaultTableModel model) {
        String query = "SELECT * FROM Cards";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("card_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("rarity"),
                        rs.getDouble("value"),
                        rs.getInt("quantity")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error Loading Inventory Data: " + e.getMessage());
        }
    }

    private static void addCard(DefaultTableModel model) {
        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField rarityField = new JTextField();
        JTextField valueField = new JTextField();
        JTextField quantityField = new JTextField();

        Object[] fields = {
                "Name:", nameField,
                "Category:", categoryField,
                "Rarity:", rarityField,
                "Value:", valueField,
                "Quantity:", quantityField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Add New Card", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String sql = "INSERT INTO Cards(name, category, rarity, value, quantity) VALUES(?,?,?,?,?)";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, categoryField.getText());
                pstmt.setString(3, rarityField.getText());
                pstmt.setDouble(4, Double.parseDouble(valueField.getText()));
                pstmt.setInt(5, Integer.parseInt(quantityField.getText()));
                pstmt.executeUpdate();

                model.addRow(new Object[] {
                        null,
                        nameField.getText(),
                        categoryField.getText(),
                        rarityField.getText(),
                        Double.parseDouble(valueField.getText()),
                        Integer.parseInt(quantityField.getText())
                });

                JOptionPane.showMessageDialog(null, "Card Added Successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error Adding Card: " + e.getMessage());
            }
        }
    }

    private static void updateCard(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a card to update.");
            return;
        }

        Object cardId = model.getValueAt(selectedRow, 0);
        JTextField nameField = new JTextField(model.getValueAt(selectedRow, 1).toString());
        JTextField categoryField = new JTextField(model.getValueAt(selectedRow, 2).toString());
        JTextField rarityField = new JTextField(model.getValueAt(selectedRow, 3).toString());
        JTextField valueField = new JTextField(model.getValueAt(selectedRow, 4).toString());
        JTextField quantityField = new JTextField(model.getValueAt(selectedRow, 5).toString());

        Object[] fields = {
                "Name:", nameField,
                "Category:", categoryField,
                "Rarity:", rarityField,
                "Value:", valueField,
                "Quantity:", quantityField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Update Card", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String sql = "UPDATE Cards SET name=?, category=?, rarity=?, value=?, quantity=? WHERE card_id=?";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, categoryField.getText());
                pstmt.setString(3, rarityField.getText());
                pstmt.setDouble(4, Double.parseDouble(valueField.getText()));
                pstmt.setInt(5, Integer.parseInt(quantityField.getText()));
                pstmt.setInt(6, Integer.parseInt(cardId.toString()));

                pstmt.executeUpdate();

                model.setValueAt(nameField.getText(), selectedRow, 1);
                model.setValueAt(categoryField.getText(), selectedRow, 2);
                model.setValueAt(rarityField.getText(), selectedRow, 3);
                model.setValueAt(Double.parseDouble(valueField.getText()), selectedRow, 4);
                model.setValueAt(Integer.parseInt(quantityField.getText()), selectedRow, 5);

                JOptionPane.showMessageDialog(null, "Card Updated Successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error Updating Card: " + e.getMessage());
            }
        }
    }

    private static void deleteCard(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a card to delete.");
            return;
        }

        Object cardId = model.getValueAt(selectedRow, 0);
        String sql = "DELETE FROM Cards WHERE card_id=?";

        int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this card?",
                "Delete Confirmation", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(cardId.toString()));
                pstmt.executeUpdate();

                model.removeRow(selectedRow);
                JOptionPane.showMessageDialog(null, "Card Deleted Successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error Deleting Card: " + e.getMessage());
            }
        }
    }

    // New Supplier Management Methods
    private static void openSuppliersWindow() {
        JFrame suppliersFrame = new JFrame("Manage Suppliers");
        suppliersFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        suppliersFrame.setSize(800, 600);

        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        model.addColumn("Supplier ID");
        model.addColumn("Name");
        model.addColumn("Contact");
        model.addColumn("Email");
        model.addColumn("Phone");

        loadSuppliersData(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Supplier");
        JButton updateButton = new JButton("Update Supplier");
        JButton deleteButton = new JButton("Delete Supplier");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        suppliersFrame.add(panel);
        suppliersFrame.setVisible(true);

        addButton.addActionListener(e -> addSupplier(model));
        updateButton.addActionListener(e -> updateSupplier(table, model));
        deleteButton.addActionListener(e -> deleteSupplier(table, model));
    }

    private static void loadSuppliersData(DefaultTableModel model) {
        String query = "SELECT * FROM Suppliers";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("supplier_id"),
                    rs.getString("name"),
                    rs.getString("contact"),
                    rs.getString("email"),
                    rs.getString("phone")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error Loading Suppliers Data: " + e.getMessage());
        }
    }

    private static void addSupplier(DefaultTableModel model) {
        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField phoneField = new JTextField();

        Object[] fields = {
            "Name:", nameField,
            "Contact Person:", contactField,
            "Email:", emailField,
            "Phone:", phoneField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Add New Supplier", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String sql = "INSERT INTO Suppliers(name, contact, email, phone) VALUES(?,?,?,?)";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, contactField.getText());
                pstmt.setString(3, emailField.getText());
                pstmt.setString(4, phoneField.getText());
                pstmt.executeUpdate();

                model.addRow(new Object[]{
                    null,
                    nameField.getText(),
                    contactField.getText(),
                    emailField.getText(),
                    phoneField.getText()
                });

                JOptionPane.showMessageDialog(null, "Supplier Added Successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error Adding Supplier: " + e.getMessage());
            }
        }
    }

    private static void updateSupplier(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a supplier to update.");
            return;
        }

        Object supplierId = model.getValueAt(selectedRow, 0);
        JTextField nameField = new JTextField(model.getValueAt(selectedRow, 1).toString());
        JTextField contactField = new JTextField(model.getValueAt(selectedRow, 2).toString());
        JTextField emailField = new JTextField(model.getValueAt(selectedRow, 3).toString());
        JTextField phoneField = new JTextField(model.getValueAt(selectedRow, 4).toString());

        Object[] fields = {
            "Name:", nameField,
            "Contact Person:", contactField,
            "Email:", emailField,
            "Phone:", phoneField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Update Supplier", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String sql = "UPDATE Suppliers SET name=?, contact=?, email=?, phone=? WHERE supplier_id=?";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, nameField.getText());
                pstmt.setString(2, contactField.getText());
                pstmt.setString(3, emailField.getText());
                pstmt.setString(4, phoneField.getText());
                pstmt.setInt(5, Integer.parseInt(supplierId.toString()));

                pstmt.executeUpdate();

                model.setValueAt(nameField.getText(), selectedRow, 1);
                model.setValueAt(contactField.getText(), selectedRow, 2);
                model.setValueAt(emailField.getText(), selectedRow, 3);
                model.setValueAt(phoneField.getText(), selectedRow, 4);

                JOptionPane.showMessageDialog(null, "Supplier Updated Successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error Updating Supplier: " + e.getMessage());
            }
        }
    }

    private static void deleteSupplier(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a supplier to delete.");
            return;
        }

        Object supplierId = model.getValueAt(selectedRow, 0);
        int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this supplier?",
                "Delete Confirmation", JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM Suppliers WHERE supplier_id=?";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(supplierId.toString()));
                pstmt.executeUpdate();

                model.removeRow(selectedRow);
                JOptionPane.showMessageDialog(null, "Supplier Deleted Successfully!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error Deleting Supplier: " + e.getMessage());
            }
        }
    }
    
 // Add a method to compare prices
    private static void comparePrices(int cardId) {
        JFrame compareFrame = new JFrame("Price Comparison");
        compareFrame.setSize(600, 400);
        compareFrame.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        model.addColumn("Supplier Name");
        model.addColumn("Price");
        model.addColumn("Last Updated");

        String query = "SELECT s.name, sp.price, sp.last_updated " +
                      "FROM Suppliers s " +
                      "JOIN SupplierPrices sp ON s.supplier_id = sp.supplier_id " +
                      "WHERE sp.card_id = ? " +
                      "ORDER BY sp.price ASC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, cardId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    String.format("$%.2f", rs.getDouble("price")),
                    rs.getString("last_updated")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading price comparison: " + e.getMessage());
        }

        JScrollPane scrollPane = new JScrollPane(table);
        compareFrame.add(scrollPane, BorderLayout.CENTER);

        // Add a button to record new prices
        JButton addPriceButton = new JButton("Record New Price");
        addPriceButton.addActionListener(e -> recordNewPrice(cardId));
        
        compareFrame.add(addPriceButton, BorderLayout.SOUTH);
        compareFrame.setVisible(true);
    }

    // Method to record a new price
    private static void recordNewPrice(int cardId) {
        // Get list of suppliers
        ArrayList<String> supplierNames = new ArrayList<>();
        ArrayList<Integer> supplierIds = new ArrayList<>();
        
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT supplier_id, name FROM Suppliers")) {
            while (rs.next()) {
                supplierIds.add(rs.getInt("supplier_id"));
                supplierNames.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading suppliers: " + e.getMessage());
            return;
        }

        // Create input dialog
        JComboBox<String> supplierCombo = new JComboBox<>(supplierNames.toArray(new String[0]));
        JTextField priceField = new JTextField();

        Object[] fields = {
            "Supplier:", supplierCombo,
            "Price:", priceField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Record New Price", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(priceField.getText());
                int supplierId = supplierIds.get(supplierCombo.getSelectedIndex());

                String sql = "INSERT OR REPLACE INTO SupplierPrices (supplier_id, card_id, price, last_updated) " +
                            "VALUES (?, ?, ?, date('now'))";
                
                try (Connection conn = connect();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, supplierId);
                    pstmt.setInt(2, cardId);
                    pstmt.setDouble(3, price);
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(null, "Price recorded successfully!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter a valid price!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error recording price: " + ex.getMessage());
            }
        }
    }
    
    

    private static void openSalesWindow() {
        JFrame salesFrame = new JFrame("Manage Sales");
        salesFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salesFrame.setSize(800, 600);
        salesFrame.setLocationRelativeTo(null);  // Center the window

        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // Make table read-only
            }
        };
        JTable table = new JTable(model);
        
        // Define columns
        model.addColumn("Sale ID");
        model.addColumn("Card Name");
        model.addColumn("Supplier Name");
        model.addColumn("Quantity");
        model.addColumn("Sale Date");
        model.addColumn("Sale Price");

        // Load initial data
        loadSalesData(model);
        JScrollPane scrollPane = new JScrollPane(table);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Record Sale");
        JButton deleteButton = new JButton("Delete Sale");
        JButton refreshButton = new JButton("Refresh");  // Add refresh button

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);  // Add refresh button to panel

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        salesFrame.add(panel);

        // Add action listeners
        addButton.addActionListener(e -> {
            recordSale(model);
            loadSalesData(model);  // Refresh after adding
        });
        
        deleteButton.addActionListener(e -> {
            deleteSale(table, model);
            loadSalesData(model);  // Refresh after deleting
        });
        
        refreshButton.addActionListener(e -> {
            model.setRowCount(0);  // Clear existing data
            loadSalesData(model);  // Reload data
        });

        salesFrame.setVisible(true);
    }

    // Update loadSalesData method to clear existing data first
    private static void loadSalesData(DefaultTableModel model) {
        model.setRowCount(0);  // Clear existing data
        String query = "SELECT s.sale_id, c.name as card_name, sup.name as supplier_name, " +
                       "s.quantity, s.sale_date, s.sale_price " +
                       "FROM Sales s " +
                       "JOIN Cards c ON s.card_id = c.card_id " +
                       "JOIN Suppliers sup ON s.supplier_id = sup.supplier_id " +
                       "ORDER BY s.sale_date DESC";  // Show newest sales first
                       
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("sale_id"),
                    rs.getString("card_name"),
                    rs.getString("supplier_name"),
                    rs.getInt("quantity"),
                    rs.getString("sale_date"),
                    String.format("$%.2f", rs.getDouble("sale_price"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error Loading Sales Data: " + e.getMessage());
        }
    }

 // Modify the sales recording interface
    private static void recordSale(DefaultTableModel model) {
        // Get list of cards and suppliers for dropdown
        Map<String, Integer> cardMap = new HashMap<>();
        Map<String, Integer> supplierMap = new HashMap<>();
        
        // Load cards
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT card_id, name FROM Cards")) {
            while (rs.next()) {
                cardMap.put(rs.getString("name"), rs.getInt("card_id"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading cards: " + e.getMessage());
            return;
        }

        // Load suppliers
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT supplier_id, name FROM Suppliers")) {
            while (rs.next()) {
                supplierMap.put(rs.getString("name"), rs.getInt("supplier_id"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading suppliers: " + e.getMessage());
            return;
        }

        JComboBox<String> cardCombo = new JComboBox<>(cardMap.keySet().toArray(new String[0]));
        JComboBox<String> supplierCombo = new JComboBox<>(supplierMap.keySet().toArray(new String[0]));
        JTextField quantityField = new JTextField();
        JTextField priceField = new JTextField();

        Object[] fields = {
            "Card:", cardCombo,
            "Supplier:", supplierCombo,
            "Quantity:", quantityField,
            "Sale Price:", priceField
        };

        int option = JOptionPane.showConfirmDialog(null, fields, "Record Sale", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            Connection conn = null;
            try {
                conn = connect();
                conn.setAutoCommit(false);  // Start transaction

                int cardId = cardMap.get(cardCombo.getSelectedItem().toString());
                int supplierId = supplierMap.get(supplierCombo.getSelectedItem().toString());
                int quantity = Integer.parseInt(quantityField.getText());
                double price = Double.parseDouble(priceField.getText());

                // First check if we have enough quantity
                String checkQuery = "SELECT quantity FROM Cards WHERE card_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, cardId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        int currentQuantity = rs.getInt("quantity");
                        if (currentQuantity < quantity) {
                            throw new SQLException("Insufficient quantity available. Current stock: " + currentQuantity);
                        }
                    }
                }

                // Update card quantity
                String updateQuantitySQL = "UPDATE Cards SET quantity = quantity - ? WHERE card_id = ?";
                try (PreparedStatement quantityStmt = conn.prepareStatement(updateQuantitySQL)) {
                    quantityStmt.setInt(1, quantity);
                    quantityStmt.setInt(2, cardId);
                    quantityStmt.executeUpdate();
                }

                // Record the sale
                String insertSaleSQL = "INSERT INTO Sales(card_id, supplier_id, quantity, sale_date, sale_price) VALUES(?, ?, ?, date('now'), ?)";
                try (PreparedStatement saleStmt = conn.prepareStatement(insertSaleSQL)) {
                    saleStmt.setInt(1, cardId);
                    saleStmt.setInt(2, supplierId);
                    saleStmt.setInt(3, quantity);
                    saleStmt.setDouble(4, price);
                    saleStmt.executeUpdate();
                }

                conn.commit();  // Commit transaction

                // Update the sales table display
                model.addRow(new Object[]{
                    null,
                    cardCombo.getSelectedItem(),
                    supplierCombo.getSelectedItem(),
                    quantity,
                    LocalDate.now().toString(),
                    price
                });

                JOptionPane.showMessageDialog(null, "Sale recorded successfully!");
            } catch (NumberFormatException ex) {
                if (conn != null) try { conn.rollback(); } catch (SQLException e) { }
                JOptionPane.showMessageDialog(null, "Please enter valid numbers for quantity and price!");
            } catch (SQLException ex) {
                if (conn != null) try { conn.rollback(); } catch (SQLException e) { }
                JOptionPane.showMessageDialog(null, "Error recording sale: " + ex.getMessage());
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) { }
                }
            }
        }
    }

    // Add a method to view sales reports
    private static void viewSalesReport() {
        JFrame reportFrame = new JFrame("Sales Report");
        reportFrame.setSize(1000, 600);
        reportFrame.setLayout(new BorderLayout());

        // Create panel for filter options
        JPanel filterPanel = new JPanel();
        JComboBox<String> supplierFilter = new JComboBox<>(new String[]{"All Suppliers"});
        JTextField dateFromField = new JTextField(10);
        JTextField dateToField = new JTextField(10);
        JButton applyFilter = new JButton("Apply Filter");

        filterPanel.add(new JLabel("Supplier:"));
        filterPanel.add(supplierFilter);
        filterPanel.add(new JLabel("From Date (YYYY-MM-DD):"));
        filterPanel.add(dateFromField);
        filterPanel.add(new JLabel("To Date (YYYY-MM-DD):"));
        filterPanel.add(dateToField);
        filterPanel.add(applyFilter);

        // Create table for sales data
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        model.addColumn("Sale ID");
        model.addColumn("Card Name");
        model.addColumn("Supplier");
        model.addColumn("Quantity");
        model.addColumn("Sale Date");
        model.addColumn("Sale Price");
        model.addColumn("Total");  // Add total column
        // Create summary panel
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryPanel.setBackground(BACKGROUND_COLOR);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel totalLabel = new JLabel("Total Sales: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setForeground(PRIMARY_COLOR);
        summaryPanel.add(totalLabel);

        // Load suppliers into filter
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM Suppliers")) {
            while (rs.next()) {
                supplierFilter.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error loading suppliers: " + e.getMessage());
        }

        // Add action listener for filter
        applyFilter.addActionListener(e -> {
        	model.setRowCount(0); // Clear existing data
            double totalSales = 0.0; // Track total sales
            
            StringBuilder query = new StringBuilder(
                "SELECT s.sale_id, c.name as card_name, sup.name as supplier_name, " +
                "s.quantity, s.sale_date, s.sale_price, " +
                "(s.quantity * s.sale_price) as total_price " +  // Changed 'total' to 'total_price'
                "FROM Sales s " +
                "JOIN Cards c ON s.card_id = c.card_id " +
                "JOIN Suppliers sup ON s.supplier_id = sup.supplier_id " +
                "WHERE 1=1"
            );

            ArrayList<Object> params = new ArrayList<>();

            if (!supplierFilter.getSelectedItem().equals("All Suppliers")) {
                query.append(" AND sup.name = ?");
                params.add(supplierFilter.getSelectedItem());
            }

            if (!dateFromField.getText().isEmpty()) {
                query.append(" AND s.sale_date >= ?");
                params.add(dateFromField.getText());
            }

            if (!dateToField.getText().isEmpty()) {
                query.append(" AND s.sale_date <= ?");
                params.add(dateToField.getText());
            }

            query.append(" ORDER BY s.sale_date DESC");

            try (Connection conn = connect();
                    PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
                   
                   for (int i = 0; i < params.size(); i++) {
                       pstmt.setObject(i + 1, params.get(i));
                   }

                   ResultSet rs = pstmt.executeQuery();
                   while (rs.next()) {
                       double total = rs.getDouble("total_price");  // Changed to match column name
                       totalSales += total;
                       
                       model.addRow(new Object[]{
                           rs.getInt("sale_id"),
                           rs.getString("card_name"),
                           rs.getString("supplier_name"),
                           rs.getInt("quantity"),
                           rs.getString("sale_date"),
                           String.format("$%.2f", rs.getDouble("sale_price")),
                           String.format("$%.2f", total)
                       });
                   }
                totalLabel.setText(String.format("Total Sales: $%.2f", totalSales));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error loading sales data: " + ex.getMessage());
            }
        });

        // Add components to frame
        reportFrame.add(filterPanel, BorderLayout.NORTH);
        reportFrame.add(new JScrollPane(table), BorderLayout.CENTER);
        reportFrame.add(summaryPanel, BorderLayout.SOUTH);  // Add summary panel
        // Show frame
        reportFrame.setVisible(true);
    }

  
    private static void deleteSale(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a sale to delete.");
            return;
        }

        Object saleId = model.getValueAt(selectedRow, 0);
        int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this sale?",
                "Delete Confirmation", JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            PreparedStatement deleteStmt = null;
            ResultSet rs = null;
            
            try {
                conn = connect();
                
                // Enable foreign keys and set timeout
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                    stmt.execute("PRAGMA busy_timeout = 5000");
                }
                
                conn.setAutoCommit(false);

                // Get card info first
                String getCardInfoSQL = "SELECT card_id, quantity FROM Sales WHERE sale_id = ?";
                pstmt = conn.prepareStatement(getCardInfoSQL);
                pstmt.setInt(1, Integer.parseInt(saleId.toString()));
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    int cardId = rs.getInt("card_id");
                    int quantity = rs.getInt("quantity");

                    // Update card quantity
                    String updateQuantitySQL = "UPDATE Cards SET quantity = quantity + ? WHERE card_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuantitySQL)) {
                        updateStmt.setInt(1, quantity);
                        updateStmt.setInt(2, cardId);
                        updateStmt.executeUpdate();
                    }

                    // Delete the sale
                    String deleteSaleSQL = "DELETE FROM Sales WHERE sale_id = ?";
                    deleteStmt = conn.prepareStatement(deleteSaleSQL);
                    deleteStmt.setInt(1, Integer.parseInt(saleId.toString()));
                    deleteStmt.executeUpdate();

                    conn.commit();
                    model.removeRow(selectedRow);
                    JOptionPane.showMessageDialog(null, "Sale Deleted Successfully!");
                }
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                JOptionPane.showMessageDialog(null, "Error Deleting Sale: " + e.getMessage());
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    if (deleteStmt != null) deleteStmt.close();
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void configureDatabaseConnection(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA journal_mode = WAL");
        }
    }
}
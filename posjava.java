import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.print.PrinterJob;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import javafx.application.Platform;

import javafx.geometry.Pos;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.*;
import java.io.*;
import java.util.Iterator;

public class posjava extends Application {

    private ListView<Product> productList;
    private ListView<String> cartList;
    private Label totalLabel;
    private TextField searchTextField;
    private TextField productNameField;
    private TextField productPriceField;

    private ObservableList<Product> allProducts;
    private String dbUrl = "jdbc:sqlite:C:\\Users\\patel\\OneDrive\\Documents\\work\\Java_learning\\Estimateapp_db\\productsdb.db";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Retail POS System");

        // Initialize product data from the database
        initializeProductData(dbUrl);

        productList = new ListView<>(FXCollections.observableArrayList(allProducts));

        // Shopping Cart
        cartList = new ListView<>();

        // Total Label
        totalLabel = new Label("Total: Rs 0.00");

        // Search TextField
        searchTextField = new TextField();
        searchTextField.setPromptText("Search Product");
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> searchProduct(newValue));

        // Product Name and Price TextFields
        productNameField = new TextField();
        productNameField.setPromptText("Product Name");

        productPriceField = new TextField();
        productPriceField.setPromptText("Product Price");

        // Quantity Spinner
        Spinner<Integer> quantitySpinner = new Spinner<>(1, Integer.MAX_VALUE, 1);
        quantitySpinner.setEditable(true); // Allow manual input

        // Buttons
        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.setOnAction(e -> addToCart(quantitySpinner.getValue())); // Pass quantity to addToCart method

        Button removeButton = new Button("Remove from Cart");
        removeButton.setOnAction(e -> removeFromCart());

        Button addNewProductButton = new Button("Add New Product");
        addNewProductButton.setOnAction(e -> openNewProductWindow());

        Button updateProductButton = new Button("Update Product");
        updateProductButton.setOnAction(e -> openUpdateProductWindow());

        Button checkoutButton = new Button("Checkout");
        checkoutButton.setOnAction(e -> checkout());

        // Layout
        BorderPane borderPane = new BorderPane();
        HBox topBox = new HBox(searchTextField);
        searchTextField.setPrefWidth(400);

        HBox leftButtons = new HBox(40, addNewProductButton, updateProductButton);
        HBox.setMargin(leftButtons, new Insets(20, 10, 15, 80));
        HBox rightButtons = new HBox(40, totalLabel, checkoutButton);
        HBox.setMargin(rightButtons, new Insets(20, 80, 15, 10));
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomBox = new HBox(leftButtons, rightButtons);
        HBox.setHgrow(leftButtons, javafx.scene.layout.Priority.ALWAYS);
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        // VBox for the left side with productList and searchTextField
        VBox leftVBox = new VBox(topBox, productList);
        VBox centerVBox = new VBox(60, quantitySpinner, addToCartButton, removeButton);
        centerVBox.setAlignment(Pos.CENTER);
        leftVBox.setSpacing(10);

        borderPane.setLeft(leftVBox);
        borderPane.setCenter(centerVBox);
        borderPane.setRight(cartList);
        borderPane.setBottom(bottomBox);

        cartList.setPrefWidth(650);
        leftVBox.setPrefWidth(400);
        centerVBox.setPrefWidth(150);
        primaryStage.setScene(new Scene(borderPane, 1200, 600));
        primaryStage.show();
    }
    
    // Define the method to update product price in the database
    private void updateProductPriceInDatabase(String productName, double newPrice) throws SQLException {
        // Define your database URL
        String dbUrl = "jdbc:sqlite:C:\\Users\\patel\\OneDrive\\Documents\\work\\Java_learning\\Estimateapp_db\\productsdb.db";

        // Define your SQL update statement
        String updateQuery = "UPDATE products_3 SET price = ? WHERE item_name = ?";

        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {

            // Set parameters for the PreparedStatement
            preparedStatement.setDouble(1, newPrice);
            preparedStatement.setString(2, productName);

            // Execute the update statement
            preparedStatement.executeUpdate();

            System.out.println("Product price updated in the database: " + productName + ", New Price: " + newPrice);
        }
    }
    
    private void initializeProductData(String dbUrl) {
        allProducts = FXCollections.observableArrayList();

        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement()) {

            // Create products table if not exists
            statement.execute("CREATE TABLE IF NOT EXISTS products_3 (item_name TEXT, price REAL)");

            ResultSet resultSet = statement.executeQuery("SELECT item_name, price FROM products_3");

            while (resultSet.next()) {
                String productName = resultSet.getString("item_name").trim();
                double productPrice = resultSet.getDouble("price");

                System.out.println("Product Name: " + productName + ", Price: " + productPrice);
                allProducts.add(new Product(productName, productPrice));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle database exception
        }
    }


    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equals(headers[i].trim())) {
                return i;
            }
        }
        return -1; // Column not found
    }

    private void searchProduct(String keyword) {
        ObservableList<Product> filteredProducts = allProducts.filtered(product ->
                product.getName().toLowerCase().contains(keyword.toLowerCase()));
        productList.setItems(filteredProducts);
    }

    private void addToCart(int quantity) {
        Product selectedProduct = productList.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // Extract product name and price
            String productName = selectedProduct.getName();
            double productPrice = selectedProduct.getPrice();

            // Calculate total for the item based on quantity and price
            double totalForItem = quantity * productPrice;

            // Calculate the maximum product name length
            int maxProductNameLength = allProducts.stream()
                    .mapToInt(p -> p.getName().length())
                    .max()
                    .orElse(0);

            // Calculate available space for price*quantity
            int availableSpace = 50;  // Adjust this value based on your requirement
            int spaceForPriceQuantity = Math.max(availableSpace - maxProductNameLength, 0);

            // Format the strings to ensure equal space between product name and price
            String formattedProductName = String.format("%-" + (maxProductNameLength + spaceForPriceQuantity + 10) + "s", productName);
            String formattedPrice = String.format("%-" + spaceForPriceQuantity + "s", "Rs " + String.format("%.2f", productPrice));
            String formattedQuantity = String.format("%1d", quantity);
            String formattedTotal = String.format("%12s", "Rs " + String.format("%.2f", totalForItem));
            String formattedProduct = formattedProductName + formattedPrice + " x " + formattedQuantity + " = " + formattedTotal;

            // Add the formatted item to the cartList
            cartList.getItems().add(formattedProduct);

            updateTotal();
        }
    }


    private class Product {
        private String name;
        private double price;
        
        
        public void setPrice(double price) {
            this.price = price;
        }

        public Product(String name, double price) {
            this.name = name;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return name + " - Rs " + String.format("%.2f", price);
        }
    }

    private void removeFromCart() {
        String selectedItem = cartList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            cartList.getItems().remove(selectedItem);
            updateTotal();
        }
    }

    private void updateTotal() {
        double total = cartList.getItems().stream()
                .mapToDouble(this::getTotalForCartItem)
                .sum();
        totalLabel.setText("Total: Rs " + String.format("%.2f", total));
    }

    // Helper method to extract total from cart item string
    private double getTotalForCartItem(String cartItem) {
        // Extract the total part from the cart item string
        String totalPart = cartItem.substring(cartItem.lastIndexOf('=') + 1).trim();
        return Double.parseDouble(totalPart.replace("Rs ", "").trim());
    }

    private void checkout() {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Format the date
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = currentDate.format(dateFormatter);

        StringBuilder receiptText = new StringBuilder();
        double total = 0.0; // Initialize total

        // Calculate the total separately
        total = cartList.getItems().stream()
                .mapToDouble(this::getTotalForCartItem)
                .sum();

        int itemNumber = 1; // Number for the first item

        for (String item : cartList.getItems()) {
            receiptText.append(itemNumber).append(". ").append(item).append("\n");
            itemNumber++;
        }

        // Append total at the bottom right corner
        receiptText.append("\n\nTotal: Rs ").append(String.format("%.2f", total)).append("\n");

        // Add more empty lines for additional space
        receiptText.append("\n\n\n\n\n\n\n\n\n\n");

        receiptText.insert(0, "Manish Electronics Estimate\nDate: " + formattedDate + "\n\n");

        TextArea receiptTextArea = new TextArea(receiptText.toString());
        receiptTextArea.setEditable(false);
        receiptTextArea.setWrapText(true);
        receiptTextArea.setMaxWidth(600);
        receiptTextArea.setMaxHeight(300);

        printReceipt(receiptTextArea.getText()); // Call printReceipt method here

        cartList.getItems().clear();
        updateTotal();
    }


    private void printReceipt(String receiptText) {
        // Use MediaSizeName to specify the paper size
        MediaSizeName a8MediaSize = MediaSize.findMedia(74, 52, MediaSize.MM);

        // Specify the printer name ("everycom" in this case)
        String printerName = "everycom";

        PrinterUtils.print(receiptText, a8MediaSize, printerName);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openNewProductWindow() {
        Stage newProductStage = new Stage();
        newProductStage.setTitle("Add New Product");

        Label nameLabel = new Label("Product Name:");
        TextField newNameField = new TextField();

        Label priceLabel = new Label("Product Price:");
        TextField newPriceField = new TextField();

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> {
            String name = newNameField.getText().trim();
            String priceText = newPriceField.getText().trim();

            if (!name.isEmpty() && !priceText.isEmpty()) {
                try {
                    double price = Double.parseDouble(priceText);

                    // Perform database insertion in a background thread
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            // Create a new product instance
                            Product newProduct = new Product(name, price);

                            // Update the GUI on the JavaFX Application Thread
                            Platform.runLater(() -> {
                                allProducts.add(newProduct);
                                showAlert("Success", "Product added: " + newProduct.getName());
                                newProductStage.close();
                            });

                            // Save the new product to the database
                            saveProductDataToDatabase(dbUrl);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showAlert("Error", "An error occurred while adding the product to the database.");
                        }
                    });

                    // Shutdown the executor after completion
                    executor.shutdown();

                } catch (NumberFormatException ex) {
                    showAlert("Error", "Please enter a valid numeric price.");
                }
            } else {
                showAlert("Error", "Please enter product name and price.");
            }
        });

        VBox vbox = new VBox(10, nameLabel, newNameField, priceLabel, newPriceField, addButton);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox, 300, 200);
        newProductStage.setScene(scene);
        newProductStage.show();
    }




    private void openUpdateProductWindow() {
        Product selectedProduct = productList.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            Stage updateProductStage = new Stage();
            updateProductStage.setTitle("Update Product");

            Label selectedProductLabel = new Label("Selected Product: " + selectedProduct.getName());

            Label priceLabel = new Label("New Product Price:");
            TextField newPriceField = new TextField();

            Button updateButton = new Button("Update");
            updateButton.setOnAction(e -> {
                String updatedPrice = newPriceField.getCharacters().toString().trim();

                if (!updatedPrice.isEmpty()) {
                    try {
                        double newPrice = Double.parseDouble(updatedPrice);

                        // Perform database update in a background thread
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                // Update the product price in the database
                                updateProductPriceInDatabase(selectedProduct.getName(), newPrice);

                                // Update the product price locally
                                selectedProduct.setPrice(newPrice);

                                // Update the GUI on the JavaFX Application Thread
                                Platform.runLater(() -> {
                                    int selectedIndex = productList.getSelectionModel().getSelectedIndex();
                                    ObservableList<Product> updatedProducts = FXCollections.observableArrayList(productList.getItems());
                                    updatedProducts.set(selectedIndex, selectedProduct);
                                    productList.setItems(updatedProducts);


                                    showAlert("Success", "Product updated: " + selectedProduct.getName() +
                                            " with price: Rs " + updatedPrice);
                                    updateProductStage.close();
                                });
                            } catch (SQLException ex) {
                                // Handle database update error
                                ex.printStackTrace();
                                showAlert("Error", "An error occurred while updating the product price in the database.");
                            }
                        });

                        // Shutdown the executor after completion
                        executor.shutdown();

                    } catch (NumberFormatException ex) {
                        showAlert("Error", "Please enter a valid numeric price.");
                    }
                } else {
                    showAlert("Error", "Please enter the new price.");
                }
            });

            VBox vbox = new VBox(10, selectedProductLabel, priceLabel, newPriceField, updateButton);
            vbox.setPadding(new Insets(10));

            Scene scene = new Scene(vbox, 300, 200);
            updateProductStage.setScene(scene);
            updateProductStage.show();
        } else {
            showAlert("Error", "Please select a product to update.");
        }
    }



    private void saveProductDataToDatabase(String dbUrl) {
        try (Connection connection = DriverManager.getConnection(dbUrl);
             Statement statement = connection.createStatement()) {

            // Create products table if not exists
            statement.execute("CREATE TABLE IF NOT EXISTS products_3 (item_name TEXT, price REAL)");

            // Clear existing data
            statement.execute("DELETE FROM products_3");

            // Insert product data using PreparedStatement
            String insertQuery = "INSERT INTO products_3 (item_name, price) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                for (Product product : allProducts) {
                    String productName = product.getName();
                    double productPrice = product.getPrice();

                    preparedStatement.setString(1, productName);
                    preparedStatement.setDouble(2, productPrice);
                    preparedStatement.executeUpdate();
                }
            }

            System.out.println("Product data saved to database: " + dbUrl);
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle database exception
            System.err.println("Error details: " + e.getMessage());
        }
    }



}

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

import javafx.application.Platform;


import java.io.*;
import java.util.Iterator;


import javafx.print.PrinterJob;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import java.io.ByteArrayInputStream;
import java.io.InputStream;




public class posjava extends Application {

    private ListView<String> productList;
    private ListView<String> cartList;
    private Label totalLabel;
    private TextField searchTextField;
    private TextField productNameField;
    private TextField productPriceField;

    private ObservableList<String> allProducts;
    private String filePath = "D:\\java software\\TEST1\\gst_item_name_dub.csv";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Retail POS System");

        // Initialize product data from the "gst_item_name.csv" file
        initializeProductData(filePath);

        productList = new ListView<>(allProducts);

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
        HBox topBox = new HBox(searchTextField, quantitySpinner);
        HBox bottomBox = new HBox(10, addToCartButton, removeButton, addNewProductButton, updateProductButton, checkoutButton, totalLabel);
        bottomBox.setPadding(new Insets(10));

        // VBox for the left side with productList and searchTextField
        VBox leftVBox = new VBox(productList, topBox);
        leftVBox.setSpacing(10);

        borderPane.setLeft(leftVBox);
        borderPane.setCenter(cartList);
        borderPane.setBottom(bottomBox);

        primaryStage.setScene(new Scene(borderPane, 900, 600));
        primaryStage.show();
    }

    private void initializeProductData(String filePath) {
        allProducts = FXCollections.observableArrayList();

        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            // Assuming the first line contains column headers
            String[] headers = br.readLine().split(",");
            int itemNameColumnIndex = findColumnIndex(headers, "item_name");
            int priceColumnIndex = findColumnIndex(headers, "price");

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (itemNameColumnIndex >= 0 && itemNameColumnIndex < values.length) {
                    String productName = values[itemNameColumnIndex].trim();
                    // Assuming the second column is for prices
                    String productPrice = (priceColumnIndex >= 0 && priceColumnIndex < values.length) ? values[priceColumnIndex].trim() : "0.00";
                    // Print the product name and price for debugging
                    System.out.println("Product Name: " + productName + ", Price: " + productPrice);
                    allProducts.add(productName + " - Rs " + productPrice);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle file reading exception
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
        ObservableList<String> filteredProducts = allProducts.filtered(product -> product.toLowerCase().contains(keyword.toLowerCase()));
        productList.setItems(filteredProducts);
    }

    private void addToCart(int quantity) {
        String selectedProduct = productList.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // Extract product name and price
            String productName = selectedProduct.split(" - ")[0];
            double productPrice = Double.parseDouble(selectedProduct.split(" - ")[1].replace("Rs ", "").trim());

            // Calculate total for the item based on quantity and price
            double totalForItem = quantity * productPrice;

            // Calculate the maximum product name length
            int maxProductNameLength = allProducts.stream().mapToInt(p -> p.split(" - ")[0].length()).max().orElse(0);

            // Calculate available space for price*quantity
            int availableSpace = 50;  // Adjust this value based on your requirement
            int spaceForPriceQuantity = Math.max(availableSpace - maxProductNameLength, 0);

            // Format the strings to ensure equal space between product name and price
            String formattedProductName = String.format("%-" + (maxProductNameLength +spaceForPriceQuantity + 10) + "s", productName);
            String formattedPrice = String.format("%-" + spaceForPriceQuantity + "s", "Rs " + String.format("%.2f", productPrice));
            String formattedQuantity = String.format("%1d", quantity);
            String formattedTotal = String.format("%12s", "Rs " + String.format("%.2f", totalForItem));
            String formattedProduct = formattedProductName + formattedPrice + " x " + formattedQuantity + " = " + formattedTotal;

            // Add the formatted item to the cartList
            cartList.getItems().add(formattedProduct);

            updateTotal();
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

        printReceipt(receiptTextArea.getText());

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
            String price = newPriceField.getText().trim();

            if (!name.isEmpty() && !price.isEmpty()) {
                allProducts.add(name + " - Rs " + price);
                showAlert("Success", "Product added: " + name);
                newProductStage.close();
                saveProductDataToFile(filePath); // Save the new product to the file
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
        String selectedProduct = productList.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            Stage updateProductStage = new Stage();
            updateProductStage.setTitle("Update Product");

            Label selectedProductLabel = new Label("Selected Product: " + selectedProduct);

            Label priceLabel = new Label("New Product Price:");
            TextField newPriceField = new TextField();

            Button updateButton = new Button("Update");
            updateButton.setOnAction(e -> {
                String updatedPrice = newPriceField.getCharacters().toString().trim();

                if (!updatedPrice.isEmpty()) {
                    int selectedIndex = productList.getSelectionModel().getSelectedIndex();
                    String productName = selectedProduct.split(" - ")[0];
                    String updatedProduct = productName + " - Rs " + updatedPrice;

                    // Update the existing product in the allProducts list
                    for (int i = 0; i < allProducts.size(); i++) {
                        if (allProducts.get(i).startsWith(productName)) {
                            allProducts.set(i, updatedProduct);
                            break;
                        }
                    }

                    // Update the GUI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        productList.setItems(FXCollections.observableArrayList(allProducts));
                        productList.getSelectionModel().select(selectedIndex);
                    });

                    showAlert("Success", "Product updated: " + selectedProduct + " with price: Rs " + updatedPrice);
                    updateProductStage.close();

                    saveProductDataToFile(filePath); // Save the updated product to the file
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



    private void saveProductDataToFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)))) {
            // Write headers
            writer.write("item_name,price\n");

            // Write product data
            for (String product : allProducts) {
                String productName = product.split(" - ")[0];
                String productPrice = product.split(" - ")[1].replace("Rs ", "");

                writer.write(productName + "," + String.format("%.2f", Double.parseDouble(productPrice)) + "\n");
            }

            System.out.println("Product data saved to file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

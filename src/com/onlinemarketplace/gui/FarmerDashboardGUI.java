package com.onlinemarketplace.gui;

import com.onlinemarketplace.exception.HarvestDateException;
import com.onlinemarketplace.exception.ValidationException;
import com.onlinemarketplace.model.Farmer;
import com.onlinemarketplace.model.Order;
import com.onlinemarketplace.model.OrderItem;
import com.onlinemarketplace.model.Product;
import com.onlinemarketplace.service.DeliverySystem;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI component for the Farmer Dashboard.
 * Allows farmers to manage their products (add/update/delete harvest schedules and quantities)
 * and view orders placed by customers for their products.
 * Simulates photo uploads.
 */
public class FarmerDashboardGUI {

    private static final String NAV_BAR_STYLE = "-fx-background-color: #2e8b57;"; // Darker green for Farmer nav bar

    private final DeliverySystem deliverySystem;
    private final Farmer currentFarmer; // The logged-in farmer
    private final LogoutCallback logoutAction;
    private BorderPane mainLayout; // Changed to BorderPane for navigation structure
    private ScrollPane contentArea; // ScrollPane for dynamic content

    // Product Management Form fields
    private TextField productIdField;
    private TextField productNameField;
    private TextField productDescriptionField;
    private TextField productPriceField;
    private TextField productQuantityField;
    private DatePicker harvestDateField;
    private Label productMessageLabel;
    private ListView<Product> farmerProductsList;

    // Order View
    private TableView<Order> farmerOrdersTable;
    private Label orderMessageLabel;


    /**
     * Constructs a FarmerDashboardGUI.
     *
     * @param deliverySystem The core delivery system instance.
     * @param currentFarmer The Farmer object currently logged in.
     * @param logoutAction A LogoutCallback to execute when the user logs out.
     */
    public FarmerDashboardGUI(DeliverySystem deliverySystem, Farmer currentFarmer, LogoutCallback logoutAction) {
        this.deliverySystem = deliverySystem;
        this.currentFarmer = currentFarmer;
        this.logoutAction = logoutAction;
        initializeGUI();
        // Initial population/refresh will happen when sections are shown
    }

    /**
     * Initializes the main GUI components and layout for the farmer dashboard,
     * including navigation bar and content area.
     */
    private void initializeGUI() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10)); // Padding around the entire layout

        // Create Navigation Bar
        HBox navBar = new HBox(20);
        navBar.setPadding(new Insets(10));
        navBar.setStyle(NAV_BAR_STYLE);
        navBar.setAlignment(Pos.CENTER_LEFT); // Align to left for welcome message

        Label welcomeLabel = new Label("Welcome, " + currentFarmer.getName());
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"); // White text for visibility
        HBox.setHgrow(welcomeLabel, Priority.ALWAYS); // Push buttons to the right

        Button productManagementButton = createNavButton("Product Management", e -> showSection(createProductManagementSection()));
        Button customerOrdersButton = createNavButton("Customer Orders", e -> showSection(createCustomerOrdersSection()));
        Button logoutButton = createNavButton("Logout", e -> logoutAction.onLogout());

        navBar.getChildren().addAll(welcomeLabel, productManagementButton, customerOrdersButton, logoutButton);
        mainLayout.setTop(navBar);

        // Content Area
        contentArea = new ScrollPane();
        contentArea.setFitToWidth(true); // Ensure content fits the width of the scroll pane
        contentArea.setStyle("-fx-background-color: #f0f8ff;"); // Light background for content area
        mainLayout.setCenter(contentArea);

        // Set default section to show on startup
        showSection(createProductManagementSection());
    }

    /**
     * Creates a styled navigation button.
     */
    private Button createNavButton(String text, EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #FF6347; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 15; -fx-background-radius: 5;");
        button.setOnAction(action);
        return button;
    }

    /**
     * Dynamically sets the displayed section in the content area.
     *
     * @param section The VBox content to display.
     */
    private void showSection(VBox section) {
        contentArea.setContent(section);
        // Refresh specific section content when it's displayed
        if (section == productManagementSection) { // Assuming productManagementSection VBox is referenced
            populateFarmerProductsList();
        } else if (section == customerOrdersSection) { // Assuming customerOrdersSection VBox is referenced
            refreshFarmerOrdersTable();
        }
    }


    // --- Product Management Section ---
    private VBox productManagementSection; // Reference to the VBox for showSection

    /**
     * Creates the Product Management section content.
     */
    private VBox createProductManagementSection() {
        productManagementSection = new VBox(15);
        productManagementSection.setPadding(new Insets(20));
        productManagementSection.setStyle("-fx-background-color: white; -fx-border-color: #c0e0c0; -fx-border-width: 1px; -fx-border-radius: 5px;");
        VBox.setVgrow(productManagementSection, Priority.ALWAYS);

        Label productManagementHeader = new Label("Product Management");
        productManagementHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));

        int row = 0;
        formGrid.add(new Label("Product ID:"), 0, row);
        productIdField = new TextField();
        productIdField.setPromptText("Unique ID (e.g., P001)");
        formGrid.add(productIdField, 1, row++);

        formGrid.add(new Label("Product Name:"), 0, row);
        productNameField = new TextField();
        productNameField.setPromptText("e.g., Organic Carrots");
        formGrid.add(productNameField, 1, row++);

        formGrid.add(new Label("Description:"), 0, row);
        productDescriptionField = new TextField();
        productDescriptionField.setPromptText("e.g., Organic, Seasonal, Vegetable");
        formGrid.add(productDescriptionField, 1, row++);

        formGrid.add(new Label("Price ($):"), 0, row);
        productPriceField = new TextField();
        productPriceField.setPromptText("e.g., 2.50");
        formGrid.add(productPriceField, 1, row++);

        formGrid.add(new Label("Quantity Available:"), 0, row);
        productQuantityField = new TextField();
        productQuantityField.setPromptText("e.g., 100");
        formGrid.add(productQuantityField, 1, row++);

        formGrid.add(new Label("Harvest Date:"), 0, row);
        harvestDateField = new DatePicker(LocalDate.now());
        formGrid.add(harvestDateField, 1, row++);

        HBox productButtons = new HBox(10);
        productButtons.setAlignment(Pos.CENTER_RIGHT);
        Button addProductButton = new Button("Add/Update Product");
        addProductButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        addProductButton.setOnAction(e -> handleAddOrUpdateProduct());

        Button deleteProductButton = new Button("Delete Selected Product");
        deleteProductButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        deleteProductButton.setOnAction(e -> handleDeleteProduct());


        productButtons.getChildren().addAll(addProductButton, deleteProductButton);

        productMessageLabel = new Label("");
        productMessageLabel.setWrapText(true);
        productMessageLabel.setStyle("-fx-font-weight: bold;");

        Label currentProductsLabel = new Label("Your Current Product Listings:");
        currentProductsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        farmerProductsList = new ListView<>();
        farmerProductsList.setPrefHeight(150);
        VBox.setVgrow(farmerProductsList, Priority.ALWAYS);

        farmerProductsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFormWithProduct(newVal);
            } else {
                clearFormFields();
            }
        });

        productManagementSection.getChildren().addAll(productManagementHeader, formGrid, productButtons, productMessageLabel, currentProductsLabel, farmerProductsList);
        return productManagementSection;
    }

    // --- Customer Orders Section ---
    private VBox customerOrdersSection; // Reference to the VBox for showSection

    /**
     * Creates the Customer Orders section content.
     */
    private VBox createCustomerOrdersSection() {
        customerOrdersSection = new VBox(15);
        customerOrdersSection.setPadding(new Insets(20));
        customerOrdersSection.setStyle("-fx-background-color: white; -fx-border-color: #c0e0c0; -fx-border-width: 1px; -fx-border-radius: 5px;");
        VBox.setVgrow(customerOrdersSection, Priority.ALWAYS);

        Label customerOrdersHeader = new Label("Customer Orders for Your Products");
        customerOrdersHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        farmerOrdersTable = new TableView<>();
        farmerOrdersTable.setPlaceholder(new Label("No orders found for your products."));
        farmerOrdersTable.setPrefHeight(250); // Increased height
        VBox.setVgrow(farmerOrdersTable, Priority.ALWAYS);

        // Define columns for the orders table
        TableColumn<Order, String> orderIdCol = new TableColumn<>("Order ID");
        orderIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrderId()));
        orderIdCol.setPrefWidth(100);

        TableColumn<Order, String> customerIdCol = new TableColumn<>("Customer ID");
        customerIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCustomerId()));
        customerIdCol.setPrefWidth(100);

        TableColumn<Order, String> orderDateCol = new TableColumn<>("Order Date");
        orderDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrderDate().toString()));
        orderDateCol.setPrefWidth(120);

        TableColumn<Order, String> productsCol = new TableColumn<>("Products Ordered");
        productsCol.setCellValueFactory(cellData -> {
            StringBuilder sb = new StringBuilder();
            for (OrderItem item : cellData.getValue().getOrderedItems()) {
                Product p = deliverySystem.getProduct(item.getProductId());
                if (p != null && p.getFarmerId().equals(currentFarmer.getUserId())) {
                    sb.append(p.getName()).append(" (x").append(item.getQuantity()).append("), ");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 2);
            }
            return new SimpleStringProperty(sb.toString());
        });
        productsCol.setPrefWidth(250);

        TableColumn<Order, String> totalCol = new TableColumn<>("Total Amount");
        totalCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getTotalAmount())));
        totalCol.setPrefWidth(100);

        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        statusCol.setPrefWidth(100);

        farmerOrdersTable.getColumns().addAll(orderIdCol, customerIdCol, orderDateCol, productsCol, totalCol, statusCol);

        orderMessageLabel = new Label("");
        orderMessageLabel.setWrapText(true);
        orderMessageLabel.setStyle("-fx-font-weight: bold;");

        customerOrdersSection.getChildren().addAll(customerOrdersHeader, farmerOrdersTable, orderMessageLabel);
        return customerOrdersSection;
    }


    /**
     * Populates the ListView with the current farmer's available products.
     */
    private void populateFarmerProductsList() {
        farmerProductsList.getItems().clear();
        if (currentFarmer != null) {
            List<Product> productsFromSystem = deliverySystem.getAllProducts();
            List<Product> farmersProducts = new ArrayList<>();
            for (Product p : productsFromSystem) {
                if (p.getFarmerId().equals(currentFarmer.getUserId())) {
                    farmersProducts.add(p);
                }
            }
            farmerProductsList.getItems().addAll(farmersProducts);
        }
    }

    /**
     * Populates the product form fields with details from a selected product.
     *
     * @param product The product to display in the form.
     */
    private void populateFormWithProduct(Product product) {
        productIdField.setText(product.getProductId());
        productNameField.setText(product.getName());
        productDescriptionField.setText(product.getDescription());
        productPriceField.setText(String.valueOf(product.getPrice()));
        productQuantityField.setText(String.valueOf(product.getQuantityAvailable()));
        harvestDateField.setValue(LocalDate.now()); // Default to today's date
    }

    /**
     * Clears all input fields in the product form.
     */
    private void clearFormFields() {
        productIdField.clear();
        productNameField.clear();
        productDescriptionField.clear();
        productPriceField.clear();
        productQuantityField.clear();
        harvestDateField.setValue(LocalDate.now());
        farmerProductsList.getSelectionModel().clearSelection();
    }

    /**
     * Handles the action of adding or updating a product.
     * Validates input and updates the DeliverySystem.
     */
    private void handleAddOrUpdateProduct() {
        String productId = productIdField.getText().trim();
        String productName = productNameField.getText().trim();
        String productDescription = productDescriptionField.getText().trim();
        String priceText = productPriceField.getText().trim();
        String quantityText = productQuantityField.getText().trim();
        LocalDate harvestDate = harvestDateField.getValue();

        if (productId.isEmpty() || productName.isEmpty() || priceText.isEmpty() || quantityText.isEmpty() || harvestDate == null) {
            productMessageLabel.setText("Please fill in all product fields.");
            productMessageLabel.setTextFill(Color.RED);
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            int quantity = Integer.parseInt(quantityText);

            Product newProduct = new Product(productId, productName, productDescription, price, quantity, harvestDate, currentFarmer.getUserId());
            deliverySystem.addProduct(newProduct);
            productMessageLabel.setText("Product '" + productName + "' added/updated successfully!");
            productMessageLabel.setTextFill(Color.GREEN);
            populateFarmerProductsList();
            clearFormFields();
        } catch (NumberFormatException e) {
            productMessageLabel.setText("Invalid number format for Price or Quantity.");
            productMessageLabel.setTextFill(Color.RED);
        } catch (DateTimeParseException e) {
            productMessageLabel.setText("Invalid date format for Harvest Date.");
            productMessageLabel.setTextFill(Color.RED);
        } catch (ValidationException e) {
            productMessageLabel.setText("Error: " + e.getMessage());
            productMessageLabel.setTextFill(Color.RED);
        } catch (HarvestDateException e) {
            productMessageLabel.setText("Error: " + e.getMessage());
            productMessageLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Handles deleting a selected product.
     */
    private void handleDeleteProduct() {
        Product selectedProduct = farmerProductsList.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            productMessageLabel.setText("Please select a product to delete.");
            productMessageLabel.setTextFill(Color.RED);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Product: " + selectedProduct.getName());
        alert.setContentText("Are you sure you want to delete this product? This action cannot be undone.");

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.OK) {
            if (deliverySystem.removeProduct(selectedProduct.getProductId())) {
                productMessageLabel.setText("Product '" + selectedProduct.getName() + "' deleted successfully.");
                productMessageLabel.setTextFill(Color.GREEN);
                populateFarmerProductsList();
                clearFormFields();
            } else {
                productMessageLabel.setText("Failed to delete product '" + selectedProduct.getName() + "'.");
                productMessageLabel.setTextFill(Color.RED);
            }
        }
    }

    /**
     * Refreshes the table view with orders relevant to the current farmer.
     */
    private void refreshFarmerOrdersTable() {
        if (currentFarmer != null) {
            List<Order> orders = deliverySystem.getOrdersForFarmer(currentFarmer.getUserId());
            farmerOrdersTable.getItems().setAll(orders);
            if (orders.isEmpty()) {
                orderMessageLabel.setText("No orders found for your products.");
                orderMessageLabel.setTextFill(Color.BLACK);
            } else {
                orderMessageLabel.setText("");
            }
        }
    }

    /**
     * Returns the BorderPane containing the entire Farmer Dashboard GUI.
     *
     * @return The BorderPane view.
     */
    public BorderPane getView() {
        return mainLayout;
    }
}
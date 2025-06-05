package com.onlinemarketplace.gui;

import com.onlinemarketplace.exception.DeliveryUnavailableException;
import com.onlinemarketplace.exception.OutOfSeasonException;
import com.onlinemarketplace.exception.ValidationException;
import com.onlinemarketplace.model.Customer;
import com.onlinemarketplace.model.Farmer;
import com.onlinemarketplace.model.Order;
import com.onlinemarketplace.model.OrderItem;
import com.onlinemarketplace.model.Product;
import com.onlinemarketplace.model.ShoppingCartItem;
import com.onlinemarketplace.model.Subscription;
import com.onlinemarketplace.service.DeliverySystem;
import com.onlinemarketplace.service.IProductSearch;
import com.onlinemarketplace.service.ProductSearchEngine;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * GUI component for the Customer Portal with full functionality.
 * Includes product Browse, shopping cart, order history,
 * farmer subscriptions, and delivery tracking.
 */
public class CustomerPortalGUI {
    private static final String NAV_BAR_STYLE = "-fx-background-color: #4CAF50;";
    private static final double DEFAULT_PROXIMITY_RADIUS = 20.0;
    private static final String BACKGROUND_IMAGE_URL = "https://static.vecteezy.com/system/resources/previews/045/822/984/non_2x/happy-muslim-woman-in-hijab-standing-in-supermarket-holding-broccoli-and-shopping-basket-smiling-and-looking-at-camera-photo.jpg";

    private final DeliverySystem deliverySystem;
    private final ProductSearchEngine productSearchEngine;
    private final Customer currentCustomer;
    private final LogoutCallback logoutAction;
    private BorderPane mainLayout;
    private ScrollPane contentArea;

    // UI components for Product Browse
    private ListView<Product> productListView;
    private TextField searchField, proximityRadiusField, categoryField;
    private DatePicker seasonDatePicker;
    private Label productMessageLabel, cartTotalLabel;
    private ListView<String> shoppingCartView;

    // UI components for Order History
    private TableView<Order> orderHistoryTable; // Initialized early
    private Label orderHistoryMessageLabel; // Initialized early

    // UI components for Subscriptions
    private ListView<Farmer> farmerSubscriptionListView;
    private ListView<Subscription> currentSubscriptionsListView;
    private Label subscriptionMessageLabel;

    // UI components for Delivery Tracking (integrated from DeliveryTrackingGUI)
    private Pane mapPane;
    private Label activeOrdersLabel;
    private List<DeliveryStatus> simulatedDeliveries;
    private Random random;
    private boolean deliverySimulationInitialized = false; // Flag to ensure simulation starts only once


    // Define some arbitrary geographical bounds for mapping to screen pixels
    private static final double MAP_MIN_LAT = 24.0; //
    private static final double MAP_MAX_LAT = 25.5; //
    private static final double MAP_MIN_LON = 54.5; //
    private static final double MAP_MAX_LON = 55.5; //

    // Section VBoxes (initialized lazily)
    private VBox productsSectionVBox;
    private VBox cartSectionVBox;
    private VBox orderHistorySectionVBox;
    private VBox subscriptionSectionVBox;
    private VBox deliveryTrackingSectionVBox;


    /**
     * Represents the simulated status of a delivery.
     * (Copied as inner class from DeliveryTrackingGUI.java)
     */
    private static class DeliveryStatus {
        String orderId;
        String customerId;
        String customerLocation;
        String status;
        double vehicleX, vehicleY;
        double targetX, targetY;
        Circle vehicleIcon;
        Timeline deliveryTimeline;

        DeliveryStatus(String orderId, String customerId, String customerLocation, double startX, double startY, double targetX, double targetY) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.customerLocation = customerLocation;
            this.status = "Processing";
            this.vehicleX = startX;
            this.vehicleY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.vehicleIcon = new Circle(startX, startY, 8, Color.BLUE);
            this.vehicleIcon.setStroke(Color.DARKBLUE);
            this.vehicleIcon.setStrokeWidth(2);
        }

        public String getStatusText() {
            return String.format("Order ID: %s, Customer: %s, Status: %s",
                    orderId, customerLocation, status);
        }
    }


    /**
     * Constructs a CustomerPortalGUI.
     */
    public CustomerPortalGUI(DeliverySystem deliverySystem, Customer currentCustomer, LogoutCallback logoutAction) {
        this.deliverySystem = deliverySystem;
        this.productSearchEngine = (ProductSearchEngine) deliverySystem.getProductSearchEngine();
        this.currentCustomer = currentCustomer;
        this.logoutAction = logoutAction;
        this.simulatedDeliveries = new ArrayList<>();
        this.random = new Random();

        initializeGUI();
        // Initial population/refresh will happen when sections are shown via showSection
    }

    /**
     * Initializes the GUI layout and navigation bar.
     */
    private void initializeGUI() {
        mainLayout = new BorderPane();

        // Initialize mapPane and activeOrdersLabel here to prevent NullPointerException
        mapPane = new Pane();
        mapPane.setPrefSize(800, 500); // Give it initial dimensions
        mapPane.setStyle(
                "-fx-background-image: url('" + "https://c8.alamy.com/comp/EX6RBB/colour-satellite-image-of-abu-dhabi-united-arab-emirates-image-taken-EX6RBB.jpg" + "');" + //
                        "-fx-background-size: cover;" + //
                        "-fx-border-color: #333;" + //
                        "-fx-border-width: 2px;" + //
                        "-fx-border-radius: 5px;" //
        );
        activeOrdersLabel = new Label("Active Deliveries: \nNone");
        activeOrdersLabel.setWrapText(true); //
        activeOrdersLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;"); //

        // Initialize orderHistoryTable and orderHistoryMessageLabel early
        orderHistoryTable = new TableView<>();
        orderHistoryTable.setPlaceholder(new Label("No past orders found."));
        orderHistoryTable.setPrefHeight(200); // Set preferred height here
        // Define columns for the order history table here, as the TableView is created early
        TableColumn<Order, String> ohOrderIdCol = new TableColumn<>("Order ID");
        ohOrderIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrderId()));
        ohOrderIdCol.setPrefWidth(100);

        TableColumn<Order, String> ohOrderDateCol = new TableColumn<>("Order Date");
        ohOrderDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrderDate().toString()));
        ohOrderDateCol.setPrefWidth(120);

        TableColumn<Order, String> ohProductsCol = new TableColumn<>("Products");
        ohProductsCol.setCellValueFactory(cellData -> {
            StringBuilder sb = new StringBuilder();
            for (OrderItem item : cellData.getValue().getOrderedItems()) {
                Product p = deliverySystem.getProduct(item.getProductId());
                sb.append(p != null ? p.getName() : "Unknown Product").append(" (x").append(item.getQuantity()).append("), ");
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 2);
            }
            return new SimpleStringProperty(sb.toString());
        });
        ohProductsCol.setPrefWidth(250);

        TableColumn<Order, String> ohTotalCol = new TableColumn<>("Total");
        ohTotalCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getTotalAmount())));
        ohTotalCol.setPrefWidth(80);

        TableColumn<Order, String> ohStatusCol = new TableColumn<>("Status");
        ohStatusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        ohStatusCol.setPrefWidth(100);

        orderHistoryTable.getColumns().addAll(ohOrderIdCol, ohOrderDateCol, ohProductsCol, ohTotalCol, ohStatusCol);

        orderHistoryMessageLabel = new Label("");
        orderHistoryMessageLabel.setWrapText(true);
        orderHistoryMessageLabel.setStyle("-fx-font-weight: bold;");


        // Create Navigation Bar
        HBox navBar = new HBox(20);
        navBar.setPadding(new Insets(10));
        navBar.setStyle(NAV_BAR_STYLE);
        navBar.setAlignment(Pos.CENTER);

        // Welcome Label
        Label welcomeLabel = new Label("Welcome, " + currentCustomer.getName());
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox.setHgrow(welcomeLabel, Priority.ALWAYS);

        Button productButton = createNavButton("Products", e -> showSection("products"));
        Button cartButton = createNavButton("Shopping Cart", e -> showSection("cart"));
        Button orderHistoryButton = createNavButton("Order History", e -> showSection("orderHistory"));
        Button subscriptionsButton = createNavButton("Subscriptions", e -> showSection("subscriptions"));
        Button deliveryButton = createNavButton("Delivery Tracking", e -> showSection("deliveryTracking"));
        Button logoutButton = createNavButton("Logout", e -> logoutAction.onLogout());

        navBar.getChildren().addAll(welcomeLabel, productButton, cartButton, orderHistoryButton, subscriptionsButton, deliveryButton, logoutButton);
        mainLayout.setTop(navBar);

        // Content Area
        contentArea = new ScrollPane();
        contentArea.setFitToWidth(true);
        contentArea.setStyle("-fx-background-color: #f0f0f0;");
        mainLayout.setCenter(contentArea);

        // Set Default Section
        showSection("products"); // Initially show the products section
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
     * Uses lazy initialization for section VBoxes.
     * @param sectionName The string identifier for the section to display.
     */
    private void showSection(String sectionName) {
        VBox sectionToShow = null;

        switch (sectionName) {
            case "products":
                if (productsSectionVBox == null) {
                    productsSectionVBox = createProductSection();
                }
                sectionToShow = productsSectionVBox;
                refreshProductList(deliverySystem.getAllProducts()); // Refresh when shown
                break;
            case "cart":
                if (cartSectionVBox == null) {
                    cartSectionVBox = createCartSection();
                }
                sectionToShow = cartSectionVBox;
                refreshShoppingCart(); // Refresh when shown
                break;
            case "orderHistory":
                if (orderHistorySectionVBox == null) {
                    orderHistorySectionVBox = createOrderHistorySection();
                }
                sectionToShow = orderHistorySectionVBox;
                refreshOrderHistoryTable(); // Refresh when shown
                break;
            case "subscriptions":
                if (subscriptionSectionVBox == null) {
                    subscriptionSectionVBox = createSubscriptionSection();
                }
                sectionToShow = subscriptionSectionVBox;
                refreshFarmerSubscriptionList(); // Refresh when shown
                refreshCurrentSubscriptionsList(); // Refresh when shown
                break;
            case "deliveryTracking":
                if (deliveryTrackingSectionVBox == null) {
                    deliveryTrackingSectionVBox = createDeliveryTrackingSection();
                }
                sectionToShow = deliveryTrackingSectionVBox;
                // Start simulation only once when this section is created/shown for the first time
                if (!deliverySimulationInitialized) {
                    startSimulationUpdates();
                    deliverySimulationInitialized = true;
                }
                break;
            default:
                // Fallback to products section if unknown sectionName
                if (productsSectionVBox == null) {
                    productsSectionVBox = createProductSection();
                }
                sectionToShow = productsSectionVBox;
                refreshProductList(deliverySystem.getAllProducts());
                break;
        }
        contentArea.setContent(sectionToShow);
    }

    /**
     * Creates the Product Browse section.
     */
    private VBox createProductSection() {
        VBox productSection = new VBox(15);
        productSection.setPadding(new Insets(20));
        productSection.setStyle("-fx-background-image: url(" +
                BACKGROUND_IMAGE_URL+
                        ");-fx-background-size: cover;" +
              "-fx-background-position: center center;  -fx-border-color: #ddd; " +
                "-fx-border-width: 1px; -fx-border-radius: 5px;");

        Label header = new Label("Product Catalog");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        // Search and Filter controls
        VBox searchControlsVBox = new VBox(10);
        searchControlsVBox.setPadding(new Insets(0, 0, 15, 0));

        searchField = new TextField();
        searchField.setPromptText("Search by Name/Description");
        searchField.setStyle("-fx-border-color: #bbb; -fx-border-radius: 5px; -fx-padding: 8px;");
        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #6495ED; -fx-text-fill: white; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> refreshProductList(productSearchEngine.searchProducts(searchField.getText())));
        HBox searchLine1 = new HBox(10, new Label("Keyword:"), searchField, searchButton);
        searchLine1.setAlignment(Pos.CENTER_RIGHT);
        searchLine1.setStyle("-fx-background-color: rgba(173,133,133,0.51); -fx-padding: 10; -fx-border-radius: 5;");

        for(Node node : searchLine1.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #050000; -fx-font-weight: bold;");
            }
        }

        seasonDatePicker = new DatePicker(LocalDate.now());
        seasonDatePicker.setStyle("-fx-border-color: #bbb; -fx-border-radius: 5px; -fx-padding: 8px;");
        Button searchSeasonButton = new Button("Search by Season");
        searchSeasonButton.setStyle("-fx-background-color: #6495ED; -fx-text-fill: white; -fx-background-radius: 5;");
        searchSeasonButton.setOnAction(e -> refreshProductList(productSearchEngine.searchBySeason(seasonDatePicker.getValue())));
        HBox searchLine2 = new HBox(10, new Label("Season:"), seasonDatePicker, searchSeasonButton);
        searchLine2.setAlignment(Pos.CENTER_RIGHT);
        searchLine2.setStyle("-fx-background-color: rgba(173,133,133,0.51); -fx-padding: 10; -fx-border-radius: 5;");

        for(Node node : searchLine2.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");
            }
        }

        proximityRadiusField = new TextField(String.valueOf(DEFAULT_PROXIMITY_RADIUS));
        proximityRadiusField.setPromptText("Radius (km)");
        proximityRadiusField.setPrefWidth(80);
        proximityRadiusField.setStyle("-fx-border-color: #bbb; -fx-border-radius: 5px; -fx-padding: 8px;");
        Button searchProximityButton = new Button("Search by Proximity");
        searchProximityButton.setStyle("-fx-background-color: #6495ED; -fx-text-fill: white; -fx-background-radius: 5;");
        searchProximityButton.setOnAction(e -> {
            try {
                double radius = Double.parseDouble(proximityRadiusField.getText());
                refreshProductList(productSearchEngine.searchByProximity(currentCustomer.getLocation(), radius));
            } catch (NumberFormatException ex) {
                if (productMessageLabel != null) {
                    productMessageLabel.setTextFill(Color.RED);
                    productMessageLabel.setText("Invalid radius input. Please enter a number.");
                }
            }
        });
        HBox searchLine3 = new HBox(10, new Label("Proximity (km):"), proximityRadiusField, searchProximityButton);
        searchLine3.setAlignment(Pos.CENTER_RIGHT);
        searchLine3.setStyle("-fx-background-color: rgba(173,133,133,0.51); -fx-padding: 10; -fx-border-radius: 5;");

        for(Node node : searchLine3.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #070000; -fx-font-weight: bold;");
            }
        }

        categoryField = new TextField();
        categoryField.setPromptText("e.g., vegetable, organic");
        categoryField.setStyle("-fx-border-color: #bbb; -fx-border-radius: 5px; -fx-padding: 8px;");
        Button searchCategoryButton = new Button("Search by Category");
        searchCategoryButton.setStyle("-fx-background-color: #6495ED; -fx-text-fill: white; -fx-background-radius: 5;");
        searchCategoryButton.setOnAction(e -> refreshProductList(productSearchEngine.searchByCategory(categoryField.getText())));
        HBox searchLine4 = new HBox(10, new Label("Category:"), categoryField, searchCategoryButton);
        searchLine4.setAlignment(Pos.CENTER_RIGHT);
        searchLine4.setStyle("-fx-background-color: rgba(173,133,133,0.51); -fx-padding: 10; -fx-border-radius: 5;");

        for(Node node : searchLine4.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #110000; -fx-font-weight: bold;");
            }
        }

        searchControlsVBox.getChildren().addAll(searchLine1, searchLine2, searchLine3, searchLine4);

        Label availableProductsLabel = new Label("Available Products:");
        availableProductsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #ffffff;");
        productListView = new ListView<>();
        productListView.setPrefHeight(200);
        productListView.setStyle("-fx-border-color: #bbb; -fx-border-radius: 5px; -fx-padding: 8px; -fx-background-color: #f9f9f9;");
        VBox.setVgrow(productListView, Priority.ALWAYS);

        for (Node node : productListView.lookupAll(".list-cell")) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #333; -fx-font-weight: bold;");
            }
        }

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        addToCartButton.setOnAction(e -> handleAddToCart());

        productMessageLabel = new Label("");
        productMessageLabel.setWrapText(true);
        productMessageLabel.setStyle("-fx-font-weight: bold;");

        productSection.getChildren().addAll(header, searchControlsVBox, availableProductsLabel, productListView, addToCartButton, productMessageLabel);
        return productSection;
    }

    /**
     * Creates the Shopping Cart section.
     */
    private VBox createCartSection() {
        VBox cartSection = new VBox(15);
        cartSection.setPadding(new Insets(20));
        cartSection.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label header = new Label("Shopping Cart");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        shoppingCartView = new ListView<>();
        shoppingCartView.setPrefHeight(150);
        VBox.setVgrow(shoppingCartView, Priority.ALWAYS);

        cartTotalLabel = new Label("Total: $0.00");
        cartTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #333;");

        HBox cartButtons = new HBox(10);
        cartButtons.setAlignment(Pos.CENTER_RIGHT);
        Button removeFromCartButton = new Button("Remove Selected");
        removeFromCartButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-background-radius: 5;");
        removeFromCartButton.setOnAction(e -> handleRemoveFromCart());
        Button clearCartButton = new Button("Clear Cart");
        clearCartButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-background-radius: 5;");
        clearCartButton.setOnAction(e -> handleClearCart());
        cartButtons.getChildren().addAll(removeFromCartButton, clearCartButton);

        Button placeOrderButton = new Button("Place Order");
        placeOrderButton.setStyle("-fx-background-color: #008CBA; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 10 20; -fx-background-radius: 8;");
        placeOrderButton.setOnAction(e -> handlePlaceOrder());

        productMessageLabel = new Label(""); // Re-using productMessageLabel for cart messages
        productMessageLabel.setWrapText(true);
        productMessageLabel.setStyle("-fx-font-weight: bold;");

        cartSection.getChildren().addAll(header, shoppingCartView, cartTotalLabel, cartButtons, placeOrderButton, productMessageLabel);
        return cartSection;
    }

    /**
     * Creates the Order History section.
     * This method now just assembles the VBox using the already initialized orderHistoryTable.
     */
    private VBox createOrderHistorySection() {
        VBox historySection = new VBox(15);
        historySection.setPadding(new Insets(20));
        historySection.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label header = new Label("Order History");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox.setVgrow(orderHistoryTable, Priority.ALWAYS); // Ensure it grows correctly

        Button cancelOrderButton = new Button("Cancel Selected Order");
        cancelOrderButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        cancelOrderButton.setOnAction(e -> handleCancelOrder());

        // orderHistoryMessageLabel is now initialized in initializeGUI()

        historySection.getChildren().addAll(header, orderHistoryTable, cancelOrderButton, orderHistoryMessageLabel);
        return historySection;
    }

    /**
     * Creates the Subscriptions section.
     */
    private VBox createSubscriptionSection() {
        VBox subscriptionSection = new VBox(15);
        subscriptionSection.setPadding(new Insets(20));
        subscriptionSection.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label header = new Label("Farmer Subscriptions");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label subscribeHeader = new Label("Subscribe to a Farmer:");
        subscribeHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        farmerSubscriptionListView = new ListView<>();
        farmerSubscriptionListView.setPrefHeight(100);
        VBox.setVgrow(farmerSubscriptionListView, Priority.ALWAYS);

        Button subscribeButton = new Button("Subscribe to Selected Farmer");
        subscribeButton.setStyle("-fx-background-color: #8BC34A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        subscribeButton.setOnAction(e -> handleSubscribeToFarmer());

        Label currentSubscriptionsHeader = new Label("Your Current Subscriptions:");
        currentSubscriptionsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        currentSubscriptionsListView = new ListView<>();
        currentSubscriptionsListView.setPrefHeight(100);
        VBox.setVgrow(currentSubscriptionsListView, Priority.ALWAYS);

        subscriptionMessageLabel = new Label("");
        subscriptionMessageLabel.setWrapText(true);
        subscriptionMessageLabel.setStyle("-fx-font-weight: bold;");

        subscriptionSection.getChildren().addAll(header, subscribeHeader, farmerSubscriptionListView, subscribeButton,
                currentSubscriptionsHeader, currentSubscriptionsListView, subscriptionMessageLabel);
        return subscriptionSection;
    }

    /**
     * Creates the Delivery Tracking section.
     */
    private VBox createDeliveryTrackingSection() {
        VBox deliverySection = new VBox(15);
        deliverySection.setPadding(new Insets(20));
        deliverySection.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 5px;");

        Label header = new Label("Delivery Tracking Map");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // mapPane and activeOrdersLabel are now initialized in initializeGUI()
        // No need to re-initialize or set style here unless you want to override.

        deliverySection.getChildren().addAll(header, activeOrdersLabel, mapPane);

        // Start simulation only once when this section is created/shown for the first time
        if (!deliverySimulationInitialized) {
            startSimulationUpdates();
            deliverySimulationInitialized = true;
        }
        return deliverySection;
    }

    /**
     * Converts a latitude value to a Y-coordinate on the map pane.
     * @param lat Latitude to convert.
     * @return Y-coordinate.
     */
    private double mapLatToY(double lat) {
        // Invert Y-axis: higher latitude (north) should be lower Y-pixel value on screen.
        // Clamp lat to bounds to avoid out-of-range issues.
        double clampedLat = Math.max(MAP_MIN_LAT, Math.min(MAP_MAX_LAT, lat));
        return mapPane.getHeight() - ((clampedLat - MAP_MIN_LAT) / (MAP_MAX_LAT - MAP_MIN_LAT)) * mapPane.getHeight();
    }

    /**
     * Converts a longitude value to an X-coordinate on the map pane.
     * @param lon Longitude to convert.
     * @return X-coordinate.
     */
    private double mapLonToX(double lon) {
        // Clamp lon to bounds to avoid out-of-range issues.
        double clampedLon = Math.max(MAP_MIN_LON, Math.min(MAP_MAX_LON, lon));
        return ((clampedLon - MAP_MIN_LON) / (MAP_MAX_LON - MAP_MIN_LON)) * mapPane.getWidth();
    }

    /**
     * Starts a periodic update for the simulated delivery statuses and map.
     */
    private void startSimulationUpdates() {
        // This timeline checks for new orders and updates existing ones every few seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            updateSimulatedDeliveries();
            updateActiveOrdersLabel();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /**
     * Updates the state of simulated deliveries.
     * Now, it checks for orders with "Dispatched" status that are not yet simulated.
     */
    private void updateSimulatedDeliveries() {
        // Get all orders from DeliverySystem (getOrdersForCustomer(null) returns all)
        List<Order> allSystemOrders = deliverySystem.getOrdersForCustomer(null);
        List<Order> newlyDispatchedOrders = new ArrayList<>();

        for (Order order : allSystemOrders) {
            if ("Dispatched".equals(order.getStatus())) {
                boolean alreadySimulated = false;
                for (DeliveryStatus ds : simulatedDeliveries) {
                    if (ds.orderId.equals(order.getOrderId())) {
                        alreadySimulated = true;
                        break;
                    }
                }
                if (!alreadySimulated) {
                    newlyDispatchedOrders.add(order);
                }
            }
        }

        // Add newly dispatched orders to simulation
        for (Order order : newlyDispatchedOrders) {
            Customer customer = deliverySystem.getCustomer(order.getCustomerId());
            if (customer != null) {
                // Determine target coordinates from customer's location
                double targetLat = customer.getLatitude();
                double targetLon = customer.getLongitude();

                // Determine start coordinates (e.g., from a central depot or a random farmer's location)
                double startLat, startLon;
                List<Farmer> allFarmers = deliverySystem.getAllFarmers();
                if (allFarmers.size() > 0) {
                    // Pick a random farmer as the origin point
                    Farmer randomFarmer = allFarmers.get(random.nextInt(allFarmers.size()));
                    startLat = randomFarmer.getLatitude();
                    startLon = randomFarmer.getLongitude();
                } else {
                    // Fallback to random point within map bounds if no farmers
                    startLat = MAP_MIN_LAT + (MAP_MAX_LAT - MAP_MIN_LAT) * random.nextDouble();
                    startLon = MAP_MIN_LON + (MAP_MAX_LON - MAP_MIN_LON) * random.nextDouble();
                }

                // Map geo-coordinates to screen pixels
                double startX = mapLonToX(startLon);
                double startY = mapLatToY(startLat);
                double targetX = mapLonToX(targetLon);
                double targetY = mapLatToY(targetLat);

                DeliveryStatus newDelivery = new DeliveryStatus(order.getOrderId(), customer.getUserId(), customer.getLocation(), startX, startY, targetX, targetY);
                newDelivery.status = "In Transit"; // Immediately set to In Transit for simulation
                simulatedDeliveries.add(newDelivery);
                mapPane.getChildren().add(newDelivery.vehicleIcon);

                // --- ADD ALERT HERE for "In Transit" ---
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Delivery Status Update");
                alert.setHeaderText(null);
                alert.setContentText("Order ID: " + order.getOrderId() + " is now IN TRANSIT to " + customer.getName() + " at " + customer.getLocation());
                alert.show(); // Use show() to display and allow code to continue

                // Start animation for this delivery
                newDelivery.deliveryTimeline = new Timeline(new KeyFrame(Duration.millis(50), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                    @Override
                    public void handle(javafx.event.ActionEvent event) {
                        moveVehicle(newDelivery);
                    }
                }));
                newDelivery.deliveryTimeline.setCycleCount(Animation.INDEFINITE);
                newDelivery.deliveryTimeline.play();
            }
        }

        // Update existing deliveries and remove completed ones
        List<DeliveryStatus> deliveriesToRemove = new ArrayList<>();
        for (DeliveryStatus delivery : simulatedDeliveries) {
            Order actualOrder = deliverySystem.getOrder(delivery.orderId);

            // If actual order is cancelled, stop simulation and remove
            if (actualOrder != null && "Cancelled".equals(actualOrder.getStatus())) {
                if (delivery.deliveryTimeline != null) delivery.deliveryTimeline.stop();
                mapPane.getChildren().remove(delivery.vehicleIcon);

                // --- ADD ALERT HERE for "Cancelled" ---
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Delivery Status Update");
                alert.setHeaderText(null);
                alert.setContentText("Order ID: " + actualOrder.getOrderId() + " has been CANCELLED.");
                alert.show();

                deliveriesToRemove.add(delivery);
                continue;
            }

            // If simulated delivery reaches target
            if (Math.abs(delivery.vehicleX - delivery.targetX) < 5 && Math.abs(delivery.vehicleY - delivery.targetY) < 5) {
                delivery.status = "Delivered";
                if (actualOrder != null) {
                    actualOrder.setStatus("Delivered"); // Update actual order status in DeliverySystem
                }
                if (delivery.deliveryTimeline != null) {
                    delivery.deliveryTimeline.stop(); // Stop animation
                }
                mapPane.getChildren().remove(delivery.vehicleIcon); // Remove vehicle from map

                // --- ADD ALERT HERE for "Delivered" ---
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Delivery Status Update");
                alert.setHeaderText(null);
                alert.setContentText("Order ID: " + delivery.orderId + " has been DELIVERED to " + delivery.customerLocation + "!");
                alert.show();

                deliveriesToRemove.add(delivery);
                continue;
            }
            // If the actual order status changed to something other than Dispatched/Pending
            if (actualOrder != null && !"Dispatched".equals(actualOrder.getStatus()) && !"Pending".equals(actualOrder.getStatus()) && !"In Transit".equals(delivery.status)) {
                if (delivery.deliveryTimeline != null) delivery.deliveryTimeline.stop();
                mapPane.getChildren().remove(delivery.vehicleIcon);
                deliveriesToRemove.add(delivery);
            }
        }
        simulatedDeliveries.removeAll(deliveriesToRemove);
    }

    /**
     * Simulates the movement of a delivery vehicle towards its target.
     *
     * @param delivery The DeliveryStatus object representing the vehicle.
     */
    private void moveVehicle(DeliveryStatus delivery) {
        double speed = 2.0; // Pixels per update
        double dx = delivery.targetX - delivery.vehicleX;
        double dy = delivery.targetY - delivery.vehicleY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > speed) {
            delivery.vehicleX += speed * (dx / distance);
            delivery.vehicleY += speed * (dy / distance);
        } else {
            delivery.vehicleX = delivery.targetX;
            delivery.vehicleY = delivery.targetY;
        }
        delivery.vehicleIcon.setCenterX(delivery.vehicleX);
        delivery.vehicleIcon.setCenterY(delivery.vehicleY);
    }

    /**
     * Updates the label displaying active orders.
     */
    private void updateActiveOrdersLabel() {
        StringBuilder sb = new StringBuilder("Active Deliveries:\n");
        if (simulatedDeliveries.isEmpty()) {
            sb.append("None");
        } else {
            for (DeliveryStatus delivery : simulatedDeliveries) {
                sb.append("- ").append(delivery.getStatusText()).append("\n");
            }
        }
        activeOrdersLabel.setText(sb.toString());
    }

    /**
     * Refreshes the product list view with the given list of products.
     *
     * @param products The list of products to display.
     */
    private void refreshProductList(List<Product> products) {
        productListView.getItems().clear();
        for (Product product : products) {
            productListView.getItems().add(product);
        }
    }

    /**
     * Handles adding a selected product to the customer's shopping cart.
     */
    private void handleAddToCart() {
        Product selectedProduct = productListView.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            productMessageLabel.setText("Please select a product to add to cart.");
            productMessageLabel.setTextFill(Color.RED);
            return;
        }

        // Prompt for quantity
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Add to Cart");
        dialog.setHeaderText("Enter Quantity for " + selectedProduct.getName());
        dialog.setContentText("Quantity:");

        String qtyStr = dialog.showAndWait().orElse(null);

        if (qtyStr != null) {
            try {
                int quantity = Integer.parseInt(qtyStr);
                if (quantity <= 0) {
                    productMessageLabel.setText("Quantity must be positive.");
                    productMessageLabel.setTextFill(Color.RED);
                    return;
                }
                if (selectedProduct.getQuantityAvailable() < quantity) {
                    productMessageLabel.setText("Not enough stock for " + selectedProduct.getName() + ". Available: " + selectedProduct.getQuantityAvailable());
                    productMessageLabel.setTextFill(Color.RED);
                    return;
                }
                currentCustomer.addToCart(selectedProduct, quantity);
                productMessageLabel.setText(quantity + " of " + selectedProduct.getName() + " added to cart.");
                productMessageLabel.setTextFill(Color.GREEN);
                refreshShoppingCart();
            } catch (NumberFormatException e) {
                productMessageLabel.setText("Invalid quantity. Please enter a number.");
                productMessageLabel.setTextFill(Color.RED);
            } catch (ValidationException e) {
                productMessageLabel.setText("Error adding to cart: " + e.getMessage());
                productMessageLabel.setTextFill(Color.RED);
            }
        }
    }

    /**
     * Handles removing a selected item from the shopping cart.
     */
    private void handleRemoveFromCart() {
        String selectedCartItemString = shoppingCartView.getSelectionModel().getSelectedItem();
        if (selectedCartItemString == null) {
            productMessageLabel.setText("Please select an item in the cart to remove.");
            productMessageLabel.setTextFill(Color.RED);
            return;
        }

        // Extract product ID from the cart item string
        String productId = selectedCartItemString.substring(selectedCartItemString.lastIndexOf("ID: ") + 4);
        Product productToRemove = deliverySystem.getProduct(productId);

        if (productToRemove != null) {
            currentCustomer.removeFromCart(productToRemove);
            productMessageLabel.setText(productToRemove.getName() + " removed from cart.");
            productMessageLabel.setTextFill(Color.GREEN);
            refreshShoppingCart();
        } else {
            productMessageLabel.setText("Error: Could not find product in cart.");
            productMessageLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Handles clearing the entire shopping cart.
     */
    private void handleClearCart() {
        currentCustomer.clearCart();
        productMessageLabel.setText("Shopping cart cleared.");
        productMessageLabel.setTextFill(Color.GREEN);
        refreshShoppingCart();
    }

    /**
     * Refreshes the shopping cart view and updates the total.
     */
    private void refreshShoppingCart() {
        shoppingCartView.getItems().clear();
        double total = 0;
        if (currentCustomer != null) {
            for (ShoppingCartItem entry : currentCustomer.getShoppingCart()) {
                Product product = entry.getProduct();
                int quantity = entry.getQuantity();
                shoppingCartView.getItems().add(String.format("%s (Qty: %d) - ID: %s", product.getName(), quantity, product.getProductId()));
                total += product.getPrice() * quantity;
            }
        }
        cartTotalLabel.setText(String.format("Cart Total: $%.2f", total));
    }


    /**
     * Handles placing an order.
     * Calls the DeliverySystem to process the order and handles exceptions.
     */
    private void handlePlaceOrder() {
        if (currentCustomer.getShoppingCart().isEmpty()) {
            productMessageLabel.setText("Your shopping cart is empty.");
            productMessageLabel.setTextFill(Color.RED);
            return;
        }

        try {
            boolean success = deliverySystem.processOrder(currentCustomer, currentCustomer.getShoppingCart());
            if (success) {
                productMessageLabel.setText("Order placed successfully! Delivery is being arranged.");
                productMessageLabel.setTextFill(Color.GREEN);
                refreshShoppingCart();
                refreshProductList(deliverySystem.getAllProducts());
                refreshOrderHistoryTable(); // This is now safe as orderHistoryTable is initialized
            } else {
                productMessageLabel.setText("Order failed. Please check product availability.");
                productMessageLabel.setTextFill(Color.RED);
            }
        } catch (OutOfSeasonException | DeliveryUnavailableException e) {
            productMessageLabel.setText("Order failed: " + e.getMessage());
            productMessageLabel.setTextFill(Color.RED);
        } catch (Exception e) {
            productMessageLabel.setText("An unexpected error occurred: " + e.getMessage());
            productMessageLabel.setTextFill(Color.RED);
            e.printStackTrace();
        }
    }

    /**
     * Refreshes the order history table for the current customer.
     */
    private void refreshOrderHistoryTable() {
        if (currentCustomer != null) {
            List<Order> orders = deliverySystem.getOrdersForCustomer(currentCustomer.getUserId());
            orderHistoryTable.getItems().setAll(orders);
            if (orders.isEmpty()) {
                orderHistoryMessageLabel.setText("No past orders found.");
                orderHistoryMessageLabel.setTextFill(Color.BLACK);
            } else {
                orderHistoryMessageLabel.setText("");
            }
        }
    }

    /**
     * Handles canceling a selected order from the order history.
     */
    private void handleCancelOrder() {
        Order selectedOrder = orderHistoryTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            orderHistoryMessageLabel.setText("Please select an order to cancel.");
            orderHistoryMessageLabel.setTextFill(Color.RED);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Order Cancellation");
        alert.setHeaderText("Cancel Order: " + selectedOrder.getOrderId());
        alert.setContentText("Are you sure you want to cancel this order? Stock will be returned.");

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.OK) {
            if (deliverySystem.cancelOrder(selectedOrder.getOrderId())) {
                orderHistoryMessageLabel.setText("Order " + selectedOrder.getOrderId() + " cancelled successfully.");
                orderHistoryMessageLabel.setTextFill(Color.GREEN);
                refreshOrderHistoryTable();
                refreshProductList(deliverySystem.getAllProducts());
            } else {
                orderHistoryMessageLabel.setText("Failed to cancel order " + selectedOrder.getOrderId() + ". It might already be delivered or cancelled.");
                orderHistoryMessageLabel.setTextFill(Color.RED);
            }
        }
    }

    /**
     * Populates the list of farmers available for subscription.
     */
    private void refreshFarmerSubscriptionList() {
        farmerSubscriptionListView.getItems().clear();
        for (Farmer farmer : deliverySystem.getAllFarmers()) {
            farmerSubscriptionListView.getItems().add(farmer);
        }
    }

    /**
     * Handles subscribing to a selected farmer.
     */
    private void handleSubscribeToFarmer() {
        Farmer selectedFarmer = farmerSubscriptionListView.getSelectionModel().getSelectedItem();
        if (selectedFarmer == null) {
            subscriptionMessageLabel.setText("Please select a farmer to subscribe to.");
            subscriptionMessageLabel.setTextFill(Color.RED);
            return;
        }

        String subscriptionType = "Weekly Product Box";

        if (deliverySystem.subscribeToFarmer(currentCustomer.getUserId(), selectedFarmer.getUserId(), subscriptionType)) {
            subscriptionMessageLabel.setText("Successfully subscribed to " + selectedFarmer.getName() + " for " + subscriptionType + "!");
            subscriptionMessageLabel.setTextFill(Color.GREEN);
            refreshCurrentSubscriptionsList();
        } else {
            subscriptionMessageLabel.setText("Failed to subscribe to " + selectedFarmer.getName() + ". You might already be subscribed.");
            subscriptionMessageLabel.setTextFill(Color.RED);
        }
    }

    /**
     * Refreshes the list of current subscriptions for the customer.
     */
    private void refreshCurrentSubscriptionsList() {
        currentSubscriptionsListView.getItems().clear();
        for (Subscription sub : deliverySystem.getSubscriptionsForCustomer(currentCustomer.getUserId())) {
            currentSubscriptionsListView.getItems().add(sub);
        }
        if (currentSubscriptionsListView.getItems().isEmpty()) {
            subscriptionMessageLabel.setText("You have no active subscriptions.");
            subscriptionMessageLabel.setTextFill(Color.BLACK);
        } else {
            subscriptionMessageLabel.setText("");
        }
    }

    /**
     * Returns the BorderPane containing the entire Customer Portal GUI.
     *
     * @return The BorderPane view.
     */
    public BorderPane getView() {
        return mainLayout;
    }
}
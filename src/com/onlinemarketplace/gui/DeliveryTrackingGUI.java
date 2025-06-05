package com.onlinemarketplace.gui;

import com.onlinemarketplace.model.Customer;
import com.onlinemarketplace.model.Farmer;
import com.onlinemarketplace.model.Order;
import com.onlinemarketplace.service.DeliverySystem;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GUI component for Delivery Tracking.
 * Simulates a delivery tracking map and displays active orders.
 * Uses a simple animation for delivery vehicle movement.
 */
public class DeliveryTrackingGUI {

    private final DeliverySystem deliverySystem;
    private VBox view;
    private Pane mapPane; // Using Pane for simple shapes
    private Label activeOrdersLabel;

    // Simulated delivery data (using a List)
    private List<DeliveryStatus> simulatedDeliveries;
    private Random random;

    // Define some arbitrary geographical bounds for mapping to screen pixels
    // These values should encompass the range of lat/lon you expect in your data
    private static final double MAP_MIN_LAT = 24.0;
    private static final double MAP_MAX_LAT = 25.5;
    private static final double MAP_MIN_LON = 54.5;
    private static final double MAP_MAX_LON = 55.5;


    /**
     * Represents the simulated status of a delivery.
     */
    private static class DeliveryStatus {
        String orderId;
        String customerId;
        String customerLocation; // Raw location string (lat,lon)
        String status; // e.g., "Processing", "Dispatched", "In Transit", "Delivered"
        double vehicleX, vehicleY; // Current position on map in pixels
        double targetX, targetY; // Destination on map in pixels
        Circle vehicleIcon; // Visual representation of the vehicle
        Timeline deliveryTimeline; // Timeline for animation

        DeliveryStatus(String orderId, String customerId, String customerLocation, double startX, double startY, double targetX, double targetY) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.customerLocation = customerLocation;
            this.status = "Processing"; // Initial status for simulation
            this.vehicleX = startX;
            this.vehicleY = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.vehicleIcon = new Circle(startX, startY, 8, Color.BLUE); // Blue circle for vehicle
            this.vehicleIcon.setStroke(Color.DARKBLUE);
            this.vehicleIcon.setStrokeWidth(2);
        }

        public String getStatusText() {
            return String.format("Order ID: %s, Customer: %s, Status: %s",
                    orderId, customerLocation, status);
        }
    }

    /**
     * Constructs a DeliveryTrackingGUI.
     *
     * @param deliverySystem The core delivery system instance.
     */
    public DeliveryTrackingGUI(DeliverySystem deliverySystem) {
        this.deliverySystem = deliverySystem;
        this.simulatedDeliveries = new ArrayList<>();
        this.random = new Random();
        initializeGUI();
        startSimulationUpdates();
    }

    /**
     * Initializes the GUI components and layout for the delivery tracking.
     */
    private void initializeGUI() {
        view = new VBox(10);
        view.setPadding(new Insets(20));
        view.setStyle("-fx-background-color: #f0f8ff;"); // Alice blue background

        Label title = new Label("Delivery Tracking Map");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0056b3;"); // Darker blue

        activeOrdersLabel = new Label("Active Deliveries: \nNone");
        activeOrdersLabel.setWrapText(true);
        activeOrdersLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");

        mapPane = new Pane();

        // Set a background image for the mapPane



        mapPane.setStyle(
                "-fx-background-image: url('" + "https://c8.alamy.com/comp/EX6RBB/colour-satellite-image-of-abu-dhabi-united-arab-emirates-image-taken-EX6RBB.jpg" + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-border-color: #333;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 5px;"
        );

        mapPane.setPrefSize(800, 500); // Fixed size for the map area

        view.getChildren().addAll(title, activeOrdersLabel, mapPane);
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
            // drawMap(); // Not strictly needed for Pane, but can be used for custom drawing
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
                // For simplicity, let's pick a random farmer's location or a central point for origin.
                // Or generate a random point within map bounds if no farmer is suitable.
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

                // Start animation for this delivery
                newDelivery.deliveryTimeline = new Timeline(new KeyFrame(Duration.millis(50), new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                    @Override
                    public void handle(javafx.event.ActionEvent event) {
                        moveVehicle(newDelivery);
                    }
                }));
                newDelivery.deliveryTimeline.setCycleCount(Animation.INDEFINITE);
                newDelivery.deliveryTimeline.play();
                System.out.println("Simulating new delivery for Order ID: " + order.getOrderId());
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
                System.out.println("Simulated delivery for Order ID: " + delivery.orderId + " removed due to cancellation.");
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
                System.out.println("Delivery completed for Order ID: " + delivery.orderId);
                deliveriesToRemove.add(delivery);
                continue;
            }
            // If the actual order status changed to something other than Dispatched/Pending
            if (actualOrder != null && !"Dispatched".equals(actualOrder.getStatus()) && !"Pending".equals(actualOrder.getStatus()) && !"In Transit".equals(delivery.status)) {
                 // This means the order was handled externally (e.g., cancelled or delivered by other means)
                 if (delivery.deliveryTimeline != null) delivery.deliveryTimeline.stop();
                 mapPane.getChildren().remove(delivery.vehicleIcon);
                 System.out.println("Simulated delivery for Order ID: " + delivery.orderId + " removed due to external status change.");
                 deliveriesToRemove.add(delivery);
            }
        }
        simulatedDeliveries.removeAll(deliveriesToRemove); // Remove completed/cancelled deliveries
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
     * Redraws the simulated map.
     * Currently, this mainly involves updating vehicle positions.
     */
    private void drawMap() {
        // With Pane and Shapes, updates are handled by setting properties of the shapes.
        // If using Canvas, you would clear and redraw here.
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
     * Returns the VBox containing the entire Delivery Tracking GUI.
     *
     * @return The VBox view.
     */
    public VBox getView() {
        return view;
    }
}
package com.onlinemarketplace.model;

import com.onlinemarketplace.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer in the Online Marketplace System.
 * Inherits from the abstract User class and includes customer-specific details
 * like delivery address (now location coordinates) and a shopping cart.
 */
public class Customer extends User {
    private String location; // Changed from deliveryAddress to location (latitude,longitude string)
    private List<ShoppingCartItem> shoppingCart;

    /**
     * Constructs a new Customer instance.
     *
     * @param customerId The unique identifier for the customer.
     * @param name The name of the customer.
     * @param email The email address of the customer.
     * @param password The password for customer authentication.
     * @param location The delivery location for the customer (latitude,longitude string).
     * @throws ValidationException If email or location is invalid.
     */
    public Customer(String customerId, String name, String email, String password, String location)
            throws ValidationException {
        super(customerId, name, email, password, "Customer");
        setLocation(location); // Use setter for validation
        this.shoppingCart = new ArrayList<>();
    }

    // --- Getters ---

    public String getLocation() {
        return location;
    }

    public List<ShoppingCartItem> getShoppingCart() {
        return shoppingCart;
    }

    /**
     * Parses the latitude from the location string.
     * @return The latitude as a double, or 0.0 if parsing fails.
     */
    public double getLatitude() {
        if (location == null || location.trim().isEmpty()) return 0.0;
        try {
            String[] parts = location.split(",");
            return Double.parseDouble(parts[0].trim());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing latitude from customer location string: " + location + ". Defaulting to 0.");
            return 0.0;
        }
    }

    /**
     * Parses the longitude from the location string.
     * @return The longitude as a double, or 0.0 if parsing fails.
     */
    public double getLongitude() {
        if (location == null || location.trim().isEmpty()) return 0.0;
        try {
            String[] parts = location.split(",");
            return Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing longitude from customer location string: " + location + ". Defaulting to 0.");
            return 0.0;
        }
    }


    // --- Setters with Validation ---

    /**
     * Sets the delivery location for the customer.
     *
     * @param location The new delivery location (latitude,longitude string).
     * @throws ValidationException If the location is null, empty, or not in valid format.
     */
    public void setLocation(String location) throws ValidationException {
        if (location == null || location.trim().isEmpty()) {
            throw new ValidationException("Location cannot be empty.");
        }
        // Basic format validation: check for comma and two parseable doubles
        String[] parts = location.split(",");
        if (parts.length != 2) {
            throw new ValidationException("Location must be in 'latitude,longitude' format.");
        }
        try {
            Double.parseDouble(parts[0].trim());
            Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Latitude and Longitude must be valid numbers.");
        }
        this.location = location;
    }

    // --- Shopping Cart Operations ---

    /**
     * Adds a product to the shopping cart or updates its quantity if already present.
     *
     * @param product The product to add.
     * @param quantity The quantity of the product to add.
     * @throws ValidationException If the quantity is not positive.
     */
    public void addToCart(Product product, int quantity) throws ValidationException {
        if (product == null) {
            throw new ValidationException("Product cannot be null.");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be positive to add to cart.");
        }

        boolean found = false;
        for (int i = 0; i < shoppingCart.size(); i++) {
            ShoppingCartItem item = shoppingCart.get(i);
            if (item.getProduct().equals(product)) {
                item.addQuantity(quantity); // Update quantity of existing item
                found = true;
                break;
            }
        }
        if (!found) {
            shoppingCart.add(new ShoppingCartItem(product, quantity)); // Add new item
        }
    }

    /**
     * Removes a product from the shopping cart.
     *
     * @param product The product to remove.
     */
    public void removeFromCart(Product product) {
        for (int i = 0; i < shoppingCart.size(); i++) {
            ShoppingCartItem item = shoppingCart.get(i);
            if (item.getProduct().equals(product)) {
                shoppingCart.remove(i);
                return; // Assuming only one entry per product
            }
        }
    }

    /**
     * Clears all items from the shopping cart.
     */
    public void clearCart() {
        shoppingCart.clear();
    }

    // --- Implementations of Abstract User Methods ---

    @Override
    public boolean authenticate(String enteredPassword) {
        return this.password.equals(enteredPassword);
    }

    @Override
    public boolean checkServiceEligibility() {
        // Simple eligibility check: assume any non-empty and valid location is eligible.
        return location != null && !location.trim().isEmpty() && location.contains(",");
    }

    @Override
    public String getNotificationPreferences() {
        // For simplicity, assume customers prefer email notifications.
        return "email";
    }

    @Override
    public String toString() {
        return "Customer ID: " + userId + ", Name: " + name + ", Email: " + email + ", Location: " + location;
    }
}
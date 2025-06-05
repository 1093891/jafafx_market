package com.onlinemarketplace.model;

import com.onlinemarketplace.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a farmer in the Online Marketplace System.
 * Inherits from the abstract User class and includes farmer-specific details
 * like farm location (coordinates) and a list of products they offer.
 */
public class Farmer extends User {
    private String location; // Changed from farmAddress to location (latitude,longitude string)
    private List<Product> availableProducts;

    /**
     * Constructs a new Farmer instance.
     *
     * @param farmerId The unique identifier for the farmer.
     * @param name The name of the farmer.
     * @param email The email address of the farmer.
     * @param password The password for farmer authentication.
     * @param location The location of the farmer's farm (latitude,longitude string).
     * @throws ValidationException If email or location is invalid.
     */
    public Farmer(String farmerId, String name, String email, String password, String location)
            throws ValidationException {
        super(farmerId, name, email, password, "Farmer");
        setLocation(location); // Use setter for validation
        this.availableProducts = new ArrayList<>();
    }

    // --- Getters ---

    public String getLocation() {
        return location;
    }

    public List<Product> getAvailableProducts() {
        return availableProducts;
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
            System.err.println("Error parsing latitude from farmer location string: " + location + ". Defaulting to 0.");
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
            System.err.println("Error parsing longitude from farmer location string: " + location + ". Defaulting to 0.");
            return 0.0;
        }
    }

    // --- Setters with Validation ---

    /**
     * Sets the farm location for the farmer.
     *
     * @param location The new farm location (latitude,longitude string).
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

    // --- Product Management Operations ---

    /**
     * Adds a product to the farmer's list of available products.
     * If a product with the same ID already exists, it updates the existing product.
     *
     * @param product The product to add or update.
     */
    public void addOrUpdateProduct(Product product) {
        if (product == null) {
            return;
        }
        boolean found = false;
        for (int i = 0; i < availableProducts.size(); i++) {
            if (availableProducts.get(i).getProductId().equals(product.getProductId())) {
                availableProducts.set(i, product);
                found = true;
                break;
            }
        }
        if (!found) {
            availableProducts.add(product);
        }
    }

    /**
     * Removes a product from the farmer's list of available products.
     *
     * @param productId The ID of the product to remove.
     * @return true if the product was removed, false otherwise.
     */
    public boolean removeProduct(String productId) {
        for (int i = 0; i < availableProducts.size(); i++) {
            if (availableProducts.get(i).getProductId().equals(productId)) {
                availableProducts.remove(i);
                return true;
            }
        }
        return false;
    }

    // --- Implementations of Abstract User Methods ---

    @Override
    public boolean authenticate(String enteredPassword) {
        return this.password.equals(enteredPassword);
    }

    @Override
    public boolean checkServiceEligibility() {
        return location != null && !location.trim().isEmpty() && location.contains(",");
    }

    @Override
    public String getNotificationPreferences() {
        return "email";
    }

    @Override
    public String toString() {
        return "Farmer ID: " + userId + ", Name: " + name + ", Email: " + email + ", Location: " + location;
    }
}
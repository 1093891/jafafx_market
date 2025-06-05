package com.onlinemarketplace.service;

import com.onlinemarketplace.model.Farmer;
import com.onlinemarketplace.model.Product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the IProductSearch interface, providing concrete search and filtering logic.
 * This class uses basic loops and conditional statements for search and sorting.
 */
public class ProductSearchEngine implements IProductSearch {

    private final DeliverySystem deliverySystem; // Reference to the core system to access products and farmers

    /**
     * Constructs a ProductSearchEngine with a reference to the DeliverySystem.
     *
     * @param deliverySystem The DeliverySystem instance to query for products and farmers.
     */
    public ProductSearchEngine(DeliverySystem deliverySystem) {
        this.deliverySystem = deliverySystem;
    }

    /**
     * Searches for products that are currently in season.
     * Prioritizes products that are most recently harvested using a basic bubble sort.
     *
     * @param date The reference date to determine seasonality.
     * @return A list of in-season products, sorted by harvest date (most recent first).
     */
    @Override
    public List<Product> searchBySeason(LocalDate date) {
        List<Product> inSeasonProducts = new ArrayList<>();
        List<Product> allProducts = deliverySystem.getAllProducts();

        for (Product product : allProducts) {
            if (product.isInSeason(date)) {
                inSeasonProducts.add(product);
            }
        }

        // Basic Bubble Sort by Harvest Date (descending - most recent first)
        for (int i = 0; i < inSeasonProducts.size() - 1; i++) {
            for (int j = 0; j < inSeasonProducts.size() - i - 1; j++) {
                if (inSeasonProducts.get(j).getHarvestDate().isBefore(inSeasonProducts.get(j + 1).getHarvestDate())) {
                    // Swap
                    Product temp = inSeasonProducts.get(j);
                    inSeasonProducts.set(j, inSeasonProducts.get(j + 1));
                    inSeasonProducts.set(j + 1, temp);
                }
            }
        }
        return inSeasonProducts;
    }

    /**
     * Filters products based on their farm's proximity to a given customer location.
     * This is a simulated proximity search using Euclidean distance.
     *
     * @param customerLocation The customer's delivery location (latitude,longitude string).
     * @param radiusKm The maximum radius (in kilometers) from the customer's location to search for farms.
     * @return A list of products from farms within the specified radius, sorted by proximity.
     */
    @Override
    public List<Product> searchByProximity(String customerLocation, Double radiusKm) {
        List<Product> proximateProducts = new ArrayList<>();
        List<Farmer> allFarmers = deliverySystem.getAllFarmers();

        double customerLat = 0.0;
        double customerLon = 0.0;
        try {
            String[] customerCoords = customerLocation.split(",");
            customerLat = Double.parseDouble(customerCoords[0].trim());
            customerLon = Double.parseDouble(customerCoords[1].trim());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Invalid customer location format for proximity search: " + customerLocation);
            return proximateProducts; // Return empty list if location is invalid
        }


        // Find nearby farmers and store them with their simulated distance
        List<FarmerWithDistance> farmersWithDistances = new ArrayList<>();
        for (Farmer farmer : allFarmers) {
            double farmerLat = farmer.getLatitude();
            double farmerLon = farmer.getLongitude();

            double distance = calculateEuclideanDistance(customerLat, customerLon, farmerLat, farmerLon);

            // Simulate radius (e.g., if distance in degrees is less than some threshold)
            // A small radius (e.g., 0.1 degree) would be very close. 1 degree is roughly 111km.
            // So, for 'radiusKm', we need to scale the distance.
            // Simplified threshold: 1 degree approx 111 km. So radiusKm / 111 degrees.

            if (distance <= radiusKm) { // Check if within simulated radius
                farmersWithDistances.add(new FarmerWithDistance(farmer, (distance))); // Store scaled distance
            }
        }

        // Basic Bubble Sort farmers by simulated distance (ascending)
        for (int i = 0; i < farmersWithDistances.size() - 1; i++) {
            for (int j = 0; j < farmersWithDistances.size() - i - 1; j++) {
                if (farmersWithDistances.get(j).getSimulatedDistance() > farmersWithDistances.get(j + 1).getSimulatedDistance()) {
                    // Swap
                    FarmerWithDistance temp = farmersWithDistances.get(j);
                    farmersWithDistances.set(j, farmersWithDistances.get(j + 1));
                    farmersWithDistances.set(j + 1, temp);
                }
            }
        }

        // Collect products from these sorted nearby farmers
        for (FarmerWithDistance fwd : farmersWithDistances) {
            List<Product> farmerProducts = fwd.getFarmer().getAvailableProducts();
            for (Product product : farmerProducts) {
                proximateProducts.add(product);
            }
        }
        return proximateProducts;
    }

    /**
     * Calculates a simplified Euclidean distance between two points (lat, lon).
     * This is NOT geographically accurate but provides a basic relative proximity.
     * For true geographical distance, the Haversine formula is needed.
     */
    private double calculateEuclideanDistance(double lat1, double lon1, double lat2, double lon2) {
        double dx = lat1 - lat2;
        double dy = lon1 - lon2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Helper class for sorting farmers by simulated distance.
     */
    private static class FarmerWithDistance {
        private Farmer farmer;
        private Double simulatedDistance;

        public FarmerWithDistance(Farmer farmer, Double simulatedDistance) {
            this.farmer = farmer;
            this.simulatedDistance = simulatedDistance;
        }

        public Farmer getFarmer() {
            return farmer;
        }

        public Double getSimulatedDistance() {
            return simulatedDistance;
        }
    }




    /**
     * Finds products belonging to a specific category.
     * Highlights organic options by prioritizing them in the returned list using a basic bubble sort.
     *
     * @param category The category to search for (e.g., "vegetables", "fruits", "dairy").
     * @return A list of products matching the given category, with organic options appearing first.
     */
    @Override
    public List<Product> searchByCategory(String category) {
        List<Product> matchingProducts = new ArrayList<>();
        List<Product> allProducts = deliverySystem.getAllProducts();

        String lowerCaseCategory = (category != null) ? category.toLowerCase() : "";

        for (Product product : allProducts) {
            if (product.getDescription().toLowerCase().contains(lowerCaseCategory) ||
                product.getName().toLowerCase().contains(lowerCaseCategory)) {
                matchingProducts.add(product);
            }
        }

        // Basic Bubble Sort: Organic first
        for (int i = 0; i < matchingProducts.size() - 1; i++) {
            for (int j = 0; j < matchingProducts.size() - i - 1; j++) {
                boolean isOrganicJ = matchingProducts.get(j).getDescription().toLowerCase().contains("organic");
                boolean isOrganicJPlus1 = matchingProducts.get(j + 1).getDescription().toLowerCase().contains("organic");

                if (!isOrganicJ && isOrganicJPlus1) { // If J is not organic but J+1 is, swap them
                    Product temp = matchingProducts.get(j);
                    matchingProducts.set(j, matchingProducts.get(j + 1));
                    matchingProducts.set(j + 1, temp);
                }
            }
        }
        return matchingProducts;
    }

    /**
     * Finds products by a general search term, combining various criteria.
     *
     * @param searchTerm The term to search for.
     * @return A list of products matching the search term in name, description, or ID.
     */
    public List<Product> searchProducts(String searchTerm) {
        List<Product> foundProducts = new ArrayList<>();
        List<Product> allProducts = deliverySystem.getAllProducts();

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return allProducts;
        }
        String lowerCaseSearchTerm = searchTerm.toLowerCase();

        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(lowerCaseSearchTerm) ||
                p.getDescription().toLowerCase().contains(lowerCaseSearchTerm) ||
                p.getProductId().toLowerCase().contains(lowerCaseSearchTerm)) {
                foundProducts.add(p);
            }
        }
        return foundProducts;
    }
}
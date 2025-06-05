package com.onlinemarketplace.gui;


import com.onlinemarketplace.model.Customer;
import com.onlinemarketplace.model.Farmer;
import com.onlinemarketplace.model.User;
import com.onlinemarketplace.service.DeliverySystem;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * GUI for user login and registration.
 * Allows users to sign in to existing accounts or create new ones.
 */
public class LoginPageGUI {
    private final DeliverySystem deliverySystem;
    private final LoginSuccessCallback onLoginSuccess;
    private final LogoutCallback onLogoutSuccess;
    private VBox view, loginFormContainer, registrationFormContainer;
    private TextField userIdField, regNameField, regEmailField, regLocationField;
    private PasswordField passwordField, regPasswordField, regConfirmPasswordField;
    private ComboBox<String> regUserTypeSelector;
    private Label loginMessageLabel, registrationMessageLabel;
    private static final String BORDER_STYLE = "-fx-padding: 20; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-color: rgba(100,100,100,0.5);";
    private final String backgroundImageUrl = "https://static.vecteezy.com/system/resources/thumbnails/039/837/857/small_2x/ai-generated-grocery-store-display-cases-with-products-ai-generated-image-photo.jpg";

    public LoginPageGUI(DeliverySystem deliverySystem, LoginSuccessCallback onLoginSuccess, LogoutCallback onLogoutSuccess) {
        this.deliverySystem = deliverySystem;
        this.onLoginSuccess = onLoginSuccess;
        this.onLogoutSuccess = onLogoutSuccess;
        initializeGUI();
        showLoginForm();
    }

    private void initializeGUI() {
        view = createVBox(15, "-fx-background-image: url('" + backgroundImageUrl + "'); -fx-background-size: cover;" +
                "-fx-background-position: center center; -fx-border-color: #ccc; -fx-border-width: 1px; -fx-border-radius: 5px");
        loginFormContainer = createFormContainer("Sign In", e -> handleLogin(), e -> showRegistrationForm());
        registrationFormContainer = createFormContainer("Create New Account", e -> handleRegister(), e -> showLoginForm());
        view.getChildren().addAll(loginFormContainer, registrationFormContainer);
    }

    private VBox createFormContainer(String headerText, EventHandler<ActionEvent> primaryAction, EventHandler<ActionEvent> secondaryAction) {
        VBox container = createVBox(10, BORDER_STYLE);
        container.getChildren().addAll(
                createLabel(headerText, 20, true),
                headerText.equals("Sign In") ? createLoginForm() : createRegistrationForm(),
                createButtonContainer(primaryAction, secondaryAction, headerText.equals("Sign In"))
        );
        return container;
    }

    private GridPane createLoginForm() {
        GridPane loginGrid = createGridPane();
        userIdField = createTextField("User ID (e.g., C001 or F001)");
        passwordField = createPasswordField("Password");

        addFieldsToGrid(loginGrid, new String[]{"User ID:", "Password:"}, new Node[]{userIdField, passwordField});
        loginMessageLabel = createLabel("", 0, false);
        return loginGrid;
    }

    private GridPane createRegistrationForm() {
        GridPane regGrid = createGridPane();
        regNameField = createTextField("Your Name");
        regEmailField = createTextField("Email");
        regPasswordField = createPasswordField("Password");
        regConfirmPasswordField = createPasswordField("Confirm Password");
        regLocationField = createTextField("Location (Lat,Lon e.g., 24.5,50.4)");
        regUserTypeSelector = new ComboBox<>();
        regUserTypeSelector.getItems().addAll("Customer", "Farmer");
        regUserTypeSelector.setPromptText("Account Type");

        addFieldsToGrid(regGrid, new String[]{"Name:", "Email:", "Password:", "Confirm Password:", "Location:", "Account Type:"},
                new Node[]{regNameField, regEmailField, regPasswordField, regConfirmPasswordField, regLocationField, regUserTypeSelector});
        registrationMessageLabel = createLabel("", 0, false);
        return regGrid;
    }

    private HBox createButtonContainer(EventHandler<ActionEvent> primaryAction, EventHandler<ActionEvent> secondaryAction, boolean isLogin) {
        Button primaryButton = createButton(isLogin ? "Login" : "Register", isLogin ? "#4CAF50" : "#007BFF", primaryAction);
        Button secondaryButton = createButton(isLogin ? "Sign Up" : "Back to Login", isLogin ? "#007BFF" : "#6C757D", secondaryAction);
        return createHBox(10, primaryButton, secondaryButton);
    }

    private VBox createVBox(int spacing, String style) {
        VBox vbox = new VBox(spacing);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle(style);
        return vbox;
    }

    private HBox createHBox(int spacing, Node... children) {
        HBox hbox = new HBox(spacing);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(children);
        return hbox;
    }

    private GridPane createGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);
        return gridPane;
    }

    private void addFieldsToGrid(GridPane grid, String[] labels, Node[] fields) {
        for (int i = 0; i < labels.length; i++) {
            grid.add(createLabel(labels[i], 14, true), 0, i);
            grid.add(fields[i], 1, i);
        }
    }

    private TextField createTextField(String prompt) {
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        textField.setPrefWidth(250);
        return textField;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(prompt);
        passwordField.setPrefWidth(250);
        return passwordField;
    }

    private Label createLabel(String text, int fontSize, boolean bold) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + fontSize + "px; -fx-text-fill: #ffffff;" + (bold ? " -fx-font-weight: bold;" : ""));
        return label;
    }

    private Button createButton(String text, String color, EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 8 20; -fx-background-radius: 5;");
        button.setOnAction(action);
        return button;
    }

    private void showLoginForm() {
        toggleFormVisibility(true);
        clearFields(userIdField, passwordField);
    }

    private void showRegistrationForm() {
        toggleFormVisibility(false);
        clearFields(regNameField, regEmailField, regPasswordField, regConfirmPasswordField, regLocationField);
        regUserTypeSelector.getSelectionModel().clearSelection();
    }

    private void toggleFormVisibility(boolean showLoginForm) {
        loginFormContainer.setVisible(showLoginForm);
        loginFormContainer.setManaged(showLoginForm);
        registrationFormContainer.setVisible(!showLoginForm);
        registrationFormContainer.setManaged(!showLoginForm);
        clearMessages();
    }

    private void clearMessages() {
        loginMessageLabel.setText("");
        registrationMessageLabel.setText("");
    }

    private void clearFields(TextField... fields) {
        for (TextField field : fields) field.clear();
    }

    private void handleLogin() {
        if (isFieldEmpty(userIdField, passwordField)) {
            setMessage(loginMessageLabel, "Please enter User ID and Password.", Color.RED);
            return;
        }
        User user = deliverySystem.authenticateUser(userIdField.getText().trim(), passwordField.getText());
        if (user != null) {
            setMessage(loginMessageLabel, "Login successful! Welcome, " + user.getName() + "!", Color.GREEN);
            onLoginSuccess.onLoginSuccess(user);
        } else {
            setMessage(loginMessageLabel, "Login failed. Invalid User ID or Password.", Color.RED);
        }
    }

    private void handleRegister() {
        if (isFieldEmpty(regNameField, regEmailField, regPasswordField, regConfirmPasswordField, regLocationField) ||
                regUserTypeSelector.getSelectionModel().isEmpty()) {
            setMessage(registrationMessageLabel, "Please fill in all registration fields.", Color.RED);
            return;
        }
        if (!regPasswordField.getText().equals(regConfirmPasswordField.getText())) {
            setMessage(registrationMessageLabel, "Passwords do not match.", Color.RED);
            return;
        }
        String userType = regUserTypeSelector.getValue();
        String userId = generateUserId(userType.equals("Customer") ? "C" : "F");
        try {
            User user = userType.equals("Customer") ?
                    new Customer(userId, regNameField.getText().trim(), regEmailField.getText().trim(), regPasswordField.getText(), regLocationField.getText())
                    : new Farmer(userId, regNameField.getText().trim(), regEmailField.getText().trim(), regPasswordField.getText(), regLocationField.getText());
            if (deliverySystem.registerUser(user)) {
                setMessage(registrationMessageLabel, "Account created for " + regNameField.getText() + " (ID: " + userId + ").", Color.GREEN);
                PauseTransition delay = new PauseTransition(Duration.seconds(5));
                delay.setOnFinished(e -> showLoginForm());
                delay.play();
            } else {
                setMessage(registrationMessageLabel, "Registration failed. Try again.", Color.RED);
            }
        } catch (Exception e) {
            setMessage(registrationMessageLabel, "Registration error: " + e.getMessage(), Color.RED);
        }
    }

    private boolean isFieldEmpty(TextField... fields) {
        for (TextField field : fields) {
            if (field.getText().trim().isEmpty()) return true;
        }
        return false;
    }

    private String generateUserId(String prefix) {
        int counter = 1;
        while (deliverySystem.getUser(prefix + String.format("%03d", counter)) != null) counter++;
        return prefix + String.format("%03d", counter);
    }

    private void setMessage(Label label, String message, Color color) {
        label.setText(message);
        label.setTextFill(color);
    }

    public VBox getView() {
        return view;
    }
}
package com.fitmate.controller;

import com.fitmate.dao.*;
import com.fitmate.model.ChatMessage;
import com.fitmate.service.*;
import com.fitmate.util.AppColors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * Controller for the AI Coach (Zenith) chat sub-view.
 * Loads previous chat history from Firestore on open, then supports
 * sending new messages to the OpenAI API via ChatbotService.
 * User and assistant messages are rendered as styled chat bubbles.
 */
public class AICoachController {

    @FXML private VBox       chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  messageInput;
    @FXML private Button     sendButton;
    @FXML private Label      errorLabel;

    private final ChatDAO chatDAO = new ChatDAOImpl();

    @FXML
    public void initialize() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) {
            showError("You must be logged in to use the AI Coach.");
            return;
        }

        // Fetch and render all previous messages for this user
        loadChatHistory(uid);
    }

    /**
     * Fetches all past ChatMessage records for this user from Firestore
     * and renders each message/reply pair as chat bubbles.
     * Shows a placeholder label if no history exists yet.
     */
    private void loadChatHistory(String uid) {
        chatDAO.getChatHistory(uid).thenAccept(messages -> Platform.runLater(() -> {
            chatContainer.getChildren().clear();

            if (messages == null || messages.isEmpty()) {
                Label empty = new Label("No previous chats yet. Ask your first question to get started! 💬");
                empty.setStyle("-fx-text-fill: " + AppColors.SECONDARY_TEXT + "; -fx-font-size: 13px;");
                empty.setWrapText(true);
                chatContainer.getChildren().add(empty);
                return;
            }

            // Add user bubble then assistant reply bubble for each chat record
            for (ChatMessage msg : messages) {
                if (msg.getMessage() != null && !msg.getMessage().isBlank())
                    addBubble(msg.getMessage(), true);
                if (msg.getReply() != null && !msg.getReply().isBlank())
                    addBubble(msg.getReply(), false);
            }

            scrollToBottom();
        })).exceptionally(ex -> {
            Platform.runLater(() -> showError("Failed to load previous chats."));
            return null;
        });
    }

    /**
     * Sends the user's typed message to the AI Coach.
     * Adds the user bubble immediately, shows a typing indicator while
     * the API call is in progress, then replaces it with the AI reply bubble.
     */
    @FXML
    private void handleSend() {
        String uid = AuthService.getInstance().getCurrentUid();
        if (uid == null) {
            showError("You must be logged in to use the AI Coach.");
            return;
        }

        String message = messageInput.getText().trim();
        if (message.isEmpty()) return;

        messageInput.clear();
        sendButton.setDisable(true);
        sendButton.setText("Thinking...");
        hideError();

        // Remove the "no previous chats" placeholder if it is still showing
        if (!chatContainer.getChildren().isEmpty()) {
            var first = chatContainer.getChildren().get(0);
            if (first instanceof Label l && l.getText().contains("No previous chats"))
                chatContainer.getChildren().clear();
        }

        // Immediately show the user's message on the right side
        addBubble(message, true);

        // Add animated typing indicator while waiting for the AI response
        Label typingLabel = new Label("● ● ●");
        typingLabel.setStyle("-fx-text-fill: " + AppColors.SECONDARY_TEXT + "; -fx-font-size: 16px;");
        HBox typingBox = new HBox(typingLabel);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        typingBox.setPadding(new Insets(4));
        chatContainer.getChildren().add(typingBox);
        scrollToBottom();

        // Call the AI service and replace the typing indicator with the real reply
        ChatbotService.getInstance().askAI(uid, message)
                .thenAccept(reply -> Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typingBox);
                    addBubble(reply, false);
                    sendButton.setDisable(false);
                    sendButton.setText("Send");
                    scrollToBottom();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        chatContainer.getChildren().remove(typingBox);
                        showError("Failed to contact AI Coach. Please try again.");
                        sendButton.setDisable(false);
                        sendButton.setText("Send");
                    });
                    return null;
                });
    }

    /**
     * Creates a styled chat bubble TextFlow and wraps it in an HBox
     * aligned right (user) or left (assistant).
     *
     * @param text   the message content to display
     * @param isUser true for user bubble (right-aligned, accent color),
     *               false for assistant bubble (left-aligned, dark background)
     */
    private void addBubble(String text, boolean isUser) {
        Text textNode = new Text(text);
        textNode.setStyle("-fx-fill: " + (isUser ? AppColors.PRIMARY_BG : AppColors.PRIMARY_TEXT) + "; -fx-font-size: 13px;");

        TextFlow textFlow = new TextFlow(textNode);
        textFlow.setMaxWidth(450);
        textFlow.setPadding(new Insets(10, 14, 10, 14));
        textFlow.setStyle(isUser
                ? "-fx-background-color: " + AppColors.PRIMARY_ACCENT + "; -fx-background-radius: 12 12 4 12;"
                : "-fx-background-color: #262626; -fx-background-radius: 12 12 12 4;");

        HBox wrapper = new HBox(textFlow);
        wrapper.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(2, 0, 2, 0));

        chatContainer.getChildren().add(wrapper);
    }

    /** Scrolls the chat pane to the bottom so the latest message is always visible. */
    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}

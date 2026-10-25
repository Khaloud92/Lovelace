package com.sep.sep_backend.chatTest;

import com.sep.sep_backend.chat.Conversation;
import com.sep.sep_backend.chat.Message;
import com.sep.sep_backend.registration.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class MessageTest {

    // --- PrePersist Lifecycle Hook ---

    @Test
    void prePersist_shouldSetSentAtWhenNull() {
        // Arrange
        Message message = new Message();
        message.setConversation(mock(Conversation.class));
        message.setSender(mock(AppUser.class));
        message.setContent("Test Nachricht");

        assertNull(message.getSentAt());

        // Act
        message.prePersist();

        // Assert
        assertNotNull(message.getSentAt());
        // Überprüft, ob das gesetzte Datum im Toleranzbereich von 1 Sekunde liegt
        assertTrue(message.getSentAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(message.getSentAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void prePersist_shouldNotOverrideSentAtWhenAlreadySet() {
        // Arrange
        Message message = new Message();
        message.setConversation(mock(Conversation.class));
        message.setSender(mock(AppUser.class));
        message.setContent("Alte Nachricht");

        LocalDateTime customTime = LocalDateTime.now().minusHours(3);
        message.setSentAt(customTime);

        // Act
        message.prePersist();

        // Assert
        assertNotNull(message.getSentAt());
        assertEquals(customTime, message.getSentAt());
    }
}

package com.sep.sep_backend.chatTest;

import com.sep.sep_backend.chat.Conversation;
import com.sep.sep_backend.registration.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ConversationTest {

    // --- PrePersist Lifecycle Hook ---

    @Test
    void prePersist_shouldSetCreatedAtWhenNull() {
        // Arrange
        Conversation conversation = new Conversation();
        conversation.setType(Conversation.Type.GROUP);
        conversation.setName("Test Gruppe");
        conversation.setCreatedBy(mock(AppUser.class));

        assertNull(conversation.getCreatedAt());

        // Act
        conversation.prePersist();

        // Assert
        assertNotNull(conversation.getCreatedAt());
        // Überprüft, ob das gesetzte Datum im Toleranzbereich von 1 Sekunde liegt
        assertTrue(conversation.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(conversation.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void prePersist_shouldNotOverrideCreatedAtWhenAlreadySet() {
        // Arrange
        Conversation conversation = new Conversation();
        conversation.setType(Conversation.Type.DIRECT);
        conversation.setCreatedBy(mock(AppUser.class));

        LocalDateTime customTime = LocalDateTime.now().minusWeeks(2);
        conversation.setCreatedAt(customTime);

        // Act
        conversation.prePersist();

        // Assert
        assertNotNull(conversation.getCreatedAt());
        assertEquals(customTime, conversation.getCreatedAt());
    }
}

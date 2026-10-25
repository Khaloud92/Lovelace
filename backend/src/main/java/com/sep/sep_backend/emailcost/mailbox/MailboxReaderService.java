package com.sep.sep_backend.emailcost.mailbox;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Reads emails from a user's mailbox via IMAP.
 */
@Service
public class MailboxReaderService {

    private static final int MAX_MESSAGES = 10;

    /**
     * Verifies that the connection credentials work by opening and closing the store.
     *
     * @param connection the mailbox connection to test
     */
    public void testConnection(MailboxConnection connection) {
        try (Store store = connect(connection)) {
            // Connection successful.
        } catch (MessagingException exception) {
            throw new IllegalArgumentException("Could not connect to mailbox", exception);
        }
    }

    /**
     * Fetches the most recent inbox messages, newest first.
     *
     * @param connection the mailbox to read from
     * @return up to {@value MAX_MESSAGES} messages
     */
    public List<MailboxEmailMessage> fetchRecentMessages(MailboxConnection connection) {
        try (Store store = connect(connection)) {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            int startIndex = Math.max(1, messages.length - MAX_MESSAGES + 1);

            List<MailboxEmailMessage> result = new ArrayList<>();

            for (int messageNumber = messages.length; messageNumber >= startIndex; messageNumber--) {
                Message message = inbox.getMessage(messageNumber);

                String externalMessageId = getExternalMessageId(message);
                String subject = message.getSubject();
                String text = extractText(message);

                result.add(new MailboxEmailMessage(externalMessageId, subject, text));
            }

            inbox.close(false);
            return result;
        } catch (MessagingException | IOException exception) {
            throw new IllegalArgumentException("Could not fetch emails from mailbox", exception);
        }
    }

    private Store connect(MailboxConnection connection) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", connection.getImapHost());
        properties.put("mail.imaps.port", String.valueOf(connection.getImapPort()));
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.connectiontimeout", "10000");
        properties.put("mail.imaps.timeout", "10000");
        properties.put("mail.imaps.writetimeout", "10000");

        Session session = Session.getInstance(properties);

        Store store = session.getStore("imaps");
        store.connect(
                connection.getImapHost(),
                connection.getImapPort(),
                connection.getEmailAddress(),
                connection.getAppPassword()
        );

        return store;
    }

    private String getExternalMessageId(Message message) throws MessagingException {
        String[] messageIds = message.getHeader("Message-ID");

        if (messageIds != null && messageIds.length > 0 && messageIds[0] != null && !messageIds[0].isBlank()) {
            return messageIds[0];
        }

        String subject = message.getSubject() != null ? message.getSubject() : "no-subject";
        long sentTime = message.getSentDate() != null ? message.getSentDate().getTime() : 0L;
        long receivedTime = message.getReceivedDate() != null ? message.getReceivedDate().getTime() : 0L;

        return "imap-message-"
                + message.getMessageNumber()
                + "-"
                + sentTime
                + "-"
                + receivedTime
                + "-"
                + subject.hashCode();
    }

    private String extractText(Message message) throws MessagingException, IOException {
        Object content = message.getContent();

        if (content instanceof String text) {
            return text;
        }

        if (content instanceof Multipart multipart) {
            return extractTextFromMultipart(multipart);
        }

        return "";
    }

    private String extractTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < multipart.getCount(); index++) {
            BodyPart bodyPart = multipart.getBodyPart(index);

            if (bodyPart.isMimeType("text/plain")) {
                builder.append(bodyPart.getContent()).append("\n");
            } else if (bodyPart.isMimeType("text/html")) {
                builder.append(stripHtml(String.valueOf(bodyPart.getContent()))).append("\n");
            } else if (bodyPart.getContent() instanceof Multipart nestedMultipart) {
                builder.append(extractTextFromMultipart(nestedMultipart)).append("\n");
            }
        }

        return builder.toString().trim();
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&euro;", "€")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
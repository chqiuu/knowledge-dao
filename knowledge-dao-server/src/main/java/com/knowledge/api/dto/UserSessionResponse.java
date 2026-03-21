package com.knowledge.api.dto;

import com.knowledge.model.ChatMessage;
import java.util.List;

/** Response DTOs for user/session admin APIs */
public class UserSessionResponse {

    public static class UserInfo {
        private Long userId;
        private long entryCount;
        private long sessionCount;
        private String lastActive;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public long getEntryCount() { return entryCount; }
        public void setEntryCount(long entryCount) { this.entryCount = entryCount; }
        public long getSessionCount() { return sessionCount; }
        public void setSessionCount(long sessionCount) { this.sessionCount = sessionCount; }
        public String getLastActive() { return lastActive; }
        public void setLastActive(String lastActive) { this.lastActive = lastActive; }
    }

    public static class SessionInfo {
        private String sessionKey;
        private String userId;
        private int messageCount;
        private String firstMessage;
        private String lastMessage;

        public String getSessionKey() { return sessionKey; }
        public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
        public String getFirstMessage() { return firstMessage; }
        public void setFirstMessage(String firstMessage) { this.firstMessage = firstMessage; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    }

    public static class MessageInfo {
        private Long id;
        private String sessionKey;
        private String role;
        private String content;
        private String createdAt;
        private String userId;

        public static MessageInfo from(ChatMessage msg) {
            MessageInfo m = new MessageInfo();
            m.setId(msg.getId());
            m.setSessionKey(msg.getSessionKey());
            m.setRole(msg.getRole());
            m.setContent(msg.getContent());
            m.setCreatedAt(msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);
            m.setUserId(msg.getUserId() != null ? msg.getUserId().toString() : null);
            return m;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSessionKey() { return sessionKey; }
        public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}

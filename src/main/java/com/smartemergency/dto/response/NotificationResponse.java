package com.smartemergency.dto.response;

import com.smartemergency.enums.NotificationType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private Long relatedRequestId;
    private String actionUrl;
    private LocalDateTime createdAt;
}

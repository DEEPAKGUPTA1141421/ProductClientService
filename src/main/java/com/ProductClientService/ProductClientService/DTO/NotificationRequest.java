package com.ProductClientService.ProductClientService.DTO;


public class NotificationRequest {
    private String type;
    private String to;
    private String subject;
    private String body;

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}

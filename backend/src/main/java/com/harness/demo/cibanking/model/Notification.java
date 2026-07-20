package com.harness.demo.cibanking.model;

/**
 * An in-app notification / message shown in the customer's inbox.
 * Dummy demo data only.
 */
public class Notification {

    private String id;
    private String date;
    private String category;  // SECURITY, PAYMENT, INFO, OFFER
    private String title;
    private String message;
    private boolean read;

    public Notification() {
    }

    public Notification(String id, String date, String category, String title,
                        String message, boolean read) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.title = title;
        this.message = message;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

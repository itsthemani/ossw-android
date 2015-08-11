package com.althink.android.ossw.notifications.model;

import java.util.Date;
import java.util.List;

/**
 * Created by krzysiek on 25/07/15.
 */
public class ListNotification extends AbstractNotification {
    private String title;
    private List items;

    public ListNotification(String id, NotificationType type, NotificationCategory category, String application, Date date, List<Operation> operations, String title, List items, Object notificationObject) {
        super(id, type, category, application, date, operations, notificationObject);
        this.title = title;
        this.items = items;
    }

    public ListNotification(String id, NotificationType type, NotificationCategory category, String application, Date date, List<Operation> operations, String title, List items, Object notificationObject, int externalId) {
        super(id, type, category, application, date, operations, notificationObject, externalId);
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "ListNotification{" +
                "title='" + title + '\'' +
                ", items=" + items +
                "}+" + super.toString();
    }
}
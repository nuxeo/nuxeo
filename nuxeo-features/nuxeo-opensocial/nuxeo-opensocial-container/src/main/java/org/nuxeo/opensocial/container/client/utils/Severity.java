package org.nuxeo.opensocial.container.client.utils;

/**
 * @author Stéphane Fourrier
 */
public enum Severity {
    ERROR("#FF0000"), INFO("orange"), SUCCESS("#32CD32");

    private String color;

    private Severity(String color) {
        this.color = color;
    }

    public String getAssociatedColor() {
        return this.color;
    }
}

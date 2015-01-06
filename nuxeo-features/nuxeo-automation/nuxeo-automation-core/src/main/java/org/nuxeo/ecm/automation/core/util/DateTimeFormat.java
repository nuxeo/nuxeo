package org.nuxeo.ecm.automation.core.util;

/**
 * DateTime Format Enum for JSON Marshalling.
 *
 * @since 7.1
 */
public enum DateTimeFormat {

    W3C("w3c"), TIME_IN_MILLIS("timeInMillis");
    private final String value;

    DateTimeFormat(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DateTimeFormat fromValue(String v) {
        for (DateTimeFormat c : DateTimeFormat.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
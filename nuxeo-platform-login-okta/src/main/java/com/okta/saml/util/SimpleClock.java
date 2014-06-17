package com.okta.saml.util;

import org.joda.time.DateTime;

/**
 * A basic implementation for Clock interface
 */
public class SimpleClock implements Clock {

    @Override
    public String instant() {
        return new DateTime().toInstant().toString();
    }

    @Override
    public DateTime dateTimeNow() {
        return new DateTime();
    }

}

package com.okta.saml.util;

import org.joda.time.DateTime;

/**
 * An interface used for obtaining the current time in SAMLRequest and SAMLResponse
 */
public interface Clock {

    /**
     * @return the current time in a string in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSSZZ)
     */
    public String instant();

    /**
     * @return the current time as a DateTime object
     */
    public DateTime dateTimeNow();

}

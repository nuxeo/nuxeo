package org.nuxeo.ecm.platform.login;

import java.security.Principal;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

/**
 * Simple class to store the result of a jaas callback
 *
 * @author tiry
 *
 */
public class CallbackResult {

    public boolean cb_handled=false;

    public UserIdentificationInfo userIdent=null;

    public Principal principal=null;

    public Object credential=null;

    public CallbackResult()
    {

    }
}

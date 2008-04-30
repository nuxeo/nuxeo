package org.nuxeo.ecm.platform.login;

import javax.security.auth.callback.CallbackHandler;


/**
 * Interface for AppServer specific jaas callbacks factories
 *
 * @author tiry
 *
 */
public interface CallbackFactory {

    public CallbackResult handleSpecificCallbacks(CallbackHandler callbackHandler);

}

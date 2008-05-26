package org.nuxeo.platform.login.jboss;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.callback.SecurityAssociationCallback;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.CallbackFactory;
import org.nuxeo.ecm.platform.login.CallbackResult;

public class JBossCallbackFactory implements CallbackFactory {

    private static final Log log = LogFactory.getLog(JBossCallbackFactory.class);

    public CallbackResult handleSpecificCallbacks(
            CallbackHandler callbackHandler) {
        boolean cb_handled=false;

        CallbackResult result = new CallbackResult();

        // JBoss specific cb : handle web=>ejb propagation
        SecurityAssociationCallback ac = new SecurityAssociationCallback();
        ObjectCallback oc = new ObjectCallback("UserInfo:");

        try {
            // If this is a SecurityAssociationCBH
            // try to get UserInfo from objectCB
            callbackHandler.handle(new Callback[] { oc });
            Object cred = oc.getCredential();
            cb_handled = true;
            if (cred instanceof UserIdentificationInfo) {
                result.userIdent = (UserIdentificationInfo) cred;
            }
        } catch (UnsupportedCallbackException e) {
            log.debug("objectCB is not supported");
        } catch (IOException e) {
            log.warn("Error calling callback handler with objectCB : "
                    + e.getMessage());
        }


        if (!cb_handled || result.userIdent == null || !result.userIdent.containsValidIdentity()) {

            try {
                // If this is a SecurityAssociationCBH
                // try to get UserInfo from objectCB
                callbackHandler.handle(new Callback[] { ac });
                result.principal = ac.getPrincipal();
                result.credential = ac.getCredential();
                cb_handled = true;
            } catch (UnsupportedCallbackException e) {
                log.debug("objectCB is not supported");
            } catch (IOException e) {
                log.warn("Error calling callback handler with objectCB : "
                        + e.getMessage());
            }
        }

        result.cb_handled=cb_handled;
        return result;
    }

}

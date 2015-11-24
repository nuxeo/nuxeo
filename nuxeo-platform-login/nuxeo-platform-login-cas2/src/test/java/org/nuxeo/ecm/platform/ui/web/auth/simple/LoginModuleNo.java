package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.platform.login.NuxeoAbstractServerLoginModule;

public class LoginModuleNo extends NuxeoAbstractServerLoginModule {

    protected Principal identity;

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean commit() throws LoginException {
        
        return false;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        
    }

    public boolean login() throws LoginException {
        return false;
    }

    public boolean logout() throws LoginException {
        return true;
    }
    
    @Override
    protected Principal getIdentity() {
        return null;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        return null;
    }

    @Override
    protected Principal createIdentity(String username) throws Exception {
        return null;
    }

}

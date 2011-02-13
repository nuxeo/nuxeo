/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleLoginModule implements LoginModule {

    protected Subject subject;
    protected CallbackHandler callbackHandler;
    @SuppressWarnings("rawtypes")
    protected Map sharedState;
    protected Principal principal;


    public Principal authenticate(String[] login) throws LoginException {
        return Activator.getInstance().getRegistry().authenticate(login[0], login[1]);
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
    }

    protected String[] retrieveLogin() throws LoginException {
        PasswordCallback pc = new PasswordCallback("Password: ", false);
        NameCallback nc = new NameCallback("Username: ", "guest");
        Callback[] callbacks = { nc, pc };
        try {
            String[] login = new String[2];
            callbackHandler.handle(callbacks);
            login[0] = nc.getName();
            char[] tmpPassword = pc.getPassword();
            if (tmpPassword != null) {
                login[1] = new String(tmpPassword);
            }
            pc.clearPassword();
            return login;
        } catch (IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback().toString()
                    + " not available to gather authentication information "
                    + "from the user");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean login() throws LoginException {
        String[] login = retrieveLogin();
        principal = authenticate(login);
        if (principal == null) {
            throw new LoginException("Authentication failed for "+login[0]);
        }
        sharedState.put("javax.security.auth.login.name", principal);
        //sharedState.put("javax.security.auth.login.password", login[1]);
        return false;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (principal != null) {
            subject.getPrincipals().add(principal);
            return true;
        } else {
           return false;
        }
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

}

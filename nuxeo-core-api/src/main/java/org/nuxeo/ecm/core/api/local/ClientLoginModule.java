/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.local;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

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
 */
public class ClientLoginModule implements LoginModule {

    protected static final LoginStack globalInstance = LoginStack.synchronizedStack();

    protected static final ThreadLocal<LoginStack> threadInstance = new ThreadLocal<LoginStack>() {
        @Override
        protected LoginStack initialValue() {
            return new LoginStack();
        }
    };

    public static LoginStack getThreadLocalLogin() {
        return threadInstance.get();
    }

    /**
     * @since 5.7
     */
    public static void clearThreadLocalLogin() {
        threadInstance.remove();
    }

    public static LoginStack.Entry getCurrentLogin() {
        LoginStack.Entry entry = threadInstance.get().peek();
        if (entry == null) {
            entry = globalInstance.peek();
        }
        return entry;
    }

   private Subject subject;
   private CallbackHandler callbackHandler;
   /** Shared state between login modules */
   private Map sharedState;
   /** Flag indicating if the shared password should be used */
   private boolean useFirstPass;
   private String username;
   private char[] password;
   private String principalClass;

   private LoginStack stack; // active login stack

   /**
    * Initialize this LoginModule.
    */
   @Override
   public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        // Check for multi-threaded option
        String mt = (String) options.get("multi-threaded");
        if (mt != null && Boolean.valueOf(mt)) {
            // Turn on the server mode which uses thread local storage for the
            // principal information.
            stack = threadInstance.get();
        } else {
            stack = globalInstance;
        }

        /*
         * Check for password sharing options. Any non-null value for
         * password_stacking sets useFirstPass as this module has no way to
         * validate any shared password.
         */
        String passwordStacking = (String) options.get("password-stacking");
        useFirstPass = passwordStacking != null;
        // the principal class to use when no principal is provided by leading
        // modules
        principalClass = (String) options.get("principal-class");
    }

   /**
    * Authenticates a Subject (phase 1).
    */
   @Override
   public boolean login() throws LoginException {
        // If useFirstPass is true, look for the shared password
        if (useFirstPass) {
            return true;
        }

        /*
         * There is no password sharing or we are the first login module. Get
         * the username and password from the callback handler.
         */
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available "
                    + "to garner authentication information from the user");
        }

        PasswordCallback pc = new PasswordCallback("Password: ", false);
        NameCallback nc = new NameCallback("User name: ", "guest");
        Callback[] callbacks = { nc, pc };
        try {
            callbackHandler.handle(callbacks);
            username = nc.getName();
            char[] tmpPassword = pc.getPassword();
            if (tmpPassword != null) {
                password = new char[tmpPassword.length];
                System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
                pc.clearPassword();
            }
        } catch (IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback().toString()
                    + " not available to garner authentication information "
                    + "from the user");
        }
        return true;
    }

    /**
     * Commits the authentication process (phase 2).
     * <p>
     * This is where the SecurityAssociation information is set. The principal
     * is obtained from: The shared state javax.security.auth.login.name
     * property when useFirstPass is true. If the value is a Principal it is
     * used as is, else a SimplePrincipal using the value.toString() as its name
     * is used. If useFirstPass the username obtained from the callback handler
     * is used to build the SimplePrincipal. Both may be overridden if the
     * resulting authenticated Subject principals set it not empty.
     */
    @Override
    public boolean commit() throws LoginException {
        Set<Principal> principals = subject.getPrincipals();
        Principal p;
        Object credential = password;
        if (useFirstPass) {
            Object user = sharedState.get("javax.security.auth.login.name");
            if (!(user instanceof Principal)) {
                username = user != null ? user.toString() : "";
                p = createPrincipal(username);
            } else {
                p = (Principal) user;
            }
            credential = sharedState.get("javax.security.auth.login.password");
        } else {
            p = createPrincipal(username);
        }

        if (!principals.isEmpty()) {
            p = principals.iterator().next();
        }
        stack.push(p, credential, subject);
        return true;
    }

   /**
    * Aborts the authentication process (phase 2).
    */
   @Override
   public boolean abort() throws LoginException {
        int length = password != null ? password.length : 0;
        for (int n = 0; n < length; n++) {
            password[n] = 0;
        }
        stack.clear();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        stack.pop();
        return true;
    }

    public Principal createPrincipal(final String name) throws LoginException {
        if (principalClass != null) {
            try {
                Class<?> klass = Class.forName(principalClass);
                Constructor<?> ctor = klass.getConstructor(String.class);
                return (Principal) ctor.newInstance(name);
            } catch (Exception e) {
                LoginException ee = new LoginException(
                        "Failed to instantiate principal : " + principalClass);
                ee.initCause(e);
                throw ee;
            }
        }

        return new Principal() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String toString() {
                return name;
            }

            @Override
            public int hashCode() {
                return name == null ? 0 : name.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (obj == this) {
                    return true;
                }
                if (obj instanceof Principal) {
                    String n = ((Principal) obj).getName();
                    if (name == n) {
                        return true;
                    }
                    if (name == null) {
                        return false;
                    }
                    return name.equals(n);
                }
                return false;
            }
        };
    }

}

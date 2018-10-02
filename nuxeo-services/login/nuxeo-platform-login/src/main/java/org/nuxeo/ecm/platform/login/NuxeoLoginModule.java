/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.login;

import java.io.IOException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.acl.Group;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.api.login.RestrictedLoginHelper;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallback;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

public class NuxeoLoginModule extends NuxeoAbstractServerLoginModule {

    private static final Log log = LogFactory.getLog(NuxeoLoginModule.class);

    private UserManager manager;

    private static final Random RANDOM = new SecureRandom();

    private NuxeoPrincipal identity;

    private LoginPluginRegistry loginPluginManager;

    private boolean useUserIdentificationInfoCB = false;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        // explicit cast to match the direct superclass method declaration
        // (JBoss implementation)
        // rather than the newer (jdk1.5) LoginModule (... Map<String,?>...)
        // This is needed to avoid compilation errors when the linker wants to
        // bind
        // with the (interface) LoginModule method (which is abstract of course
        // and cannot be called)
        String useUIICB = (String) options.get("useUserIdentificationInfoCB");
        if (useUIICB != null && useUIICB.equalsIgnoreCase("true")) {
            useUserIdentificationInfoCB = true;
        }

        super.initialize(subject, callbackHandler, sharedState, options);

        manager = Framework.getService(UserManager.class);
        log.debug("NuxeoLoginModule initialized");

        final RuntimeService runtime = Framework.getRuntime();
        loginPluginManager = (LoginPluginRegistry) runtime.getComponent(LoginPluginRegistry.NAME);
    }

    /**
     * Gets the roles the user belongs to.
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {
        log.debug("getRoleSets");
        if (manager == null) {
            // throw new LoginException("UserManager implementation not found");
        }
        String username = identity.getName();
        List<String> roles = identity.getRoles();

        Group roleSet = new GroupImpl("Roles");
        log.debug("Getting roles for user=" + username);
        for (String roleName : roles) {
            Principal role = new PrincipalImpl(roleName);
            log.debug("Found role=" + roleName);
            roleSet.addMember(role);
        }
        Group callerPrincipal = new GroupImpl("CallerPrincipal");
        callerPrincipal.addMember(identity);

        return new Group[] { roleSet, callerPrincipal };
    }

    @SuppressWarnings({ "unchecked" })
    protected NuxeoPrincipal getPrincipal() throws LoginException {
        UserIdentificationInfo userIdent = null;

        // **** init the callbacks
        // Std login/password callbacks
        NameCallback nc = new NameCallback("Username: ", SecurityConstants.ANONYMOUS);
        PasswordCallback pc = new PasswordCallback("Password: ", false);

        // Nuxeo specific cb : handle LoginPlugin initialization
        UserIdentificationInfoCallback uic = new UserIdentificationInfoCallback();

        // JBoss specific cb : handle web=>ejb propagation
        // SecurityAssociationCallback ac = new SecurityAssociationCallback();
        // ObjectCallback oc = new ObjectCallback("UserInfo:");

        // **** handle callbacks
        // We can't check the callback handler class to know what will be
        // supported
        // because the cbh is wrapped by JAAS
        // => just try and swalow exceptions
        // => will be externalised to plugins via EP to avoid JBoss dependency
        boolean cb_handled = false;

        try {
            // only try this cbh when called from the web layer
            if (useUserIdentificationInfoCB) {
                callbackHandler.handle(new Callback[] { uic });
                // First check UserInfo CB return
                userIdent = uic.getUserInfo();
                cb_handled = true;
            }
        } catch (UnsupportedCallbackException e) {
            log.debug("UserIdentificationInfoCallback is not supported");
        } catch (IOException e) {
            log.warn("Error calling callback handler with UserIdentificationInfoCallback : " + e.getMessage());
        }

        Principal principal = null;
        Object credential = null;

        if (!cb_handled) {
            CallbackResult result = loginPluginManager.handleSpecifcCallbacks(callbackHandler);

            if (result != null && result.cb_handled) {
                if (result.userIdent != null && result.userIdent.containsValidIdentity()) {
                    userIdent = result.userIdent;
                    cb_handled = true;
                } else {
                    principal = result.principal;
                    credential = result.credential;
                    if (principal != null) {
                        cb_handled = true;
                    }
                }
            }
        }

        if (!cb_handled) {
            try {
                // Std CBH : will only works for L/P
                callbackHandler.handle(new Callback[] { nc, pc });
                cb_handled = true;
            } catch (UnsupportedCallbackException e) {
                LoginException le = new LoginException("Authentications Failure - " + e.getMessage());
                le.initCause(e);
            } catch (IOException e) {
                LoginException le = new LoginException("Authentications Failure - " + e.getMessage());
                le.initCause(e);
            }
        }

        // Login via the Web Interface : may be using a plugin
        if (userIdent != null && userIdent.containsValidIdentity()) {
            NuxeoPrincipal nxp = validateUserIdentity(userIdent);

            if (nxp != null) {
                sharedState.put("javax.security.auth.login.name", nxp.getName());
                sharedState.put("javax.security.auth.login.password", userIdent);
            }
            return nxp;
        }

        if (LoginComponent.isSystemLogin(principal)) {
            return new SystemPrincipal(principal.getName());
        }
        // if (principal instanceof NuxeoPrincipal) { // a nuxeo principal
        // return validatePrincipal((NuxeoPrincipal) principal);
        // } else
        if (principal != null) { // a non null principal
            String password = null;
            if (credential instanceof char[]) {
                password = new String((char[]) credential);
            } else if (credential != null) {
                password = credential.toString();
            }
            return validateUsernamePassword(principal.getName(), password);
        } else { // we don't have a principal - try the username &
            // password
            String username = nc.getName();
            if (username == null) {
                return null;
            }
            char[] password = pc.getPassword();
            return validateUsernamePassword(username, password != null ? new String(password) : null);
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (manager == null) {
            // throw new LoginException("UserManager implementation not found");
        }

        loginOk = false;

        identity = getPrincipal();
        if (identity == null) { // auth failed
            throw new LoginException("Authentication Failed");
        }

        if (RestrictedLoginHelper.isRestrictedModeActivated()) {
            if (!identity.isAdministrator()) {
                throw new LoginException("Only Administrators can login when restricted mode is activated");
            }
        }

        loginOk = true;
        log.trace("User '" + identity + "' authenticated");

        /*
         * if( getUseFirstPass() == true ) { // Add the username and password to the shared state map // not sure it's
         * needed sharedState.put("javax.security.auth.login.name", identity.getName());
         * sharedState.put("javax.security.auth.login.password", identity.getPassword()); }
         */

        return true;
    }

    @Override
    public Principal getIdentity() {
        return identity;
    }

    @Override
    public Principal createIdentity(String username) throws LoginException {
        log.debug("createIdentity: " + username);
        try {
            NuxeoPrincipal principal;
            if (manager == null) {
                principal = new NuxeoPrincipalImpl(username);
            } else {
                principal = Framework.doPrivileged(() -> manager.getPrincipal(username));
                if (principal == null) {
                    throw new LoginException(String.format("principal %s does not exist", username));
                }
            }

            String principalId = String.valueOf(RANDOM.nextLong());
            principal.setPrincipalId(principalId);
            return principal;
        } catch (NuxeoException | LoginException e) {
            LoginException le = new LoginException("createIdentity failed for user " + username);
            le.initCause(e);
            throw le;
        }
    }

    protected NuxeoPrincipal validateUserIdentity(UserIdentificationInfo userIdent) throws LoginException {
        String loginPluginName = userIdent.getLoginPluginName();
        if (loginPluginName == null) {
            // we don't use a specific plugin
            boolean authenticated;
            try {
                authenticated = manager.checkUsernamePassword(userIdent.getUserName(), userIdent.getPassword());
            } catch (DirectoryException e) {
                throw (LoginException) new LoginException("Unable to validate identity").initCause(e);
            }
            if (authenticated) {
                return (NuxeoPrincipal) createIdentity(userIdent.getUserName());
            } else {
                return null;
            }
        } else {
            LoginPlugin lp = loginPluginManager.getPlugin(loginPluginName);
            if (lp == null) {
                log.error("Can't authenticate against a null loginModul plugin");
                return null;
            }
            // set the parameters and reinit if needed
            LoginPluginDescriptor lpd = loginPluginManager.getPluginDescriptor(loginPluginName);
            if (!lpd.getInitialized()) {
                Map<String, String> existingParams = lp.getParameters();
                if (existingParams == null) {
                    existingParams = new HashMap<String, String>();
                }
                Map<String, String> loginParams = userIdent.getLoginParameters();
                if (loginParams != null) {
                    existingParams.putAll(loginParams);
                }
                boolean init = lp.initLoginModule();
                if (init) {
                    lpd.setInitialized(true);
                } else {
                    log.error("Unable to initialize LoginModulePlugin " + lp.getName());
                    return null;
                }
            }

            String username = lp.validatedUserIdentity(userIdent);
            if (username == null) {
                return null;
            } else {
                return (NuxeoPrincipal) createIdentity(username);
            }
        }
    }

    protected NuxeoPrincipal validateUsernamePassword(String username, String password) throws LoginException {
        if (!manager.checkUsernamePassword(username, password)) {
            return null;
        }
        return (NuxeoPrincipal) createIdentity(username);
    }

}

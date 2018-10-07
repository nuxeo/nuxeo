/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.login.test;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.api.login.RestrictedLoginHelper;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallback;
import org.nuxeo.ecm.platform.login.CallbackResult;
import org.nuxeo.ecm.platform.login.GroupImpl;
import org.nuxeo.ecm.platform.login.LoginPlugin;
import org.nuxeo.ecm.platform.login.LoginPluginDescriptor;
import org.nuxeo.ecm.platform.login.LoginPluginRegistry;
import org.nuxeo.ecm.platform.login.NuxeoAbstractServerLoginModule;
import org.nuxeo.ecm.platform.login.PrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * Dummy login module for TEST PURPOSE ONLY that will not check user/password against usermanager so that any
 * username/password will authenticate a dummy user. The aim is to not use the user manager. Useful for directories
 * tests where it's not possible to use the usermanager in the test due to the cyclic dependency between projects
 *
 * @since 6.0
 */
public class DummyNuxeoLoginModule extends NuxeoAbstractServerLoginModule {

    private static final Log log = LogFactory.getLog(DummyNuxeoLoginModule.class);

    private Random random;

    private NuxeoPrincipal identity;

    private LoginPluginRegistry loginPluginManager;

    private boolean useUserIdentificationInfoCB = false;

    public static final String ADMINISTRATOR_USERNAME = "Administrator";

    public final List<String> groupsToAdd = new ArrayList<String>();

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
        random = new Random(System.currentTimeMillis());

        log.debug("DummyNuxeoLoginModule initialized");

        try {
            final RuntimeService runtime = Framework.getRuntime();
            loginPluginManager = (LoginPluginRegistry) runtime.getComponent(LoginPluginRegistry.NAME);
        } catch (Throwable t) {
            log.error("Unable to load Plugin Registry : " + t.getMessage());
        }
    }

    /**
     * Gets the roles the user belongs to.
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {

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

        try {
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
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            // jboss catches LoginException, so show it at least in the logs
            String msg = "Authentication failed: " + e.getMessage();
            log.error(msg, e);
            throw (LoginException) new LoginException(msg).initCause(e);
        }
    }

    public boolean login() throws LoginException {
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

        return true;
    }

    @Override
    public NuxeoPrincipal getIdentity() {
        return identity;
    }

    @Override
    public NuxeoPrincipal createIdentity(String username) throws LoginException {
        log.debug("createIdentity: " + username);
        try {
            NuxeoPrincipal principal;

            boolean isAdmin = false;
            if (ADMINISTRATOR_USERNAME.equalsIgnoreCase(username)) {
                isAdmin = true;
            }

            // don't retrieve from usernamanger, create a dummy principal
            principal = new NuxeoPrincipalImpl(username, false, isAdmin);

            String principalId = String.valueOf(random.nextLong());
            principal.setPrincipalId(principalId);
            return principal;
        } catch (Exception e) {
            log.error("createIdentity failed", e);
            LoginException le = new LoginException("createIdentity failed for user " + username);
            le.initCause(e);
            throw le;
        }
    }

    protected NuxeoPrincipal validateUserIdentity(UserIdentificationInfo userIdent) throws LoginException {
        String loginPluginName = userIdent.getLoginPluginName();
        if (loginPluginName == null) {
            // we don't use a specific plugin
            // For dummy test, Don't check against usermanager
            return createIdentity(userIdent.getUserName());
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
                return createIdentity(username);
            }
        }
    }

    protected NuxeoPrincipal validateUsernamePassword(String username, String password) throws Exception {
        // Dummy login module will not check against the user manager
        return createIdentity(username);
    }

}

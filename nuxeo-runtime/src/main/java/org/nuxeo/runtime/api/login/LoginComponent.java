/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.RuntimeInstanceIdentifier;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class LoginComponent extends DefaultComponent implements LoginService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.LoginComponent");

    public static final String SYSTEM_LOGIN = "nuxeo-system-login";

    public static final String CLIENT_LOGIN = "nuxeo-client-login";

    public static final String SYSTEM_USERNAME = "system";

    protected static final String instanceId = RuntimeInstanceIdentifier.getId();

    protected static final SystemLoginRestrictionManager systemLoginManager = new SystemLoginRestrictionManager();

    protected static final Log log = LogFactory.getLog(LoginComponent.class);

    private LoginConfiguration config;

    private final Map<String, SecurityDomain> domains = new Hashtable<String, SecurityDomain>();

    private SecurityDomain systemLogin;

    private SecurityDomain clientLogin;

    @Override
    public void activate(ComponentContext context) throws Exception {
        // setup the nuxeo login configuration
        Configuration parentConfig = null;
        try {
            parentConfig = Configuration.getConfiguration();
        } catch (Exception e) {
            // do nothing - this can happen if default configuration provider
            // is not correctly configured
            // for examnple FileConfig fails if no config file was defined
        }
        config = new LoginConfiguration(this, parentConfig);
        Configuration.setConfiguration(config);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        Configuration.setConfiguration(config.getParent());
        config = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("domains")) {
            SecurityDomain domain = (SecurityDomain) contribution;
            addSecurityDomain(domain);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("domains")) {
            SecurityDomain domain = (SecurityDomain) contribution;
            removeSecurityDomain(domain.getName());
        }
    }

    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        SecurityDomain domain = domains.get(name);
        if (domain != null) {
            return domain.getAppConfigurationEntries();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (LoginService.class.isAssignableFrom(adapter)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public SecurityDomain getSecurityDomain(String name) {
        return domains.get(name);
    }

    @Override
    public void addSecurityDomain(SecurityDomain domain) {
        domains.put(domain.getName(), domain);
        if (SYSTEM_LOGIN.equals(domain.getName())) {
            systemLogin = domain;
        } else if (CLIENT_LOGIN.equals(domain.getName())) {
            clientLogin = domain;
        }
    }

    @Override
    public void removeSecurityDomain(String name) {
        domains.remove(name);
        if (SYSTEM_LOGIN.equals(name)) {
            systemLogin = null;
        } else if (CLIENT_LOGIN.equals(name)) {
            clientLogin = null;
        }
    }

    @Override
    public SecurityDomain[] getSecurityDomains() {
        return domains.values().toArray(new SecurityDomain[domains.size()]);
    }

    @Override
    public void removeSecurityDomains() {
        domains.clear();
        systemLogin = null;
        clientLogin = null;
    }

    private LoginContext systemLogin(String username) throws LoginException {
        if (systemLogin != null) {
            Set<Principal> principals = new HashSet<Principal>();
            SystemID sysId = new SystemID(username);
            principals.add(sysId);
            Subject subject = new Subject(false, principals,
                    new HashSet<String>(), new HashSet<String>());
            return systemLogin.login(subject, new DefaultCallbackHandler(
                    SYSTEM_USERNAME, sysId));
        }
        return null;
    }

    @Override
    public LoginContext login() throws LoginException {
        return loginAs(null);
    }

    @Override
    public LoginContext loginAs(final String username) throws LoginException {
        // login as system user is a privileged action
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<LoginContext>() {
                @Override
                public LoginContext run() throws LoginException {
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkPermission(new SystemLoginPermission());
                    }
                    return systemLogin(username);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (LoginException) e.getException();
        }
    }

    @Override
    public LoginContext login(String username, Object credentials)
            throws LoginException {
        if (clientLogin != null) {
            return clientLogin.login(username, credentials);
        }
        return null;
    }

    @Override
    public LoginContext login(CallbackHandler cbHandler) throws LoginException {
        if (clientLogin != null) {
            return clientLogin.login(cbHandler);
        }
        return null;
    }

    @Override
    public boolean isSystemId(Principal principal) {
        return isSystemLogin(principal);
    }

    public static boolean isSystemLogin(Object principal) {
        if (principal != null && principal.getClass() == SystemID.class) {
            if (!systemLoginManager.isRemoteSystemLoginRestricted()) {
                return true;
            } else {
                SystemID sys = (SystemID) principal;
                String sourceInstanceId = sys.getSourceInstanceId();
                if (sourceInstanceId == null) {
                    log.warn("Can not accept a system login without InstanceID of the source : System login is rejected");
                    return false;
                } else {
                    if (sourceInstanceId.equals(instanceId)) {
                        return true;
                    } else {
                        if (systemLoginManager.isRemoveSystemLoginAllowedForInstance(sourceInstanceId)) {
                            if (log.isTraceEnabled()) {
                                log.trace("Remote SystemLogin from instance "
                                        + sourceInstanceId + " accepted");
                            }
                            return true;
                        } else {
                            log.warn("Remote SystemLogin attempt from instance "
                                    + sourceInstanceId + " was denied");
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static class SystemID implements Principal, Serializable {

        private static final long serialVersionUID = 2758247997191809993L;

        private final String userName;

        protected final String sourceInstanceId = instanceId;

        public SystemID() {
            userName = null;
        }

        public SystemID(String origUser) {
            userName = origUser == null ? SYSTEM_USERNAME : origUser;
        }

        @Override
        public String getName() {
            return userName;
        }

        public String getSourceInstanceId() {
            return sourceInstanceId;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Principal) {
                Principal oPal = (Principal) other;
                String oName = oPal.getName();
                if (userName == null && oName != null) {
                    return false;
                } else if (!userName.equals(oName)) {
                    return false;
                }
                if (systemLoginManager.isRemoteSystemLoginRestricted()
                        && (other instanceof LoginComponent.SystemID)) {
                    // compare sourceInstanceId
                    String oSysId = ((LoginComponent.SystemID) other).sourceInstanceId;
                    if (sourceInstanceId == null) {
                        return oSysId == null;
                    } else {
                        return sourceInstanceId.equals(oSysId);
                    }
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (!systemLoginManager.isRemoteSystemLoginRestricted()) {
                return userName == null ? 0 : userName.hashCode();
            } else {
                return userName == null ? 0 : userName.hashCode()
                        + sourceInstanceId.hashCode();
            }
        }

    }

}

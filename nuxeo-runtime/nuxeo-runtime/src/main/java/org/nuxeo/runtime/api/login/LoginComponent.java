/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Objects;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
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

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.LoginComponent");

    public static final String SYSTEM_LOGIN = "nuxeo-system-login";

    public static final String CLIENT_LOGIN = "nuxeo-client-login";

    public static final String SYSTEM_USERNAME = "system";

    protected static final String instanceId = RuntimeInstanceIdentifier.getId();

    protected static final SystemLoginRestrictionManager systemLoginManager = new SystemLoginRestrictionManager();

    protected static final Log log = LogFactory.getLog(LoginComponent.class);

    private final Map<String, SecurityDomain> domains = new Hashtable<String, SecurityDomain>();

    private SecurityDomain systemLogin;

    private SecurityDomain clientLogin;

    @Override
    public void activate(ComponentContext context) {
        LoginConfiguration.INSTANCE.install(new LoginConfiguration.Provider() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return LoginComponent.this.getAppConfigurationEntry(name);
            }

        });
    }

    @Override
    public void deactivate(ComponentContext context) {
        LoginConfiguration.INSTANCE.uninstall();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("domains")) {
            SecurityDomain domain = (SecurityDomain) contribution;
            addSecurityDomain(domain);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
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
            Subject subject = new Subject(false, principals, new HashSet<String>(), new HashSet<String>());
            return systemLogin.login(subject, new CredentialsCallbackHandler(sysId.getName(), sysId));
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
    public LoginContext login(String username, Object credentials) throws LoginException {
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
                                log.trace("Remote SystemLogin from instance " + sourceInstanceId + " accepted");
                            }
                            return true;
                        } else {
                            log.warn("Remote SystemLogin attempt from instance " + sourceInstanceId + " was denied");
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
                if (!Objects.equals(userName, oName)) {
                    return false;
                }
                if (systemLoginManager.isRemoteSystemLoginRestricted() && (other instanceof LoginComponent.SystemID)) {
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
                return userName == null ? 0 : userName.hashCode() + sourceInstanceId.hashCode();
            }
        }

    }

}

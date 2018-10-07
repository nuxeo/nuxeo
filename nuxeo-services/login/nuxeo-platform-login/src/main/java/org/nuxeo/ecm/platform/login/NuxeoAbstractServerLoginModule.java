/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.login;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Abstract implementation of the {@link LoginModule} SPI from {@code javax.security.auth.spi}.
 */
public abstract class NuxeoAbstractServerLoginModule implements LoginModule {

    private static final Log log = LogFactory.getLog(NuxeoAbstractServerLoginModule.class);

    protected Subject subject;

    protected Map sharedState;

    protected Map options;

    protected boolean loginOk;

    /** An optional custom Principal class implementation */
    protected String principalClassName;

    /** the principal to use when a null username and password are seen */
    protected NuxeoPrincipal unauthenticatedIdentity;

    protected CallbackHandler callbackHandler;

    /** Flag indicating if the shared credential should be used */
    protected boolean useFirstPass;

    protected abstract NuxeoPrincipal getIdentity();

    protected abstract Group[] getRoleSets() throws LoginException;

    protected abstract NuxeoPrincipal createIdentity(String username) throws LoginException;

    public boolean abort() throws LoginException {
        log.trace("abort");
        return true;
    }

    public boolean commit() throws LoginException {
        log.trace("commit, loginOk=" + loginOk);
        if (!loginOk) {
            return false;
        }

        Set<Principal> principals = subject.getPrincipals();
        Principal identity = getIdentity();
        principals.add(identity);
        Group[] roleSets = getRoleSets();
        for (Group group : roleSets) {
            String name = group.getName();
            Group subjectGroup = createGroup(name, principals);

            /*
             * if( subjectGroup instanceof NestableGroup ) { SimpleGroup tmp = new SimpleGroup("Roles");
             * subjectGroup.addMember(tmp); subjectGroup = tmp; }
             */

            // Copy the group members to the Subject group
            Enumeration<? extends Principal> members = group.members();
            while (members.hasMoreElements()) {
                Principal role = members.nextElement();
                subjectGroup.addMember(role);
            }
        }
        return true;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        if (log.isTraceEnabled()) {
            log.trace("initialize, instance=@" + System.identityHashCode(this));
        }

        /*
         * Check for password sharing options. Any non-null value for password_stacking sets useFirstPass as this module
         * has no way to validate any shared password.
         */
        String passwordStacking = (String) options.get("password-stacking");
        if (passwordStacking != null && passwordStacking.equalsIgnoreCase("useFirstPass")) {
            useFirstPass = true;
        }

        // Check for a custom Principal implementation
        principalClassName = (String) options.get("principalClass");

        // Check for unauthenticatedIdentity option.
        String name = (String) options.get("unauthenticatedIdentity");
        if (name != null) {
            try {
                unauthenticatedIdentity = createIdentity(name);
                log.trace("Saw unauthenticatedIdentity=" + name);
            } catch (LoginException e) {
                log.warn("Failed to create custom unauthenticatedIdentity", e);
            }
        }
    }

    public boolean logout() throws LoginException {
        log.trace("logout");
        // Remove the user identity
        Principal identity = getIdentity();
        Set<Principal> principals = subject.getPrincipals();
        principals.remove(identity);
        // Remove any added Groups...
        return true;
    }

    /**
     * Finds or creates a Group with the given name. Subclasses should use this method to locate the 'Roles' group or
     * create additional types of groups.
     *
     * @return A named Group from the principals set.
     */
    protected Group createGroup(String name, Set<Principal> principals) {
        Group roles = null;
        for (Principal principal : principals) {
            if (!(principal instanceof Group)) {
                continue;
            }
            Group grp = (Group) principal;
            if (grp.getName().equals(name)) {
                roles = grp;
                break;
            }
        }
        // If we did not find a group, create one
        if (roles == null) {
            roles = new GroupImpl(name);
            principals.add(roles);
        }
        return roles;
    }

}

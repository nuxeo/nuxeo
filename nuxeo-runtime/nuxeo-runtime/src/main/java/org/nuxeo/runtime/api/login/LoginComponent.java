/*
 * (C) Copyright 2006-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.runtime.api.login;

import java.io.Serializable;
import java.security.Principal;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component holding the stack of logged in principals.
 */
public class LoginComponent extends DefaultComponent implements LoginService {

    public static final String SYSTEM_USERNAME = "system";

    /**
     * The thread-local principal stack. The top of the stack (last element) contains the current principal.
     *
     * @since 11.1
     */
    protected static final ThreadLocal<Deque<Principal>> PRINCIPAL_STACK = ThreadLocal.withInitial(
            () -> new LinkedList<>());

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (LoginService.class.isAssignableFrom(adapter)) {
            return (T) this;
        }
        return null;
    }

    protected NuxeoLoginContext systemLogin(String originatingUser) {
        Principal principal = new SystemID(originatingUser);
        NuxeoLoginContext loginContext = NuxeoLoginContext.create(principal);
        loginContext.login();
        return loginContext;
    }

    @Override
    public LoginContext login() {
        return systemLogin(null);
    }

    @Override
    public LoginContext loginAs(String username) {
        return systemLogin(username);
    }

    @Deprecated
    @Override
    public LoginContext login(String username, Object credentials) throws LoginException {
        return Framework.loginUser(username);
    }

    @Override
    public boolean isSystemId(Principal principal) {
        return isSystemLogin(principal);
    }

    public static boolean isSystemLogin(Object principal) {
        return principal instanceof SystemID;
    }

    public static class SystemID implements Principal, Serializable {

        private static final long serialVersionUID = 2758247997191809993L;

        private final String userName;

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

        @Override
        public boolean equals(Object object) {
            if (object instanceof SystemID) {
                SystemID other = (SystemID) object;
                return Objects.equals(userName, other.userName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return userName == null ? 0 : userName.hashCode();
        }

    }

    /**
     * INTERNAL.
     *
     * @since 11.1
     */
    public static Deque<Principal> getPrincipalStack() {
        return PRINCIPAL_STACK.get();
    }

    /**
     * INTERNAL.
     *
     * @since 11.1
     */
    public static void clearPrincipalStack() {
        // removes the whole thread-local, so original stack object is untouched
        PRINCIPAL_STACK.remove();
    }

    /**
     * Pushes the principal to the current principal stack.
     *
     * @param principal the principal
     * @since 11.1
     */
    public static void pushPrincipal(Principal principal) {
        PRINCIPAL_STACK.get().addLast(principal);
    }

    /**
     * Pops the last principal from the current principal stack.
     *
     * @return the last principal, or {@code null} if the stack is empty
     * @since 11.1
     */
    public static Principal popPrincipal() {
        return PRINCIPAL_STACK.get().pollLast();
    }

    /**
     * Returns the last principal from the current principal stack.
     *
     * @return the last principal, or {@code null} if the stack is empty
     * @since 11.1
     */
    public static Principal getCurrentPrincipal() {
        return PRINCIPAL_STACK.get().peekLast();
    }

}

/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.local;

import java.security.Principal;

import javax.security.auth.Subject;

import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * This class is deprecated and now delegates to {@link LoginComponent}.
 *
 * @deprecated since 11.1
 */
@Deprecated
public class LoginStack {

    public void clear() {
        LoginComponent.getPrincipalStack().clear();
    }

    public void push(Principal principal, Object credential, Subject subject) {
        // credential and subject are ignored
        LoginComponent.pushPrincipal(principal);
    }

    public Entry pop() {
        Principal principal = LoginComponent.popPrincipal();
        return principal == null ? null : new Entry(principal, null, null);
    }

    public Entry peek() {
        Principal principal = LoginComponent.getPrincipalStack().peekLast();
        return principal == null ? null : new Entry(principal, null, null);
    }

    public boolean isEmpty() {
        return LoginComponent.getPrincipalStack().isEmpty();
    }

    public int size() {
        return LoginComponent.getPrincipalStack().size();
    }

    public Entry[] toArray() {
        return (Entry[]) LoginComponent.getPrincipalStack()
                                       .stream()
                                       .map(principal -> new Entry(principal, null, null))
                                       .toArray();
    }

    /** @deprecated since 11.1 */
    @Deprecated
    public static class Entry {
        protected final Principal principal;

        protected final Object credential;

        protected final Subject subject;

        public Entry(Principal principal, Object credential, Subject subject) {
            this.principal = principal;
            this.credential = credential;
            this.subject = subject;
        }

        public Principal getPrincipal() {
            return principal;
        }

        /** @deprecated since 11.1, unused */
        @Deprecated
        public Object getCredential() {
            return credential;
        }

        /** @deprecated since 11.1, unused */
        @Deprecated
        public Subject getSubject() {
            return subject;
        }
    }

}

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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.local;

import java.security.Principal;
import java.util.LinkedList;

import javax.security.auth.Subject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LoginStack {

    public static LoginStack synchronizedStack() {
        return new Sync();
    }

    protected final LinkedList<Entry> stack = new LinkedList<>();

    public void clear() {
        stack.clear();
    }

    public void push(Principal principal, Object credential, Subject subject) {
        stack.add(new Entry(principal, credential, subject));
    }

    public Entry pop() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.removeLast();
    }

    public Entry peek() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getLast();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public Entry get(int index) {
        return stack.get(index);
    }

    public Entry remove(int index) {
        return stack.remove(index);
    }

    public Entry[] toArray() {
        return stack.toArray(new Entry[stack.size()]);
    }

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

        public Object getCredential() {
            return credential;
        }

        public Subject getSubject() {
            return subject;
        }
    }

    public static class Sync extends LoginStack {

        @Override
        public synchronized void clear() {
            stack.clear();
        }

        @Override
        public synchronized void push(Principal principal, Object credential, Subject subject) {
            stack.add(new Entry(principal, credential, subject));
        }

        @Override
        public synchronized Entry pop() {
            if (stack.isEmpty()) {
                return null;
            }
            return stack.removeLast();
        }

        @Override
        public synchronized Entry peek() {
            if (stack.isEmpty()) {
                return null;
            }
            return stack.getLast();
        }

        @Override
        public synchronized boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public synchronized int size() {
            return stack.size();
        }

        @Override
        public synchronized Entry get(int index) {
            return stack.get(index);
        }

        @Override
        public synchronized Entry remove(int index) {
            return stack.remove(index);
        }

        @Override
        public synchronized Entry[] toArray() {
            return stack.toArray(new Entry[stack.size()]);
        }

    }

}

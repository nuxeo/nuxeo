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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MessagesBundle extends ResourceBundle {

    protected final Map<String, String> messages;

    public MessagesBundle(ResourceBundle parent, Map<String, String> messages) {
        this.parent = parent;
        this.messages = messages == null ? new HashMap<>() : messages;
    }

    @Override
    public Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return messages.get(key);
    }

    /**
     * Implementation of ResourceBundle.getKeys.
     */
    @Override
    public Enumeration<String> getKeys() {
        ResourceBundle parent = this.parent;
        return new Keys(messages.keySet().iterator(), (parent != null) ? parent.getKeys() : null);
    }

    static class Keys implements Enumeration<String> {
        protected final Iterator<String> it;

        protected final Enumeration<String> parent;

        Keys(Iterator<String> it, Enumeration<String> parent) {
            this.it = it;
            this.parent = parent;
        }

        @Override
        public boolean hasMoreElements() {
            if (it.hasNext()) {
                return true;
            }
            return parent.hasMoreElements();
        }

        @Override
        public String nextElement() {
            if (it.hasNext()) {
                return it.next();
            }
            return parent.nextElement();
        }
    }

}

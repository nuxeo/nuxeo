/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class MessagesBundle extends ResourceBundle {

    protected final Map<String,String> messages;

    public MessagesBundle(ResourceBundle parent, Map<String,String> messages) {
        this.parent = parent;
        this.messages = messages == null ? new HashMap<String, String>() : messages;
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
        return new Keys(messages.keySet().iterator(),
                (parent != null) ? parent.getKeys() : null);
    }

    static class Keys implements Enumeration<String> {
        protected final Iterator<String> it;
        protected final Enumeration<String> parent;

        Keys(Iterator<String> it, Enumeration<String> parent) {
            this.it = it;
            this.parent = parent;
        }

        public boolean hasMoreElements() {
            if (it.hasNext()) {
                return true;
            }
            return parent.hasMoreElements();
        }

        public String nextElement() {
            if (it.hasNext()) {
                return it.next();
            }
            return parent.nextElement();
        }
    }

}

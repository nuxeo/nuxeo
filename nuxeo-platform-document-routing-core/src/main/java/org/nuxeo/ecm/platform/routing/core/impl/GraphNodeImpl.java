/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.6
 */
public class GraphNodeImpl implements GraphNode {

    public static final String PROP_START = "rnode:start";

    protected final DocumentModel doc;

    public GraphNodeImpl(DocumentModel doc) {
        this.doc = doc;
    }

    protected Object getProperty(String propertyName) {
        try {
            return doc.getPropertyValue(propertyName);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected boolean getBoolean(String propertyName) {
        return Boolean.TRUE.equals(getProperty(propertyName));
    }

    @Override
    public boolean isStart() {
        return getBoolean(PROP_START);
    }

}

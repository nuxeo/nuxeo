/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Descriptor for a {@link BlobDispatcher} and its configuration.
 *
 * @since 7.3
 */
@XObject(value = "blobdispatcher")
public class BlobDispatcherDescriptor {

    public BlobDispatcherDescriptor() {
    }

    @XNode("class")
    public Class<? extends BlobDispatcher> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties = new HashMap<String, String>();

    private BlobDispatcher instance;

    public synchronized BlobDispatcher getBlobDispatcher() {
        if (instance == null) {
            if (klass == null) {
                throw new NuxeoException("Missing class in blob dispatcher descriptor");
            }
            BlobDispatcher blobDispatcher;
            try {
                blobDispatcher = klass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
            blobDispatcher.initialize(properties);
            instance = blobDispatcher;
        }
        return instance;
    }

}

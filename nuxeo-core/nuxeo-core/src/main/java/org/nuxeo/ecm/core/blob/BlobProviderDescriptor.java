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

/**
 * Descriptor for a {@link BlobProvider}.
 */
@XObject(value = "blobprovider")
public class BlobProviderDescriptor {

    @XNode("@name")
    public String name = "";

    @XNode("class")
    public Class<?> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties = new HashMap<String, String>();

    public BlobProviderDescriptor() {
    }

    /** Copy constructor. */
    public BlobProviderDescriptor(BlobProviderDescriptor other) {
        name = other.name;
        klass = other.klass;
        properties = new HashMap<String, String>(other.properties);
    }

    public void merge(BlobProviderDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.klass != null) {
            klass = other.klass;
        }
        properties.putAll(other.properties);
    }

}

/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.externalblob;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for registration of an external blob adapter.
 *
 * @see ExternalBlobAdapter
 *
 * @author Anahide Tchertchian
 */
@XObject("adapter")
public class ExternalBlobAdapterDescriptor {

    @XNode("@prefix")
    protected String prefix;

    @XNode("@class")
    protected Class<? extends ExternalBlobAdapter> adapter;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<String, String>();

    public Class<? extends ExternalBlobAdapter> getAdapterClass() {
        return adapter;
    }

    public String getPrefix() {
        return prefix;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public ExternalBlobAdapter getAdapter() throws InstantiationException,
            IllegalAccessException {
        return adapter.newInstance();
    }

}

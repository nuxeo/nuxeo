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
 * @author Anahide Tchertchian
 */
@XObject("adapter")
public class ExternalBlobAdapterDescriptor {

    @XNode("@prefix")
    protected String prefix;

    @XNode("@class")
    protected Class<? extends ExternalBlobAdapter> adapter;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<>();

    public Class<? extends ExternalBlobAdapter> getAdapterClass() {
        return adapter;
    }

    public String getPrefix() {
        return prefix;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public ExternalBlobAdapter getAdapter() {
        try {
            return adapter.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}

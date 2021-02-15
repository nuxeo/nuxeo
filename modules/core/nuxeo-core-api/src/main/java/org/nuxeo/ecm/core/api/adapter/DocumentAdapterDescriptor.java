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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.adapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("adapter")
@XRegistry
@XRegistryId("@class")
public class DocumentAdapterDescriptor {

    private static final Logger log = LogManager.getLogger(DocumentAdapterDescriptor.class);

    @XNode("@facet")
    private String facet;

    @XNode("@class")
    private Class<?> itf;

    private DocumentAdapterFactory factory;

    public String getFacet() {
        return facet;
    }

    public Class<?> getInterface() {
        return itf;
    }

    /**
     * Used by XMap to set the factory.
     */
    @XNode("@factory")
    void setFactory(Class<DocumentAdapterFactory> factoryClass) throws ReflectiveOperationException {
        try {
            factory = factoryClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            log.error("ERROR instantiating document adapter factory class!");
            throw e;
        }
    }

    public DocumentAdapterFactory getFactory() {
        return factory;
    }

    @Override
    public String toString() {
        return facet + ": " + itf;
    }

}

/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: ResourceAdapterDescriptor.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;

/**
 * Adapter to transform a {@link DocumentModel} into a {@link QNameResource} and reverse. This is done using criteria
 * like resource namespace and document type.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("adapter")
@XRegistry
public class ResourceAdapterDescriptor {

    @XNode("@namespace")
    @XRegistryId
    protected String namespace;

    @XNode("@class")
    protected Class<? extends ResourceAdapter> adapterClass;

    public String getNamespace() {
        return namespace;
    }

    /** @since 11.5 */
    public Class<? extends ResourceAdapter> getAdapterClass() {
        return adapterClass;
    }

}

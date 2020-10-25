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

package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * XMap descriptor for contributed factories.
 *
 * @author tiry
 */
@XObject("blobHolderFactory")
public class BlobHolderFactoryDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@docType")
    protected String docType;

    @XNode("@facet")
    protected String facet;

    @XNode("@class")
    private Class<BlobHolderFactory> adapterClass;

    public String getName() {
        return name;
    }

    public String getDocType() {
        return docType;
    }

    public String getFacet() {
        return facet;
    }

    public BlobHolderFactory getFactory() {
        try {
            return (BlobHolderFactory) adapterClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

}

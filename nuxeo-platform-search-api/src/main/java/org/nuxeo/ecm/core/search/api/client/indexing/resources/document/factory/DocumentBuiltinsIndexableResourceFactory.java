/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.factory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedDataImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourceImpl;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.AbstractIndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentBuiltinsIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;

/**
 * Document builtins indexable resource factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentBuiltinsIndexableResourceFactory extends
        AbstractIndexableResourceFactory {

    private static final long serialVersionUID = 1L;

    public ResolvedResource resolveResourceFor(IndexableResource resource)
            throws IndexingException {

        IndexableResourceConf conf = resource.getConfiguration();

        if (!conf.getType().equals(ResourceType.DOC_BUILTINS)) {
            throw new IndexingException("Factory expects a resource of type="
                    + ResourceType.DOC_BUILTINS);
        }

        List<ResolvedData> rdata = new ArrayList<ResolvedData>();
        for (IndexableResourceDataConf dataConf : conf.getIndexableFields().values()) {

            String name = dataConf.getIndexingName();
            String prefix = conf.getPrefix();
            String prefixedName = prefix + ':' + name;

            Object value = resource.getValueFor(prefixedName);

            rdata.add(new ResolvedDataImpl(prefixedName,
                    dataConf.getIndexingAnalyzer(), dataConf.getIndexingType(),
                    value, dataConf.isStored(), dataConf.isIndexed(),
                    dataConf.isMultiple(), dataConf.isSortable(),
                    null, dataConf.getTermVector(), dataConf.isBinary(), null));
        }

        return new ResolvedResourceImpl(
                ((DocumentIndexableResource) resource).getDocUUID(),
                resource, rdata);
    }

    public IndexableResource createEmptyIndexableResource() {
        return new DocumentBuiltinsIndexableResourceImpl();
    }

    public IndexableResource createIndexableResourceFrom(
            Serializable targetResource, IndexableResourceConf conf, String sid) {
        return new DocumentBuiltinsIndexableResourceImpl(
                (DocumentModel) targetResource, conf, sid);
    }

    public ResolvedResources resolveResourcesFor(IndexableResource resource) {
        // TODO Auto-generated method stub
        return null;
    }
}

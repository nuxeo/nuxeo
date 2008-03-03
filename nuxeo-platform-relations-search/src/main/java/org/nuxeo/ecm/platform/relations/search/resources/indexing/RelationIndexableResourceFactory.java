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
 * $Id: RelationIndexableResourceFactory.java 21963 2007-07-04 15:42:14Z janguenot $
 */

package org.nuxeo.ecm.platform.relations.search.resources.indexing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedDataImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourceImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourcesImpl;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.AbstractIndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.security.IndexingSecurityConstants;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.BuiltinRelationsFields;

/**
 * Audit indexable resource factory.
 *
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 *
 */
public class RelationIndexableResourceFactory extends
        AbstractIndexableResourceFactory implements IndexableResourceFactory {

    private static final long serialVersionUID = -2103767785989950281L;

    private static final Log log = LogFactory.getLog(RelationIndexableResourceFactory.class);

    public IndexableResource createEmptyIndexableResource() {
        return new RelationIndexableResourceImpl();
    }

    /**
     * relations are their own ids, mostly
     */
    public IndexableResource createIndexableResourceFrom(
            Serializable targetResourceId, IndexableResourceConf conf, String sid) throws IndexingException {
        if (targetResourceId instanceof Statement) {
            return new RelationIndexableResourceImpl(
                    (Statement) targetResourceId, conf);
        }

        throw new IndexingException("Unrecognized target resource");
    }

    /**
     * Do the resolution.
     * <p> As far as field are concerned, builtin fields {@link RelationBuiltinFields}
     * can be overriden by the resource conf. Just specify explicitely in configuration.
     * Otherwise the default from the class applies
     * </p>
     */
    public ResolvedResource resolveResourceFor(IndexableResource resource)
            throws IndexingException {
        String id = resource.computeId();
        List<ResolvedData> data = new ArrayList<ResolvedData>();
        IndexableResourceConf conf = resource.getConfiguration();

        Map<String, IndexableResourceDataConf> confFields = null;
        if (conf != null) { // unit tests
            confFields = conf.getIndexableFields();
        }
        Map<String, IndexableResourceDataConf> builtinFields
            = BuiltinRelationsFields.getIndexableFields();

        HashSet<String> fieldNames = new HashSet<String>();
        if (confFields != null) {
            fieldNames.addAll(confFields.keySet());
        }
        fieldNames.addAll(builtinFields.keySet());
        if (conf != null) {
            fieldNames.removeAll(conf.getExcludedFields());
        }

        // builtins can be overriden by conf. Hence iterate over
        // the union of sets, and retrieve in right order
        IndexableResourceDataConf field;
        for (String fieldName : fieldNames) {
            field = null;
            if (confFields != null) {
                field = confFields.get(fieldName);
            }
            if (field == null) {
                field = builtinFields.get(fieldName);
            }
            if (field == null) {
                log.warn("Could not resolve indexing field " + fieldName);
            }

            data.add(new ResolvedDataImpl(
                    field.getIndexingName(),
                    field.getIndexingAnalyzer(),
                    field.getIndexingType(),
                    resource.getValueFor(fieldName),
                    field.isStored(),
                    field.isIndexed(),
                    field.isMultiple(),
                    field.isSortable(),
                    field.getTermVector(),
                    field.isBinary(),
                    field.getProperties()
                    ));
        }
        return new ResolvedResourceImpl(id, resource, data);
    }

    @SuppressWarnings("unchecked")
    public ResolvedResources resolveResourcesFor(IndexableResource resource)
            throws IndexingException {
        List<ResolvedResource> resolvedResources =
            Arrays.asList(resolveResourceFor(resource));
        return new ResolvedResourcesImpl(
                resource.computeId(), resolvedResources,
                Collections.EMPTY_LIST,
                IndexingSecurityConstants.getOpenAcp());
    }

}

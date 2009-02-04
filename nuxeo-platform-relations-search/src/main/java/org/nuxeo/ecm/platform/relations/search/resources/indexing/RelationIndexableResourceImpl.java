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
 *     racinet
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.search.resources.indexing;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.AbstractNXCoreIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.security.IndexingSecurityConstants;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.search.delegate.RelationSearchBusinessDelegate;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.BuiltinRelationsFields;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.RelationIndexableResource;

/**
 * Relations indexable resource implementation.
 *
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 */
public class RelationIndexableResourceImpl extends
        AbstractNXCoreIndexableResource implements RelationIndexableResource {

    private static final long serialVersionUID = -5490252027860954273L;

    private static final Log log = LogFactory.getLog(RelationIndexableResourceImpl.class);

    private Statement statement;

    private Graph graph;

    private RelationManager relationManager;

    public RelationIndexableResourceImpl() {
    }

    private void initGraph() {
        try {
            graph = RelationSearchBusinessDelegate.getRelationManager().getGraphByName(
                    name);
        } catch (ClientException e) {
            log.error("Couldn't find graph with name " + name);
        }
    }

    public RelationIndexableResourceImpl(Statement statement) {
        this.statement = statement;
        initManager();
        // initGraph();
    }

    public RelationIndexableResourceImpl(Statement statement,
            IndexableResourceConf configuration) {
        this.statement = statement;
        this.configuration = configuration;
        initManager();
        // initGraph();
    }

    private void initManager() {
        relationManager = RelationSearchBusinessDelegate.getRelationManager();
    }

    public Serializable getValueFor(String indexableDataName)
            throws IndexingException {
        login();

        if (indexableDataName.equals(BuiltinRelationsFields.SUBJECT_URI)) {
            Subject s = statement.getSubject();
            if (s.isResource()) {
                return ((Resource) s).getUri();
            } else {
                return ""; // null has special meaning to search service
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.SUBJECT_URI_LOCAL)) {
            Subject s = statement.getSubject();
            if (s.isQNameResource()) {
                return ((QNameResource) s).getLocalName();
            } else {
                return ""; // null has special meaning to search service
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.SUBJECT_URI_NAMESPACE)) {
            Subject s = statement.getSubject();
            if (s.isQNameResource()) {
                return ((QNameResource) s).getNamespace();
            } else {
                return ""; // null has special meaning to search service
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.OBJECT_URI)) {
            Node o = statement.getObject();
            if (o.isResource()) {
                return ((Resource) o).getUri();
            } else {
                return ""; // see above
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.OBJECT_URI_LOCAL)) {
            Node o = statement.getObject();
            if (o.isQNameResource()) {
                return ((QNameResource) o).getLocalName();
            } else {
                return ""; // null has special meaning to search service
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.OBJECT_URI_NAMESPACE)) {
            Node o = statement.getObject();
            if (o.isQNameResource()) {
                return ((QNameResource) o).getNamespace();
            } else {
                return ""; // null has special meaning to search service
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.PREDICATE_URI)) {
            return statement.getPredicate().getUri();
        }
        if (indexableDataName.equals(BuiltinRelationsFields.OBJECT_URI_LOCAL)) {
            Node o = statement.getObject();
            if (o.isQNameResource()) {
                return ((QNameResource) o).getLocalName();
            } else {
                return ""; // null has special meaning to search service
            }
        }

        if (indexableDataName.equals(BuiltinRelationsFields.SUBJECT)) {
            // TODO: literals and blank
            Subject s = statement.getSubject();
            if (!s.isQNameResource()) {
                return "";
            }
            return getIndexingRepresentation((QNameResource) s);
        }

        if (indexableDataName.equals(BuiltinRelationsFields.OBJECT)) {
            // TODO: literals and blank
            Node o = statement.getObject();
            if (!o.isQNameResource()) {
                return "";
            }
            return getIndexingRepresentation((QNameResource) o);
        }

        // fallback to properties
        // TODO crawler on big map is definitely not optimal
        if (graph == null) {
            // return null;
        }

        Map<Resource, Node[]> properties = statement.getProperties();
        for (Resource predicate : properties.keySet()) {
            if (!predicate.isQNameResource()) {
                continue;
            }
            QNameResource qnr = (QNameResource) predicate;
            Object resourceRepr = null;
            try {
                resourceRepr = relationManager.getResourceRepresentation(
                        qnr.getNamespace(), qnr);
            } catch (ClientException e) {
                log.error(e);
            }
            if (indexableDataName.equals(resourceRepr)) {
                return extractPropertyValue(
                        getConfiguration().getIndexableFields().get(
                                indexableDataName), properties.get(predicate));
            }
        }

        logout();

        return null;
    }

    private static Serializable extractPropertyValue(IndexableResourceDataConf conf,
            Node[] nodes) {
        // TODO handle lists and type according to conf
        if (nodes.length == 0) {
            return null;
        }
        Node node = nodes[0];
        if (!node.isLiteral()) {
            return null; // TODO
        }

        String sValue = ((Literal) node).getValue();
        if (conf == null) {
            return sValue;
        }

        String type = conf.getIndexingType();
        if (type != null) {
            type = type.toLowerCase();
        }

        // try and convert
        if ("int".equals(type)) {
            return new Integer(sValue);
        }

        if ("date".equals(type)) {
            // Search Service expects Calendar, in turn because that's
            // what NXCore feeds him
            return RelationDate.getCalendar((Literal) node);
        }

        return sValue;
    }

    private Serializable getIndexingRepresentation(QNameResource s)
            throws IndexingException {
        try {
            Object repr = relationManager.getResourceRepresentation(
                    s.getNamespace(), s);
            if (repr instanceof DocumentModel) {
                DocumentModel docModel = (DocumentModel) repr;
                return docModel.getRepositoryName() + ":" + docModel.getId();
            }
            return (Serializable) repr;
        } catch (ClientException e) {
            throw new IndexingException(e);
        }
    }

    // For unit tests
    public Statement getStatement() {
        return statement;
    }

    public String computeId() {
        return String.format("%d", statement.hashCode());
    }

    @Override
    public ACP computeAcp() {
        return IndexingSecurityConstants.getOpenAcp();
    }

}

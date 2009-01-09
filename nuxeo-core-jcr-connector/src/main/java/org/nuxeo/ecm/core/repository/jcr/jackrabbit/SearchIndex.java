/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Eugen Ionica
 */

package org.nuxeo.ecm.core.repository.jcr.jackrabbit;

import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.NamePathResolverImpl;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SingletonTokenStream;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.nuxeo.ecm.core.repository.jcr.NodeConstants;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Bogdan Stefanescu
 * @author Eugen Ionica
 * @author Florent Guillaume
 */
public class SearchIndex extends
        org.apache.jackrabbit.core.query.lucene.SearchIndex {

    private static SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);

    /**
     * {@inheritDoc}
     * <p>
     * For Nuxeo, also:
     * <ul>
     * <li>dereferences proxies,</li>
     * <li>indexes facets and parent id.</li>
     * </ul>
     */
    @Override
    protected Document createDocument(NodeState node,
            NamespaceMappings nsMappings, IndexFormatVersion indexFormatVersion)
            throws RepositoryException {

        Document doc = null;
        NamePathResolver resolver = NamePathResolverImpl.create(nsMappings);
        if (node.getNodeTypeName().equals(
                NodeConstants.ECM_NT_DOCUMENT_PROXY.qname)) {
            doc = createProxyDocument(node, nsMappings, indexFormatVersion,
                    resolver);
        } else {
            doc = super.createDocument(node, nsMappings, indexFormatVersion);
        }
        if (node.getNodeTypeName().getNamespaceURI().equals(
                NodeConstants.NS_ECM_DOCS_URI)) {
            addFacets(resolver, indexFormatVersion, node, doc);
            addParent(resolver, indexFormatVersion, node, doc);
        }
        return doc;
    }

    // copied from NodeIndexer.createFieldWithoutNorms
    private Field createStringField(String propName, String string,
            IndexFormatVersion indexFormatVersion) {
        Field field;
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            field = new Field(FieldNames.PROPERTIES, new SingletonTokenStream(
                    FieldNames.createNamedValue(propName, string),
                    PropertyType.STRING));
            field.setOmitNorms(true);
        } else {
            field = new Field(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(propName, string),
                    Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO);
        }
        return field;
    }

    private Set<String> getFacets(NodeState node) {
        Name name = node.getNodeTypeName();
        String typeName = name.getLocalName();
        DocumentType dt = schemaManager.getDocumentType(typeName);
        return dt == null ? null : dt.getFacets();
    }

    private void addFacets(NamePathResolver resolver,
            IndexFormatVersion indexFormatVersion, NodeState node, Document doc) {
        Name name = NodeConstants.ECM_MIXIN_TYPE.qname;
        String propName = name.getLocalName();
        try {
            propName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }

        // add property to the _PROPERTIES_SET for searching
        // beginning with V2
        int version = indexFormatVersion.getVersion();
        if (version >= IndexFormatVersion.V2.getVersion()) {
            doc.add(new Field(FieldNames.PROPERTIES_SET, propName,
                    Field.Store.NO, Field.Index.NO_NORMS));
        }

        Set<String> facets = getFacets(node);
        if (facets != null && !facets.isEmpty()) {
            for (String facet : facets) {
                Field field = createStringField(propName, facet,
                        indexFormatVersion);
                doc.add(field);
                // TODO addLength for V3
            }
            if (facets.size() > 1) {
                doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO,
                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            }
        }
    }

    private void addParent(NamePathResolver resolver,
            IndexFormatVersion indexFormatVersion, NodeState node, Document doc)
            throws RepositoryException {
        String parentId;
        try {
            ItemStateManager stateMgr = getContext().getItemStateManager();
            NodeId pid = node.getParentId();
            if (pid == null) {
                return;
            }
            NodeState parent = (NodeState) stateMgr.getItemState(pid);
            pid = parent.getParentId();
            if (pid == null) {
                return;
            }
            parentId = pid.toString();
        } catch (ItemStateException e) {
            String msg = "Error while indexing node: " + node.getNodeId() +
                    " of type: " + node.getNodeTypeName();
            throw new RepositoryException(msg, e);
        }

        Name name = NodeConstants.ECM_PARENT_ID.qname;
        String propName = name.getLocalName();
        try {
            propName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }

        Field field = createStringField(propName, parentId, indexFormatVersion);
        doc.add(field);
        // TODO addLength for V3
    }

    private Document createProxyDocument(NodeState node,
            NamespaceMappings nsMappings,
            IndexFormatVersion indexFormatVersion, NamePathResolver resolver)
            throws RepositoryException {
        PropertyId id = new PropertyId(node.getNodeId(),
                NodeConstants.ECM_REF_FROZEN_NODE.qname);
        ItemStateManager stateMgr = getContext().getItemStateManager();

        // find the version node
        NodeState versionNode;
        try {
            PropertyState ps = (PropertyState) stateMgr.getItemState(id);
            InternalValue internalValue = ps.getValues()[0];
            versionNode = (NodeState) stateMgr.getItemState(new NodeId(
                    internalValue.getUUID()));
        } catch (ItemStateException e) {
            throw new RepositoryException("No item state: " + id, e);
        }

        // index the version node
        Document doc = super.createDocument(versionNode, nsMappings,
                indexFormatVersion);

        // replace UUID and parent
        doc.removeField(FieldNames.UUID);
        doc.removeField(FieldNames.PARENT);
        doc.add(new Field(FieldNames.UUID, node.getNodeId().toString(),
                Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));
        doc.add(new Field(FieldNames.PARENT, node.getParentId().toString(),
                Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));

        // TODO replace label too?
        try {
            doc.removeField(FieldNames.LABEL);
            NodeState parent = (NodeState) stateMgr.getItemState(node.getParentId());
            ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
            if (child == null) {
                // this can only happen when jackrabbit
                // is running in a cluster.
                throw new RepositoryException("Missing child node entry " +
                        "for node with id: " + node.getNodeId());
            }
            String name = resolver.getJCRName(child.getName());
            doc.add(new Field(FieldNames.LABEL, name, Field.Store.NO,
                    Field.Index.NO_NORMS, Field.TermVector.NO));
        } catch (ItemStateException e) {
            e.printStackTrace();
        }

        return doc;
    }

}

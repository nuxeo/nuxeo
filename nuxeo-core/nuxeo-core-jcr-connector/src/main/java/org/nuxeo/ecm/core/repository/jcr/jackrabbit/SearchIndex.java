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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
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

    /**
     * Name of the Immutable facet, added by {@code DocumentModelFactory} when
     * instantiating a proxy or a version.
     */
    public static final String FACET_IMMUTABLE = "Immutable";

    private static SchemaManager schemaManager;

    public SearchIndex() {
        super();
        schemaManager = Framework.getLocalService(SchemaManager.class);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For Nuxeo, also:
     * <ul>
     * <li>dereferences proxies,</li>
     * <li>indexes correctly uuid and parent id on proxies and versions,</li>
     * <li>indexes facets.</li>
     * </ul>
     */
    @Override
    protected Document createDocument(NodeState node,
            NamespaceMappings nsMappings, IndexFormatVersion indexFormatVersion)
            throws RepositoryException {

        NamePathResolver resolver = NamePathResolverImpl.create(nsMappings);

        Name nodeTypeName = node.getNodeTypeName();
        boolean isDoc = nodeTypeName.getNamespaceURI().equals(
                NodeConstants.NS_ECM_DOCS_URI); // nuxeo doc or version
        boolean isProxy = nodeTypeName.equals(NodeConstants.ECM_NT_DOCUMENT_PROXY.qname);

        NodeState dataNode = isProxy ? getProxyDataNode(node) : node;
        Document doc = super.createDocument(dataNode, nsMappings,
                indexFormatVersion);
        if (isProxy) {
            fixupProxy(doc, node, dataNode, resolver, nsMappings,
                    indexFormatVersion);
        }

        if (isDoc || isProxy) {

            // check if it's a version
            boolean isVersion;
            String label = "";
            if (isProxy) {
                isVersion = false;
            } else {
                try {
                    ItemStateManager itemStateManager = getContext().getItemStateManager();
                    NodeState parent = (NodeState) itemStateManager.getItemState(node.getParentId());
                    Name nt = parent.getNodeTypeName();
                    isVersion = nt.equals(NodeConstants.ECM_VERSION.qname);
                    if (isVersion) {
                        PropertyId labelProp = new PropertyId(
                                parent.getNodeId(),
                                NodeConstants.ECM_VERSION_LABEL.qname);

                        PropertyState ps = (PropertyState) itemStateManager.getItemState(labelProp);
                        InternalValue value = ps.getValues()[0];
                        label = value.getString();
                    }
                } catch (ItemStateException e) {
                    throw new RepositoryException("No item state: " +
                            node.getParentId(), e);
                }
            }

            // ecm:parentId
            String parentId;
            if (isVersion) {
                parentId = getParentId(node); // XXX ?
            } else {
                parentId = getParentId(node);
            }
            addParent(doc, parentId, resolver, indexFormatVersion);

            // ecm:mixinType
            Set<String> facets = getFacets(dataNode);
            if (isProxy || isVersion) {
                facets = new HashSet<String>(facets);
                facets.add(FACET_IMMUTABLE);
            }
            addFacets(doc, facets, resolver, indexFormatVersion);

            // ecm:label added on frozen version too
            if (isVersion) {
                addVersionLabel(doc, label, resolver, indexFormatVersion);
            }
        }

        // log.warn("Indexing doc: " + doc);
        return doc;
    }

    /**
     * Gets the version node from a proxy.
     */
    private NodeState getProxyDataNode(NodeState proxyNode)
            throws RepositoryException {
        PropertyId versionId = new PropertyId(proxyNode.getNodeId(),
                NodeConstants.ECM_REF_FROZEN_NODE.qname);
        ItemStateManager stateMgr = getContext().getItemStateManager();

        try {
            PropertyState ps = (PropertyState) stateMgr.getItemState(versionId);
            InternalValue value = ps.getValues()[0];
            return (NodeState) stateMgr.getItemState(new NodeId(value.getUUID()));
        } catch (ItemStateException e) {
            throw new RepositoryException("No item state: " + versionId, e);
        }
    }

    // see NodeIndexer.createDoc
    private void fixupProxy(Document doc, NodeState node, NodeState dataNode,
            NamePathResolver resolver, NamespaceMappings nsMappings,
            IndexFormatVersion indexFormatVersion) throws RepositoryException {

        ItemStateManager itemStateManager = getContext().getItemStateManager();

        // primaryType is that of the target
        // add ecmnt:documentProxy to MIXINTYPES so that we can find proxies
        addPropertyName(doc, NameConstants.JCR_MIXINTYPES, resolver,
                indexFormatVersion);
        doc.add(createNameField(NameConstants.JCR_MIXINTYPES,
                node.getNodeTypeName(), resolver, indexFormatVersion));

        // UUID
        String uuid = node.getNodeId().toString();
        doc.removeField(FieldNames.UUID);
        doc.add(new Field(FieldNames.UUID, uuid, Field.Store.YES,
                Field.Index.NO_NORMS, Field.TermVector.NO));
        changePropertyValue(doc, NameConstants.JCR_UUID, uuid, resolver);

        // PARENT
        doc.removeField(FieldNames.PARENT);
        doc.add(new Field(FieldNames.PARENT, node.getParentId().toString(),
                Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));

        // LABEL
        NodeState parent;
        try {
            parent = (NodeState) itemStateManager.getItemState(node.getParentId());
        } catch (ItemStateException e) {
            throw new RepositoryException("No item state: " +
                    node.getParentId(), e);
        }
        ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
        if (child == null) {
            // this can only happen when jackrabbit
            // is running in a cluster.
            throw new RepositoryException(
                    "Missing child node entry for node with id: " +
                            node.getNodeId());
        }
        addNodeName(doc, child.getName(), nsMappings, indexFormatVersion);

    }

    /**
     * Deep surgery inside Lucene to change a single property.
     * <p>
     * We have to check the value and this can't be done without destroying a
     * stream so we have to recreate it after checking.
     */
    private void changePropertyValue(Document doc, Name name, String value,
            NamePathResolver resolver) {
        String qualifiedName;
        try {
            qualifiedName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
            qualifiedName = "";
        }
        String prefix = FieldNames.createNamedValue(qualifiedName, "");
        Iterator<?> it = doc.getFields().iterator();
        while (it.hasNext()) {
            Fieldable fieldable = (Fieldable) it.next();
            if (!fieldable.name().equals(FieldNames.PROPERTIES)) {
                continue;
            }
            Field field = (Field) fieldable;
            TokenStream tokenStream = field.tokenStreamValue();
            if (!(tokenStream instanceof SingletonTokenStream)) {
                continue;
            }
            Token token = ((SingletonTokenStream) tokenStream).next();
            // next() destroyed the stream...
            byte type = token.getPayload().getData()[0];
            String v = new String(token.termBuffer());
            if (v.startsWith(prefix)) {
                // fixup the value
                v = prefix + value;
            }
            // set back a new token stream
            field.setValue(new SingletonTokenStream(v, type));
        }
    }

    private void addNodeName(Document doc, Name name,
            NamespaceMappings nsMappings, IndexFormatVersion indexFormatVersion)
            throws NamespaceException {
        String namespaceURI = name.getNamespaceURI();
        String localName = name.getLocalName();
        String qualifiedName = nsMappings.getPrefix(namespaceURI) + ":" +
                localName;
        doc.removeField(FieldNames.LABEL);
        doc.add(new Field(FieldNames.LABEL, qualifiedName, Field.Store.NO,
                Field.Index.NO_NORMS));
        // as of version 3, also index combination of namespace URI and
        // local name
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            doc.removeField(FieldNames.NAMESPACE_URI);
            doc.add(new Field(FieldNames.NAMESPACE_URI, namespaceURI,
                    Field.Store.NO, Field.Index.NO_NORMS));
            doc.removeField(FieldNames.LOCAL_NAME);
            doc.add(new Field(FieldNames.LOCAL_NAME, localName, Field.Store.NO,
                    Field.Index.NO_NORMS));
        }
    }

    private void addPropertyName(Document doc, Name name,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion) {
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V2.getVersion()) {
            String fieldName = name.getLocalName();
            try {
                fieldName = resolver.getJCRName(name);
            } catch (NamespaceException e) {
                // will never happen
            }
            doc.add(new Field(FieldNames.PROPERTIES_SET, fieldName,
                    Field.Store.NO, Field.Index.NO_NORMS));
        }
    }

    private Field createPathField(Name propName, Path path,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion) {
        String pathString = path.toString();
        try {
            pathString = resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            // will never happen
        }
        return createField(propName, pathString, PropertyType.PATH, resolver,
                indexFormatVersion);
    }

    private Field createNameField(Name propName, Name name,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion)
            throws NamespaceException {
        return createField(propName, resolver.getJCRName(name),
                PropertyType.NAME, resolver, indexFormatVersion);
    }

    private Field createStringField(Name propName, String string,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion) {
        return createField(propName, string, PropertyType.STRING, resolver,
                indexFormatVersion);
    }

    // copied from NodeIndexer.createFieldWithoutNorms
    private Field createField(Name name, String string, int propertyType,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion) {
        String propName = name.getLocalName();
        try {
            propName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }
        Field field;
        if (indexFormatVersion.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            field = new Field(FieldNames.PROPERTIES,
                    new SingletonTokenStream(FieldNames.createNamedValue(
                            propName, string), propertyType));
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
        return dt == null ? Collections.<String> emptySet() : dt.getFacets();
    }

    private void addFacets(Document doc, Set<String> facets,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion) {
        Name name = NodeConstants.ECM_MIXIN_TYPE.qname;

        String propName = name.getLocalName();
        try {
            propName = resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // will never happen
        }

        addPropertyName(doc, name, resolver, indexFormatVersion);

        for (String facet : facets) {
            doc.add(createStringField(name, facet, resolver, indexFormatVersion));
            // TODO addLength for V3
        }
        if (facets.size() > 1) {
            doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO,
                    Field.Index.UN_TOKENIZED, Field.TermVector.NO));
        }
    }

    private String getParentId(NodeState node) throws RepositoryException {
        try {
            ItemStateManager stateMgr = getContext().getItemStateManager();
            NodeId pid = node.getParentId();
            if (pid == null) {
                return null;
            }
            NodeState parent = (NodeState) stateMgr.getItemState(pid);
            pid = parent.getParentId();
            if (pid == null) {
                return null;
            }
            return pid.toString();
        } catch (ItemStateException e) {
            String msg = "Error while indexing node: " + node.getNodeId() +
                    " of type: " + node.getNodeTypeName();
            throw new RepositoryException(msg, e);
        }
    }

    private void addParent(Document doc, String parentId,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion)
            throws RepositoryException {
        if (parentId == null) {
            return;
        }
        doc.add(createStringField(NodeConstants.ECM_PARENT_ID.qname, parentId,
                resolver, indexFormatVersion));
        // TODO addLength for V3
    }

    private void addVersionLabel(Document doc, String label,
            NamePathResolver resolver, IndexFormatVersion indexFormatVersion) {
        doc.add(createStringField(NodeConstants.ECM_VERSION_LABEL.qname, label,
                resolver, indexFormatVersion));
    }

}

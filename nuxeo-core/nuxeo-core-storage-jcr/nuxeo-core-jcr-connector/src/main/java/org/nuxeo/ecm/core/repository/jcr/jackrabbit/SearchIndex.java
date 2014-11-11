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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.IndexFormatVersion;
import org.apache.jackrabbit.core.query.lucene.LongField;
import org.apache.jackrabbit.core.query.lucene.NamePathResolverImpl;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SingletonTokenStream;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.nuxeo.ecm.core.repository.jcr.NodeConstants;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Bogdan Stefanescu
 * @author Eugen Ionica
 * @author Florent Guillaume
 */
public class SearchIndex extends
        org.apache.jackrabbit.core.query.lucene.SearchIndex {

    private static SchemaManager schemaManager;

    public SearchIndex() {
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
        return new DocumentIndexer(nsMappings, indexFormatVersion).createDocument(node);
    }

    protected class DocumentIndexer {

        protected final NamespaceMappings nsMappings;

        protected final IndexFormatVersion indexFormatVersion;

        protected final NamePathResolver resolver;

        protected final ItemStateManager itemStateManager;

        protected DocumentIndexer(NamespaceMappings nsMappings,
                IndexFormatVersion indexFormatVersion) {
            this.nsMappings = nsMappings;
            this.indexFormatVersion = indexFormatVersion;
            resolver = NamePathResolverImpl.create(nsMappings);
            itemStateManager = getContext().getItemStateManager();
        }

        protected Document createDocument(NodeState node)
                throws RepositoryException {

            Name nodeTypeName = node.getNodeTypeName();
            boolean isDoc = nodeTypeName.getNamespaceURI().equals(
                    NodeConstants.NS_ECM_DOCS_URI); // nuxeo doc or version
            boolean isProxy = nodeTypeName.equals(NodeConstants.ECM_NT_DOCUMENT_PROXY.qname);

            NodeState dataNode = isProxy ? getProxyDataNode(node) : node;
            Document doc = SearchIndex.super.createDocument(dataNode,
                    nsMappings, indexFormatVersion);

            if (isProxy) {
                fixupProxy(doc, node, dataNode);
            }

            if (isDoc || isProxy) {

                // check if it's a version
                boolean isVersion;
                String label = "";
                if (isProxy) {
                    isVersion = false;
                } else {
                    try {
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
                        throw new RepositoryException("No item state: "
                                + node.getParentId(), e);
                    }
                }

                // ecm:parentId
                String parentId;
                if (isVersion) {
                    parentId = getParentId(node); // XXX ?
                } else {
                    parentId = getParentId(node);
                }
                addParent(doc, parentId);

                // ecm:path
                addPath(doc, getPath(node));

                // ecm:name
                addName(doc, getName(node));

                // ecm:mixinType
                Set<String> facets = getFacets(dataNode);
                if (isProxy || isVersion) {
                    facets = new HashSet<String>(facets);
                    facets.add(FacetNames.IMMUTABLE);
                }
                addFacets(doc, facets);

                // ecm:label added on frozen version too
                if (isVersion) {
                    addVersionLabel(doc, label);
                }

                // ecm:isProxy
                addIsProxy(doc, isProxy);

                // ecm:isCheckedInVersion
                addIsVersion(doc, isVersion);
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

            try {
                PropertyState ps = (PropertyState) itemStateManager.getItemState(versionId);
                InternalValue value = ps.getValues()[0];
                return (NodeState) itemStateManager.getItemState(new NodeId(
                        value.getUUID()));
            } catch (ItemStateException e) {
                throw new RepositoryException("No item state: " + versionId, e);
            }
        }

        // see NodeIndexer.createDoc
        private void fixupProxy(Document doc, NodeState node, NodeState dataNode)
                throws RepositoryException {

            // primaryType is that of the target
            // add ecmnt:documentProxy to MIXINTYPES so that we can find proxies
            addPropertyName(doc, NameConstants.JCR_MIXINTYPES);
            doc.add(createNameField(NameConstants.JCR_MIXINTYPES,
                    node.getNodeTypeName()));

            // UUID
            String uuid = node.getNodeId().toString();
            doc.removeField(FieldNames.UUID);
            doc.add(new Field(FieldNames.UUID, uuid, Field.Store.YES,
                    Field.Index.NO_NORMS, Field.TermVector.NO));
            changePropertyValue(doc, NameConstants.JCR_UUID, uuid);

            // Ref UUID
            setRefUUIDField(doc, node);

            // PARENT
            doc.removeField(FieldNames.PARENT);
            doc.add(new Field(FieldNames.PARENT, node.getParentId().toString(),
                    Field.Store.YES, Field.Index.NO_NORMS, Field.TermVector.NO));

            // LABEL
            NodeState parent;
            try {
                parent = (NodeState) itemStateManager.getItemState(node.getParentId());
            } catch (ItemStateException e) {
                throw new RepositoryException("No item state: "
                        + node.getParentId(), e);
            }
            ChildNodeEntry child = parent.getChildNodeEntry(node.getNodeId());
            if (child == null) {
                // this can only happen when jackrabbit
                // is running in a cluster.
                throw new RepositoryException(
                        "Missing child node entry for node with id: "
                                + node.getNodeId());
            }
            addNodeName(doc, child.getName(), nsMappings, indexFormatVersion);

        }

        private void setRefUUIDField(Document doc, NodeState node)
                throws RepositoryException {
            String fieldName = resolver.getJCRName(NodeConstants.ECM_REF_UUID.qname);
            doc.add(new Field(FieldNames.PROPERTIES_SET, fieldName,
                    Field.Store.NO, Field.Index.NO_NORMS));
            PropertyId refUUID = new PropertyId(node.getNodeId(),
                    NodeConstants.ECM_REF_UUID.qname);
            try {
                PropertyState ps = (PropertyState) itemStateManager.getItemState(refUUID);
                InternalValue value = ps.getValues()[0];
                doc.add(createStringField(NodeConstants.ECM_REF_UUID.qname,
                        value.getString()));
                Field field = new Field(FieldNames.PROPERTIES,
                        new SingletonTokenStream(FieldNames.createNamedValue(
                                fieldName, value.getString()),
                                PropertyType.STRING));
                field.setOmitNorms(true);
                doc.add(field);
                // create fulltext index on property
                int idx = fieldName.indexOf(':');
                fieldName = fieldName.substring(0, idx + 1)
                        + FieldNames.FULLTEXT_PREFIX
                        + fieldName.substring(idx + 1);
                Field f = new Field(fieldName, value.getString(),
                        Field.Store.NO, Field.Index.TOKENIZED,
                        Field.TermVector.NO);
                doc.add(f);
                doc.add(new Field(FieldNames.FULLTEXT, value.getString(),
                        Field.Store.NO, Field.Index.TOKENIZED,
                        Field.TermVector.NO));

                doc.add(new Field(FieldNames.PROPERTY_LENGTHS,
                        FieldNames.createNamedLength(fieldName,
                                value.getString().length()), Field.Store.NO,
                        Field.Index.NO_NORMS));
            } catch (NoSuchItemStateException e1) {
                throw new RepositoryException(e1);
            } catch (ItemStateException e1) {
                throw new RepositoryException(e1);
            }
        }

        /**
         * Deep surgery inside Lucene to change a single property.
         * <p>
         * We have to check the value and this can't be done without destroying
         * a stream so we have to recreate it after checking.
         */
        private void changePropertyValue(Document doc, Name name, String value) {
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
                NamespaceMappings nsMappings,
                IndexFormatVersion indexFormatVersion)
                throws NamespaceException {
            String namespaceURI = name.getNamespaceURI();
            String localName = name.getLocalName();
            String qualifiedName = nsMappings.getPrefix(namespaceURI) + ":"
                    + localName;
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
                doc.add(new Field(FieldNames.LOCAL_NAME, localName,
                        Field.Store.NO, Field.Index.NO_NORMS));
            }
        }

        private void addPropertyName(Document doc, Name name) {
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

        private Field createNameField(Name propName, Name name)
                throws NamespaceException {
            return createField(propName, resolver.getJCRName(name),
                    PropertyType.NAME);
        }

        private Field createStringField(Name propName, String string) {
            return createField(propName, string, PropertyType.STRING);
        }

        private Field createLongField(Name propName, long value) {
            return createField(propName, LongField.longToString(value),
                    PropertyType.LONG);
        }

        // see NodeIndexer.createFieldWithoutNorms
        private Field createField(Name name, String string, int propertyType) {
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
                        Field.Store.NO, Field.Index.NO_NORMS,
                        Field.TermVector.NO);
            }
            return field;
        }

        private void addFacets(Document doc, Set<String> facets) {
            Name name = NodeConstants.ECM_MIXIN_TYPE.qname;

            String propName = name.getLocalName();
            try {
                propName = resolver.getJCRName(name);
            } catch (NamespaceException e) {
                // will never happen
            }

            addPropertyName(doc, name);

            for (String facet : facets) {
                doc.add(createStringField(name, facet));
                // TODO addLength for V3
            }
            if (facets.size() > 1) {
                doc.add(new Field(FieldNames.MVP, propName, Field.Store.NO,
                        Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            }
        }

        private void addParent(Document doc, String parentId) {
            if (parentId == null) {
                return;
            }
            doc.add(createStringField(NodeConstants.ECM_PARENT_ID.qname,
                    parentId));
            // TODO addLength for V3
        }

        private void addPath(Document doc, String path) {
            doc.add(createStringField(NodeConstants.ECM_PATH.qname, path));
            // TODO addLength for V3
        }

        private void addName(Document doc, String name) {
            doc.add(createStringField(NodeConstants.ECM_NAME.qname, name));
            // TODO addLength for V3
        }

        private void addVersionLabel(Document doc, String label) {
            doc.add(createStringField(NodeConstants.ECM_VERSION_LABEL.qname,
                    label));
        }

        private void addIsProxy(Document doc, boolean isProxy) {
            doc.add(createLongField(NodeConstants.ECM_ISPROXY.qname,
                    isProxy ? 1 : 0));
        }

        private void addIsVersion(Document doc, boolean isVersion) {
            doc.add(createLongField(NodeConstants.ECM_ISCHECKEDINVERSION.qname,
                    isVersion ? 1 : 0));
        }

        private Set<String> getFacets(NodeState node) {
            Name name = node.getNodeTypeName();
            String typeName = name.getLocalName();
            DocumentType dt = schemaManager.getDocumentType(typeName);
            return dt == null ? Collections.<String> emptySet()
                    : dt.getFacets();
        }

        private String getParentId(NodeState node) throws RepositoryException {
            try {
                NodeId pid = node.getParentId();
                if (pid == null) {
                    return null;
                }
                NodeState parent = (NodeState) itemStateManager.getItemState(pid);
                pid = parent.getParentId();
                if (pid == null) {
                    return null;
                }
                return pid.toString();
            } catch (ItemStateException e) {
                String msg = "Error while indexing node: " + node.getNodeId()
                        + " of type: " + node.getNodeTypeName();
                throw new RepositoryException(msg, e);
            }
        }

        /**
         * Finds the path, using only local names.
         *
         * @param node the node state
         * @return the path
         * @throws RepositoryException
         */
        private String getPath(NodeState node) throws RepositoryException {
            try {
                List<String> names = new LinkedList<String>();
                int size = 0;
                while (true) {
                    NodeId pid = node.getParentId();
                    if (pid == null) {
                        break;
                    }
                    NodeState parent;
                    parent = (NodeState) itemStateManager.getItemState(pid);
                    if (parent == null) {
                        break;
                    }
                    pid = parent.getParentId();
                    if (pid == null) {
                        // parent is the root
                        break;
                    }
                    ChildNodeEntry childNodeEntry = parent.getChildNodeEntry(node.getNodeId());
                    // we get the local name only, as we don't have a session
                    // mapping to interpret the namespace
                    String name = childNodeEntry.getName().getLocalName();
                    names.add(name);
                    size += name.length() + 1;
                    // then go up another time to skip the ecm:children
                    node = (NodeState) itemStateManager.getItemState(pid);
                }
                StringBuilder buf = new StringBuilder(size);
                Collections.reverse(names);
                for (String name : names) {
                    buf.append('/');
                    buf.append(name);
                }
                if (buf.length() == 0) {
                    buf.append('/');
                }
                return buf.toString();
            } catch (ItemStateException e) {
                String msg = "Error while indexing node: " + node.getNodeId()
                        + " of type: " + node.getNodeTypeName();
                throw new RepositoryException(msg, e);
            }
        }

        /**
         * Finds the local name.
         *
         * @param node the node state
         * @return the name
         * @throws RepositoryException
         */
        private String getName(NodeState node) throws RepositoryException {
            try {
                NodeId pid = node.getParentId();
                if (pid == null) {
                    return "";
                }
                NodeState parent;
                parent = (NodeState) itemStateManager.getItemState(pid);
                if (parent == null) {
                    return "";
                }
                pid = parent.getParentId();
                if (pid == null) {
                    // parent is the root
                    return "";
                }
                ChildNodeEntry childNodeEntry = parent.getChildNodeEntry(node.getNodeId());
                // we get the local name only, as we don't have a session
                // mapping to interpret the namespace
                return childNodeEntry.getName().getLocalName();
            } catch (ItemStateException e) {
                String msg = "Error while indexing node: " + node.getNodeId()
                        + " of type: " + node.getNodeTypeName();
                throw new RepositoryException(msg, e);
            }
        }
    }

}

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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.ItemDef;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.name.QName;
import org.nuxeo.ecm.core.schema.SchemaManager;

/**
 * The builtin types manager.
 * <p>
 * This class creates and registers the builtin ECM types into JackRabbit.
 * <p>
 * The current implementation maps documents to nodes.
 * Sub-documents are stored as sub-nodes of the parent document.
 * Complex schema properties are stored as nodes in a special sub-node
 * named <code>ecm:properties</code>.
 * These sub-nodes are of type <code>ecm-nt:object</code>.
 * <p>
 * All ECM types are derived from a base type <code>ecm-nt:base</code>.
 * <p>
 * Schemas are defined as mixin types.
 * All custom schemas must be derived from the base schema mixin type
 * <code>ecm-nt:schema</code>.
 * Custom schema types must be prefixed with <code>ecm-schema:</code>
 * <p>
 * All custom document types must be derived from the base document type
 * <code>ecm-nt:document</code>.
 * Custom document types must be prefixed with <code>ecm-doc:</code>
 * <p>
 * This is the list of the current implemented types:
 * <ul>
 * <li><i>ecm-nt:base</i>            - base type for all the ecm types
 * <li><i>ecm-nt:document</i>        - base type for documents
 * <li><i>ecm-nt:object</i>          - base type for complex objects (schema properties)
 * <li><i>ecm-nt:objbag</i>          - type used for the special document sub-node used to store complex schema properties
 * <li><i>ecm-nt:objseq</i>          - type used for the special document sub-node used to store ordered complex schema properties
 *                                     <b>Unused</b>
 * <li><i>ecm-nt:docbag</i>          - type used for the special document sub-node used to store unordered children.
 *                                     <b>Unused</b>
 * <li><i>ecm-nt:docseq</i>          - type used for the special document sub-node used to store ordered children.
 *                                     <b>Unused</b>
 * <li><i>ecm-mix:schema</i>         - base type for schemas
 * <li><i>ecm-mix:folder</i>         - marker type that denotes a folder
 * <li><i>ecm-mix:orderedfolder</i>  - marker type that denotes an ordered folder
 *
 * </ul>
 * The <code>ecm-mix</code> nodes cannot represent themselves a node type,
 * instead they are used as additional
 * types given a primary type and they are not derived from ecm-nt:base
 * <p>
 * Reserved names are prefixed with <code>ecm</code> and are. This is the list
 * of the defined names:
 * <ul>
 * <li><code>ecm:properties</code>
 * <li><code>ecm:children</code>
 * </ul>
 * The following namespaces are defined:
 * <ul>
 * <li>uri: <code>http://nuxeo.org/ecm/jcr/types/</code>,
 *     prefix: <code>ecm-nt</code> - denotes ECM node types
 * <li>uri: <code>http://nuxeo.org/ecm/jcr/mixin/</code>,
 *     prefix: <code>ecm-mix</code> - denotes ECM mixin types
 * <li>uri: <code>http://nuxeo.org/ecm/jcr/names/</code>,
 *     prefix: <code>ecm</code> - denotes ECM names
 * <li>uri: <code>http://nuxeo.org/ecm/jcr/docs/</code>,
 *     prefix: <code>ecm-doc</code> - denotes ECM document types (derived from ecm-nt:document)
 * <li>uri: <code>http://nuxeo.org/ecm/jcr/schemas/</code>,
 *     prefix: <code>ecm-schema</code> - denotes ECM schema types (derived from ecm-nt:schema)
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class BuiltinTypes implements NodeConstants {

    private static final Log log = LogFactory.getLog(BuiltinTypes.class);

    // Utility class
    private BuiltinTypes() { }

    public static void registerTypes(SchemaManager typeMgr, Workspace ws,
            boolean force) throws RepositoryException,
            InvalidNodeTypeDefException {
        // register builtin types
        registerBuiltinTypes(typeMgr, ws, force);
        // register user types
        registerUserTypes(ws.getSession(), typeMgr);
    }

    public static void registerTypes(SchemaManager typeMgr, Workspace ws)
            throws RepositoryException, InvalidNodeTypeDefException {
        // register builtin types
        registerBuiltinTypes(typeMgr, ws, false);
        // register user types
        registerUserTypes(ws.getSession(), typeMgr);
    }

    public static void registerBuiltinTypes(SchemaManager typeMgr, Workspace ws,
            boolean force) throws RepositoryException, InvalidNodeTypeDefException {
        NodeTypeRegistry ntReg = ((NodeTypeManagerImpl) ws.getNodeTypeManager()).getNodeTypeRegistry();
        boolean firstImport = !ntReg.isRegistered(ECM_NT_BASE.qname);
        if (firstImport) {
            // builtin types not registered -> register them now
            log.info("Importing ECM types to JackRabbit repository ...");
            registerBuiltinNamespaces(ws.getNamespaceRegistry());
            registerBuiltinNodeTypes((NodeTypeManagerImpl) ws.getNodeTypeManager());
        } else if (force) {
            //TODO: use force flag -> add force property to type service
            // builtin types not registered -> register them now
            log.info("Re-Importing ECM types to JackRabbit repository ...");
            registerBuiltinNamespaces(ws.getNamespaceRegistry());
            reregisterBuiltinNodeTypes((NodeTypeManagerImpl) ws.getNodeTypeManager());
        }
    }

    public static void registerUserTypes(Session session, SchemaManager typeMgr)
            throws RepositoryException, InvalidNodeTypeDefException {
        TypeImporter importer = new TypeImporter(session);
        importer.registerTypes(typeMgr);
    }

    public static void registerBuiltinNodeTypes(NodeTypeManagerImpl ntMgr)
            throws RepositoryException, InvalidNodeTypeDefException {
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        Collection<NodeTypeDef> ntDefs = createBuiltinNodeTypes();
        ntReg.registerNodeTypes(ntDefs);
    }

    public static void reregisterBuiltinNodeTypes(NodeTypeManagerImpl ntMgr)
            throws RepositoryException, InvalidNodeTypeDefException {
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        Collection<NodeTypeDef> ntDefs = createBuiltinNodeTypes();
        for (NodeTypeDef ntDef : ntDefs) {
            ntReg.reregisterNodeType(ntDef);
        }
    }

    private static void registerBuiltinNamespaces(NamespaceRegistry nsReg)
            throws RepositoryException {
        try {
            nsReg.registerNamespace(NS_ECM_SYSTEM_PREFIX, NS_ECM_SYSTEM_URI);
            nsReg.registerNamespace(NS_ECM_TYPES_PREFIX, NS_ECM_TYPES_URI);
            nsReg.registerNamespace(NS_ECM_MIXIN_PREFIX, NS_ECM_MIXIN_URI);
            nsReg.registerNamespace(NS_ECM_DOCS_PREFIX, NS_ECM_DOCS_URI);
            nsReg.registerNamespace(NS_ECM_SCHEMAS_PREFIX, NS_ECM_SCHEMAS_URI);
            nsReg.registerNamespace(NS_ECM_FIELDS_PREFIX, NS_ECM_FIELDS_URI);

            nsReg.registerNamespace(NS_ECM_VERSIONING_PREFIX, NS_ECM_VERSIONING_URI);
        } catch (NamespaceException e) {
            // do nothing --> may be the ns was already registered
            log.debug("Failed to register builtin namespaces");
        }
    }

    public static Collection<NodeTypeDef> createBuiltinNodeTypes() {
        List<NodeTypeDef> list = new ArrayList<NodeTypeDef>();

        list.add(createBaseNodeType());
        list.add(createFolderMixinType());
        list.add(createUnstructuredMixinNodeType());
        list.add(createDocumentNodeType());
        list.add(createContainerNodeType());
        list.add(createOrderedFolderMixinType());
        list.add(createOrderedContainerNodeType());
        list.add(createSchemaNodeType());
        list.add(createComplexFieldNodeType());
        list.add(createComplexListNodeType());
        list.add(createComplexBagNodeType());

        list.add(createSystemAny());
        list.add(createSystemSchemaNodeType());
        list.add(createProxySchemaNodeType());
        list.add(createFrozenDocumentProxyType());
        list.add(createACPNodeType());
        list.add(createACLNodeType());
        list.add(createACENodeType());
        list.add(createSecuritySchemaNodeType());
        list.add(createLifeCycleSchemaNodeType());

        list.add(createVersionStorageNodeType());
        list.add(createVersionHistoryNodeType());
        list.add(createVersionNodeType());
        list.add(createVersionableMixinNodeType());

        list.add(createMixinContentNodeType());

        return list;
    }

    public static void registerNodeTypes(JCRSession session, List<NodeTypeDef> list)
            throws RepositoryException, InvalidNodeTypeDefException {
        Workspace ws = session.getSession().getWorkspace();
        NodeTypeManagerImpl ntMgr = (NodeTypeManagerImpl) ws.getNodeTypeManager();
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        ntReg.registerNodeTypes(list);
    }

    public static void reregisterNodeTypes(JCRSession session, List<NodeTypeDef> ntDefs)
            throws RepositoryException, InvalidNodeTypeDefException {
        Workspace ws = session.getSession().getWorkspace();
        NodeTypeManagerImpl ntMgr = (NodeTypeManagerImpl) ws.getNodeTypeManager();
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        //ntReg.registerNodeTypes(ntDefs);
        for (NodeTypeDef ntDef : ntDefs) {
            try {
                ntReg.reregisterNodeType(ntDef);
            } catch (NoSuchNodeTypeException e) {
                ntReg.registerNodeType(ntDef);
            } catch (Exception e) {
                // skip it
                log.warn("failed to re-register nodetype " + ntDef);
            }
        }
    }

    /**
     * The base type of any ECM object.
     *
     * @return
     */
    public static NodeTypeDef createBaseNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_BASE.qname);
        ntd.setSupertypes(new QName[] {QName.NT_BASE});

        /*
            // TODO remove this and use unstructured node type
            // residual node defs set to any ecm nt base
            NodeDefImpl ndef = new NodeDefImpl();
            ndef.setDeclaringNodeType(ECM_NT_BASE.qname);
            ndef.setRequiredPrimaryTypes(new QName[] { ECM_NT_BASE.qname });

            // residual property defs set to any property
            PropDefImpl pdef = new PropDefImpl();
            pdef.setDeclaringNodeType(ECM_NT_BASE.qname);

            ntd.setChildNodeDefs(new NodeDef[] { ndef });
            ntd.setPropertyDefs(new PropDef[] { pdef });
        */

        return ntd;
    }

    /**
     * The base type of any unstructured ECM object.
     *
     * @return
     */
    private static NodeTypeDef createUnstructuredMixinNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_MIX_UNSTRUCTURED.qname);
        ntd.setMixin(true);

        // residual node defs set to any ecm nt base
        NodeDefImpl ndef = new NodeDefImpl();
        ndef.setDeclaringNodeType(ECM_MIX_UNSTRUCTURED.qname);
        ndef.setRequiredPrimaryTypes(new QName[] { ECM_NT_BASE.qname });

        // residual property defs set to any property
        PropDefImpl pdef = new PropDefImpl();
        pdef.setDeclaringNodeType(ECM_MIX_UNSTRUCTURED.qname);

        ntd.setChildNodeDefs(new NodeDef[] { ndef });
        ntd.setPropertyDefs(new PropDef[] { pdef });

        return ntd;
    }

    /**
     * The system schema mixin type.
     * <p>
     * All types implement this schema.
     *
     * @return
     */
    public static NodeTypeDef createSecuritySchemaNodeType() {
        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_SECURITY_SCHEMA.qname);
        ntd.setMixin(true);
        ntd.setSupertypes(new QName[] {ECM_MIX_SCHEMA.qname});

        // create children definitions
        // the system schema has only one field -> the security field
        NodeDefImpl ndefDoc = new NodeDefImpl();
        ndefDoc.setName(ECM_ACP.qname);
        ndefDoc.setAllowsSameNameSiblings(false);
        ndefDoc.setAutoCreated(false);
        ndefDoc.setOnParentVersion(OnParentVersionAction.IGNORE);
        ndefDoc.setDeclaringNodeType(ECM_NT_SECURITY_SCHEMA.qname);
        ndefDoc.setDefaultPrimaryType(ECM_NT_ACP.qname);
        ndefDoc.setRequiredPrimaryTypes(new QName[] { ECM_NT_ACP.qname });

        ntd.setChildNodeDefs(new NodeDef[] { ndefDoc });

        return ntd;
    }

    /**
     * Create node type definition used to create child node that
     * will hold any (random) system props for a doc.
     * @return
     */
    public static NodeTypeDef createSystemAny() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_SYSTEM_ANY.qname);

        PropDefImpl pd = new PropDefImpl();
        pd.setDeclaringNodeType(ntd.getName());

        ntd.setPropertyDefs(new PropDef[] {pd});

        return ntd;
    }

    public static NodeTypeDef createSystemSchemaNodeType() {
        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_SYSTEM_SCHEMA.qname);
        ntd.setMixin(true);
        ntd.setSupertypes(new QName[] {ECM_MIX_SCHEMA.qname});

        NodeDefImpl nd = new NodeDefImpl();
        nd.setDeclaringNodeType(ntd.getName());
        nd.setDefaultPrimaryType(ECM_SYSTEM_ANY.qname);
        nd.setName(ECM_SYSTEM_ANY.qname);
        nd.setAutoCreated(true);
        ntd.setChildNodeDefs(new NodeDef[] {nd});

        // create children definitions
        PropDefImpl pdLock = new PropDefImpl();
        pdLock.setDeclaringNodeType(ECM_NT_SYSTEM_SCHEMA.qname);
        pdLock.setMultiple(false);
        pdLock.setMandatory(false);
        pdLock.setName(ECM_LOCK.qname);
        pdLock.setRequiredType(PropertyType.STRING);
        pdLock.setOnParentVersion(OnParentVersionAction.IGNORE);

        PropDefImpl pdIndexed = new PropDefImpl();
        pdIndexed.setDeclaringNodeType(ECM_NT_SYSTEM_SCHEMA.qname);
        pdIndexed.setMultiple(false);
        pdIndexed.setMandatory(false);
        pdIndexed.setName(ECM_INDEXED.qname);
        pdIndexed.setRequiredType(PropertyType.BOOLEAN);
        pdIndexed.setOnParentVersion(OnParentVersionAction.IGNORE);

        PropDefImpl pdDirty = new PropDefImpl();
        pdDirty.setDeclaringNodeType(ECM_NT_SYSTEM_SCHEMA.qname);
        pdDirty.setMultiple(false);
        pdDirty.setMandatory(false);
        pdDirty.setName(ECM_DIRTY.qname);
        pdDirty.setRequiredType(PropertyType.BOOLEAN);
        pdDirty.setOnParentVersion(OnParentVersionAction.IGNORE);

        ntd.setPropertyDefs(new PropDef[] { pdLock, pdIndexed, pdDirty });

        return ntd;
    }

    public static NodeTypeDef createProxySchemaNodeType() {
        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_PROXY_SCHEMA.qname);
        ntd.setMixin(true);
        ntd.setSupertypes(new QName[] {ECM_MIX_SCHEMA.qname});

        // create children definitions
        PropDefImpl refFrozenNode = new PropDefImpl();
        refFrozenNode.setDeclaringNodeType(ECM_NT_PROXY_SCHEMA.qname);
        refFrozenNode.setMultiple(false);
        refFrozenNode.setMandatory(false);
        refFrozenNode.setName(ECM_REF_FROZEN_NODE.qname);
        refFrozenNode.setRequiredType(PropertyType.REFERENCE);

        PropDefImpl refUuid = new PropDefImpl();
        refUuid.setDeclaringNodeType(ECM_NT_PROXY_SCHEMA.qname);
        refUuid.setMultiple(false);
        refUuid.setMandatory(false);
        refUuid.setName(ECM_REF_UUID.qname);
        refUuid.setRequiredType(PropertyType.STRING);

        ntd.setPropertyDefs(new PropDef[] { refFrozenNode, refUuid });

        return ntd;
    }

    /**
     * The base type of any document.
     *
     * @return
     */
    public static NodeTypeDef createDocumentNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_DOCUMENT.qname);
        ntd.setSupertypes(new QName[] { ECM_NT_BASE.qname,
                ECM_NT_SECURITY_SCHEMA.qname,
                ECM_NT_LIFECYCLE_SCHEMA.qname,
                ECM_NT_SYSTEM_SCHEMA.qname,
                QName.MIX_LOCKABLE,
                QName.MIX_REFERENCEABLE, });

        // no direct fields inside basic document type
        return ntd;
    }

    public static NodeTypeDef createFrozenDocumentProxyType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_DOCUMENT_PROXY.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_DOCUMENT.qname,
                ECM_NT_PROXY_SCHEMA.qname, });

        // no direct fields
        return ntd;
    }

    /**
     * The marker mixin type for folder nodes.
     *
     * @return
     */
    public static NodeTypeDef createFolderMixinType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_MIX_FOLDER.qname);
        ntd.setMixin(true);

        NodeDefImpl ndefDocBag = new NodeDefImpl();
        ndefDocBag.setName(ECM_CHILDREN.qname);
        ndefDocBag.setAllowsSameNameSiblings(false);
        ndefDocBag.setAutoCreated(true);
        ndefDocBag.setDeclaringNodeType(ECM_MIX_FOLDER.qname);
        ndefDocBag.setDefaultPrimaryType(ECM_NT_CONTAINER.qname);
        ndefDocBag.setRequiredPrimaryTypes(new QName[] { ECM_NT_CONTAINER.qname });

        ntd.setChildNodeDefs(new NodeDef[] { ndefDocBag });

        return ntd;
    }

    /**
     * The marker mixin type for ordered folder nodes.
     *
     * @return
     */
    public static NodeTypeDef createOrderedFolderMixinType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_MIX_ORDERED.qname);
        ntd.setMixin(true);

        NodeDefImpl ndefDocBag = new NodeDefImpl();
        ndefDocBag.setName(ECM_CHILDREN.qname);
        ndefDocBag.setAllowsSameNameSiblings(false);
        ndefDocBag.setAutoCreated(true);
        ndefDocBag.setDeclaringNodeType(ECM_MIX_ORDERED.qname);
        ndefDocBag.setDefaultPrimaryType(ECM_NT_OCONTAINER.qname);
        ndefDocBag.setRequiredPrimaryTypes(new QName[] { ECM_NT_OCONTAINER.qname });

        ntd.setChildNodeDefs(new NodeDef[] { ndefDocBag });

        return ntd;
    }

    /**
     * The container node used by a folder to holds its children.
     *
     * @return
     */
    public static NodeTypeDef createContainerNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_CONTAINER.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_BASE.qname});

        NodeDefImpl ndefDocs = new NodeDefImpl();
        ndefDocs.setAllowsSameNameSiblings(false);
        ndefDocs.setDeclaringNodeType(ECM_NT_CONTAINER.qname);
        ndefDocs.setDefaultPrimaryType(ECM_NT_DOCUMENT.qname);

        ntd.setChildNodeDefs(new NodeDef[] { ndefDocs });

        return ntd;
    }

    /**
     * The ordered container node used by an ordered folder to holds its children.
     *
     * @return
     */
    public static NodeTypeDef createOrderedContainerNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_OCONTAINER.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_CONTAINER.qname});
        ntd.setOrderableChildNodes(true);

        return ntd;
    }

    /**
     * The base node type for all schemas.
     *
     * @return
     */
    public static NodeTypeDef createSchemaNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_MIX_SCHEMA.qname);
        ntd.setMixin(true);

        return ntd;
    }

    /**
     * The base node type for all complex fields.
     *
     * @return
     */
    public static NodeTypeDef createComplexFieldNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_PROPERTY.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_BASE.qname});

        return ntd;
    }

    /**
     * The node type used for all complex list.
     *
     * @return
     */
    public static NodeTypeDef createComplexListNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_PROPERTY_LIST.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_PROPERTY.qname});
        ntd.setOrderableChildNodes(true);

        createUnstructuredChildren(ntd);
        PropDefImpl pd = new PropDefImpl();
        pd.setDeclaringNodeType(ntd.getName());
        PropDefImpl pd2 = new PropDefImpl();
        pd2.setMultiple(true);
        pd2.setRequiredType(PropertyType.STRING);
        pd2.setName(ECM_LIST_KEYS.qname);
        pd2.setDeclaringNodeType(ntd.getName());

        NodeDefImpl nd = new NodeDefImpl();
        nd.setDeclaringNodeType(ntd.getName());
        ntd.setPropertyDefs(new PropDef[] {pd, pd2});
        ntd.setChildNodeDefs(new NodeDef[] {nd});

//        // residual node defs set to any ecm nt base
//        NodeDefImpl ndef = new NodeDefImpl();
//        ndef.setDeclaringNodeType(ECM_NT_PROPERTY_LIST.qname);
//        ndef.setRequiredPrimaryTypes(new QName[] { ECM_NT_PROPERTY.qname });
//
//        // residual property defs set to any property
//        PropDefImpl pdef = new PropDefImpl();
//        pdef.setDeclaringNodeType(ECM_NT_PROPERTY_LIST.qname);
//
//        ntd.setChildNodeDefs(new NodeDef[] {ndef} );
//        ntd.setPropertyDefs(new PropDef[] {pdef} );

        return ntd;
    }

    public static void createUnstructuredChildren(NodeTypeDef ntd) {
        PropDefImpl pd = new PropDefImpl();
        pd.setDeclaringNodeType(ntd.getName());
        NodeDefImpl nd = new NodeDefImpl();
        nd.setDeclaringNodeType(ntd.getName());
        ntd.setPropertyDefs(new PropDef[] {pd});
        ntd.setChildNodeDefs(new NodeDef[] {nd});
    }

    public static NodeTypeDef createComplexBagNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_PROPERTY_BAG.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_PROPERTY.qname});

        // residual node defs set to any ecm nt base
        NodeDefImpl ndef = new NodeDefImpl();
        ndef.setDeclaringNodeType(ECM_NT_PROPERTY_BAG.qname);
        ndef.setRequiredPrimaryTypes(new QName[] { ECM_NT_PROPERTY.qname });

        // residual property defs set to any property
        PropDefImpl pdef = new PropDefImpl();
        pdef.setDeclaringNodeType(ECM_NT_PROPERTY_BAG.qname);

        ntd.setChildNodeDefs(new NodeDef[] { ndef });
        ntd.setPropertyDefs(new PropDef[] { pdef });

        return ntd;
    }

    /**
     * The ACP node type.
     *
     * @return
     */
    public static NodeTypeDef createACPNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_ACP.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_PROPERTY.qname});
        ntd.setOrderableChildNodes(true);

        NodeDefImpl ndACL = new NodeDefImpl();
        ndACL.setAllowsSameNameSiblings(false);
        ndACL.setDeclaringNodeType(ECM_NT_ACP.qname);
        ndACL.setDefaultPrimaryType(ECM_NT_ACL.qname);
        ndACL.setRequiredPrimaryTypes(new QName[] { ECM_NT_ACL.qname });
        ndACL.setName(ItemDef.ANY_NAME);

        PropDefImpl pdOwners = new PropDefImpl();
        pdOwners.setDeclaringNodeType(ECM_NT_ACP.qname);
        pdOwners.setMultiple(true);
        // Jackrabbit doesn't accept empty arrays as def values so we cannot use this here
//        pdOwners.setAutoCreated(true);
//        pdOwners.setDefaultValues(null); // empty array
        pdOwners.setName(ECM_OWNERS.qname);
        pdOwners.setRequiredType(PropertyType.STRING);
        //pdOwners.setMandatory(false);

        ntd.setChildNodeDefs(new NodeDef[] { ndACL });
        ntd.setPropertyDefs(new PropDef[] { pdOwners });
        return ntd;
    }

    /**
     * The ACL node type.
     *
     * @return
     */
    public static NodeTypeDef createACLNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_ACL.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_PROPERTY.qname});
        ntd.setOrderableChildNodes(true);

        NodeDefImpl ndACE = new NodeDefImpl();
        ndACE.setAllowsSameNameSiblings(false);
        ndACE.setDeclaringNodeType(ECM_NT_ACL.qname);
        ndACE.setDefaultPrimaryType(ECM_NT_ACE.qname);
        ndACE.setRequiredPrimaryTypes(new QName[] { ECM_NT_ACE.qname });
        ndACE.setName(ItemDef.ANY_NAME);

        PropDefImpl pdName = new PropDefImpl();
        pdName.setDeclaringNodeType(ECM_NT_ACL.qname);
        pdName.setMultiple(false);
        pdName.setName(ECM_NAME.qname);
        pdName.setRequiredType(PropertyType.STRING);
        //pdOwners.setMandatory(false);

        ntd.setChildNodeDefs(new NodeDef[] { ndACE });
        ntd.setPropertyDefs(new PropDef[] { pdName });
        return ntd;
    }

    /**
     * The ACE node type.
     *
     * @return
     */
    public static NodeTypeDef createACENodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_ACE.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_PROPERTY.qname});

        PropDefImpl pdType = new PropDefImpl();
        pdType.setDeclaringNodeType(ECM_NT_ACE.qname);
        pdType.setMultiple(false);
        pdType.setName(ECM_TYPE.qname);
        pdType.setRequiredType(PropertyType.BOOLEAN);
        //pdOwners.setMandatory(false);

        PropDefImpl pdPermissions = new PropDefImpl();
        pdPermissions.setDeclaringNodeType(ECM_NT_ACE.qname);
        pdPermissions.setMultiple(false);
        pdPermissions.setName(ECM_PERMISSION.qname);
        pdPermissions.setRequiredType(PropertyType.STRING);
        //pdPermissions.setMandatory(false);

        PropDefImpl pdPrincipals = new PropDefImpl();
        pdPrincipals.setDeclaringNodeType(ECM_NT_ACE.qname);
        pdPrincipals.setMultiple(false);
        pdPrincipals.setName(ECM_PRINCIPAL.qname);
        pdPrincipals.setRequiredType(PropertyType.STRING);
        //pdPrincipals.setMandatory(false);

        ntd.setPropertyDefs(new PropDef[] { pdType, pdPermissions, pdPrincipals });
        return ntd;
    }

    /**
     * Creates a LifeCycle Schema node type.
     *
     * @return node type def
     */
    public static NodeTypeDef createLifeCycleSchemaNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_LIFECYCLE_SCHEMA.qname);
        ntd.setMixin(true);
        ntd.setSupertypes(new QName[] {ECM_MIX_SCHEMA.qname});

        PropDefImpl pdState = new PropDefImpl();
        pdState.setDeclaringNodeType(ECM_NT_LIFECYCLE_SCHEMA.qname);
        pdState.setMultiple(false);
        pdState.setName(ECM_LIFECYCLE_STATE.qname);
        pdState.setRequiredType(PropertyType.STRING);

        PropDefImpl pdLifecyclePolicy = new PropDefImpl();
        pdLifecyclePolicy.setDeclaringNodeType(ECM_NT_LIFECYCLE_SCHEMA.qname);
        pdLifecyclePolicy.setMultiple(false);
        pdLifecyclePolicy.setName(ECM_LIFECYCLE_POLICY.qname);
        pdLifecyclePolicy.setRequiredType(PropertyType.STRING);

        ntd.setPropertyDefs(new PropDef[] { pdState , pdLifecyclePolicy });

        return ntd;
    }

    //------------
    /**
     * Creates ecm:versionStorage node type.
     */
    public static NodeTypeDef createVersionStorageNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_VERSION_STORAGE.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_BASE.qname});
        ntd.setOrderableChildNodes(true);

        NodeDefImpl ndefDocs = new NodeDefImpl();
        ndefDocs.setAllowsSameNameSiblings(false);
        ndefDocs.setDeclaringNodeType(ECM_VERSION_STORAGE.qname);
        ndefDocs.setDefaultPrimaryType(ECM_NT_VERSION_HISTORY.qname);

        ntd.setChildNodeDefs(new NodeDef[] { ndefDocs });

        return ntd;
    }

    /**
     * Creates ecm:versionHistory node type.
     */
    public static NodeTypeDef createVersionHistoryNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_VERSION_HISTORY.qname);
        ntd.setSupertypes(new QName[] { ECM_NT_VERSION_HISTORY.qname,
                QName.MIX_REFERENCEABLE });
        ntd.setMixin(true);

        // ecm:versionId
        PropDefImpl pdVersionId = new PropDefImpl();
        pdVersionId.setDeclaringNodeType(ECM_VERSION_HISTORY.qname);
        pdVersionId.setMultiple(false);
        pdVersionId.setMandatory(false);
        pdVersionId.setName(ECM_VERSION_ID.qname);
        pdVersionId.setRequiredType(PropertyType.LONG);

        ntd.setPropertyDefs(new PropDef[] {pdVersionId});

        // versions
        PropDefImpl pdVersion = new PropDefImpl();
        pdVersion.setDeclaringNodeType(ECM_NT_VERSION.qname);
        pdVersion.setMultiple(true);
        pdVersion.setName(ECM_VERSION.qname);

        // child node types : version
        NodeDefImpl ndefDocs = new NodeDefImpl();
        ndefDocs.setAllowsSameNameSiblings(false);
        ndefDocs.setDeclaringNodeType(ECM_VERSION_HISTORY.qname);
        ndefDocs.setDefaultPrimaryType(ECM_NT_VERSION.qname);

        ntd.setChildNodeDefs(new NodeDef[] { ndefDocs });

        return ntd;
    }

    public static NodeTypeDef createVersionNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_VERSION.qname);
        ntd.setSupertypes(new QName[] {ECM_NT_VERSION.qname, QName.MIX_REFERENCEABLE});
        ntd.setMixin(true);

        // ecm:createDate
        PropDefImpl pdCreateDate = new PropDefImpl();
        pdCreateDate.setDeclaringNodeType(ECM_VERSION.qname);
        pdCreateDate.setMultiple(false);
        pdCreateDate.setName(ECM_VERSION_CREATEDATE.qname);
        pdCreateDate.setRequiredType(PropertyType.DATE);

        // ecm:label
        PropDefImpl pdLabel = new PropDefImpl();
        pdLabel.setDeclaringNodeType(ECM_VERSION.qname);
        pdLabel.setMultiple(false);
        pdLabel.setName(ECM_VERSION_LABEL.qname);
        pdLabel.setRequiredType(PropertyType.STRING);

        // ecm:description
        PropDefImpl pdDescription = new PropDefImpl();
        pdDescription.setDeclaringNodeType(ECM_VERSION.qname);
        pdDescription.setMultiple(false);
        pdDescription.setName(ECM_VERSION_DESCRIPTION.qname);
        pdDescription.setRequiredType(PropertyType.STRING);

        // ecm:predecessor
        PropDefImpl pdPredecessor = new PropDefImpl();
        pdPredecessor.setDeclaringNodeType(ECM_VERSION.qname);
        pdPredecessor.setMultiple(false);
        pdPredecessor.setName(ECM_VERSION_PREDECESSOR.qname);
        pdPredecessor.setRequiredType(PropertyType.REFERENCE);

        // ecm:successor
        PropDefImpl pdSuccessor = new PropDefImpl();
        pdSuccessor.setDeclaringNodeType(ECM_VERSION.qname);
        pdSuccessor.setMultiple(false);
        pdSuccessor.setName(ECM_VERSION_SUCCESSOR.qname);
        pdSuccessor.setRequiredType(PropertyType.REFERENCE);

        ntd.setPropertyDefs(new PropDef[] { pdCreateDate, pdLabel,
                pdDescription, pdPredecessor, pdSuccessor });

        return ntd;
    }

    /**
     * Creates a versionable node type.
     *
     * @return node type def
     */
    public static NodeTypeDef createVersionableMixinNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_VERSIONABLE_MIXIN.qname);
        ntd.setMixin(true);
        //ntd.setSupertypes(new QName[] {ECM_MIX_SCHEMA.qname});

        // ecm:versionHistory
        PropDefImpl pdVersionHistory = new PropDefImpl();
        pdVersionHistory.setDeclaringNodeType(ECM_VERSIONABLE_MIXIN.qname);
        pdVersionHistory.setMultiple(false);
        pdVersionHistory.setMandatory(false);
        pdVersionHistory.setName(ECM_VERSION_HISTORY.qname);
        pdVersionHistory.setRequiredType(PropertyType.REFERENCE);

        // ecm:baseVersion
        PropDefImpl pdBaseVersion = new PropDefImpl();
        pdBaseVersion.setDeclaringNodeType(ECM_NT_VER_BASE_VERSION.qname);
        pdBaseVersion.setMultiple(false);
        pdBaseVersion.setMandatory(false);
        pdBaseVersion.setName(ECM_VER_BASE_VERSION.qname);

        // ecm:isCheckedOut
        PropDefImpl pdCheckedOut = new PropDefImpl();
        pdCheckedOut.setDeclaringNodeType(ECM_VERSIONABLE_MIXIN.qname);
        pdCheckedOut.setMultiple(false);
        //pdCheckedOut.setMandatory(true);
        pdCheckedOut.setName(ECM_VER_ISCHECKEDOUT.qname);
        pdCheckedOut.setRequiredType(PropertyType.BOOLEAN);

        // ecm:frozenUuid
        PropDefImpl pdFrozenUuid = new PropDefImpl();
        pdFrozenUuid.setDeclaringNodeType(ECM_VERSIONABLE_MIXIN.qname);
        pdFrozenUuid.setMultiple(false);
        //pdCheckedOut.setMandatory(true);
        pdFrozenUuid.setName(ECM_FROZEN_NODE_UUID.qname);
        pdFrozenUuid.setRequiredType(PropertyType.STRING);

        ntd.setPropertyDefs(new PropDef[] {pdVersionHistory, pdCheckedOut, pdFrozenUuid});

        return ntd;
    }


    public static NodeTypeDef createMixinContentNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_MIX_CONTENT.qname);
        ntd.setMixin(true);

        PropDefImpl pdLen = new PropDefImpl();
        pdLen.setDeclaringNodeType(ECM_MIX_CONTENT.qname);
        pdLen.setMultiple(false);
        pdLen.setMandatory(false);
        pdLen.setName(ECM_CONTENT_LENGTH.qname);
        pdLen.setRequiredType(PropertyType.LONG);

        PropDefImpl pdDigest = new PropDefImpl();
        pdDigest.setDeclaringNodeType(ECM_MIX_CONTENT.qname);
        pdDigest.setMultiple(false);
        pdDigest.setMandatory(false);
        pdDigest.setName(ECM_CONTENT_DIGEST.qname);
        pdDigest.setRequiredType(PropertyType.STRING);

        PropDefImpl pdName = new PropDefImpl();
        pdName.setDeclaringNodeType(ECM_MIX_CONTENT.qname);
        pdName.setMultiple(false);
        pdName.setMandatory(false);
        pdName.setName(ECM_CONTENT_FILENAME.qname);
        pdName.setRequiredType(PropertyType.STRING);

        ntd.setPropertyDefs(new PropDef[] {pdName, pdLen, pdDigest});

        return ntd;
    }
}

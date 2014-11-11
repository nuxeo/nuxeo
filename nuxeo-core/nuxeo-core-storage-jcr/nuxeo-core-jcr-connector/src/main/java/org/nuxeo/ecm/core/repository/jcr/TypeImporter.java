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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.nuxeo.ecm.core.repository.jcr.versioning.JCRVersioningService;
import org.nuxeo.ecm.core.repository.jcr.versioning.Versioning;
import org.nuxeo.ecm.core.repository.jcr.versioning.VersioningService;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.versioning.custom.CustomVersioningService;
import org.nuxeo.runtime.api.Framework;


/**
 *
 * When registering types you must respect the following order to avoid dependency errors:
 * <ol>
 * <li> Register namespaces first
 * <li> Register complex field types
 * <li> Register schema types
 * <li> Register document types
 * </ol>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeImporter implements NodeConstants {

    private static final Log log = LogFactory.getLog(TypeImporter.class);

    private final NodeTypeRegistry ntReg;
    private final NamespaceRegistry nsReg;


    public TypeImporter(JCRSession session) throws RepositoryException {
        this(session.jcrSession());
    }

    public TypeImporter(Session session) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        NodeTypeManagerImpl ntMgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        ntReg = ntMgr.getNodeTypeRegistry();
        nsReg = workspace.getNamespaceRegistry();
    }

    /**
     * Registers a namespace.
     *
     * @param ns the namespace to register
     * @throws RepositoryException
     */
    public final void registerNamespace(Namespace ns) throws RepositoryException {
        try {
            nsReg.registerNamespace(ns.prefix, ns.uri);
        } catch (NamespaceException e) {
            // ignore exceptions
        }
    }

    public final void registerTypes(SchemaManager typeMgr)
            throws RepositoryException, InvalidNodeTypeDefException {
        List<NodeTypeDef> ntDefs = new ArrayList<NodeTypeDef>();
        // this register only global types
        Type[] types = typeMgr.getTypes();
        if (types != null && types.length > 0) {
            collectFieldTypes(types, ntDefs);
        }
        Schema[] schemas = typeMgr.getSchemas();
        if (schemas != null && schemas.length > 0) {
            collectSchemas(typeMgr, schemas, ntDefs);
        }
        DocumentType[] docTypes = typeMgr.getDocumentTypes();
        if (docTypes != null && docTypes.length > 0) {
            collectDocTypes(docTypes, ntDefs);
        }
        ntReg.registerNodeTypes(ntDefs);
    }

    /**
     * Registers a group of dependent schemas.
     * <p>
     * This is creating and registering the mixin node type corresponding
     * with that schema.
     * <p>
     * If the schema is already registered do nothing.
     * <p>
     * Types referred by the schema field are not registered.
     * You should register them before registering the schema.
     *
     * @param schemas the schemas to register
     * @throws RepositoryException
     * @throws InvalidNodeTypeDefException
     */
    public void registerSchemas(Collection<Schema> schemas)
            throws RepositoryException, InvalidNodeTypeDefException {
        List<NodeTypeDef> ntDefs = new ArrayList<NodeTypeDef>();
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        collectSchemas(mgr, schemas.toArray(new Schema[schemas.size()]), ntDefs);
        ntReg.registerNodeTypes(ntDefs);
    }

    /**
     * Collects and creates the node types to register for the given
     * set of dependent schemas.
     * <p>
     * If the schema is already registered do nothing.
     * <p>
     * Types referred by the schema field are not collected.
     * You should collect them before registering the schema.
     *
     * @param schemas the schemas to register
     * @param ntDefs the node type definition array to fill with collected types
     * @throws RepositoryException
     */
    public void collectSchemas(SchemaManager mgr, Schema[] schemas, List<NodeTypeDef> ntDefs)
            throws RepositoryException {
        for (Schema schema : schemas) {
            // first collect types defined by each schemas
            Type[] types = mgr.getTypes(schema.getName());
            if (types != null && types.length > 0) {
                collectFieldTypes(types, ntDefs);
            }
            // now collect the schema
            Name qname = TypeAdapter.getSchemaName(schema);
            if (!ntReg.isRegistered(qname)) { // test if schema already registered
                // first, register the schema namespace
                Namespace ns = schema.getNamespace();
                if (ns.hasPrefix()) { // ignore empty namespace
                    registerNamespace(ns);
                }
                // add the schema to the list to be registered
                ntDefs.add(createSchemaDefinition(schema, qname));
            } else {
                // reregister schema
                try {
                    ntReg.reregisterNodeType(createSchemaDefinition(schema, qname));
                    // reregister namespace?
                } catch (Exception e) {
                    log.error("Failed to reregister node type for schema: "
                            + schema.getName() + " : " + e.getMessage());
                    //e.printStackTrace();
                }
            }
        }
    }

    protected NodeTypeDef createSchemaDefinition(Schema schema, Name qname) {
        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(qname);
        ntd.setMixin(true);
        // schemas have no supertypes
        ntd.setSupertypes(new Name[] {NodeConstants.ECM_MIX_SCHEMA.qname});

        // create childen definitions
        if (schema.hasFields()) {
            createChildrenDefs(schema, ntd);
        }
        return ntd;
    }

    /**
     * Registers a group of dependent complex types.
     * <p>
     * This is creating and registering the node type corresponding with that complex field.
     * <p>
     * If the type is already registered, do nothing.
     * <p>
     * Types referred by the schema field are not registered.
     * You should register them before registering this type.
     *
     * @param types the types to register
     * @throws RepositoryException
     * @throws InvalidNodeTypeDefException
     */
    public void registerFieldTypes(Collection<Type> types)
            throws RepositoryException, InvalidNodeTypeDefException {
        List<NodeTypeDef> ntDefs = new ArrayList<NodeTypeDef>();
        collectFieldTypes(types.toArray(new Type[types.size()]), ntDefs);
        ntReg.registerNodeTypes(ntDefs);
    }

    /**
     * Collects and creates the type definitions for the given set of
     * dependent complex types.
     * <p>
     * If the type is already registered, do nothing.
     * <p>
     * Types referred by these complex types are not collected.
     * You should collect them too.
     *
     * @param types the types to register
     * @param ntDefs the node type definition array to fill with collected types
     */
    public void collectFieldTypes(Type[] types, List<NodeTypeDef> ntDefs) {
        for (Type fieldType : types) {
            // only complex type may be registered - TODO what about list types?
            if (fieldType.isListType()) {
                ListType listType = (ListType) fieldType;
                if (!listType.isScalarList()) {
                    Name qname = TypeAdapter.getFieldTypeName(fieldType);
                    // test if schema already registered
                    if (!ntReg.isRegistered(qname)) {
                        ntDefs.add(createFieldDefinition((ListType) fieldType, qname));
                    }
                }
            } else if (fieldType.isComplexType()) { // complex type
                Name qname = TypeAdapter.getFieldTypeName(fieldType);
                // test if schema already registered
                if (!ntReg.isRegistered(qname)) {
                    ntDefs.add(createFieldDefinition((ComplexType) fieldType, qname));
                } else { // reregister type
                    try {
                        ntReg.reregisterNodeType(createFieldDefinition(
                            (ComplexType) fieldType, qname));
                    // XXX: do we really want to catch this exception ?
                    } catch (Exception e) {
                        log.error("Failed to reregister node type for complex type: "
                                + fieldType.getName() + " : " + e.getMessage());
                    }
                }
            }
        }
    }

    protected NodeTypeDef createFieldDefinition(ComplexType fieldType, Name qname) {
        if (qname.equals(NodeConstants.ECM_NT_CONTENT.qname)) { // special case for blobs
            return createContentNodeType();
        }
        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(qname);

        // set super type
        Name superQname = NodeConstants.ECM_NT_PROPERTY.qname;
        Type superType = fieldType.getSuperType();
        if (superType != null) {
            superQname = TypeAdapter.getFieldTypeName(superType);
        }
        ntd.setSupertypes(new Name[] {superQname});

        if (fieldType.hasFields()) {
            createChildrenDefs(fieldType, ntd);
        }

        return ntd;
    }

    protected static NodeTypeDef createFieldDefinition(ListType fieldType, Name qname) {
        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(qname);
        ntd.setOrderableChildNodes(true);

        // set super type
        Name superQname = NodeConstants.ECM_NT_PROPERTY_LIST.qname;
        ntd.setSupertypes(new Name[] { superQname });

        //createUnstructuredChildren(ntd);

        return ntd;
    }

    /**
     * Creates the children nodea and properties definitions for the given
     * node type def that correspond to a complex type.
     *
     * @param ctype the complex type
     * @param ntd the JCR node type def for the complex type
     */
    protected static void createChildrenDefs(ComplexType ctype, NodeTypeDef ntd) {
        List<PropDef> propDefs = new ArrayList<PropDef>();
        List<NodeDef> nodeDefs = new ArrayList<NodeDef>();
        Namespace ns = ctype.getNamespace();
        String nsUri = ns.hasPrefix() ? ns.uri : "";
        for (Field field : ctype.getFields()) {
            org.nuxeo.ecm.core.schema.types.QName name = field.getName();
            Type type = field.getType();
            if (type.getName().equals("content")) {
                // the content type is special -> it should be mapped to
                // nt:resource to be indexed by jackrabbit
                // TODO this is a hack. add namespaces to type names.
                NodeDefImpl nd = new NodeDefImpl();
                nd.setDeclaringNodeType(ntd.getName());
                //nd.setAutoCreated(true); // TODO: autocreated?
                Name typeQname = NameConstants.NT_RESOURCE;
                nd.setDefaultPrimaryType(typeQname);
                nd.setRequiredPrimaryTypes(new Name[]{typeQname});
                nd.setName(JCRName.NAME_FACTORY.create(nsUri, name.getLocalName()));
                nd.setAllowsSameNameSiblings(false);
                nodeDefs.add(nd);
            } else if (type.isComplexType()) {
                NodeDefImpl nd = new NodeDefImpl();
                nd.setDeclaringNodeType(ntd.getName());
                //nd.setAutoCreated(true); // TODO: autocreated?
                Name typeQname = TypeAdapter.getFieldTypeName(type);
                nd.setDefaultPrimaryType(typeQname);
                nd.setRequiredPrimaryTypes(new Name[]{typeQname});
                nd.setName(JCRName.NAME_FACTORY.create(nsUri, name.getLocalName()));
                nd.setAllowsSameNameSiblings(false);
                nodeDefs.add(nd);
                // TODO
            } else if (type.isListType()) {
                // only scalar list are supported for now
                ListType listType = (ListType) type;
                if (listType.getFieldType().isSimpleType()) {
                    PropDefImpl pd = new PropDefImpl();
                    //pd.setAutoCreated(true); // TODO: autocreated?
                    pd.setName(JCRName.NAME_FACTORY.create(nsUri, name.getLocalName())); //use default namespace
                    pd.setRequiredType(TypeAdapter.scalarType2Jcr(listType.getFieldType()));
                    pd.setDeclaringNodeType(ntd.getName());
                    pd.setMultiple(true);
                    //TODO pd.setDefaultValues(defaultValues);
                    propDefs.add(pd);
                } else {
                    NodeDefImpl nd = new NodeDefImpl();
                    nd.setDeclaringNodeType(ntd.getName());
                    //nd.setAutoCreated(true); // TODO: autocreated?
                    Name typeQname = TypeAdapter.getFieldTypeName(type);
                    nd.setDefaultPrimaryType(typeQname);
                    nd.setRequiredPrimaryTypes(new Name[]{typeQname});
                    nd.setName(JCRName.NAME_FACTORY.create(nsUri, name.getLocalName()));
                    nd.setAllowsSameNameSiblings(false);
                    nodeDefs.add(nd);
                }
            } else { // scalar type
                PropDefImpl pd = new PropDefImpl();
                //pd.setAutoCreated(true); // TODO: autocreated?
                pd.setName(JCRName.NAME_FACTORY.create(nsUri, name.getLocalName())); //use default namespace
                pd.setRequiredType(TypeAdapter.scalarType2Jcr(type));
                pd.setDeclaringNodeType(ntd.getName());
                //TODO pd.setDefaultValues(defaultValues);
                propDefs.add(pd);
            }
        }

        if (!propDefs.isEmpty()) {
            ntd.setPropertyDefs(propDefs.toArray(new PropDef[propDefs.size()]));
        }
        if (!nodeDefs.isEmpty()) {
            ntd.setChildNodeDefs(nodeDefs.toArray(new NodeDef[nodeDefs.size()]));
        }
    }

    protected static void createUnstructuredChildren(NodeTypeDef ntd) {
        PropDefImpl pd = new PropDefImpl();
        pd.setDeclaringNodeType(ntd.getName());
        NodeDefImpl nd = new NodeDefImpl();
        nd.setDeclaringNodeType(ntd.getName());
        ntd.setPropertyDefs(new PropDef[] {pd});
        ntd.setChildNodeDefs(new NodeDef[] {nd});
    }

    /**
     * Registers a group of dependent document types.
     * <p>
     * This is creating and registering the node type corresponding
     * with that complex field.
     * <p>
     * If the type is already registered do nothing.
     * <p>
     * Types referred by the schema field are not registered.
     * You should register them before registering this type.
     *
     * @param types the types to register
     * @throws RepositoryException
     * @throws InvalidNodeTypeDefException
     */
    public void registerDocTypes(Collection<DocumentType> types)
            throws RepositoryException, InvalidNodeTypeDefException {
        List<NodeTypeDef> ntDefs = new ArrayList<NodeTypeDef>();
        collectDocTypes(types.toArray(new DocumentType[types.size()]), ntDefs);
        ntReg.registerNodeTypes(ntDefs);
    }

    /**
     * Collects and creates the type definitions for the given set of
     * dependent document types.
     * <p>
     * If the type is already registered, does nothing.
     * <p>
     * Types refered by these doc types are not collected.
     * You should collect them separately.
     *
     * @param types the types to register
     * @param ntDefs the node type definition array to fill with collected types
     */
    public void collectDocTypes(DocumentType[] types, List<NodeTypeDef> ntDefs) {
        for (DocumentType docType : types) {
            Name qname = TypeAdapter.getDocTypeName(docType);
            if (!ntReg.isRegistered(qname)) { // test if schema already registered
                ntDefs.add(createDocTypeDefinition(docType, qname));
            } else {
                try {
                    ntReg.reregisterNodeType(createDocTypeDefinition(docType, qname));
                // XXX: do we really want to catch this exception ?
                } catch (Exception e) {
                    log.error(">>>> ERROR: Failed to reregister doc type: "
                            + docType.getName() + " : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Creates a JCR node def from the given document type.
     *
     * @param docType the document type
     * @param qname the qname of the node  type to create
     * @return the node type definition
     */
    protected static NodeTypeDef createDocTypeDefinition(DocumentType docType,
            Name qname) {

        // create the mixin node type
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(qname);

        // set super type
        Type superType = docType.getSuperType();

        Name superQname;
        if (superType != null) {
            superQname = TypeAdapter.getDocTypeName((DocumentType) superType);
        } else { // a root type
            superQname = NodeConstants.ECM_NT_DOCUMENT.qname;
        }

        List<Name> superTypes = new ArrayList<Name>();
        superTypes.add(superQname);
        if (docType.isFolder()) {
            if (docType.isOrdered()) {
                superTypes.add(NodeConstants.ECM_MIX_ORDERED.qname);
            } else {
                superTypes.add(NodeConstants.ECM_MIX_FOLDER.qname);
            }
        }
        if (docType.hasSchemas()) {
            Collection<Schema> schemas = docType.getSchemas();
            for (Schema schema : schemas) {
                if (schema == null) {
                    throw new IllegalStateException("docType: "
                            + docType.getName()
                            + " has an undefined (null) schema");
                }
                superTypes.add(TypeAdapter.getSchemaName(schema));
            }
        }
        // check if this document is versionable
        if (docType.getFacets().contains(FacetNames.VERSIONABLE)) {
            final VersioningService vs = Versioning.getService();

            //
            // the versioning system to be used can be specified from
            // a versioning service
            // TODO: delegate this to the versioning service
            //
            if (vs instanceof JCRVersioningService) {
                log.debug("add mixin: " + NameConstants.MIX_VERSIONABLE +
                        " for doc type: " + docType.getName());
                superTypes.add(NameConstants.MIX_VERSIONABLE);
            } else if (vs instanceof CustomVersioningService) {
                // add our custom versionhistory mix
                log.debug("add mixin: " +
                        NodeConstants.ECM_VERSIONABLE_MIXIN.qname +
                        " for doc type: " + docType.getName());
                superTypes.add(NodeConstants.ECM_VERSIONABLE_MIXIN.qname);
            } else {
                log.error("Cannot identify enabled versioning service " + vs);
            }
        }

        ntd.setSupertypes(superTypes.toArray(new Name[superTypes.size()]));

        // doc types have no children or property defs
        // these are specified by the attached schemas

        return ntd;
    }


    public boolean isTypeRegistered(Type type) {
        return ntReg.isRegistered(TypeAdapter.getFieldTypeName(type));
    }

    public boolean isDocTypeRegistered(DocumentType type) {
        return ntReg.isRegistered(TypeAdapter.getDocTypeName(type));
    }

    public boolean isSchemaRegistered(Schema type) {
        return ntReg.isRegistered(TypeAdapter.getSchemaName(type));
    }

    public static NodeTypeDef createContentNodeType() {
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(ECM_NT_CONTENT.qname);
        ntd.setSupertypes(new Name[] {ECM_NT_PROPERTY.qname,  ECM_MIX_CONTENT.qname,  NameConstants.NT_RESOURCE});

        return ntd;
    }

}

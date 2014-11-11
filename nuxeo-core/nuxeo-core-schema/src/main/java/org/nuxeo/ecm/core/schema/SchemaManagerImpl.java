/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.CompositeTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Schema Manager implementation.
 * <p>
 * Holds basic types (String, Integer, etc.), schemas, document types and
 * facets.
 */
public class SchemaManagerImpl implements SchemaManager {

    private static final Log log = LogFactory.getLog(SchemaManagerImpl.class);

    private final Map<String, TypeHelper> helpers = new Hashtable<String, TypeHelper>();

    private final Map<String, Type> typeReg = new HashMap<String, Type>();

    private final Map<String, Schema> schemaReg = new HashMap<String, Schema>();

    private final Map<String, Schema> uri2schemaReg = new HashMap<String, Schema>();

    private final Map<String, Schema> prefix2schemaReg = new HashMap<String, Schema>();

    private final Map<String, DocumentType> docTypeReg = new HashMap<String, DocumentType>();

    private final Map<String, CompositeType> facetReg = new HashMap<String, CompositeType>();

    private final Map<String, Set<String>> inheritanceCache = new HashMap<String, Set<String>>();

    private final Map<String, List<DocumentTypeDescriptor>> pendingDocTypes;

    private final Map<String, Field> fields = new HashMap<String, Field>();

    /** Facet -> docTypes having this facet. */
    private Map<String, Set<String>> facetsCache;

    private File schemaDir;

    private PrefetchInfo prefetchInfo; // global prefetch info

    public SchemaManagerImpl() throws Exception {
        pendingDocTypes = new HashMap<String, List<DocumentTypeDescriptor>>();
        schemaDir = new File(Framework.getRuntime().getHome(), "schemas");
        if (!schemaDir.isDirectory()) {
            schemaDir.mkdirs();
        }
        for (Type type : XSDTypes.getTypes()) {
            registerType(type);
        }
        registerBuiltinTypes();
        TypeProvider provider = Framework.getService(TypeProvider.class);
        if (provider != this && provider != null) { // should be a remote
                                                    // provider
            importTypes(provider);
        }
    }

    protected void registerBuiltinTypes() {
        registerDocumentType(new DocumentTypeImpl(null, TypeConstants.DOCUMENT,
                null, null, DocumentTypeImpl.T_DOCUMENT));
        registerType(AnyType.INSTANCE);
    }

    /**
     * Initializes initial types using a remote provider if any was specified.
     * <p>
     * Should be called when a provider is registered.
     */
    public synchronized void importTypes(TypeProvider provider) {
        // import remote types
        DocumentType[] docTypes = provider.getDocumentTypes();
        for (DocumentType docType : docTypes) {
            registerDocumentType(docType);
        }
        Schema[] schemas = provider.getSchemas();
        for (Schema schema : schemas) {
            registerSchema(schema);
        }
        Type[] types = provider.getTypes();
        for (Type type : types) {
            registerType(type);
        }
        for (CompositeType facet : provider.getFacets()) {
            registerFacet(facet);
        }
    }

    @Override
    public Type getType(String schema, String name) {
        if (SchemaNames.BUILTIN.equals(schema)) {
            return typeReg.get(name);
        } else if (SchemaNames.DOCTYPES.equals(schema)) {
            return docTypeReg.get(name);
        } else if (SchemaNames.SCHEMAS.equals(schema)) {
            return schemaReg.get(name);
        } else if (SchemaNames.FACETS.equals(schema)) {
            return facetReg.get(name);
        } else {
            Schema ownerSchema = schemaReg.get(schema);
            if (ownerSchema != null) {
                return ownerSchema.getType(name);
            }
            return null;
        }
    }

    // public void putType(Type type) {
    // TypeName name = type.getName();
    // if (name.isSchema()) {
    // schemas.put(name.getLocalName(), (Schema)type);
    // } else if (name.isDocumentType()) {
    // doctypes.put(name.getLocalName(), (DocumentType)type);
    // } else {
    // String ns = name.getNamespace();
    // Map<String, Type> types = (Map<String, Type>)namespaces.get(ns);
    // if (types == null) {
    // types = new HashMap<String, Type>();
    // namespaces.put(ns, types);
    // }
    // types.put(name.getLocalName(), type);
    // }
    // }
    //
    // public void removeType(TypeName name) {
    // if (name.isSchema()) {
    // schemas.remove(name.getLocalName());
    // } else if (name.isDocumentType()) {
    // doctypes.remove(name.getLocalName());
    // } else {
    // String ns = name.getNamespace();
    // Map<String, Type> types = (Map<String, Type>)namespaces.get(ns);
    // if (types != null) {
    // types.remove(name.getLocalName());
    // if (types.isEmpty()) {
    // namespaces.remove(ns);
    // }
    // }
    // }
    // }
    //

    @Override
    public void registerType(Type type) {
        String schema = type.getSchemaName();
        if (SchemaNames.BUILTIN.equals(schema)) {
            typeReg.put(type.getName(), type);
        } else if (SchemaNames.SCHEMAS.equals(schema)) {
            schemaReg.put(type.getName(), (Schema) type);
        } else if (SchemaNames.DOCTYPES.equals(schema)) {
            docTypeReg.put(type.getName(), (DocumentType) type);
        } else if (SchemaNames.FACETS.equals(schema)) {
            facetReg.put(type.getName(), (CompositeType) type);
        } else {
            Schema ownerSchema = schemaReg.get(schema);
            if (ownerSchema != null) {
                ownerSchema.registerType(type);
            }
        }
    }

    @Override
    public Type unregisterType(String name) {
        return typeReg.remove(name);
    }

    @Override
    public Type getType(String name) {
        return typeReg.get(name);
    }

    @Override
    public Type[] getTypes() {
        return typeReg.values().toArray(new Type[typeReg.size()]);
    }

    @Override
    public Type[] getTypes(String schema) {
        Schema ownerSchema = schemaReg.get(schema);
        if (schema != null) {
            return ownerSchema.getTypes();
        }
        return null;
    }

    @Override
    public int getTypesCount() {
        return typeReg.size();
    }

    @Override
    public void registerSchema(Schema schema) {
        synchronized (schemaReg) {
            Namespace ns = schema.getNamespace();
            uri2schemaReg.put(ns.uri, schema);
            prefix2schemaReg.put(ns.prefix, schema);
            schemaReg.put(schema.getName(), schema);
        }
    }

    @Override
    public Schema unregisterSchema(String name) {
        Schema schema = schemaReg.get(name);
        if (schema == null) {
            return null;
        }
        Namespace ns = schema.getNamespace();
        log.info("Unregister schema: " + name);
        synchronized (schemaReg) {
            uri2schemaReg.remove(ns.uri);
            prefix2schemaReg.remove(ns.prefix);
            return schemaReg.remove(name);
        }
    }

    @Override
    public Schema getSchema(String name) {
        synchronized (schemaReg) {
            return schemaReg.get(name);
        }
    }

    @Override
    public Schema getSchemaFromPrefix(String schemaPrefix) {
        synchronized (schemaReg) {
            return prefix2schemaReg.get(schemaPrefix);
        }
    }

    @Override
    public Schema getSchemaFromURI(String schemaURI) {
        synchronized (schemaReg) {
            return uri2schemaReg.get(schemaURI);
        }
    }

    @Override
    public Field getField(String prefixedName) {
        Field field = fields.get(prefixedName);
        if (field == null) {
            QName qname = QName.valueOf(prefixedName);
            String prefix = qname.getPrefix();
            Schema schema = getSchemaFromPrefix(prefix);
            if (schema == null) {
                // try using the name
                schema = getSchema(prefix);
            }
            if (schema != null) {
                field = schema.getField(qname.getLocalName());
                if (field != null) {
                    fields.put(prefixedName, field);
                }
            }
        }
        return field;
    }

    @Override
    public Schema[] getSchemas() {
        synchronized (schemaReg) {
            return schemaReg.values().toArray(new Schema[schemaReg.size()]);
        }
    }

    @Override
    public int getSchemasCount() {
        synchronized (schemaReg) {
            return schemaReg.size();
        }
    }

    public void setPrefetchInfo(PrefetchInfo prefetchInfo) {
        this.prefetchInfo = prefetchInfo;
    }

    public PrefetchInfo getPrefetchInfo() {
        return prefetchInfo;
    }

    // Document Types

    @Override
    public void registerDocumentType(DocumentType docType) {
        log.info("Register document type: " + docType.getName());
        synchronized (docTypeReg) {
            docTypeReg.put(docType.getName(), docType);
            facetsCache = null;
        }
    }

    public void registerDocumentType(DocumentTypeDescriptor dtd) {
        synchronized (docTypeReg) {
            DocumentType superType = null;
            if (dtd.superTypeName != null) {
                superType = docTypeReg.get(dtd.superTypeName);
                if (superType == null) {
                    postponeDocTypeRegistration(dtd);
                    return;
                }
            }
            registerDocumentType(superType, dtd);
        }
    }

    private DocumentType registerDocumentType(DocumentType superType,
            DocumentTypeDescriptor dtd) {
        synchronized (docTypeReg) {
            try {
                Set<String> schemaNames = SchemaDescriptor.getSchemaNames(dtd.schemas);
                // add schemas from facets
                for (String facetName : dtd.facets) {
                    CompositeType facet = getFacet(facetName);
                    if (facet != null) {
                        schemaNames.addAll(Arrays.asList(facet.getSchemaNames()));
                    } else {
                        log.warn("Document type " + dtd.name
                                + " uses undeclared facet: " + facetName);
                        // register it with no schemas
                        CompositeType ct = new CompositeTypeImpl(
                                (TypeRef<CompositeType>) null,
                                SchemaNames.FACETS, facetName, null);
                        registerFacet(ct);
                    }
                }
                DocumentType docType = new DocumentTypeImpl(superType,
                        dtd.name, schemaNames.toArray(new String[0]),
                        dtd.facets);
                docType.setChildrenTypes(dtd.childrenTypes);
                // use global prefetch info if not a local one was defined
                docType.setPrefetchInfo(dtd.prefetch != null ? new PrefetchInfo(
                        dtd.prefetch) : prefetchInfo);
                docTypeReg.put(dtd.name, docType);
                facetsCache = null;
                log.info("Registered document type: " + dtd.name);
                registerPendingDocTypes(docType);
                return docType;
            } catch (Exception e) {
                log.error("Error registering document type: " + dtd.name, e);
                // TODO: use component dependencies instead?
            }
            return null;
        }
    }

    private void registerPendingDocTypes(DocumentType superType) {
        List<DocumentTypeDescriptor> list = pendingDocTypes.remove(superType.getName());
        if (list == null) {
            return;
        }
        for (DocumentTypeDescriptor dtd : list) {
            registerDocumentType(superType, dtd);
        }
    }

    private void removeFromFacetsCache(DocumentType docType) {
        if (facetsCache == null) {
            return;
        }
        String name = docType.getName();
        for (String facet : docType.getFacets()) {
            Set<String> types = facetsCache.get(facet);
            types.remove(name);
            if (types.isEmpty()) {
                facetsCache.remove(facet); // Consistency
            }
        }
    }

    private void removeFromInheritanceCache(DocumentType docType) {
        String name = docType.getName();
        for (String type : inheritanceCache.keySet()) {
            Set<String> types = inheritanceCache.get(type);
            types.remove(name);
        }
        // The only case where an entry becomes empty.
        inheritanceCache.remove(name);
    }

    @Override
    public DocumentType unregisterDocumentType(String name) {
        log.info("Unregister document type: " + name);
        // TODO handle the case when the doctype to unreg is in the reg. pending
        // queue
        synchronized (docTypeReg) {
            DocumentType docType = docTypeReg.remove(name);
            if (docType != null) {
                removeFromFacetsCache(docType);
                removeFromInheritanceCache(docType);
            }
            return docType;
        }
    }

    private void postponeDocTypeRegistration(DocumentTypeDescriptor dtd) {
        List<DocumentTypeDescriptor> list = pendingDocTypes.get(dtd.superTypeName);
        if (list == null) {
            list = new ArrayList<DocumentTypeDescriptor>();
            pendingDocTypes.put(dtd.superTypeName, list);
        }
        list.add(dtd);
    }

    @Override
    public DocumentType getDocumentType(String name) {
        synchronized (docTypeReg) {
            return docTypeReg.get(name);
        }
    }

    @Override
    public DocumentType[] getDocumentTypes() {
        synchronized (docTypeReg) {
            return docTypeReg.values().toArray(
                    new DocumentType[docTypeReg.size()]);
        }
    }

    @Override
    public int getDocumentTypesCount() {
        synchronized (docTypeReg) {
            return docTypeReg.size();
        }
    }

    @Override
    public void registerFacet(CompositeType facet) {
        synchronized (facetReg) {
            facetReg.put(facet.getName(), facet);
            log.info("Registered facet: " + facet.getName());
        }
    }

    public void registerFacet(FacetDescriptor fd) {
        Set<String> schemas = SchemaDescriptor.getSchemaNames(fd.schemas);
        CompositeType ct = new CompositeTypeImpl((TypeRef<CompositeType>) null,
                SchemaNames.FACETS, fd.name, schemas.toArray(new String[0]));
        registerFacet(ct);
    }

    @Override
    public CompositeType unregisterFacet(String name) {
        synchronized (facetReg) {
            log.info("Unregistered facet: " + name);
            return facetReg.remove(name);
        }
    }

    @Override
    public CompositeType getFacet(String name) {
        synchronized (facetReg) {
            return facetReg.get(name);
        }
    }

    @Override
    public CompositeType[] getFacets() {
        synchronized (facetReg) {
            return facetReg.values().toArray(new CompositeType[facetReg.size()]);
        }
    }

    // Misc

    @Override
    public void clear() {
        synchronized (docTypeReg) {
            docTypeReg.clear();
            if (facetsCache != null) {
                facetsCache.clear();
            }
        }
        synchronized (schemaReg) {
            schemaReg.clear();
        }
        typeReg.clear();
        facetReg.clear();
    }

    public void setSchemaDirectory(File dir) {
        schemaDir = dir;
    }

    public File getSchemaDirectory() {
        return schemaDir;
    }

    public File getSchemaFile(String name) {
        return new File(schemaDir, name + ".xsd");
    }

    public URL resolveSchemaLocation(String location) {
        if (location.startsWith("schema://")) {
            try {
                return new File(schemaDir, location).toURI().toURL();
            } catch (MalformedURLException e) {
                log.error("failed to resolve schema location: " + location, e);
            }
        }
        return null;
    }

    /**
     * Same remarks as in {@link #getDocumentTypeNamesExtending}.
     *
     * Tested in nuxeo-core
     */
    @Override
    public Set<String> getDocumentTypeNamesForFacet(String facet) {
        if (facetsCache == null) {
            initFacetsCache();
        }
        return facetsCache.get(facet);
    }

    private void initFacetsCache() {
        if (facetsCache != null) {
            return; // another thread just did it
        }
        synchronized (this) {
            facetsCache = new HashMap<String, Set<String>>();
            for (DocumentType dt : getDocumentTypes()) {
                for (String facet : dt.getFacets()) {
                    Set<String> dts = facetsCache.get(facet);
                    if (dts == null) {
                        dts = new HashSet<String>();
                        facetsCache.put(facet, dts);
                    }
                    dts.add(dt.getName());
                }
            }
        }
    }

    /**
     * Implementation details: there is a cache on each server for this Assumes
     * that types never change in the lifespan of this server process and that
     * the Core server has finished loading its types.
     * <p>
     * This is tested in nuxeo-core and SearchBackendTestCase (hence compass
     * plugin).
     */
    @Override
    public Set<String> getDocumentTypeNamesExtending(String docTypeName) {
        Set<String> res = inheritanceCache.get(docTypeName);
        if (res != null) {
            return res;
        }
        synchronized (inheritanceCache) {
            // recheck in case another thread just did it

            res = inheritanceCache.get(docTypeName);
            if (res != null) {
                return res;
            }

            if (getDocumentType(docTypeName) == null) {
                return null;
            }
            res = new HashSet<String>();
            res.add(docTypeName);
            for (DocumentType dt : getDocumentTypes()) {
                Type parent = dt.getSuperType();
                if (parent == null) {
                    continue; // Must be the root document
                }
                if (docTypeName.equals(parent.getName())) {
                    res.addAll(getDocumentTypeNamesExtending(dt.getName()));
                }
            }
            inheritanceCache.put(docTypeName, res);
            return res;
        }
    }

    @Override
    public String getXmlSchemaDefinition(String name) {
        File file = getSchemaFile(name);
        if (file != null) {
            try {
                return FileUtils.readFile(file);
            } catch (IOException e) {
                log.error(
                        String.format("Could not read xsd file for '%s'", name),
                        e);
                return null;
            }
        }
        return null;
    }

    @Override
    public void registerHelper(String schema, String type, TypeHelper helper) {
        if (schema == null) { // a primitive type helper
            helpers.put(type, helper);
        } else {
            helpers.put(schema + ':' + type, helper);
        }
    }

    @Override
    public void unregisterHelper(String schema, String type) {
        if (schema == null) { // a primitive type helper
            helpers.remove(type);
        } else {
            helpers.remove(schema + ':' + type);
        }
    }

    @Override
    public TypeHelper getHelper(String schema, String type) {
        if (schema == null) { // a primitive type helper
            return helpers.get(type);
        } else {
            return helpers.get(schema + ':' + type);
        }
    }

}

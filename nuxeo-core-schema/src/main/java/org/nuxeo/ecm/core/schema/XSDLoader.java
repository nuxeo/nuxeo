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

package org.nuxeo.ecm.core.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.ListTypeImpl;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeBindingException;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSListSimpleType;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.parser.XSOMParser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XSDLoader {

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

    private static final Log log = LogFactory.getLog(XSDLoader.class);


    private final SchemaManagerImpl typeManager;

    private XSOMParser parser;


    public XSDLoader(SchemaManagerImpl typeManager) {
        this.typeManager = typeManager;
        //initParser();
        // TODO: all schemas are collected in the schema set when reusing the parser
    }

    protected void initParser() {
        parser = new XSOMParser();
        ErrorHandler errorHandler = new SchemaErrorHandler();
        parser.setErrorHandler(errorHandler);
        parser.setEntityResolver(new CustomEntityResolver());
    }

    /**
     * @param schemaSet
     * @return
     */
    // TODO: this type of loading schemas must use a new parser each time
    // a new schema should be loaded.
    // When reusing the parser the SchemaSet is collecting all the schemas.
    public final XSSchema getUserSchema(XSSchemaSet schemaSet) {
        Collection<XSSchema> schemas = schemaSet.getSchemas();
        for (XSSchema schema : schemas) {
            String ns = schema.getTargetNamespace();
            if (ns.length() > 0 && !ns.equals(NS_XSD)) {
                return schema;
            }
        }
        return null;
    }

    public Schema loadSchema(String name, String prefix, File file, boolean override)
            throws SAXException, IOException, TypeException {
        initParser();
        // TODO: after fixing schema loading remove this and put it in the ctor
        // since we may improve schema loading speed by reusing already parsed schemas
        parser.parse(file);
        XSSchemaSet schemaSet = parser.getResult();
        if (schemaSet != null) {
            XSSchema schema = getUserSchema(schemaSet);
            if (schema != null) {
                return loadSchema(name, prefix, schema, override);
            }
        }
        return null;
    }

    public Schema loadSchema(String name, String prefix, URL url)
            throws SAXException, TypeException {
        initParser();
        // TODO: after fixing schema loading remove this and put it in the ctor
        parser.parse(url);
        XSSchemaSet schemaSet = parser.getResult();
        if (schemaSet != null) {
            XSSchema schema = getUserSchema(schemaSet);
            if (schema != null) {
                return loadSchema(name, prefix, schema, false);
            }
        }
        return null;
    }

    public Schema loadSchema(String name, String prefix, InputStream in)
            throws SAXException, TypeException {
        initParser();
        // TODO: after fixing schema loading remove this and put it in the ctor
        parser.parse(in);
        XSSchemaSet schemaSet = parser.getResult();
        if (schemaSet != null) {
            XSSchema schema = getUserSchema(schemaSet);
            if (schema != null) {
                return loadSchema(name, prefix, schema, false);
            }
        }
        return null;
    }

    public Schema loadSchema(String name, String prefix, XSSchema schema, boolean override)
            throws TypeException {
        String ns = schema.getTargetNamespace();
        try {
            Schema ecmSchema = typeManager.getSchema(name);
            if (ecmSchema != null) {
                // schema already defined
                log.warn("Schema " + ns + " is already registered");
                if (!override) {
                    return ecmSchema;
                }
            }
            ecmSchema = new SchemaImpl(name, new Namespace(ns, prefix));
            // load elements
            Collection<XSElementDecl> elements = schema.getElementDecls().values();
            for (XSElementDecl el : elements) {
                // register the type if not yet registered
                Type ecmType = loadType(ecmSchema, el.getType());
                if (ecmType != null) {
                    // add the field to the schema
                    createField(ecmSchema, el, ecmType);
                } else {
                    log.warn("Failed to load field " + el.getName() + " : " + el.getType());
                }
            }
            typeManager.registerSchema(ecmSchema);
            return ecmSchema;
        } catch (TypeBindingException e) {
            throw e;
        } catch (Throwable t) {
            throw new TypeException("Failed to load XSD schema " + ns, t);
        }
    }

    public Type loadType(Schema schema, XSType type) throws TypeBindingException {
        String name;
        if (type.getName() == null || type.isLocal()) {
            name = getAnonymousTypeName(type);
            if (name == null) {
                log.warn("Unable to load type - no name found");
                return null;
            }
        } else {
            name = type.getName();
        }
        Type ecmType = typeManager.getType(name);
        // look into global types
        if (ecmType != null) { // an already registered type
            return ecmType;
        }
        // look into user types
        ecmType = schema.getType(name);
        if (ecmType != null) { // an already registered type
            return ecmType;
        }  // TODO!!!!!!!
        if (type.getTargetNamespace().equals(NS_XSD)) {
            ecmType = XSDTypes.getType(name);
            typeManager.registerType(ecmType);
            return ecmType; // register the primitive type
        } else if (type.isSimpleType()) {
            if (type instanceof XSListSimpleType) {
                ecmType = loadListType(schema, (XSListSimpleType) type);
            } else {
                ecmType = loadSimpleType(schema, type);
            }
        } else {
            ecmType = loadComplexType(schema, name, type.asComplexType());
        }
        if (ecmType != null) {
            schema.registerType(ecmType);
        }
        return ecmType;
    }

    public Type loadLocalType(XSType xsType) {
        // TODO
        return null;
    }

    /**
     *
     * @param name the type name (not theat type may have a null name if an anonymous type)
     * @param type
     * @return
     * @throws TypeBindingException
     */
    private Type loadComplexType(Schema schema, String name, XSType type)
            throws TypeBindingException {
        //String name = type.getName();
        XSType baseType = type.getBaseType();
        ComplexType superType = null;
        // the anyType is the basetype of itself
        if (baseType.getBaseType() != baseType) { // have a base type
            if (baseType.isComplexType()) {
                superType = (ComplexType) loadType(schema, baseType);
            } else {
                log.warn("Complex type has a non complex type super type???");
            }
        }
        XSComplexType xsct = type.asComplexType();
        // try to get the delta content
        XSContentType content = xsct.getExplicitContent();
        // if none get the entire content
        if (content == null) {
            content = xsct.getContentType();
        }
        Type ret = createComplexType(schema, superType, name, content);
        if (ret instanceof ComplexType) {
            // load attributes if any
            loadAttributes(schema, xsct, (ComplexType) ret);
        }

        return ret;
    }

    private void loadAttributes(Schema schema, XSComplexType xsct, ComplexType ct)
            throws TypeBindingException {
        Collection<? extends XSAttributeUse> attrs = xsct.getAttributeUses();
        for (XSAttributeUse attr : attrs) {
            XSAttributeDecl at = attr.getDecl();
            Type fieldType = loadType(schema, at.getType());
            if (fieldType == null) {
                throw new TypeBindingException("Cannot add type for '" + at.getName() + "'");
            }
            createField(ct, at, fieldType);
        }
    }

    private SimpleType loadSimpleType(Schema schema, XSType type) throws TypeBindingException {
        String name = type.getName();
        if (name == null) {
            // probably a local type -> ignore it
            return null;
        }
        XSType baseType = type.getBaseType();
        SimpleType superType = null;
        if (baseType != type) {
            // have a base type
            superType = (SimpleType) loadType(schema, baseType);
        }
        return new SimpleTypeImpl(superType, schema.getName(), name);
    }

    private ListType loadListType(Schema schema, XSListSimpleType type) {
        String name = type.getName();
        if (name == null) {
            // probably a local type -> ignore it
            return null;
        }
        XSType xsItemType = type.getItemType();
        Type itemType;
        if (xsItemType.getTargetNamespace().equals(NS_XSD)) {
            itemType = XSDTypes.getType(xsItemType.getName());
        } else {
            //itemType = loadType(schema, type);
            //TODO: type must be already defined - use a dependency manager or something to
            // support types that are not yet defined
            itemType = typeManager.getType(xsItemType.getName());
        }
        if (itemType == null) {
            log.error("list item type was not defined -> you should define first the item type");
            return null;
        }
        return new ListTypeImpl(schema.getName(), name, itemType);
    }

    private Type createComplexType(Schema schema, ComplexType superType, String name,
            XSContentType content) throws TypeBindingException {
        //System.out.println("DEBUG > defining complex type: " + name);
        ComplexType ct = new ComplexTypeImpl(superType, schema.getName(), name);
        // --------  Workaround - we register now the complex type - to fix recursive references to the same type
        schema.registerType(ct);
        // ------------------------------------------
        XSParticle particle = content.asParticle();
        if (particle == null) {
            // complex type without particle -> may be it contains only attributes -> return it as is
            return ct;
        }
        XSTerm term = particle.getTerm();
        XSModelGroup mg = term.asModelGroup();
        if (mg == null) {
            // TODO don't know how to handle this for now
            throw new TypeBindingException("unsupported complex type");
        }
        XSParticle[] group = mg.getChildren();
        if (group.length == 1 && superType == null && group[0].isRepeated()) {
            // a list
            return createListType(schema, name, group[0]);
        }
        for (XSParticle child : group) {
            term = child.getTerm();
            XSElementDecl element = term.asElementDecl();
            if (element == null) {
                // TODO don't know how to handle this for now
                log.warn("Ignoring " + name + " unsupported complex type");
                return null;
            }
            loadComplexTypeElement(schema, ct, element);
        }
        return ct;
    }


    public ListType createListType(Schema schema, String name, XSParticle particle)
            throws TypeBindingException {
        XSElementDecl element = particle.getTerm().asElementDecl();
        if (element == null) {
            log.warn("Ignoring " + name + " unsupported list type");
            return null;
        }
        XmlString dv = element.getDefaultValue();
        String defValue = null;
        if (dv != null) {
            defValue = dv.value;
        }
        Type type = loadType(schema, element.getType());
        return new ListTypeImpl(schema.getName(), name, type, element.getName(),
                defValue, particle.getMinOccurs(), particle.getMaxOccurs());
    }

    private void loadComplexTypeElement(Schema schema, ComplexType type, XSElementDecl element)
            throws TypeBindingException {
        XSType elementType = element.getType();

        Type fieldType = loadType(schema, elementType);
        if (fieldType != null) {
            createField(type , element, fieldType);
        }
    }

    private static Field createField(ComplexType type, XSElementDecl element, Type fieldType) {
        String elementName = element.getName();
        XmlString dv = element.getDefaultValue();
        String defValue = null;
        if (dv != null) {
            defValue = dv.value;
        }
        int flags = 0;
        if (defValue == null) {
            dv = element.getFixedValue();
            if (dv != null) {
                defValue = dv.value;
                flags |= Field.CONSTANT;
            }
        }

        if (element.isNillable()) {
            flags |= Field.NILLABLE;
        }
        return type.addField(elementName, fieldType.getRef(), defValue, flags);
    }

    private static Field createField(ComplexType type, XSAttributeDecl element, Type fieldType) {
        String elementName = element.getName();
        XmlString dv = element.getDefaultValue();
        String defValue = null;
        if (dv != null) {
            defValue = dv.value;
        }
        int flags = 0;
        if (defValue == null) {
            dv = element.getFixedValue();
            if (dv != null) {
                defValue = dv.value;
                flags |= Field.CONSTANT;
            }
        }
        return type.addField(elementName, fieldType.getRef(), defValue, flags);
    }


    static class SchemaErrorHandler implements ErrorHandler {

        public void error(SAXParseException exception) throws SAXException {
            log.error("Error: " + exception.getMessage());
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            log.error("FatalError: " + exception.getMessage());
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            log.error("Warning: " + exception.getMessage());
        }

    }

    class CustomEntityResolver implements EntityResolver {

        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            if (systemId != null) {
                URL url = typeManager.resolveSchemaLocation(systemId);
                if (url != null) {
                    InputSource is = new InputSource(url.openStream());
                    is.setPublicId(publicId);
                    return is;
                }
            }
            return null;
        }

    }

    private static String getAnonymousTypeName(XSType type) {
        if (type.isComplexType()) {
            XSElementDecl container = type.asComplexType().getScope();
            String elName = container.getName();
            return elName + "#anonymousType";
        }
        return null;
    }

}

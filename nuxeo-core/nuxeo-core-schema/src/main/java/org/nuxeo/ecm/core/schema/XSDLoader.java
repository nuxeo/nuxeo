/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Wojciech Sulejman
 *     Florent Guillaume
 *     Thierry Delprat
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema;

import static com.sun.xml.xsom.XSFacet.FACET_ENUMERATION;
import static com.sun.xml.xsom.XSFacet.FACET_LENGTH;
import static com.sun.xml.xsom.XSFacet.FACET_MAXEXCLUSIVE;
import static com.sun.xml.xsom.XSFacet.FACET_MAXINCLUSIVE;
import static com.sun.xml.xsom.XSFacet.FACET_MAXLENGTH;
import static com.sun.xml.xsom.XSFacet.FACET_MINEXCLUSIVE;
import static com.sun.xml.xsom.XSFacet.FACET_MININCLUSIVE;
import static com.sun.xml.xsom.XSFacet.FACET_MINLENGTH;
import static com.sun.xml.xsom.XSFacet.FACET_PATTERN;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintUtils;
import org.nuxeo.ecm.core.schema.types.constraints.DateIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.EnumConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.LengthConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NumericIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.ObjectResolverConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolverService;
import org.nuxeo.runtime.api.Framework;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xml.xsom.ForeignAttributes;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSListSimpleType;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.impl.RestrictionSimpleTypeImpl;
import com.sun.xml.xsom.parser.XSOMParser;

/**
 * Loader of XSD schemas into Nuxeo Schema objects.
 */
public class XSDLoader {

    private static final String ATTR_CORE_EXTERNAL_REFERENCES = "resolver";

    private static final Log log = LogFactory.getLog(XSDLoader.class);

    private static final String ANONYMOUS_TYPE_SUFFIX = "#anonymousType";

    private static final String NAMESPACE_CORE_VALIDATION = "http://www.nuxeo.org/ecm/schemas/core/validation/";

    private static final String NAMESPACE_CORE_EXTERNAL_REFERENCES = "http://www.nuxeo.org/ecm/schemas/core/external-references/";

    private static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

    protected final SchemaManagerImpl schemaManager;

    protected List<String> referencedXSD = new ArrayList<>();

    protected boolean collectReferencedXSD = false;

    protected SchemaBindingDescriptor sd;

    private ObjectResolverService referenceService;

    protected ObjectResolverService getObjectResolverService() {
        if (referenceService == null) {
            referenceService = Framework.getService(ObjectResolverService.class);
        }
        return referenceService;
    }

    public XSDLoader(SchemaManagerImpl schemaManager) {
        this.schemaManager = schemaManager;
    }

    public XSDLoader(SchemaManagerImpl schemaManager, SchemaBindingDescriptor sd) {
        this.schemaManager = schemaManager;
        this.sd = sd;
    }

    public XSDLoader(SchemaManagerImpl schemaManager, boolean collectReferencedXSD) {
        this.schemaManager = schemaManager;
        this.collectReferencedXSD = collectReferencedXSD;
    }

    protected void registerSchema(Schema schema) {
        schemaManager.registerSchema(schema);
    }

    protected Type getType(String name) {
        return schemaManager.getType(name);
    }

    protected XSOMParser getParser() {
        XSOMParser parser = new XSOMParser();
        ErrorHandler errorHandler = new SchemaErrorHandler();
        parser.setErrorHandler(errorHandler);
        if (sd != null) {
            parser.setEntityResolver(new NXSchemaResolver(schemaManager, sd));
        }
        return parser;
    }

    protected static class NXSchemaResolver implements EntityResolver {

        protected SchemaManagerImpl schemaManager;

        protected SchemaBindingDescriptor sd;

        NXSchemaResolver(SchemaManagerImpl schemaManager, SchemaBindingDescriptor sd) {
            this.schemaManager = schemaManager;
            this.sd = sd;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws IOException {

            String[] parts = systemId.split("/" + SchemaManagerImpl.SCHEMAS_DIR_NAME + "/");
            String importXSDSubPath = parts[1];

            File xsd = new File(schemaManager.getSchemasDir(), importXSDSubPath);
            if (!xsd.exists()) {
                int idx = sd.src.lastIndexOf("/");
                importXSDSubPath = sd.src.substring(0, idx + 1) + importXSDSubPath;
                URL url = sd.context.getLocalResource(importXSDSubPath);
                if (url == null) {
                    // try asking the class loader
                    url = sd.context.getResource(importXSDSubPath);
                }
                if (url != null) {
                    return new InputSource(url.openStream());
                }
            }

            return null;
        }

    }

    protected static class SchemaErrorHandler implements ErrorHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            log.error("Error: " + e.getMessage());
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            log.error("FatalError: " + e.getMessage());
            throw e;
        }

        @Override
        public void warning(SAXParseException e) {
            log.error("Warning: " + e.getMessage());
        }
    }

    // called by SchemaManagerImpl
    public Schema loadSchema(String name, String prefix, File file) throws SAXException, IOException, TypeException {
        return loadSchema(name, prefix, file, null);
    }

    /**
     * Called by schema manager.
     *
     * @since 5.7
     */
    public Schema loadSchema(String name, String prefix, File file, String xsdElement)
            throws SAXException, IOException, TypeException {
        return loadSchema(name, prefix, file, xsdElement, false);
    }

    /**
     * @param isVersionWritable if true, the schema's fields will be writable even for Version document.
     * @since 8.4
     */
    public Schema loadSchema(String name, String prefix, File file, String xsdElement, boolean isVersionWritable)
            throws SAXException, IOException, TypeException {
        XSOMParser parser = getParser();
        String systemId = file.toURI().toURL().toExternalForm();
        if (file.getPath().startsWith("\\\\")) { // Windows UNC share
            // work around a bug in Xerces due to
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5086147
            // (xsom passes a systemId of the form file://server/share/...
            // but this is not parsed correctly when turned back into
            // a File object inside Xerces)
            systemId = systemId.replace("file://", "file:////");
        }
        try {
            parser.parse(systemId);
        } catch (SAXParseException e) {
            throw new SAXException("Error parsing schema: " + systemId, e);
        }

        XSSchemaSet xsSchemas = parser.getResult();
        if (collectReferencedXSD) {
            collectReferencedXSD(xsSchemas);
        }
        return loadSchema(name, prefix, xsSchemas, xsdElement, isVersionWritable);
    }

    protected void collectReferencedXSD(XSSchemaSet xsSchemas) {

        Collection<XSSchema> schemas = xsSchemas.getSchemas();
        String ns;
        for (XSSchema s : schemas) {
            ns = s.getTargetNamespace();
            if (ns.length() <= 0 || ns.equals(NS_XSD)) {
                continue;
            }

            String systemId = s.getLocator().getSystemId();
            if (systemId != null && systemId.startsWith("file:/")) {
                String filePath = systemId.substring(6);
                if (!referencedXSD.contains(filePath)) {
                    referencedXSD.add(filePath);
                }
            }
        }

    }

    /**
     * Create Nuxeo schema from a XSD resource. If xsdElement is non null and correspont to the name of a complex
     * element, the schema is created from the target complex type instead of from the global schema
     *
     * @since 5.7
     * @param name schema name
     * @param prefix schema prefix
     * @param url url to load the XSD resource
     * @param xsdElement name of the complex element to use as root of the schema
     * @since 5.7
     */
    public Schema loadSchema(String name, String prefix, URL url, String xsdElement)
            throws SAXException, TypeException {
        XSOMParser parser = getParser();
        parser.parse(url);
        XSSchemaSet xsSchemas = parser.getResult();
        return loadSchema(name, prefix, xsSchemas, xsdElement);
    }

    // called by tests
    public Schema loadSchema(String name, String prefix, URL url) throws SAXException, TypeException {
        return loadSchema(name, prefix, url, null);
    }

    /**
     * @since 8.4
     */
    protected Schema loadSchema(String name, String prefix, XSSchemaSet schemaSet, String xsdElement)
            throws TypeException {
        return loadSchema(name, prefix, schemaSet, xsdElement, false);
    }

    protected Schema loadSchema(String name, String prefix, XSSchemaSet schemaSet, String xsdElement,
            boolean isVersionWritable) throws TypeException {
        if (schemaSet == null) {
            return null;
        }
        Collection<XSSchema> schemas = schemaSet.getSchemas();
        XSSchema schema = null;
        String ns = null;
        for (XSSchema s : schemas) {
            ns = s.getTargetNamespace();
            if (ns.length() > 0 && !ns.equals(NS_XSD)) {
                schema = s;
                break;
            }
        }
        if (schema == null) {
            return null;
        }
        Schema ecmSchema = new SchemaImpl(name, new Namespace(ns, prefix), isVersionWritable);

        // load elements
        Collection<XSElementDecl> elements = schema.getElementDecls().values();
        for (XSElementDecl el : elements) {
            // register the type if not yet registered
            Type ecmType = loadType(ecmSchema, el.getType(), el.getName());
            if (ecmType != null) {
                // add the field to the schema
                createField(ecmSchema, el, ecmType);
            } else {
                log.warn("Failed to load field " + el.getName() + " : " + el.getType());
            }
        }

        // load attributes
        Collection<XSAttributeDecl> attributes = schema.getAttributeDecls().values();
        for (XSAttributeDecl att : attributes) {
            // register the type if not yet registered
            Type ecmType = loadType(ecmSchema, att.getType(), att.getName());
            if (ecmType != null) {
                // add the field to the schema
                createField(ecmSchema, att, ecmType, true);
            } else {
                log.warn("Failed to load field from attribute " + att.getName() + " : " + att.getType());
            }
        }

        if (xsdElement != null) {
            Field singleComplexField = ecmSchema.getField(xsdElement);
            if (singleComplexField == null) {
                log.warn("Unable to find element " + xsdElement + " to rebase schema " + name);
            } else {
                if (singleComplexField.getType().isComplexType()) {
                    ComplexType singleComplexFieldType = (ComplexType) singleComplexField.getType();
                    ecmSchema = new SchemaImpl(singleComplexFieldType, name, new Namespace(ns, prefix),
                            isVersionWritable);
                } else {
                    log.warn("can not rebase schema " + name + " on " + xsdElement + " that is not a complex type");
                }
            }
        }

        registerSchema(ecmSchema);
        return ecmSchema;
    }

    /**
     * @param schema the nuxeo schema into we register the type.
     * @param type the XSD type to load
     * @param fieldName the field name owning this type, this is used when type is anonymous/local
     * @return the loaded type
     */
    protected Type loadType(Schema schema, XSType type, String fieldName) throws TypeBindingException {
        String name = getTypeName(type, fieldName);
        // look into global types
        Type ecmType = getType(name);
        if (ecmType != null) {
            return ecmType;
        }
        // look into user types for this schema
        ecmType = schema.getType(name);
        if (ecmType != null) {
            return ecmType;
        }
        // maybe an alias to a primitive type?
        if (type.getTargetNamespace().equals(NS_XSD)) {
            ecmType = XSDTypes.getType(name); // find alias
            if (ecmType == null) {
                log.warn("Cannot use unknown XSD type: " + name);
            }
            return ecmType;
        }
        if (type.isSimpleType()) {
            if (type instanceof XSListSimpleType) {
                ecmType = loadListType(schema, (XSListSimpleType) type, fieldName);
            } else {
                ecmType = loadSimpleType(schema, type, fieldName);
            }
        } else {
            ecmType = loadComplexType(schema, name, type.asComplexType());
        }
        if (ecmType != null) {
            schema.registerType(ecmType);
        } else {
            log.warn("loadType for " + fieldName + " of " + type + " returns null");
        }
        return ecmType;
    }

    /**
     * @param name the type name (note, the type may have a null name if an anonymous type)
     */
    protected Type loadComplexType(Schema schema, String name, XSType type) throws TypeBindingException {
        XSType baseType = type.getBaseType();
        ComplexType superType = null;
        // the anyType is the basetype of itself
        if (baseType.getBaseType() != baseType) { // have a base type
            if (baseType.isComplexType()) {
                superType = (ComplexType) loadType(schema, baseType, name);
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
        Type ret = createComplexType(schema, superType, name, content, xsct.isAbstract());
        if (ret != null && ret instanceof ComplexType) {
            // load attributes if any
            loadAttributes(schema, xsct, (ComplexType) ret);
        }

        return ret;
    }

    protected void loadAttributes(Schema schema, XSComplexType xsct, ComplexType ct) throws TypeBindingException {
        Collection<? extends XSAttributeUse> attrs = xsct.getAttributeUses();
        for (XSAttributeUse attr : attrs) {
            XSAttributeDecl at = attr.getDecl();
            Type fieldType = loadType(schema, at.getType(), at.getName());
            if (fieldType == null) {
                throw new TypeBindingException("Cannot add type for '" + at.getName() + "'");
            }
            createField(ct, at, fieldType, !attr.isRequired());
        }
    }

    protected SimpleType loadSimpleType(Schema schema, XSType type, String fieldName) throws TypeBindingException {
        String name = getTypeName(type, fieldName);
        XSType baseType = type.getBaseType();
        SimpleType superType = null;
        if (baseType != type) {
            // have a base type
            superType = (SimpleType) loadType(schema, baseType, fieldName);
        }
        SimpleTypeImpl simpleType = new SimpleTypeImpl(superType, schema.getName(), name);

        // add constraints/restrictions to the simple type
        if (type instanceof RestrictionSimpleTypeImpl) {
            RestrictionSimpleTypeImpl restrictionType = (RestrictionSimpleTypeImpl) type;

            List<Constraint> constraints = new ArrayList<>();

            // pattern
            XSFacet patternFacet = restrictionType.getFacet(FACET_PATTERN);
            if (patternFacet != null) {
                if (simpleType.getPrimitiveType().support(PatternConstraint.class)) {
                    // String pattern
                    String pattern = patternFacet.getValue().toString();
                    Constraint constraint = new PatternConstraint(pattern);
                    constraints.add(constraint);
                } else {
                    logUnsupportedFacetRestriction(schema, fieldName, simpleType, FACET_PATTERN);
                }
            }

            // length
            XSFacet minLengthFacet = restrictionType.getFacet(FACET_MINLENGTH);
            XSFacet maxLengthFacet = restrictionType.getFacet(FACET_MAXLENGTH);
            XSFacet lengthFacet = restrictionType.getFacet(FACET_LENGTH);
            if (maxLengthFacet != null || minLengthFacet != null || lengthFacet != null) {
                if (simpleType.getPrimitiveType().support(LengthConstraint.class)) {
                    // String Length
                    Object min = null, max = null;
                    if (lengthFacet != null) {
                        min = lengthFacet.getValue().toString();
                        max = min;
                    } else {
                        if (minLengthFacet != null) {
                            min = minLengthFacet.getValue();
                        }
                        if (maxLengthFacet != null) {
                            max = maxLengthFacet.getValue();
                        }
                    }
                    Constraint constraint = new LengthConstraint(min, max);
                    constraints.add(constraint);
                } else {
                    logUnsupportedFacetRestriction(schema, fieldName, simpleType, FACET_MINLENGTH, FACET_MAXLENGTH,
                            FACET_LENGTH);
                }
            }

            // Intervals
            XSFacet minExclusiveFacet = restrictionType.getFacet(FACET_MINEXCLUSIVE);
            XSFacet minInclusiveFacet = restrictionType.getFacet(FACET_MININCLUSIVE);
            XSFacet maxExclusiveFacet = restrictionType.getFacet(FACET_MAXEXCLUSIVE);
            XSFacet maxInclusiveFacet = restrictionType.getFacet(FACET_MAXINCLUSIVE);
            if (minExclusiveFacet != null || minInclusiveFacet != null || maxExclusiveFacet != null
                    || maxInclusiveFacet != null) {
                if (simpleType.getPrimitiveType().support(NumericIntervalConstraint.class)) {
                    // Numeric Interval
                    Object min = null, max = null;
                    boolean includingMin = true, includingMax = true;
                    if (minExclusiveFacet != null) {
                        min = minExclusiveFacet.getValue();
                        includingMin = false;
                    } else if (minInclusiveFacet != null) {
                        min = minInclusiveFacet.getValue();
                        includingMin = true;
                    }
                    if (maxExclusiveFacet != null) {
                        max = maxExclusiveFacet.getValue();
                        includingMax = false;
                    } else if (maxInclusiveFacet != null) {
                        max = maxInclusiveFacet.getValue();
                        includingMax = true;
                    }
                    Constraint constraint = new NumericIntervalConstraint(min, includingMin, max, includingMax);
                    constraints.add(constraint);
                } else if (simpleType.getPrimitiveType().support(DateIntervalConstraint.class)) {
                    // Date Interval
                    Object min = null, max = null;
                    boolean includingMin = true, includingMax = true;
                    if (minExclusiveFacet != null) {
                        min = minExclusiveFacet.getValue();
                        includingMin = false;
                    }
                    if (minInclusiveFacet != null) {
                        min = minInclusiveFacet.getValue();
                        includingMin = true;
                    }
                    if (maxExclusiveFacet != null) {
                        max = maxExclusiveFacet.getValue();
                        includingMax = false;
                    }
                    if (maxInclusiveFacet != null) {
                        max = maxInclusiveFacet.getValue();
                        includingMax = true;
                    }
                    Constraint constraint = new DateIntervalConstraint(min, includingMin, max, includingMax);
                    constraints.add(constraint);
                } else {
                    logUnsupportedFacetRestriction(schema, fieldName, simpleType, FACET_MINEXCLUSIVE,
                            FACET_MININCLUSIVE, FACET_MAXEXCLUSIVE, FACET_MAXINCLUSIVE);
                }
            }

            // Enumeration
            List<XSFacet> enumFacets = restrictionType.getFacets("enumeration");
            if (enumFacets != null && enumFacets.size() > 0) {
                if (simpleType.getPrimitiveType().support(EnumConstraint.class)) {
                    // string enumeration
                    List<String> enumValues = new ArrayList<>();
                    for (XSFacet enumFacet : enumFacets) {
                        enumValues.add(enumFacet.getValue().toString());
                    }
                    Constraint constraint = new EnumConstraint(enumValues);
                    constraints.add(constraint);
                } else {
                    logUnsupportedFacetRestriction(schema, fieldName, simpleType, FACET_ENUMERATION);
                }
            }

            String refName = restrictionType.getForeignAttribute(NAMESPACE_CORE_EXTERNAL_REFERENCES,
                    ATTR_CORE_EXTERNAL_REFERENCES);
            Map<String, String> refParameters = new HashMap<>();
            for (ForeignAttributes attr : restrictionType.getForeignAttributes()) {
                for (int index = 0; index < attr.getLength(); index++) {
                    String attrNS = attr.getURI(index);
                    String attrName = attr.getLocalName(index);
                    String attrValue = attr.getValue(index);
                    if (NAMESPACE_CORE_EXTERNAL_REFERENCES.equals(attrNS)) {
                        if (!ATTR_CORE_EXTERNAL_REFERENCES.equals(attrName)) {
                            refParameters.put(attrName, attrValue);
                        }
                    }
                }
            }
            if (refName != null) {
                ObjectResolver resolver = getObjectResolverService().getResolver(refName, refParameters);
                if (resolver != null) {
                    simpleType.setResolver(resolver);
                    constraints.add(new ObjectResolverConstraint(resolver));
                } else {
                    log.info("type of " + fieldName + "|" + type.getName()
                            + " targets ObjectResolver namespace but has no matching resolver registered "
                            + "(please contribute to component : org.nuxeo.ecm.core.schema.ObjectResolverService)");
                }
            }

            simpleType.addConstraints(constraints);
        }

        return simpleType;
    }

    private void logUnsupportedFacetRestriction(Schema schema, String fieldName, SimpleTypeImpl simpleType,
            String... facetNames) {
        StringBuilder msg = new StringBuilder();
        msg.append("schema|field|type : ").append(schema.getName());
        msg.append("|").append(fieldName);
        msg.append("|").append(simpleType.getPrimitiveType());
        msg.append(" following restriction facet are not handled by constraints API for this type :");
        for (String facetName : facetNames) {
            msg.append(facetName).append(" ");
        }
        log.warn(msg.toString());
    }

    protected ListType loadListType(Schema schema, XSListSimpleType type, String fieldName)
            throws TypeBindingException {
        String name = getTypeName(type, fieldName);
        XSType xsItemType = type.getItemType();
        Type itemType;
        if (xsItemType.getTargetNamespace().equals(NS_XSD)) {
            itemType = XSDTypes.getType(xsItemType.getName());
        } else {
            itemType = loadSimpleType(schema, xsItemType, null);
        }
        if (itemType == null) {
            log.error("list item type was not defined -> you should define first the item type");
            return null;
        }
        return new ListTypeImpl(schema.getName(), name, itemType);
    }

    protected Type createComplexType(Schema schema, ComplexType superType, String name, XSContentType content,
            boolean abstractType) throws TypeBindingException {

        ComplexType ct = new ComplexTypeImpl(superType, schema.getName(), name);

        // -------- Workaround - we register now the complex type - to fix
        // recursive references to the same type
        schema.registerType(ct);

        // ------------------------------------------
        XSParticle particle = content.asParticle();
        if (particle == null) {
            // complex type without particle -> may be it contains only
            // attributes -> return it as is
            return ct;
        }
        XSTerm term = particle.getTerm();
        XSModelGroup mg = term.asModelGroup();

        return processModelGroup(schema, superType, name, ct, mg, abstractType);
    }

    protected Type createFakeComplexType(Schema schema, ComplexType superType, String name, XSModelGroup mg)
            throws TypeBindingException {

        ComplexType ct = new ComplexTypeImpl(superType, schema.getName(), name);
        // -------- Workaround - we register now the complex type - to fix
        // recursive references to the same type
        schema.registerType(ct);

        return processModelGroup(schema, superType, name, ct, mg, false);
    }

    protected Type processModelGroup(Schema schema, ComplexType superType, String name, ComplexType ct, XSModelGroup mg,
            boolean abstractType) throws TypeBindingException {
        if (mg == null) {
            // TODO don't know how to handle this for now
            throw new TypeBindingException("unsupported complex type");
        }
        XSParticle[] group = mg.getChildren();
        if (group.length == 0) {
            return null;
        }
        if (group.length == 1 && superType == null && group[0].isRepeated()) {
            // a list ?
            // only convert to list of type is not abstract
            if (!abstractType) {
                return createListType(schema, name, group[0]);
            }
        }
        for (XSParticle child : group) {
            XSTerm term = child.getTerm();
            XSElementDecl element = term.asElementDecl();
            int maxOccur = child.getMaxOccurs().intValue();

            if (element == null) {
                // assume this is a xs:choice group
                // (did not find any other way to detect !
                //
                // => make an aggregation of xs:choice subfields
                if (maxOccur < 0 || maxOccur > 1) {
                    // means this is a list
                    //
                    // first create a fake complex type
                    Type fakeType = createFakeComplexType(schema, superType, name + "#anonymousListItem",
                            term.asModelGroup());
                    // wrap it as a list
                    ListType listType = createListType(schema, name + "#anonymousListType", fakeType, 0, maxOccur);
                    // add the listfield to the current CT
                    String fieldName = ct.getName() + "#anonymousList";
                    ct.addField(fieldName, listType, null, 0, null);
                } else {
                    processModelGroup(schema, superType, name, ct, term.asModelGroup(), abstractType);
                }
            } else {
                XSType elementType = element.getType();
                // type could be anonymous
                // concat complex name to enforce inner element type unity across type
                String fieldName = name + '#' + element.getName();
                if (maxOccur < 0 || maxOccur > 1) {
                    Type fieldType = loadType(schema, elementType, fieldName);
                    if (fieldType != null) {
                        ListType listType = createListType(schema, fieldName + "#anonymousListType", fieldType, 0,
                                maxOccur);
                        // add the listfield to the current CT
                        ct.addField(element.getName(), listType, null, 0, null);
                    }
                } else {
                    Type fieldType = loadType(schema, elementType, fieldName);
                    if (fieldType != null) {
                        createField(ct, element, fieldType);
                    }
                }
            }
        }

        // add fields from Parent
        if (superType != null && superType.isComplexType()) {
            for (Field parentField : superType.getFields()) {
                ct.addField(parentField.getName().getLocalName(), parentField.getType(),
                        (String) parentField.getDefaultValue(), 0, null);
            }
        }
        return ct;
    }

    protected ListType createListType(Schema schema, String name, XSParticle particle) throws TypeBindingException {
        XSElementDecl element = particle.getTerm().asElementDecl();
        if (element == null) {
            log.warn("Ignoring " + name + " unsupported list type");
            return null;
        }
        // type could be anonymous
        // concat list name to enforce inner element type unity across type
        Type type = loadType(schema, element.getType(), name + '#' + element.getName());
        if (type == null) {
            log.warn("Unable to find type for " + element.getName());
            return null;
        }

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
        boolean computedNillable = isNillable(element);
        if (computedNillable) {
            flags |= Field.NILLABLE;
        }

        Set<Constraint> constraints = new HashSet<>();
        if (!computedNillable) {
            constraints.add(NotNullConstraint.get());
        }
        if (type instanceof SimpleType) {
            SimpleType st = (SimpleType) type;
            constraints.addAll(st.getConstraints());
        }

        return new ListTypeImpl(schema.getName(), name, type, element.getName(), defValue, flags, constraints,
                particle.getMinOccurs().intValue(), particle.getMaxOccurs().intValue());
    }

    protected static ListType createListType(Schema schema, String name, Type itemType, int min, int max) {
        String elementName = name + "#item";
        return new ListTypeImpl(schema.getName(), name, itemType, elementName, null, min, max);
    }

    protected void loadComplexTypeElement(Schema schema, ComplexType type, XSElementDecl element)
            throws TypeBindingException {
        XSType elementType = element.getType();

        Type fieldType = loadType(schema, elementType, element.getName());
        if (fieldType != null) {
            createField(type, element, fieldType);
        }
    }

    protected static Field createField(ComplexType type, XSElementDecl element, Type fieldType) {
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

        boolean computedNillable = isNillable(element);

        if (computedNillable) {
            flags |= Field.NILLABLE;
        }

        Set<Constraint> constraints = new HashSet<>();
        if (!computedNillable) {
            constraints.add(NotNullConstraint.get());
        }
        if (fieldType instanceof SimpleType) {
            SimpleType st = (SimpleType) fieldType;
            constraints.addAll(st.getConstraints());
        }
        Field field = type.addField(elementName, fieldType, defValue, flags, constraints);

        // set the max field length from the constraints
        if (fieldType instanceof SimpleTypeImpl) {
            LengthConstraint lc = ConstraintUtils.getConstraint(field.getConstraints(), LengthConstraint.class);
            if (lc != null && lc.getMax() != null) {
                field.setMaxLength(lc.getMax().intValue());
            }
        }

        return field;
    }

    protected static Field createField(ComplexType type, XSAttributeDecl element, Type fieldType, boolean isNillable) {
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
        Set<Constraint> constraints = new HashSet<>();
        if (!isNillable) {
            constraints.add(NotNullConstraint.get());
        }
        if (fieldType.isSimpleType()) {
            constraints.addAll(fieldType.getConstraints());
        }
        return type.addField(elementName, fieldType, defValue, flags, constraints);
    }

    protected static String getTypeName(XSType type, String fieldName) {
        String typeName = type.getName();
        if (typeName == null || type.isLocal()) {
            return getAnonymousTypeName(type, fieldName);
        } else {
            return typeName;
        }
    }

    protected static String getAnonymousTypeName(XSType type, String fieldName) {
        if (type.isComplexType()) {
            XSElementDecl container = type.asComplexType().getScope();
            String elName = container.getName();
            return elName + ANONYMOUS_TYPE_SUFFIX;
        } else {
            return fieldName + ANONYMOUS_TYPE_SUFFIX;
        }
    }

    public List<String> getReferencedXSD() {
        return referencedXSD;
    }

    /**
     * ignore case where xsd:nillable is recognized as false by xsom (we don't know if it's not specified and we want to
     * preserve a default value to true. Therefore, we provide a custom attribute nxs:nillable to force nillable as
     * false) NB: if xsd:nillable is present and sets to true, deducted value will be true even if nxs:nillable is false
     *
     * @since 7.1
     */
    protected static boolean isNillable(XSElementDecl element) {
        String value = element.getForeignAttribute(NAMESPACE_CORE_VALIDATION, "nillable");
        return element.isNillable() || value == null || Boolean.parseBoolean(value);
    }

}

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffComplexFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ContentDiffDisplayImpl;
import org.nuxeo.ecm.diff.model.impl.ContentProperty;
import org.nuxeo.ecm.diff.model.impl.ContentPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffDisplayBlockImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldItemDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.PropertyDiffDisplayImpl;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.diff.service.ComplexPropertyHelper;
import org.nuxeo.ecm.diff.service.DiffDisplayService;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetReferenceImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of the {@link DiffDisplayService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class DiffDisplayServiceImpl extends DefaultComponent implements
        DiffDisplayService {

    private static final long serialVersionUID = 6608445970773402827L;

    private static final Log LOGGER = LogFactory.getLog(DiffDisplayServiceImpl.class);

    protected static final String DIFF_DISPLAY_EXTENSION_POINT = "diffDisplay";

    protected static final String DIFF_DEFAULT_DISPLAY_EXTENSION_POINT = "diffDefaultDisplay";

    protected static final String DIFF_BLOCK_EXTENSION_POINT = "diffBlock";

    protected static final String DIFF_WIDGET_CATEGORY = "diff";

    protected static final String DIFF_BLOCK_DEFAULT_TEMPLATE_PATH = "/layouts/layout_diff_template.xhtml";

    protected static final String DIFF_BLOCK_LABEL_PROPERTY_NAME = "label";

    protected static final String DIFF_BLOCK_DEFAULT_LABEL_PREFIX = "label.diffBlock.";

    protected static final String DIFF_WIDGET_LABEL_PREFIX = "label.";

    protected static final String CONTENT_DIFF_LINKS_WIDGET_NAME = "contentDiffLinks";

    protected static final String CONTENT_DIFF_LINKS_WIDGET_NAME_SUFFIX = "_contentDiffLinks";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_VALUE = "value";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_DIFFERENCE_TYPE = "differenceType";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_STYLE_CLASS = "styleClass";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_FILENAME = "filename";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_DISPLAY_HTML_CONVERSION = "displayHtmlConversion";

    protected static final String DIFF_WIDGET_FIELD_DEFINITION_DISPLAY_TEXT_CONVERSION = "displayTextConversion";

    protected static final String DIFF_WIDGET_PROPERTY_DISPLAY_ALL_ITEMS = "displayAllItems";

    protected static final String DIFF_WIDGET_PROPERTY_DISPLAY_ITEM_INDEXES = "displayItemIndexes";

    // TODO: refactor name (not related to widget)
    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD = "index";

    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_TYPE = "int";

    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_LABEL = "label.list.index";

    protected static final String DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD = "value";

    /** Diff excluded fields contributions. */
    protected Map<String, List<String>> diffExcludedFieldsContribs = new HashMap<String, List<String>>();

    /** Diff complex fields contributions. */
    protected Map<String, Map<String, DiffComplexFieldDefinition>> diffComplexFieldsContribs = new HashMap<String, Map<String, DiffComplexFieldDefinition>>();

    /** Diff display contributions. */
    protected Map<String, List<String>> diffDisplayContribs = new HashMap<String, List<String>>();

    /** Diff block contributions. */
    protected Map<String, DiffBlockDefinition> diffBlockContribs = new HashMap<String, DiffBlockDefinition>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (DIFF_DEFAULT_DISPLAY_EXTENSION_POINT.equals(extensionPoint)) {
            if (contribution instanceof DiffExcludedFieldsDescriptor) {
                registerDiffExcludedFields((DiffExcludedFieldsDescriptor) contribution);
            } else if (contribution instanceof DiffComplexFieldDescriptor) {
                registerDiffComplexField((DiffComplexFieldDescriptor) contribution);
            }
        } else if (DIFF_DISPLAY_EXTENSION_POINT.equals(extensionPoint)) {
            if (contribution instanceof DiffDisplayDescriptor) {
                registerDiffDisplay((DiffDisplayDescriptor) contribution);
            }
        } else if (DIFF_BLOCK_EXTENSION_POINT.equals(extensionPoint)) {
            if (contribution instanceof DiffBlockDescriptor) {
                registerDiffBlock((DiffBlockDescriptor) contribution);
            }
        }
        super.registerContribution(contribution, extensionPoint, contributor);
    }

    @Override
    public Map<String, List<String>> getDiffExcludedSchemas() {
        return diffExcludedFieldsContribs;
    }

    @Override
    public List<String> getDiffExcludedFields(String schemaName) {
        return diffExcludedFieldsContribs.get(schemaName);
    }

    @Override
    public List<DiffComplexFieldDefinition> getDiffComplexFields() {
        List<DiffComplexFieldDefinition> diffComplexFields = new ArrayList<DiffComplexFieldDefinition>();
        for (Map<String, DiffComplexFieldDefinition> diffComplexFieldsBySchema : diffComplexFieldsContribs.values()) {
            for (DiffComplexFieldDefinition diffComplexField : diffComplexFieldsBySchema.values()) {
                diffComplexFields.add(diffComplexField);
            }
        }
        return diffComplexFields;
    }

    @Override
    public DiffComplexFieldDefinition getDiffComplexField(String schemaName,
            String fieldName) {
        Map<String, DiffComplexFieldDefinition> diffComplexFieldsBySchema = diffComplexFieldsContribs.get(schemaName);
        if (diffComplexFieldsBySchema != null) {
            return diffComplexFieldsBySchema.get(fieldName);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getDiffDisplays() {
        return diffDisplayContribs;
    }

    @Override
    public List<String> getDiffDisplay(String docType) {
        return diffDisplayContribs.get(docType);

    }

    @Override
    public Map<String, DiffBlockDefinition> getDiffBlockDefinitions() {
        return diffBlockContribs;
    }

    @Override
    public DiffBlockDefinition getDiffBlockDefinition(String name) {
        return diffBlockContribs.get(name);
    }

    @Override
    public List<DiffDisplayBlock> getDiffDisplayBlocks(DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        String leftDocType = leftDoc.getType();
        String rightDocType = rightDoc.getType();
        if (leftDocType.equals(rightDocType)) {
            LOGGER.info(String.format(
                    "The 2 documents have the same type '%s' => looking for a diffDisplay contribution defined for this type or the nearest super type.",
                    leftDocType));
            List<String> diffBlockRefs = getNearestSuperTypeDiffDisplay(leftDocType);
            if (diffBlockRefs != null) {
                LOGGER.info(String.format(
                        "Found a diffDisplay contribution defined for the type '%s' or one of its super type => using it to display the diff.",
                        leftDocType));
                return getDiffDisplayBlocks(
                        getDiffBlockDefinitions(diffBlockRefs), docDiff,
                        leftDoc, rightDoc);
            } else {
                LOGGER.info(String.format(
                        "No diffDisplay contribution was defined for the type '%s' or one of its super type => using default diff display.",
                        leftDocType));
            }
        } else {
            LOGGER.info(String.format(
                    "The 2 documents don't have the same type: '%s'/'%s' => looking for a diffDisplay contribution defined for the nearest common super type.",
                    leftDocType, rightDocType));
            List<String> diffBlockRefs = getNearestSuperTypeDiffDisplay(
                    leftDocType, rightDocType);
            if (diffBlockRefs != null) {
                LOGGER.info(String.format(
                        "Found a diffDisplay contribution defined for a common super type of the types '%s'/'%s' => using it to display the diff.",
                        leftDocType, rightDocType));
                return getDiffDisplayBlocks(
                        getDiffBlockDefinitions(diffBlockRefs), docDiff,
                        leftDoc, rightDoc);
            } else {
                LOGGER.info(String.format(
                        "No diffDisplay contribution was defined for any of the common super types of the types '%s'/'%s' => using default diff display.",
                        leftDocType, rightDocType));
            }
        }
        return getDefaultDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
    }

    public List<DiffDisplayBlock> getDefaultDiffDisplayBlocks(
            DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        return getDiffDisplayBlocks(getDefaultDiffBlockDefinitions(docDiff),
                docDiff, leftDoc, rightDoc);
    }

    protected List<String> getNearestSuperTypeDiffDisplay(String docTypeName)
            throws ClientException {

        List<String> diffDisplay = getDiffDisplay(docTypeName);
        Type docType = getSchemaManager().getDocumentType(docTypeName);
        while (diffDisplay == null && docType != null) {
            Type superType = docType.getSuperType();
            if (superType != null) {
                diffDisplay = getDiffDisplay(superType.getName());
            }
            docType = superType;
        }
        return diffDisplay;
    }

    protected List<String> getNearestSuperTypeDiffDisplay(
            String leftDocTypeName, String rightDocTypeName)
            throws ClientException {

        List<String> diffDisplay = null;

        List<String> leftDocSuperTypeNames = new ArrayList<String>();
        List<String> rightDocSuperTypeNames = new ArrayList<String>();
        leftDocSuperTypeNames.add(leftDocTypeName);
        rightDocSuperTypeNames.add(rightDocTypeName);

        Type leftDocType = getSchemaManager().getDocumentType(leftDocTypeName);
        Type rightDocType = getSchemaManager().getDocumentType(rightDocTypeName);
        if (leftDocType != null && rightDocType != null) {
            Type[] leftDocTypeHierarchy = leftDocType.getTypeHierarchy();
            for (Type type : leftDocTypeHierarchy) {
                leftDocSuperTypeNames.add(type.getName());
            }
            Type[] rightDocTypeHierarchy = rightDocType.getTypeHierarchy();
            for (Type type : rightDocTypeHierarchy) {
                rightDocSuperTypeNames.add(type.getName());
            }
        }

        for (String superTypeName : leftDocSuperTypeNames) {
            if (rightDocSuperTypeNames.contains(superTypeName)) {
                return getNearestSuperTypeDiffDisplay(superTypeName);
            }
        }

        return diffDisplay;
    }

    /**
     * Registers a diff excluded fields contrib.
     */
    protected final void registerDiffExcludedFields(
            DiffExcludedFieldsDescriptor descriptor) {

        String schemaName = descriptor.getSchema();
        if (!StringUtils.isEmpty(schemaName)) {
            boolean enabled = descriptor.isEnabled();
            // Check existing diffExcludedFields contrib for this schema
            List<String> diffExcludedFields = diffExcludedFieldsContribs.get(schemaName);
            if (diffExcludedFields != null) {
                // If !enabled remove contrib
                if (!enabled) {
                    diffExcludedFieldsContribs.remove(schemaName);
                }
                // Else override contrib (no merge)
                // TODO: implement merge
                else {
                    diffExcludedFieldsContribs.put(schemaName,
                            getDiffExcludedFieldRefs(descriptor.getFields()));
                }
            }
            // No existing diffExcludedFields contrib for this
            // schema and enabled => add contrib
            else if (enabled) {
                diffExcludedFieldsContribs.put(schemaName,
                        getDiffExcludedFieldRefs(descriptor.getFields()));
            }
        }
    }

    /**
     * Registers a diff complex field contrib.
     */
    protected final void registerDiffComplexField(
            DiffComplexFieldDescriptor descriptor) {

        String schemaName = descriptor.getSchema();
        String fieldName = descriptor.getName();
        if (!StringUtils.isEmpty(schemaName) && !StringUtils.isEmpty(fieldName)) {
            // Check existing diffComplexField contrib for this schema/field
            DiffComplexFieldDefinition diffComplexField = getDiffComplexField(
                    schemaName, fieldName);
            if (diffComplexField != null) {
                // Override contrib (no merge)
                // TODO: implement merge
                diffComplexFieldsContribs.get(schemaName).put(fieldName,
                        descriptor.getDiffComplexFieldDefinition());
            }
            // No existing diffComplexField contrib for this
            // schema/field => add contrib
            else {
                Map<String, DiffComplexFieldDefinition> diffComplexFieldsBySchema = diffComplexFieldsContribs.get(schemaName);
                if (diffComplexFieldsBySchema == null) {
                    diffComplexFieldsBySchema = new HashMap<String, DiffComplexFieldDefinition>();
                    diffComplexFieldsContribs.put(schemaName,
                            diffComplexFieldsBySchema);
                }
                diffComplexFieldsBySchema.put(fieldName,
                        descriptor.getDiffComplexFieldDefinition());
            }
        }
    }

    /**
     * Registers a diff display contrib.
     *
     * @param contribution the contribution
     */
    protected final void registerDiffDisplay(DiffDisplayDescriptor descriptor) {

        String docType = descriptor.getType();
        if (!StringUtils.isEmpty(docType)) {
            boolean enabled = descriptor.isEnabled();
            // Check existing diffDisplay contrib for this type
            List<String> diffDisplay = diffDisplayContribs.get(docType);
            if (diffDisplay != null) {
                // If !enabled remove contrib
                if (!enabled) {
                    diffDisplayContribs.remove(docType);
                }
                // Else override contrib (no merge)
                // TODO: implement merge
                else {
                    diffDisplayContribs.put(docType,
                            getDiffBlockRefs(descriptor.getDiffBlocks()));
                }
            }
            // No existing diffDisplay contrib for this
            // type and enabled => add contrib
            else if (enabled) {
                diffDisplayContribs.put(docType,
                        getDiffBlockRefs(descriptor.getDiffBlocks()));
            }
        }
    }

    protected final List<String> getDiffBlockRefs(
            List<DiffBlockReferenceDescriptor> diffBlocks) {

        List<String> diffBlockRefs = new ArrayList<String>();
        for (DiffBlockReferenceDescriptor diffBlockRef : diffBlocks) {
            diffBlockRefs.add(diffBlockRef.getName());
        }
        return diffBlockRefs;
    }

    protected final List<String> getDiffExcludedFieldRefs(
            List<DiffFieldDescriptor> diffExcludedFields) {

        List<String> diffExcludedFieldRefs = new ArrayList<String>();
        for (DiffFieldDescriptor diffExcludedFieldRef : diffExcludedFields) {
            diffExcludedFieldRefs.add(diffExcludedFieldRef.getName());
        }
        return diffExcludedFieldRefs;
    }

    protected final void registerDiffBlock(DiffBlockDescriptor descriptor) {

        String diffBlockName = descriptor.getName();
        if (!StringUtils.isEmpty(diffBlockName)) {
            List<DiffFieldDescriptor> fieldDescriptors = descriptor.getFields();
            // No field descriptors => don't take diff block into account.
            if (fieldDescriptors == null || fieldDescriptors.isEmpty()) {
                LOGGER.warn(String.format(
                        "The diffBlock contribution named '%s' has no fields, it won't be taken into account.",
                        diffBlockName));
            } else {
                List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
                // Some field descriptors were found => use them to add the
                // described fields, taking their order into account.
                for (DiffFieldDescriptor fieldDescriptor : fieldDescriptors) {
                    String category = fieldDescriptor.getCategory();
                    String schema = fieldDescriptor.getSchema();
                    String name = fieldDescriptor.getName();
                    boolean displayContentDiffLinks = fieldDescriptor.isDisplayContentDiffLinks();
                    List<DiffFieldItemDescriptor> fieldItemDescriptors = fieldDescriptor.getItems();
                    if (!StringUtils.isEmpty(schema)
                            && !StringUtils.isEmpty(name)) {
                        List<DiffFieldItemDefinition> items = new ArrayList<DiffFieldItemDefinition>();
                        for (DiffFieldItemDescriptor fieldItemDescriptor : fieldItemDescriptors) {
                            items.add(new DiffFieldItemDefinitionImpl(
                                    fieldItemDescriptor.getName(),
                                    fieldItemDescriptor.isDisplayContentDiffLinks()));
                        }
                        fields.add(new DiffFieldDefinitionImpl(category,
                                schema, name, displayContentDiffLinks, items));
                    }
                }
                // TODO: implement merge
                diffBlockContribs.put(
                        diffBlockName,
                        new DiffBlockDefinitionImpl(diffBlockName,
                                descriptor.getTemplates(), fields,
                                descriptor.getProperties()));
            }
        }
    }

    protected final List<DiffBlockDefinition> getDefaultDiffBlockDefinitions(
            DocumentDiff docDiff) throws ClientException {

        List<DiffBlockDefinition> diffBlockDefs = new ArrayList<DiffBlockDefinition>();

        for (String schemaName : docDiff.getSchemaNames()) {
            List<String> diffExcludedFields = getDiffExcludedFields(schemaName);
            // Only add the schema fields if the whole schema is not excluded
            if (diffExcludedFields == null || diffExcludedFields.size() > 0) {
                SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
                List<DiffFieldDefinition> fieldDefs = new ArrayList<DiffFieldDefinition>();
                for (String fieldName : schemaDiff.getFieldNames()) {
                    // Only add the field if it is not excluded
                    if (diffExcludedFields == null
                            || !diffExcludedFields.contains(fieldName)) {
                        List<DiffFieldItemDefinition> fieldItems = new ArrayList<DiffFieldItemDefinition>();
                        DiffComplexFieldDefinition complexFieldDef = getDiffComplexField(
                                schemaName, fieldName);
                        if (complexFieldDef != null) {
                            List<DiffFieldItemDefinition> includedItems = complexFieldDef.getIncludedItems();
                            List<DiffFieldItemDefinition> excludedItems = complexFieldDef.getExcludedItems();
                            // Check included field items
                            if (!CollectionUtils.isEmpty(includedItems)) {
                                fieldItems.addAll(includedItems);
                            }
                            // Check excluded field items
                            else if (!CollectionUtils.isEmpty(excludedItems)) {
                                Field complexField = ComplexPropertyHelper.getField(
                                        schemaName, fieldName);
                                if (complexField.getType().isListType()) {
                                    complexField = ComplexPropertyHelper.getListFieldItem(complexField);
                                }
                                if (!complexField.getType().isComplexType()) {
                                    throw new ClientException(
                                            String.format(
                                                    "Cannot compute field items for [%s:%s] since it is not a complex nor a complex list property.",
                                                    schemaName, fieldName));
                                }
                                List<Field> complexFieldItems = ComplexPropertyHelper.getComplexFieldItems(complexField);
                                for (Field complexFieldItem : complexFieldItems) {
                                    String complexFieldItemName = complexFieldItem.getName().getLocalName();
                                    boolean isFieldItem = true;
                                    for (DiffFieldItemDefinition fieldItemDef : excludedItems) {
                                        if (fieldItemDef.getName().equals(
                                                complexFieldItemName)) {
                                            isFieldItem = false;
                                            break;
                                        }
                                    }
                                    if (isFieldItem) {
                                        fieldItems.add(new DiffFieldItemDefinitionImpl(
                                                complexFieldItemName));
                                    }
                                }
                            }
                        }
                        fieldDefs.add(new DiffFieldDefinitionImpl(
                                DIFF_WIDGET_CATEGORY, schemaName, fieldName,
                                fieldItems));
                    }
                }

                Map<String, String> defaultDiffBlockTemplates = new HashMap<String, String>();
                defaultDiffBlockTemplates.put(BuiltinModes.ANY,
                        DIFF_BLOCK_DEFAULT_TEMPLATE_PATH);

                Map<String, Map<String, Serializable>> defaultDiffBlockProperties = new HashMap<String, Map<String, Serializable>>();
                Map<String, Serializable> labelProperty = new HashMap<String, Serializable>();
                labelProperty.put(DIFF_BLOCK_LABEL_PROPERTY_NAME,
                        DIFF_BLOCK_DEFAULT_LABEL_PREFIX + schemaName);
                defaultDiffBlockProperties.put(BuiltinModes.ANY, labelProperty);

                diffBlockDefs.add(new DiffBlockDefinitionImpl(schemaName,
                        defaultDiffBlockTemplates, fieldDefs,
                        defaultDiffBlockProperties));
            }
        }

        return diffBlockDefs;
    }

    protected final List<DiffBlockDefinition> getDiffBlockDefinitions(
            List<String> diffBlockRefs) {

        List<DiffBlockDefinition> diffBlockDefinitions = new ArrayList<DiffBlockDefinition>();
        for (String diffBlockRef : diffBlockRefs) {
            diffBlockDefinitions.add(getDiffBlockDefinition(diffBlockRef));
        }
        return diffBlockDefinitions;
    }

    protected final List<DiffDisplayBlock> getDiffDisplayBlocks(
            List<DiffBlockDefinition> diffBlockDefinitions,
            DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        List<DiffDisplayBlock> diffDisplayBlocks = new ArrayList<DiffDisplayBlock>();

        for (DiffBlockDefinition diffBlockDef : diffBlockDefinitions) {
            if (diffBlockDef != null) {
                DiffDisplayBlock diffDisplayBlock = getDiffDisplayBlock(
                        diffBlockDef, docDiff, leftDoc, rightDoc);
                if (!diffDisplayBlock.isEmpty()) {
                    diffDisplayBlocks.add(diffDisplayBlock);
                }
            }
        }

        return diffDisplayBlocks;
    }

    protected final DiffDisplayBlock getDiffDisplayBlock(
            DiffBlockDefinition diffBlockDefinition, DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        Map<String, Map<String, PropertyDiffDisplay>> leftValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        Map<String, Map<String, PropertyDiffDisplay>> rightValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();

        List<LayoutRowDefinition> layoutRowDefinitions = new ArrayList<LayoutRowDefinition>();
        List<WidgetDefinition> widgetDefinitions = new ArrayList<WidgetDefinition>();

        List<DiffFieldDefinition> fieldDefinitions = diffBlockDefinition.getFields();
        for (DiffFieldDefinition fieldDefinition : fieldDefinitions) {

            String category = fieldDefinition.getCategory();
            if (StringUtils.isEmpty(category)) {
                category = DIFF_WIDGET_CATEGORY;
            }
            String schemaName = fieldDefinition.getSchema();
            String fieldName = fieldDefinition.getName();
            boolean displayContentDiffLinks = fieldDefinition.isDisplayContentDiffLinks();
            List<DiffFieldItemDefinition> fieldItemDefs = fieldDefinition.getItems();

            SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
            if (schemaDiff != null) {
                PropertyDiff fieldDiff = schemaDiff.getFieldDiff(fieldName);
                if (fieldDiff != null) {

                    Serializable leftProperty = (Serializable) leftDoc.getProperty(
                            schemaName, fieldName);
                    Serializable rightProperty = (Serializable) rightDoc.getProperty(
                            schemaName, fieldName);

                    // Only include field diff if it is significant
                    if (isFieldDiffSignificant(leftProperty, rightProperty)) {

                        String propertyName = getPropertyName(schemaName,
                                fieldName);
                        List<WidgetReference> widgetReferences = new ArrayList<WidgetReference>();

                        // Set property widget definition
                        WidgetDefinition propertyWidgetDefinition = getWidgetDefinition(
                                category, propertyName,
                                fieldDiff.getPropertyType(), null,
                                fieldItemDefs, false);
                        widgetDefinitions.add(propertyWidgetDefinition);
                        // Set property widget ref
                        WidgetReferenceImpl propertyWidgetRef = new WidgetReferenceImpl(
                                category, propertyName);
                        widgetReferences.add(propertyWidgetRef);

                        // Check if must display the content diff links widget
                        if (!displayContentDiffLinks) {
                            for (DiffFieldItemDefinition fieldItemDef : fieldItemDefs) {
                                if (fieldItemDef.isDisplayContentDiffLinks()) {
                                    displayContentDiffLinks = true;
                                    break;
                                }
                            }
                        }
                        // Set content diff links widget definition and ref if
                        // needed
                        if (displayContentDiffLinks) {
                            WidgetDefinition contentDiffLinksWidgetDefinition = getWidgetDefinition(
                                    category, propertyName,
                                    fieldDiff.getPropertyType(), null,
                                    fieldItemDefs, true);
                            widgetDefinitions.add(contentDiffLinksWidgetDefinition);
                            WidgetReferenceImpl contentDiffLinksWidgetRef = new WidgetReferenceImpl(
                                    category,
                                    propertyName
                                            + CONTENT_DIFF_LINKS_WIDGET_NAME_SUFFIX);
                            widgetReferences.add(contentDiffLinksWidgetRef);
                        }

                        // Set layout row definition
                        LayoutRowDefinition layoutRowDefinition = new LayoutRowDefinitionImpl(
                                propertyName, null, widgetReferences, false,
                                true);
                        layoutRowDefinitions.add(layoutRowDefinition);

                        // Set diff display field value
                        boolean isDisplayAllItems = isDisplayAllItems(propertyWidgetDefinition);
                        boolean isDisplayItemIndexes = isDisplayItemIndexes(propertyWidgetDefinition);

                        // Left diff display
                        setFieldDiffDisplay(leftProperty, fieldDiff,
                                isDisplayAllItems, isDisplayItemIndexes,
                                leftValue, schemaName, fieldName, leftDoc,
                                PropertyDiffDisplay.RED_BACKGROUND_STYLE_CLASS);

                        // Right diff display
                        setFieldDiffDisplay(
                                rightProperty,
                                fieldDiff,
                                isDisplayAllItems,
                                isDisplayItemIndexes,
                                rightValue,
                                schemaName,
                                fieldName,
                                rightDoc,
                                PropertyDiffDisplay.GREEN_BACKGROUND_STYLE_CLASS);

                        // Content diff display
                        if (displayContentDiffLinks) {
                            PropertyDiffDisplay contentDiffDisplay = getFieldXPaths(
                                    propertyName, fieldDiff, leftProperty,
                                    rightProperty, isDisplayAllItems,
                                    isDisplayItemIndexes, fieldItemDefs);
                            Map<String, PropertyDiffDisplay> contentDiffSchemaMap = contentDiffValue.get(schemaName);
                            if (contentDiffSchemaMap == null) {
                                contentDiffSchemaMap = new HashMap<String, PropertyDiffDisplay>();
                                contentDiffValue.put(schemaName,
                                        contentDiffSchemaMap);
                            }
                            contentDiffSchemaMap.put(fieldName,
                                    contentDiffDisplay);
                        }
                    }
                }
            }
        }

        // Build layout definition
        LayoutDefinition layoutDefinition = new LayoutDefinitionImpl(
                diffBlockDefinition.getName(),
                diffBlockDefinition.getProperties(),
                diffBlockDefinition.getTemplates(), layoutRowDefinitions,
                widgetDefinitions);

        // Build diff display block
        Map<String, Serializable> diffBlockProperties = diffBlockDefinition.getProperties(BuiltinModes.ANY);
        DiffDisplayBlock diffDisplayBlock = new DiffDisplayBlockImpl(
                (String) diffBlockProperties.get(DIFF_BLOCK_LABEL_PROPERTY_NAME),
                leftValue, rightValue, contentDiffValue, layoutDefinition);

        return diffDisplayBlock;
    }

    /**
     * Checks if the difference between the two specified properties is
     * significant.
     * <p>
     * For example in the case of a date property, checks if the difference is
     * greater than 1 minute since we don't display seconds in the default date
     * widget.
     */
    protected boolean isFieldDiffSignificant(Serializable leftProperty,
            Serializable rightProperty) {

        if (leftProperty instanceof Calendar
                && rightProperty instanceof Calendar) {
            Calendar leftDate = (Calendar) leftProperty;
            Calendar rightDate = (Calendar) rightProperty;
            if (Math.abs(leftDate.getTimeInMillis()
                    - rightDate.getTimeInMillis()) <= 60000) {
                return false;
            }
        }
        return true;
    }

    protected final boolean isDisplayAllItems(WidgetDefinition wDef) {

        // Check 'displayAllItems' widget property
        return getBooleanProperty(wDef, BuiltinModes.ANY,
                DIFF_WIDGET_PROPERTY_DISPLAY_ALL_ITEMS);
    }

    protected final boolean isDisplayItemIndexes(WidgetDefinition wDef) {

        // Check 'displayItemIndexes' widget property
        return getBooleanProperty(wDef, BuiltinModes.ANY,
                DIFF_WIDGET_PROPERTY_DISPLAY_ITEM_INDEXES);
    }

    protected final boolean getBooleanProperty(WidgetDefinition wDef,
            String mode, String property) {

        Map<String, Map<String, Serializable>> props = wDef.getProperties();
        if (props != null) {
            Map<String, Serializable> modeProps = props.get(mode);
            if (modeProps != null) {
                Serializable propertyValue = modeProps.get(property);
                if (propertyValue instanceof String) {
                    return Boolean.parseBoolean((String) propertyValue);
                }
            }
        }
        return false;
    }

    /**
     * Sets the field diff display.
     *
     * @param property the property
     * @param fieldDiff the field diff
     * @param isDisplayAllItems the is display all items
     * @param isDisplayItemIndexes the is display item indexes
     * @param value the value
     * @param schemaName the schema name
     * @param fieldName the field name
     * @param doc the doc
     * @param styleClass the style class
     * @throws ClientException the client exception
     */
    protected void setFieldDiffDisplay(Serializable property,
            PropertyDiff fieldDiff, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes,
            Map<String, Map<String, PropertyDiffDisplay>> value,
            String schemaName, String fieldName, DocumentModel doc,
            String styleClass) throws ClientException {

        PropertyDiffDisplay fieldDiffDisplay = getFieldDiffDisplay(property,
                fieldDiff, isDisplayAllItems, isDisplayItemIndexes, false,
                styleClass);
        Map<String, PropertyDiffDisplay> schemaMap = value.get(schemaName);
        if (schemaMap == null) {
            schemaMap = new HashMap<String, PropertyDiffDisplay>();
            value.put(schemaName, schemaMap);
        }
        schemaMap.put(fieldName, fieldDiffDisplay);
        // Handle mime type for the note:note property
        putMimeTypeDiffDisplay(schemaName, fieldName, schemaMap, doc);
    }

    protected final void putMimeTypeDiffDisplay(String schemaName,
            String fieldName, Map<String, PropertyDiffDisplay> schemaMap,
            DocumentModel doc) throws ClientException {

        if ("note".equals(schemaName) && "note".equals(fieldName)
                && !schemaMap.containsKey("mime_type")) {
            schemaMap.put("mime_type", new PropertyDiffDisplayImpl(
                    (Serializable) doc.getProperty("note", "mime_type")));
        }
    }

    protected final PropertyDiffDisplay getFieldDiffDisplay(
            Serializable property, PropertyDiff propertyDiff,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes,
            boolean mustApplyStyleClass, String styleClass)
            throws ClientException {

        if (property == null) {
            String fieldDiffDisplayStyleClass = PropertyDiffDisplay.DEFAULT_STYLE_CLASS;
            if (mustApplyStyleClass && propertyDiff != null) {
                fieldDiffDisplayStyleClass = styleClass;
            }
            // TODO: add DifferenceType.removed?
            return new PropertyDiffDisplayImpl(null, fieldDiffDisplayStyleClass);
        }

        // List type
        if (isListType(property)) {
            List<Serializable> listProperty = getListProperty(property);
            return getListFieldDiffDisplay(listProperty,
                    (ListPropertyDiff) propertyDiff, isDisplayAllItems,
                    isDisplayItemIndexes, styleClass);
        }
        // Other types (scalar, complex, content)
        else {
            return getFinalFieldDiffDisplay(property, propertyDiff,
                    mustApplyStyleClass, styleClass);
        }
    }

    protected final PropertyDiffDisplay getFinalFieldDiffDisplay(
            Serializable fieldDiffDisplay, PropertyDiff propertyDiff,
            boolean mustApplyStyleClass, String styleClass)
            throws ClientException {

        String finalFieldDiffDisplayStyleClass = PropertyDiffDisplay.DEFAULT_STYLE_CLASS;
        if (mustApplyStyleClass && propertyDiff != null) {
            finalFieldDiffDisplayStyleClass = styleClass;
        }
        PropertyDiffDisplay finalFieldDiffDisplay;
        if (isComplexType(fieldDiffDisplay)) {
            ComplexPropertyDiff complexPropertyDiff = null;
            if (propertyDiff != null) {
                if (!propertyDiff.isComplexType()) {
                    throw new ClientException(
                            "'fieldDiffDisplay' is of complex type whereas 'propertyDiff' is not, this is inconsistent");
                }
                complexPropertyDiff = (ComplexPropertyDiff) propertyDiff;
            }
            Map<String, Serializable> complexFieldDiffDisplay = getComplexProperty(fieldDiffDisplay);
            for (String complexItemName : complexFieldDiffDisplay.keySet()) {
                PropertyDiff complexItemPropertyDiff = null;
                if (complexPropertyDiff != null) {
                    complexItemPropertyDiff = complexPropertyDiff.getDiff(complexItemName);
                }
                complexFieldDiffDisplay.put(complexItemName,
                // TODO: shouldn't we call getFieldDiffDisplay in case
                // of an embedded list?
                        getFinalFieldDiffDisplay(
                                complexFieldDiffDisplay.get(complexItemName),
                                complexItemPropertyDiff, true, styleClass));
            }
            finalFieldDiffDisplay = new PropertyDiffDisplayImpl(
                    (Serializable) complexFieldDiffDisplay);
        } else if (fieldDiffDisplay instanceof Calendar) {
            // TODO: add propertyDiff.getDifferenceType()?
            finalFieldDiffDisplay = new PropertyDiffDisplayImpl(
                    ((Calendar) fieldDiffDisplay).getTime(),
                    finalFieldDiffDisplayStyleClass);
        } else {
            // TODO: add propertyDiff.getDifferenceType()?
            finalFieldDiffDisplay = new PropertyDiffDisplayImpl(
                    fieldDiffDisplay, finalFieldDiffDisplayStyleClass);
        }
        return finalFieldDiffDisplay;
    }

    /**
     * Gets the list field diff display.
     *
     * @param listProperty the list property
     * @param listPropertyDiff the list property diff
     * @param isDisplayAllItems the is display all items
     * @param isDisplayItemIndexes the is display item indexes
     * @param styleClass the style class
     * @return the list field diff display
     * @throws ClientException the client exception
     */
    protected final PropertyDiffDisplay getListFieldDiffDisplay(
            List<Serializable> listProperty, ListPropertyDiff listPropertyDiff,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes,
            String styleClass) throws ClientException {

        // Get list property indexes
        // By default: only items that are different (ie. held by the
        // propertyDiff)
        List<Integer> listPropertyIndexes = new ArrayList<Integer>();
        if (isDisplayAllItems) {
            // All items
            for (int index = 0; index < listProperty.size(); index++) {
                listPropertyIndexes.add(index);
            }
        } else {
            if (listPropertyDiff != null) {
                listPropertyIndexes = listPropertyDiff.getDiffIndexes();
            }
        }

        return getComplexListFieldDiffDisplay(listProperty,
                listPropertyIndexes, listPropertyDiff, isDisplayAllItems,
                isDisplayItemIndexes, styleClass);
    }

    protected final PropertyDiffDisplay getComplexListFieldDiffDisplay(
            List<Serializable> listProperty, List<Integer> listPropertyIndexes,
            ListPropertyDiff listPropertyDiff, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes, String styleClass)
            throws ClientException {

        if (listPropertyIndexes.isEmpty()) {
            // TODO: add differenceType?
            return new PropertyDiffDisplayImpl(new ArrayList<Serializable>());
        }
        boolean isComplexListWidget = isDisplayItemIndexes
                || (listPropertyDiff != null && listPropertyDiff.isComplexListType());

        if (isComplexListWidget) {
            List<Map<String, Serializable>> listFieldDiffDisplay = new ArrayList<Map<String, Serializable>>();
            Set<String> complexPropertyItemNames = null;
            for (int index : listPropertyIndexes) {

                Map<String, Serializable> listItemDiffDisplay = new HashMap<String, Serializable>();
                // Put item index if wanted
                if (isDisplayItemIndexes) {
                    listItemDiffDisplay.put(
                            DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD, index + 1);
                }
                // Only put value if index is in list range
                if (index < listProperty.size()) {
                    Serializable listPropertyItem = listProperty.get(index);
                    PropertyDiff listItemPropertyDiff = null;
                    if (listPropertyDiff != null) {
                        listItemPropertyDiff = listPropertyDiff.getDiff(index);
                    }
                    if (isComplexType(listPropertyItem)) { // Complex
                                                           // list
                        ComplexPropertyDiff complexPropertyDiff = null;
                        if (listItemPropertyDiff != null
                                && listItemPropertyDiff.isComplexType()) {
                            complexPropertyDiff = (ComplexPropertyDiff) listItemPropertyDiff;
                        }
                        Map<String, Serializable> complexProperty = getComplexProperty(listPropertyItem);
                        complexPropertyItemNames = complexProperty.keySet();
                        for (String complexPropertyItemName : complexPropertyItemNames) {
                            Serializable complexPropertyItem = complexProperty.get(complexPropertyItemName);
                            // TODO: take into account subwidget properties
                            // 'displayAllItems' and 'displayItemIndexes'
                            // instead of inheriting them from the parent
                            // widget.
                            PropertyDiff complexItemPropertyDiff = null;
                            if (complexPropertyDiff != null) {
                                complexItemPropertyDiff = complexPropertyDiff.getDiff(complexPropertyItemName);
                            }
                            listItemDiffDisplay.put(
                                    complexPropertyItemName,
                                    getFieldDiffDisplay(complexPropertyItem,
                                            complexItemPropertyDiff,
                                            isDisplayAllItems,
                                            isDisplayItemIndexes, true,
                                            styleClass));
                        }
                    } else { // Scalar or content list
                        listItemDiffDisplay.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                getFinalFieldDiffDisplay(listPropertyItem,
                                        listItemPropertyDiff,
                                        isDisplayAllItems, styleClass));
                    }
                } else {// Index not in list range => put null value
                    if (complexPropertyItemNames != null) {
                        for (String complexPropertyItemName : complexPropertyItemNames) {
                            // TODO: add DifferenceType.removed?
                            listItemDiffDisplay.put(complexPropertyItemName,
                                    new PropertyDiffDisplayImpl(null,
                                            styleClass));
                        }
                    } else {
                        // TODO: add DifferenceType.removed?
                        listItemDiffDisplay.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                new PropertyDiffDisplayImpl(
                                        null,
                                        isDisplayAllItems ? styleClass
                                                : PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
                    }
                }
                listFieldDiffDisplay.add(listItemDiffDisplay);
            }
            return new PropertyDiffDisplayImpl(
                    (Serializable) listFieldDiffDisplay);
        } else {
            List<Serializable> listFieldDiffDisplay = new ArrayList<Serializable>();
            for (int index : listPropertyIndexes) {
                // Only put value if index is in list range
                if (index < listProperty.size()) {
                    PropertyDiff listItemPropertyDiff = null;
                    if (listPropertyDiff != null) {
                        listItemPropertyDiff = listPropertyDiff.getDiff(index);
                    }
                    listFieldDiffDisplay.add(getFinalFieldDiffDisplay(
                            listProperty.get(index), listItemPropertyDiff,
                            isDisplayAllItems, styleClass));
                } else {// Index not in list range => put null value
                    // TODO: add DifferenceType.removed?
                    listFieldDiffDisplay.add(new PropertyDiffDisplayImpl(null,
                            isDisplayAllItems ? styleClass
                                    : PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
                }
            }
            return new PropertyDiffDisplayImpl(
                    (Serializable) listFieldDiffDisplay);
        }
    }

    protected final PropertyDiffDisplay getFieldXPaths(String propertyName,
            PropertyDiff propertyDiff, Serializable leftProperty,
            Serializable rightProperty, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes,
            List<DiffFieldItemDefinition> complexFieldItemDefs)
            throws ClientException {

        PropertyDiffDisplay fieldXPaths = null;
        if (propertyDiff == null) {
            throw new ClientException(
                    "The 'propertyDiff' parameter cannot be null.");
        }

        boolean isDisplayHtmlConversion = ContentDiffHelper.isDisplayHtmlConversion(leftProperty)
                && ContentDiffHelper.isDisplayHtmlConversion(rightProperty);
        boolean isDisplayTextConversion = ContentDiffHelper.isDisplayTextConversion(leftProperty)
                && ContentDiffHelper.isDisplayTextConversion(rightProperty);

        // Simple type
        if (propertyDiff.isSimpleType()) {
            SimplePropertyDiff simplePropertyDiff = (SimplePropertyDiff) propertyDiff;
            // Keep fieldXPaths null if one of the left or right properties is
            // empty
            if (!StringUtils.isEmpty(simplePropertyDiff.getLeftValue())
                    && !StringUtils.isEmpty(simplePropertyDiff.getRightValue())) {
                fieldXPaths = new ContentDiffDisplayImpl(propertyName,
                        simplePropertyDiff.getDifferenceType(),
                        isDisplayHtmlConversion, isDisplayTextConversion);
            }
        }
        // Content type
        else if (propertyDiff.isContentType()) {
            ContentPropertyDiff contentPropertyDiff = (ContentPropertyDiff) propertyDiff;
            ContentProperty leftContent = contentPropertyDiff.getLeftContent();
            ContentProperty rightContent = contentPropertyDiff.getRightContent();
            // Keep fieldXPaths null if one of the left or right properties is
            // empty
            if (leftContent != null
                    && rightContent != null
                    && (!StringUtils.isEmpty(leftContent.getFilename()) && !StringUtils.isEmpty(rightContent.getFilename()))
                    || (!StringUtils.isEmpty(leftContent.getDigest()) && !StringUtils.isEmpty(rightContent.getDigest()))) {
                fieldXPaths = new ContentDiffDisplayImpl(propertyName,
                        contentPropertyDiff.getDifferenceType(),
                        isDisplayHtmlConversion, isDisplayTextConversion);
            }
        }
        // Complex type
        else if (propertyDiff.isComplexType()) {

            Map<String, Serializable> leftComplexProperty = getComplexPropertyIfNotNull(leftProperty);
            Map<String, Serializable> rightComplexProperty = getComplexPropertyIfNotNull(rightProperty);

            // TODO (maybe): take into account subwidget properties
            // 'displayAllItems' and 'displayItemIndexes'
            // instead of inheriting them from the parent
            // widget.
            Map<String, PropertyDiff> complexPropertyDiffMap = ((ComplexPropertyDiff) propertyDiff).getDiffMap();
            Map<String, Serializable> complexPropertyXPaths = new HashMap<String, Serializable>();
            if (CollectionUtils.isEmpty(complexFieldItemDefs)) {
                Iterator<String> complexFieldItemNamesIt = complexPropertyDiffMap.keySet().iterator();
                while (complexFieldItemNamesIt.hasNext()) {
                    String complexFieldItemName = complexFieldItemNamesIt.next();
                    setComplexPropertyXPaths(
                            complexPropertyXPaths,
                            complexFieldItemName,
                            getSubPropertyFullName(propertyName,
                                    complexFieldItemName),
                            complexPropertyDiffMap, leftComplexProperty,
                            rightComplexProperty, isDisplayAllItems,
                            isDisplayItemIndexes);
                }
            } else {
                for (DiffFieldItemDefinition complexFieldItemDef : complexFieldItemDefs) {
                    if (complexFieldItemDef.isDisplayContentDiffLinks()) {
                        String complexFieldItemName = complexFieldItemDef.getName();
                        if (complexPropertyDiffMap.containsKey(complexFieldItemName)) {
                            setComplexPropertyXPaths(
                                    complexPropertyXPaths,
                                    complexFieldItemName,
                                    getSubPropertyFullName(propertyName,
                                            complexFieldItemName),
                                    complexPropertyDiffMap,
                                    leftComplexProperty, rightComplexProperty,
                                    isDisplayAllItems, isDisplayItemIndexes);
                        }
                    }
                }
            }
            fieldXPaths = new ContentDiffDisplayImpl(
                    (Serializable) complexPropertyXPaths);
        }
        // List type
        else {
            List<Serializable> leftListProperty = getListPropertyIfNotNull(leftProperty);
            List<Serializable> rightListProperty = getListPropertyIfNotNull(rightProperty);

            ListPropertyDiff listPropertyDiff = (ListPropertyDiff) propertyDiff;

            // Get list property indexes
            // By default: only items that are different (ie. held by the
            // propertyDiff)
            List<Integer> listPropertyIndexes = new ArrayList<Integer>();
            if (isDisplayAllItems) {
                // All items
                int listPropertySize = Math.min(leftListProperty.size(),
                        rightListProperty.size());

                for (int index = 0; index < listPropertySize; index++) {
                    listPropertyIndexes.add(index);
                }
            } else {
                listPropertyIndexes = listPropertyDiff.getDiffIndexes();
            }
            fieldXPaths = getComplexListXPaths(propertyName,
                    listPropertyIndexes, listPropertyDiff, leftListProperty,
                    rightListProperty, isDisplayAllItems, isDisplayItemIndexes);
        }
        return fieldXPaths;
    }

    protected final PropertyDiffDisplay getComplexListXPaths(
            String propertyName, List<Integer> listPropertyIndexes,
            ListPropertyDiff listPropertyDiff,
            List<Serializable> leftListProperty,
            List<Serializable> rightListProperty, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes) throws ClientException {

        if (listPropertyIndexes.isEmpty()) {
            // TODO: add differenceType?
            return new ContentDiffDisplayImpl(new ArrayList<Serializable>());
        }
        boolean isComplexListWidget = isDisplayItemIndexes
                || (listPropertyDiff != null && listPropertyDiff.isComplexListType());

        if (isComplexListWidget) {
            List<Map<String, Serializable>> listFieldXPaths = new ArrayList<Map<String, Serializable>>();
            for (int index : listPropertyIndexes) {

                Map<String, Serializable> listItemXPaths = new HashMap<String, Serializable>();
                // Put item index if wanted
                if (isDisplayItemIndexes) {
                    listItemXPaths.put(DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD,
                            index + 1);
                }
                PropertyDiff listItemPropertyDiff = listPropertyDiff.getDiff(index);
                if (listItemPropertyDiff != null) {

                    Serializable leftListPropertyItem = null;
                    Serializable rightListPropertyItem = null;
                    if (index < leftListProperty.size()) {
                        leftListPropertyItem = leftListProperty.get(index);
                    }
                    if (index < rightListProperty.size()) {
                        rightListPropertyItem = rightListProperty.get(index);
                    }
                    Map<String, Serializable> leftComplexProperty = null;
                    Map<String, Serializable> rightComplexProperty = null;
                    if (isComplexType(leftListPropertyItem)) {
                        leftComplexProperty = getComplexProperty(leftListPropertyItem);
                    }
                    if (isComplexType(rightListPropertyItem)) {
                        rightComplexProperty = getComplexProperty(rightListPropertyItem);
                    }

                    // Complex list
                    if (listItemPropertyDiff.isComplexType()) {
                        Map<String, PropertyDiff> complexPropertyDiffMap = ((ComplexPropertyDiff) listItemPropertyDiff).getDiffMap();
                        Iterator<String> complexPropertyItemNamesIt = complexPropertyDiffMap.keySet().iterator();
                        while (complexPropertyItemNamesIt.hasNext()) {
                            String complexPropertyItemName = complexPropertyItemNamesIt.next();
                            // TODO: take into account subwidget properties
                            // 'displayAllItems' and 'displayItemIndexes'
                            // instead of inheriting them from the parent
                            // widget.
                            setComplexPropertyXPaths(
                                    listItemXPaths,
                                    complexPropertyItemName,
                                    getSubPropertyFullName(
                                            propertyName,
                                            getSubPropertyFullName(
                                                    String.valueOf(index),
                                                    complexPropertyItemName)),
                                    complexPropertyDiffMap,
                                    leftComplexProperty, rightComplexProperty,
                                    isDisplayAllItems, isDisplayItemIndexes);
                        }
                    }
                    // Scalar or content list
                    else {
                        String listItemXPath = null;
                        // Keep listItemXPath null if one of the left or right
                        // properties is empty
                        if (leftListPropertyItem != null
                                && rightListPropertyItem != null) {
                            listItemXPath = getSubPropertyFullName(
                                    propertyName, String.valueOf(index));
                        }
                        boolean isDisplayHtmlConversion = ContentDiffHelper.isDisplayHtmlConversion(leftListPropertyItem)
                                && ContentDiffHelper.isDisplayHtmlConversion(rightListPropertyItem);
                        boolean isDisplayTextConversion = ContentDiffHelper.isDisplayTextConversion(leftListPropertyItem)
                                && ContentDiffHelper.isDisplayTextConversion(rightListPropertyItem);
                        listItemXPaths.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                new ContentDiffDisplayImpl(
                                        listItemXPath,
                                        listItemPropertyDiff.getDifferenceType(),
                                        isDisplayHtmlConversion,
                                        isDisplayTextConversion));
                    }
                }
                listFieldXPaths.add(listItemXPaths);
            }
            return new ContentDiffDisplayImpl((Serializable) listFieldXPaths);
        } else {
            List<PropertyDiffDisplay> listFieldXPaths = new ArrayList<PropertyDiffDisplay>();
            for (int index : listPropertyIndexes) {
                PropertyDiffDisplay listItemXPath = null;
                // Keep listItemXPath null if one of the left or right
                // properties is empty
                if (index < leftListProperty.size()
                        && index < rightListProperty.size()) {
                    PropertyDiff listItemPropertyDiff = null;
                    if (listPropertyDiff != null) {
                        listItemPropertyDiff = listPropertyDiff.getDiff(index);
                    }
                    DifferenceType differenceType = DifferenceType.different;
                    if (listItemPropertyDiff != null) {
                        differenceType = listItemPropertyDiff.getDifferenceType();
                    }
                    Serializable leftListPropertyItem = leftListProperty.get(index);
                    Serializable rightListPropertyItem = rightListProperty.get(index);
                    boolean isDisplayHtmlConversion = ContentDiffHelper.isDisplayHtmlConversion(leftListPropertyItem)
                            && ContentDiffHelper.isDisplayHtmlConversion(rightListPropertyItem);
                    boolean isDisplayTextConversion = ContentDiffHelper.isDisplayTextConversion(leftListPropertyItem)
                            && ContentDiffHelper.isDisplayTextConversion(rightListPropertyItem);
                    listItemXPath = new ContentDiffDisplayImpl(
                            getSubPropertyFullName(propertyName,
                                    String.valueOf(index)), differenceType,
                            isDisplayHtmlConversion, isDisplayTextConversion);
                }
                listFieldXPaths.add(listItemXPath);
            }
            return new ContentDiffDisplayImpl((Serializable) listFieldXPaths);
        }
    }

    /**
     * Sets the complex property xpaths.
     *
     * @param complexPropertyXPaths the complex property xpaths
     * @param complexFieldItemName the complex field item name
     * @param subPropertyFullName the sub property full name
     * @param complexPropertyDiffMap the complex property diff map
     * @param leftComplexProperty the left complex property
     * @param rightComplexProperty the right complex property
     * @param isDisplayAllItems the is display all items
     * @param isDisplayItemIndexes the is display item indexes
     * @throws ClientException the client exception
     */
    protected void setComplexPropertyXPaths(
            Map<String, Serializable> complexPropertyXPaths,
            String complexFieldItemName, String subPropertyFullName,
            Map<String, PropertyDiff> complexPropertyDiffMap,
            Map<String, Serializable> leftComplexProperty,
            Map<String, Serializable> rightComplexProperty,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes)
            throws ClientException {

        Serializable leftComplexPropertyItemValue = null;
        Serializable rightComplexPropertyItemValue = null;
        if (leftComplexProperty != null) {
            leftComplexPropertyItemValue = leftComplexProperty.get(complexFieldItemName);
        }
        if (rightComplexProperty != null) {
            rightComplexPropertyItemValue = rightComplexProperty.get(complexFieldItemName);
        }
        complexPropertyXPaths.put(
                complexFieldItemName,
                getFieldXPaths(subPropertyFullName,
                        complexPropertyDiffMap.get(complexFieldItemName),
                        leftComplexPropertyItemValue,
                        rightComplexPropertyItemValue, isDisplayAllItems,
                        isDisplayItemIndexes, null));
    }

    protected boolean isListType(Serializable property) {

        return property instanceof List<?>
                || property instanceof Serializable[];
    }

    protected boolean isComplexType(Serializable property) {

        return property instanceof Map<?, ?>;
    }

    /**
     * Casts or convert a {@link Serializable} property to {@link List
     * <Serializable>}.
     *
     * @param property the property
     * @return the list property
     * @throws ClassCastException if the {@code property} is not a {@link List
     *             <Serializable>} nor an array.
     */
    @SuppressWarnings("unchecked")
    protected List<Serializable> getListProperty(Serializable property) {
        List<Serializable> listProperty;
        if (property instanceof List<?>) { // List
            listProperty = (List<Serializable>) property;
        } else { // Array
            listProperty = Arrays.asList((Serializable[]) property);
        }
        return listProperty;
    }

    /**
     * Gets the list property if the {@code property} is not null.
     *
     * @param property the property
     * @return the list property if the {@code property} is not null, null
     *         otherwise
     * @throws ClientException if the {@code property} is not a list.
     */
    protected List<Serializable> getListPropertyIfNotNull(Serializable property)
            throws ClientException {

        if (property != null) {
            if (!isListType(property)) {
                throw new ClientException(
                        "Tryed to get a list property from a Serializable property that is not a list, this is inconsistent.");
            }
            return getListProperty(property);
        }
        return null;
    }

    /**
     * Casts a {@link Serializable} property to {@link Map<String,
     * Serializable>}.
     *
     * @param property the property
     * @return the complex property
     * @throws ClassCastException if the {@code property} is not a {@link Map
     *             <String, Serializable>}.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Serializable> getComplexProperty(Serializable property) {
        return (Map<String, Serializable>) property;
    }

    /**
     * Gets the complex property if the {@code property} is not null.
     *
     * @param property the property
     * @return the complex property if the {@code property} is not null, null
     *         otherwise
     * @throws ClientException if the {@code property} is not a list.
     */
    protected Map<String, Serializable> getComplexPropertyIfNotNull(
            Serializable property) throws ClientException {

        if (property != null) {
            if (!isComplexType(property)) {
                throw new ClientException(
                        "Tryed to get a complex property from a Serializable property that is not a map, this is inconsistent.");
            }
            return getComplexProperty(property);
        }
        return null;
    }

    // TODO: better separate regular and contentDiffLinksWidget cases (call
    // submethods)
    protected final WidgetDefinition getWidgetDefinition(String category,
            String propertyName, String propertyType, Field field,
            List<DiffFieldItemDefinition> complexFieldItemDefs,
            boolean isContentDiffLinksWidget) throws ClientException {

        boolean isGeneric = false;
        boolean isCloned = false;

        WidgetDefinition wDef = null;
        if (!isContentDiffLinksWidget) {
            // Look for a specific widget in the "diff" category named with the
            // property name
            wDef = getLayoutStore().getWidgetDefinition(category, propertyName);
            if (wDef == null) {
                isGeneric = true;
                // Fallback on a generic widget in the "diff" category named
                // with the property type
                wDef = getLayoutStore().getWidgetDefinition(category,
                        propertyType);
                if (wDef == null) {
                    throw new ClientException(
                            String.format(
                                    "Could not find any specific widget named '%s', nor any generic widget named '%s'. Please make sure at least a generic widget is defined for this type.",
                                    propertyName, propertyType));
                }
            }
        } else {
            isGeneric = true;
            if (PropertyType.isSimpleType(propertyType)
                    || PropertyType.isContentType(propertyType)) {
                wDef = getLayoutStore().getWidgetDefinition(category,
                        CONTENT_DIFF_LINKS_WIDGET_NAME);
                if (wDef == null) {
                    throw new ClientException(
                            String.format(
                                    "Could not find any generic widget named '%s'. Please make sure a generic widget is defined with this name.",
                                    CONTENT_DIFF_LINKS_WIDGET_NAME));
                }
            } else {
                // Get the generic widget in the "diff" category named with
                // the property type
                wDef = getLayoutStore().getWidgetDefinition(category,
                        propertyType);
                if (wDef == null) {
                    throw new ClientException(
                            String.format(
                                    "Could not find any generic widget named '%s'. Please make sure a generic widget is defined for this type.",
                                    propertyType));
                }
            }
        }

        if (isGeneric) {
            // Clone widget definition
            wDef = wDef.clone();
            isCloned = true;
            // Set widget name
            String widgetName = propertyName;
            if (isContentDiffLinksWidget) {
                widgetName += CONTENT_DIFF_LINKS_WIDGET_NAME_SUFFIX;
            }
            wDef.setName(widgetName);

            // Set labels
            Map<String, String> labels = new HashMap<String, String>();
            labels.put(BuiltinModes.ANY, DIFF_WIDGET_LABEL_PREFIX
                    + getPropertyLabel(propertyName));
            wDef.setLabels(labels);

            // Set translated
            wDef.setTranslated(true);
        }

        // Set field definitions if generic or specific and not already set in
        // widget definition
        if (isGeneric || !isFieldDefinitions(wDef)) {

            String fieldName = propertyName;
            if (field != null) {
                fieldName = field.getName().getLocalName();
            }

            FieldDefinition[] fieldDefinitions;
            if (isContentDiffLinksWidget) {
                fieldDefinitions = new FieldDefinition[4];
                fieldDefinitions[0] = new FieldDefinitionImpl(null,
                        getFieldDefinition(fieldName,
                                DIFF_WIDGET_FIELD_DEFINITION_VALUE));
                fieldDefinitions[1] = new FieldDefinitionImpl(null,
                        getFieldDefinition(fieldName,
                                DIFF_WIDGET_FIELD_DEFINITION_DIFFERENCE_TYPE));
                fieldDefinitions[2] = new FieldDefinitionImpl(
                        null,
                        getFieldDefinition(fieldName,
                                DIFF_WIDGET_FIELD_DEFINITION_DISPLAY_HTML_CONVERSION));
                fieldDefinitions[3] = new FieldDefinitionImpl(
                        null,
                        getFieldDefinition(fieldName,
                                DIFF_WIDGET_FIELD_DEFINITION_DISPLAY_TEXT_CONVERSION));
            } else {
                int fieldCount = 2;
                if (PropertyType.isContentType(propertyType)
                        || ("note:note".equals(propertyName))) {
                    fieldCount = 3;
                }
                fieldDefinitions = new FieldDefinition[fieldCount];
                fieldDefinitions[0] = new FieldDefinitionImpl(null,
                        getFieldDefinition(fieldName,
                                DIFF_WIDGET_FIELD_DEFINITION_VALUE));

                FieldDefinition styleClassFieldDef = new FieldDefinitionImpl(
                        null, getFieldDefinition(fieldName,
                                DIFF_WIDGET_FIELD_DEFINITION_STYLE_CLASS));
                if (PropertyType.isContentType(propertyType)) {
                    fieldDefinitions[1] = new FieldDefinitionImpl(
                            null,
                            getFieldDefinition(
                                    getFieldDefinition(fieldName,
                                            DIFF_WIDGET_FIELD_DEFINITION_VALUE),
                                    DIFF_WIDGET_FIELD_DEFINITION_FILENAME));
                    fieldDefinitions[2] = styleClassFieldDef;
                } else if ("note:note".equals(propertyName)) {
                    fieldDefinitions[1] = new FieldDefinitionImpl(null,
                            getFieldDefinition("note:mime_type",
                                    DIFF_WIDGET_FIELD_DEFINITION_VALUE));
                    fieldDefinitions[2] = styleClassFieldDef;
                } else {
                    fieldDefinitions[1] = styleClassFieldDef;
                }
            }

            // Clone if needed
            if (!isCloned) {
                wDef = wDef.clone();
                isCloned = true;
            }
            wDef.setFieldDefinitions(fieldDefinitions);
        }

        // Set subwidgets if not already set
        if (!isSubWidgets(wDef)) {
            if (PropertyType.isListType(propertyType)
                    || PropertyType.isComplexType(propertyType)) {

                Field declaringField = field;
                if (declaringField == null) {
                    declaringField = ComplexPropertyHelper.getField(
                            getPropertySchema(propertyName),
                            getPropertyField(propertyName));
                }
                // Clone if needed
                if (!isCloned) {
                    wDef = wDef.clone();
                    isCloned = true;
                }
                wDef.setSubWidgetDefinitions(getSubWidgetDefinitions(category,
                        propertyName, propertyType, declaringField,
                        complexFieldItemDefs, isDisplayItemIndexes(wDef),
                        isContentDiffLinksWidget));
            }
        }

        return wDef;
    }

    protected final boolean isSubWidgets(WidgetDefinition wDef) {

        WidgetDefinition[] subWidgetDefs = wDef.getSubWidgetDefinitions();
        return subWidgetDefs != null && subWidgetDefs.length > 0;
    }

    protected final boolean isFieldDefinitions(WidgetDefinition wDef) {

        FieldDefinition[] fieldDefs = wDef.getFieldDefinitions();
        return fieldDefs != null && fieldDefs.length > 0;
    }

    protected final String getFieldDefinition(String fieldName,
            String subPropertyName) {

        return fieldName + "/" + subPropertyName;
    }

    protected final WidgetDefinition[] getSubWidgetDefinitions(String category,
            String propertyName, String propertyType, Field field,
            List<DiffFieldItemDefinition> complexFieldItemDefs,
            boolean isDisplayItemIndexes, boolean isContentDiffLinks)
            throws ClientException {

        WidgetDefinition[] subWidgetDefs = null;
        // Complex
        if (PropertyType.isComplexType(propertyType)) {
            subWidgetDefs = getComplexSubWidgetDefinitions(category,
                    propertyName, field, complexFieldItemDefs, false,
                    isContentDiffLinks);
        }
        // Scalar or content list
        else if (PropertyType.isScalarListType(propertyType)
                || PropertyType.isContentListType(propertyType)) {
            Field listFieldItem = ComplexPropertyHelper.getListFieldItem(field);
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes, 1);
            subWidgetDefs[subWidgetDefs.length - 1] = getWidgetDefinition(
                    category,
                    getSubPropertyFullName(propertyName,
                            listFieldItem.getName().getLocalName()),
                    ComplexPropertyHelper.getFieldType(listFieldItem),
                    new FieldImpl(new QName(
                            DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD),
                            field.getType(), listFieldItem.getType()), null,
                    isContentDiffLinks);
        }
        // Complex list
        else if (PropertyType.isComplexListType(propertyType)) {
            Field listFieldItem = ComplexPropertyHelper.getListFieldItem(field);
            subWidgetDefs = getComplexSubWidgetDefinitions(category,
                    propertyName, listFieldItem, complexFieldItemDefs,
                    isDisplayItemIndexes, isContentDiffLinks);
        }
        return subWidgetDefs;
    }

    protected final WidgetDefinition[] getComplexSubWidgetDefinitions(
            String category, String propertyName, Field field,
            List<DiffFieldItemDefinition> complexFieldItemDefs,
            boolean isDisplayItemIndexes, boolean isContentDiffLinks)
            throws ClientException {

        WidgetDefinition[] subWidgetDefs;
        int subWidgetIndex = isDisplayItemIndexes ? 1 : 0;

        if (CollectionUtils.isEmpty(complexFieldItemDefs)) {
            List<Field> complexFieldItems = ComplexPropertyHelper.getComplexFieldItems(field);
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes,
                    complexFieldItems.size());

            for (Field complexFieldItem : complexFieldItems) {
                subWidgetDefs[subWidgetIndex] = getWidgetDefinition(
                        category,
                        getSubPropertyFullName(propertyName,
                                complexFieldItem.getName().getLocalName()),
                        ComplexPropertyHelper.getFieldType(complexFieldItem),
                        complexFieldItem, null, isContentDiffLinks);
                subWidgetIndex++;
            }
        } else {
            int subWidgetCount = complexFieldItemDefs.size();
            // Only add a subwidget for the items marked to display the content
            // diff links
            if (isContentDiffLinks) {
                subWidgetCount = 0;
                for (DiffFieldItemDefinition complexFieldItemDef : complexFieldItemDefs) {
                    if (complexFieldItemDef.isDisplayContentDiffLinks()) {
                        subWidgetCount++;
                    }
                }
            }
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes,
                    subWidgetCount);

            for (DiffFieldItemDefinition complexFieldItemDef : complexFieldItemDefs) {
                if (!isContentDiffLinks
                        || complexFieldItemDef.isDisplayContentDiffLinks()) {
                    String complexFieldItemName = complexFieldItemDef.getName();
                    Field complexFieldItem = ComplexPropertyHelper.getComplexFieldItem(
                            field, complexFieldItemName);
                    if (complexFieldItem != null) {
                        subWidgetDefs[subWidgetIndex] = getWidgetDefinition(
                                category,
                                getSubPropertyFullName(propertyName,
                                        complexFieldItemName),
                                ComplexPropertyHelper.getFieldType(complexFieldItem),
                                complexFieldItem, null, isContentDiffLinks);
                        subWidgetIndex++;
                    }
                }
            }
        }
        return subWidgetDefs;
    }

    protected final WidgetDefinition[] initSubWidgetDefinitions(
            boolean isDisplayItemIndexes, int subWidgetCount) {

        WidgetDefinition[] subWidgetDefs;
        if (isDisplayItemIndexes) {
            subWidgetDefs = new WidgetDefinition[subWidgetCount + 1];
            subWidgetDefs[0] = getIndexSubwidgetDefinition();
        } else {
            subWidgetDefs = new WidgetDefinition[subWidgetCount];
        }

        return subWidgetDefs;
    }

    protected final WidgetDefinition getIndexSubwidgetDefinition() {

        FieldDefinition[] fieldDefinitions = { new FieldDefinitionImpl(null,
                DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD) };

        return new WidgetDefinitionImpl(DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD,
                DIFF_LIST_WIDGET_INDEX_SUBWIDGET_TYPE,
                DIFF_LIST_WIDGET_INDEX_SUBWIDGET_LABEL, null, true, null,
                Arrays.asList(fieldDefinitions), null, null);
    }

    /**
     * Gets the property name.
     *
     * @param schema the schema
     * @param field the field
     * @return the property name
     */
    protected final String getPropertyName(String schema, String field) {

        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(schema)) {
            sb.append(schema);
            sb.append(":");
        }
        sb.append(field);
        return sb.toString();
    }

    protected final String getSubPropertyFullName(String basePropertyName,
            String subPropertyName) {

        if (StringUtils.isEmpty(subPropertyName)) {
            return basePropertyName;
        }
        StringBuilder sb = new StringBuilder(basePropertyName);
        sb.append("/");
        sb.append(subPropertyName);
        return sb.toString();
    }

    protected final String getPropertySchema(String propertyName) {

        int indexOfColon = propertyName.indexOf(':');
        if (indexOfColon > -1) {
            return propertyName.substring(0, indexOfColon);
        }
        return null;
    }

    protected final String getPropertyField(String propertyName) {

        int indexOfColon = propertyName.indexOf(':');
        if (indexOfColon > -1 && indexOfColon < propertyName.length() - 1) {
            return propertyName.substring(indexOfColon + 1);
        }
        return propertyName;
    }

    protected final String getPropertyLabel(String propertyName) {

        return propertyName.replaceAll(":", ".").replaceAll("/", ".");
    }

    /**
     * Gets the schema manager.
     *
     * @return the schema manager
     * @throws ClientException if the schema manager cannot be found
     */
    protected final SchemaManager getSchemaManager() throws ClientException {

        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        if (schemaManager == null) {
            throw new ClientException("SchemaManager service is null.");
        }
        return schemaManager;
    }

    /**
     * Gets the layout store service.
     *
     * @return the layout store service
     * @throws ClientException if the layout store service cannot be found
     */
    protected final LayoutStore getLayoutStore() throws ClientException {

        LayoutStore layoutStore;
        try {
            layoutStore = Framework.getService(LayoutStore.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        if (layoutStore == null) {
            throw new ClientException("LayoutStore service is null.");
        }
        return layoutStore;
    }
}

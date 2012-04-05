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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.diff.differs.diff_match_patch;
import org.nuxeo.ecm.diff.differs.diff_match_patch.Diff;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffDisplayBlockImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.diff.service.DiffDisplayService;
import org.nuxeo.ecm.diff.web.ComplexPropertyHelper;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Default implementation of the {@link DiffDisplayService}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffDisplayServiceImpl extends DefaultComponent implements
        DiffDisplayService {

    private static final long serialVersionUID = 6608445970773402827L;

    private static final Log LOGGER = LogFactory.getLog(DiffDisplayServiceImpl.class);

    protected static final String DIFF_DISPLAY_EXTENSION_POINT = "diffDisplay";

    protected static final String DIFF_BLOCK_EXTENSION_POINT = "diffBlock";

    protected static final String DIFF_WIDGET_CATEGORY = "diff";

    protected static final String DIFF_WIDGET_LABEL_PREFIX = "label.";

    protected static final String DIFF_WIDGET_PROPERTY_DISPLAY_ALL_ITEMS = "displayAllItems";

    protected static final String DIFF_WIDGET_PROPERTY_DISPLAY_ITEM_INDEXES = "displayItemIndexes";

    // TODO: refactor name (not related to widget)
    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD = "index";

    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_TYPE = "int";

    protected static final String DIFF_LIST_WIDGET_INDEX_SUBWIDGET_LABEL = "label.list.index";

    protected static final String DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD = "value";

    /** Diff display contributions. */
    protected Map<String, List<String>> diffDisplayContribs = new HashMap<String, List<String>>();

    /** Diff block contributions. */
    protected Map<String, DiffBlockDefinition> diffBlockContribs = new HashMap<String, DiffBlockDefinition>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (DIFF_DISPLAY_EXTENSION_POINT.equals(extensionPoint)) {
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

    public Map<String, List<String>> getDiffDisplays() {
        return diffDisplayContribs;
    }

    public List<String> getDiffDisplay(String type) {
        return diffDisplayContribs.get(type);

    }

    public List<String> getDefaultTypeDiffDisplay() {
        return diffDisplayContribs.get(DEFAULT_DIFF_DISPLAY_TYPE);

    }

    public Map<String, DiffBlockDefinition> getDiffBlockDefinitions() {
        return diffBlockContribs;
    }

    public DiffBlockDefinition getDiffBlockDefinition(String name) {
        return diffBlockContribs.get(name);
    }

    public List<DiffDisplayBlock> getDiffDisplayBlocks(DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        String leftDocType = leftDoc.getType();
        String rightDocType = rightDoc.getType();
        if (leftDocType.equals(rightDocType)) {
            LOGGER.info(String.format(
                    "The 2 documents have the same type '%s' => looking for a diffDisplay contribution defined for this type.",
                    leftDocType));
            List<String> diffBlockRefs = getDiffDisplay(leftDocType);
            if (diffBlockRefs != null) {
                LOGGER.info(String.format(
                        "Found a diffDisplay contribution defined for the type '%s' => using it to display the diff.",
                        leftDocType));
                return getDiffDisplayBlocks(
                        getDiffBlockDefinitions(diffBlockRefs), docDiff,
                        leftDoc, rightDoc);
            } else {
                LOGGER.info(String.format(
                        "No diffDisplay contribution was defined for the type '%s' => looking for the default (Document) diffDisplay contribution.",
                        leftDocType));
            }
        } else {
            LOGGER.info(String.format(
                    "The 2 documents don't have the same type ('%s'/'%s') => looking for the default (Document) diffDisplay contribution.",
                    leftDocType, rightDocType));
        }
        return getDefaultDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
    }

    public List<DiffDisplayBlock> getDefaultDiffDisplayBlocks(
            DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        List<String> diffBlockRefs = getDefaultTypeDiffDisplay();
        if (diffBlockRefs != null) {
            LOGGER.info("Found the default (Document) diffDisplay contribution => using it to display the diff.");
            return getDiffDisplayBlocks(getDiffBlockDefinitions(diffBlockRefs),
                    docDiff, leftDoc, rightDoc);
        } else {
            LOGGER.info("The default (Document) diffDisplay contribution was not found => using the document type schemas and fields to display the diff (random schema and field order).");
            return getDiffDisplayBlocks(getRawDiffBlockDefinitions(docDiff),
                    docDiff, leftDoc, rightDoc);
        }
    }

    /**
     * Registers a diff display contrib.
     *
     * @param contribution the contribution
     */
    protected final void registerDiffDisplay(DiffDisplayDescriptor descriptor) {

        String type = descriptor.getType();
        if (!StringUtils.isEmpty(type)) {
            boolean enabled = descriptor.isEnabled();
            // Check existing diffDisplay contrib for this type
            List<String> diffDisplay = diffDisplayContribs.get(type);
            if (diffDisplay != null) {
                // If !enabled remove contrib
                if (!enabled) {
                    diffDisplayContribs.remove(type);
                } else { // Else override contrib (no merge)
                    diffDisplayContribs.put(type,
                            getDiffBlockRefs(descriptor.getDiffBlocks()));
                }
            } else if (enabled) { // No existing diffDisplay contrib for this
                                  // type and enabled => add contrib
                diffDisplayContribs.put(type,
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
                    String schema = fieldDescriptor.getSchema();
                    String name = fieldDescriptor.getName();
                    if (!StringUtils.isEmpty(schema)
                            && !StringUtils.isEmpty(name)) {
                        List<String> items = fieldDescriptor.getItems();
                        fields.add(new DiffFieldDefinitionImpl(schema, name,
                                items));
                    }
                }
                diffBlockContribs.put(
                        diffBlockName,
                        new DiffBlockDefinitionImpl(diffBlockName,
                                descriptor.getLabel(), fields));
            }
        }
    }

    protected final List<DiffBlockDefinition> getRawDiffBlockDefinitions(
            DocumentDiff docDiff) {

        List<DiffBlockDefinition> diffBlockDefs = new ArrayList<DiffBlockDefinition>();

        for (String schemaName : docDiff.getSchemaNames()) {
            SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
            List<DiffFieldDefinition> fieldDefs = new ArrayList<DiffFieldDefinition>();
            for (String fieldName : schemaDiff.getFieldNames()) {
                fieldDefs.add(new DiffFieldDefinitionImpl(schemaName, fieldName));
            }
            diffBlockDefs.add(new DiffBlockDefinitionImpl(schemaName, null,
                    fieldDefs));
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

        Map<String, Map<String, Serializable>> leftValue = new HashMap<String, Map<String, Serializable>>();
        Map<String, Map<String, Serializable>> rightValue = new HashMap<String, Map<String, Serializable>>();
        Map<String, Map<String, Serializable>> detailedDiffValue = new HashMap<String, Map<String, Serializable>>();

        // TODO: manage detailedDiff when needed

        List<LayoutRowDefinition> layoutRowDefinitions = new ArrayList<LayoutRowDefinition>();
        List<WidgetDefinition> widgetDefinitions = new ArrayList<WidgetDefinition>();

        List<DiffFieldDefinition> fieldDefinitions = diffBlockDefinition.getFields();
        for (DiffFieldDefinition fieldDefinition : fieldDefinitions) {

            String schemaName = fieldDefinition.getSchema();
            String fieldName = fieldDefinition.getName();
            List<String> fieldItems = fieldDefinition.getItems();

            SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
            if (schemaDiff != null) {
                PropertyDiff fieldDiff = schemaDiff.getFieldDiff(fieldName);
                if (fieldDiff != null) {

                    String propertyName = getPropertyName(schemaName, fieldName);

                    // Set layout row definition
                    LayoutRowDefinition layoutRowDefinition = new LayoutRowDefinitionImpl(
                            propertyName, propertyName, DIFF_WIDGET_CATEGORY);
                    layoutRowDefinitions.add(layoutRowDefinition);

                    // Set widget definition
                    WidgetDefinition widgetDefinition = getWidgetDefinition(
                            propertyName, fieldDiff.getPropertyType(), null,
                            fieldItems);
                    widgetDefinitions.add(widgetDefinition);

                    // Set diff display field value
                    boolean isDisplayAllItems = isDisplayAllItems(widgetDefinition);
                    boolean isDisplayItemIndexes = isDisplayItemIndexes(widgetDefinition);

                    Serializable leftFieldDiffDisplay = getFieldDiffDisplay(
                            (Serializable) leftDoc.getProperty(schemaName,
                                    fieldName), fieldDiff, isDisplayAllItems,
                            isDisplayItemIndexes);
                    Serializable rightFieldDiffDisplay = getFieldDiffDisplay(
                            (Serializable) rightDoc.getProperty(schemaName,
                                    fieldName), fieldDiff, isDisplayAllItems,
                            isDisplayItemIndexes);

                    String detailedDiffDisplay = null;
                    // TODO: better condition (for example, don't need detailed diff on dates)
                    if (PropertyType.isSimpleType(fieldDiff.getPropertyType())) {
                        SimplePropertyDiff simpleFieldDiff = ((SimplePropertyDiff) fieldDiff);
                        String simpleLeftValue = simpleFieldDiff.getLeftValue();
                        String simpleRightValue = simpleFieldDiff.getRightValue();
                        if (simpleLeftValue != null && simpleRightValue != null) {
                            detailedDiffDisplay = getDetailedDiffDisplay(
                                    simpleLeftValue, simpleRightValue);
                        }
                    }
                    // Left
                    Map<String, Serializable> leftSchemaMap = leftValue.get(schemaName);
                    if (leftSchemaMap == null) {
                        leftSchemaMap = new HashMap<String, Serializable>();
                        leftValue.put(schemaName, leftSchemaMap);
                    }
                    leftSchemaMap.put(fieldName, leftFieldDiffDisplay);
                    // TODO: better manage content (file) and note
                    putFilenameDiffDisplay(schemaName, fieldName,
                            leftSchemaMap, leftFieldDiffDisplay);
                    putMimetypeDiffDisplay(schemaName, fieldName,
                            leftSchemaMap, leftDoc);
                    // Right
                    Map<String, Serializable> rightSchemaMap = rightValue.get(schemaName);
                    if (rightSchemaMap == null) {
                        rightSchemaMap = new HashMap<String, Serializable>();
                        rightValue.put(schemaName, rightSchemaMap);
                    }
                    rightSchemaMap.put(fieldName, rightFieldDiffDisplay);
                    // TODO: better manage content (file)
                    putFilenameDiffDisplay(schemaName, fieldName,
                            rightSchemaMap, rightFieldDiffDisplay);
                    putMimetypeDiffDisplay(schemaName, fieldName,
                            rightSchemaMap, rightDoc);

                    // TODO: manage better detailedDiff if needed
                    // Detailed diff
                    Map<String, Serializable> detailedDiffSchemaMap = detailedDiffValue.get(schemaName);
                    if (detailedDiffSchemaMap == null) {
                        detailedDiffSchemaMap = new HashMap<String, Serializable>();
                        detailedDiffValue.put(schemaName, detailedDiffSchemaMap);
                    }
                    detailedDiffSchemaMap.put(fieldName, detailedDiffDisplay);

                }
            }
        }

        // Build layout definition
        LayoutDefinition layoutDefinition = new LayoutDefinitionImpl(
                diffBlockDefinition.getName(), null, null,
                layoutRowDefinitions, widgetDefinitions);

        // Build diff display block
        DiffDisplayBlock diffDisplayBlock = new DiffDisplayBlockImpl(
                diffBlockDefinition.getLabel(), leftValue, rightValue,
                detailedDiffValue, layoutDefinition);

        return diffDisplayBlock;
    }

    /**
     * @param schemaName
     * @param fieldName
     * @param fieldDiffDisplay
     * @param schemaMap
     */
    // TODO: should not be hardcoded
    protected final void putFilenameDiffDisplay(String schemaName,
            String fieldName, Map<String, Serializable> schemaMap,
            Serializable fieldDiffDisplay) {

        if ("file".equals(schemaName) && "content".equals(fieldName)
                && !schemaMap.containsKey("filename")
                && fieldDiffDisplay instanceof Blob) {
            schemaMap.put("filename", ((Blob) fieldDiffDisplay).getFilename());
        }
    }

    // TODO: should not be hardcoded
    protected final void putMimetypeDiffDisplay(String schemaName,
            String fieldName, Map<String, Serializable> schemaMap,
            DocumentModel doc) throws ClientException {

        if ("note".equals(schemaName) && "note".equals(fieldName)
                && !schemaMap.containsKey("mime_type")) {
            schemaMap.put("mime_type",
                    (Serializable) doc.getProperty("note", "mime_type"));
        }
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

    @SuppressWarnings("unchecked")
    protected final Serializable getFieldDiffDisplay(Serializable property,
            PropertyDiff propertyDiff, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes) throws ClientException {

        if (property == null) {
            return null;
        }

        // List type
        if (isListType(property)) {
            // Cast or convert to List
            List<Serializable> listProperty;
            if (property instanceof List<?>) { // List
                listProperty = (List<Serializable>) property;
            } else { // Array
                listProperty = Arrays.asList((Serializable[]) property);
            }
            return getListFieldDiffDisplay(listProperty,
                    (ListPropertyDiff) propertyDiff, isDisplayAllItems,
                    isDisplayItemIndexes);
        }
        // Other types (scalar, complex, content)
        else {
            return getConvertedFieldDiffDisplay(property);
        }
    }

    protected boolean isListType(Serializable property) {

        return property instanceof List<?>
                || property instanceof Serializable[];
    }

    @SuppressWarnings("unchecked")
    protected final Serializable getConvertedFieldDiffDisplay(
            Serializable fieldDiffDisplay) {

        if (fieldDiffDisplay instanceof Calendar) {
            return ((Calendar) fieldDiffDisplay).getTime();
        } else if (fieldDiffDisplay instanceof Map<?, ?>) {
            Map<String, Serializable> complexFieldDiffDisplay = (Map<String, Serializable>) fieldDiffDisplay;
            for (String complexItemName : complexFieldDiffDisplay.keySet()) {
                complexFieldDiffDisplay.put(
                        complexItemName,
                        getConvertedFieldDiffDisplay(complexFieldDiffDisplay.get(complexItemName)));
            }
        }
        return fieldDiffDisplay;
    }

    /**
     * @param propertyDiff
     * @param wDef
     * @param property
     * @param propertyType
     * @return
     * @throws ClientException
     */
    protected final Serializable getListFieldDiffDisplay(
            List<Serializable> listProperty, ListPropertyDiff listPropertyDiff,
            boolean isDisplayAllItems, boolean isDisplayItemIndexes)
            throws ClientException {

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

        return (Serializable) getComplexListFieldDiffDisplay(listProperty,
                listPropertyIndexes, listPropertyDiff, isDisplayAllItems,
                isDisplayItemIndexes);
    }

    @SuppressWarnings("unchecked")
    protected final Serializable getComplexListFieldDiffDisplay(
            List<Serializable> listProperty, List<Integer> listPropertyIndexes,
            ListPropertyDiff listPropertyDiff, boolean isDisplayAllItems,
            boolean isDisplayItemIndexes) throws ClientException {

        if (listPropertyIndexes.isEmpty()) {
            return new ArrayList<Serializable>();
        }
        boolean isComplexListWidget = isDisplayItemIndexes
                || listProperty.get(0) instanceof Map<?, ?>;

        if (isComplexListWidget) {
            List<Map<String, Serializable>> listFieldDiffDisplay = new ArrayList<Map<String, Serializable>>();
            for (int index : listPropertyIndexes) {

                Map<String, Serializable> listItemDiffDisplay = new HashMap<String, Serializable>();
                // Put item index if wanted
                if (isDisplayItemIndexes) {
                    listItemDiffDisplay.put(
                            DIFF_LIST_WIDGET_INDEX_SUBWIDGET_FIELD, index + 1);
                }
                // Only put value if index is in list range
                if (index < listProperty.size()) {
                    Serializable listPropertyValue = listProperty.get(index);
                    if (listPropertyValue instanceof Map<?, ?>) { // Complex
                                                                  // list
                        ComplexPropertyDiff complexPropertyDiff = null;
                        PropertyDiff listItemPropertyDiff = listPropertyDiff.getDiff(index);
                        if (listItemPropertyDiff != null
                                && listItemPropertyDiff.isComplexType()) {
                            complexPropertyDiff = (ComplexPropertyDiff) listItemPropertyDiff;
                        }
                        Map<String, Serializable> complexListPropertyValue = (Map<String, Serializable>) listPropertyValue;
                        for (String complexListItemPropertyName : complexListPropertyValue.keySet()) {
                            Serializable complexListItem = complexListPropertyValue.get(complexListItemPropertyName);
                            // TODO: take into account subwidget properties
                            // 'displayAllItems' and 'displayItemIndexes'
                            // instead of inheriting them from the parent
                            // widget.
                            PropertyDiff complexListItemPropertyDiff = null;
                            if (complexPropertyDiff != null) {
                                complexListItemPropertyDiff = complexPropertyDiff.getDiff(complexListItemPropertyName);
                            }
                            listItemDiffDisplay.put(
                                    complexListItemPropertyName,
                                    getFieldDiffDisplay(complexListItem,
                                            complexListItemPropertyDiff,
                                            isDisplayAllItems,
                                            isDisplayItemIndexes));
                        }
                    } else { // Scalar or content list
                        listItemDiffDisplay.put(
                                DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD,
                                getConvertedFieldDiffDisplay(listPropertyValue));
                    }
                }
                listFieldDiffDisplay.add(listItemDiffDisplay);
            }
            return (Serializable) listFieldDiffDisplay;
        } else {
            List<Serializable> listFieldDiffDisplay = new ArrayList<Serializable>();
            for (int index : listPropertyIndexes) {
                // Only put value if index is in list range
                if (index < listProperty.size()) {
                    listFieldDiffDisplay.add(getConvertedFieldDiffDisplay(listProperty.get(index)));
                }
            }
            return (Serializable) listFieldDiffDisplay;
        }
    }

    protected final String getDetailedDiffDisplay(String leftValue,
            String rightValue) {

        diff_match_patch dmp = new diff_match_patch();

        LinkedList<Diff> diffs = dmp.diff_main(leftValue, rightValue);
        dmp.diff_cleanupSemantic(diffs);
        return dmp.diff_prettyHtml(diffs);
    }

    protected final WidgetDefinition getWidgetDefinition(String propertyName,
            String propertyType, Field field, List<String> complexFieldItemNames)
            throws ClientException {

        boolean isGeneric = false;
        // Look for a specific widget in the "diff" category named with the
        // property name
        WidgetDefinition wDef = getLayoutStore().getWidgetDefinition(
                DIFF_WIDGET_CATEGORY, propertyName);
        if (wDef == null) {
            isGeneric = true;
            // Fallback on a generic widget in the "diff" category named with
            // the property type
            wDef = getLayoutStore().getWidgetDefinition(DIFF_WIDGET_CATEGORY,
                    propertyType);
            if (wDef == null) {
                throw new ClientException(
                        String.format(
                                "Could not find any specific widget named '%s', nor any generic widget named '%s'. Please make sure at least a generic widget is defined for this type.",
                                propertyName, propertyType));
            }
            // Clone widget definition
            wDef = wDef.clone();
            // Set widget name
            wDef.setName(propertyName);

            // Set labels
            Map<String, String> labels = new HashMap<String, String>();
            labels.put(BuiltinModes.ANY, DIFF_WIDGET_LABEL_PREFIX
                    + getPropertyLabel(propertyName));
            wDef.setLabels(labels);

            // Set translated
            wDef.setTranslated(true);

            // TODO: set props ?
        }

        // Set subwidgets if not already set
        if (!isSubWidgets(wDef)) {
            if (PropertyType.isListType(propertyType)
                    || (PropertyType.isComplexType(propertyType) && !PropertyType.isContentType(propertyType))) {

                Field declaringField = field;
                if (declaringField == null) {
                    declaringField = ComplexPropertyHelper.getField(
                            getPropertySchema(propertyName),
                            getPropertyField(propertyName));
                }
                wDef.setSubWidgetDefinitions(getSubWidgetDefinitions(
                        propertyName, propertyType, declaringField,
                        complexFieldItemNames, isDisplayItemIndexes(wDef)));
            }
        }

        // TODO: better manage specific case of content type
        // filename/content (file and files) and note

        // Set field definitions if generic or specific and not already set in
        // widget definition
        if (isGeneric || !isFieldDefinitions(wDef)) {

            FieldDefinition[] fieldDefinitions;
            int fieldCount = 1;
            if (PropertyType.isContentType(propertyType)) {
                fieldCount = 2;
            }
            fieldDefinitions = new FieldDefinition[fieldCount];

            String fieldDefinitionFieldName = propertyName;
            if (field != null) {
                fieldDefinitionFieldName = field.getName().getLocalName();
            }
            fieldDefinitions[0] = new FieldDefinitionImpl(null,
                    fieldDefinitionFieldName);
            if (PropertyType.isContentType(propertyType)) {
                fieldDefinitionFieldName = "filename";
                if (field == null) {
                    fieldDefinitionFieldName = getPropertyName(
                            getPropertySchema(propertyName),
                            fieldDefinitionFieldName);
                }
                fieldDefinitions[1] = new FieldDefinitionImpl(null,
                        fieldDefinitionFieldName);
            }

            wDef.setFieldDefinitions(fieldDefinitions);
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

    protected final WidgetDefinition[] getSubWidgetDefinitions(
            String propertyName, String propertyType, Field field,
            List<String> complexFieldItemNames, boolean isDisplayItemIndexes)
            throws ClientException {

        WidgetDefinition[] subWidgetDefs = null;
        // Complex
        if (PropertyType.isComplexType(propertyType)
                && !PropertyType.isContentType(propertyType)) {
            subWidgetDefs = getComplexSubWidgetDefinitions(propertyName, field,
                    complexFieldItemNames, false);
        }
        // Scalar or content list
        else if (PropertyType.isScalarListType(propertyType)
                || PropertyType.isContentListType(propertyType)) {
            Field listFieldItem = ComplexPropertyHelper.getListFieldItem(field);
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes, 1);
            subWidgetDefs[subWidgetDefs.length - 1] = getWidgetDefinition(
                    getSubPropertyFullName(propertyName,
                            listFieldItem.getName().getLocalName()),
                    ComplexPropertyHelper.getFieldType(listFieldItem),
                    new FieldImpl(new QName(
                            DIFF_LIST_WIDGET_VALUE_SUBWIDGET_FIELD),
                            field.getType(), listFieldItem.getType()), null);
        }
        // Complex list
        else if (PropertyType.isComplexListType(propertyType)) {
            Field listFieldItem = ComplexPropertyHelper.getListFieldItem(field);
            subWidgetDefs = getComplexSubWidgetDefinitions(propertyName,
                    listFieldItem, complexFieldItemNames, isDisplayItemIndexes);
        }
        return subWidgetDefs;
    }

    protected final WidgetDefinition[] getComplexSubWidgetDefinitions(
            String propertyName, Field field,
            List<String> complexFieldItemNames, boolean isDisplayItemIndexes)
            throws ClientException {

        WidgetDefinition[] subWidgetDefs;
        int subWidgetIndex = isDisplayItemIndexes ? 1 : 0;

        if (CollectionUtils.isEmpty(complexFieldItemNames)) {
            List<Field> complexFieldItems = ComplexPropertyHelper.getComplexFieldItems(field);
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes,
                    complexFieldItems.size());

            for (Field complexFieldItem : complexFieldItems) {
                subWidgetDefs[subWidgetIndex] = getWidgetDefinition(
                        getSubPropertyFullName(propertyName,
                                complexFieldItem.getName().getLocalName()),
                        ComplexPropertyHelper.getFieldType(complexFieldItem),
                        complexFieldItem, null);
                subWidgetIndex++;
            }
        } else {
            subWidgetDefs = initSubWidgetDefinitions(isDisplayItemIndexes,
                    complexFieldItemNames.size());
            for (String complexFieldItemName : complexFieldItemNames) {
                Field complexFieldItem = ComplexPropertyHelper.getComplexFieldItem(
                        field, complexFieldItemName);
                if (complexFieldItem != null) {
                    subWidgetDefs[subWidgetIndex] = getWidgetDefinition(
                            getSubPropertyFullName(propertyName,
                                    complexFieldItemName),
                            ComplexPropertyHelper.getFieldType(complexFieldItem),
                            complexFieldItem, null);
                    subWidgetIndex++;
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

    @SuppressWarnings("unchecked")
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
        return propertyName;
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
     * Gets the layout store service.
     *
     * @return the layout store service
     * @throws ClientException the client exception
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

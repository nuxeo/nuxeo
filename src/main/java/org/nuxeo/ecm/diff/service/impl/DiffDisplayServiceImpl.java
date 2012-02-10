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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffDisplayField;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffDisplayBlockImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.SimpleDiffDisplayField;
import org.nuxeo.ecm.diff.service.DiffDisplayService;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

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

    protected static final String DIFF_WIDGET_LABEL_PREFIX = "label.diff.widget.";

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

    public List<String> getDefaultDiffDisplay() {
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
                return getDiffDisplayBlocks(diffBlockRefs, docDiff, leftDoc,
                        rightDoc);
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

        List<String> diffBlockRefs = getDefaultDiffDisplay();
        if (diffBlockRefs != null) {
            LOGGER.info("Found the default (Document) diffDisplay contribution => using it to display the diff.");
            return getDiffDisplayBlocks(diffBlockRefs, docDiff, leftDoc,
                    rightDoc);
        } else {
            // TODO: Use schema/fields in random order...
            LOGGER.info("The default (Document) diffDisplay contribution was not found => using the document type schemas and fields to display the diff (random schema and field order).");
            return new ArrayList<DiffDisplayBlock>();
        }
    }

    /**
     * Registers a diff display contrib.
     * 
     * @param contribution the contribution
     */
    private void registerDiffDisplay(DiffDisplayDescriptor descriptor) {

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

    private List<String> getDiffBlockRefs(
            List<DiffBlockReferenceDescriptor> diffBlocks) {

        List<String> diffBlockRefs = new ArrayList<String>();
        for (DiffBlockReferenceDescriptor diffBlockRef : diffBlocks) {
            diffBlockRefs.add(diffBlockRef.getName());
        }
        return diffBlockRefs;
    }

    private void registerDiffBlock(DiffBlockDescriptor descriptor) {

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

    private List<DiffDisplayBlock> getDiffDisplayBlocks(
            List<String> diffBlockNames, DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        List<DiffDisplayBlock> diffDisplayBlocks = new ArrayList<DiffDisplayBlock>();

        for (String diffBlockRef : diffBlockNames) {
            DiffBlockDefinition diffBlockDef = getDiffBlockDefinition(diffBlockRef);
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

    private DiffDisplayBlock getDiffDisplayBlock(
            DiffBlockDefinition diffBlockDefinition, DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException {

        Map<String, DiffDisplayField> value = new HashMap<String, DiffDisplayField>();
        List<LayoutRowDefinition> layoutRowDefinitions = new ArrayList<LayoutRowDefinition>();
        List<WidgetDefinition> widgetDefinitions = new ArrayList<WidgetDefinition>();

        List<DiffFieldDefinition> fieldDefinitions = diffBlockDefinition.getFields();
        for (DiffFieldDefinition fieldDefinition : fieldDefinitions) {

            String schemaName = fieldDefinition.getSchema();
            String fieldName = fieldDefinition.getName();

            SchemaDiff schemaDiff = docDiff.getSchemaDiff(schemaName);
            if (schemaDiff != null) {
                PropertyDiff fieldDiff = schemaDiff.getFieldDiff(fieldName);
                if (fieldDiff != null) {

                    String propertyName = getPropertyName(schemaName, fieldName);
                    String propertyType = fieldDiff.getPropertyType();

                    // Set diff display field value
                    DiffDisplayField diffDisplayField = getDiffDisplayField(
                            propertyType,
                            leftDoc.getProperty(schemaName, fieldName),
                            rightDoc.getProperty(schemaName, fieldName));
                    value.put(propertyName, diffDisplayField);

                    // Set layout row definition
                    LayoutRowDefinition layoutRowDefinition = new LayoutRowDefinitionImpl(
                            propertyName, propertyName, DIFF_WIDGET_CATEGORY);
                    layoutRowDefinitions.add(layoutRowDefinition);

                    // Set widget definition
                    WidgetDefinition widgetDefinition = getWidgetDefinition(
                            schemaName, fieldName, propertyType);
                    widgetDefinitions.add(widgetDefinition);

                }
            }
        }

        // Build layout definition
        LayoutDefinition layoutDefinition = new LayoutDefinitionImpl(
                diffBlockDefinition.getName(), null, null,
                layoutRowDefinitions, widgetDefinitions);

        // Build diff display block
        DiffDisplayBlock diffDisplayBlock = new DiffDisplayBlockImpl(
                diffBlockDefinition.getLabel(), value, layoutDefinition);

        return diffDisplayBlock;
    }

    private DiffDisplayField getDiffDisplayField(String propertyType,
            Object leftProperty, Object rightProperty) {

        if (PropertyType.isSimpleType(propertyType)) {
            return new SimpleDiffDisplayField((Serializable) leftProperty,
                    (Serializable) rightProperty);
        }
        // TODO: list, complex, complex lists, content

        return null;
    }

    private WidgetDefinition getWidgetDefinition(String schemaName,
            String fieldName, String propertyType) throws ClientException {

        String propertyName = getPropertyName(schemaName, fieldName);

        // Look for a specific widget in the "diff" category named with the
        // property name
        WidgetDefinition wDef = getLayoutStore().getWidgetDefinition(
                DIFF_WIDGET_CATEGORY, propertyName);
        if (wDef == null) {
            // Fallback on a generic widget in the "diff" category named with
            // the property type
            wDef = getLayoutStore().getWidgetDefinition(DIFF_WIDGET_CATEGORY,
                    propertyType);
        }
        if (wDef == null) {
            throw new ClientException(
                    String.format(
                            "Could not find any specific widget named '%s', nor any generic widget named '%s'. Please make sure at least a generic widget is defined for this type.",
                            propertyName, propertyType));
        }
        // Clone widget definition
        wDef = wDef.clone();

        // Set name for the generic widget case
        wDef.setName(propertyName);

        // Set labels
        Map<String, String> labels = new HashMap<String, String>();
        labels.put(BuiltinModes.ANY, DIFF_WIDGET_LABEL_PREFIX + schemaName
                + "." + fieldName);
        wDef.setLabels(labels);

        // Set translated
        wDef.setTranslated(true);

        // Set field definitions
        FieldDefinition[] fieldDefinitions = { new FieldDefinitionImpl(
                schemaName, fieldName) };
        wDef.setFieldDefinitions(fieldDefinitions);

        // TODO: set props ?

        return wDef;
    }

    /**
     * Gets the property name.
     * 
     * @param schema the schema
     * @param field the field
     * @return the property name
     */
    protected final String getPropertyName(String schema, String field) {
        return schema + ":" + field;
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

    /**
     * Gets the schema manager.
     * 
     * @return the schema manager
     * @throws ClientException the client exception
     */
    // protected final SchemaManager getSchemaManager() throws ClientException {
    //
    // SchemaManager schemaManager;
    //
    // try {
    // schemaManager = Framework.getService(SchemaManager.class);
    // } catch (Exception e) {
    // throw ClientException.wrap(e);
    // }
    // if (schemaManager == null) {
    // throw new ClientException("SchemaManager is null.");
    // }
    // return schemaManager;
    // }

    // public void applyComplexItemsOrder(String schemaName, String fieldName,
    // List<String> complexItems) {
    //
    // List<String> orderedComplexItems = getComplexItems(schemaName,
    // fieldName);
    // if (orderedComplexItems != null) {
    // for (int i = 0; i < orderedComplexItems.size(); i++) {
    // String orderedComplexItem = orderedComplexItems.get(i);
    // if (complexItems.contains(orderedComplexItem)) {
    // int complexItemIndex = complexItems.indexOf(orderedComplexItem);
    // String tempItem = complexItems.get(i);
    // complexItems.set(i, orderedComplexItem);
    // complexItems.set(complexItemIndex, tempItem);
    // }
    // }
    // }
    // }

}

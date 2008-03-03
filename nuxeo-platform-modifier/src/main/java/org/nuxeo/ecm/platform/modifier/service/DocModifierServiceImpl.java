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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.modifier.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.modifier.DocModifierException;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Document modification procedure: Core Event -> Extract word File + some
 * fields -> call NXPlugin (generate the new file) -> update the document with
 * the generated file.
 *
 * @author DM
 */
public class DocModifierServiceImpl extends DefaultComponent implements
        DocModifierService {

    public static final String NAME = "org.nuxeo.ecm.platform.modifier.service.DocModifierService";

    public static final String MODIFIER_EXTENSION_POINT_ASSOCIATIONS = "docTypeToTransformer";

    private static final Log log = LogFactory.getLog(DocModifierServiceImpl.class);

    private final Map<String, List<DocModifierEPDescriptor>> docModifDescriptorsMap = new HashMap<String, List<DocModifierEPDescriptor>>();

    static class TransformationOutcome {
        ByteArrayBlob content;

        Map<String, Serializable> properties;
    }

    public DocModifierServiceImpl() {
        log.debug("<init>");
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        log.debug("<activate>");
        super.activate(context);

    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        log.debug("<registerExtension>");
        super.registerExtension(extension);

        final String extPoint = extension.getExtensionPoint();
        if (MODIFIER_EXTENSION_POINT_ASSOCIATIONS.equals(extPoint)) {
            log.info("register contributions for extension point: "
                    + MODIFIER_EXTENSION_POINT_ASSOCIATIONS);

            final Object[] contribs = extension.getContributions();
            if (null == contribs) {
                log.warn("no contributions for EP: " + extPoint);
            } else {
                registerModifiers(contribs);
            }
        } else {
            log.warn("extension not handled: " + extPoint);
        }
    }

    /**
     * Adds modifiers in a hash map with the key based on document type.
     *
     * @param contribs
     */
    private void registerModifiers(Object[] contribs) {

        for (Object object : contribs) {
            if (object instanceof DocModifierEPDescriptor) {
                final DocModifierEPDescriptor descriptor = (DocModifierEPDescriptor) object;
                final String[] docTypes = descriptor.getDocumentTypes();
                assert docTypes != null;

                if (docTypes.length == 0) {
                    log.warn("no document types specified for doc modifier: "
                            + descriptor.getName());
                }

                for (String docType : docTypes) {

                    List<DocModifierEPDescriptor> existingDescriptors = this.docModifDescriptorsMap.get(docType);

                    if (null == existingDescriptors) {
                        existingDescriptors = new ArrayList<DocModifierEPDescriptor>();
                    }

                    existingDescriptors.add(descriptor);

                    // order modifiers by order
                    Collections.sort(existingDescriptors,
                            new Comparator<DocModifierEPDescriptor>() {

                                public int compare(
                                        DocModifierEPDescriptor modifier1,
                                        DocModifierEPDescriptor modifier2) {

                                    Integer modifier1Order = modifier1.getOrder();
                                    Integer modifier2Order = modifier2.getOrder();
                                    return modifier1Order.compareTo(modifier2Order);
                                }

                            });

                    // actually not needed...
                    docModifDescriptorsMap.put(docType, existingDescriptors);

                    log.info("modifier for Doc type " + docType + " registered");
                }
            } else {
                log.warn("Descriptor not handled: " + object);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        log.debug("<unregisterExtension>");
        super.unregisterExtension(extension);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        log.debug("<deactivate>");
        super.deactivate(context);
    }

    public void processDocument(DocumentModel doc, String eventName)
            throws DocModifierException {

        // check document type:
        final DocumentType docType = doc.getDocumentType();

        final List<DocModifierEPDescriptor> descriptors = docModifDescriptorsMap.get(docType.getName());
        if (descriptors == null) {
            log.debug(String.format(
                    "No modifier for document type '%s' registered",
                    docType.getName()));
            return;
        }

        for (DocModifierEPDescriptor descriptor : descriptors) {
            final String[] applyForCoreEvents = descriptor.getCoreEvents();
            if (applyForCoreEvents == null || applyForCoreEvents.length == 0) {
                // we consider for DOCUMENT_UPDATED & DOCUMENT_CREATED
                if (DocumentEventTypes.DOCUMENT_UPDATED.equals(eventName)
                        || DocumentEventTypes.DOCUMENT_CREATED.equals(eventName)) {
                    // will apply
                    applyModifier(descriptor, doc);
                }
            } else {
                for (String evt : applyForCoreEvents) {
                    if (evt.equals(eventName)) {
                        applyModifier(descriptor, doc);
                        break; // break to owner cycle
                    }
                }
            }
        }
    }

    /**
     * Apply the given document modifier. This is being called for each
     * registered document modifier for the received document type and core
     * event name.
     *
     * @param descriptor
     * @param doc
     * @throws DocModifierException
     */
    private void applyModifier(final DocModifierEPDescriptor descriptor,
            DocumentModel doc) throws DocModifierException {
        final String logPrefix = "<applyModifier> ";

        final String contentFieldName = descriptor.getSrcFieldName();

        if (null == contentFieldName) {
            throw new DocModifierException(
                    "Bad descriptor, contentFieldName is null.");
        }

        final String destinationFieldName = descriptor.getDestFieldName();

        if (null == destinationFieldName) {
            throw new DocModifierException(
                    "Bad descriptor, destinationFieldName is null.");
        }

        log.debug(logPrefix + "doc content field: " + contentFieldName);

        try {
            final Object wouldBeContent = doc.getProperty(
                    DocumentModelUtils.getSchemaName(contentFieldName),
                    DocumentModelUtils.getFieldName(contentFieldName));
            if (null == wouldBeContent) {
                log.warn("The content value is null. Doc modification not performed.");
                return;
            }

            if (wouldBeContent instanceof Blob) {

                final Map<String, Serializable> replacementValues = new HashMap<String, Serializable>();
                final CustomField[] replacementFields = descriptor.getCustomFields();
                int fieldIndex = 0;
                for (CustomField field : replacementFields) {

                    fieldIndex++;

                    final String fieldName = field.getName();
                    final String fieldValue = field.getValue();

                    Object value;
                    if (fieldName != null) {
                        // check for index (if the field has an array of values)

                        int indexStart = fieldName.indexOf('[');
                        int valueIndex = -1;
                        String propertyName = fieldName;
                        if (indexStart != -1 && fieldName.endsWith("]")) {
                            String valueIndexStr = fieldName.substring(
                                    indexStart + 1, fieldName.length() - 1);
                            try {
                                valueIndex = Integer.parseInt(valueIndexStr);
                                propertyName = fieldName.substring(0,
                                        indexStart);
                            } catch (NumberFormatException e) {
                                log.warn("property index not valid: "
                                        + fieldName + ", offending value: "
                                        + valueIndexStr);
                            }
                        }

                        value = doc.getProperty(
                                DocumentModelUtils.getSchemaName(propertyName),
                                DocumentModelUtils.getFieldName(propertyName));

                        if (valueIndex != -1) {
                            if (value instanceof Object[]) {
                                value = ((Object[]) value)[valueIndex];
                            }
                        }

                        if (value == null) {
                            log.warn(logPrefix
                                    + "property value for fieldName '"
                                    + fieldName + "' is null, skipping");
                            continue;
                        }
                    } else if (fieldValue != null) {
                        value = fieldValue;
                        if (value == null) {
                            log.warn(logPrefix + "value for field #'"
                                    + fieldIndex + "' is null, skipping");
                            continue;
                        }
                    } else {
                        log.warn(logPrefix
                                + "cannot get a valid value for custom field");
                        continue;
                    }

                    assert value != null;

                    final String transformationParamName = field.getTransformParamName();

                    if (null == transformationParamName) {
                        log.warn(logPrefix + "transformParamName for field #'"
                                + fieldIndex + "' is null. Skipping...");
                        continue;
                    }

                    if (value instanceof Serializable) {
                        Serializable newValue = (Serializable) value;

                        //
                        // We try to join older and newer values to obtain for
                        // example version string like XX.XX from 2 fields
                        //
                        final Serializable oldValue = replacementValues.get(transformationParamName);
                        if (oldValue != null) {
                            // attempt to join
                            newValue = oldValue.toString() + newValue;
                            log.debug(logPrefix + "xmlNode joined values: "
                                    + newValue);
                        }
                        replacementValues.put(transformationParamName, newValue);
                    } else {
                        log.warn("Cannot replace value for field "
                                + field
                                + " as it is not Serializable. Offending value: "
                                + value);
                    }
                }

                final TransformationOutcome outcome = performTransformation(
                        (Blob) wouldBeContent, replacementValues,
                        descriptor.getPluginName());

                if (outcome != null) {
                    // set back the content
                    log.debug(logPrefix + "set back modified value to field: "
                            + destinationFieldName);

                    final ByteArrayBlob newContent = outcome.content;
                    doc.setProperty(
                            DocumentModelUtils.getSchemaName(destinationFieldName),
                            DocumentModelUtils.getFieldName(destinationFieldName),
                            newContent);

                    // set regular properties
                    CustomOutputField[] outputFields = descriptor.getCustomOutputFields();
                    for (CustomOutputField field : outputFields) {
                        Serializable outValue = outcome.properties.get(field.getOutputParamName());
                        final String propertyName = field.getName();
                        doc.setProperty(
                                DocumentModelUtils.getSchemaName(propertyName),
                                DocumentModelUtils.getFieldName(propertyName),
                                outValue);
                    }
                } else {
                    // nothing to set back
                }

            } else {
                throw new DocModifierException("Value from field: "
                        + contentFieldName + " is not a content instance ("
                        + wouldBeContent.getClass() + ')');
            }
        } catch (DocumentException e) {
            // e.printStackTrace();
            throw new DocModifierException("Error processing document. "
                    + e.getMessage(), e);
        } catch (DocModifierException e) {
            // e.printStackTrace();
            throw e;
        } catch (Exception e) {
            // e.printStackTrace();
            throw new DocModifierException("Error processing document. "
                    + e.getMessage(), e);
        }
    }

    /**
     * Attempts to perform specified field values injection into the given
     * content.
     *
     * @param content
     * @param pluginName
     * @return a transformed content to put it back in the document or
     *         <code>null</code> if there is no transformation result due to
     *         the fact that the plugin was not supposed to handle given kind of
     *         data (i.e. wordML injection plugin cannot handle MSWord.doc type
     *         data)
     * @throws Exception
     */
    private TransformationOutcome performTransformation(Blob content,
            Map<String, Serializable> fieldValues, String pluginName)
            throws Exception {
        assert content != null;
        assert fieldValues != null;
        assert pluginName != null;

        final String logPrefix = "<performTransformation> ";
        log.debug(logPrefix + "replacement values: " + fieldValues);
        log.debug(logPrefix + "plugin name       : " + pluginName);

        // get transformation plugin
        final TransformServiceCommon service = ServiceHelper.getNXTransform();

        final List<TransformDocument> results;
        final Plugin transformer = service.getPluginByName(pluginName);

        if (null == transformer) {
            // BBB waiting for a fix
            Transformer bbb = service.getTransformerByName(pluginName);
            if (null == bbb) {
                throw new DocModifierException(
                        "Error processing document. Transformer  " + pluginName
                                + " not available.");
            } else {
                Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String,Serializable>>();
                options.put(pluginName, fieldValues);
                results = bbb.transform(options, new TransformDocumentImpl(content));
            }
        } else {
            // perform transformations
            results = transformer.transform(fieldValues,
                    new TransformDocumentImpl(content));
        }

        if (results.size() != 1) {
            // the plugin might not be supposed to handle the data of this
            // mimeType so it's ok not to get a result
            //
            // throw new DocModifierException(
            // "Unexpected result from NXTransform, results size="
            // + results.size());
            log.debug(logPrefix
                    + "no valid result from transformation. results count = "
                    + results.size());
            return null;
        } else {

            final TransformDocument result = results.get(0);
            final byte[] data = FileUtils.readBytes(result.getBlob().getStream());

            final ByteArrayBlob newContent = new ByteArrayBlob(data,
                    result.getMimetype());

            TransformationOutcome outcome = new TransformationOutcome();
            outcome.content = newContent;
            outcome.properties = result.getProperties();

            log.debug(logPrefix + "transformation succeeded.");
            return outcome;
        }
    }

}

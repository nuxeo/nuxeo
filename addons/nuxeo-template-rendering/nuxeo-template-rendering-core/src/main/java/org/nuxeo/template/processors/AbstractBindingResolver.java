/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.processors;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.ContentInputType;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import freemarker.template.TemplateModelException;

public abstract class AbstractBindingResolver implements InputBindingResolver {

    protected Log log = LogFactory.getLog(AbstractBindingResolver.class);

    protected abstract Object handleLoop(String paramName, Object value);

    protected abstract Object handlePictureField(String paramName, Blob blobValue);

    protected abstract void handleBlobField(String paramName, Blob blobValue);

    protected String handleHtmlField(String paramName, String htmlValue) {
        return HtmlBodyExtractor.extractHtmlBody(htmlValue);
    }

    protected DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

    public AbstractBindingResolver() {
        super();
    }

    protected DocumentObjectWrapper getWrapper() {
        return nuxeoWrapper;
    }

    @Override
    public void resolve(List<TemplateInput> inputParams, Map<String, Object> context,
            TemplateBasedDocument templateBasedDocument) {

        for (TemplateInput param : inputParams) {
            try {
                if (param.isSourceValue()) {
                    if (param.getType() == InputType.Content) {
                        if (ContentInputType.HtmlPreview.getValue().equals(param.getSource())) {
                            BlobHolder bh = templateBasedDocument.getAdaptedDoc().getAdapter(BlobHolder.class);
                            String htmlValue = handleHtmlField(param.getName(), getHtmlValue(bh));
                            context.put(param.getName(), htmlValue);
                            continue;
                        } else if (ContentInputType.BlobContent.getValue().equals(param.getSource())) {
                            Object propValue = templateBasedDocument.getAdaptedDoc().getPropertyValue(param.getSource());
                            if (propValue != null && propValue instanceof Blob) {
                                Blob blobValue = (Blob) propValue;
                                context.put(param.getName(), blobValue.getString());
                                handleBlobField(param.getName(), blobValue);
                            }
                        } else {
                            Object propValue = templateBasedDocument.getAdaptedDoc().getPropertyValue(param.getSource());
                            if (propValue instanceof String) {
                                String stringContent = (String) propValue;
                                String htmlValue = handleHtmlField(param.getName(), stringContent);
                                context.put(param.getName(), htmlValue);
                            }
                        }
                    }
                    Property property = null;
                    try {
                        property = templateBasedDocument.getAdaptedDoc().getProperty(param.getSource());
                    } catch (PropertyException e) {
                        log.warn("Unable to ready property " + param.getSource(), e);
                    }

                    Serializable value = null;
                    if (property != null) {
                        value = property.getValue();
                    }

                    if (value != null) {
                        if (param.getType() != InputType.Content) {
                            if (Blob.class.isAssignableFrom(value.getClass())) {
                                Blob blob = (Blob) value;
                                if (param.getType() == InputType.PictureProperty) {
                                    if (blob.getMimeType() == null || "".equals(blob.getMimeType().trim())) {
                                        blob.setMimeType("image/jpeg");
                                    }
                                    context.put(param.getName(), handlePictureField(param.getName(), blob));
                                }
                            } else {
                                if (param.isAutoLoop()) {
                                    // should do the same on all children
                                    // properties ?
                                    Object loopVal = handleLoop(param.getName(), property);
                                    context.put(param.getName(), loopVal);
                                } else {
                                    context.put(param.getName(), nuxeoWrapper.wrap(property));
                                }
                            }
                        }
                    } else {
                        // no available value, try to find a default one ...
                        if (property != null) {
                            Type pType = property.getType();
                            if (pType.getName().equals(BooleanType.ID)) {
                                context.put(param.getName(), new Boolean(false));
                            } else if (pType.getName().equals(DateType.ID)) {
                                context.put(param.getName(), new Date());
                            } else if (pType.getName().equals(StringType.ID)) {
                                context.put(param.getName(), "");
                            } else if (pType.getName().equals(InputType.Content.getValue())) {
                                context.put(param.getName(), "");
                            } else {
                                context.put(param.getName(), "!NOVALUE!");
                            }
                            // handle special case for pictures
                            if (param.getType() == InputType.PictureProperty) {
                                context.put(param.getName(), handlePictureField(param.getName(), null));
                            }
                        } else {
                            if (param.getType().equals(InputType.PictureProperty)) {
                                context.put(param.getName(), handlePictureField(param.getName(), null));
                            }
                        }
                    }

                } else {
                    if (InputType.StringValue.equals(param.getType())) {
                        context.put(param.getName(), param.getStringValue());
                    } else if (InputType.BooleanValue.equals(param.getType())) {
                        context.put(param.getName(), param.getBooleanValue());
                    } else if (InputType.DateValue.equals(param.getType())) {
                        context.put(param.getName(), param.getDateValue());
                    }
                }
            } catch (TemplateModelException | IOException e) {
                log.warn("Unable to handle binding for param " + param.getName(), e);
            }
        }
    }

    protected String getHtmlValue(BlobHolder bh) throws IOException {
        if (bh == null) {
            return "";
        }

        Blob blob = bh.getBlob();
        if (blob != null && "text/html".equals(blob.getMimeType())) {
            return blob.getString();
        }

        ConversionService conversion = Framework.getService(ConversionService.class);
        BlobHolder htmlBh = conversion.convertToMimeType("text/html", bh, Collections.emptyMap());
        if (htmlBh != null) {
            return htmlBh.getBlob().getString();
        }

        if (blob != null && blob.getMimeType() != null && blob.getMimeType().startsWith("text/")) {
            return blob.getString();
        }

        return "";
    }

}

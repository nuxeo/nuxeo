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

import static org.nuxeo.template.api.ContentInputType.BlobContent;
import static org.nuxeo.template.api.ContentInputType.HtmlPreview;
import static org.nuxeo.template.api.InputType.Content;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
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

    private static final Logger log = LogManager.getLogger(AbstractBindingResolver.class);

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
                Object value = extractValueFromParam(templateBasedDocument, param);
                context.put(param.getName(), value);
            } catch (ValueNotFound e) {
                log.warn("Unable to handle binding for param: {}", param::getName);
                log.debug(e, e);
            } catch (NoValueToAddInContext e) {
                log.warn("Skip param to add: {} ", param::getName);
                log.debug(e, e);
            }
        }
    }

    protected Object extractValueFromParam(TemplateBasedDocument templateBasedDocument, TemplateInput param) {
        DocumentModel doc = templateBasedDocument.getAdaptedDoc();
        String propKey = param.getSource();

        switch (param.getType()) {
        case BooleanValue:
            return param.getBooleanValue();
        case DateValue:
            return param.getDateValue();
        case StringValue:
            return param.getStringValue();
        case MapValue:
            Map<String, Object> resultMap = new HashMap<>();
            param.getMapValue().entrySet().forEach(entry -> {
                try {
                    resultMap.put(entry.getKey(), extractValueFromParam(templateBasedDocument, entry.getValue()));
                } catch (NoValueToAddInContext | ValueNotFound e) {
                    log.warn("Skip param to add: {} in: {}", entry::getKey, param::getName);
                    log.debug(e, e);
                }
            });
            return resultMap;
        case ListValue:
            List<Object> resultList = new ArrayList<>();
            param.getListValue().forEach(p -> {
                try {
                    resultList.add(extractValueFromParam(templateBasedDocument, p));
                } catch (NoValueToAddInContext | ValueNotFound e) {
                    log.warn("Skip param to add: {} in: {}", p::getName, param::getName);
                    log.debug(e, e);
                }
            });
            return resultList;
        case Content:
            ContentInputType contentInput = ContentInputType.getByValue(param.getSource());
            if (BlobContent.equals(contentInput)) {
                return extractBlobContent(doc, param);
            } else if (HtmlPreview.equals(contentInput)) {
                return extractHTMLPreview(doc, param);
            } else {
                Serializable docPropertyValue = getDocPropertyValue(doc, propKey);
                if (docPropertyValue instanceof String) {
                    return handleHtmlField(param.getName(), (String) getDocPropertyValue(doc, propKey));
                }
            }
            break;
        case PictureProperty:
            try {
                Serializable docPropertyValue = getDocPropertyValue(doc, propKey);
                if (isBlob(docPropertyValue)) {
                    Blob blob = (Blob) getDocPropertyValue(doc, propKey);
                    addDefaultMimetypeIfRequired(blob);
                    return handlePictureField(param.getName(), blob);
                }
            } catch (ValueNotFound e) {
                return handlePictureField(param.getName(), null);
            }
            break;
        }

        Serializable docPropertyValue = getDocPropertyValue(doc, propKey);
        if (docPropertyValue == null) {
            return extractBlobContent(doc, param);
        }
        if (isBlob(getDocPropertyValue(doc, propKey))) {
            throw new NoValueToAddInContext();
        }

        Property property = getDocProperty(param, doc);
        if (param.isAutoLoop()) {
            return extractAutoLoop(param, property);
        } else {
            try {
                return nuxeoWrapper.wrap(property);
            } catch (TemplateModelException e) {
                throw new ValueNotFound(e);
            }
        }
    }

    protected void addDefaultMimetypeIfRequired(Blob blob) {
        if (StringUtils.isBlank(blob.getMimeType())) {
            blob.setMimeType("image/jpeg");
        }
    }

    protected Object extractAutoLoop(TemplateInput param, Property property) {
        // should do the same on all children properties ?
        return handleLoop(param.getName(), property);
    }

    protected Object extractBlobContent(DocumentModel doc, TemplateInput param) {
        Object propValue = getDocPropertyValue(doc, param.getSource());
        if (propValue instanceof Blob) {
            Blob blobValue = (Blob) propValue;
            handleBlobField(param.getName(), blobValue);
            try {
                return blobValue.getString();
            } catch (IOException e) {
                log.warn("Unable to handle binding for param: {}", param.getName(), e);
                return "";
            }
        }
        return extractDefaultValue(doc, param);
    }

    protected String extractHTMLPreview(DocumentModel doc, TemplateInput param) {
        try {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            return handleHtmlField(param.getName(), getHtmlValue(bh));
        } catch (IOException e) {
            log.warn("Unable to handle binding for param: {}", param.getName(), e);
            return null;
        }
    }

    protected Object extractDefaultValue(DocumentModel doc, TemplateInput param) {

        // handle special case for pictures
        if (param.getType().equals(InputType.PictureProperty)) {
            return handlePictureField(param.getName(), null);
        }

        try {
            Property property = doc.getProperty(param.getSource());

            if (property != null) {
                Type pType = property.getType();
                if (pType.getName().equals(BooleanType.ID)) {
                    return Boolean.FALSE;
                } else if (pType.getName().equals(DateType.ID)) {
                    return new Date();
                } else if (pType.getName().equals(StringType.ID)) {
                    return "";
                } else if (pType.getName().equals(Content.getValue())) {
                    return "";
                } else {
                    return "!NOVALUE!";
                }
            }
        } catch (PropertyNotFoundException e) {
            throw new ValueNotFound(e);
        }
        throw new ValueNotFound();
    }

    protected Property getDocProperty(TemplateInput param, DocumentModel doc) {
        Property property;
        try {
            property = doc.getProperty(param.getSource());
        } catch (PropertyException e) {
            throw new ValueNotFound(e);
        }
        return property;
    }

    protected Serializable getDocPropertyValue(DocumentModel doc, String propKey) {
        try {
            return doc.getPropertyValue(propKey);
        } catch (PropertyException e) {
            throw new ValueNotFound(e);
        }
    }

    protected boolean isBlob(Serializable propValue) {
        return propValue != null && Blob.class.isAssignableFrom(propValue.getClass());
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

    protected static class ValueNotFound extends NuxeoException {
        public ValueNotFound(Exception e) {
            super(e);
        }

        public ValueNotFound() {
        }
    }

    protected static class NoValueToAddInContext extends NuxeoException {
    }

}

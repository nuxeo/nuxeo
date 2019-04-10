package org.nuxeo.template.processors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.template.api.ContentInputType;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

public abstract class AbstractBindingResolver implements InputBindingResolver {

    protected Log log = LogFactory.getLog(AbstractBindingResolver.class);

    protected abstract Object handleLoop(String paramName, Object value);

    protected abstract Object handlePictureField(String paramName,
            Blob blobValue);

    protected abstract void handleBlobField(String paramName, Blob blobValue);

    protected String handleHtmlField(String paramName, String htmlValue) {
        return HtmlBodyExtractor.extractHtmlBody(htmlValue);
    }

    protected DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(
            null);

    public AbstractBindingResolver() {
        super();
    }

    protected DocumentObjectWrapper getWrapper() {
        return nuxeoWrapper;
    }

    @Override
    public void resolve(List<TemplateInput> inputParams,
            Map<String, Object> context,
            TemplateBasedDocument templateBasedDocument) {

        for (TemplateInput param : inputParams) {
            try {
                if (param.isSourceValue()) {
                    if (param.getType() == InputType.Content) {

                        if (ContentInputType.HtmlPreview.getValue().equals(
                                param.getSource())) {
                            HtmlPreviewAdapter preview = templateBasedDocument.getAdaptedDoc().getAdapter(
                                    HtmlPreviewAdapter.class);
                            String htmlValue = "";
                            if (preview != null) {
                                List<Blob> blobs = preview.getFilePreviewBlobs();
                                if (blobs.size() > 0) {
                                    Blob htmlBlob = preview.getFilePreviewBlobs().get(
                                            0);
                                    if (htmlBlob != null) {
                                        htmlValue = htmlBlob.getString();
                                    }
                                }
                            } else {
                                BlobHolder bh = templateBasedDocument.getAdaptedDoc().getAdapter(
                                        BlobHolder.class);
                                if (bh != null) {
                                    Blob htmlBlob = bh.getBlob();
                                    if (htmlBlob != null
                                            && htmlBlob.getMimeType() != null
                                            && htmlBlob.getMimeType().startsWith(
                                                    "text/")) {
                                        htmlValue = htmlBlob.getString();
                                    }
                                }
                            }
                            htmlValue = handleHtmlField(param.getName(),
                                    htmlValue);
                            context.put(param.getName(), htmlValue);
                            continue;
                        } else if (ContentInputType.BlobContent.getValue().equals(
                                param.getSource())) {
                            Object propValue = templateBasedDocument.getAdaptedDoc().getPropertyValue(
                                    param.getSource());
                            if (propValue != null && propValue instanceof Blob) {
                                Blob blobValue = (Blob) propValue;
                                context.put(param.getName(),
                                        blobValue.getString());
                                handleBlobField(param.getName(), blobValue);
                            }
                        } else {
                            Object propValue = templateBasedDocument.getAdaptedDoc().getPropertyValue(
                                    param.getSource());
                            if (propValue instanceof String) {
                                String stringContent = (String) propValue;
                                String htmlValue = handleHtmlField(
                                        param.getName(), stringContent);
                                context.put(param.getName(), htmlValue);
                            }
                        }
                    }
                    Property property = null;
                    try {
                        property = templateBasedDocument.getAdaptedDoc().getProperty(
                                param.getSource());
                    } catch (Throwable e) {
                        log.warn(
                                "Unable to ready property " + param.getSource(),
                                e);
                    }

                    Serializable value = null;
                    if (property != null) {
                        value = property.getValue();
                    }

                    if (value != null) {
                        if (param.getType() == InputType.Content) {

                        } else {
                            if (Blob.class.isAssignableFrom(value.getClass())) {
                                Blob blob = (Blob) value;
                                if (param.getType() == InputType.PictureProperty) {
                                    if (blob.getMimeType() == null
                                            || "".equals(blob.getMimeType().trim())) {
                                        blob.setMimeType("image/jpeg");
                                    }
                                    context.put(
                                            param.getName(),
                                            handlePictureField(param.getName(),
                                                    blob));
                                }
                            } else {
                                if (param.isAutoLoop()) {
                                    // should do the same on all children
                                    // properties ?
                                    Object loopVal = handleLoop(
                                            param.getName(), property);
                                    context.put(param.getName(), loopVal);
                                } else {
                                    context.put(param.getName(),
                                            nuxeoWrapper.wrap(property));
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
                            } else if (pType.getName().equals(InputType.Content)) {
                                context.put(param.getName(), "");
                            } else if (pType.getName().equals(
                                    InputType.PictureProperty)) {
                                context.put(
                                        param.getName(),
                                        handlePictureField(param.getName(),
                                                null));
                            } else {
                                context.put(param.getName(), "!NOVALUE!");
                            }
                        } else {
                            if (param.getType().equals(
                                    InputType.PictureProperty)) {
                                context.put(
                                        param.getName(),
                                        handlePictureField(param.getName(),
                                                null));
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
            } catch (Exception e) {
                log.warn(
                        "Unable to handle binding for param " + param.getName(),
                        e);
            }
        }
    }

}
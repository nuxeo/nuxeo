package org.nuxeo.ecm.platform.template.processors.xdocreport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.ecm.platform.template.ContentInputType;
import org.nuxeo.ecm.platform.template.InputType;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.fm.FMContextBuilder;
import org.nuxeo.ecm.platform.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.ecm.platform.template.processors.AbstractTemplateProcessor;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;
import org.nuxeo.runtime.api.Framework;

import fr.opensagres.xdocreport.core.document.SyntaxKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.images.IImageProvider;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;

public class XDocReportProcessor extends AbstractTemplateProcessor implements
        TemplateProcessor {

    protected static final Log log = LogFactory.getLog(XDocReportProcessor.class);

    public static final String TEMPLATE_TYPE = "XDocReport";

    public static final String OOO_TEMPLATE_TYPE = "OpenDocument";

    public static final String DocX_TEMPLATE_TYPE = "DocX";

    protected String getTemplateFormat(Blob blob) {
        String filename = blob.getFilename();
        if (filename == null && blob instanceof FileBlob) {
            File file = ((FileBlob) blob).getFile();
            if (file != null) {
                filename = file.getName();
            }
        }
        if (filename != null && !filename.isEmpty()) {
            if (filename.endsWith(".docx")) {
                return DocX_TEMPLATE_TYPE;
            } else if (filename.endsWith(".odt")) {
                return OOO_TEMPLATE_TYPE;
            }
        }
        return OOO_TEMPLATE_TYPE;
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob)
            throws Exception {

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        String xmlContent = null;

        if (OOO_TEMPLATE_TYPE.equals(getTemplateFormat(blob))) {
            xmlContent = ZipXmlHelper.readXMLContent(blob,
                    ZipXmlHelper.OOO_MAIN_FILE);
        } else if (DocX_TEMPLATE_TYPE.equals(getTemplateFormat(blob))) {
            xmlContent = ZipXmlHelper.readXMLContent(blob,
                    ZipXmlHelper.DOCX_MAIN_FILE);
        }

        if (xmlContent != null) {
            List<String> vars = FreeMarkerVariableExtractor.extractVariables(xmlContent);

            for (String var : vars) {
                TemplateInput input = new TemplateInput(var);
                params.add(input);
            }
        }
        return params;
    }

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument,
            String templateName) throws Exception {

        Blob sourceTemplateBlob = getSourceTemplateBlob(templateBasedDocument,
                templateName);

        // load the template
        IXDocReport report = XDocReportRegistry.getRegistry().loadReport(
                sourceTemplateBlob.getStream(), TemplateEngineKind.Freemarker,
                false);

        // manage parameters
        List<TemplateInput> params = templateBasedDocument.getParams(templateName);
        FieldsMetadata metadata = new FieldsMetadata();
        for (TemplateInput param : params) {
            if (param.getType() == InputType.PictureProperty) {
                metadata.addFieldAsImage(param.getName());
            }
        }
        report.setFieldsMetadata(metadata);

        // fill Freemarker context
        IContext context = report.createContext();

        DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

        for (TemplateInput param : params) {
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
                        }
                        context.put(param.getName(), htmlValue);
                        metadata.addFieldAsTextStyling(param.getName(),
                                SyntaxKind.Html);
                        continue;
                    } else if (ContentInputType.BlobContent.getValue().equals(
                            param.getSource())) {
                        Object propValue = templateBasedDocument.getAdaptedDoc().getPropertyValue(
                                param.getSource());
                        if (propValue != null && propValue instanceof Blob) {
                            Blob blobValue = (Blob) propValue;
                            context.put(param.getName(), blobValue.getString());
                            if ("text/html".equals(blobValue.getMimeType())) {
                                metadata.addFieldAsTextStyling(param.getName(),
                                        SyntaxKind.Html);
                            }
                        }
                    } else {
                        Object propValue = templateBasedDocument.getAdaptedDoc().getPropertyValue(
                                param.getSource());
                        if (propValue instanceof String) {
                            String stringContent = (String) propValue;
                            context.put(param.getName(), stringContent);
                            MimetypeRegistry mtr = Framework.getLocalService(MimetypeRegistry.class);
                            InputStream in = new ByteArrayInputStream(
                                    stringContent.getBytes());
                            if (mtr != null
                                    && "text/html".equals(mtr.getMimetypeFromStream(in))) {
                                metadata.addFieldAsTextStyling(param.getName(),
                                        SyntaxKind.Html);
                            }
                        }
                    }
                }
                Property property = null;
                try {
                    property = templateBasedDocument.getAdaptedDoc().getProperty(
                            param.getSource());
                } catch (Throwable e) {
                    log.warn("Unable to ready property " + param.getSource(), e);
                }
                if (property != null) {
                    Serializable value = property.getValue();
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
                                    IImageProvider imgBlob = new BlobImageProvider(
                                            blob);
                                    context.put(param.getName(), imgBlob);
                                    metadata.addFieldAsImage(param.getName());
                                }
                            } else {
                                if (param.isAutoLoop()) {
                                    // should do the same on all children
                                    // properties ?
                                    metadata.addFieldAsList(param.getName());
                                }
                                context.put(param.getName(),
                                        nuxeoWrapper.wrap(property));
                            }
                        }
                    } else {
                        // no available value, try to find a default one ...
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
                            // NOP
                        } else {
                            context.put(param.getName(), "!NOVALUE!");
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
        }

        // add default context vars
        DocumentModel doc = templateBasedDocument.getAdaptedDoc();
        Map<String, Object> ctx = FMContextBuilder.build(doc);
        for (String key : ctx.keySet()) {
            context.put(key, ctx.get(key));
        }
        // add automatic loop on audit entries
        metadata.addFieldAsList("auditEntries.principalName");
        metadata.addFieldAsList("auditEntries.eventId");
        metadata.addFieldAsList("auditEntries.eventDate");
        metadata.addFieldAsList("auditEntries.docUUID");
        metadata.addFieldAsList("auditEntries.docPath");
        metadata.addFieldAsList("auditEntries.docType");
        metadata.addFieldAsList("auditEntries.category");
        metadata.addFieldAsList("auditEntries.comment");
        metadata.addFieldAsList("auditEntries.docLifeCycle");
        metadata.addFieldAsList("auditEntries.repositoryId");

        File workingDir = getWorkingDir();
        File generated = new File(workingDir, "XDOCReportresult-"
                + System.currentTimeMillis());
        generated.createNewFile();

        OutputStream out = new FileOutputStream(generated);

        report.process(context, out);

        Blob newBlob = new FileBlob(generated);

        String templateFileName = sourceTemplateBlob.getFilename();

        // set the output file name
        String targetFileExt = FileUtils.getFileExtension(templateFileName);
        String targetFileName = FileUtils.getFileNameNoExt(templateBasedDocument.getAdaptedDoc().getTitle());
        targetFileName = targetFileName + "." + targetFileExt;
        newBlob.setFilename(targetFileName);

        // mark the file for automatic deletion on GC
        Framework.trackFile(generated, newBlob);
        return newBlob;
    }
}

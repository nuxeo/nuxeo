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
package org.nuxeo.template.processors.xdocreport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.fm.FMContextBuilder;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.template.processors.AbstractTemplateProcessor;

import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;

/**
 * XDocReport based {@link TemplateProcessor}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XDocReportProcessor extends AbstractTemplateProcessor implements TemplateProcessor {

    protected static final Log log = LogFactory.getLog(XDocReportProcessor.class);

    public static final String TEMPLATE_TYPE = "XDocReport";

    public static final String OOO_TEMPLATE_TYPE = "OpenDocument";

    public static final String DocX_TEMPLATE_TYPE = "DocX";

    protected FMContextBuilder fmContextBuilder = new FMContextBuilder();

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
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) throws IOException {

        List<TemplateInput> params = new ArrayList<>();
        String xmlContent = null;

        if (OOO_TEMPLATE_TYPE.equals(getTemplateFormat(blob))) {
            xmlContent = ZipXmlHelper.readXMLContent(blob, ZipXmlHelper.OOO_MAIN_FILE);
        } else if (DocX_TEMPLATE_TYPE.equals(getTemplateFormat(blob))) {
            xmlContent = ZipXmlHelper.readXMLContent(blob, ZipXmlHelper.DOCX_MAIN_FILE);
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
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument, String templateName) throws IOException {

        Blob sourceTemplateBlob = getSourceTemplateBlob(templateBasedDocument, templateName);

        // load the template
        IXDocReport report;
        try {
            report = XDocReportRegistry.getRegistry().loadReport(sourceTemplateBlob.getStream(),
                    TemplateEngineKind.Freemarker, false);
        } catch (XDocReportException e) {
            throw new IOException(e);
        }

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
        DocumentModel doc = templateBasedDocument.getAdaptedDoc();
        Map<String, Object> ctx = fmContextBuilder.build(doc, templateName);

        XDocReportBindingResolver resolver = new XDocReportBindingResolver(metadata);
        resolver.resolve(params, ctx, templateBasedDocument);

        // add default context vars
        IContext context;
        try {
            context = report.createContext();
        } catch (XDocReportException e) {
            throw new IOException(e);
        }
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
        File generated = new File(workingDir, "XDOCReportresult-" + System.currentTimeMillis());
        generated.createNewFile();

        OutputStream out = new FileOutputStream(generated);

        try {
            report.process(context, out);
        } catch (XDocReportException e) {
            throw new IOException(e);
        }

        Blob newBlob = Blobs.createBlob(generated);

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

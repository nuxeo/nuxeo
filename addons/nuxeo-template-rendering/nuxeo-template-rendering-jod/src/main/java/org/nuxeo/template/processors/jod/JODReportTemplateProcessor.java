/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.processors.jod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.jooreports.templates.DocumentTemplate;
import net.sf.jooreports.templates.DocumentTemplateFactory;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.fm.FMContextBuilder;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.template.odt.OOoArchiveModifier;
import org.nuxeo.template.processors.AbstractTemplateProcessor;

/**
 * {@link TemplateProcessor} for ODT based templates. Using JODReports but also custom ODT hacks. May be migrated to
 * pure ODT + Custom Freemarker soon.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class JODReportTemplateProcessor extends AbstractTemplateProcessor implements TemplateProcessor {

    public static final String TEMPLATE_TYPE = "JODTemplate";

    protected FMContextBuilder fmContextBuilder = new FMContextBuilder();

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) throws Exception {

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        String xmlContent = readXMLContent(blob);

        List<String> vars = FreeMarkerVariableExtractor.extractVariables(xmlContent);

        for (String var : vars) {
            TemplateInput input = new TemplateInput(var);
            params.add(input);
        }

        // add includes
        // params.addAll(IncludeManager.getIncludes(xmlContent));

        return params;
    }

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument, String templateName) throws Exception {

        OOoArchiveModifier modifier = new OOoArchiveModifier();

        Blob sourceTemplateBlob = templateBasedDocument.getTemplateBlob(templateName);
        if (templateBasedDocument.getSourceTemplateDoc(templateName) != null) {
            sourceTemplateBlob = templateBasedDocument.getSourceTemplateDoc(templateName).getAdapter(
                    TemplateSourceDocument.class).getTemplateBlob();
        }
        List<TemplateInput> params = templateBasedDocument.getParams(templateName);

        // init Jod template from the template DocumentModel Blob
        DocumentTemplateFactory documentTemplateFactory = new DocumentTemplateFactory();
        DocumentTemplate template = documentTemplateFactory.getTemplate(sourceTemplateBlob.getStream());

        // build fm context
        Map<String, Object> context = new HashMap<String, Object>();

        // store Blobs to be inserted
        List<Blob> blobsToInsert = new ArrayList<Blob>();

        DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

        for (TemplateInput param : params) {
            if (param.isSourceValue()) {
                Property property = null;
                try {
                    property = templateBasedDocument.getAdaptedDoc().getProperty(param.getSource());
                } catch (Throwable e) {
                    log.warn("Unable to ready property " + param.getSource(), e);
                }
                if (property != null) {
                    Serializable value = property.getValue();
                    if (value != null) {
                        if (Blob.class.isAssignableFrom(value.getClass())) {
                            Blob blob = (Blob) value;
                            if (param.getType() == InputType.PictureProperty) {
                                if (blob.getMimeType() == null || "".equals(blob.getMimeType().trim())) {
                                    blob.setMimeType("image/jpeg");
                                }
                                context.put(param.getName(), blob);
                                blobsToInsert.add((Blob) value);
                            }
                        } else {
                            context.put(param.getName(), nuxeoWrapper.wrap(property));
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
                        } else if (pType.getName().equals(StringType.ID)) {
                            context.put(param.getName(), "");
                        } else {
                            context.put(param.getName(), new Object());
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
        context.putAll(fmContextBuilder.build(doc, templateName));

        File workingDir = getWorkingDir();
        File generated = new File(workingDir, "JODReportresult");
        generated.createNewFile();

        template.createDocument(context, new FileOutputStream(generated));

        generated = modifier.updateArchive(workingDir, generated, blobsToInsert);

        Blob newBlob = Blobs.createBlob(generated);
        newBlob.setMimeType("application/vnd.oasis.opendocument.text");
        if (templateBasedDocument.getTemplateBlob(templateName) != null) {
            newBlob.setFilename(templateBasedDocument.getTemplateBlob(templateName).getFilename());
        } else {
            newBlob.setFilename(sourceTemplateBlob.getFilename());
        }

        // mark the file for automatic deletion on GC
        Framework.trackFile(generated, newBlob);

        return newBlob;
    }

    public String readXMLContent(Blob blob) throws Exception {
        ZipInputStream zIn = new ZipInputStream(blob.getStream());
        ZipEntry zipEntry = zIn.getNextEntry();
        String xmlContent = null;
        while (zipEntry != null) {
            if (zipEntry.getName().equals("content.xml")) {
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = zIn.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, read));
                }
                xmlContent = sb.toString();
                break;
            }
            zipEntry = zIn.getNextEntry();
        }
        zIn.close();
        return xmlContent;
    }

}

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
package org.nuxeo.template.processors.jxls;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.jxls.exception.ParsePropertyException;
import net.sf.jxls.transformer.XLSTransformer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.context.SimpleContextBuilder;
import org.nuxeo.template.processors.AbstractTemplateProcessor;

/**
 * JXLS {@link TemplateProcessor}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class JXLSTemplateProcessor extends AbstractTemplateProcessor {

    public static final String TEMPLATE_TYPE = "JXLS";

    protected SimpleContextBuilder contextBuilder = new SimpleContextBuilder();

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument, String templateName) throws IOException {

        Blob sourceTemplateBlob = getSourceTemplateBlob(templateBasedDocument, templateName);
        List<TemplateInput> params = templateBasedDocument.getParams(templateName);

        DocumentModel doc = templateBasedDocument.getAdaptedDoc();
        Map<String, Object> ctx = contextBuilder.build(doc, templateName);

        JXLSBindingResolver resolver = new JXLSBindingResolver();

        resolver.resolve(params, ctx, templateBasedDocument);

        File workingDir = getWorkingDir();
        File generated = new File(workingDir, "JXLSresult-" + System.currentTimeMillis());
        generated.createNewFile();

        File input = new File(workingDir, "JXLSInput-" + System.currentTimeMillis());
        input.createNewFile();

        sourceTemplateBlob.transferTo(input);

        XLSTransformer transformer = new XLSTransformer();
        configureTransformer(transformer);
        try {
            transformer.transformXLS(input.getAbsolutePath(), ctx, generated.getAbsolutePath());
        } catch (InvalidFormatException | ParsePropertyException e) {
            throw new NuxeoException(e);
        }

        input.delete();

        Blob newBlob = Blobs.createBlob(generated);

        String templateFileName = sourceTemplateBlob.getFilename();

        // set the output file name
        String targetFileExt = FileUtils.getFileExtension(templateFileName);
        String targetFileName = FileUtils.getFileNameNoExt(templateBasedDocument.getAdaptedDoc().getTitle());
        targetFileName = targetFileName + "." + targetFileExt;
        newBlob.setFilename(targetFileName);
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        newBlob.setMimeType(mimetypeRegistry.getMimetypeFromExtension(targetFileExt));

        // mark the file for automatic deletion on GC
        Framework.trackFile(generated, newBlob);
        return newBlob;

    }

    protected void configureTransformer(XLSTransformer transformer) {
        // NOP but subclass may use this to register a CellProcessor or a
        // RowProcessor
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) {
        return new ArrayList<TemplateInput>();
    }

}

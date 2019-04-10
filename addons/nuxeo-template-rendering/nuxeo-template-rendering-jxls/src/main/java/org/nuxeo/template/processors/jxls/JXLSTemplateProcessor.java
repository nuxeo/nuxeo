package org.nuxeo.template.processors.jxls;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.jxls.transformer.XLSTransformer;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
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
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument, String templateName) throws Exception {

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
        transformer.transformXLS(input.getAbsolutePath(), ctx, generated.getAbsolutePath());

        input.delete();

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

    protected void configureTransformer(XLSTransformer transformer) {
        // NOP but subclass may use this to register a CellProcessor or a
        // RowProcessor
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob) throws Exception {
        return new ArrayList<TemplateInput>();
    }

}

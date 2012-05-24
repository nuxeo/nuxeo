package org.nuxeo.template.processors.fm;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.fm.FMContextBuilder;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;
import org.nuxeo.template.processors.AbstractTemplateProcessor;

import freemarker.cache.StringTemplateLoader;

public class FreeMarkerProcessor extends AbstractTemplateProcessor implements
        TemplateProcessor {

    protected StringTemplateLoader loader = new StringTemplateLoader();

    protected FreemarkerEngine fmEngine = null;

    protected FMContextBuilder fmContextBuilder = new FMContextBuilder();

    protected FreemarkerEngine getEngine() {
        if (fmEngine == null) {
            fmEngine = new FreemarkerEngine();
            fmEngine.getConfiguration().setTemplateLoader(loader);
        }
        return fmEngine;
    }

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument,
            String templateName) throws Exception {

        Blob sourceTemplateBlob = getSourceTemplateBlob(templateBasedDocument,
                templateName);

        String fmTemplateKey = "main" + System.currentTimeMillis();

        String ftl = sourceTemplateBlob.getString();

        loader.putTemplate(fmTemplateKey, ftl);

        Map<String, Object> ctx = fmContextBuilder.build(templateBasedDocument,
                templateName);

        FMBindingResolver resolver = new FMBindingResolver();
        resolver.resolve(templateBasedDocument.getParams(templateName), ctx,
                templateBasedDocument);

        StringWriter writer = new StringWriter();
        getEngine().render(fmTemplateKey, ctx, writer);

        Blob result = new StringBlob(writer.toString());

        result.setMimeType("text/html");
        String targetFileName = FileUtils.getFileNameNoExt(templateBasedDocument.getAdaptedDoc().getTitle());
        result.setFilename(targetFileName + ".html");

        return result;
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob)
            throws Exception {
        List<TemplateInput> params = new ArrayList<TemplateInput>();

        if (blob != null) {
            String xmlContent = blob.getString();

            if (xmlContent != null) {
                List<String> vars = FreeMarkerVariableExtractor.extractVariables(xmlContent);

                for (String var : vars) {
                    TemplateInput input = new TemplateInput(var);
                    params.add(input);
                }
            }
        }
        return params;
    }

}

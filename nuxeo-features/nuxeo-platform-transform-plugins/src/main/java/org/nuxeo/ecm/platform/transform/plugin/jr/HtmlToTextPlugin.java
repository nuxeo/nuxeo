package org.nuxeo.ecm.platform.transform.plugin.jr;

import org.apache.jackrabbit.extractor.HTMLTextExtractor;
import org.apache.jackrabbit.extractor.TextExtractor;

public class HtmlToTextPlugin extends AbstractJRBasedTextExtractorPlugin {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected TextExtractor getExtractor() {
        return new HTMLTextExtractor();
    }

}

package org.nuxeo.template.web.url;

import org.nuxeo.ecm.platform.rendition.url.RenditionBasedCodec;

public class TemplateRenderCodec extends RenditionBasedCodec {

    public static final String PREFIX = "nxtemplate";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

}

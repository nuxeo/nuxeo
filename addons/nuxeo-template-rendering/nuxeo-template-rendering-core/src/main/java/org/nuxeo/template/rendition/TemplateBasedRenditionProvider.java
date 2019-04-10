package org.nuxeo.template.rendition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

public class TemplateBasedRenditionProvider implements RenditionProvider {

    protected static Log log = LogFactory.getLog(TemplateBasedRenditionProvider.class);

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        if (tbd != null) {
            // check if some template has been bound to a rendition
            String template = tbd.getTemplateNameForRendition(def.getName());
            return template == null ? false : true;
        }
        return false;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        String template = tbd.getTemplateNameForRendition(definition.getName());
        Blob rendered = tbd.renderWithTemplate(template);
        List<Blob> blobs = new ArrayList<Blob>();
        blobs.add(rendered);
        return blobs;
    }

}

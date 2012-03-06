package org.nuxeo.ecm.platform.template.rendition;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;

public class TemplateBasedRenditionProvider implements RenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc) {
        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        if (tbd != null) {
            // check is some template has been bound to a rendition !?
            return true;
        }
        return false;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition)
            throws RenditionException {

        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        try {
            Blob rendered = tbd.renderWithTemplate();
            List<Blob> blobs = new ArrayList<Blob>();
            blobs.add(rendered);
            return blobs;
        } catch (Exception e) {
            throw new RenditionException(
                    "Unable to render template based Document", e);
        }
    }

}

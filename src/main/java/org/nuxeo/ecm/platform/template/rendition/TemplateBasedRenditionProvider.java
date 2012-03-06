package org.nuxeo.ecm.platform.template.rendition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;

public class TemplateBasedRenditionProvider implements RenditionProvider {

    protected static Log log = LogFactory.getLog(TemplateBasedRenditionProvider.class);

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        if (tbd != null) {
            try {
                // check is some template has been bound to a rendition
                if (def.getName().equals(
                        tbd.getSourceTemplate().getTargetRenditionName())) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Error while testing Rendition Availability", e);
                return false;
            }
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

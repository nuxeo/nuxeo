package org.nuxeo.template.rendition;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

public class HtmlRenditionProvider implements RenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        if (holder != null) {
            return true;
        }
        return false;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition)
            throws RenditionException {
        try {
            HtmlPreviewAdapter preview = doc.getAdapter(HtmlPreviewAdapter.class);
            return preview.getFilePreviewBlobs();
        } catch (Exception e) {
            throw new RenditionException("Unable to compute Html Preview", e);
        }
    }

}

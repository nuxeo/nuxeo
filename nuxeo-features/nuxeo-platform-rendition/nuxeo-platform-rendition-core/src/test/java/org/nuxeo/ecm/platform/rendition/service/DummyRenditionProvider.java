package org.nuxeo.ecm.platform.rendition.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;

public class DummyRenditionProvider implements RenditionProvider {

    private static final String CONTENT_TYPE = "dummy/pdf";

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        return true;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        Blob blob = Blobs.createBlob(doc.getTitle(), CONTENT_TYPE);
        List<Blob> blobs = new ArrayList<Blob>();
        blobs.add(blob);
        return blobs;
    }

    @Override public String generateVariant(DocumentModel doc, RenditionDefinition definition) {
        return null;
    }

}

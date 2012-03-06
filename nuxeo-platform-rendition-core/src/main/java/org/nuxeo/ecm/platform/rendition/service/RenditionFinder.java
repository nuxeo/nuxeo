package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_NAME_PROPERTY;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class RenditionFinder extends UnrestrictedSessionRunner {

    protected final DocumentModel source;

    protected DocumentModel storedRendition;

    protected final String definitionName;

    protected RenditionFinder(DocumentModel source, String definitionName) {
        super(source.getCoreSession());
        this.source = source;
        this.definitionName = definitionName;
    }

    @Override
    public void run() throws ClientException {

        String query = "select * from Document where ";
        query = query + RENDITION_NAME_PROPERTY + "='" + definitionName
                + "' AND ";
        if (source.isVersion()) {
            query = query + RENDITION_SOURCE_ID_PROPERTY + "='"
                    + source.getId() + "' ";
        } else {
            query = query + RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY + "='"
                    + source.getId() + "' ";
        }

        query = query + " order by dc:modified desc ";

        List<DocumentModel> docs = session.query(query);
        if (docs.size() > 0) {
            storedRendition = docs.get(0);
        }
    }

    public DocumentModel getStoredRendition() {
        return storedRendition;
    }

}

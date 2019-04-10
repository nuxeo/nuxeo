package org.nuxeo.ecm.platform.suggestbox.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Suggest to navigate to a specific document.
 */
public class DocumentSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    protected final DocumentLocation documentLocation;

    public DocumentSuggestion(DocumentLocation documentLocation, String label,
            String iconURL) {
        super(CommonSuggestionTypes.DOCUMENT, label, iconURL);
        this.documentLocation = documentLocation;
    }

    public static Suggestion fromDocumentModel(DocumentModel doc)
            throws ClientException {
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        String description = doc.getProperty("dc:description").getValue(
                String.class);
        return new DocumentSuggestion(new DocumentLocationImpl(doc),
                doc.getTitle(), typeInfo.getIcon()).withDescription(description);
    }

    public DocumentLocation getDocumentLocation() {
        return documentLocation;
    }

}

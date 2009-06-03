package org.nuxeo.dam.platform.context;

import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Name;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * Helper for managing context.
 *
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 */
@Name("urlPatternHelper")
public class URLPatternHelper {

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String initContextFromRestRequest(DocumentView docView)
            throws ClientException {
        // Whatever is the document view, view is the same
        return null;
    }

    public DocumentView getNewDocumentView() throws ClientException {
        // DAM views are not document centric...
        return null;
    }



    public DocumentView getDocumentView() {
        // DAM views are not document centric...
        return null;
    }

}

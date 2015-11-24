package org.nuxeo.opensocial.container.server.webcontent.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.opensocial.container.server.service.WebContentSaverService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stéphane Fourrier
 */
public class WebContentAdapterFactory implements DocumentAdapterFactory {
    private static final Log log = LogFactory.getLog(WebContentAdapterFactory.class);

    @SuppressWarnings("unchecked")
    public Object getAdapter(DocumentModel doc, Class itf) {
        WebContentSaverService service;
        try {
            service = Framework.getService(WebContentSaverService.class);

            return service.getWebContentAdapterFor(doc).getConstructor(
                    DocumentModel.class).newInstance(doc);
        } catch (Exception e) {
            log.error("Unable to find an adpater for : " + doc.getType(), e);
            return null;
        }
    }
}

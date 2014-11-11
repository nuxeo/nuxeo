/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */


package org.nuxeo.ecm.core.event.test.virusscan.listeners;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.virusscan.VirusScanConsts;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.api.Framework;

/**
 * Synchronous listener that intercept Document create/update events.
 * <p/>
 * This listener detects if Blobs have been modified inside the doc, and if yes,
 * it will raise the virusScanNeeded so that the async listener can do the real
 * job in async mode.
 * <p/>
 * The work done in sync includes extracting dirty Blobs xpath that are then
 * tranmisted to the Async listener using a custom extended
 * {@link VirusScanEventContext}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class VirusScanSyncListener implements EventListener {

    protected static final Log log = LogFactory.getLog(VirusScanSyncListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            DocumentModel targetDoc = docCtx.getSourceDocument();

            Boolean block = (Boolean) event.getContext().getProperty(
                    VirusScanConsts.DISABLE_VIRUSSCAN_LISTENER);
            if (block != null && block) {
                // ignore the event - we are blocked by the caller
                return;
            }

            List<String> propertiesPath = null;

            if (DocumentEventTypes.ABOUT_TO_CREATE.equals(event.getName())) {
                // add the facet before save
                markDocumentForScaning(targetDoc);
            } else if (DocumentEventTypes.DOCUMENT_CREATED.equals(event.getName())) {
                // process Blobs now that document is created
                propertiesPath = getBlobsXPath(targetDoc, false);
            } else if (DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
                // process Blobs before update
                propertiesPath = getBlobsXPath(targetDoc, true);
                markDocumentForScaning(targetDoc);
            }

            if (propertiesPath != null && propertiesPath.size() > 0) {
                VirusScanEventContext virusScanCtx = new VirusScanEventContext(
                        docCtx, propertiesPath);

                EventService eventService = Framework.getLocalService(EventService.class);
                eventService.fireEvent(virusScanCtx.newVirusScanEvent());
            }
        }
    }

    protected void markDocumentForScaning(DocumentModel doc)
            throws ClientException {
        if (!doc.hasFacet(VirusScanConsts.VIRUSSCAN_FACET)) {
            doc.addFacet(VirusScanConsts.VIRUSSCAN_FACET);
        }
        doc.setPropertyValue(VirusScanConsts.VIRUSSCAN_STATUS_PROP,
                VirusScanConsts.VIRUSSCAN_STATUS_PENDING);
    }

    protected List<String> getBlobsXPath(DocumentModel doc,
            boolean onlyChangedBlob) throws ClientException {

        List<String> propertiesPath = new ArrayList<String>();
        BlobsExtractor extractor = new BlobsExtractor();

        try {
            List<Property> blobProperties = extractor.getBlobsProperties(doc);

            for (Property prop : blobProperties) {
                if (onlyChangedBlob) {
                    if (prop.isDirty()) {
                        propertiesPath.add(prop.getPath());
                    }
                } else {
                    propertiesPath.add(prop.getPath());
                }
            }
        } catch (Exception e) {
            log.error("Error when scanning blobs from Document", e);
            throw new ClientException(e);
        }

        return propertiesPath;
    }

}

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.extension;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * Class introduced to share code between sync and lazy automation based renditions
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class AutomationRenderer {

    protected static final Log log = LogFactory.getLog(AutomationRenderer.class);

    public static boolean isRenditionAvailable(DocumentModel doc, RenditionDefinition def) {
        String chain = def.getOperationChain();
        if (chain == null) {
            log.error("Can not run Automation rendition if chain is not defined");
            return false;
        }
        AutomationService as = Framework.getLocalService(AutomationService.class);

        try {
            if (as.getOperation(chain) == null) {
                log.error("Chain " + chain + " is not defined : rendition can not be used");
                return false;
            }
        } catch (Exception e) {
            log.error("Unable to test Rendition availability", e);
            return false;
        }

        if (!def.isEmptyBlobAllowed()) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh == null) {
                return false;
            }
            try {
                Blob blob = bh.getBlob();
                if (blob == null) {
                    return false;
                }
            } catch (Exception e) {
                log.error("Unable to get Blob to test Rendition availability", e);
                return false;
            }
        }
        return true;
    }

    public static List<Blob> render(DocumentModel doc, RenditionDefinition definition, CoreSession session)
            throws RenditionException {

        String chain = definition.getOperationChain();
        if (chain == null) {
            throw new RenditionException("no operation defined");
        }

        if (session == null) {
            session = doc.getCoreSession();
        }
        AutomationService as = Framework.getLocalService(AutomationService.class);
        OperationContext oc = new OperationContext(session);
        oc.push(Constants.O_DOCUMENT, doc);

        try {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                try {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        oc.push(Constants.O_BLOB, blob);
                    }
                } catch (Exception e) {
                    if (!definition.isEmptyBlobAllowed()) {
                        throw new RenditionException("No Blob available", e);
                    }
                }
            } else {
                if (!definition.isEmptyBlobAllowed()) {
                    throw new RenditionException("No Blob available");
                }
            }

            Blob blob = (Blob) as.run(oc, definition.getOperationChain());
            List<Blob> blobs = new ArrayList<Blob>();
            blobs.add(blob);
            return blobs;

        } catch (Exception e) {
            throw new RenditionException("Exception while running the operation chain: "
                    + definition.getOperationChain(), e);
        }
    }

}

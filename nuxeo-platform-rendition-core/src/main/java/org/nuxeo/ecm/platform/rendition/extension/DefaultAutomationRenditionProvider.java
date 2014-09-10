package org.nuxeo.ecm.platform.rendition.extension;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

public class DefaultAutomationRenditionProvider implements RenditionProvider {

    protected static final Log log = LogFactory.getLog(DefaultAutomationRenditionProvider.class);

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {

        String chain = def.getOperationChain();
        if (chain == null) {
            log.error("Can not run Automation rendition if chain is not defined");
            return false;
        }
        AutomationService as = Framework.getLocalService(AutomationService.class);

        try {
            if (as.getOperationChain(chain) == null) {
                log.error("Chain " + chain
                        + " is not defined : rendition can not be used");
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
                log.error("Unable to get Blob to test Rendition availability",
                        e);
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition)
            throws RenditionException {

        String chain = definition.getOperationChain();
        if (chain == null) {
            throw new RenditionException("no operation defined");
        }

        AutomationService as = Framework.getLocalService(AutomationService.class);
        OperationContext oc = new OperationContext();
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
            throw new RenditionException(
                    "Exception while running the operation chain: "
                            + definition.getOperationChain(), e);
        }
    }
}

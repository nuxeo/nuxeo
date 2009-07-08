package org.nuxeo.ecm.platform.publisher.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

public class VersioningHelper {

    private static Log log = LogFactory.getLog(VersioningHelper.class);

    private VersioningHelper() {
        // Helper class
    }

    private static VersioningManager getService() throws Exception {
        return Framework.getService(VersioningManager.class);
    }

    public static String getVersionLabelFor(DocumentModel doc) {
        try {
            return getService().getVersionLabel(doc);
        } catch (Exception e) {
            log.error("Unable to get VersionLabel for: "
                    + doc.getPathAsString(), e);
            return null;
        }
    }

}

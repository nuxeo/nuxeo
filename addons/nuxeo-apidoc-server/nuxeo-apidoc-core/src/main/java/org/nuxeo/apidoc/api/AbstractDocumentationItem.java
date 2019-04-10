package org.nuxeo.apidoc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.documentation.DocumentationComponent;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractDocumentationItem implements DocumentationItem {

    protected static final Log log = LogFactory.getLog(AbstractDocumentationItem.class);

    @Override
    public int compareTo(DocumentationItem o) {

        List<String> myVersions = new ArrayList<String>(getApplicableVersion());
        List<String> otherVersions = new ArrayList<String>(
                o.getApplicableVersion());

        Collections.sort(myVersions);
        Collections.sort(otherVersions);
        Collections.reverse(myVersions);
        Collections.reverse(otherVersions);

        if (myVersions.isEmpty()) {
            if (otherVersions.isEmpty()) {
                return 0;
            }
            return 1;
        } else if (otherVersions.isEmpty()) {
            return -1;
        }

        return myVersions.get(0).compareTo(otherVersions.get(0));
    }

    @Override
    public String getTypeLabel() {
        String type = getType();
        if ("".equals(type)) {
            return "";
        }
        if (Framework.isTestModeSet()) {
            return type;
        }
        Session session = null;
        try {
            DirectoryService dm = Framework.getService(DirectoryService.class);
            session = dm.open(DocumentationComponent.DIRECTORY_NAME);
            DocumentModel entry = session.getEntry(type);
            return (String) entry.getProperty("vocabulary", "label");
        } catch (Exception e) {
            log.error("Error while resolving typeLabel", e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                    log.warn("Error while close directory session", e);
                }
            }
        }
        return "";
    }

}

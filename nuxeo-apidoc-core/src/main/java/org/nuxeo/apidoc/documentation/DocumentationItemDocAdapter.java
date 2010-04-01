package org.nuxeo.apidoc.documentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class DocumentationItemDocAdapter implements DocumentationItem {

    protected DocumentModel doc;

    public static final String DOC_TYPE = "NXDocumentation";

    protected static Log log = LogFactory
            .getLog(DocumentationItemDocAdapter.class);

    public DocumentationItemDocAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    public DocumentModel getDocumentModel() {
        return doc;
    }

    @SuppressWarnings("unchecked")
    public List<String> getApplicableVersion() {
        try {
            return (List<String>) doc
                    .getPropertyValue("nxdoc:applicableVersions");
        } catch (Exception e) {
            log.error("Error while reading applicable version", e);
            return new ArrayList<String>();
        }
    }

    public String getContent() {
        try {
            Blob blob = (Blob) doc.getPropertyValue("file:content");
            return blob.getString();
        } catch (Exception e) {
            log.error("Error while reading content", e);
            return "";
        }
    }

    public String getRenderingType() {
        try {
            return (String) doc.getPropertyValue("nxdoc:renderingType");
        } catch (Exception e) {
            log.error("Error while reading rendering type", e);
            return "";
        }
    }

    public String getTarget() {
        try {
            return (String) doc.getPropertyValue("nxdoc:target");
        } catch (Exception e) {
            log.error("Error while reading target", e);
            return "";
        }
    }

    public String getTargetType() {
        try {
            return (String) doc.getPropertyValue("nxdoc:targetType");
        } catch (Exception e) {
            log.error("Error while reading targetType", e);
            return "";
        }
    }

    public String getType() {
        try {
            return (String) doc.getPropertyValue("nxdoc:type");
        } catch (Exception e) {
            log.error("Error while reading type", e);
            return "";
        }
    }

    public String getTypeLabel() {
        String type = getType();
        if ("".equals(type)) {
            return "";
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

    public boolean isApproved() {
        try {
            return (Boolean) doc.getPropertyValue("nxdoc:nuxeoApproved");
        } catch (Exception e) {
            log.error("Error while reading type", e);
            return false;
        }
    }

    public String getId() {
        try {
            return (String) doc.getPropertyValue("nxdoc:documentationId");
        } catch (Exception e) {
            log.error("Error while reading target", e);
            return "";
        }
    }

    public String getUUID() {
        return doc.getId();
    }

    public String getTitle() {
        try {
            return (String) doc.getPropertyValue("dc:title");
        } catch (Exception e) {
            log.error("Error while reading title", e);
            return "";
        }
    }

    public Map<String, String> getAttachements() {

        Map<String, String> attachements = new LinkedMap();

        try {
            List<Map<String, Serializable>> atts = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
            if (atts!=null) {
                for (Map<String, Serializable> att : atts) {
                    attachements.put((String)att.get("filename"), ((Blob)att.get("file")).getString());
                }
            }
        }
        catch (Exception e) {
            log.error("Error while reading Attachements", e);
        }
        return attachements;
    }
}

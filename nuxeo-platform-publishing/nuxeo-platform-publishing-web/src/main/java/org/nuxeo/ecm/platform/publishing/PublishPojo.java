package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin BAICAN</a>
 *
 *         Simple POJO used in order to store additional info needed to be
 *         displayed in 'TAB_PUBLISH' page.
 *
 */
public class PublishPojo {

    DocumentModel section;

    String proxyVersion;

    public PublishPojo(DocumentModel section, String proxyVersion) {
        super();
        this.section = section;
        this.proxyVersion = proxyVersion;
    }

    public DocumentModel getSection() {
        return section;
    }

    public void setSection(DocumentModel section) {
        this.section = section;
    }

    public String getProxyVersion() {
        return proxyVersion;
    }

    public void setProxyVersion(String proxyVersion) {
        this.proxyVersion = proxyVersion;
    }

}

package org.nuxeo.template.jaxrs.context;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.context.DocumentWrapper;

public class JAXRSExtensions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    protected final String templateName;

    public JAXRSExtensions(DocumentModel doc, DocumentWrapper nuxeoWrapper,
            String templateName) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
        this.templateName = templateName;
    }

    protected static String getContextPathProperty() {
        return Framework.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
    }

    public String getResourceUrl(String resourceName) {
        StringBuffer sb = new StringBuffer(getContextPathProperty());
        sb.append("/site/templates/doc/");
        sb.append(doc.getId());
        sb.append("/resource/");
        sb.append(templateName);
        sb.append("/");
        sb.append(resourceName);
        return sb.toString();
    }
}

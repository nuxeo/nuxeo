package org.nuxeo.ecm.platform.site.api;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;

public interface SiteTemplateManager {

    InputStream getTemplateForDoc(DocumentModel doc);

    InputStream getTemplateFromName(String templateName);

    URL getTemplateUrlForDoc(DocumentModel doc);

    URL getTemplateUrlFromName(String templateName);

    String getTemplateNameForDoc(DocumentModel doc);

    List<String> getTemplateNames();

    String registerDynamicTemplate(SiteAwareObject site, String templateContent);

    RenderingEngine getRenderingEngine();
}

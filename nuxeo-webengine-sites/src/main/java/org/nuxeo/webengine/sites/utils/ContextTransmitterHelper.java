package org.nuxeo.webengine.sites.utils;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;

public class ContextTransmitterHelper {


    public static void feedContext(DocumentModel doc) {

        WebContext ctx = WebEngine.getActiveContext();
        String basePath = ctx.getModulePath();

        Resource target = ctx.getTargetObject();
        Resource parentResource = target.getPrevious();
        String siteName = "";
        while (parentResource!=null && !parentResource.isInstanceOf(SiteConstants.WEBSITE)) {
            parentResource = parentResource.getPrevious();
        }
        if (parentResource!=null) {
            siteName = parentResource.getName();
        }
        String targetObjectPath = target.getPath();
        doc.getContextData().putScopedValue("basePath", basePath);
        doc.getContextData().putScopedValue("siteName", siteName);
        doc.getContextData().putScopedValue("targetObjectPath", targetObjectPath);

    }
}

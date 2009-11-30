package org.nuxeo.dam.webapp.helper;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DownloadHelper {

    private static Log log = LogFactory.getLog(DownloadHelper.class);

    private DownloadHelper() {
        // Helper class
    }

    public static void download(FacesContext context, DocumentModel doc,
            String filePropertyPath, String filename) {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        String bigDownloadURL = BaseURL.getBaseURL(request);
        bigDownloadURL += "nxbigfile" + "/";
        bigDownloadURL += doc.getRepositoryName() + "/";
        bigDownloadURL += doc.getRef().toString() + "/";
        bigDownloadURL += filePropertyPath + "/";
        bigDownloadURL += URIUtils.quoteURIPathComponent(filename, true);
        try {
            response.sendRedirect(bigDownloadURL);
        } catch (IOException e) {
            log.error("Error while redirecting for big file downloader", e);
        }
    }
    
    public static void downloadWithWatermark(FacesContext context,
            DocumentModel doc, String filePropertyPath, String filename) {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        StringBuilder bigDownloadURL = new StringBuilder(
                BaseURL.getBaseURL(request));
        bigDownloadURL.append("watermark/");
        bigDownloadURL.append(doc.getRepositoryName()).append('/');
        bigDownloadURL.append(doc.getRef().toString()).append('/');
        bigDownloadURL.append(filePropertyPath).append('/');
        bigDownloadURL.append(URIUtils.quoteURIPathComponent(filename, true));
        try {
            response.sendRedirect(bigDownloadURL.toString());
        } catch (IOException e) {
            log.error("Error while redirecting for big file downloader", e);
        }
    }    

}

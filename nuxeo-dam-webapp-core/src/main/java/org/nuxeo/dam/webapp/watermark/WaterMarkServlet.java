package org.nuxeo.dam.webapp.watermark;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.remoting.transport.coyote.ClientAbortException;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;

public class WaterMarkServlet extends HttpServlet {
    
    private static final long serialVersionUID = -4809049542136414807L;
    
    protected static final int BUFFER_SIZE = 1024 * 512;

    public static final Log log = LogFactory.getLog(WaterMarkServlet.class);

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        request = new WaterMarkRequest(request);
        response = new WaterMarkResponse(response);
        String requestURI;
        try {
            requestURI = new URI(request.getRequestURI()).getPath();
        } catch (URISyntaxException e1) {
            requestURI = request.getRequestURI();
        }
        String filePath = requestURI.replace("/nuxeo/nxbigfile/", "");
        String[] pathParts = filePath.split("/");

        String repoName = pathParts[0];
        InputStream in = null;

        try {
            UnrestrictedSessionCreator creator = new UnrestrictedSessionCreator(
                    request, response, in, repoName);
            creator.runUnrestricted();
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (response != null) {
                try {
                    response.flushBuffer();
                } catch (ClientAbortException cae) {
                    log.warn("Download aborted by client");
                }
            }
            if (in != null) {
                in.close();
            }
        }
    }

    protected static class UnrestrictedSessionCreator extends
            UnrestrictedSessionRunner {
        protected HttpServletRequest req;

        protected HttpServletResponse resp;

        protected InputStream in;

        public UnrestrictedSessionCreator(HttpServletRequest req,
                HttpServletResponse resp, InputStream in, String repositoryName) {

            super(repositoryName);
            this.req = req;
            this.resp = resp;
            this.in = in;
        }

        @Override
        public void run() throws ClientException {

            String requestURI;
            try {
                requestURI = new URI(req.getRequestURI()).getPath();
            } catch (URISyntaxException e1) {
                requestURI = req.getRequestURI();
            }
            String filePath = requestURI.replace("/nuxeo/nxbigfile/", "");
            String[] pathParts = filePath.split("/");

            String docId = pathParts[1];
            String fieldPath = pathParts[2];
            String fileName = pathParts[3];

            String completePath = filePath.split(docId)[1];
            int idx = completePath.lastIndexOf('/');
            if (idx > 0) {
                fieldPath = completePath.substring(0, idx);
                fileName = completePath.substring(idx + 1);
            }
            try {
                DocumentModel doc = session.getDocument(new IdRef(docId));
                Blob blob;
                if (fieldPath != null) {
                    // Hack for Flash Url wich doesn't support ':' char
                    fieldPath = fieldPath.replace(';', ':');
                    // BlobHolder urls
                    if (fieldPath.startsWith("/blobholder")) {
                        BlobHolder bh = doc.getAdapter(BlobHolder.class);
                        if (bh == null) {
                            return;
                        }
                        String bhPath = fieldPath.replace("/blobholder:", "");
                        if ("".equals(bhPath) || "0".equals(bhPath)) {
                            blob = bh.getBlob();
                        } else {
                            int idxbh = Integer.parseInt(bhPath);
                            blob = bh.getBlobs().get(idxbh);
                        }
                    } else {
                        blob = (Blob) DocumentModelUtils.getPropertyValue(
                                doc,
                                DocumentModelUtils.decodePropertyName(fieldPath));
                        if (blob == null) {
                            // maybe it's a complex property
                            blob = (Blob) DocumentModelUtils.getComplexPropertyValue(
                                    doc, fieldPath);
                        }
                    }
                } else {
                    return;
                }

                if (fileName == null || fileName.length() == 0) {
                    fileName = "file";
                }
                boolean inline = req.getParameter("inline") != null;
                String userAgent = req.getHeader("User-Agent");
                String contentDisposition = RFC2231.encodeContentDisposition(
                        fileName, inline, userAgent);
                resp.setHeader("Content-Disposition", contentDisposition);
                resp.setContentType(blob.getMimeType());

                long fileSize = blob.getLength();
                if (fileSize > 0) {
                    resp.setContentLength((int) fileSize);
                }

                OutputStream out = resp.getOutputStream();
                in = blob.getStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (ClientAbortException cae) {
                log.warn("Download aborted by client");
            } catch (Exception e) {
                throw new ClientException(e);
            }

        }

        public CoreSession getSession() {
            return session;
        }
    }
}

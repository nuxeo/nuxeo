package org.nuxeo.ecm.platform.preview.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

public class PlainTextPreviewer extends AbstractPreviewer implements
        MimeTypePreviewer {

    public List<Blob> getPreview(Blob blob, DocumentModel dm)
            throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();

        StringBuffer htmlPage = new StringBuffer();

        htmlPage.append("<html>");
        try {
            String temp = blob.getString().replace("&", "&amp;").replace("<",
                    "&lt;").replace(">", "&gt;").replace("\'", "&apos;").replace(
                    "\"", "&quot;");
            htmlPage.append("<pre>").append(temp.replace("\n", "<br/>")).append(
                    "</pre>");
        } catch (IOException e) {
            throw new PreviewException(e);
        }
        htmlPage.append("</html>");

        Blob mainBlob = new StringBlob(htmlPage.toString());
        mainBlob.setFilename("index.html");
        mainBlob.setMimeType("text/html");

        blobResults.add(mainBlob);
        return blobResults;
    }
}
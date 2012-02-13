package org.nuxeo.sitefactory.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;

public abstract class AbstractContentEntry implements ContentEntry {

    protected final DocumentModel document;

    protected static final Log log = LogFactory.getLog(AbstractContentEntry.class);

    protected AbstractContentEntry(DocumentModel doc) {
        this.document = doc;
    }

    @Override
    public DocumentModel getDocument() {
        return document;
    }

    @Override
    public String getTitle() {
        try {
            return document.getTitle();
        } catch (ClientException e) {
            log.error(e);
            return null;
        }
    }

    public <T> T safeGet(String name, Class<T> propType) {
        try {
            return propType.cast(document.getPropertyValue(name));
        } catch (ClientException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public String getAbstract() {
        String result = null;
        try {
            result = (String) document.getPropertyValue("dc:description");
            if (result == null) {
                // XXX extract from content ?
            }
            return result;
        } catch (ClientException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public String getAuthor() {
        return safeGet("dc:creator", String.class);
    }

    @Override
    public List<String> getContributors() {
        return (List<String>) safeGet("dc:creator", Object.class);
    }

    @Override
    public Calendar getCreationDate() {
        return safeGet("dc:created", Calendar.class);
    }

    @Override
    public Calendar getModificationDate() {
        return safeGet("dc:modified", Calendar.class);
    }

    @Override
    public Calendar getPublicationDate() {
        Calendar date = safeGet("dc:isssued", Calendar.class);
        return date == null ? getModificationDate() : date;
    }

    @Override
    public Blob getRenderedContent() throws Exception {
        
        TemplateBasedDocument template = getDocument().getAdapter(TemplateBasedDocument.class);
        if (template!=null) {
            return template.renderWithTemplate();
        }
        HtmlPreviewAdapter preview = getDocument().getAdapter(HtmlPreviewAdapter.class);
        if (preview!=null) {
            List<Blob> blobs= preview.getFilePreviewBlobs();
            if (blobs!=null && blobs.size()>0) {
                return blobs.get(0);
            }                    
        }
        return null;
    }

    @Override
    public List<Blob> getAttachements() {
        try {
            BlobHolder bh = getDocument().getAdapter(BlobHolder.class);
            return bh.getBlobs();
        } catch (Exception e) {
            log.error(e);
            return new ArrayList<Blob>();
        }
    }
}

package org.nuxeo.ecm.platform.filemanager.core.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

public class BlobReferencer implements EventListener {

    private static final Log log = LogFactory.getLog(BlobReferencer.class);

    private List<String> xpathFields;

    public static String BINARY_REFERENCES = "binaryReferences";

    @Override
    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();
        String evt = event.getName();
        if (ABOUT_TO_CREATE.equals(evt) || BEFORE_DOC_UPDATE.equals(evt) || ABOUT_TO_REMOVE.equals(evt)) {
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;
                DocumentModel doc = docCtx.getSourceDocument();
                if (doc == null || doc.isProxy()) {
                    return;
                }
                FileManager fm = Framework.getService(FileManager.class);
                xpathFields = fm.getFields();
                if (xpathFields == null || xpathFields.isEmpty()) {
                    return;
                }
                updateKVStore(doc, event);
            }
        }
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(BINARY_REFERENCES);
    }

    protected void updateKVStore(DocumentModel doc, Event event) {
        String eventName = event.getName();
        for (String xpathField : xpathFields) {
            Property blobProp = null;
            try {
                blobProp = doc.getProperty(xpathField);
            } catch (PropertyException e) {
                log.debug("Property " + xpathField + " not found on doc, skipping");
            }
            if (blobProp == null || blobProp.isPhantom()) {
                return;
            }
            Blob blob = (Blob) blobProp.getValue();

            KeyValueStore kv = getKvStore();
            String digest = null;
            try {
                blob = (Blob) blobProp.getValue();
                if (blob != null) {
                    digest = blob.getDigest();
                    if (!digest.isEmpty()) {
                        // Check if entry exists in KV store, if yes increment count, if not create entry

                        switch (eventName) {
                        case ABOUT_TO_CREATE:
                            documentCreatedWithBlob(kv, digest);
                            break;
                        case BEFORE_DOC_UPDATE:
                            documentUpdatedWithBlob(kv, xpathField, digest, event);
                            break;
                        default:
                            DocumentDeleteWithBlob(kv, digest);
                        }
                    }
                }
            } catch (PropertyException e) {
                log.error("Error while trying to save blob digest", e);
            }
        }
    }

    protected void documentCreatedWithBlob(KeyValueStore kv, String digest) {
        Long nbOccurences = kv.getLong(digest);
        if (nbOccurences == null) {
            nbOccurences = 0L;
        }
        kv.put(digest, Long.valueOf(nbOccurences + 1));
         System.out.println("Entry updated (creation) in KV store for digest: " + digest + " count: "
         + kv.getLong(digest));
    }

    protected void documentUpdatedWithBlob(KeyValueStore kv, String xpathField, String newDigest, Event event) {
        EventContext ctx = event.getContext();
        DocumentModel previousDocModel = (DocumentModel) ctx.getProperty(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL);
        BinaryBlob blobProp = null;
        try {
            blobProp = (BinaryBlob) previousDocModel.getPropertyValue(xpathField);

        } catch (PropertyException e) {
            log.debug("Property " + xpathField + " not found on doc, skipping");
        }
        String oldDigest = blobProp.getDigest();
        Long nbOccurences = kv.getLong(oldDigest);
        if (nbOccurences == null || nbOccurences == 0) {
            nbOccurences = 0L;
            throw new NuxeoException("Error in the BlobReferncer counting");
        } else {
            kv.put(oldDigest, Long.valueOf(nbOccurences - 1));
        }

        nbOccurences = kv.getLong(newDigest);
        if (nbOccurences == null) {
            nbOccurences = 0L;
        }
        kv.put(newDigest, Long.valueOf(nbOccurences + 1));
        System.out.println(
                "Entry updated (update) in KV store for old unused digest: " + oldDigest + " count: " + kv.getLong(oldDigest));
        System.out.println(
                "Entry updated (update) in KV store for new digest: " + newDigest + " count: " + kv.getLong(newDigest));
    }

    protected void DocumentDeleteWithBlob(KeyValueStore kv, String digest) {
        Long nbOccurences = kv.getLong(digest);
        if (nbOccurences == null) {
            nbOccurences = 0L;
            throw new NuxeoException("Error in the BlobReferncer counting");
        }
        kv.put(digest, Long.valueOf(nbOccurences - 1));
         System.out.println("Entry updated (deletion) in KV store for digest: " + digest + " count: "
         + kv.getLong(digest));
    }
}

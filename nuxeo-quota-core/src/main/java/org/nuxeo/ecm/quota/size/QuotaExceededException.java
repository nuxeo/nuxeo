package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class QuotaExceededException extends ClientException {

    private static final long serialVersionUID = 1L;

    protected long quotaValue;

    protected String targetPath;

    protected String addedDocumentID;

    public QuotaExceededException(DocumentModel targetDocument, String message) {
        super(message + "on " + targetDocument.getPathAsString());
        this.targetPath = targetDocument.getPathAsString();
    }

    public QuotaExceededException(DocumentModel targetDocument,
            DocumentModel addedDocument, long quotaValue) {
        this(targetDocument.getPathAsString(), addedDocument.getId(),
                quotaValue);
    }

    public QuotaExceededException(String targetDocumentPath,
            String addedDocumentID, long quotaValue) {
        super("Quota Exceeded on " + targetDocumentPath);
        this.quotaValue = quotaValue;
        this.targetPath = targetDocumentPath;
        this.addedDocumentID = addedDocumentID;
    }

    public long getQuotaValue() {
        return quotaValue;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getAddedDocumentID() {
        return addedDocumentID;
    }

    public static QuotaExceededException unwrap(Throwable e) {
        if (e instanceof QuotaExceededException) {
            return (QuotaExceededException) e;
        } else {
            if (e.getCause() != null) {
                return unwrap(e.getCause());
            } else {
                return null;
            }
        }
    }

    public static boolean isQuotaExceededException(Throwable e) {
        return unwrap(e) != null;
    }
}

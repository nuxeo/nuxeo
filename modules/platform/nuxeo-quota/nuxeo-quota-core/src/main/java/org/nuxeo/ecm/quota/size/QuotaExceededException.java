/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Exception throws by the {@link DocumentsSizeUpdater} to enforce Quotas in case a transaction tries to add too
 * much Blobs
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaExceededException extends RecoverableClientException {

    private static final long serialVersionUID = 1L;

    protected long quotaValue;

    protected String targetPath;

    protected String addedDocumentID;

    public QuotaExceededException(DocumentModel targetDocument, String message) {
        super(message, "label.quotaException." + message, new String[] { targetDocument.getPathAsString() });
        this.targetPath = targetDocument.getPathAsString();
    }

    public QuotaExceededException(DocumentModel targetDocument, DocumentModel addedDocument, long quotaValue) {
        this(targetDocument.getPathAsString(), addedDocument.getId(), quotaValue);
    }

    public QuotaExceededException(String targetDocumentPath, String addedDocumentID, long quotaValue) {
        super("QuotaExceeded", "label.quotaException.QuotaExceeded",
                new String[] { targetDocumentPath, addedDocumentID });
        this.quotaValue = quotaValue;
        this.addedDocumentID = addedDocumentID;
        this.targetPath = targetDocumentPath;
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

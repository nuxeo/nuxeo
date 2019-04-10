/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Exception throws by the {@link QuotaSyncListenerChecker} to enforce Quotas in
 * case a transaction tries to add too much Blobs
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
        super(message, "label.quotaException." + message,
                new String[] { targetDocument.getPathAsString() });
        this.targetPath = targetDocument.getPathAsString();
    }

    public QuotaExceededException(DocumentModel targetDocument,
            DocumentModel addedDocument, long quotaValue) {
        this(targetDocument.getPathAsString(), addedDocument.getId(),
                quotaValue);
    }

    public QuotaExceededException(String targetDocumentPath,
            String addedDocumentID, long quotaValue) {
        super("QuotaExceeded", "label.quotaException.QuotaExceeded",
                new String[] { targetDocumentPath, addedDocumentID,
                        new Long(quotaValue).toString() });
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

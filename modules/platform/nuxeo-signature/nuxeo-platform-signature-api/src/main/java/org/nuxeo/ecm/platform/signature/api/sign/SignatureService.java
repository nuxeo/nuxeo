/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Wojciech Sulejman
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.signature.api.sign;

import java.security.cert.X509Certificate;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;

/**
 * Provides digital signature services that can be performed on PDF documents, e.g.:
 * <ul>
 * <li>signing a specific PDF,</li>
 * <li>obtaining a list of certificates already associated with a document.</li>
 * </ul>
 * A PDF document can be signed using a user certificate. This requires an existing user certificate present in the
 * system. A certificate password must be made available to use this service.
 */
public interface SignatureService {

    /**
     * Information about a blob and its signing status.
     */
    public class StatusWithBlob {

        /**
         * The signing status for a document that is not signable (no attachment or unsupported attachment type).
         */
        public static final int UNSIGNABLE = -1;

        /**
         * The signing status for a document that is not signed.
         */
        public static final int UNSIGNED = 0;

        /**
         * The signing status for a document that is signed by the current user (and maybe others).
         */
        public static final int SIGNED_CURRENT = 1;

        /**
         * The signing status for a document that is signed by users other than the current user.
         */
        public static final int SIGNED_OTHER = 2;

        /**
         * A document's status may be:
         * <ul>
         * <li>unsignable ({@link #UNSIGNABLE}),</li>
         * <li>unsigned ({@link #UNSIGNED}),</li>
         * <li>signed by the current user (and maybe also others) ( {@link #SIGNED_CURRENT}),</li>
         * <li>signed only by others ({@link #SIGNED_OTHER}).</li>
         * </ul>
         */
        public final int status;

        public final Blob blob;

        public final BlobHolder blobHolder;

        public final String path;

        public StatusWithBlob(int status, Blob blob, BlobHolder blobHolder, String path) {
            this.status = status;
            this.blob = blob;
            this.blobHolder = blobHolder;
            this.path = path;
        }

        public int getStatus() {
            return status;
        }

        public Blob getBlob() {
            return blob;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(status=" + status + ",path=" + path + ",blob=" + blob + ")";
        }
    }

    /**
     * Finds the signing status for the document.
     * <p>
     * A signature user is determined according to its email.
     *
     * @param doc the document
     * @return the signing status
     */
    StatusWithBlob getSigningStatus(DocumentModel doc, DocumentModel currentUser);

    enum SigningDisposition {
        /** Replace the main blob with the signed one. */
        REPLACE,
        /** Replace the main blob with the signed one and archive the original. */
        ARCHIVE,
        /** Put the signed blob as an attachment. */
        ATTACH
    }

    /**
     * Signs a document with a user certificate (converts it into a PDF first if needed).
     * <p>
     * Requires a password to retrieve the certificate from the user keystore.
     * <p>
     * Does not save the modified document.
     *
     * @param doc the document to sign
     * @param user the signing user
     * @param userKeyPassword the password for the user's signing certificate
     * @param reason the signing reason
     * @param pdfa {@code true} if the generated PDF should be a PDF/A-1b
     * @param disposition the signing disposition
     * @param archiveFilename the archive filename when using an archive
     * @return a blob containing the signed PDF
     * @throws SignException
     * @throws ConversionException
     */
    Blob signDocument(DocumentModel doc, DocumentModel user, String userKeyPassword, String reason, boolean pdfa,
            SigningDisposition disposition, String archiveFilename);

    /**
     * Signs a PDF document with a user certificate. Requires a password to retrieve the certificate from the user
     * keystore.
     *
     * @param doc Document model beign signed
     * @param pdfBlob the blob containing the PDF to sign
     * @param user the signing user
     * @param userKeyPassword the password for the user's signing certificate
     * @param reason the signing reason
     * @return a blob containing the signed PDF
     * @throws SignException
     */
    Blob signPDF(Blob pdfBlob, DocumentModel doc, DocumentModel user, String userKeyPassword, String reason) throws SignException;

    /**
     * Returns a list of certificates associated with a given document.
     *
     * @param doc the document
     * @return the list of certificates (may be empty)
     */
    List<X509Certificate> getCertificates(DocumentModel doc);

    SignatureLayout getSignatureLayout();
}

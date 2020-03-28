/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Encrypt/Decrypt a PDF.
 * <p>
 * Notice that encryption is not only about pure encryption, it is also about setting the available features on the pdf:
 * can print, copy, modify, ... (see PDFBox {@link AccessPermission}).
 * <p>
 * To use this class and its encrypt()/removeEncryption() methods you must always use the appropriate setters first, to
 * set the misc info (original owner password so an encrypted PDF can be handled, encryption key length, ...).
 *
 * @since 8.10
 */
public class PDFEncryption {

    private Blob pdfBlob;

    private PDDocument pdfDoc;

    private static final List<Integer> ALLOWED_LENGTH = Arrays.asList(40, 128);

    private static final int DEFAULT_KEYLENGTH = 128;

    private int keyLength = DEFAULT_KEYLENGTH;

    private String originalOwnerPwd;

    private String ownerPwd;

    private String userPwd;

    /**
     * Basic constructor.
     *
     * @param inBlob Input blob.
     */
    public PDFEncryption(Blob inBlob) {
        pdfBlob = inBlob;
    }

    /**
     * Encrypts the PDF with readonly permission.
     * <p>
     * WARNING: If you are familiar with PDFBox {@link AccessPermission}, notice our encryptReadOnly() method is not the
     * same as {@link AccessPermission#AccessPermission#setReadOnly}. The latter just makes sure the code cannot call
     * other setter later on.
     * <p>
     * <code>encryptReadOnly</code> sets the following permissions on the document:
     * <ul>
     * <li>Can print: True</li>
     * <li>Can Modify: False</li>
     * <li>Can Extract Content: True</li>
     * <li>Can Add/Modify annotations: False</li>
     * <li>Can Fill Forms: False</li>
     * <li>Can Extract Info for Accessibility: True</li>
     * <li>Can Assemble: False</li>
     * <li>Can print degraded: True</li>
     * </ul>
     * <p>
     * <b>IMPORTANT
     * </p>
     * It is required that the following setters are called <i>before</i>:
     * <ul>
     * <li>{@link PDFEncryption#setOriginalOwnerPwd}: Only if the original PDF already is encrypted. This password
     * allows to open it for modification.</li>
     * <li>{@link PDFEncryption#setKeyLength}: To set the length of the key.</li>
     * <li>{@link PDFEncryption#setOwnerPwd}: The password for the owner. If not called, <code>originalOwnerPwd</code>
     * is used instead.</li>
     * <li>{@link PDFEncryption#setUserPwd}: The password for the user.</li>
     * </ul>
     *
     * @return A copy of the blob with the readonly permissions set.
     */
    public Blob encryptReadOnly() {
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(true);
        ap.setCanModify(false);
        ap.setCanExtractContent(true);
        ap.setCanModifyAnnotations(false);
        ap.setCanFillInForm(false);
        ap.setCanExtractForAccessibility(true);
        ap.setCanAssembleDocument(false);
        ap.setCanPrintDegraded(true);
        return encrypt(ap);
    }

    /**
     * Encrypts the PDF with the new permissions (see {@link AccessPermission}).
     * <p>
     * <b>IMPORTANT
     * </p>
     * It is required that the following setters are called <i>before</i>:
     * <ul>
     * <li>{@link PDFEncryption#setOriginalOwnerPwd}: Only if the original PDF already is encrypted. This password
     * allows to open it for modification.</li>
     * <li>{@link PDFEncryption#setKeyLength}: To set the length of the key.</li>
     * <li>{@link PDFEncryption#setOwnerPwd}: The password for the owner. If not called, <code>originalOwnerPwd</code>
     * is used instead.</li>
     * <li>{@link PDFEncryption#setUserPwd}: The password for the user.</li>
     * </ul>
     *
     * @param inPerm Input permissions.
     * @return A copy of the blob with the new permissions set.
     */
    public Blob encrypt(AccessPermission inPerm) {
        if (!ALLOWED_LENGTH.contains(keyLength)) {
            throw new NuxeoException(keyLength + " is not an allowed length for the encrytion key");
        }
        ownerPwd = (StringUtils.isBlank(ownerPwd)) ? originalOwnerPwd : ownerPwd;
        try {
            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPwd, userPwd, inPerm);
            spp.setEncryptionKeyLength(keyLength);
            pdfDoc = PDDocument.load(pdfBlob.getFile(), originalOwnerPwd);
            pdfDoc.protect(spp);
            Blob result = Blobs.createBlobWithExtension(".pdf");
            pdfDoc.save(result.getFile());
            result.setMimeType("application/pdf");
            if (StringUtils.isNotBlank(pdfBlob.getFilename())) {
                result.setFilename(pdfBlob.getFilename());
            }
            pdfDoc.close();
            FileBlob fb = new FileBlob(result.getFile());
            fb.setMimeType("application/pdf");
            return fb;
        } catch (Exception e) {
            throw new NuxeoException("Failed to encrypt the PDF", e);
        }
    }

    /**
     * Removes all protection from the PDF, returns a copy of it. If the PDF was not encrypted, just returns a copy of
     * it with no changes.
     * <p>
     * <b>IMPORTANT
     * </p>
     * If the PDF is encrypted, it is required for {@link PDFEncryption#setOriginalOwnerPwd} to be called before to
     * <code>removeEncryption</code>.
     * <ul>
     * <li>{@link PDFEncryption#setOriginalOwnerPwd}: Only if the original PDF already is encrypted. This password
     * allows to open it for modification.</li>
     * <li>{@link PDFEncryption#setKeyLength}: To set the length of the key.</li>
     * <li>{@link PDFEncryption#setOwnerPwd}: The password for the owner. If not called, <code>originalOwnerPwd</code>
     * is used instead.</li>
     * <li>{@link PDFEncryption#setUserPwd}: The password for the user.</li>
     * </ul>
     */
    public Blob removeEncryption() {
        try {
            String password = (StringUtils.isBlank(originalOwnerPwd)) ? ownerPwd : originalOwnerPwd;
            pdfDoc = PDDocument.load(pdfBlob.getFile(), password);
            if (!pdfDoc.isEncrypted()) {
                pdfDoc.close();
                return pdfBlob;
            }
            pdfDoc.setAllSecurityToBeRemoved(true);
            Blob result = Blobs.createBlobWithExtension(".pdf");
            pdfDoc.save(result.getFile());
            result.setMimeType("application/pdf");
            if (StringUtils.isNotBlank(pdfBlob.getFilename())) {
                result.setFilename(pdfBlob.getFilename());
            }
            pdfDoc.close();
            FileBlob fb = new FileBlob(result.getFile());
            fb.setMimeType("application/pdf");
            return fb;
        } catch (Exception e) {
            throw new NuxeoException("Failed to remove encryption of the PDF", e);
        }
    }

    /**
     * Set the lentgh of the key to be used for encryption.
     * <p>
     * Possible values are 40 and 128. Default value is 128 if <code>keyLength</code> is <= 0.
     *
     * @param keyLength Lenght of the encryption key.
     */
    public void setKeyLength(int keyLength) throws NuxeoException {
        if (keyLength < 1) {
            keyLength = DEFAULT_KEYLENGTH;
        } else {
            if (!ALLOWED_LENGTH.contains(keyLength)) {
                throw new NuxeoException("Cannot use " + keyLength + " is not allowed as lenght for the encrytion key");
            }
        }
        this.keyLength = keyLength;
    }

    /**
     * Set the password to use when opening a protected PDF. Must be called <i>before</i> encrypting the PDF.
     *
     * @param originalOwnerPwd Original owner password.
     */
    public void setOriginalOwnerPwd(String originalOwnerPwd) {
        this.originalOwnerPwd = originalOwnerPwd;
    }

    /**
     * Set the owner password to use when encrypting PDF. Must be called <i>before</i> encrypting the PDF.
     * <p>
     * Owners can do whatever they want to the PDF (modify, change protection, ...).
     *
     * @param ownerPwd Owner password.
     */
    public void setOwnerPwd(String ownerPwd) {
        this.ownerPwd = ownerPwd;
    }

    /**
     * Set the user password to use when encrypting PDF. Must be called <i>before</i> encrypting the PDF.
     * <p>
     * Users can have less rights than owners (for example, not being able to remove protection).
     *
     * @param userPwd User password.
     */
    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

}

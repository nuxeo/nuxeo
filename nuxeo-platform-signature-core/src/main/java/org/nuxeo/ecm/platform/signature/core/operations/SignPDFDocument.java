/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Mickael Vachette <mv@nuxeo.com>
 */
package org.nuxeo.ecm.platform.signature.core.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService.SigningDisposition;
import org.nuxeo.ecm.platform.signature.core.sign.SignatureHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;

@Operation(id = SignPDFDocument.ID, category = Constants.CAT_SERVICES, label = "Sign PDF", description = "Applies a digital signature to the"
        + " PDF blob of the input document.")
public class SignPDFDocument {

    public static final String ID = "Services.SignPDFDocument";

    private static final String MIME_TYPE_PDF = "application/pdf";

    @Context
    protected UserManager userManager;

    @Context
    protected SignatureService signatureService;

    @Param(name = "username", required = true, description = "The user ID for" + " signing PDF document.")
    protected String username;

    @Param(name = "password", required = true, description = "Certificate " + "password.")
    protected String password;

    @Param(name = "reason", required = true, description = "Signature reason.")
    protected String reason;

    @OperationMethod
    public Blob run(DocumentModel doc) {
        DocumentModel user = userManager.getUserModel(username);
        Blob originalBlob = doc.getAdapter(BlobHolder.class).getBlob();
        boolean originalIsPdf = MIME_TYPE_PDF.equals(originalBlob.getMimeType());
        // decide if we want PDF/A
        boolean pdfa = SignatureHelper.getPDFA();
        // decide disposition
        SigningDisposition disposition = SignatureHelper.getDisposition(originalIsPdf);
        // decide archive filename
        String filename = originalBlob.getFilename();
        String archiveFilename = SignatureHelper.getArchiveFilename(filename);
        return signatureService.signDocument(doc, user, password, reason, pdfa, disposition, archiveFilename);
    }
}

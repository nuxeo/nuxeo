package org.nuxeo.ecm.platform.signature.api.sign;

import org.nuxeo.ecm.core.api.DocumentModel;

import com.lowagie.text.pdf.PdfSignatureAppearance;

public interface SignatureAppearanceFactory {

    public void format(PdfSignatureAppearance pdfSignatureAppearance, DocumentModel doc, String principal, String reason);
}

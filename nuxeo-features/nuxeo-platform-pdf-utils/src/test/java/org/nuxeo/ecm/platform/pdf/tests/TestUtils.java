/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.pdf.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

class TestUtils {

    static final String PDF_PATH = "files/document.pdf";

    static final String PDF_ENCRYPTED_PATH = "files/document-encrypted.pdf";

    static final String PDF_ENCRYPTED_PASSWORD = "nuxeo";

    static final String PDF_PROTECTED_PATH = "files/document-protected.pdf";

    static final String PDF_PROTECTED_OWNER_PASSWORD = "owner";

    static final String PDF_PROTECTED_USER_PASSWORD = "user";

    static final String PDF_XMP_PATH = "files/xmp-embedded.pdf";

    static final String PDF_LINKED_1_PATH = "files/linked-pdf-1.pdf";

    static final String PDF_LINKED_2_PATH = "files/linked-pdf-2.pdf";

    static final String PDF_LINKED_3_PATH = "files/linked-pdf/linked-pdf-3.pdf";

    static final String PDF_MERGE_1 = "files/merge-1.pdf";

    static final String PDF_MERGE_2 = "files/merge-2.pdf";

    static final String PDF_MERGE_3 = "files/merge-3.pdf";

    static final String PDF_TRANSCRIPT_PATH = "files/transcript.pdf";

    static final String PDF_CONTRACT_PATH = "files/contract.pdf";

    static final String PDF_IMAGES_PATH = "files/pictures.pdf";

    static final String PDF_WATERMARK_PATH = "files/logo.pdf";

    static final String PNG_WATERMARK_PATH = "files/logo.png";

    static final int PNG_WATERMARK_WIDTH = 201;

    static final int PNG_WATERMARK_HEIGHT = 78;

    static final String JPG_WATERMARK_PATH = "files/logo.jpg";

    static final String JPG_PATH = "files/picture.jpg";

    static String extractText(PDDocument inDoc, int startPage, int endPage) throws IOException {
        endPage = endPage < startPage ? startPage : endPage;
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(startPage);
        stripper.setEndPage(endPage);
        return stripper.getText(inDoc);
    }

    static String calculateMd5(File inFile) throws IOException {
        FileInputStream fis = new FileInputStream(inFile);
        String md5 = DigestUtils.md5Hex(fis);
        fis.close();
        return md5;
    }

}

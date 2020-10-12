/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Imports the string content of a blob as text for the content of the "note" field of a new Note document.
 * <p>
 * If an existing document with the same title is found the existing Note document is updated instead.
 */
public class NoteImporter extends AbstractFileImporter {

    private static final Log log = LogFactory.getLog(NoteImporter.class);

    private static final String NOTE_TYPE = "Note";

    private static final String NOTE_SCHEMA = "note";

    private static final String NOTE_FIELD = "note";

    private static final String MT_FIELD = "mime_type";

    private static final long serialVersionUID = 1L;

    @Override
    public String getDefaultDocType() {
        return NOTE_TYPE;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return true;
    }

    @Override
    public boolean updateDocumentIfPossible(DocumentModel doc, Blob content) {
        if (!doc.hasSchema(NOTE_SCHEMA)) {
            log.warn("Schema '" + NOTE_SCHEMA + "' is not available for document " + doc);
            return false;
        }
        return super.updateDocumentIfPossible(doc, content);
    }

    @Override
    public void updateDocument(DocumentModel doc, Blob content) {
        String string;
        try {
            string = getString(content);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        doc.setProperty(NOTE_SCHEMA, NOTE_FIELD, string);
        doc.setProperty(NOTE_SCHEMA, MT_FIELD, content.getMimeType());
    }

    protected String getString(Blob blob) throws IOException {
        String s = guessEncoding(blob);
        if (s == null) {
            s = blob.getString(); // uses default charset
        }
        return s;
    }

    protected static String guessEncoding(Blob blob) throws IOException {
        // encoding already known?
        if (blob.getEncoding() != null) {
            return null;
        }

        // bad mime type?
        String mimeType = blob.getMimeType();
        if (mimeType == null) {
            return null;
        }
        if (!mimeType.startsWith("text/") && !mimeType.startsWith("application/xhtml")) {
            // not a text file, we shouldn't be in the Note importer
            return null;
        }

        byte[] bytes = blob.getByteArray();

        List<String> charsets = new ArrayList<>(Arrays.asList("utf-8", "iso-8859-1"));

        String CSEQ = "charset=";
        int i = mimeType.indexOf(CSEQ);
        if (i > 0) {
            // charset specified in MIME type
            String onlyMimeType = mimeType.substring(0, i).replace(";", "").trim();
            blob.setMimeType(onlyMimeType);
            String charset = mimeType.substring(i + CSEQ.length());
            i = charset.indexOf(";");
            if (i > 0) {
                charset = charset.substring(0, i);
            }
            charset = charset.trim().replace("\"", "");
            charsets.add(0, charset);
        } else {
            // charset detected from the actual bytes
            CharsetMatch charsetMatch = new CharsetDetector().setText(bytes).detect();
            if (charsetMatch != null) {
                String charset = charsetMatch.getName();
                charsets.add(0, charset);
            }
        }

        // now convert the string according to the charset, and fallback on others if not possible
        for (String charset : charsets) {
            try {
                Charset cs = Charset.forName(charset);
                CharsetDecoder d = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(
                        CodingErrorAction.REPORT);
                CharBuffer cb = d.decode(ByteBuffer.wrap(bytes));
                if (cb.length() != 0 && cb.charAt(0) == '\ufeff') {
                    // remove BOM
                    cb = cb.subSequence(1, cb.length());
                }
                return cb.toString();
            } catch (IllegalArgumentException e) {
                // illegal charset
            } catch (CharacterCodingException e) {
                // could not decode
            }
        }
        // nothing worked, use platform
        return null;
    }

}

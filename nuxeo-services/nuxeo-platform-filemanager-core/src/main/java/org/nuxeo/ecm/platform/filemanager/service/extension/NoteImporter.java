/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Imports the string content of a blob as text for the content of the "note"
 * field of a new Note document.
 * <p>
 * If an existing document with the same title is found the existing Note
 * document is updated instead.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class NoteImporter extends AbstractFileImporter {

    private static final String ICON_FIELD = "icon";
    private static final String COMMON_SCHEMA = "common";
    private static final String NOTE_FIELD = "note";
    private static final String MT_FIELD = "mime_type";
    private static final String NOTE_SCHEMA = NOTE_FIELD;
    private static final String TITLE_FIELD = "title";
    private static final String DUBLINCORE_SCHEMA = "dublincore";
    private static final String NOTE_TYPE = "Note";

    private static final long serialVersionUID = 1073550562485540108L;

    private static final Log log = LogFactory.getLog(NoteImporter.class);

    /**
     * Return the note document type. Can be override if the document to be
     * created is an extension of the Note type.
     */
    protected String getNoteTypeName() {
        return NOTE_TYPE;
    }

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String fullname,
            TypeManager typeService) throws ClientException, IOException {
        path = getNearestContainerPath(documentManager, path);
        doSecurityCheck(documentManager, path, getNoteTypeName(), typeService);

        String filename = FileManagerUtils.fetchFileName(fullname);

        String title = FileManagerUtils.fetchTitle(filename);

        // Looking if an existing Document with the same filename exists.
        DocumentModel docModel = FileManagerUtils.getExistingDocByTitle(
                documentManager, path, title);

        if (overwrite && docModel != null) {

            docModel.setProperty(NOTE_SCHEMA, NOTE_FIELD, getString(content));
            docModel.setProperty(NOTE_SCHEMA, MT_FIELD, content.getMimeType());
            docModel = overwriteAndIncrementversion(documentManager, docModel);
        } else {
            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            // Create a new empty DocumentModel of type Note in memory
            docModel = documentManager.createDocumentModel(getNoteTypeName());

            // Update known attributes (title, note)
            docModel.setProperty(DUBLINCORE_SCHEMA, TITLE_FIELD, title);
            docModel.setProperty(NOTE_SCHEMA, NOTE_FIELD, getString(content));
            docModel.setProperty(NOTE_SCHEMA, MT_FIELD, content.getMimeType());

            // Create the new document in the repository
            docModel.setPathInfo(path, pss.generatePathSegment(docModel));
            docModel = documentManager.createDocument(docModel);
        }
        documentManager.save();

        log.debug("Created the Note: " + docModel.getName() + " with icon: "
                + docModel.getProperty(COMMON_SCHEMA, ICON_FIELD));
        return docModel;
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
        if (!mimeType.startsWith("text/")
                && !mimeType.startsWith("application/xhtml")) {
            // not a text file, we shouldn't be in the Note importer
            return null;
        }

        byte[] bytes = blob.getByteArray();

        List<String> charsets = Arrays.asList("utf-8", "iso-8859-1");

        // charset specified in MIME type?
        String CSEQ = "charset=";
        int i = mimeType.indexOf(CSEQ);
        if (i > 0) {
            String onlyMimeType = mimeType.substring(0, i).replace(";", "").trim();
            blob.setMimeType(onlyMimeType);
            String charset = mimeType.substring(i + CSEQ.length());
            i = charset.indexOf(";");
            if (i > 0) {
                charset = charset.substring(0, i);
            }
            charset = charset.trim().replace("\"", "");
            charsets = new ArrayList<String>(charsets);
            charsets.add(0, charset);
        }

        // resort to auto-detection
        for (String charset : charsets) {
            try {
                Charset cs = Charset.forName(charset);
                CharsetDecoder d = cs.newDecoder().onMalformedInput(
                        CodingErrorAction.REPORT).onUnmappableCharacter(
                        CodingErrorAction.REPORT);
                CharBuffer cb = d.decode(ByteBuffer.wrap(bytes));
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

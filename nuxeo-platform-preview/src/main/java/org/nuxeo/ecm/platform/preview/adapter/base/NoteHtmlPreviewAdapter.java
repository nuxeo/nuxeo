/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NoteHtmlPreviewAdapter extends PreprocessedHtmlPreviewAdapter {

    public NoteHtmlPreviewAdapter(List<String> fieldsPaths) {
        super(fieldsPaths);
    }

    @Override
    public List<Blob> getPreviewBlobs() throws PreviewException {
        List<Blob> blobs = super.getPreviewBlobs();
        if (!blobs.isEmpty()) {
            Blob blob = blobs.remove(0);
            Blob newBlob = processNoteBlob(blob);
            blobs.add(0, newBlob);
        }
        return blobs;
    }

    protected Blob processNoteBlob(Blob blob) throws PreviewException {
        try {
            String note = blob.getString();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body>");
            sb.append(note);
            sb.append("</body></html>");

            byte[] bytes = blob.getEncoding() == null ? sb.toString().getBytes() : sb.toString().getBytes(blob.getEncoding());
            String mimeType = blob.getMimeType();
            if (mimeType == null) {
                mimeType = "text/html";
            }
            Blob newBlob = new ByteArrayBlob(bytes, mimeType, blob.getEncoding());
            return newBlob;
        } catch(IOException e) {
            throw new PreviewException(e);
        } catch (UnsupportedCharsetException e) {
            throw new PreviewException(e);
        }
    }

    @Override
    public List<Blob> getPreviewBlobs(String xpath) throws PreviewException {
        return getPreviewBlobs();
    }

}

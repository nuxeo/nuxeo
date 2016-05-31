/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
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

            String mimeType = blob.getMimeType();
            if (mimeType == null) {
                mimeType = "text/html";
            }
            return Blobs.createBlob(sb.toString(), mimeType, blob.getEncoding());
        } catch (IOException e) {
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

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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.diff.content.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.differs.diff_match_patch;
import org.nuxeo.ecm.diff.content.differs.diff_match_patch.Diff;

public class PlainTextContentDiffer implements MimeTypeContentDiffer {

    public List<Blob> getContentDiff(Blob leftBlob, Blob rightBlob,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ContentDiffException {

        // TODO: test XML entities (&, ', "", ...)

        List<Blob> blobResults = new ArrayList<Blob>();

        diff_match_patch dmp = new diff_match_patch();

        LinkedList<Diff> diffs;
        try {
            diffs = dmp.diff_main(leftBlob.getString(), rightBlob.getString());
        } catch (IOException ioe) {
            throw new ContentDiffException(
                    "Error while processing plain text diff.", ioe);
        }
        dmp.diff_cleanupSemantic(diffs);
        String prettyHtmlDiff = dmp.diff_prettyHtml(diffs);

        Blob mainBlob = new StringBlob(prettyHtmlDiff.toString());
        mainBlob.setFilename("contentDiff.txt");
        mainBlob.setMimeType("text/plain");

        blobResults.add(mainBlob);
        return blobResults;
    }

}

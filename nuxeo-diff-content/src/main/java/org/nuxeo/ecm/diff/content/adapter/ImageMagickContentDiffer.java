/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.ecm.diff.content.adapter;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * ImageMagickContentDiffer
 * <p>
 *
 * @since 7.4
 */
public class ImageMagickContentDiffer implements MimeTypeContentDiffer {

	public MimeTypeContentDiffer.TYPE_OF_PARAMETERS getTypeOfParameters() {
		return TYPE_OF_PARAMETERS.DOCUMENT;
	}

	@Override
	public List<Blob> getContentDiff(DocumentModel leftDoc,
			DocumentModel rightDoc, String xpath, Locale locale)
			throws ContentDiffException {

		try {
			String leftDocId = leftDoc.getId();
			String rightDocId = rightDoc.getId();
			String url = VirtualHostHelper.getContextPathProperty();

			url += "/diffPictures?action=diff&leftDocId=" + leftDocId
					+ "&rightDocId=" + rightDocId + "&xpath=" + xpath;

			List<Blob> blobResults = new ArrayList<Blob>();
			StringWriter sw = new StringWriter();

			sw.write("<html>");
			sw.write("<body>");
			sw.write("<div><img src='" + url + "'></img></div>");
			sw.write("</body>");
			sw.write("</html>");

			String stringBlob = sw.toString();
			Blob mainBlob = Blobs.createBlob(stringBlob);
			sw.close();

			mainBlob.setFilename("contentDiff.html");
			mainBlob.setMimeType("text/html");

			blobResults.add(mainBlob);
			return blobResults;
		} catch (Exception e) {
			throw new ContentDiffException(e);
		}
	}

	@Override
	public List<Blob> getContentDiff(Blob leftBlob, Blob rightBlob,
			Locale locale) throws ContentDiffException {

		throw new ContentDiffException(
				"ImageMagickContentDiffer can handle only DocumentModel");
	}
}

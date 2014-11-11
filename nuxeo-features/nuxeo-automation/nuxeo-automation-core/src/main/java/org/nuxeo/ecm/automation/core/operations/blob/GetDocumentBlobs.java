/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;

/**
 * Save the input document
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = GetDocumentBlobs.ID, category = Constants.CAT_BLOB, label = "Get Document Files", description = "Get a list of files that are attached on the input document. The files location should eb specified using the blob list property xpath. Return a list of files.")
public class GetDocumentBlobs {

    public final static String ID = "Blob.GetList";

    @Param(name = "xpath", required = false, values = "files:files")
    protected String xpath = "files:files";

    @OperationMethod
    public BlobList run(DocumentModel doc) throws Exception {
        BlobList blobs = new BlobList();
        ListProperty list = (ListProperty) doc.getProperty(xpath);
        if (list == null) {
            return blobs;
        }
        for (Property p : list) {
            blobs.add((Blob) p.getValue("file"));
        }
        return blobs;
    }

    @OperationMethod
    public BlobList run(DocumentModelList docs) throws Exception {
        BlobList blobs = new BlobList();
        for (DocumentModel doc : docs) {
            try {
                blobs.addAll(run(doc));
            } catch (PropertyException e) {
                // continue -> ignore docs with no blobs
            }
        }
        return blobs;
    }

}

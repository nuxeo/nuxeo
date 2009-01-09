/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;

/**
 * Fake transformer that assumes a source text blob and therefore just copies it
 * to the output blob.
 *
 * @author Florent Guillaume
 */
public class FakeTransformPlugin extends AbstractPlugin {

    private static final long serialVersionUID = 1L;

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        Blob blob;
        String mimeType;
        if (sources.length == 0) {
            blob = new StringBlob("");
            mimeType = "text/plain";
        } else {
            blob = new InputStreamBlob(sources[0].getBlob().getStream());
            mimeType = sources[0].getMimetype();
        }
        TransformDocument result = new TransformDocumentImpl(blob, mimeType);
        return Collections.singletonList(result);
    }
}

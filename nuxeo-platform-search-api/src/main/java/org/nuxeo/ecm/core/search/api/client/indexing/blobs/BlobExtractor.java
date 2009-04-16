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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.indexing.blobs;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;

/**
 * Blob full text extractor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface BlobExtractor extends Serializable {

    /**
     * Extracts fulltext from a streaming blob.
     *
     * @param blob : a nxcore blob instance
     * @param mimetype blob mimetype
     * @return a string holding the the extracted fulltext for a given blob.
     * @throws Exception
     */
    String extract(Blob blob, String mimetype,
            FulltextFieldDescriptor desc) throws Exception;

}

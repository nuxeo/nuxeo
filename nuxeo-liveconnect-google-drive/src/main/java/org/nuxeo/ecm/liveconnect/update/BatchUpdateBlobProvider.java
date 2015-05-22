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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.liveconnect.update;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 7.3
 */
public interface BatchUpdateBlobProvider {

    static final long MAX_RESULT = 50;

    /**
     * @param documents to be checked for update
     * @return the list of DocumentModel that have changed
     * @throws IOException
     */
    List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> doc);

    void processDocumentsUpdate();

}

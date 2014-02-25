/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.automation;

import net.sf.json.JSONArray;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * @since 5.9.3
 */
@Operation(id = SuggestCollectionEntry.ID, category = Constants.CAT_SERVICES, label = "Get collection suggestion", description = "Get the collection list accessible by the current user. This is returning a blob containing a serialized JSON array..", addToStudio = false)
public class SuggestCollectionEntry {

    public static final String ID = "Collection.Suggestion";

    @OperationMethod
    public Blob run() throws ClientException {
        JSONArray result = new JSONArray();
        // TODO
        return new StringBlob(result.toString(), "application/json");
    }
}

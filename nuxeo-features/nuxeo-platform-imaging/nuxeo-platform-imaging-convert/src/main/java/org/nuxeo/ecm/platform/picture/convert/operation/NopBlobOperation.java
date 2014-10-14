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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.convert.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.BlobWrapper;

/**
 * Operation which simply return the passed blob
 *
 * @since 5.9.6
 *
 * @author Vincent Vergnolle
 */
@Operation(category = Constants.CAT_BLOB, id = NopBlobOperation.ID, label = NopBlobOperation.ID, description = "Operation which simply return a wrapper from the passed blob", since = "5.9.6")
public class NopBlobOperation {

    public static final String ID = "Blob.Nop";

    @OperationMethod
    public Blob exec(Blob blob) {
        return new BlobWrapper(blob);
    }
}

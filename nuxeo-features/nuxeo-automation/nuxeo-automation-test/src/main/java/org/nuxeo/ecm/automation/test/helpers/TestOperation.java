/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test.helpers;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Basic operation that just record the documents on which it was called and
 * the parameters used
 *
 * @since 5.7.2
 */
@Operation(id = TestOperation.ID, category = Constants.CAT_SERVICES, label = "log call params", description = "Test operation that log call in a static map")
public class TestOperation {

    public static final String ID = "testOp";

    @Param(name = "one")
    String one;

    @Param(name = "two")
    Integer two;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        return docs;
    }

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        return blob;
    }

}

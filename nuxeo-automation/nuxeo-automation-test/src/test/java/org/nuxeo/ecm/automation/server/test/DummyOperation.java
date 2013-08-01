/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import java.util.List;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.7.3
 */
@Operation(id = DummyOperation.ID)
public class DummyOperation {

    public static final String ID = "Dummy";

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        return doc;
    }

    @OperationMethod
    public String runWithString(String data) {
        return data;
    }

    @OperationMethod
    public List<String> runWithList(List<String> data) {
        return data;
    }
}

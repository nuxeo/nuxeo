/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

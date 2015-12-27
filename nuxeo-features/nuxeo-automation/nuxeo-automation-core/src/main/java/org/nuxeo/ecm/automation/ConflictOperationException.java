/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Marker Exception to identify Dirty update detected by using the ChangeToken of the DocumentModel
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class ConflictOperationException extends OperationException {

    private static final long serialVersionUID = 1L;

    public ConflictOperationException(DocumentModel doc) {
        super("Conflict detected while trying to update document " + doc.getId());
    }
}

/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.io.impl;

import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Alternative to the default {@link DocumentPipe} that handles Transactions demarcation aligned with the Pipe batch
 * size
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TransactionBatchingDocumentPipeImpl extends DocumentPipeImpl {

    public TransactionBatchingDocumentPipeImpl(int pageSize) {
        super(pageSize);
    }

    @Override
    protected void handleBatchEnd() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }
}

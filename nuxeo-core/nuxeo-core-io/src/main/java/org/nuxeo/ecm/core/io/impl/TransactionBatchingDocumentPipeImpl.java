/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.io.impl;

import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 *
 * Alternative to the default {@link DocumentPipe} that handles Transactions
 * demarcation aligned with the Pipe batch size
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
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


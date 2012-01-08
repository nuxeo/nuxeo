/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class UpdaterTask implements Runnable {

    private static final Log log = LogFactory.getLog(UpdaterTask.class);

    private final QuotaStatsUpdater updater;

    private final DocumentEventContext docCtx;

    private final String eventName;

    public UpdaterTask(QuotaStatsUpdater updater, DocumentEventContext docCtx, String eventName) {
        this.updater = updater;
        this.docCtx = docCtx;
        this.eventName = eventName;
    }

    @Override
    public void run() {
        TransactionHelper.startTransaction();
        try {
            new UnrestrictedSessionRunner(docCtx.getRepositoryName()) {
                @Override
                public void run() throws ClientException {
                    updater.updateStatistics(session, docCtx, eventName);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            TransactionHelper.setTransactionRollbackOnly();
            log.error(e, e);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

}

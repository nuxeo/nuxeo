/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.importer.core.helper;

import org.nuxeo.dam.exception.DamRuntimeException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Helper class to launch an {@code UnrestrictedSessionRunner} in a separate
 * thread
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class UnrestrictedSessionRunnerHelper {

    private UnrestrictedSessionRunnerHelper() {
        // Helper class
    }

    /**
     * Launch the given {@code UnrestrictedSessionRunner} in a separate thread
     * and wait for it to finish.
     *
     * @param runner the {@code UnrestrictedSessionRunner} to launch
     * @throws DamRuntimeException If the thread is interrupted or if the
     *             {@code runner} throws a {@code ClientException}.
     */
    public static void runInNewThread(final UnrestrictedSessionRunner runner) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    runner.runUnrestricted();
                } catch (ClientException e) {
                    TransactionHelper.setTransactionRollbackOnly();
                    throw new DamRuntimeException(e);
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new DamRuntimeException(e);
        }
    }

}

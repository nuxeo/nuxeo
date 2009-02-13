/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.listener;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;


/**
 * This listener is notified at each save operation by passing all events collected between 2 save operations in the same thread.
 * Note that this is also working when no transactions are started but in that case you may have weird
 * results because if you forgot to send save events the events will remain collected in a thread local variable and when the
 * thread is reused you will consume these pending events!
 * <p>
 * When in a transaction context the transaction commit or rollback will flush the event stack so when a new transaction
 * is started the new events will always be collected in a clean stack.
 *  <p>
 *  Note that the save event that triggered the notification is not part of the events passed to that listener
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface OnSaveListener extends TransactedListener {

    /**
     * Notify Last save in current transaction.
     * <p>
     * This method should be used by listeners using the CoreEvent model
     * and ignored by the one using Operation events.
     *
     * @param events all core events collected in current transaction.
     */
    void onSave(CoreEvent[] events);

    /**
     * Notify Last save in current transaction.
     * <p>
     * This method should be used by listeners using the Operation events
     * and ignored by the one using CoreEvent events.
     *
     * @param events all operation events collected in current transaction
     */
    void onSave(Operation<?>[] events);

}

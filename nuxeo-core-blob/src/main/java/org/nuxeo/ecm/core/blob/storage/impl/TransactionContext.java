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

package org.nuxeo.ecm.core.blob.storage.impl;

import javax.resource.ResourceException;
import javax.transaction.xa.Xid;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TransactionContext extends TransactedSession {

    protected boolean isSuspended;
    protected Xid xid;

    public TransactionContext(DefaultBlobStorage storage, Xid xid) {
        super (storage);
        this.xid = xid;
    }

    public void setSuspended(boolean isSuspended) {
        this.isSuspended = isSuspended;
    }

    /**
     * @return the isSuspended.
     */
    public boolean isSuspended() {
        return isSuspended;
    }

    /**
     * @return the xid.
     */
    public Xid getXid() {
        return xid;
    }

    public void prepare() throws ResourceException {
        // do nothing
    }
}

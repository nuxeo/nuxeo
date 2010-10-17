/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav.provider;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;

public class TransactionAwareBlob {

    protected final Blob blob;

    protected final CoreSession session;

    protected boolean releaseSession = true;

    public TransactionAwareBlob(CoreSession session, Blob blob) {
        this.blob = blob;
        this.session = session;
    }

    public TransactionAwareBlob(CoreSession session, Blob blob, boolean releaseSession) {
        this.blob = blob;
        this.session = session;
        this.releaseSession = releaseSession;
    }

    public Blob getBlob() {
        return blob;
    }

    public void commitOrRollback() {
        if (session != null && releaseSession) {
            CoreInstance.getInstance().close(session);
        }
        Util.endTransaction();
    }

    public long getLength() {
        if (blob != null) {
            return blob.getLength();
        }
        return 0L;
    }

}

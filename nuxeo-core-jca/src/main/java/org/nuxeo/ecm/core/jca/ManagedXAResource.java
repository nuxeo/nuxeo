/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.core.jca;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object maintains a dynamic xa resource so that
 * managed connections can be reused after closing sessions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ManagedXAResource implements XAResource {

    private static final Log log = LogFactory.getLog(ManagedXAResource.class);

    private XAResource xar;


    public ManagedXAResource() {

    }

    public ManagedXAResource(XAResource xar) {
        this.xar = xar;
    }

    public void setXAResource(XAResource xar) {
        this.xar = xar;
    }

    public XAResource getXAResource() {
        return xar;
    }

    public void commit(Xid arg0, boolean arg1) throws XAException {
        log.debug("XA DEBUG >>>>>>>>>>> commiting tx ...." + xar);
        checkState();
        xar.commit(arg0, arg1);
    }

    public void end(Xid arg0, int arg1) throws XAException {
        log.debug("XA DEBUG >>>>>>>>>>> ending tx ...." + xar);
        checkState();
        xar.end(arg0, arg1);
    }

    public void forget(Xid arg0) throws XAException {
        checkState();
        xar.forget(arg0);
    }

    public int getTransactionTimeout() throws XAException {
        checkState();
        return xar.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource arg0) throws XAException {
        checkState();
        return xar.isSameRM(arg0);
    }

    public int prepare(Xid arg0) throws XAException {
        log.debug("XA DEBUG >>>>>>>>>>> preparing tx ...." + xar);
        checkState();
        return xar.prepare(arg0);
    }

    public Xid[] recover(int arg0) throws XAException {
        checkState();
        return xar.recover(arg0);
    }

    public void rollback(Xid arg0) throws XAException {
        log.debug("XA DEBUG >>>>>>>>>>> rollback tx ...." + xar);
        checkState();
        xar.rollback(arg0);
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        checkState();
        return xar.setTransactionTimeout(arg0);
    }

    public void start(Xid arg0, int arg1) throws XAException {
        log.debug("XA DEBUG >>>>>>>>>>> starting tx ...." + xar);
        checkState();
        xar.start(arg0, arg1);
    }

    private void checkState() throws XAException {
        if (xar == null) {
            throw new XAException("the session was not initialized");
        }
    }

}

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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.Serializable;
import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 * javax.transaction.xa.Xid may have several implementations and comming from
 * several libraries. When the xid implementation on a client side is not
 * available on server side, it is causing class not found errors.
 * 
 * <br />
 * This is a xid implementation known by both client and server.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class MapperClientXid implements Xid, Serializable {

    byte[] branchQualifier;

    int formatId;

    byte[] globalTransactionId;

    private static final long serialVersionUID = 1L;

    public MapperClientXid(byte[] branchQualifier, int formatId,
            byte[] globalTransactionId) {
        super();
        this.branchQualifier = branchQualifier;
        this.formatId = formatId;
        this.globalTransactionId = globalTransactionId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.xa.Xid#getBranchQualifier()
     */
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.xa.Xid#getFormatId()
     */
    public int getFormatId() {
        return formatId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.xa.Xid#getGlobalTransactionId()
     */
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Xid)) {
            return super.equals(obj);
        }
        Xid xidObject = (Xid) obj;
        return Arrays.equals(globalTransactionId,
                xidObject.getGlobalTransactionId());
    }

}

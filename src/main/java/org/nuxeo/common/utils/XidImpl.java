/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *     Sun Seng David TAN
 */

package org.nuxeo.common.utils;

import java.io.Serializable;
import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 * A Serializable {@link Xid} independent of the transaction manager
 * implementation.
 */
public class XidImpl implements Xid, Serializable {

    private static final long serialVersionUID = 1L;

    public final int fid;

    public final byte[] gtrid;

    public final byte[] bqual;

    /**
     * Copy constructor.
     *
     * @param xid the xid to copy
     */
    public XidImpl(Xid xid) {
        fid = xid.getFormatId();
        gtrid = xid.getGlobalTransactionId().clone();
        bqual = xid.getBranchQualifier().clone();
    }

    /**
     * Constructor mostly used in unit tests.
     *
     * @param id the global transaction id
     */
    public XidImpl(String id) {
        fid = 0;
        gtrid = id.getBytes();
        // MySQL JDBC driver needs a non 0-length branch qualifier
        bqual = new byte[] { 0 };
    }

    public int getFormatId() {
        return fid;
    }

    public byte[] getGlobalTransactionId() {
        return gtrid.clone();
    }

    public byte[] getBranchQualifier() {
        return bqual.clone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = 1;
        hash = prime * hash + fid;
        hash = prime * hash + Arrays.hashCode(gtrid);
        hash = prime * hash + Arrays.hashCode(bqual);
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof XidImpl)) {
            return false;
        }
        return equals((XidImpl) other);
    }

    private boolean equals(XidImpl other) {
        if (other == this) {
            return true;
        }
        return fid == other.fid && Arrays.equals(gtrid, other.gtrid)
                && Arrays.equals(bqual, other.bqual);
    }

}

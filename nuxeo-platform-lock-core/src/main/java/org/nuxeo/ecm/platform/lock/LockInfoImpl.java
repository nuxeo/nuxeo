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
package org.nuxeo.ecm.platform.lock;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import org.nuxeo.ecm.platform.lock.api.LockInfo;

/**
 * Default implementation of lock info. This embed a LockRecord and delegates
 * it.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class LockInfoImpl implements LockInfo, Serializable{


    private static final long serialVersionUID = 1L;

    protected LockInfoImpl(LockRecord record) {
        this.resource = record.resource;
        this.expireTime = record.expireTime;
        this.lockTime = record.lockTime;
        this.owner = record.owner;
        this.isExpired = false;
    }


    protected final URI resource;

    public URI getResource() {
        return resource;
    }

    protected final Date expireTime;

    public Date getExpiredTime() {
        return expireTime;
    }

    protected final Date lockTime;

    public Date getLockTime() {
        return lockTime;
    }

    protected final URI owner;

    public URI getOwner() {
        return owner;
    }

    protected final boolean isExpired;

    public boolean isExpired() {
        return isExpired;
    }

}

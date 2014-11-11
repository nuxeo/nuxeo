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

import java.net.URI;
import java.util.Date;

import org.nuxeo.ecm.platform.lock.api.LockInfo;

/**
 * Default implementation of lock info. This embed a LockRecord and delegates
 * it.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class LockInfoImpl implements LockInfo {

    LockRecord record;

    public LockInfoImpl(LockRecord record) {
        this.record = record;
    }

    public URI getResource() {
        return record.resource;
    }

    public Date getExpireTime() {
        return record.expireTime;
    }

    public Date getLockTime() {
        return record.lockTime;
    }

    public URI getRunning() {
        return record.owner;
    }

    public boolean isExpired() {
        return false;
    }

}

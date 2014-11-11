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
 *     matic
 */
package org.nuxeo.ecm.platform.lock;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.lock.api.LockInfo;
import org.nuxeo.ecm.platform.lock.api.LockReader;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;

/**
 * @author matic
 *
 */
public class LockReaderImpl implements LockReader, LockComponentDelegate {

    protected LockRecordProvider provider;

    /**
     * Takes a reference onto provider
     *
     * @param lockComponent
     */
    public void activate(LockComponent lockComponent) {
        this.provider = lockComponent.provider;
    }

    /**
     * Releases reference onto provider
     */
    public void deactivate() {
        this.provider = null;
    }

    @Override
    public List<LockInfo> getInfos() throws InterruptedException {
        List<LockRecord> records = provider.getRecords();
        List<LockInfo> infos = new ArrayList<LockInfo>(records.size());
        for (LockRecord record : records) {
            infos.add(new LockInfoImpl(record));
        }
        return infos;
    }

    @Override
    public LockInfo getInfo(final URI resource) throws NoSuchLockException, InterruptedException {
        LockRecord record = provider.getRecord(resource);
        return new LockInfoImpl(record);

    }
}

/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.storage.lock.LockManager;
import org.nuxeo.ecm.core.storage.lock.LockManagerService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class, CacheFeature.class, RedisFeature.class})
public class TestRedisLockManager {

    protected static void assertTimeEquals(Calendar expected, Lock lock) {
        assertEquals(expected.getTimeInMillis(),
                lock.getCreated().getTimeInMillis());
    }

    @Test
    public void testRedisLockManager() throws Exception {
        LockManager lockManager = Framework.getLocalService(
                LockManagerService.class).getLockManager("default");
        String id = "1234";

        // no lock initially present
        assertNull(lockManager.getLock(id));

        // set a lock
        Calendar created = Calendar.getInstance();
        Lock lock = new Lock("bob", created);
        assertNull(lockManager.setLock(id, lock));

        // lock is set
        Lock lock1 = lockManager.getLock(id);
        assertNotNull(lock1);
        assertFalse(lock1.getFailed());
        assertEquals("bob", lock1.getOwner());
        assertTimeEquals(created, lock1);

        // cannot set another lock
        Lock lock2 = new Lock("pete", created);
        Lock lock3 = lockManager.setLock(id, lock2);
        assertNotNull(lock3);
        assertEquals("bob", lock3.getOwner());
        assertTimeEquals(created, lock3);

        // cannot remove lock with another user
        Lock lock4 = lockManager.removeLock(id, "pete");
        assertNotNull(lock4);
        assertTrue(lock4.getFailed());
        assertEquals("bob", lock4.getOwner());
        assertTimeEquals(created, lock4);

        // remove lock as correct user
        Lock lock5 = lockManager.removeLock(id, "bob");
        assertNotNull(lock5);
        assertFalse(lock5.getFailed());
        assertEquals("bob", lock5.getOwner());
        assertTimeEquals(created, lock5);
        assertNull(lockManager.getLock(id));

        // recreate lock
        assertNull(lockManager.setLock(id, lock));

        // remove lock unconditionally
        Lock lock6 = lockManager.removeLock(id, null);
        assertNotNull(lock6);
        assertFalse(lock6.getFailed());
        assertEquals("bob", lock6.getOwner());
        assertTimeEquals(created, lock6);
        assertNull(lockManager.getLock(id));

        // can't remove non-existent lock
        Lock lock7 = lockManager.removeLock(id, null);
        assertNull(lock7);
    }

}

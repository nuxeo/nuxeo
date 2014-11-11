/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.lock;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.lock.api.AlreadyLockedException;
import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;
import org.nuxeo.ecm.platform.lock.api.NotOwnerException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class TestLockCoordinator extends NXRuntimeTestCase {

    public static final Log log = LogFactory.getLog(TestLockCoordinator.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.lock.api");
        deployBundle("org.nuxeo.ecm.platform.lock.core");
        deployTestContrib("org.nuxeo.ecm.platform.lock.core",
                "nxlocks-tests.xml");
        super.fireFrameworkStarted();
    }

    // TODO if we reduced the delay to less that
    // this value, algorithm is failing
    final static long DELAY = 800;

    /**
     * Basic lock scenario: there is 2 competitors that want to get a lock.
     * winner is getting the lock for a certain time and need to unlock before,
     * looser is waiting (until the timeout is reached), winner unlocks, looser
     * finally has lost (AlreadyLockedException)
     * 
     * @throws Exception
     */
    public void testLockService() throws Exception {

        URI winner = new URI("nxlockcompetitor://winner@server");
        URI looser = new URI("nxlockcompetitor://looser@server");
        URI resourceUri = new URI("nxlockresource://server/resource");

        Locker winnerLocker = new Locker(winner, resourceUri, "winner lock",
                500);
        Locker looserLocker = new Locker(looser, resourceUri, "looser lock",
                500);

        winnerLocker.start();
        Thread.sleep(10);
        looserLocker.start();

        winnerLocker.thread.join();
        looserLocker.thread.join();

        assertNull("winner should has locked the resource before looser",
                winnerLocker.alreadyLocked);
        assertNull("winner should has unlocked the resource successfully",
                winnerLocker.noSuchLockException);
        assertNull("winner should has unlocked an existing resource",
                winnerLocker.notOwnerException);

        assertNotNull(
                "looser should has been waiting for the lock and has his lock refused",
                looserLocker.alreadyLocked);

    }

    class Locker implements Runnable {

        AlreadyLockedException alreadyLocked;

        NoSuchLockException noSuchLockException;

        NotOwnerException notOwnerException;

        Object lockinfo;

        URI resource;

        long executionTime;

        URI selfcompetitor;

        String comment;

        Locker(URI competitor, URI resource, String comment, long executionTime) {
            this.resource = resource;
            this.executionTime = executionTime;
            this.comment = comment;
            selfcompetitor = competitor;

        }

        public void run() {

            LockCoordinator coordinator = Framework.getLocalService(LockCoordinator.class);

            assertNotNull("The lock coordinator should be created", coordinator);

            try {
                coordinator.lock(selfcompetitor, resource, comment, DELAY);
            } catch (InterruptedException e) {
                log.error("A unintended InterruptedException has been raised",
                        e);
            } catch (AlreadyLockedException e) {
                alreadyLocked = e;
                return;
            }
            try {
                Thread.sleep(executionTime);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
            try {
                coordinator.unlock(selfcompetitor, resource);
            } catch (NoSuchLockException e) {
                noSuchLockException = e;
            } catch (NotOwnerException e) {
                notOwnerException = e;
            } catch (Throwable e) {
                throw new Error("Unexpected error", e);
            }
        }

        Thread thread;

        /**
         * Start the thread with this runner
         */
        public void start() {
            thread = new Thread(this);
            thread.start();
        }

    }

}

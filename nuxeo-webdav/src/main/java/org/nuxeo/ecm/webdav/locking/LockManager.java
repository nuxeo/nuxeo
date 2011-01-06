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

package org.nuxeo.ecm.webdav.locking;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simple singleton class to manage locks.
 * <p>
 * It remains to be decided whether the WebDAV lock management systems
 * should better stay independent, or be integrated with the Nuxeo lock
 * management system.
 */
public class LockManager {

    private static LockManager instance;

    private final Map<String, LockInfo> lockedResources = new HashMap<String, LockInfo>();

    private final Random random = new Random();

    private LockManager() {
    }

    public static LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    /**
     * Locks resource at path.
     *
     * @return the lock token
     */
    public String lock(String path) {
        LockInfo lockInfo = new LockInfo();
        lockedResources.put(normalize(path), lockInfo);
        return lockInfo.token;
    }

    public void unlock(String path) {
        lockedResources.remove(normalize(path));
    }

    public boolean canUnlock(String path, String token) {
        if (token == null) {
            return false;
        }
        path = normalize(path);
        if (isLocked(path)) {
            LockInfo lockInfo = lockedResources.get(path);
            return token.equals(lockInfo.token);
        }
        return false;
    }

    public boolean isLocked(String path) {
        return lockedResources.containsKey(normalize(path));
    }

    private String normalize(String path) {
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        return path;
    }

    class LockInfo {
        final String token;
        String userName;
        Date lockDate;

        LockInfo() {
            token = Long.toHexString(System.currentTimeMillis()) + "-" + random.nextInt();
        }
    }

}

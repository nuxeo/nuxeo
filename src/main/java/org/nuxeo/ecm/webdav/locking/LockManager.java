package org.nuxeo.ecm.webdav.locking;

import java.util.*;

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
            token = Long.toHexString(System.currentTimeMillis());
        }
    }

}

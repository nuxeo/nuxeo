package org.nuxeo.ecm.webdav;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple singleton class to manage locks.
 * <p>
 * It remains to be decided whether the WebDAV lock management systems
 * should better stay independent, or be integrated with the Nuxeo lock
 * management system.
 */
public class LockManager {

    private static LockManager instance;

    private final Set<String> lockedResources = new HashSet<String>();

    private LockManager() {

    }

    public static LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    public void lock(String path) {
        lockedResources.add(normalize(path));
    }


    public void unlock(String path) {
        lockedResources.remove(normalize(path));
    }

    public boolean isLocked(String path) {
        return lockedResources.contains(normalize(path));
    }
    
    private String normalize(String path) {
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        return path;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Locked resources:\n");
        for (String resource : lockedResources) {
            sb.append(resource).append("\n");
        }
        return sb.toString();
    }
}

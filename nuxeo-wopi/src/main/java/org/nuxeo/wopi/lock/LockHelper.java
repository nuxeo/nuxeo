/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.wopi.lock;

import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_DOC_ID;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_FILE_ID;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_LOCK;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_NAME;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_REPOSITORY;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_SCHEMA_NAME;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_TIMESTAMP;
import static org.nuxeo.wopi.Constants.LOCK_TTL;
import static org.nuxeo.wopi.Constants.WOPI_USER;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wopi.Constants;
import org.nuxeo.wopi.FileInfo;

/**
 * @since 10.3
 */
public class LockHelper {

    private static final Logger log = LogManager.getLogger(LockHelper.class);

    private LockHelper() {
        // helper class
    }

    /**
     * Stores the given WOPI lock for the given file id with a timestamp for expiration purpose.
     */
    public static void addLock(String fileId, String lock) {
        FileInfo fileInfo = new FileInfo(fileId);
        addLock(fileId, fileInfo.repositoryName, fileInfo.docId, lock);
    }

    /**
     * @see #addLock(String, String)
     */
    public static void addLock(String fileId, String repository, String docId, String lock) {
        log.debug("Locking: fileId={} Adding lock {}", fileId, lock);
        doPrivilegedOnLockDirectory(session -> {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put(LOCK_DIRECTORY_FILE_ID, fileId);
            entryMap.put(LOCK_DIRECTORY_REPOSITORY, repository);
            entryMap.put(LOCK_DIRECTORY_DOC_ID, docId);
            entryMap.put(LOCK_DIRECTORY_LOCK, lock);
            entryMap.put(LOCK_DIRECTORY_TIMESTAMP, System.currentTimeMillis());
            session.createEntry(entryMap);
        });
    }

    /**
     * Gets the WOPI lock stored for the given file id if it exists, returns {@code null} otherwise.
     */
    public static String getLock(String fileId) {
        return doPrivilegedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(fileId);
            return entry == null ? null : (String) entry.getProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_LOCK);
        });
    }

    /**
     * Checks if a WOPI lock is stored for the given repository and doc id, no matter the xpath.
     */
    public static boolean isLocked(String repository, String docId) {
        return doPrivilegedOnLockDirectory(session -> {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(LOCK_DIRECTORY_REPOSITORY, repository);
            filter.put(LOCK_DIRECTORY_DOC_ID, docId);
            return !session.query(filter).isEmpty();
        });
    }

    /**
     * Checks if a WOPI lock is stored for another file id than the given one.
     * <p>
     * Repository name and document id are extracted from the given file id.
     */
    public static boolean hasOtherLock(String fileId) {
        FileInfo fileInfo = new FileInfo(fileId);
        return doPrivilegedOnLockDirectory(session -> {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(LOCK_DIRECTORY_REPOSITORY, fileInfo.repositoryName);
            filter.put(LOCK_DIRECTORY_DOC_ID, fileInfo.docId);
            return !session.query(filter)
                           .stream()
                           .filter(e -> !e.getId().equals(fileId))
                           .collect(Collectors.toList())
                           .isEmpty();
        });
    }

    /**
     * Updates the WOPI lock stored for the given file id with the given lock and a fresh timestamp.
     */
    public static void updateLock(String fileId, String lock) {
        log.debug("Locking: fileId={} Updating lock {}", fileId, lock);
        doPrivilegedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(fileId);
            entry.setProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_LOCK, lock);
            entry.setProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP, System.currentTimeMillis());
            session.updateEntry(entry);
        });
    }

    /**
     * Updates the WOPI lock stored for the given file id with a fresh timestamp.
     */
    public static void refreshLock(String fileId) {
        log.debug("Locking: fileId={} Refreshing lock", fileId);
        doPrivilegedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(fileId);
            entry.setProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP, System.currentTimeMillis());
            session.updateEntry(entry);
        });
    }

    /**
     * Removes the WOPI lock stored for the given file id.
     */
    public static void removeLock(String fileId) {
        log.debug("Locking: fileId={} Removing lock", fileId);
        doPrivilegedOnLockDirectory((Session session) -> session.deleteEntry(fileId));
    }

    /**
     * Removes all the WOPI locks stored for the given repository and doc id.
     */
    public static void removeLocks(String repository, String docId) {
        log.debug("Locking: repository={} docId={} Document was unlocked in Nuxeo, removing related WOPI locks",
                repository, docId);
        doPrivilegedOnLockDirectory(session -> {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(LOCK_DIRECTORY_REPOSITORY, repository);
            filter.put(LOCK_DIRECTORY_DOC_ID, docId);
            session.query(filter).forEach(session::deleteEntry);
        });
    }

    /**
     * Performs the given consumer with a privileged session on the lock directory.
     */
    public static void doPrivilegedOnLockDirectory(Consumer<Session> consumer) {
        Framework.doPrivileged(() -> {
            try (Session session = openLockDirectorySession()) {
                consumer.accept(session);
            }
        });
    }

    /**
     * Applies the given function with a privileged session on the lock directory.
     */
    public static <R> R doPrivilegedOnLockDirectory(Function<Session, R> function) {
        return Framework.doPrivileged(() -> {
            try (Session session = openLockDirectorySession()) {
                return function.apply(session);
            }
        });
    }

    /**
     * Returns the list of expired stored WOPI locks according to the {@link Constants#LOCK_TTL} for each repository.
     * <p>
     * The given session must be privileged.
     */
    public static Map<String, List<DocumentModel>> getExpiredLocksByRepository(Session session) {
        return Framework.getService(RepositoryManager.class)
                        .getRepositoryNames()
                        .stream()
                        .map(repository -> getExpiredLocks(session, repository))
                        .reduce(new HashMap<String, List<DocumentModel>>(), (a, b) -> {
                            a.putAll(b);
                            return a;
                        });
    }

    /**
     * Marks the given principal as a WOPI user.
     */
    public static void markAsWOPIUser(NuxeoPrincipal principal) {
        synchronized (principal) { // NOSONAR
            principal.getModel().putContextData(WOPI_USER, true);
        }
    }

    /**
     * Checks if the given principal is marked as a WOPI user.
     */
    public static boolean isWOPIUser(NuxeoPrincipal principal) {
        synchronized (principal) { // NOSONAR
            return principal.getModel().getContextData(WOPI_USER) != null;
        }
    }

    protected static Map<String, List<DocumentModel>> getExpiredLocks(Session session, String repository) {
        long expirationTime = System.currentTimeMillis() - LOCK_TTL;
        // TODO: inefficient if there are many locks.
        // To be refactored to filter directly in the query when NXP-19262 is done.
        List<DocumentModel> expiredLocks = session.query(
                Collections.singletonMap(LOCK_DIRECTORY_REPOSITORY, repository))
                                                  .stream()
                                                  .filter(entry -> filterExpiredLocks(entry, expirationTime))
                                                  .collect(Collectors.toList());
        return Collections.singletonMap(repository, expiredLocks);
    }

    protected static boolean filterExpiredLocks(DocumentModel entry, long expirationTime) {
        return expirationTime > (long) entry.getProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP);
    }

    protected static Session openLockDirectorySession() {
        return Framework.getService(DirectoryService.class).open(LOCK_DIRECTORY_NAME);
    }

}

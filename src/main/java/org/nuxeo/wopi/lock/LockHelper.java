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
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        doPriviledgedOnLockDirectory(session -> {
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
        return callPriviledgedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(fileId);
            return entry == null ? null : (String) entry.getProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_LOCK);
        });
    }

    /**
     * Checks if a WOPI lock is stored for the given repository and doc id, no matter the xpath.
     */
    public static boolean isLocked(String repository, String docId) {
        return callPriviledgedOnLockDirectory(session -> {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(LOCK_DIRECTORY_REPOSITORY, repository);
            filter.put(LOCK_DIRECTORY_DOC_ID, docId);
            return !session.query(filter).isEmpty();
        });
    }

    /**
     * Updates the WOPI lock stored for the given file id with the given lock and a fresh timestamp.
     */
    public static void updateLock(String fileId, String lock) {
        doPriviledgedOnLockDirectory(session -> {
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
        doPriviledgedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(fileId);
            entry.setProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP, System.currentTimeMillis());
            session.updateEntry(entry);
        });
    }

    /**
     * Removes the WOPI lock stored for the given file id.
     */
    public static void removeLock(String fileId) {
        doPriviledgedOnLockDirectory(session -> session.deleteEntry(fileId));
    }

    /**
     * Returns the list of expired stored WOPI locks according to the {@link Constants#LOCK_TTL} for each repository.
     */
    public static Map<String, List<DocumentModel>> getExpiredLocksByRepository() {
        return callPriviledgedOnLockDirectory(LockHelper::getExpiredLocksByRepository);
    }

    /**
     * Marks the given principal as a WOPI user.
     */
    public static void markAsWOPIUser(Principal principal) {
        if (principal instanceof NuxeoPrincipal) {
            synchronized (principal) {
                ((NuxeoPrincipal) principal).getModel().putContextData(WOPI_USER, true);
            }
        }
    }

    /**
     * Checks if the given principal is marked as a WOPI user.
     */
    public static boolean isWOPIUser(Principal principal) {
        if (!(principal instanceof NuxeoPrincipal)) {
            return false;
        }
        synchronized (principal) {
            return ((NuxeoPrincipal) principal).getModel().getContextData(WOPI_USER) != null;
        }
    }

    protected static Map<String, List<DocumentModel>> getExpiredLocksByRepository(Session session) {
        return Framework.getService(RepositoryManager.class)
                        .getRepositoryNames()
                        .stream()
                        .map(repository -> getExpiredLocks(session, repository))
                        .reduce(new HashMap<String, List<DocumentModel>>(), (a, b) -> {
                            a.putAll(b);
                            return a;
                        });
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

    protected static void doPriviledgedOnLockDirectory(Consumer<Session> consumer) {
        Framework.doPrivileged(() -> {
            try (Session session = openLockDirectorySession()) {
                consumer.accept(session);
            }
        });
    }

    protected static <R> R callPriviledgedOnLockDirectory(Function<Session, R> function) {
        return Framework.doPrivileged(() -> {
            try (Session session = openLockDirectorySession()) {
                return function.apply(session);
            }
        });
    }

    protected static Session openLockDirectorySession() {
        return Framework.getService(DirectoryService.class).open(LOCK_DIRECTORY_NAME);
    }

}

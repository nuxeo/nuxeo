/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.directory;

import static org.nuxeo.ecm.core.query.sql.model.Operator.AND;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.eq;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.isnull;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.maxPreferences;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserDocPreferences;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.sanitizeValue;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.validateKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.annotation.Nonnull;

import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.user.preferences.api.UserPreference;
import org.nuxeo.user.preferences.api.UserPreferences;
import org.nuxeo.user.preferences.api.UserPreferencesService;
import org.nuxeo.user.preferences.api.UserPreferencesUtil;
import org.nuxeo.user.preferences.exception.TooManyUserPreferencesException;
import org.nuxeo.user.preferences.exception.UserPreferencesExistsException;
import org.nuxeo.user.preferences.exception.UserPreferencesNotFound;

/**
 * {@link org.nuxeo.ecm.directory.Directory} based implementation of {@link UserPreferencesService}.
 * <p>
 * Both {@link UserPreference} and {@link UserPreferences} are stored in the same {@link #DIRECTORY_NAME} directory and
 * have the following fields:
 * <ul>
 * <li>{@code userId}</li>
 * <li>{@code repository}</li>
 * <li>{@code key}</li>
 * <li>{@code value}</li>
 * </ul>
 * {@link UserPreferences} has the {@code docId} additional field.
 *
 * @since 2025.16
 */
public class UserPreferencesServiceImpl extends DefaultComponent implements UserPreferencesService {

    private static final Logger log = LogManager.getLogger(UserPreferencesServiceImpl.class);

    protected static final String DIRECTORY_NAME = "userPreferences";

    @Override
    public Optional<UserPreference> get(CoreSession session, @Nonnull String key) {
        Objects.requireNonNull(key, "The key can not be null");
        return withDirectorySession(dirSession -> {
            return doGet(session, dirSession, key).map(UserPreferencesServiceImpl::newUserPreference);
        });
    }

    protected Optional<DocumentModel> doGet(CoreSession session, Session dirSession, String key) {
        return queryPreferences(session, dirSession, null, key, 1).stream().findFirst();
    }

    @Override
    public UserPreferences get(CoreSession session, @Nonnull DocumentRef docRef) {
        Objects.requireNonNull(docRef, "The document reference can not be null");
        var idRef = getDocId(session, docRef);
        return withDirectorySession(dirSession -> {
            return doGet(session, dirSession, idRef);
        });
    }

    public UserPreferences doGet(CoreSession session, Session dirSession, IdRef idRef) {
        var preferences = retrieveAllDocPreferences(session, dirSession,
                idRef).stream().map(UserPreferencesServiceImpl::newUserPreference).toList();
        return newUserDocPreferences(preferences);
    }

    @Override
    public Optional<UserPreference> get(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull String key) {
        Objects.requireNonNull(key, "The key can not be null");
        Objects.requireNonNull(docRef, "The document reference can not be null");
        var idRef = getDocId(session, docRef);
        return withDirectorySession(dirSession -> {
            return retrieveDocPreferenceForKey(session, dirSession, idRef, key).map(
                    UserPreferencesServiceImpl::newUserPreference);
        });
    }

    @Override
    public UserPreferences list(CoreSession session) {
        return withDirectorySession(dirSession -> {
            return newUserDocPreferences(retrieveAllPreferencesForUser(session,
                    dirSession).stream().map(UserPreferencesServiceImpl::newUserPreference).toList());
        });
    }

    @Override
    public UserPreference create(CoreSession session, @Nonnull String key, @Nonnull String value) {
        Objects.requireNonNull(key, "The key can not be null");
        Objects.requireNonNull(value, "The value can not be null");
        validateKey(key);
        return withDirectorySession(dirSession -> {
            return doCreate(session, dirSession, key, value);
        });
    }

    protected UserPreference doCreate(CoreSession session, Session dirSession, String key, String value) {
        checkMaxPreferences(session, dirSession, 1);
        if (doGet(session, dirSession, key).isPresent()) {
            throw new UserPreferencesExistsException(key);
        }
        return newUserPreference(dirSession.createEntry(getFieldMap(session, null, key, value)));
    }

    @Override
    public UserPreference createOrUpdate(CoreSession session, @Nonnull String key, @Nonnull String value) {
        Objects.requireNonNull(key, "The key can not be null");
        Objects.requireNonNull(value, "The value can not be null");
        validateKey(key);
        return withDirectorySession(dirSession -> {
            return doGet(session, dirSession, key).map(docModel -> doUpdate(dirSession, docModel, value))
                                                  .orElseGet(() -> doCreate(session, dirSession, key, value));
        });
    }

    @Override
    public UserPreferences create(CoreSession session, @Nonnull DocumentRef docRef,
            @Nonnull Map<String, String> preferences) {
        Objects.requireNonNull(docRef, "The document reference can not be null");
        var sanitizedPreferences = validateAndSanitize(preferences);
        return withDirectorySession(dirSession -> {
            return doCreate(session, dirSession, docRef, sanitizedPreferences);
        });
    }

    protected UserPreferences doCreate(CoreSession session, Session dirSession, DocumentRef docRef,
            Map<String, String> preferences) {
        checkMaxPreferences(session, dirSession, preferences.size());
        var idRef = getDocId(session, docRef);
        if (hasPreferences(session, dirSession, idRef)) {
            throw new UserPreferencesExistsException(idRef);
        }
        preferences.forEach((key, value) -> dirSession.createEntry(getFieldMap(session, idRef, key, value)));
        return doGet(session, dirSession, idRef);
    }

    protected static boolean hasPreferences(CoreSession session, Session dirSession, IdRef idRef) {
        return !queryPreferences(session, dirSession, idRef, null, 1).isEmpty();
    }

    @Override
    public UserPreference update(CoreSession session, @Nonnull String key, @Nonnull String value) {
        Objects.requireNonNull(key, "The key can not be null");
        Objects.requireNonNull(value, "The value can not be null");
        return withDirectorySession(dirSession -> {
            return doGet(session, dirSession, key).map(docModel -> doUpdate(dirSession, docModel, value))
                                                  .orElseThrow(() -> new UserPreferencesNotFound(key));
        });
    }

    protected UserPreference doUpdate(Session dirSession, DocumentModel docModel, String value) {
        var id = String.valueOf(docModel.getPropertyValue(dirSession.getIdField()));
        docModel.setPropertyValue("value", sanitizeValue(value));
        dirSession.updateEntry(docModel);
        return newUserPreference(dirSession.getEntry(id));
    }

    @Override
    public UserPreferences update(CoreSession session, @Nonnull DocumentRef docRef,
            @Nonnull UserPreferences docPreferences) {
        var sanitizedPreferences = validateAndSanitize(docPreferences.preferences());
        var idRef = getDocId(session, docRef);
        return withDirectorySession(dirSession -> {
            var toBeDeleted = retrieveAllDocPreferences(session, dirSession, idRef);
            if (toBeDeleted.isEmpty()) {
                throw new UserPreferencesNotFound(idRef);
            }
            checkMaxPreferences(session, dirSession, sanitizedPreferences.size() - toBeDeleted.size());
            toBeDeleted.forEach(dirSession::deleteEntry);
            return doCreate(session, dirSession, idRef, sanitizedPreferences);
        });
    }

    @Override
    public UserPreferences putAll(CoreSession session, @Nonnull DocumentRef docRef,
            @Nonnull UserPreferences docPreferences) {
        var idRef = getDocId(session, docRef);
        return withDirectorySession(dirSession -> {
            List<DocumentModel> toBeUpdated = new ArrayList<>();
            List<Map<String, Object>> toBeCreated = new ArrayList<>();
            validateAndSanitize(docPreferences.preferences()).forEach((key, value) -> {
                Optional<DocumentModel> optionalPreference = retrieveDocPreferenceForKey(session, dirSession, idRef,
                        key);
                optionalPreference.ifPresentOrElse(doc -> {
                    doc.setPropertyValue("value", value);
                    toBeUpdated.add(doc);
                }, () -> toBeCreated.add(getFieldMap(session, idRef, key, value)));
            });
            checkMaxPreferences(session, dirSession, toBeCreated.size());
            toBeUpdated.forEach(dirSession::updateEntry);
            toBeCreated.forEach(dirSession::createEntry);
            return doGet(session, dirSession, idRef);
        });
    }

    @Override
    public UserPreferences remove(CoreSession session, @Nonnull DocumentRef docRef, @Nonnull String key) {
        Objects.requireNonNull(key, "The key can not be null");
        var idRef = getDocId(session, docRef);
        return withDirectorySession(dirSession -> {
            retrieveDocPreferenceForKey(session, dirSession, idRef, key).ifPresent(dirSession::deleteEntry);
            return doGet(session, dirSession, idRef);
        });
    }

    @Override
    public void delete(CoreSession session, @Nonnull String key) {
        withDirectorySession(dirSession -> {
            doGet(session, dirSession, key).ifPresent(dirSession::deleteEntry);
        });
    }

    @Override
    public void delete(CoreSession session, @Nonnull DocumentRef docRef) {
        var idRef = getDocId(session, docRef);
        withDirectorySession(dirSession -> {
            retrieveAllDocPreferences(session, dirSession, idRef).forEach(dirSession::deleteEntry);
        });
    }

    // Internal use
    @Override
    public void deleteAllForDocument(CoreSession session, @Nonnull IdRef idRef) {
        log.debug("Deleting all user preferences for doc: {}", idRef::toString);
        var queryBuilder = new QueryBuilder().predicate(eq("docId", idRef.toString()))
                                             .and(eq("repository", session.getRepositoryName()));
        doBatchDelete(queryBuilder);
    }

    // Internal use
    @Override
    public void deleteAllForUser(CoreSession session, @Nonnull String userId) {
        log.debug("Deleting all preferences for user: {}", userId);
        var queryBuilder = new QueryBuilder().predicate(eq("userId", userId))
                                             .and(eq("repository", session.getRepositoryName()));
        doBatchDelete(queryBuilder);
    }

    protected void doBatchDelete(QueryBuilder queryBuilder) {
        log.debug("Deleting all preferences for query: {}", queryBuilder);
        queryBuilder.limit(100);
        withDirectorySession(dirSession -> {
            List<String> ids;
            do {
                ids = dirSession.queryIds(queryBuilder);
                ids.forEach(dirSession::deleteEntry);
                log.trace("Deleted {} user preferences", ids::size);
            } while (!ids.isEmpty());
        });
    }

    protected static void checkMaxPreferences(CoreSession session, Session dirSession, int nbNew) {
        var limit = maxPreferences();
        var queryBuilder = new QueryBuilder().predicate(eq("userId", session.getPrincipal().getId()))
                                             .and(eq("repository", session.getRepositoryName()))
                                             .countTotal(true)
                                             .limit(0);
        @SuppressWarnings("deprecation")
        var actualSize = dirSession.query(queryBuilder).totalSize();
        if (actualSize < 0 || actualSize + nbNew > limit) {
            throw new TooManyUserPreferencesException(session.getPrincipal().getId(), actualSize, limit);
        }
    }

    protected static IdRef getDocId(CoreSession session, DocumentRef documentRef) {
        if (documentRef.type() == DocumentRef.ID) {
            return (IdRef) documentRef;
        }
        return (IdRef) session.getDocument(documentRef).getRef();
    }

    protected static Map<String, Object> getFieldMap(CoreSession session, IdRef idRef, String key, String value) {
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("userId", session.getPrincipal().getId());
        fieldMap.put("repository", session.getRepositoryName());
        fieldMap.put("key", key);
        fieldMap.put("value", sanitizeValue(value));
        if (idRef != null) {
            fieldMap.put("docId", idRef.toString());
        }
        return fieldMap;
    }

    protected static UserPreference newUserPreference(DocumentModel document) {
        return UserPreferencesUtil.newUserPreference((String) document.getPropertyValue("key"),
                (String) document.getPropertyValue("value"));
    }

    @SuppressWarnings("deprecation")
    protected static DocumentModelList queryPreferences(CoreSession session, Session dirSession, IdRef idRef,
            String key, long limit) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(eq("userId", session.getPrincipal().getId()));
        predicates.add(eq("repository", session.getRepositoryName()));
        if (idRef == null) {
            predicates.add(isnull("docId"));
        } else {
            predicates.add(eq("docId", idRef.toString()));
        }
        if (key != null) {
            predicates.add(eq("key", key));
        }
        var queryBuilder = new QueryBuilder().predicate(new MultiExpression(AND, predicates))
                                             .limit(limit)
                                             .order(OrderByExprs.desc("id"));
        return dirSession.query(queryBuilder);
    }

    protected static DocumentModelList retrieveAllDocPreferences(CoreSession session, Session dirSession, IdRef idRef) {
        return queryPreferences(session, dirSession, idRef, null, 0);
    }

    protected static DocumentModelList retrieveAllPreferencesForUser(CoreSession session, Session dirSession) {
        return queryPreferences(session, dirSession, null, null, 0);
    }

    protected static Optional<DocumentModel> retrieveDocPreferenceForKey(CoreSession session, Session dirSession,
            IdRef idRef, String key) {
        return queryPreferences(session, dirSession, idRef, key, 1).stream().findFirst();
    }

    protected static Map<String, String> validateAndSanitize(Map<String, String> preferences) {
        if (MapUtils.isEmpty(preferences)) {
            throw new NullPointerException("The preferences can not be null or empty");
        }
        Map<String, String> result = new HashMap<>();
        preferences.forEach((key, value) -> {
            validateKey(key);
            Objects.requireNonNull(value, "The preference: %s can not be null".formatted(key));
            result.put(key, sanitizeValue(value));
        });
        return result;
    }

    protected static <R> R withDirectorySession(Function<Session, R> function) {
        try (Session dirSession = Framework.getService(DirectoryService.class).open(DIRECTORY_NAME)) {
            return function.apply(dirSession);
        }
    }

    protected static void withDirectorySession(Consumer<Session> consumer) {
        try (Session dirSession = Framework.getService(DirectoryService.class).open(DIRECTORY_NAME)) {
            consumer.accept(dirSession);
        }
    }
}

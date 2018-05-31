/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.local.LocalException;
import org.nuxeo.ecm.core.schema.types.resolver.AbstractObjectResolver;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

/**
 * This {@link ObjectResolver} allows to manage integrity for fields containing {@link DocumentModel} references (id or
 * path).
 * <p>
 * Resolved references must be either a path or an id, default mode is id. Storing path keep link with place in the
 * Document hierarchy no matter which Document is referenced. Storing id track the Document no matter where the Document
 * is stored.
 * </p>
 * <p>
 * All references, id or path, are prefixed with the document expected repository name. For example :
 * </p>
 * <ul>
 * <li>default:352c21bc-f908-4507-af99-411d3d84ee7d</li>
 * <li>test:/path/to/my/doc</li>
 * </ul>
 * <p>
 * The {@link #fetch(Object)} method returns {@link DocumentModel}. The {@link #fetch(Class, Object)} returns
 * {@link DocumentModel} or specific document adapter.
 * </p>
 * <p>
 * To use it, put the following code in your schema XSD :
 * </p>
 *
 * <pre>
 * {@code
 * <!-- default resolver is an id based resolver -->
 * <xs:simpleType name="favoriteDocument1">
 *   <xs:restriction base="xs:string" ref:resolver="documentResolver" />
 * </xs:simpleType>
 *
 * <!-- store id -->
 * <xs:simpleType name="favoriteDocument2">
 *   <xs:restriction base="xs:string" ref:resolver="documentResolver" ref:store="id" />
 * </xs:simpleType>
 *
 * <!-- store path -->
 * <xs:simpleType name="bestDocumentRepositoryPlace">
 *   <xs:restriction base="xs:string" ref:resolver="documentResolver" ref:store="path" />
 * </xs:simpleType>
 * }
 * </pre>
 *
 * @since 7.1
 */
public class DocumentModelResolver extends AbstractObjectResolver implements ObjectResolver {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "documentResolver";

    public static final String PARAM_STORE = "store";

    /** @deprecated since 10.2, use {@link #STORE_REPO_AND_PATH instead} */
    @Deprecated
    public static final String STORE_PATH_COMPAT = "path";

    /** Since 10.2 */
    public static final String STORE_REPO_AND_PATH = "repoAndPath";

    /** Since 10.2 */
    public static final String STORE_PATH_ONLY = "pathOnly";

    /** @deprecated since 10.2, use {@link #STORE_REPO_AND_ID instead} */
    @Deprecated
    public static final String STORE_ID_COMPAT = "id";

    /** Since 10.2 */
    public static final String STORE_REPO_AND_ID = "repoAndId";

    /** Since 10.2 */
    public static final String STORE_ID_ONLY = "idOnly";

    public enum MODE {
        /** Reference is a path prefixed with a repository (repository is optional on set). */
        REPO_AND_PATH_REF,
        /** Reference is an id prefixed with a repository (repository is optional on set). */
        REPO_AND_ID_REF,
        /** Reference is a path (repository prefix is optional on set). */
        PATH_ONLY_REF,
        /** Reference is a path (repository prefix is optional on set). */
        ID_ONLY_REF,
    }

    private MODE mode = MODE.REPO_AND_ID_REF;

    public MODE getMode() {
        return mode;
    }

    private List<Class<?>> managedClasses = null;

    @Override
    public List<Class<?>> getManagedClasses() {
        if (managedClasses == null) {
            managedClasses = new ArrayList<>();
            managedClasses.add(DocumentModel.class);
        }
        return managedClasses;
    }

    @Override
    public void configure(Map<String, String> parameters) throws IllegalStateException {
        super.configure(parameters);
        String store = parameters.get(PARAM_STORE);
        if (store == null) {
            store = ""; // use default
        }
        switch (store) {
        case STORE_PATH_ONLY:
            mode = MODE.PATH_ONLY_REF;
            break;
        case STORE_ID_ONLY:
            mode = MODE.ID_ONLY_REF;
            break;
        case STORE_REPO_AND_PATH:
        case STORE_PATH_COMPAT:
            mode = MODE.REPO_AND_PATH_REF;
            store = STORE_REPO_AND_PATH;
            break;
        case STORE_REPO_AND_ID:
        case STORE_ID_COMPAT:
        default:
            mode = MODE.REPO_AND_ID_REF;
            store = STORE_REPO_AND_ID;
            break;
        }
        this.parameters.put(PARAM_STORE, store);
    }

    @Override
    public String getName() {
        checkConfig();
        return NAME;
    }

    @Override
    public boolean validate(Object value) {
        return validate(value, null);
    }

    @Override
    public boolean validate(Object value, Object context) {
        MutableBoolean validated = new MutableBoolean();
        resolve(value, context, (session, docRef) -> {
            if (session.exists(docRef)) {
                validated.setTrue();
            }
        });
        return validated.isTrue();
    }

    @Override
    public Object fetch(Object value) {
        return fetch(value, null);
    }

    @Override
    public Object fetch(Object value, Object context) {
        MutableObject<DocumentModel> docHolder = new MutableObject<>();
        resolve(value, context, (session, docRef) -> {
            if (session.exists(docRef)) {
                DocumentModel doc = session.getDocument(docRef);
                // detach because we're about to close the session
                doc.detach(true);
                docHolder.setValue(doc);
            }
        });
        return docHolder.getValue();
    }

    /**
     * Resolves the value (in the context) into a session and docRef, and passes them to the resolver.
     * <p>
     * The resolver is not called if the value cannot be resolved.
     */
    protected void resolve(Object value, Object context, BiConsumer<CoreSession, DocumentRef> resolver) {
        checkConfig();
        if (!(value instanceof String)) {
            return;
        }
        REF ref = REF.fromValue((String) value);
        if (ref == null) {
            return;
        }
        CloseableCoreSession closeableCoreSession = null;
        try {
            CoreSession session;
            try {
                if (ref.repo != null) {
                    // we have an explicit repository name
                    if (context != null && ref.repo.equals(((CoreSession) context).getRepositoryName())) {
                        // if it's the same repository as the context session, use it directly
                        session = (CoreSession) context;
                    } else {
                        // otherwise open a new one
                        closeableCoreSession = CoreInstance.openCoreSession(ref.repo);
                        session = closeableCoreSession;
                    }
                } else {
                    // use session from context
                    session = (CoreSession) context;
                    if (session == null) {
                        // use the default repository if none is provided in the context
                        closeableCoreSession = CoreInstance.openCoreSession(null);
                        session = closeableCoreSession;
                    }
                }
            } catch (LocalException e) {
                // no such repository
                return;
            }
            DocumentRef docRef;
            switch (mode) {
            case ID_ONLY_REF:
            case REPO_AND_ID_REF:
                docRef = new IdRef(ref.ref);
                break;
            case PATH_ONLY_REF:
            case REPO_AND_PATH_REF:
                docRef = new PathRef(ref.ref);
                break;
            default:
                // unknown ref type
                return;
            }
            resolver.accept(session, docRef);
        } finally {
            if (closeableCoreSession != null) {
                closeableCoreSession.close();
            }
        }
    }

    @Override
    public <T> T fetch(Class<T> type, Object value) throws IllegalStateException {
        checkConfig();
        DocumentModel doc = (DocumentModel) fetch(value);
        if (doc != null) {
            if (type.isInstance(doc)) {
                return type.cast(doc);
            }
            return doc.getAdapter(type);
        }
        return null;
    }

    @Override
    public Serializable getReference(Object entity) throws IllegalStateException {
        checkConfig();
        if (entity instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) entity;
            switch (mode) {
            case ID_ONLY_REF:
                return doc.getId();
            case PATH_ONLY_REF:
                return doc.getPath().toString();
            case REPO_AND_ID_REF:
                return doc.getRepositoryName() + ":" + doc.getId();
            case REPO_AND_PATH_REF:
                return doc.getRepositoryName() + ":" + doc.getPath().toString();
            }
        }
        return null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        checkConfig();
        switch (mode) {
        case ID_ONLY_REF:
        case REPO_AND_ID_REF:
            return Helper.getConstraintErrorMessage(this, "id", invalidValue, locale);
        case PATH_ONLY_REF:
        case REPO_AND_PATH_REF:
            return Helper.getConstraintErrorMessage(this, "path", invalidValue, locale);
        default:
            return String.format("%s cannot resolve reference %s", getName(), invalidValue);
        }
    }


    protected static final class REF {

        protected String repo;

        protected String ref;

        protected REF() {
        }

        protected static REF fromValue(String value) {
            String[] split = value.split(":");
            if (split.length == 1) {
                REF ref = new REF();
                ref.repo = null; // caller will use context session, or the default repo
                ref.ref = split[0];
                return ref;
            }
            if (split.length == 2) {
                REF ref = new REF();
                ref.repo = split[0];
                ref.ref = split[1];
                return ref;
            }
            return null;
        }

    }

}

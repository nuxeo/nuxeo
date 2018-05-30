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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.local.LocalException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.schema.types.resolver.AbstractObjectResolver;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.api.Framework;

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

    /** @deprecated since 10.2, use {@link #STORE_REPO_AND_ID instead} */
    @Deprecated
    public static final String STORE_ID_COMPAT = "id";

    /** Since 10.2 */
    public static final String STORE_REPO_AND_ID = "repoAndId";

    public enum MODE {
        /** Reference is a path optionally prefixed with a repository name. */
        REPO_AND_PATH_REF,
        /** Reference is an id optionally prefixed with a repository name. */
        REPO_AND_ID_REF,
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
    public boolean validate(Object value) throws IllegalStateException {
        checkConfig();
        if (!validation) {
            return true;
        }
        if (value instanceof String) {
            REF ref = REF.fromValue((String) value);
            if (ref != null) {
                try (CloseableCoreSession session = CoreInstance.openCoreSession(ref.repo)) {
                    switch (mode) {
                    case REPO_AND_ID_REF:
                        return session.exists(new IdRef(ref.ref));
                    case REPO_AND_PATH_REF:
                        return session.exists(new PathRef(ref.ref));
                    }
                } catch (LocalException le) { // no such repo
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public Object fetch(Object value) throws IllegalStateException {
        checkConfig();
        if (value instanceof String) {
            REF ref = REF.fromValue((String) value);
            if (ref != null) {
                try (CloseableCoreSession session = CoreInstance.openCoreSession(ref.repo)) {
                    DocumentModel doc;
                    DocumentRef docRef;

                    switch (mode) {
                    case REPO_AND_ID_REF:
                        docRef = new IdRef(ref.ref);
                        break;
                    case REPO_AND_PATH_REF:
                        docRef = new PathRef(ref.ref);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                    }

                    if (!session.exists(docRef)) {
                        // the document doesn't exist or is not accessible by the current user
                        return null;
                    }

                    doc = session.getDocument(docRef);
                    // detach because we're about to close the session
                    doc.detach(true);
                    return doc;
                } catch (LocalException le) { // no such repo
                    return null;
                }
            }
        }
        return null;
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
            String repositoryName = doc.getRepositoryName();
            if (repositoryName != null) {
                switch (mode) {
                case REPO_AND_ID_REF:
                    return repositoryName + ":" + doc.getId();
                case REPO_AND_PATH_REF:
                    return repositoryName + ":" + doc.getPath().toString();
                }
            }
        }
        return null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        checkConfig();
        switch (mode) {
        case REPO_AND_ID_REF:
            return Helper.getConstraintErrorMessage(this, "id", invalidValue, locale);
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
                ref.repo = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
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

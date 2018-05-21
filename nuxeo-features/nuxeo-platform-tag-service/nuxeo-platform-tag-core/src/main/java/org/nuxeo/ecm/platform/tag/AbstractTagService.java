/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.3
 */
public abstract class AbstractTagService implements TagService {

    protected enum PAGE_PROVIDERS {
        GET_DOCUMENT_IDS_FOR_FACETED_TAG,
        //
        GET_DOCUMENT_IDS_FOR_TAG,
        //
        GET_FIRST_TAGGING_FOR_DOC_AND_TAG_AND_USER,
        //
        GET_FIRST_TAGGING_FOR_DOC_AND_TAG,
        //
        GET_TAGS_FOR_DOCUMENT,
        // core version: should keep on querying VCS
        GET_TAGS_FOR_DOCUMENT_CORE,
        //
        GET_DOCUMENTS_FOR_TAG,
        //
        GET_TAGS_FOR_DOCUMENT_AND_USER,
        // core version: should keep on querying VCS
        GET_TAGS_FOR_DOCUMENT_AND_USER_CORE,
        //
        GET_DOCUMENTS_FOR_TAG_AND_USER,
        //
        GET_TAGS_TO_COPY_FOR_DOCUMENT,
        //
        GET_FACETED_TAG_SUGGESTIONS,
        //
        GET_TAG_SUGGESTIONS,
        //
        GET_TAG_SUGGESTIONS_FOR_USER,
        //
        GET_TAGGED_DOCUMENTS_UNDER,
        //
        GET_ALL_TAGS,
        //
        GET_ALL_TAGS_FOR_USER,
        //
        GET_TAGS_FOR_DOCUMENTS,
        //
        GET_TAGS_FOR_DOCUMENTS_AND_USER,
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void tag(CoreSession session, String docId, String label) throws DocumentSecurityException {
        String cleanLabel = cleanLabel(label, true, false);
        String username = cleanUsername(session.getPrincipal().getName());
        CoreInstance.doPrivileged(session, s -> {
            doTag(s, docId, cleanLabel, username);
        });
        fireUpdateEvent(session, docId);
    }

    @Override
    public void tag(CoreSession session, String docId, String label, String username) {
        tag(session, docId, label);
    }

    @Override
    public void untag(CoreSession session, String docId, String label)
            throws DocumentSecurityException {
        // There's two allowed cases here:
        // - document doesn't exist, we're here after documentRemoved event
        // - regular case: check if user can remove this tag on document
        if (!session.exists(new IdRef(docId)) || canUntag(session, docId, label)) {
            String cleanLabel = cleanLabel(label, true, false);
            CoreInstance.doPrivileged(session, s -> {
                doUntag(s, docId, cleanLabel);
            });
            if (label != null) {
                fireUpdateEvent(session, docId);
            }
        } else {
            String principalName = session.getPrincipal().getName();
            throw new DocumentSecurityException("User '" + principalName + "' is not allowed to remove tag '" + label
                    + "' on document '" + docId + "'");
        }
    }

    @Override
    public void untag(CoreSession session, String docId, String label, String username)
            throws DocumentSecurityException {
        untag(session, docId, label);
    }

    @Override
    public boolean canUntag(CoreSession session, String docId, String label) {
        return session.hasPermission(new IdRef(docId), SecurityConstants.WRITE);
    }

    @Override
    public Set<String> getTags(CoreSession session, String docId) {
        return CoreInstance.doPrivileged(session, (CoreSession s) -> doGetTags(s, docId));
    }

    @Override
    public List<Tag> getDocumentTags(CoreSession session, String docId, String username) {
        return getTags(session, docId).stream().map(t -> new Tag(t, 0)).collect(Collectors.toList());
    }

    @Override
    public List<Tag> getDocumentTags(CoreSession session, String docId, String username, boolean useCore) {
        return getTags(session, docId).stream().map(t -> new Tag(t, 0)).collect(Collectors.toList());
    }

    @Override
    public void removeTags(CoreSession session, String docId) {
        untag(session, docId, null);
    }

    @Override
    public void copyTags(CoreSession session, String srcDocId, String dstDocId) {
        copyTags(session, srcDocId, dstDocId, false);
    }

    protected void copyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags) {
        CoreInstance.doPrivileged(session, s -> {
            doCopyTags(s, srcDocId, dstDocId, removeExistingTags);
        });
    }

    @Override
    public void replaceTags(CoreSession session, String srcDocId, String dstDocId) {
        copyTags(session, srcDocId, dstDocId, true);
    }

    @Override
    public List<String> getTagDocumentIds(CoreSession session, String label) {
        String cleanLabel = cleanLabel(label, true, false);
        return CoreInstance.doPrivileged(session, (CoreSession s) -> doGetTagDocumentIds(s, cleanLabel));
    }

    @Override
    public List<String> getTagDocumentIds(CoreSession session, String label, String username) {
        return getTagDocumentIds(session, label);
    }

    @Override
    public Set<String> getSuggestions(CoreSession session, String label) {
        label = cleanLabel(label, true, true);
        if (!label.contains("%")) {
            label += "%";
        }
        // effectively final for lambda
        String l = label;
        return CoreInstance.doPrivileged(session, (CoreSession s) -> doGetTagSuggestions(s, l));
    }

    @Override
    public List<Tag> getSuggestions(CoreSession session, String label, String username) {
        return getSuggestions(session, label).stream().map(t -> new Tag(t, 0)).collect(Collectors.toList());
    }

    public abstract void doTag(CoreSession session, String docId, String label, String username);

    public abstract void doUntag(CoreSession session, String docId, String label);

    public abstract Set<String> doGetTags(CoreSession session, String docId);

    public abstract void doCopyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags);

    public abstract List<String> doGetTagDocumentIds(CoreSession session, String label);

    public abstract Set<String> doGetTagSuggestions(CoreSession session, String label);

    protected static String cleanLabel(String label, boolean allowEmpty, boolean allowPercent) {
        if (label == null) {
            if (allowEmpty) {
                return null;
            }
            throw new NuxeoException("Invalid empty tag");
        }
        label = label.toLowerCase(); // lowercase
        label = label.replace(" ", ""); // no spaces
        label = label.replace("/", ""); // no slash
        label = label.replace("\\", ""); // dubious char
        label = label.replace("'", ""); // dubious char
        if (!allowPercent) {
            label = label.replace("%", ""); // dubious char
        }
        if (label.length() == 0) {
            throw new NuxeoException("Invalid empty tag");
        }
        return label;
    }

    protected static String cleanUsername(String username) {
        return username == null ? null : username.replace("'", "");
    }

    /**
     * Returns results from calls to {@link CoreSession#queryAndFetch(String, String, Object...)} using page providers.
     *
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    protected static List<Map<String, Serializable>> getItems(String pageProviderName, CoreSession session,
            Object... params) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        if (ppService == null) {
            throw new RuntimeException("Missing PageProvider service");
        }
        Map<String, Serializable> props = new HashMap<>();
        // first retrieve potential props from definition
        PageProviderDefinition def = ppService.getPageProviderDefinition(pageProviderName);
        if (def != null) {
            Map<String, String> defProps = def.getProperties();
            if (defProps != null) {
                props.putAll(defProps);
            }
        }
        props.put(CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) ppService.getPageProvider(
                pageProviderName, null, null, null, props, params);
        if (pp == null) {
            throw new NuxeoException("Page provider not found: " + pageProviderName);
        }
        return pp.getCurrentPage();
    }

    protected void fireUpdateEvent(CoreSession session, String docId) {
        DocumentRef documentRef = new IdRef(docId);
        if (session.exists(documentRef)) {
            DocumentModel documentModel = session.getDocument(documentRef);
            DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), documentModel);
            Event event = ctx.newEvent(DocumentEventTypes.DOCUMENT_TAG_UPDATED);
            Framework.getService(EventService.class).fireEvent(event);
        }
    }

}

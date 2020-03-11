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
 */

package org.nuxeo.ecm.platform.tag;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.audit.service.NXAuditEventsService.DISABLE_AUDIT_LOGGER;
import static org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener.DISABLE_DUBLINCORE_LISTENER;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_FACET;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_LIST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.versioning.VersioningService;

/**
 * Implementation of the tag service based on facet
 *
 * @since 9.3
 */
public class FacetedTagService extends AbstractTagService {

    private static final Logger log = LogManager.getLogger(FacetedTagService.class);

    public static final String LABEL_PROPERTY = "label";

    public static final String USERNAME_PROPERTY = "username";

    /**
     * Context data to disable versioning, used by NoVersioningFacetedTagFilter.
     *
     * @since 9.10
     */
    public static final String DISABLE_VERSIONING = "tag.facet.disable.versioning";

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
        case TAGS_BELONG_TO_DOCUMENT:
            return true;
        default:
            throw new UnsupportedOperationException(feature.name());
        }
    }

    @Override
    public boolean supportsTag(CoreSession session, String docId) {
        return session.getDocument(new IdRef(docId)).hasFacet(TAG_FACET);
    }

    protected void saveDocument(CoreSession session, DocumentModel doc) {
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
        doc.putContextData(DISABLE_VERSIONING, Boolean.TRUE);
        doc.putContextData(DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
        doc.putContextData(DISABLE_AUDIT_LOGGER, Boolean.TRUE);
        session.saveDocument(doc);
    }

    @Override
    public void doTag(CoreSession session, String docId, String label, String username) {
        DocumentModel docModel = session.getDocument(new IdRef(docId));
        if (docModel.isProxy()) {
            throw new NuxeoException("Adding tags is not allowed on proxies");
        }
        List<Map<String, Serializable>> tags = getTags(docModel);
        if (tags.stream().noneMatch(t -> label.equals(t.get(LABEL_PROPERTY)))) {
            Map<String, Serializable> tag = new HashMap<>();
            tag.put(LABEL_PROPERTY, label);
            tag.put(USERNAME_PROPERTY, username);
            tags.add(tag);
            setTags(docModel, tags);
            saveDocument(session, docModel);
        }
    }

    @Override
    public void doUntag(CoreSession session, String docId, String label) {
        DocumentRef docRef = new IdRef(docId);
        if (!session.exists(docRef)) {
            return;
        }
        DocumentModel docModel = session.getDocument(docRef);
        if (docModel.isProxy()) {
            throw new NuxeoException("Removing tags is not allowed on proxies");
        }
        if (docModel.hasFacet(TAG_FACET)) {
            // If label is null, all the tags are removed
            if (label == null) {
                if (!getTags(docModel).isEmpty()) {
                    setTags(docModel, new ArrayList<>());
                    saveDocument(session, docModel);
                }
            } else {
                List<Map<String, Serializable>> tags = getTags(docModel);
                Map<String, Serializable> tag = tags.stream()
                                                    .filter(t -> label.equals(t.get(LABEL_PROPERTY)))
                                                    .findFirst()
                                                    .orElse(null);
                if (tag != null) {
                    tags.remove(tag);
                    setTags(docModel, tags);
                    saveDocument(session, docModel);
                }
            }
        }
    }

    @Override
    public boolean canUntag(CoreSession session, String docId, String label) {
        boolean canUntag = super.canUntag(session, docId, label);
        if (!canUntag) {
            // Check also if the current user is the one who applied the tag
            DocumentModel docModel = session.getDocument(new IdRef(docId));
            Map<String, Serializable> tag = getTags(docModel).stream()
                                                             .filter(t -> label.equals(t.get(LABEL_PROPERTY)))
                                                             .findFirst()
                                                             .orElse(null);
            if (tag != null) {
                String username = session.getPrincipal().getName();
                canUntag = username.equals(tag.get(USERNAME_PROPERTY));
            }
        }
        return canUntag;
    }

    @Override
    public Set<String> doGetTags(CoreSession session, String docId) {
        DocumentRef docRef = new IdRef(docId);
        if (!session.exists(docRef)) {
            return Collections.emptySet();
        }
        DocumentModel docModel = session.getDocument(docRef);
        List<Map<String, Serializable>> tags = getTags(docModel);
        return tags.stream().map(t -> (String) t.get(LABEL_PROPERTY)).collect(Collectors.toSet());
    }

    @Override
    public void doCopyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags) {
        DocumentModel srcDocModel = session.getDocument(new IdRef(srcDocId));
        DocumentModel dstDocModel = session.getDocument(new IdRef(dstDocId));

        if (!dstDocModel.isProxy()) {
            List<Map<String, Serializable>> srcTags = getTags(srcDocModel);
            List<Map<String, Serializable>> dstTags;
            if (removeExistingTags) {
                dstTags = srcTags;
            } else {
                dstTags = getTags(dstDocModel);
                for (Map<String, Serializable> tag : srcTags) {
                    if (dstTags.stream().noneMatch(t -> tag.get(LABEL_PROPERTY).equals(t.get(LABEL_PROPERTY)))) {
                        dstTags.add(tag);
                    }
                }
            }
            setTags(dstDocModel, dstTags);
            saveDocument(session, dstDocModel);
        }
    }

    @Override
    public List<String> doGetTagDocumentIds(CoreSession session, String label) {
        List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_DOCUMENT_IDS_FOR_FACETED_TAG.name(), session,
                label);
        if (res == null) {
            return Collections.emptyList();
        }
        return res.stream().map(m -> (String) m.get(ECM_UUID)).collect(Collectors.toList());
    }

    @Override
    public Set<String> doGetTagSuggestions(CoreSession session, String label) {
        List<Map<String, Serializable>> res = getItems(PAGE_PROVIDERS.GET_FACETED_TAG_SUGGESTIONS.name(), session,
                label);
        if (res == null) {
            return Collections.emptySet();
        }
        return res.stream().map(m -> (String) m.get(TagConstants.TAG_LIST + "/*1/label")).collect(Collectors.toSet());
    }

    @Override
    public List<Tag> getTagCloud(CoreSession session, String docId, String username, Boolean normalize) {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Serializable>> getTags(DocumentModel docModel) {
        if (docModel.hasFacet(TAG_FACET)) {
            return (List<Map<String, Serializable>>) docModel.getPropertyValue(TAG_LIST);
        } else {
            return new ArrayList<>();
        }
    }

    protected void setTags(DocumentModel docModel, List<Map<String, Serializable>> tags) {
        if (!docModel.hasFacet(TAG_FACET)) {
            throw new NuxeoException(String.format("Document %s of type %s doesn't have the %s facet", docModel,
                    docModel.getType(), TAG_FACET));
        }
        if (docModel.isVersion()) {
            docModel.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        docModel.setPropertyValue(TAG_LIST, (Serializable) tags);
    }
}

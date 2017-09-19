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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.versioning.VersioningService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_FACET;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_LIST;

/**
 * Implementation of the tag service based on facet
 *
 * @since 9.3
 */
public class FacetedTagService extends AbstractTagService {

    public static final String LABEL_PROPERTY = "label";

    public static final String USERNAME_PROPERTY = "username";

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
            session.saveDocument(docModel);
        }
    }

    @Override
    public void doUntag(CoreSession session, String docId, String label) {
        DocumentModel docModel = session.getDocument(new IdRef(docId));
        if (docModel.isProxy()) {
            throw new NuxeoException("Removing tags is not allowed on proxies");
        }
        if (docModel.hasFacet(TAG_FACET)) {
            // If label is null, all the tags are removed
            if (label == null) {
                if (!getTags(docModel).isEmpty()) {
                    setTags(docModel, new ArrayList<>());
                    session.saveDocument(docModel);
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
                    session.saveDocument(docModel);
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
        DocumentModel docModel = session.getDocument(new IdRef(docId));
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
            // Disable auto checkout of destination document model
            // to disable deletion of a version which could be the base of the live document
            if (srcDocModel.isVersion()) {
                dstDocModel.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
            }
            session.saveDocument(dstDocModel);
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
        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Serializable>> getTags(DocumentModel docModel) {
        try {
            return (List<Map<String, Serializable>>) docModel.getPropertyValue(TAG_LIST);
        } catch (PropertyNotFoundException e) {
            return Collections.emptyList();
        }
    }

    protected void setTags(DocumentModel docModel, List<Map<String, Serializable>> tags) {
        if (docModel.isVersion()) {
            docModel.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        docModel.setPropertyValue(TAG_LIST, (Serializable) tags);
    }
}

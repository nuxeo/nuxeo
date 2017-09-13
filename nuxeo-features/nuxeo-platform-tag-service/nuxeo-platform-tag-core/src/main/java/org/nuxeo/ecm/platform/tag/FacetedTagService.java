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

    @Override
    public void doTag(CoreSession session, String docId, String label) {
        DocumentModel docModel = session.getDocument(new IdRef(docId));
        if (docModel.isProxy()) {
            throw new NuxeoException("Adding tags is not allowed on proxies");
        }
        List<String> tags = getTags(docModel);
        if (!tags.contains(label)) {
            tags.add(label);
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
                List<String> tags = getTags(docModel);
                if (tags.contains(label)) {
                    tags.remove(label);
                    setTags(docModel, tags);
                    session.saveDocument(docModel);
                }
            }
        }
    }

    @Override
    public Set<String> doGetTags(CoreSession session, String docId) {
        DocumentModel docModel = session.getDocument(new IdRef(docId));
        return new HashSet<>(getTags(docModel));
    }

    @Override
    public void doCopyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags) {
        DocumentModel srcDocModel = session.getDocument(new IdRef(srcDocId));
        DocumentModel dstDocModel = session.getDocument(new IdRef(dstDocId));

        if (!dstDocModel.isProxy()) {
            List<String> srcTags = getTags(srcDocModel);
            List<String> dstTags;
            if (removeExistingTags) {
                dstTags = srcTags;
            } else {
                dstTags = getTags(dstDocModel);
                for (String tag : srcTags) {
                    if (!dstTags.contains(tag)) {
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
        return res.stream().map(m -> (String) m.get(TagConstants.TAG_LIST + "/*1")).collect(Collectors.toSet());
    }

    @Override
    public List<Tag> getTagCloud(CoreSession session, String docId, String username, Boolean normalize) {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getTags(DocumentModel docModel) {
        try {
            return (List<String>) docModel.getPropertyValue(TAG_LIST);
        } catch (PropertyNotFoundException e) {
            return Collections.emptyList();
        }
    }

    protected void setTags(DocumentModel docModel, List<String> tags) {
        if (docModel.isVersion()) {
            docModel.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        docModel.setPropertyValue(TAG_LIST, (Serializable) tags);
    }
}

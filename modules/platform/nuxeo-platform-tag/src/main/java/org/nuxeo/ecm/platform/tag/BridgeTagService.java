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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Tag Service delegating to two different backends, for use during migration.
 *
 * @since 9.3
 */
public class BridgeTagService extends AbstractTagService {

    protected final AbstractTagService first;

    protected final AbstractTagService second;

    public BridgeTagService(TagService first, TagService second) {
        this.first = (AbstractTagService) first;
        this.second = (AbstractTagService) second;
    }

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
        case TAGS_BELONG_TO_DOCUMENT:
            return false;
        default:
            throw new UnsupportedOperationException(feature.name());
        }
    }

    @Override
    public boolean supportsTag(CoreSession session, String docId) {
        return first.supportsTag(session, docId) || second.supportsTag(session, docId);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<Tag> getTagCloud(CoreSession session, String docId, String username, Boolean normalize) {
        return first.getTagCloud(session, docId, username, normalize);
    }

    @Override
    public void doTag(CoreSession session, String docId, String label, String username) {
        // write to second only
        second.doTag(session, docId, label, username);
    }

    @Override
    public boolean canUntag(CoreSession session, String docId, String label) {
        // if the tag is not present we can untag, so we have to be careful doing the checks
        // we call getTags and not doGetTags because the query need to be privileged
        boolean can1 = !first.getTags(session, docId).contains(label) || first.canUntag(session, docId, label);
        boolean can2 = !second.getTags(session, docId).contains(label) || second.canUntag(session, docId, label);
        return can1 && can2;
    }

    @Override
    public void doUntag(CoreSession session, String docId, String label) {
        first.doUntag(session, docId, label);
        second.doUntag(session, docId, label);
    }

    @Override
    public Set<String> doGetTags(CoreSession session, String docId) {
        Set<String> tags = new HashSet<>();
        tags.addAll(first.doGetTags(session, docId));
        tags.addAll(second.doGetTags(session, docId));
        return tags;
    }

    @Override
    public void doCopyTags(CoreSession session, String srcDocId, String dstDocId, boolean removeExistingTags) {
        first.doCopyTags(session, srcDocId, dstDocId, removeExistingTags);
        second.doCopyTags(session, srcDocId, dstDocId, removeExistingTags);
    }

    @Override
    public List<String> doGetTagDocumentIds(CoreSession session, String label) {
        Set<String> ids = new HashSet<>();
        ids.addAll(first.doGetTagDocumentIds(session, label));
        ids.addAll(second.doGetTagDocumentIds(session, label));
        return new ArrayList<>(ids);
    }

    @Override
    public Set<String> doGetTagSuggestions(CoreSession session, String label) {
        Set<String> tags = new HashSet<>();
        tags.addAll(first.doGetTagSuggestions(session, label));
        tags.addAll(second.doGetTagSuggestions(session, label));
        return tags;
    }

}

/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.rendition.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Conversation-scoped bean for rendition actions contextual to current document, for cache reasons.
 *
 * @since 8.2
 */
@Name("renditionActionCache")
@Scope(ScopeType.CONVERSATION)
public class RenditionsActionsBeanCache implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Map<String, Boolean> hasVisibleRenditionsCache = new HashMap<String, Boolean>();

    public boolean hasVisibleRenditions(String excludedKinds) {
        if (!hasVisibleRenditionsCache.containsKey(excludedKinds)) {
            doCache(excludedKinds);
        }
        return hasVisibleRenditionsCache.get(excludedKinds);
    }

    protected void doCache(String excludedKinds) {
        RenditionActionBean renditionAction = (RenditionActionBean) Component.getInstance("renditionAction");
        hasVisibleRenditionsCache.put(excludedKinds, !renditionAction.getVisibleRenditions(excludedKinds).isEmpty());
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
            EventNames.LOCATION_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetCache() {
        hasVisibleRenditionsCache.clear();
    }

}

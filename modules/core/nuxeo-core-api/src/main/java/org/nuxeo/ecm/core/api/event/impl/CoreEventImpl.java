/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.event.impl;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.CoreEvent;

/**
 * Nuxeo core event implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class CoreEventImpl implements CoreEvent {

    protected final String eventId;

    protected final Object source;

    protected final Map<String, Object> info;

    protected final Date date;

    protected final Principal principal;

    protected final String category;

    protected final String comment;

    // Interesting attributes to make accessible in the eventInfo
    public static final String COMMENT_ATTRIBUTE = "comment";

    public static final String CATEGORY_ATTRIBUTE = "category";

    @SuppressWarnings("unchecked")
    public CoreEventImpl(String eventId, Object source, Map<String, ?> info, Principal principal, String category,
            String comment) {
        date = new Date();
        if (eventId != null) {
            this.eventId = eventId.intern();
        } else {
            this.eventId = null;
        }
        this.source = source;
        if (info == null) {
            this.info = new HashMap<>();
        } else {
            this.info = new HashMap<String, Object>(info);
        }
        this.principal = principal;

        // CB: NXP-2253 - Values passed as parameters will be put into the info
        // map only if the map doesn't contain the corresponding keys.
        if (!this.info.containsKey(COMMENT_ATTRIBUTE)) {
            this.info.put(COMMENT_ATTRIBUTE, comment);
        }
        if (!this.info.containsKey(CATEGORY_ATTRIBUTE)) {
            this.info.put(CATEGORY_ATTRIBUTE, category);
        }

        this.comment = comment;
        this.category = category;
    }

    public boolean isComposite() {
        return false;
    }

    public List<CoreEvent> getNestedEvents() {
        return null;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public Map<String, ?> getInfo() {
        return info;
    }

    @Override
    public Object getSource() {
        return source;
    }

    @Override
    public String getCategory() {
        if (category != null) {
            return category;
        } else {
            Object categoryObj = info.get(CATEGORY_ATTRIBUTE);
            if (categoryObj instanceof String) {
                return (String) categoryObj;
            } else {
                return null;
            }
        }
    }

    @Override
    public String getComment() {
        if (comment != null) {
            return comment;
        } else {
            Object commentObj = info.get(COMMENT_ATTRIBUTE);
            if (commentObj instanceof String) {
                return (String) commentObj;
            } else {
                return null;
            }
        }
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(CoreEventImpl.class.getSimpleName());
        sb.append(" {");
        sb.append(" eventId: ");
        sb.append(eventId);
        sb.append(", source: ");
        sb.append(source);
        sb.append(", info: ");
        sb.append(info);
        sb.append(", date: ");
        sb.append(date);
        sb.append(", principal name: ");
        if (principal != null) {
            sb.append(principal.getName());
        }
        sb.append(", comment: ");
        sb.append(getComment());
        sb.append(", category: ");
        sb.append(getCategory());
        sb.append('}');

        return sb.toString();
    }

}

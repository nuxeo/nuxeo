/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.event.impl;

import java.security.Principal;
import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.CoreEvent;

/**
 * Nuxeo core event implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class CoreEventImpl implements CoreEvent {

    protected final String eventId;

    protected final Object source;

    protected final Map<String, ?> info;

    protected final Date date;

    protected final Principal principal;

    protected final String category;

    protected final String comment;


    public CoreEventImpl(String eventId, Object source, Map<String, ?> info,
            Principal principal, String category, String comment) {
        date = new Date();
        if (eventId != null) {
            this.eventId = eventId.intern();
        } else {
            this.eventId = null;
        }
        this.source = source;
        this.info = info;
        this.principal = principal;
        this.category = category;
        this.comment = comment;
    }

    public String getEventId() {
        return eventId;
    }

    public Map<String, ?> getInfo() {
        return info;
    }

    public Object getSource() {
        return source;
    }

    public String getCategory() {
        return category;
    }

    public String getComment() {
        return comment;
    }

    public Date getDate() {
        return date;
    }

    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(CoreEventImpl.class.getSimpleName());
        buf.append(" {");
        buf.append(" eventId: ");
        buf.append(eventId);
        buf.append(", source: ");
        buf.append(source);
        buf.append(", info: ");
        buf.append(info);
        buf.append(", date: ");
        buf.append(date);
        buf.append(", principal name: ");
        if (principal != null) {
            buf.append(principal.getName());
        }
        buf.append(", comment: ");
        buf.append(comment);
        buf.append(", category: ");
        buf.append(category);
        buf.append('}');

        return buf.toString();
    }

}

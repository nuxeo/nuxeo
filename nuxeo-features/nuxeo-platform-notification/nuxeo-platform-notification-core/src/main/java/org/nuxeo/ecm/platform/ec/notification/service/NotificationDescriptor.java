/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.notification.api.Notification;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
@XObject("notification")
public class NotificationDescriptor implements Notification {

    private static final long serialVersionUID = -5974825427889204458L;

    @XNode("@name")
    protected String name;

    @XNode("@label")
    protected String label; // used for i10n

    @XNode("@channel")
    protected String channel;

    @XNode("@subject")
    protected String subject;

    @XNode("@subjectTemplate")
    protected String subjectTemplate;

    @XNode("@template")
    protected String template;

    /**
     * The mail template name will be dinamycally evaluated from a Mvel exp
     *
     * @since 5.6
     */
    @XNode("@templateExpr")
    protected String templateExpr;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@autoSubscribed")
    protected boolean autoSubscribed = false;

    @XNode("@availableIn")
    protected String availableIn;

    @XNodeList(value = "event", type = ArrayList.class, componentType = NotificationEventDescriptor.class)
    protected List<NotificationEventDescriptor> events;

    public boolean getAutoSubscribed() {
        return autoSubscribed;
    }

    public String getAvailableIn() {
        return availableIn;
    }

    public String getChannel() {
        return channel;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public List<NotificationEventDescriptor> getEvents() {
        return events;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getTemplate() {
        return template;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getTemplateExpr() {
        return templateExpr;
    }

}

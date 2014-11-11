/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
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

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@autoSubscribed")
    protected Boolean autoSubscribed;

    @XNode("@availableIn")
    protected String availableIn;

    @XNodeList(value = "event", type = ArrayList.class, componentType = NotificationEventDescriptor.class)
    protected List<NotificationEventDescriptor> events;

    public Boolean getAutoSubscribed() {
        return autoSubscribed;
    }

    // Not used.
    public void setAutoSubscribed(Boolean autoSubscribed) {
        this.autoSubscribed = autoSubscribed;
    }

    public String getAvailableIn() {
        return availableIn;
    }

    // Not used.
    public void setAvailableIn(String availableIn) {
        this.availableIn = availableIn;
    }

    public String getChannel() {
        return channel;
    }

    // Not used.
    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    // Not used.
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<NotificationEventDescriptor> getEvents() {
        return events;
    }

    // Not used.
    public void setEvents(List<NotificationEventDescriptor> events) {
        this.events = events;
    }

    public String getLabel() {
        return label;
    }

    // Not used.
    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    // Not used.
    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    // Not used.
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplate() {
        return template;
    }

    // Not used.
    public void setTemplate(String template) {
        this.template = template;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    // Not used.
    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

}

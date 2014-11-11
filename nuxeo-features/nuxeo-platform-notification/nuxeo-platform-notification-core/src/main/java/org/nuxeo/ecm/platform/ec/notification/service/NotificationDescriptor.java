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

    @Override
    public boolean getAutoSubscribed() {
        return autoSubscribed;
    }

    @Override
    public String getAvailableIn() {
        return availableIn;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    public List<NotificationEventDescriptor> getEvents() {
        return events;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    @Override
    public String getTemplateExpr(){
        return templateExpr;
    }
}
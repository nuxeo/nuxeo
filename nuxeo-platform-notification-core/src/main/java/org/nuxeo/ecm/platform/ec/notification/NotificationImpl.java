/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ec.notification;

import org.nuxeo.ecm.platform.notification.api.Notification;

/**
 * A notification that a user can subscribe to.
 * <p>
 * It has:
 * <ul>
 * <li>a name
 * <li>a channel - for now only email is supported
 * <li>a subject - as a fixed string or a template to customize subject notifications
 * <li>a template - so the notifications that the user will receive can be
 * customized
 * </ul>
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 *
 */
public class NotificationImpl implements Notification {

    private static final long serialVersionUID = 6550698875484943882L;

    private final String name;

    private final String template;
    
    private final String subjectTemplate;

    private final String subject;

    private final String channel;

    private final Boolean autoSubscribed;

    private final String availableIn;

    private final String label;

    private Boolean enabled;

    /**
     * @param name
     * @param template
     * @param channel
     * @param subjectTemplate
     */
    public NotificationImpl(String name, String template, String channel, String subjectTemplate,
            Boolean autoSubscribed, String subject, String availableIn, String label) {
        this.name = name;
        this.template = template;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.autoSubscribed = autoSubscribed;
        this.subject = subject;
        this.availableIn = availableIn;
        this.label = label;
    }

    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @return the template.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @return the autoSubscribed.
     */
    public Boolean getAutoSubscribed() {
        return autoSubscribed;
    }

    /**
     * @return the subject.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @return the subject template.
     */
    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    /**
     * @return the availableIn.
     */
    public String getAvailableIn() {
        return availableIn;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NotificationImpl) {
            NotificationImpl other = (NotificationImpl) obj;
            return name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}

/*
 * (C) Copyright 2007-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * <li>a template - so the notifications that the user will receive can be customized
 * </ul>
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
public class NotificationImpl implements Notification {

    private static final long serialVersionUID = 6550698875484943882L;

    private final String name;

    private final String template;

    private final String subjectTemplate;

    private final String subject;

    private final String channel;

    private final boolean autoSubscribed;

    private final String availableIn;

    private final String label;

    private boolean enabled;

    private String templateExpr;

    public NotificationImpl(String name, String template, String channel, String subjectTemplate,
            boolean autoSubscribed, String subject, String availableIn, String label) {
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
     * @since 5.6
     */
    public void setTemplateExpr(String templateExpr) {
        this.templateExpr = templateExpr;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public boolean getAutoSubscribed() {
        return autoSubscribed;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    @Override
    public String getAvailableIn() {
        return availableIn;
    }

    @Override
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

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getTemplateExpr() {
        return templateExpr;
    }

}

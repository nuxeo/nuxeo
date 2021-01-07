/*
 * (C) Copyright 2007-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Narcis Paslaru
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.notification.api.Notification;

/**
 * Descriptor for a notification.
 */
@XObject("notification")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class NotificationDescriptor implements Notification {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@label")
    protected String label; // used for l10n

    @XNode("@channel")
    protected String channel;

    @XNode("@subject")
    protected String subject;

    @XNode("@subjectTemplate")
    protected String subjectTemplate;

    @XNode("@template")
    protected String template;

    /**
     * The mail template name will be dynamically evaluated from a Mvel expression.
     *
     * @since 5.6
     */
    @XNode("@templateExpr")
    protected String templateExpr;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean enabled;

    @XNode("@autoSubscribed")
    protected boolean autoSubscribed = false;

    @XNode("@availableIn")
    protected String availableIn;

    @XNodeList(value = "event@name", type = HashSet.class, componentType = String.class)
    protected HashSet<String> events;

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

    /** @since 11.5 **/
    public Set<String> getEvents() {
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
    public String getTemplateExpr() {
        return templateExpr;
    }

}

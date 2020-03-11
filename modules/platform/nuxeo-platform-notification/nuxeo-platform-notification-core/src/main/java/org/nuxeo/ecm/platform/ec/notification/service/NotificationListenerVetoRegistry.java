/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.ec.notification.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link org.nuxeo.ecm.platform.ec.notification.service.NotificationListenerVetoDescriptor} elements
 *
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 * @since 5.6
 */
public class NotificationListenerVetoRegistry extends ContributionFragmentRegistry<NotificationListenerVetoDescriptor> {

    private static final Log log = LogFactory.getLog(NotificationListenerVetoRegistry.class);

    private Map<String, NotificationListenerVeto> vetos;

    public NotificationListenerVetoRegistry() {
        super();
        vetos = new HashMap<>();
    }

    @Override
    public NotificationListenerVetoDescriptor clone(NotificationListenerVetoDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void contributionRemoved(String id, NotificationListenerVetoDescriptor contrib) {
        vetos.remove(id);
    }

    @Override
    public void contributionUpdated(String id, NotificationListenerVetoDescriptor contrib,
            NotificationListenerVetoDescriptor newOrigContrib) {
        if (contrib.isRemove()) {
            contributionRemoved(id, contrib);
        } else {
            try {
                vetos.put(id, contrib.getNotificationVeto().getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                log.error(e);
            }
        }
    }

    @Override
    public String getContributionId(NotificationListenerVetoDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void merge(NotificationListenerVetoDescriptor arg0, NotificationListenerVetoDescriptor arg1) {
        throw new UnsupportedOperationException();
    }

    public NotificationListenerVeto getVeto(String id) {
        return vetos.get(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    public Collection<NotificationListenerVeto> getVetos() {
        return vetos.values();
    }

    public void clear() {
        vetos.clear();
        contribs.clear();
    }

}

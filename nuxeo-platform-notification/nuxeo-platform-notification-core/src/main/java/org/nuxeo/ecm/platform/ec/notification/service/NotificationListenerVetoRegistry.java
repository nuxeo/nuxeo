/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Registry for
 * {@link org.nuxeo.ecm.platform.ec.notification.service.NotificationListenerVetoDescriptor}
 * elements
 *
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 * @since 5.6
 */
public class NotificationListenerVetoRegistry extends
        ContributionFragmentRegistry<NotificationListenerVetoDescriptor> {

    private static final Log log = LogFactory.getLog(NotificationListenerVetoRegistry.class);

    private Map<String, NotificationListenerVeto> vetos;

    public NotificationListenerVetoRegistry() {
        super();
        vetos = new HashMap<String, NotificationListenerVeto>();
    }

    @Override
    public NotificationListenerVetoDescriptor clone(
            NotificationListenerVetoDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void contributionRemoved(String id,
            NotificationListenerVetoDescriptor contrib) {
        vetos.remove(id);
    }

    @Override
    public void contributionUpdated(String id,
            NotificationListenerVetoDescriptor contrib,
            NotificationListenerVetoDescriptor newOrigContrib) {
        if (contrib.isRemove()) {
            contributionRemoved(id, contrib);
        } else {
            try {
                vetos.put(id, contrib.getNotificationVeto().newInstance());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public String getContributionId(NotificationListenerVetoDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void merge(NotificationListenerVetoDescriptor arg0,
            NotificationListenerVetoDescriptor arg1) {
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

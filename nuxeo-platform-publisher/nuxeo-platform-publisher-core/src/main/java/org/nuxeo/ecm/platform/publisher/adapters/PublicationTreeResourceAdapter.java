/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.adapters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class PublicationTreeResourceAdapter extends AbstractResourceAdapter
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublicationTreeResourceAdapter.class);

    @Override
    public Class<?> getKlass() {
        return PublicationTree.class;
    }

    @Override
    public Resource getResource(Serializable object,
            Map<String, Serializable> stringSerializableMap) {
        PublicationTree tree = (PublicationTree) object;
        String localName = tree.getConfigName();
        return new QNameResourceImpl(namespace, localName);
    }

    @Override
    public Serializable getResourceRepresentation(Resource resource,
            Map<String, Serializable> context) {
        Serializable object = null;
        if (resource.isQNameResource()) {
            CoreSession session = null;
            Serializable givenSessionId = context.get(CORE_SESSION_ID_CONTEXT_KEY);
            if (givenSessionId instanceof String) {
                session = CoreInstance.getInstance().getSession(
                        (String) givenSessionId);
            }
            if (session != null) {
                String localName = ((QNameResource) resource).getLocalName();
                try {
                    PublisherService publisherService = Framework.getService(PublisherService.class);
                    object = publisherService.getPublicationTree(localName, session, null);
                } catch (Exception e) {
                    log.error("Unable to get PublicationTree for name: " + localName, e);
                }
            }
        }

        return object;
    }

}

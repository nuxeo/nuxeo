/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Marwane Kalam-Alami
 */
package org.nuxeo.ecm.platform.publisher.web;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

public class DummyPublishActions extends AbstractPublishActions {

    public DummyPublishActions(CoreSession documentManager,
            ResourcesAccessor resourcesAccessor) {
        this.documentManager = documentManager;
        this.messages = resourcesAccessor.getMessages();
    }

}

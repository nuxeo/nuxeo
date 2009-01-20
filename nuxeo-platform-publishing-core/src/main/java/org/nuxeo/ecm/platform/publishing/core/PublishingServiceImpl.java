/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing.core;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.publishing.PublishingService;

/**
 * @author arussel
 *
 */
public class PublishingServiceImpl implements PublishingService {
    private final Map<String, PublishableLocationWrapper> locationWrappers;

    private final Map<String, PublishableDocumentWrapper> documentWrappers;

    private final List<Publisher> publishers;

    public PublishingServiceImpl(
            Map<String, PublishableLocationWrapper> locationWrappers,
            Map<String, PublishableDocumentWrapper> documentWrappers,
            List<Publisher> publishers) {
        this.locationWrappers = locationWrappers;
        this.documentWrappers = documentWrappers;
        this.publishers = publishers;
    }

    public List<PublishableLocation> getPublishableLocation(Object document,
            NuxeoPrincipal principal) {
        return null;
    }

    public boolean isPublishableDocument(Object object) {
        return documentWrappers.get(object.getClass().getName()) != null;
    }

    public boolean isPublishableLocation(Object object) {
        return locationWrappers.get(object.getClass().getName()) != null;
    }

    public boolean isPublished(Object document, Object location,
            NuxeoPrincipal principal) {
        PublishableLocation publishableLocation = getDocumentLocation(location);
        for (Publisher publisher : publishers) {
        }
        return false;
    }

    private PublishableLocation getDocumentLocation(Object location) {
        PublishableLocationWrapper wrapper = locationWrappers.get(location.getClass().getName());
        return null;
    }

    public void submitToPublication(List<Object> documentToPublish,
            List<Object> locationToPublishTo, NuxeoPrincipal principal) {
    }

    public PublishableDocument wrapPublishableDocument(String className) {
        return null;
    }

    public PublishableLocation wrapPublishableLocation(String className) {
        return null;
    }

}

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

package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.runtime.api.Framework;

/**
 * This helper is now a simple wrapper around the service API. This class is now
 * deprecated, please use {@link PublisherService}.getRootSectionFinder
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Deprecated
public class RootSectionsFinderHelper {

    private RootSectionsFinderHelper() {
        // Helper class
    }

    public static RootSectionFinder getRootSectionsFinder(
            CoreSession coreSession) {
        return Framework.getLocalService(PublisherService.class).getRootSectionFinder(
                coreSession);
    }

}

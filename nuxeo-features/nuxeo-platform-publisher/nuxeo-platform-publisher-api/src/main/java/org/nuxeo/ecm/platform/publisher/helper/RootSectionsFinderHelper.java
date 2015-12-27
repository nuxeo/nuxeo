/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.runtime.api.Framework;

/**
 * This helper is now a simple wrapper around the service API. This class is now deprecated, please use
 * {@link PublisherService}.getRootSectionFinder
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Deprecated
public class RootSectionsFinderHelper {

    private RootSectionsFinderHelper() {
        // Helper class
    }

    public static RootSectionFinder getRootSectionsFinder(CoreSession coreSession) {
        return Framework.getLocalService(PublisherService.class).getRootSectionFinder(coreSession);
    }

}

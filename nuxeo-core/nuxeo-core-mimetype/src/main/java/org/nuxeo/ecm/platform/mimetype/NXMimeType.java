/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: NXMimeType.java 21336 2007-06-25 15:20:32Z janguenot $
 */

package org.nuxeo.ecm.platform.mimetype;

import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.api.Framework;

/**
 * NXMimeType helper.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @deprecated since 7.3.
 */
@Deprecated
public final class NXMimeType {

    private NXMimeType() {
    }

    public static MimetypeRegistryService getMimetypeRegistryService() {
        return (MimetypeRegistryService) Framework.getRuntime().getComponent(MimetypeRegistryService.NAME);
    }
}

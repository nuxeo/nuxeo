/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Handler called after the content creation done by the {@link ContentTemplateService}.
 * <p>
 * The registered handlers are always called when the server starts even if no content creation is done.
 * <p>
 * Useful for packages deployed on an existing Nuxeo that need a default documents structure.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface PostContentCreationHandler {

    /**
     * Executes this handler with a system {@code session}.
     */
    void execute(CoreSession session);

}

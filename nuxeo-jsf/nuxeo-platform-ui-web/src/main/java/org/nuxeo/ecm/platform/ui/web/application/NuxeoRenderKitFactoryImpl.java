/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */

package org.nuxeo.ecm.platform.ui.web.application;

import com.sun.faces.renderkit.RenderKitFactoryImpl;

/**
 * @since 6.0
 */
public class NuxeoRenderKitFactoryImpl extends RenderKitFactoryImpl {

    public NuxeoRenderKitFactoryImpl() {
        super();
        // Erase default faces RederKitImpl with Nuxeo custom one to use our own
        // ServerSideStateHelper
        addRenderKit(HTML_BASIC_RENDER_KIT, new NuxeoRenderKitImpl());
    }

}

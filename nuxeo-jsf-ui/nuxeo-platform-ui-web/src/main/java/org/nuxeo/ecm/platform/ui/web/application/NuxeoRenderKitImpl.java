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

import javax.faces.render.ResponseStateManager;

import com.sun.faces.renderkit.RenderKitImpl;

/**
 * @since 6.0
 */
public class NuxeoRenderKitImpl extends RenderKitImpl {

    private ResponseStateManager responseStateManager = new NuxeoResponseStateManagerImpl();

    @Override
    public synchronized ResponseStateManager getResponseStateManager() {
        if (responseStateManager == null) {
            responseStateManager = new NuxeoResponseStateManagerImpl();
        }
        return responseStateManager;
    }

}

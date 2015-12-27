/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service.lazy;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.platform.rendition.lazy.AbstractLazyCachableRenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Very basic implementation of a Lazy Rendition : here we simply wait !
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class LazyRenditionProviderSample extends AbstractLazyCachableRenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition definition) {
        return true;
    }

    @Override
    protected Work getRenditionWork(String key, DocumentModel doc, RenditionDefinition def) {
        return new LazyRenditionWorkSample(key, doc, def);
    }

}

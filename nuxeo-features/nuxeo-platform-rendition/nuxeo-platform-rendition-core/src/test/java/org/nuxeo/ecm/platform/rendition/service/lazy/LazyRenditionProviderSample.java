/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    /**
     * @deprecated since 7.10-HF01
     */
    @Override
    @Deprecated
    protected boolean perUserRendition() {
        return false;
    }

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition definition) {
        return true;
    }

    @Override
    protected Work getRenditionWork(String key, DocumentModel doc, RenditionDefinition def) {
        return new LazyRenditionWorkSample(key, doc, def);
    }

}

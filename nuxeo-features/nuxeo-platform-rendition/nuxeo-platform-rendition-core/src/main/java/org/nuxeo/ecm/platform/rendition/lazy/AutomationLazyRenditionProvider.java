/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.lazy;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.platform.rendition.extension.AutomationRenderer;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class AutomationLazyRenditionProvider extends AbstractLazyCachableRenditionProvider {

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        return AutomationRenderer.isRenditionAvailable(doc, def);
    }

    /**
     * @deprecated since 7.10-HF01
     */
    @Deprecated
    @Override
    protected boolean perUserRendition() {
        return false;
    }

    @Override
    protected Work getRenditionWork(String key, DocumentModel doc, RenditionDefinition def) {
        return new AutomationRenditionBuilder(key, doc, def);
    }

}

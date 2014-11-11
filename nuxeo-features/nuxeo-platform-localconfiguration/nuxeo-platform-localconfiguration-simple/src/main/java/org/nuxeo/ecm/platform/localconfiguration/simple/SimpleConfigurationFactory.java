/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.localconfiguration.simple;

import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * Factory creating the {@code SimpleConfigurationAdapter} adapter if the
 * document has the {@code SimpleConfiguration} facet.
 *
 * @see SimpleConfigurationAdapter
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class SimpleConfigurationFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (doc.hasFacet(SIMPLE_CONFIGURATION_FACET)) {
            return new SimpleConfigurationAdapter(doc);
        }
        return null;
    }

}

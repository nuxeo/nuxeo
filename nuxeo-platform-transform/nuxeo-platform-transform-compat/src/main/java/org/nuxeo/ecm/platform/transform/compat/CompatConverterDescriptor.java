/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.compat;

import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.service.extensions.PluginExtension;

public class CompatConverterDescriptor extends ConverterDescriptor {

    private static final long serialVersionUID = 1L;

    protected Class<Plugin> pluginClass;

    public CompatConverterDescriptor(PluginExtension pluginDesciptor, Class<Plugin> pluginClass) {
        destinationMimeType = pluginDesciptor.getDestinationMimeType();
        sourceMimeTypes = pluginDesciptor.getSourceMimeTypes();
        this.pluginClass = pluginClass;
        converterName = pluginDesciptor.getName();
    }

    @Override
    public void initConverter() throws Exception {
        if (instance == null) {
            Plugin plugin = pluginClass.newInstance();
            instance = new ConverterWrappingPlugin(plugin);
            instance.init(this);
        }
    }

    @Override
    public Converter getConverterInstance() {
        try {
            initConverter();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return instance;
    }

}

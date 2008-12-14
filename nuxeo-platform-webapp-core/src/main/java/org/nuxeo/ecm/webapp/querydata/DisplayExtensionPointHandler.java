/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.querydata;

import org.nuxeo.runtime.model.Extension;

@Deprecated
public class DisplayExtensionPointHandler extends NXQueryDataExtensionPointHandler {

    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();

        log.info("unregisterExtension()....................................");

        for (Object contrib : contribs) {
            DisplayPluginExtension pluginExtension = (DisplayPluginExtension) contrib;

            try {
                unregisterOne(pluginExtension, extension);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public void registerExtension(Extension extension) {
        log.info("RegisterExtension.....................");

        Object[] contribs = extension.getContributions();

        for (Object contrib : contribs) {
            DisplayPluginExtension pluginExtension = (DisplayPluginExtension) contrib;
            try {
                registerOne(pluginExtension, extension);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private void registerOne(DisplayPluginExtension pluginExtension,
            Extension extension) throws Exception {
        pluginExtension.setColumnsChain();
        QueryDataServiceCommon queryDataService = getNXQueryData();
        if (queryDataService != null) {
            getNXQueryData().registerDisplayPlugin(pluginExtension.getName(), pluginExtension);
        } else {
            log.error("No QueryDataServiceCommon service found impossible to register plugin");
        }
    }

    private void unregisterOne(DisplayPluginExtension pluginExtension,
            Extension extension) throws Exception {
        String name = pluginExtension.getName();
        getNXQueryData().unregisterDisplayPlugin(name);
    }

}

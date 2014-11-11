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
public class QueryExtensionPointHandler extends NXQueryDataExtensionPointHandler {

    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();

        log.info("unregisterExtension()....................................");

        for (Object contrib : contribs) {
            QueryPluginExtension pluginExtension = (QueryPluginExtension) contrib;

            try {
                unregisterOne(pluginExtension, extension);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void registerExtension(Extension extension) {

        log.info("RegisterExtension.....................");

        Object[] contribs = extension.getContributions();

        for (Object contrib : contribs) {
            QueryPluginExtension pluginExtension = (QueryPluginExtension) contrib;
            try {
                registerOne(pluginExtension, extension);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void registerOne(QueryPluginExtension pluginExtension,
            Extension extension) throws Exception {

        try {
            QueryDataServiceCommon queryDataService = getNXQueryData();
            if (queryDataService != null) {
                getNXQueryData().registerQueryPlugin(pluginExtension.getName(), pluginExtension);
            } else {
                log.error("No QueryDataServiceCommon service found impossible to register plugin");
            }

        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    private void unregisterOne(QueryPluginExtension pluginExtension,
            Extension extension) throws Exception {
        String name = pluginExtension.getName();
        getNXQueryData().unregisterQueryPlugin(name);
    }
}

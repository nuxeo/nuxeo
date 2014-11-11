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
 * $Id: JOOoConverterPlugin.java 22290 2007-07-10 16:37:37Z janguenot $
 */

package org.nuxeo.ecm.platform.transform.plugin.joooconverter.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

/**
 * JOOoConverterPlugin for TransformServiceCommon.
 * <p>
 * Defines a plugin that can be registred as a TransformServiceCommon plugin
 * extension. This plugin will request an distant OpenOffice server for
 * conversion. It leverages joooconverter the OpenOffice.org java lib.
 * <p>
 * As specific options to be given to this plugin :
 * <ul>
 * <li>ooo_host_url</li>
 * <li>ooo_host_port</li>
 * </ul>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface JOOoConverterPlugin extends Plugin {

    /**
     * Returns the remote OOo server's URL.
     *
     * @return string the server URL
     */
    String getOOoHostURL();

    /**
     * Returns the port on which the remote OOo server is running.
     *
     * @return int the port number
     */
    int getOOoHostPort();

    /**
     * Returns a remote OOo connection.
     *
     * @return
     */
    OpenOfficeConnection getOOoConnection();

    /**
     * Returns the actual OOo converter object linked to the server.
     *
     * @return
     */
    OpenOfficeDocumentConverter getOOoDocumentConverter() throws Exception;

    /**
     * Close the actual open office connection the plugin maintains.
     */
    void releaseOOoConnection();

    List<TransformDocument> transform(
            Map<String, Serializable> options, TransformDocument... sources)
            throws Exception;

}

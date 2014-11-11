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

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

@Deprecated
public class NXQueryDataExtensionPointHandler {

    protected static Log log = LogFactory.getLog(NXQueryDataExtensionPointHandler.class);

    /**
     * Get the NXTransformBean EJB remotely performing a JNDI lookup.
     *
     * @return TransformServiceCommon bean
     */
    protected QueryDataServiceCommon getNXQueryData() throws NamingException {
        return (QueryDataServiceCommon) Framework.getRuntime().getComponent(QueryDataService.NAME);
    }

}

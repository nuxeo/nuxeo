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

import java.io.Serializable;

import org.nuxeo.runtime.api.Framework;

@Deprecated
public final class NXQueryData implements Serializable {

    private static final long serialVersionUID = 722077308657454263L;

    private static QueryDataService queryDataService;

    private NXQueryData() {
    }

    public static QueryDataService getQueryDataService() {
        if (queryDataService == null) {
            queryDataService = (QueryDataService) Framework.getRuntime().getComponent(QueryDataService.NAME);
        }
        return queryDataService;
    }
}

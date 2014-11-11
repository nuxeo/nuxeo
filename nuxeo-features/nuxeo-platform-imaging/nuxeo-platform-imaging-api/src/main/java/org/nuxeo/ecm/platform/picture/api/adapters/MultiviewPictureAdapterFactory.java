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

package org.nuxeo.ecm.platform.picture.api.adapters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class MultiviewPictureAdapterFactory implements DocumentAdapterFactory {

    private static final Log log = LogFactory.getLog(MultiviewPictureAdapterFactory.class);

    @Override
    public Object getAdapter(DocumentModel doc, Class itf) {
        if (doc.hasFacet("MultiviewPicture")) {
            try {
                return new MultiviewPictureAdapter(doc);
            } catch (ClientException e) {
                log.error("Unable to build MultiviewPictureAdapter", e);
                return null;
            }
        } else {
            return null;
        }
    }

}

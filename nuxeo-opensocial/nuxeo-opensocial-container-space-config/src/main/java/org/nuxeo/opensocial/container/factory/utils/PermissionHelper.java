/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.factory.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

public class PermissionHelper {

  private static final Log log = LogFactory.getLog(PermissionHelper.class);

  public static final String PERMISSION_WRITE = "WRITE";

  public static boolean canWrite(String docId, CoreSession session) {

    try {
      DocumentModel doc = session.getDocument(new IdRef(docId));
      boolean hasPermission = session.hasPermission(doc.getRef(),
          PERMISSION_WRITE);

      return hasPermission;
    } catch (ClientException e) {
      log.error(e);
      return false;
    }

  }
}

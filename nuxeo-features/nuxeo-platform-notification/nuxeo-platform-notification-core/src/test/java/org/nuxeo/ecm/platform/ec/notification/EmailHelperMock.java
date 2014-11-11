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

package org.nuxeo.ecm.platform.ec.notification;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;

public class EmailHelperMock extends EmailHelper {
    public Log log = LogFactory.getLog(this.getClass());
    public int compteur = 0;

    @Override
    public void sendmail(Map<String, Object> mail) throws Exception {
        compteur++;
        log.info("Faking send mail : ");
        for (String key : mail.keySet()) {
               log.info(key+" : "+mail.get(key));
        }

    }

    public int getCompteur() {
        return compteur;
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public void sendmail(Map<String, Object> mail) {
        compteur++;
        log.info("Faking send mail : ");
        for (String key : mail.keySet()) {
            log.info(key + " : " + mail.get(key));
        }

    }

    public int getCompteur() {
        return compteur;
    }

}

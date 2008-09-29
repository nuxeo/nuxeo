/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.repository.jcr.versioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Versioning extends DefaultComponent {

    private static final Log log = LogFactory.getLog(Versioning.class);

    private static String klazzName;

    private static VersioningService service;

    public static VersioningService getService() {
        if (service == null) {
            if (klazzName != null) {
                // CB: NXP-2679 - Allow lazy register in versioning service
                try {
                    Class implemObj = Class.forName(klazzName);
                    service = (VersioningService) implemObj.newInstance();
                } catch (ClassNotFoundException e) {
                    log.warn("Cannot load implementor class: " + klazzName, e);
                } catch (InstantiationException e) {
                    log.warn("Cannot instantiate implementor class: " + klazzName, e);
                } catch (IllegalAccessException e) {
                    log.error("Cannot create implementor class: " + klazzName, e);
                }
            } else {
                service = new JCRVersioningService();
            }
        }

        return service;
    }

    @Override
    public void activate(ComponentContext context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String klass = (String) context.getPropertyValue("versioningService");
        if (klass != null) {
            klazzName = klass;
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        service = null;
        klazzName = null;
    }

}

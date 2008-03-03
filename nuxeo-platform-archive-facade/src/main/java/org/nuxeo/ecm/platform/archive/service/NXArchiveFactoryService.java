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

package org.nuxeo.ecm.platform.archive.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecordFactory;
import org.nuxeo.ecm.platform.archive.service.extension.ArchiveRecordFactoryDescriptor;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class NXArchiveFactoryService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.archive.service.NXArchiveFactoryService");

    private static final String FACTORY_EXT_POINT = "archiveRecordFactory";

    private static final Log log = LogFactory.getLog(NXArchiveFactoryService.class);

    // The default
    private static Class<ArchiveRecordFactory> archiveRecordFactoryKlass;

    @Override
    public void registerExtension(Extension extension) {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(FACTORY_EXT_POINT)) {
                for (Object contribution : contributions) {
                    ArchiveRecordFactoryDescriptor desc = (ArchiveRecordFactoryDescriptor) contribution;
                    log.info("Register a new archive record factory");
                    archiveRecordFactoryKlass = desc.getKlass();
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(FACTORY_EXT_POINT)) {
                for (Object contribution : contributions) {
                    ArchiveRecordFactoryDescriptor desc = (ArchiveRecordFactoryDescriptor) contribution;
                    if (archiveRecordFactoryKlass == desc.getKlass()) {
                        log.info("UnRegister archive record factory");
                        archiveRecordFactoryKlass = null;
                    }
                }
            }
        }
    }

    public static Class<ArchiveRecordFactory> getArchiveRecordFactoryKlass() {
        return archiveRecordFactoryKlass;
    }

    public static ArchiveRecordFactory getArchiveRecordFactory() {
        Class<ArchiveRecordFactory> klass = archiveRecordFactoryKlass;
        ArchiveRecordFactory factory = null;
        if (klass != null) {
            try {
                factory = klass.newInstance();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return factory;
    }

}

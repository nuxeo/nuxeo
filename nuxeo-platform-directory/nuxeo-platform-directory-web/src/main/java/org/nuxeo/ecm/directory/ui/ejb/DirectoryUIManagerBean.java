/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DirectoryUIManagerBean.java 57103 2008-08-23 00:40:52Z atchertchian $
 */

package org.nuxeo.ecm.directory.ui.ejb;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Directory ui manager bean
 */
@Stateless
@Local(DirectoryUIManagerLocal.class)
@Remote(DirectoryUIManager.class)
public class DirectoryUIManagerBean implements DirectoryUIManagerLocal {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DirectoryUIManagerBean.class);

    private transient DirectoryUIManager service;

    @PostActivate
    @PostConstruct
    public void initialize() {
        try {
            // get Runtime service
            service = Framework.getLocalService(DirectoryUIManager.class);
        } catch (Exception e) {
            log.error("Could not get distribution service", e);
        }
    }

    @Remove
    public void remove() {
        service = null;
    }

    public DirectoryUI getDirectoryInfo(String directoryName)
            throws ClientException {
        return service.getDirectoryInfo(directoryName);
    }

    public List<String> getDirectoryNames() throws ClientException {
        return service.getDirectoryNames();
    }

}

package org.nuxeo.ecm.platform.lock.api;

import java.net.URI;
import java.util.List;

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
 *     matic
 */

/**
 * Retrieve life lock informations about resources
 *
 * @author matic
 *
 */
public interface LockReader {

    /**
     * Provide life locks information about all resources
     *
     * @return
     * @throws InterruptedException
     */
    List<LockInfo> getInfos() throws InterruptedException;

    /**
     * Provide live lock information about a single resource.
     *
     * @param resource
     * @return
     * @throws InterruptedException
     */
    LockInfo getInfo(URI resource) throws NoSuchLockException,
            InterruptedException;


}

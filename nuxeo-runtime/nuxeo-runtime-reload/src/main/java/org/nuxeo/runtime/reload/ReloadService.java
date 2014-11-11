/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.reload;

import java.io.File;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ReloadService {

    /**
     * @since 5.5
     */
    String deployBundle(File file, boolean reloadResources) throws Exception;
    
    String deployBundle(File file) throws Exception;

    /**
     * @since 5.5
     */
   void undeployBundle(String name) throws Exception;

    void reloadRepository() throws Exception;

    void flushJaasCache() throws Exception;

    void reloadProperties() throws Exception;
    
    /**
     * @since 5.5
     */
    void reloadSeamComponents() throws Exception;

    void addJar(File file) throws Exception;

    void removeJar(File file) throws Exception;

    /**
     * Sends a flush event so that listeners can be notified that a reload has
     * been done.
     * @throws Exception 
     *
     * @since 5.5
     */
    void flush() throws Exception;

    /**
     * Copy web resources in nuxeo WAR
     * 
     * @since 5.5
     */
    void installWebResources(File file) throws Exception;

    /**
     * @since 5.5
     */
    void reload() throws Exception;
}

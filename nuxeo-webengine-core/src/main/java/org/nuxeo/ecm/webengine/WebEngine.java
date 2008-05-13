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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.webengine.scripting.Scripting;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebEngine {

    Scripting getScripting();

    File getRootDirectory();

    void registerObject(ObjectDescriptor obj);

    void unregisterObject(ObjectDescriptor obj);

    String getTypeBinding(String type);

    void registerBinding(String type, String objectId);

    void unregisterBinding(String type);

    List<ObjectDescriptor> getRegisteredObjects();

    List<ObjectDescriptor> getPendingObjects();

    List<ObjectDescriptor> getResolvedObjects();

    ObjectDescriptor getObject(String id);

    boolean isObjectResolved(String id);

    void reset();

    Map<String, Object> getEnvironment();

    WebApplication getApplication(String name);

    public void registerApplication(WebApplicationDescriptor desc) throws WebException;

    public void unregisterApplication(String id);

    public WebApplication[]  getApplications();

}

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

package org.nuxeo.ecm.platform.gwt.debug;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Debug {

    static final String REDIRECT_HOST = "localhost";
    static final int REDIRECT_PORT = 8080;
    static final String REDIRECT_PREFIX = "/redirect";
    static final String REDIRECT_PATTERN = REDIRECT_PREFIX+"/(.*)";
    static final String REDIRECT_REPLACEMENT = "/$1";
    static final boolean REDIRECT_TRACE = true;
    static final boolean REDIRECT_TRACE_CONTENT = false;

    /*
     * Add this code in tomcat/webapps/ROOT/WEB-INF/web.xml
     */

     /*
<servlet>
    <servlet-name>redirect</servlet-name>
    <servlet-class>org.nuxeo.ecm.webengine.gwt.debug.RedirectServlet</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>redirect</servlet-name>
    <url-pattern>/redirect/*</url-pattern>
</servlet-mapping>

    */
}

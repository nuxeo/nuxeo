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

package org.nuxeo.ecm.webengine.atom;

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.DefaultProvider;
import org.apache.abdera.protocol.server.impl.SimpleTarget;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.nuxeo.common.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoProvider extends DefaultProvider {

    /**
     *
     */
    public NuxeoProvider() {
    }

    public NuxeoProvider(String base) {
        super(base);
    }

    /**
     * There is a pb. in version 0.4.0 - servlet path is taken into account when resolving which is wrong
     */
    @Override
    public Target resolveTarget(RequestContext context) {
        HttpServletRequest req = ((ServletRequestContext)context).getRequest();
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.length() == 0) { // service
            return new SimpleTarget(TargetType.TYPE_SERVICE, context);
        } else { // collection or entry
            Path path = new Path(pathInfo);
            int len = path.segmentCount();
            if (len == 1) { // collection
                context.setAttribute("PATH_INFO", path);
                return new SimpleTarget(TargetType.TYPE_COLLECTION, context);
            } else { // entry
                context.setAttribute("PATH_INFO", path);
                return new SimpleTarget(TargetType.TYPE_ENTRY, context);
            }
            //TODO categories
        }
    }



}

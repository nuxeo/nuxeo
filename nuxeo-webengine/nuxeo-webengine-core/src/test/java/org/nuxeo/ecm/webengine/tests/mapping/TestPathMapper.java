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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.tests.mapping;

import junit.framework.TestCase;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.PathInfo;
import org.nuxeo.ecm.webengine.mapping.MappingDescriptor;
import org.nuxeo.ecm.webengine.mapping.PathMapper;

public class TestPathMapper extends TestCase {

    // FIXME
    public void testPathMapper() {
        PathMapper mapper = new PathMapper();
        MappingDescriptor mdef = new MappingDescriptor();
        mdef.setPattern("(?name1:.*)/demo/(?name2:[^/]+)");
        mdef.setScript("$1 $2 $path");
        mapper.addMapping(mdef);
        PathInfo mapping = mapper.rewrite(new PathInfo(new Path("/wiki/a/b/c/demo/index.view")));
        //System.out.println(mapping.getScript());
        assertEquals("/wiki/a/b/c index.view /wiki/a/b/c/demo/index.view", mapping.getScript());

        // -----------------

        mapper = new PathMapper();
        mdef = new MappingDescriptor();
        mdef.setPattern("(?name1:.*)/demo/(?name2:[^/]+)");
        mdef.setScript("$name1 $name2 $path");
        mapper.addMapping(mdef);
        mapping = mapper.rewrite(new PathInfo(new Path("/wiki/a/b/c/demo/index.view")));
        //System.out.println(mapping.getScript());
        assertEquals("/wiki/a/b/c index.view /wiki/a/b/c/demo/index.view", mapping.getScript());
    }

}

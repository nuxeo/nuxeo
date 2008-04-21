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

package org.nuxeo.ecm.platform.site.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Match patterns of the type:
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathMapper {

    List<MappingDescriptor> entries = null;

    ConcurrentMap<String, Mapping> cache = null;

    public PathMapper() {
        entries = new ArrayList<MappingDescriptor>();
        cache = new ConcurrentHashMap<String, Mapping>();
    }

    public void addMapping(MappingDescriptor mdef) {
        entries.add(mdef);
    }

    public void clearMappings() {
        entries.clear();
    }

    public final Mapping getMapping(String pathInfo) {
        for (MappingDescriptor entry: entries) {
            Mapping mapping = entry.match(pathInfo);
            if (mapping != null) {
                return mapping;
            }
        }
//        Mapping mapping = new Mapping();
//        mapping.traversalPath =
        return null;
    }


    public static void main(String[] args) {
        PathMapper mapper = new PathMapper();
        MappingDescriptor mdef = new MappingDescriptor();
        mdef.setPattern("**/demo/*");
        mdef.setScript("$1 $2");
        //mdef.setPattern("/wiki/**/d/*/*");
        //mdef.setScript("$1 $2 $3: $0");
        mapper.addMapping(mdef);

        //Mapping mapping = mapper.getMapping("/wiki/a/b/c/d/e/index.view");
        Mapping mapping = mapper.getMapping("/wiki/a/b/c/demo/index.view");
        mapping.mdef = mdef;
        System.out.println(mapping.getScript());
        //System.out.println(mapper.getMapping(new SitePath("wiki/a/b/d/e/index.view", "Note")).script);

        PathPattern pat = new PathPattern("/wiki(?:/(.*))?/d/([^/]+)/([^/]+)");
        //pat = new PathPattern2("wiki(?:/(.*))?/d/([^/]+)/([^/]+)");
        //pat = new PathPattern2("(?:(.*)/)?d/([^/]+)/([^/]+)");
        pat = new PathPattern("/wiki/**/d/*/*");
        mapping = pat.match("/wiki/a/b/c/d/e/index.view");
        mapping.mdef = mdef;
        System.out.println(mapping.getScript());

        double s = System.currentTimeMillis();
        for (int i=0; i<10000; i++) {
            mapper.getMapping("/wiki/a/b/c/d/e/index.view");
        }
        System.out.println(">>>> REGEX: "+((System.currentTimeMillis()-s)/1000));


//        mapper.addMapping("/wiki/a/index_Note.view", "view");
//        mapper.addMapping("/wiki/a/*_Note.view", "view");
//        mapper.addMapping("/wiki/a/*_*.view", "view");
//        mapper.addMapping("/wiki/a/*_*.*", "view");
//        mapper.addMapping("/wiki/a/b/c/*_*.*", "test/$1_$2.$3");
//        mapper.addMapping("/wiki/a/*/c/*_*.*", "$1/$2_$3.$4");
//        mapper.addMapping("/wiki/a/**/*_*.*", "$1/$2_$3.$4");
//        mapper.addMapping("/wiki/a/**/c/*_*.*", "$1/$2_$3.$4");
//
//        double s = System.currentTimeMillis();
//        System.out.println("===========");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view2", "Note")));
//
//        System.out.println("===========");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index2.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index2.view2", "Note")));
//
//        System.out.println("===========");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index2.view", "Note2")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view2", "Note")));
//
//        System.out.println("===========");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index2.view", "Note2")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view2", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/b/index.view2", "Note")));
//
//        System.out.println("=========== path matching");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/b/c/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/b/index.view", "Note")));
//
//        System.out.println("=========== path matching");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/b/c/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/Z/c/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/Z/Y/c/index.view", "Note")));
//
//        System.out.println("=========== path matching");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/b/c/index.view", "Note")));
//
//        System.out.println("=========== path matching");
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/b/c/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/Z/c/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/a/Z/Y/c/index.view", "Note")));
//        System.out.println(mapper.getMapping(new SitePath("wiki/Z/Y/c/index.view", "Note")));
//
//        System.out.println(">>> "+((System.currentTimeMillis()-s)/1000));
    }

}

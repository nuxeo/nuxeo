package org.nuxeo.ecm.webengine.tests.mapping;

import junit.framework.TestCase;
import org.nuxeo.ecm.webengine.mapping.MappingDescriptor;
import org.nuxeo.ecm.webengine.mapping.Mapping;
import org.nuxeo.ecm.webengine.mapping.PathPattern;
import org.nuxeo.ecm.webengine.mapping.PathMapper;

public class TestPathMapper extends TestCase {

    // FIXME
    public void XXXtest() {
        PathMapper mapper = new PathMapper();
        MappingDescriptor mdef = new MappingDescriptor();
        mdef.setPattern("(?name1:.*)/demo/(?name2:[^/]+)");
        mdef.setScript("$1 $2 $path");
        //mdef.setPattern("/wiki/**/d/*/*");
        //mdef.setScript("$1 $2 $3: $0");
        mapper.addMapping(mdef);

        //Mapping mapping = mapper.getMapping("/wiki/a/b/c/d/e/index.view");
        Mapping mapping = mapper.getMapping("/wiki/a/b/c/demo/index.view");
        mapping.setDescriptor(mdef);
        mapping.addVar("path", "ThePath");
        System.out.println(mapping.getScript());
        //System.out.println(mapper.getMapping(new SitePath("wiki/a/b/d/e/index.view", "Note")).script);

        PathPattern pat = new PathPattern("/wiki(?:/(.*))?/d/([^/]+)/([^/]+)");
        //pat = new PathPattern2("wiki(?:/(.*))?/d/([^/]+)/([^/]+)");
        //pat = new PathPattern2("(?:(.*)/)?d/([^/]+)/([^/]+)");
        pat = new PathPattern("/wiki/**/d/*/*");
        mapping = pat.match("/wiki/a/b/c/d/e/index.view");
        mapping.setDescriptor(mdef);
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

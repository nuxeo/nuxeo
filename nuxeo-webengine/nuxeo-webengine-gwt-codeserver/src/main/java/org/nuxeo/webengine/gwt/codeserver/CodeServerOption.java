package org.nuxeo.webengine.gwt.codeserver;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("option")
public class CodeServerOption {

    @XNode("@name")
    String name;

    @XNode("@value")
    String value;

    void toArgs(List<String> args) {
        args.add(name);
        args.add(value);

        // ensure code server output directories exists
        if (name.endsWith("Dir")) {
            File dir = new File(value);
            FileUtils.deleteQuietly(dir);
            dir.mkdirs();
        }
    }
}

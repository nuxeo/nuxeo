package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;

public class InfoExporter {
    List<String> infos = new ArrayList<String>();

    public void append(String info) {
        infos.add(info);
    }

    public void save(File file) throws IOException {
        FileUtils.writeLines(file, infos);
    }
}

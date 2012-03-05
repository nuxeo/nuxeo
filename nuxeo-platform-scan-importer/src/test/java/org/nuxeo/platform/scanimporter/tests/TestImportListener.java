package org.nuxeo.platform.scanimporter.tests;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.scanimporter.listener.IngestionTrigger;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestImportListener extends NXRuntimeTestCase {

    protected List<File> tmpDirectories = new ArrayList<File>();


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        for (File dir : tmpDirectories) {
            if (dir.exists()) {
                FileUtils.deleteTree(dir);
            }
        }
    }
    protected String deployTestFiles(String name) throws IOException {

        File directory = new File(FileUtils.getResourcePathFromContext("data/"
                + name));

        String tmpDirectoryPath = System.getProperty("java.io.tmpdir");
        File dst = new File(tmpDirectoryPath);

        FileUtils.copy(directory, dst);
        tmpDirectories.add(dst);

        return dst.getPath() + "/" + name;
    }


    public void testTrigger() throws Exception {

        EventContext ctx= new EventContextImpl(null,null);
        ctx.setProperty("Testing", true);
        Event evt = ctx.newEvent(IngestionTrigger.START_EVENT);

        IngestionTrigger listener = new IngestionTrigger();
        listener.handleEvent(evt);

        assertNotNull(evt.getContext().getProperty("Tested"));
    }
}

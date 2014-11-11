package org.nuxeo.ecm.automation.server.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy( { "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.server" })
public class BatchManagerTest {

    @Test
    public void testServiceRegistred() {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        Assert.assertNotNull(bm);
    }


    @Test
    public void testBatchCleanup() throws IOException {

        BatchManager bm = Framework.getLocalService(BatchManager.class);

        String batchId = bm.initBatch(null, null);
        Assert.assertNotNull(batchId);

        for (int i = 0; i<10; i++) {
            bm.addStream(batchId, ""+i, new ByteArrayInputStream(("SomeContent" + i).getBytes()), i+".txt", "text/plain");
        }

        List<Blob> blobs = bm.getBlobs(batchId);
        Assert.assertNotNull(blobs);
        Assert.assertEquals(10, blobs.size());

        Assert.assertEquals("4.txt", blobs.get(4).getFilename());
        Assert.assertEquals("SomeContent7", blobs.get(7).getString());

        FileBlob fileBlob = (FileBlob) blobs.get(9);
        File tmplFile = fileBlob.getFile();
        Assert.assertNotNull(tmplFile);
        Assert.assertTrue(tmplFile.exists());

        bm.clean(batchId);

        Assert.assertFalse(tmplFile.exists());

    }
}

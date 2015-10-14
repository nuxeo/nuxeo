package org.nuxeo.ecm.automation.server.jaxrs.batch;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class BatchTest {


    @Test
    public void it_should_be_able_to_store_blobs() throws Exception {
        Batch batch = new Batch("test");
        batch.addStream("myFirst", new ByteArrayInputStream("Hello".getBytes()), "hello.txt", "text/plain");
        batch.addStream("mySecond", new ByteArrayInputStream("Hello I said !".getBytes()), "hello2.txt", "text/plain");

        assertEquals(2, batch.getBlobs().size());
        assertEquals("hello2.txt", batch.getBlob("mySecond").getFilename() );
    }

    @Test(expected=SecurityException.class)
    public void it_should_not_be_able_to_create_a_batchid_with_doublepoint_in_id() throws Exception {
        new Batch("../testbatch");
    }
}

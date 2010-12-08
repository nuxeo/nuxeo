package org.nuxeo.runtime.test.runner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.services.streaming.RemoteInputStream;
import org.nuxeo.runtime.services.streaming.StreamManagerClient;
import org.nuxeo.runtime.services.streaming.StreamManagerServer;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.services.streaming.StringSource;

import com.google.inject.Inject;


@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, StreamingFeature.class })
public class CanStreamBlobsTest {

    @Inject StreamManagerServer server;

    @Inject StreamManagerClient client;

    protected final String contentString = "pff";
    protected String contentLocation;

    @Before public void addContent() throws IOException {
        contentLocation = server.addStream(new StringSource(contentString));
    }

    @Test public void canReadRemoteInput() throws IOException {
        RemoteInputStream in = new RemoteInputStream(client, contentLocation);
        String content = FileUtils.read(in);
        assertThat("streamed content is 'pff'", content, is(contentString));
    }
}

/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     slacoin
 */
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

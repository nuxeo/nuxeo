/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

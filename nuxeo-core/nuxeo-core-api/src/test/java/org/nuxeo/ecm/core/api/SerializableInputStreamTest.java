/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class SerializableInputStreamTest {

    @Test
    public void testFileBlobSerialization() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("test.blob");
        InputStream sin = new SerializableInputStream(url.openStream());
        File tmp = Framework.createTempFile("SerializableISTest-", ".tmp");
        Framework.trackFile(tmp, this);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tmp))) {
            out.writeObject(sin);
        }
        try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(tmp))) {
            sin = (InputStream) oin.readObject();
        }
        byte[] bytes1 = IOUtils.toByteArray(url.openStream());
        byte[] bytes2 = IOUtils.toByteArray(sin);
        assertTrue(Arrays.equals(bytes1, bytes2));
        sin.close();
    }

}

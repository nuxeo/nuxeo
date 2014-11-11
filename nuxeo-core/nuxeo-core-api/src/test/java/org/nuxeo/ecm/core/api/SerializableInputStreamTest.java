/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class SerializableInputStreamTest {

    @Test
    public void testFileBlobSerialization() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                "test.blob");
        InputStream sin = new SerializableInputStream(url.openStream());
        File tmp = File.createTempFile("SerializableISTest-", ".tmp");
        Framework.trackFile(tmp, this);
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(tmp))) {
            out.writeObject(sin);
        }
        try (ObjectInputStream oin = new ObjectInputStream(new FileInputStream(
                tmp))) {
            sin = (InputStream) oin.readObject();
        }
        byte[] bytes1 = FileUtils.readBytes(url.openStream());
        byte[] bytes2 = FileUtils.readBytes(sin);
        assertTrue(Arrays.equals(bytes1, bytes2));
        sin.close();
    }

}

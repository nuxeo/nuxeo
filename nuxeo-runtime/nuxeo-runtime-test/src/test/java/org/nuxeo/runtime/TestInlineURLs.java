/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.runtime.test.protocols.inline.InlineURLFactory;

/**
 * @author matic
 */
public class TestInlineURLs {

    String info = "some info";

    URL inlineURL;

    @BeforeClass
    public static void installHandler() throws Exception {
        InlineURLFactory.install();
    }

    @Before
    public void encodeURL() throws IOException {
        inlineURL = InlineURLFactory.newURL(info);
    }

    @Test
    public void hasCorrectContent() throws IOException {
        String inlinedContent = InlineURLFactory.newObject(String.class, inlineURL);
        assertThat(inlinedContent, equalTo(info));
    }

    @Test
    public void canRead() throws IOException {
        try (InputStream stream = inlineURL.openStream()) {
            String inlinedContent = IOUtils.toString(stream, Charsets.UTF_8);
            assertThat(inlinedContent, equalTo(info));
        }
    }
}

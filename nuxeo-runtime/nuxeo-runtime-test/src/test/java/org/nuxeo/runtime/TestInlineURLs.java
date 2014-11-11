/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.protocols.inline.InlineURLFactory;

/**
 *
 * @author matic
 *
 */
public class TestInlineURLs {

    String info = "some info";
    URL inlineURL;

    @BeforeClass public static void installHandler() throws Exception {
        InlineURLFactory.install();
    }

    @Before public void encodeURL() throws IOException {
        inlineURL = InlineURLFactory.newURL(info);
    }

    @Test public void hasCorrectContent() throws IOException {
        String inlinedContent = InlineURLFactory.newObject(String.class, inlineURL);
        assertThat(inlinedContent, equalTo(info));
    }

    @Test public void canRead() throws IOException {
        InputStream stream = inlineURL.openStream();
        String inlinedContent = FileUtils.read(stream);
        assertThat(inlinedContent, equalTo(info));
    }
}

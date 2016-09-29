/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContextImpl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @since 8.4
 */
public class AppendCommandTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private CommandContext ctx;

    private ObjectMapper om;

    @Before
    public void setUp() {
        ctx = new CommandContextImpl(tmp.getRoot());
        om = new ObjectMapper();
    }

    @Test
    public void testJson() throws Exception {
        File dest = tmp.newFile("dst.json");
        File src1 = tmp.newFile("src1.json");

        FileUtils.writeFile(dest, "{}");
        FileUtils.writeFile(src1, "{\"key1\":\"value1\"}");
        append(src1, dest);

        ObjectNode json = om.readValue(dest, ObjectNode.class);
        assertEquals("value1", json.get("key1").asText());

        File src2 = tmp.newFile("src2.JSON");
        FileUtils.writeFile(src2, "{\"key1\":\"override\", \"key2\":\"value2\"}");
        append(src2, dest);

        json = om.readValue(dest, ObjectNode.class);
        assertEquals("override", json.get("key1").asText());
        assertEquals("value2", json.get("key2").asText());
    }

    @Test
    public void testProperties() throws Exception {
        File dest = tmp.newFile("dst.properties");
        File src = tmp.newFile("src1.properties");

        FileUtils.writeFile(dest, "key1=value1");
        FileUtils.writeFile(src, "key2=value2");
        append(src, dest);

        Properties props = new Properties();
        props.load(new FileReader(dest));
        assertEquals(2, props.size());
    }

    private void append(File src, File dst) throws IOException {
        Command cmd = new AppendCommand(new Path(src.getName()), new Path(dst.getName()));
        cmd.exec(ctx);
    }
}

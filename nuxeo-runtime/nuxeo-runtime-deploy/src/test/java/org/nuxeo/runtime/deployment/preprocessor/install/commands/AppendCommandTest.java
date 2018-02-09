/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContextImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

        FileUtils.writeStringToFile(dest, "{}", UTF_8);
        FileUtils.writeStringToFile(src1, "{\"key1\":\"value1\"}", UTF_8);
        append(src1, dest);

        ObjectNode json = om.readValue(dest, ObjectNode.class);
        assertEquals("value1", json.get("key1").asText());

        File src2 = tmp.newFile("src2.JSON");
        FileUtils.writeStringToFile(src2, "{\"key1\":\"override\", \"key2\":\"value2\"}", UTF_8);
        append(src2, dest);

        json = om.readValue(dest, ObjectNode.class);
        assertEquals("override", json.get("key1").asText());
        assertEquals("value2", json.get("key2").asText());
    }

    @Test
    public void testProperties() throws Exception {
        File dest = tmp.newFile("dst.properties");
        File src = tmp.newFile("src1.properties");

        FileUtils.writeStringToFile(dest, "key1=value1", UTF_8);
        FileUtils.writeStringToFile(src, "key2=value2", UTF_8);
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

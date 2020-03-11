/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tests.tools;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @since 9.10
 */
public class TestToolsChronicle extends TestTools {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected Path basePath;

    @Test
    public void testPositionAfterDate() {
        try {
            run(String.format("position %s --log-name %s --group anotherGroup --after-date %s", getManagerOptions(),
                    LOG_NAME, Instant.now().minus(1, ChronoUnit.HOURS)));
            fail();
        } catch (UnsupportedOperationException uoe) {
            assertTrue(uoe.getMessage().contains("does not support seek by timestamp"));
        }
    }

    @Override
    public String getManagerOptions() {
        return String.format("--chronicle %s", getBasePath());
    }

    protected String getBasePath() {
        if (basePath == null) {
            try {
                basePath = folder.newFolder().toPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return basePath.toString();
    }

}

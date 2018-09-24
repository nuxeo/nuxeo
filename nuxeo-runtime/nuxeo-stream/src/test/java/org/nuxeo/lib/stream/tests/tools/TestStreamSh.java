/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.Test;
import org.nuxeo.lib.stream.tools.Main;

/**
 * Shows how to run stream.sh from a unit test, for debugging purpose.
 *
 * @since 10.2
 */
public class TestStreamSh {

    @Test
    public void runACommand() {
        // String nuxeoRoot = "/home/ben/tmp/tomcat";
        // run(String.format("tail --chronicle %s/nxserver/data/stream/audit -l audit", nuxeoRoot));
        // run(String.format("cat -n 20 --kafka %s/nxserver/config/kafka-config.xml --schema-store %s/nxserver/data/avro
        // -l bulk-counter", nuxeoRoot, nuxeoRoot));
        run("help cat");
    }

    protected boolean run(String commandLine) {
        String[] args = commandLine.split(" ");
        return new Main().run(args);
    }

}

/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */

/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.log4j;

import static java.util.Comparator.naturalOrder;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;

public class Log4JHelperTest {

    @Test
    public void testGetFileAppendersFileNames() {
        File log4jFile = FileUtils.getResourceFileFromContext("log4j2-for-tests.xml");
        List<String> fileAppenderNames = Log4JHelper.getFileAppendersFileNames(log4jFile);
        fileAppenderNames.sort(naturalOrder());
        assertEquals(5, fileAppenderNames.size());
        assertEquals("${sys:nuxeo.log.dir}/classloader.log", fileAppenderNames.get(0));
        assertEquals("${sys:nuxeo.log.dir}/nuxeo-error.log", fileAppenderNames.get(1));
        assertEquals("${sys:nuxeo.log.dir}/server.log", fileAppenderNames.get(2));
        assertEquals("${sys:nuxeo.log.dir}/stderr.log", fileAppenderNames.get(3));
        assertEquals("${sys:nuxeo.log.dir}/tomcat.log", fileAppenderNames.get(4));
    }

}

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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;

/**
 * @since 10.3
 */
public class JSONHelper {

    public static String readFile(File file, Map<String, String> toReplace) throws IOException {
        final String rawString = org.apache.commons.io.FileUtils.readFileToString(file, UTF_8);
        return StringUtils.expandVars(rawString, toReplace);
    }

    private JSONHelper() {
        // helper class
    }
}

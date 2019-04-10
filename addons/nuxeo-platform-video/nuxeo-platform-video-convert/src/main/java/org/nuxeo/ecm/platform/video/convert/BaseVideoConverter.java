/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */
package org.nuxeo.ecm.platform.video.convert;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;

/**
 * Factorize common code for video converter implementations.
 *
 * @author ogrisel
 */
public abstract class BaseVideoConverter {

    protected static final Pattern DURATION_PATTERN = Pattern.compile("Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)");

    /**
     * @deprecated since 5.5. The duration is now extracted with the other information stored in the VideoInfo.
     */
    @Deprecated
    protected static Double extractDuration(List<String> output) throws ConversionException {
        for (String line : output) {
            Matcher matcher = DURATION_PATTERN.matcher(line);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1)) * 3600 + Double.parseDouble(matcher.group(2)) * 60
                        + Double.parseDouble(matcher.group(3)) + Double.parseDouble(matcher.group(3)) / 100;
            }
        }
        // could not find the duration
        throw new ConversionException("failed to extract the duration from output: " + StringUtils.join(output, " "));
    }

    /**
     * @deprecated since 5.5.
     */
    @Deprecated
    public static String quoteFilePath(String filePath) {
        return String.format("\"%s\"", filePath);
    }

}

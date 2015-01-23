/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    protected CommandLineExecutorService cleService;

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

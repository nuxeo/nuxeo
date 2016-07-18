/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import org.nuxeo.runtime.api.Framework;

/**
 * @deprecated Since 8.4. See 'soffice' use with {@link org.nuxeo.ecm.platform.convert.plugins.CommandLineConverter} instead
 */
@Deprecated
public class ConfigBuilderHelper {

    private static final String JPIPE_LIB_PATH_PROPERTY_KEY = "jod.jpipe.lib.path";

    private static final String[] UNIX_JPIPE_PATHS = { "/usr/lib/ure/lib", "/usr/lib/libreoffice/program" };

    private static final String[] MAC_JPIPE_PATHS = {
            "/Applications/OpenOffice.org.app/Contents/basis-link/ure-link/lib",
            "/Applications/LibreOffice.app/Contents/Frameworks"
    };

    private ConfigBuilderHelper() {
    }

    protected static void hackClassLoader() throws IOException {
        try {
            String ldPath = getLibPath();
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[]) field.get(null);
            for (String path : paths) {
                if (ldPath.equals(path)) {
                    return;
                }
            }
            String[] tmp = new String[paths.length + 1];
            System.arraycopy(paths, 0, tmp, 0, paths.length);
            tmp[paths.length] = ldPath;
            field.set(null, tmp);
            System.setProperty("java.library.path", System.getProperty("java.library.path"));

        } catch (IllegalAccessException e) {
            throw new IOException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set library path");
        }
    }

    protected static String getLibPath() throws IOException {
        String jpipeLibPath = Framework.getProperty(JPIPE_LIB_PATH_PROPERTY_KEY);
        if (jpipeLibPath != null) {
            return jpipeLibPath;
        }
        jpipeLibPath = findJlibPipe();
        if (jpipeLibPath != null) {
            return jpipeLibPath;
        }
        throw new IOException("Failed to get jPipe LIbrary path.");
    }

    protected static String findJlibPipe() {
        List<String> possiblePaths = new ArrayList<>();

        if (SystemUtils.IS_OS_LINUX) {
            possiblePaths.addAll(Arrays.asList(UNIX_JPIPE_PATHS));
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            possiblePaths.addAll(Arrays.asList(MAC_JPIPE_PATHS));
        }

        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_MAC_OSX}
     */
    @Deprecated
    protected static boolean isMac() {
        return SystemUtils.IS_OS_MAC_OSX;
    }

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_LINUX}
     */
    @Deprecated
    protected static boolean isLinux() {
        return SystemUtils.IS_OS_LINUX;
    }

}

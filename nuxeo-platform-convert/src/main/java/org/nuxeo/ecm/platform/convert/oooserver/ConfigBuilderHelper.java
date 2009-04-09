/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.convert.oooserver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.anwrt.ooserver.daemon.Config;

public class ConfigBuilderHelper {

    private static String UNIX_OO_EXE = "soffice";
    private static String WIN_OO_EXE = "soffice.exe";
    private static String[] UNIX_OO_PATHS = {"/usr/lib/openoffice/program"};
    private static String[] WIN_OO_PATHS = {
            "C:/Program Files/OpenOffice.org 2.2",
            "C:/Program Files/OpenOffice.org 2.3",
            "C:/Program Files/OpenOffice.org 2.4",
            "C:/Program Files/OpenOffice.org 2.5"};

    protected OOoServerDescriptor desc;

    protected String ooCommandPath;

    private Config ooServerConfig;

    public ConfigBuilderHelper(OOoServerDescriptor desc) {
        this.desc = desc;
    }

    protected static List<String> getSystemPaths() {
        String pathStr = System.getenv("PATH");
        String[] paths = pathStr.split(":");
        return Arrays.asList(paths);
    }

    protected static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    protected String getOOServerPath() {
        String exeName = UNIX_OO_EXE;
        if (isWindows()) {
            exeName = WIN_OO_EXE;
        }
        if (ooCommandPath == null) {
            ooCommandPath = desc.getOooInstallationPath();
            File oo = new File(ooCommandPath + "/" + exeName);
            if (!oo.exists()) {
                ooCommandPath = null;
            }
        }
        if (ooCommandPath == null) {
            // try to autodetect
            List<String> paths = new ArrayList<String>();
            if (isWindows()) {
                paths.addAll(Arrays.asList(WIN_OO_PATHS));
            } else {
                paths.addAll(Arrays.asList(UNIX_OO_PATHS));
            }
            paths.addAll(getSystemPaths());

            for (String path : paths) {
                File oo = new File(path + "/" + exeName);
                if (oo.exists()) {
                    ooCommandPath = path;
                    break;
                }
            }
        }
        return ooCommandPath;
    }

    protected ArrayList<String> getUserDirs() {
        ArrayList<String> userDirs = new ArrayList<String>();

        for (int i = 0; i < desc.getOooWorkers(); i++) {
            userDirs.add("file://" + System.getProperty("java.io.tmpdir")
                    + "/nxooserver" + i);
        }
        return userDirs;
    }

    protected static void hackClassLoader(String ldPath) throws IOException {
        try {
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
            System.setProperty("java.library.path",
                    System.getProperty("java.library.path"));

        } catch (IllegalAccessException e) {
            throw new IOException(
                    "Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException(
                    "Failed to get field handle to set library path");
        }
    }

    protected String getLibPath() {
        if (desc.getJpipeLibPath() != null) {
            return desc.getJpipeLibPath();
        }
        return getOOServerPath();
    }

    public Config getServerConfig() {
        if (ooServerConfig == null) {
            if (getOOServerPath() == null) {
                return null;
            }
            ooServerConfig = new Config();
            ooServerConfig.officeProgramDirectoryPath = getOOServerPath();
            ooServerConfig.acceptor = "socket,host=" + desc.getOooListenerIP()
                    + ",port=" + desc.getOooListenerPort();
            ooServerConfig.adminAcceptor = "socket,host=" + desc.getOooListenerIP()
                    + ",port=" + desc.getOooDaemonListenerPort();
            ooServerConfig.maxUsageCountPerInstance = desc.getOooRecycleInterval();
            ooServerConfig.userInstallation = getUserDirs();

            try {
                String ld_path = getLibPath();
                hackClassLoader(ld_path);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return ooServerConfig;
    }

}

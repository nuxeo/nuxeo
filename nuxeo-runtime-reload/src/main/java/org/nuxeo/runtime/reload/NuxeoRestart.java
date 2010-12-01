/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;

/**
 * Restart a Nuxeo. For now works only on Unix systems.
 * <p>
 * Usage: <code>NuxeoRestart.restart()</code>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoRestart {

    public static void restart() throws Exception {
        List<String> cmd = new ArrayList<String>();
        String javaHome = System.getProperty("java.home");
        File java = new File(new File(javaHome), "bin/java").getCanonicalFile();
        if (java.isFile()) {
            cmd.add(java.getAbsolutePath());
        } else { // try java
            cmd.add("java");
        }
        File bundle = Framework.getRuntime().getBundleFile(
                ReloadComponent.getBundle());
        cmd.add("-cp");
        cmd.add(bundle.getAbsolutePath());
        cmd.add(NuxeoRestart.class.getName());
        Environment env = Environment.getDefault();
        if (env.isJBoss()) {
            String home = System.getProperty("jboss.home.dir");
            File bin = new File(home, "bin");
            File file = new File(bin, "nuxeoctl").getCanonicalFile();
            if (file.isFile()) {
                cmd.add(file.getAbsolutePath());
                cmd.add("start");
            } else {
                file = new File(bin, "jbossctl").getCanonicalFile();
                if (file.isFile()) {
                    cmd.add(file.getAbsolutePath());
                    cmd.add("start");
                }
            }
        } else if (env.isTomcat()) {
            String home = System.getProperty("catalina.base");
            File bin = new File(home, "bin");
            File file = new File(bin, "nuxeoctl").getCanonicalFile();
            if (file.isFile()) {
                cmd.add(file.getAbsolutePath());
                cmd.add("start");
            } else {
                file = new File(bin, "catalina.sh").getCanonicalFile();
                if (file.isFile()) {
                    cmd.add(file.getAbsolutePath());
                    cmd.add("start");
                }
            }
        } else {
            File file = new File(env.getHome(), "bin/nuxeoctl").getCanonicalFile();
            if (file.isFile()) {
                cmd.add(file.getAbsolutePath());
                cmd.add("start");
            } else {
                file = new File(env.getHome(), "nxserverctl.sh").getCanonicalFile();
                if (file.isFile()) {
                    cmd.add(file.getAbsolutePath());
                    cmd.add("start");
                }
            }
        }

        if (cmd.size() <= 1) {
            throw new FileNotFoundException("Could not find startup script");
        }

        // create lock file
        File lock = File.createTempFile("nx-restart", ".lock").getCanonicalFile();
        lock.deleteOnExit();
        cmd.add(lock.getAbsolutePath());

        new ProcessBuilder().command(cmd).start();

        try {
            Framework.shutdown();
        } finally {
            System.exit(100); // signal for restart
        }
    }

    /**
     * First argument is the script to run followed by script arguments. The
     * last argument is the lock file.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: RestartNuxeo script lock");
            System.exit(1);
        }
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 0, newArgs, 0, newArgs.length);
        File lock = new File(args[args.length - 1]);
        File script = new File(args[0]);
        try {
            // wait for the lock file to be removed
            while (lock.isFile()) {
                Thread.sleep(2000);
            }
            Thread.sleep(1000);
            // start nuxeo
            Runtime.getRuntime().exec(
                    newArgs,
                    new String[] { "JAVA_HOME="
                            + System.getProperty("java.home") },
                    script.getParentFile());
        } catch (Throwable e) {
            System.exit(2);
        }
    }

}

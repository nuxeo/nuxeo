/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
 * @deprecated since 9.10 - use {@link org.nuxeo.ecm.admin.NuxeoCtlManager#restart()} instead which handles windows OS.
 */
@Deprecated
public class NuxeoRestart {

    public static void restart() throws IOException {
        List<String> cmd = new ArrayList<String>();
        String javaHome = System.getProperty("java.home");
        File java = new File(new File(javaHome), "bin/java").getCanonicalFile();
        if (java.isFile()) {
            cmd.add(java.getAbsolutePath());
        } else { // try java
            cmd.add("java");
        }
        File bundle = Framework.getRuntime().getBundleFile(ReloadComponent.getBundle());
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
        File lock = Framework.createTempFile("nx-restart", ".lock").getCanonicalFile();
        lock.deleteOnExit();
        cmd.add(lock.getAbsolutePath());

        new ProcessBuilder().command(cmd).start();

        try {
            Framework.shutdown();
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during shutdown, still proceeding", cause);
        } finally {
            System.exit(100); // signal for restart
        }
    }

    /**
     * First argument is the script to run followed by script arguments. The last argument is the lock file.
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
        boolean ok = false;
        try {
            // wait for the lock file to be removed
            while (lock.isFile()) {
                Thread.sleep(2000);
            }
            Thread.sleep(1000);
            // start nuxeo
            Runtime.getRuntime().exec(newArgs, new String[] { "JAVA_HOME=" + System.getProperty("java.home") },
                    script.getParentFile());
            ok = true;
        } finally {
            if (!ok) {
                System.exit(2);
            }
        }
    }

}

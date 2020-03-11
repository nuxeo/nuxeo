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
 *     bstefanescu, jcarsique
 *
 * $Id$
 */

package org.nuxeo.runtime.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("You must specify a main class to invoke as the first parameter to that program.");
            System.exit(2);
        }

        // The class name format is: path/ClassName:methodName
        // where the path may be a path to a Jar file or a directory
        // The default method name is 'main'
        String path = args[0];
        String method = "main";
        int p = path.lastIndexOf(':');
        if (p > -1) {
            method = path.substring(p + 1);
            path = path.substring(0, p);
            if (method == null || method.length() == 0) {
                method = "main";
            }
        }
        p = path.lastIndexOf('/');
        String mainClassName = null;
        if (p > -1) {
            mainClassName = path.substring(p + 1);
            path = path.substring(0, p);
        }

        // construct class loader to use to load application
        try {
            File file = new File(path).getCanonicalFile();
            URL[] urls = new URL[1];
            if (file.exists()) {
                urls[0] = file.toURI().toURL();
            } else {
                System.err.println("Could not find main class: " + args[0]
                        + ". Make sure you have this class on the boot class path");
                System.exit(3);
            }
            URLClassLoader classLoader = new URLClassLoader(urls, Main.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);

            // set the property used by Nuxeo OSGi launcher to create the system
            // bundle
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Method m = mainClass.getMethod(method, String[].class);
            if (args.length > 1) {
                String[] tmp = new String[args.length - 1];
                System.arraycopy(args, 1, tmp, 0, tmp.length);
                args = tmp;
            } else {
                args = new String[0];
            }
            m.invoke(null, new Object[] { args });
        } catch (IOException e) {
            System.err.println("Could not find main class: " + args[0]
                    + ". Make sure you have this class on the boot class path");
            e.printStackTrace();
            System.exit(3);
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load main class: " + args[0]
                    + ". Make sure you have this class on the boot class path");
            e.printStackTrace();
            System.exit(4);
        } catch (NoSuchMethodException e) {
            System.err.println("Could not find main class method: " + mainClassName + "." + method + "(String[] args)");
            e.printStackTrace();
            System.exit(5);
        } catch (SecurityException e) {
            System.err.println("Failed to access the main class method: " + mainClassName + "." + method
                    + "(String[] args)");
            e.printStackTrace();
            System.exit(5);
        } catch (IllegalAccessException e) {
            System.err.println("Failed to invoke method: " + mainClassName + "." + method + "(String[] args)");
            e.printStackTrace();
            System.exit(6);
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to invoke method: " + mainClassName + "." + method + "(String[] args)");
            e.printStackTrace();
            System.exit(6);
        } catch (InvocationTargetException e) {
            System.err.println("Failed to invoke method: " + mainClassName + "." + method + "(String[] args)");
            e.printStackTrace();
            System.exit(6);
        }

    }

}

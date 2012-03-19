/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.nuxeo.runtime.tomcat.adapter.tests.metaindex;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Constructs a meta-index of the specified jar files. The meta-index contains
 * prefixes of packages contained in these jars, indexed by the jar file name.
 * It is intended to be consumed by the JVM to allow the boot class loader to be
 * made lazier. For example, when class data sharing is enabled, the presence of
 * the meta-index allows the JVM to skip opening rt.jar if all of the dependent
 * classes of the application are in the shared archive. A similar mechanism
 * could be useful at the application level as well, for example to make the
 * extension class loader lazier.
 * 
 * <p>
 * The contents of the meta-index file for jre/lib look something like this:
 * 
 * <PRE>
 * % VERSION 2
 * # charsets.jar
 * sun/
 * # jce.jar
 * javax/
 * ! jsse.jar
 * sun/
 * com/sun/net/
 * javax/
 * com/sun/security/
 * # management-agent.jar
 * ! rt.jar
 * org/w3c/
 * com/sun/image/
 * com/sun/org/
 * com/sun/imageio/
 * com/sun/accessibility/
 * javax/
 * ...
 * </PRE>
 * 
 * <p>
 * It is a current invariant of the code in the JVM which consumes the
 * meta-index that the meta-index indexes only jars in one directory. It is
 * acceptable for jars in that directory to not be mentioned in the meta-index.
 * The meta-index is designed more to be able to perform a quick rejection test
 * of the presence of a particular class in a particular jar file than to be a
 * precise index of the contents of the jar.
 */

public class BuildMetaIndex {
    public static void main(String... args) throws IOException {
        /*
         * The correct usage of this class is as following: java BuildMetaIndex
         * -o <meta-index> <a list of jar files> So the argument length should
         * be at least 3 and the first argument should be '-o'.
         */
        if (args.length < 3 || !args[0].equals("-o")) {
            printUsage();
            System.exit(1);
        }

        PrintStream out = null;
        try {
           out = new PrintStream(new FileOutputStream(args[1]));
        } catch (FileNotFoundException fnfe) {
            System.err.println("FileNotFoundException occurred");
            System.exit(2);
        }
        try {
            build(out, Arrays.copyOfRange(args, 2, args.length));
        } finally {
            out.close();
        }
    }

    public static void build(PrintStream out, String... args)
            throws IOException {
        out.println("% VERSION 2");
        out.println("% WARNING: this file is auto-generated; do not edit");
        out.println("% UNSUPPORTED: this file and its format may change and/or");
        out.println("%   may be removed in a future release");
        for (int i = 0; i < args.length; i++) {
            String filename = args[i];
            JarMetaIndex jmi = new JarMetaIndex(filename);
            HashSet<String> index = jmi.getMetaIndex();
            if (index == null) {
                continue;
            }
            /*
             * meta-index file plays different role in JVM and JDK side. On the
             * JVM side, meta-index file is used to speed up locating the class
             * files only while on the JDK side, meta-index file is used to
             * speed up the resources file and class file. To help the JVM and
             * JDK code to better utilize the information in meta-index file, we
             * mark the jar file differently. Here is the current rule we use
             * (See JarFileKind.getMarkChar() method. ) For jar file containing
             * only class file, we put '!' before the jar file name; for jar
             * file containing only resources file, we put '@' before the jar
             * file name; for jar file containing both resources and class file,
             * we put '#' before the jar name. Notice the fact that every jar
             * file contains at least the manifest file, so when we say
             * "jar file containing only class file", we don't include that
             * file.
             */

            out.println(jmi.getJarFileKind().getMarkerChar() + " " + filename);
            for (Iterator<String> iter = index.iterator(); iter.hasNext();) {
                out.println(iter.next());
            }

        }
        out.flush();
    }

    private static void printUsage() {
        String usage = "BuildMetaIndex is used to generate a meta index file for the jar files\n"
                + "you specified. The following is its usage:\n"
                + " java BuildMetaIndex -o <the output meta index file> <a list of jar files> \n"
                + " You can specify *.jar to refer to all the jar files in the current directory";

        System.err.println(usage);
    }
}

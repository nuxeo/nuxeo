/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.client;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Test {

    public static void main(String[] args) {
        int n = 100000;
        String s1 = "asdfgh";
        String s2 = "asdfgh";

        int i = 0;
        double s = 0;

        for (i=0; i<n; i++) {
            "asdfgh".equals("asdfgh");
        }

        s = System.currentTimeMillis();
    for (i=0; i<n; i++) {
        "asdfgh".equals("asdfgh");
    }
    System.out.println("eq1>> "+((System.currentTimeMillis()-s)/1000));
    s = System.currentTimeMillis();
    for (i=0; i<n; i++) {
        s1 = new String("asdfgh"); s2 = new String("asdfgh");
        s1.equals(s2);
    }
    System.out.println("eq2>> "+((System.currentTimeMillis()-s)/1000));

    s = System.currentTimeMillis();
    for (i=0; i<n; i++) {
        s1 = new String("asdfgh"); s2 = new String("asdfgh");
        s1.startsWith(s2, 0);
        if (s1.length() < s2.length()) {}
    }
    System.out.println("st>> "+((System.currentTimeMillis()-s)/1000));

    }

}

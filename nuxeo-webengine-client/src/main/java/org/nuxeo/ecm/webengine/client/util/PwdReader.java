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
package org.nuxeo.ecm.webengine.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * In java 6 there is direct support for reading passwords form a console
 * <pre> 
 *  if ((cons = System.console()) != null &&
 *    (passwd = cons.readPassword("[%s]", "Password:")) != null) {
 *    ...
 *  }
 * </pre>
 * This can be for java < 6.
 * A separate thread is used to send to the console the backspace character to erase the last-typed character.  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PwdReader {

    public static String read() throws IOException {
        ConsoleEraser consoleEraser = new ConsoleEraser();
        System.out.print("Password:  ");
        BufferedReader stdin = new BufferedReader(new
                InputStreamReader(System.in));
        consoleEraser.start();                       
        String pass = stdin.readLine();
        consoleEraser.halt();
        System.out.print("\b");
        return pass;
    }

    static class ConsoleEraser extends Thread {
        private boolean running = true;
        public void run() {
            while (running) {
                System.out.print("\b ");
            }
        }

        public synchronized void halt() {
            running = false;
        }
    }
    
}

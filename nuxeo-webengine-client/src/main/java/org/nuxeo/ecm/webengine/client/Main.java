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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.client;

import org.nuxeo.ecm.webengine.client.console.Console;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    
    public static void main(String[] args) {
        
        if (args.length > 0) {
            String cmd = args[0];
            if (!cmd.startsWith("-")) {
                // builtin command
                if ("command".equals(cmd)) {
                    
                } else if ("help".equals(cmd)) {
                    
                } else {
                    
                }
            }
        }

        try {
            Console console = new Console();
            //TODO use user profiles to setup console  like prompt and default service to cd in
            console.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}

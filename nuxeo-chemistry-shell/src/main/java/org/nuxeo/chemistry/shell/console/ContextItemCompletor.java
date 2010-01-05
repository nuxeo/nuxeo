/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.chemistry.shell.console;

import java.util.List;

import jline.Completor;

import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContextItemCompletor implements Completor {


    public ContextItemCompletor() {
    }

    protected void collectNames(String[] keys, String prefix, List candidates) {
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                candidates.add(key);
            }
        }
    }
    
    public int complete(String buf, int off, List candidates) {         
        if (buf == null) {
            buf = "";
        }
        
        Context ctx = null;
        Path path = new Path(buf);
        String prefix = path.lastSegment();
        if (prefix == null) {
            ctx = Console.getDefault().getApplication().getContext();
            prefix = "";
        } else if (path.segmentCount() == 1) {
            ctx = Console.getDefault().getApplication().getContext();
        } else {
            path = path.removeLastSegments(1);
            ctx = Console.getDefault().getApplication().resolveContext(path);            
        }
        if (ctx != null) {            
            collectNames(ctx.entries(), prefix, candidates);
            return buf.length()-prefix.length();
        } else {
            return -1;
        }
    }

}

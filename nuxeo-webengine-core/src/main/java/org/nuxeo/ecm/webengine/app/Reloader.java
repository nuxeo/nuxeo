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
package org.nuxeo.ecm.webengine.app;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Reloader {

    public static final int DEFAULT_TIMEOUT_CHECK = 2000; // 2 seconds 
    
    private int checkTimeout = 0;
    
    private long lastCheck;
    private long lastModified;
    protected File fileToCheck;  
    protected WebEngine engine;
    protected List<Reloadable> listeners;
    
    //TODO add it to webengine ?
    private static Reloader instance = new Reloader();
    
    public static Reloader getInstance() {
        return instance;
    }
    
    public Reloader() {
        this (0);
    }
    
    public Reloader(int checkTimeout) {
        engine = Framework.getLocalService(WebEngine.class);
        listeners = new Vector<Reloadable>();
        fileToCheck = engine.getRootDirectory();
        lastModified = fileToCheck.lastModified();
        setCheckTimeout(checkTimeout);
    }
    
    public void addListener(Reloadable listener) {
        listeners.add(listener);
    }

    public void removeListener(Reloadable listener) {
        listeners.remove(listener);
    }

    public void setEnabled(boolean isEnabled) {
        checkTimeout = DEFAULT_TIMEOUT_CHECK; 
    }
    
    public boolean isEnabled() {
        return checkTimeout != 0;
    }
    
    public void setCheckTimeout(int checkTimeout) {
        this.checkTimeout = checkTimeout;
    }
    
    public int getCheckTimeout() {
        return checkTimeout;
    }
    
    public void check() {
        if (checkTimeout == 0) {
            return;
        }
//        if (lastModified == 0) {
//            lastModified = fileToCheck.lastModified();
//        }
        long now = System.currentTimeMillis(); 
        if (now - lastCheck > checkTimeout) {
            lastCheck = now;
            long tm = fileToCheck.lastModified();
            if (tm > lastModified) {
                lastModified = tm;
                reload();
            }
        }
    }
    
    public void reload() {
        engine.reload();
        for (Reloadable reloadable : listeners.toArray(new Reloadable[listeners.size()])) {
            reloadable.reload();
        }
    }
    
    
}

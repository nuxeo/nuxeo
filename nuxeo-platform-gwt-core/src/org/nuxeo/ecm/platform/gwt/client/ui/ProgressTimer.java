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

package org.nuxeo.ecm.platform.gwt.client.ui;

import org.nuxeo.ecm.platform.gwt.client.Framework;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * This class is used to show the progress dialog if an async request was not completed after a given
 * time interval.   
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProgressTimer {
    
    protected Timer timer;
    protected PopupPanel busy;
        
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        hideBusy();
    }
    
    public void start(int timeout) {
        if (timer != null) {
            return;
        }
        timer = new Timer() {
            @Override
            public void run() {
                if (timer != null) { // not canceled
                    showBusy();  
                }
                if (timer == null) { // canceled
                    hideBusy();
                }
            }
        };
        timer.schedule(timeout); 
    }
    
    public void showBusy() {
        Framework.showLoading("Please Wait ...");
    }
    
    public void hideBusy() {
        Framework.showLoading(null);
    }
    
}

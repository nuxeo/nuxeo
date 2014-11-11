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

package org.nuxeo.ecm.webengine.gwt.client.ui;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

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
        if (busy == null) {
            busy = new PopupPanel(false, true);
            //HorizontalPanel panel = new HorizontalPanel();
            //panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
            //panel.add(new Image("images/loading.gif"));
            //panel.add(new HTML(<img src="images/loading.gif"));
//            panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
//            panel.add(new Label("Loading ..."));
            VerticalPanel panel = new VerticalPanel();
            panel.setSpacing(0);
            panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            panel.add(new Label("Loading ..."));
            panel.add(new Image("images/progress.gif"));
            busy.add(panel);
            busy.hide();
        }
        busy.center();
//        busy.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
//            public void setPosition(int offsetWidth, int offsetHeight) {
//                RootPanel root = RootPanel.get();
//                busy.setPopupPosition((root.getOffsetWidth() - offsetWidth) / 2,
//                        (root.getOffsetHeight() - offsetHeight) / 2);
//            }
//        });
    }

    public void hideBusy() {
        if (busy == null) return;
        busy.hide();
    }

}

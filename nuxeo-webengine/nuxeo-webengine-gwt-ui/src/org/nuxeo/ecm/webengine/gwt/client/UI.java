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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.client;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.webengine.gwt.client.ui.Context;
import org.nuxeo.ecm.webengine.gwt.client.ui.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.ui.Images;
import org.nuxeo.ecm.webengine.gwt.client.ui.ProgressTimer;
import org.nuxeo.ecm.webengine.gwt.client.ui.UIApplication;
import org.nuxeo.ecm.webengine.gwt.client.ui.impl.DefaultApplicationBundle;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class UI implements EntryPoint {

    private static Image EMPTY_IMAGE = null;
    protected static Images imagesBundle = GWT.create(Images.class);
    protected static List<ContextListener> contextListeners = new ArrayList<ContextListener>();
    protected static Context ctx = new Context();
    protected static ProgressTimer progressTimer = new ProgressTimer();

    public static Images getImages() {
        return imagesBundle;
    }

    public static Image getEmptyImage() {
        if (EMPTY_IMAGE == null) {
            EMPTY_IMAGE = imagesBundle.noimage().createImage();
        }
        return EMPTY_IMAGE;
    }

    public static void addContextListener(ContextListener listener) {
        contextListeners.add(listener);
    }

    public static void removeContextListener(ContextListener listener) {
        contextListeners.remove(listener);
    }

    public static ContextListener[] getContextListeners() {
        return contextListeners.toArray(new ContextListener[contextListeners.size()]);
    }

    public static void fireEvent(int event) {
        for (ContextListener listener : contextListeners) {
            listener.onContextEvent(event);
        }
    }

    public static boolean isAuthenticated() {
        return ctx.getUsername() != null;
    }

    public static Context getContext() {
        return ctx;
    }

    public static void openInEditor(Object input) {
        ((UIApplication)Framework.getApplication()).openInEditor(input);
    }

    public static void showView(String name) {
        ((UIApplication)Framework.getApplication()).showView(name);
    }

    public static void showError(Throwable t) {
        final PopupPanel panel = new PopupPanel(true, true);
        panel.add(new Label(t.getMessage()));
        panel.center();
//        panel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
//            public void setPosition(int offsetWidth, int offsetHeight) {
//                RootPanel root = RootPanel.get();
//                panel.setPopupPosition((root.getOffsetWidth() - offsetWidth) / 2,
//                        (root.getOffsetHeight() - offsetHeight) / 2);
//            }
//        });
    }

    public static void showBusy() {
        progressTimer.start(100);
    }

    public static void hideBusy() {
        progressTimer.cancel();
    }

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
      ApplicationBundle bundle = GWT.create(DefaultApplicationBundle.class);
      bundle.start();
  }


}

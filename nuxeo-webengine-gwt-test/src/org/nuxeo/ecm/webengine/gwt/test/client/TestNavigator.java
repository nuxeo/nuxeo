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

package org.nuxeo.ecm.webengine.gwt.test.client;

import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpCallback;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.webengine.gwt.client.http.Server;
import org.nuxeo.ecm.webengine.gwt.client.ui.View;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestNavigator extends View {

    public TestNavigator() {
        super("navigator");
        setTitle("Navigator");
        setSize("100%", "100%");
        System.out.println("-------------------"+(toString())+"-----------------");
    }

    @Override
    protected Widget createContent() {
        Button button1 = new Button("Show Tabs");
        button1.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                System.out.println("cliccccccccccccck");
                //TabPanel w = new TabPanel();
                //UI.openInEditor(w);
                try {
                Server.post("/skin/wiki/css/wiki11.css").setCallback(new HttpCallback() {
                    @Override
                    public void onSuccess(HttpResponse response) {
                        System.out.println("333333333333333333333");
                    }
                    public void onFailure(Throwable cause) {
                        System.out.println("errrrrrrror");
                        cause.printStackTrace();
                    }

                }).send();
                } catch (Exception e) {
                    Framework.handleError(e);
                }
            }
        });
        Button button2 = new Button("Button 2");
        button2.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                UI.openInEditor("Testing Unknown Input");
            }
        });
        Button button3 = new Button("Button 3");
        button3.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                System.out.println(TestNavigator.this.toString());
            }
        });

        HorizontalPanel panel = new HorizontalPanel();
        //panel.setWidth("100%");
        panel.add(button1);
        panel.add(button2);
        panel.add(button3);

        System.out.println(panel.toString());

        return panel;
    }

    @Override
    public Image getIcon() {
        return Main.getImages().filtersgroup().createImage();
    }
}

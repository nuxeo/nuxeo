/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.ui.admin;

import java.util.Map;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartWidget;
import org.nuxeo.ecm.platform.gwt.client.ui.UI;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AdministrationView extends SmartView {


    public AdministrationView() {
        super("admin");
    }


    @Override
    public String getTitle() {
        return "Administration";
    }

    static boolean _show = false;
    @Override
    protected VStack createWidget() {
        VStack stack = new VStack();
//        ImgButton btn = new ImgButton();
//        btn.setTitle("Create");
        Anchor a= new Anchor("Create");
        a.setHref("#");
        a.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Framework.showLoading("Testing ...");
                new Timer() {
                    @Override
                    public void run() {
                        Framework.showLoading(null);
                    }
                }.schedule(5000);
                //UI.showView("views/login");
            }
        });
        SmartWidget wc = new SmartWidget(a);
        stack.addMember(wc);
        HTMLFlow aa= new HTMLFlow("<a href=\"#www\">Delete</a>");
        stack.addMember(aa);
        Button b= new Button("Create");
        b.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                //UI.openInEditor(new Url("http://google.com"));
                Map<String,String> map = Framework.getRepositoryRoots();
                for (String key : map.keySet()) {
                    System.out.println("*** "+key+" : "+map.get(key));
                }
            }
        });
        stack.addMember(b);
        b= new Button("Delete");
        b.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                UI.openInEditor("<b>Hello!</b> some text: "+System.currentTimeMillis());
            }
        });
        stack.addMember(b);
        return stack;
    }

}

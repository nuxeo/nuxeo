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

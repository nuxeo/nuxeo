/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.presenter;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.allen_sauer.gwt.dnd.client.util.DragClientBundle;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class YUIFlowPanelDropController extends FlowPanelDropController {

    public YUIFlowPanelDropController(FlowPanel dropTarget) {
        super(dropTarget);
    }

    @Override
    protected Widget newPositioner(DragContext context) {
        HTML positioner = new HTML("");
        positioner.addStyleName(DragClientBundle.INSTANCE.css().flowPanelPositioner());
        return positioner;
    }
}

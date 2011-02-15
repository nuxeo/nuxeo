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

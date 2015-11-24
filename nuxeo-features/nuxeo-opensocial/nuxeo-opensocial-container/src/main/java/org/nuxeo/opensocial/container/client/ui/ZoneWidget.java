package org.nuxeo.opensocial.container.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.client.ui.api.HasUnits;

import com.google.gwt.user.client.ui.FlowPanel;

/**
 * @author Stéphane Fourrier
 */
public class ZoneWidget extends FlowPanel implements HasUnits {

    public ZoneWidget(String cssStyle) {
        setCssTemplate(cssStyle);
    }

    public boolean hasWebContents() {
        for (int i = 0; i < getWidgetCount(); i++) {
            if (((UnitWidget) getWidget(i)).hasWebContents()) {
                return true;
            }
        }
        return false;
    }

    public void addUnit(UnitWidget unitWidget) {
        this.add(unitWidget);
    }

    public void setCssTemplate(String cssTemplate) {
        this.setStyleName(cssTemplate);
    }

    public void removeUnit(int index) {
        this.remove(index);
    }

    public UnitWidget getUnit(String id) {
        for (int i = 0; i < getWidgetCount(); i++) {
            if (getWidget(i).getElement().getAttribute("id").equals(id))
                return (UnitWidget) getWidget(i);
        }
        return null;
    }

    public UnitWidget getUnit(int unitIndex) {
        return (UnitWidget) getWidget(unitIndex);
    }

    public List<UnitWidget> getUnits() {
        List<UnitWidget> list = new ArrayList<UnitWidget>();

        for (int i = 0; i < getWidgetCount(); i++) {
            list.add((UnitWidget) getWidget(i));
        }
        return list;
    }

    public int getNumberOfUnits() {
        return getWidgetCount();
    }
}

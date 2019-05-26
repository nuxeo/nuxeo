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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.view;

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util.Rectangle;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class SelectedArea {

    private static final String FULL_MOVING_AREA_CSS_CLASS = "thumbnailMovingSelectedArea";

    private static final String INNER_MOVING_AREA_CSS_CLASS = "thumbnailInnerMovingSelectedArea";

    private static final String FULL_AREA_CSS_CLASS = "thumbnailSelectedArea";

    private static final String INNER_AREA_CSS_CLASS = "thumbnailInnerSelectedArea";

    private int maxWidth;

    private int maxHeight;

    private Rectangle area;

    private final SimplePanel fullMovingArea = new SimplePanel();

    private final SimplePanel innerMovingArea = new SimplePanel();

    private final SimplePanel fullArea = new SimplePanel();

    private final SimplePanel innerArea = new SimplePanel();

    public SelectedArea(int maxWidth, int maxHeight, Panel parent) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;

        createAreas();
        parent.add(fullArea);
        parent.add(innerArea);
        parent.add(fullMovingArea);
        parent.add(innerMovingArea);
    }

    private void createAreas() {
        fullMovingArea.setStyleName(FULL_MOVING_AREA_CSS_CLASS);
        setDefaultStyleAttributes(fullMovingArea);

        innerMovingArea.setStyleName(INNER_MOVING_AREA_CSS_CLASS);
        setDefaultStyleAttributes(innerMovingArea);

        fullArea.setStyleName(FULL_AREA_CSS_CLASS);
        setDefaultStyleAttributes(fullArea);

        innerArea.setStyleName(INNER_AREA_CSS_CLASS);
        setDefaultStyleAttributes(innerArea);
    }

    private void setDefaultStyleAttributes(Widget w) {
        Element element = w.getElement();
        DOM.setStyleAttribute(element, "display", "block");
        DOM.setStyleAttribute(element, "position", "absolute");
    }

    public void changeArea(Rectangle area) {
        this.area = area;
        drawArea();
    }

    public Rectangle getCurrentArea() {
        return area;
    }

    public void move(int x, int y) {
        area.move(x, y);
        ensureValidArea();
        drawArea();
    }

    public void centerOn(int x, int y) {
        area.centerOn(x, y);
        ensureValidArea();
        drawArea();
    }

    public void putArea() {
        updateAreaStyles(fullArea, area.getTopLeft().getX(), area.getTopLeft().getY(), area.getWidth(),
                area.getHeight());
        updateAreaStyles(innerArea, area.getTopLeft().getX(), area.getTopLeft().getY(), area.getWidth(),
                area.getHeight());
    }

    private void ensureValidArea() {
        int newX = area.getTopLeft().getX();
        int newY = area.getTopLeft().getY();

        if (newX + fullMovingArea.getOffsetWidth() > maxWidth) {
            newX = maxWidth - fullMovingArea.getOffsetWidth();
        } else if (newX < 0) {
            newX = 0;
        }
        if (newY + fullMovingArea.getOffsetHeight() > maxHeight) {
            newY = maxHeight - fullMovingArea.getOffsetHeight();
        } else if (newY < 0) {
            newY = 0;
        }
        area.setLocation(newX, newY);
    }

    private void drawArea() {
        updateAreaStyles(fullMovingArea, area.getTopLeft().getX(), area.getTopLeft().getY(), area.getWidth(),
                area.getHeight());
        updateAreaStyles(innerMovingArea, area.getTopLeft().getX(), area.getTopLeft().getY(), area.getWidth(),
                area.getHeight());
    }

    private static void updateAreaStyles(Widget w, int left, int top, int width, int height) {
        Element element = w.getElement();
        DOM.setStyleAttribute(element, "left", "" + left + "px");
        DOM.setStyleAttribute(element, "top", "" + top + "px");
        DOM.setStyleAttribute(element, "width", "" + width + "px");
        DOM.setStyleAttribute(element, "height", "" + height + "px");
    }

}

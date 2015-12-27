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

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class Rectangle {

    private Point topLeft;

    private int width;

    private int height;

    public Rectangle() {
        this(new Point(), 0, 0);
    }

    public Rectangle(Point topLeft, int width, int height) {
        this.topLeft = topLeft;
        this.width = width;
        this.height = height;
    }

    public void move(int x, int y) {
        topLeft.move(x, y);
    }

    public void setLocation(int x, int y) {
        topLeft.setLocation(x, y);
    }

    public void centerOn(int x, int y) {
        topLeft.setLocation(x - (width / 2), y - (height / 2));
    }

    /**
     * @return the topLeft.
     */
    public Point getTopLeft() {
        return topLeft;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Point getCenter() {
        int x = topLeft.getX() + (width / 2);
        int y = topLeft.getY() + (height / 2);
        return new Point(x, y);
    }

    public boolean contains(Point p) {
        Point bottomRight = new Point(topLeft.getX() + width, topLeft.getY() + height);
        if (p.getX() >= topLeft.getX() && p.getX() <= bottomRight.getX() && p.getY() >= topLeft.getY()
                && p.getY() <= bottomRight.getY()) {
            return true;
        }
        return false;
    }

    public boolean containsAny(Point... points) {
        return containsAny(Arrays.asList(points));
    }

    public boolean containsAny(List<Point> points) {
        for (Point p : points) {
            if (contains(p)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Point... points) {
        return containsAll(Arrays.asList(points));
    }

    public boolean containsAll(List<Point> points) {
        for (Point p : points) {
            if (!contains(p)) {
                return false;
            }
        }
        return true;
    }

}

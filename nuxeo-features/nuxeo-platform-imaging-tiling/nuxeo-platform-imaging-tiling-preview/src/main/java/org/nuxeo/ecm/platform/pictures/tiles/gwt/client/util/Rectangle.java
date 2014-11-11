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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
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
        Point bottomRight = new Point(topLeft.getX() + width, topLeft.getY()
                + height);
        if (p.getX() >= topLeft.getX() && p.getX() <= bottomRight.getX()
                && p.getY() >= topLeft.getY() && p.getY() <= bottomRight.getY()) {
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

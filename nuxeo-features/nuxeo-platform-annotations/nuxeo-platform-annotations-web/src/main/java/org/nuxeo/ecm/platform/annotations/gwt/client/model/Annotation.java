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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointer;

import com.google.gwt.i18n.client.DateTimeFormat;

/**
 * @author Alexandre Russel
 *
 */
public class Annotation {
    private String uuid;

    private XPointer xpointer;

    private boolean isBodyUrl;

    private Date date;

    private String formattedDate = "";

    private String author;

    private String body;

    private String type;

    private int id;

    private Map<String, String> fields = new HashMap<String, String>();

    private Container startContainer;

    private Container endContainer;

    public Annotation(String uuid) {
        this.uuid = uuid;
    }

    public Annotation() {
    }

    public String getUUID() {
        return uuid;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setStringDate(String stringDate) {
        date = computeDate(stringDate);
        formattedDate = DateTimeFormat.getShortDateFormat().format(date);
    }

    public Annotation(XPointer xpointer) {
        this.xpointer = xpointer;
    }

    public boolean isBodyUrl() {
        return isBodyUrl;
    }

    public void setBodyUrl(boolean isBodyUrl) {
        this.isBodyUrl = isBodyUrl;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public String getShortType() {
        return type.substring(type.lastIndexOf("#") + 1);
    }

    public void setType(String type) {
        this.type = type;
    }

    public XPointer getXpointer() {
        return xpointer;
    }

    public String serialize() {
        return type + ' ' + xpointer + ' ' + body;
    }

    public void setXpointer(XPointer xpointer) {
        this.xpointer = xpointer;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public Date getDate() {
        return date;
    }

    @SuppressWarnings("deprecation")
    private static Date computeDate(String stringDate) {
        String d = stringDate.substring(0, stringDate.indexOf("T"));
        String t = stringDate.substring(stringDate.indexOf("T") + 1,
                stringDate.indexOf("Z"));
        String[] ds = d.split("-");
        String[] ts = t.split(":");
        Date now = new Date();
        int second = ts.length == 3 ? Integer.parseInt(ts[2]) : 0;
        now = new Date(Date.UTC(Integer.parseInt(ds[0]) - 1900,
                Integer.parseInt(ds[1]) - 1, Integer.parseInt(ds[2]),
                Integer.parseInt(ts[0]), Integer.parseInt(ts[1]), second)
                + now.getTimezoneOffset());
        return now;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public Container getStartContainer() {
        return startContainer;
    }

    public void setStartContainer(Container container) {
        this.startContainer = container;
    }

    public boolean hasStartContainer() {
        return startContainer != null;
    }

    public Container getEndContainer() {
        return endContainer;
    }

    public void setEndContainer(Container container) {
        this.endContainer = container;
    }

    public boolean hasEndContainer() {
        return endContainer != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Annotation)) {
            return false;
        }

        Annotation annotation = (Annotation) obj;
        return xpointer.equals(annotation.xpointer)
                && author.equals(annotation.author);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 17 * xpointer.hashCode();
        result += 17 * author.hashCode();
        return result;
    }

}

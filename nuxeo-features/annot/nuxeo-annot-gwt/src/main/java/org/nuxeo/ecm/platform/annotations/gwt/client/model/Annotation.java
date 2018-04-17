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
        String dateFormatPattern = getDateFormatPattern();
        DateTimeFormat dateTimeFormat = dateFormatPattern != null ? DateTimeFormat.getFormat(dateFormatPattern)
                : DateTimeFormat.getShortDateFormat();
        formattedDate = dateTimeFormat.format(date);
    }

    private native String getDateFormatPattern() /*-{
                                                 return top['dateFormatPattern'];
                                                 }-*/;

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
        String t = stringDate.substring(stringDate.indexOf("T") + 1, stringDate.indexOf("Z"));
        String[] ds = d.split("-");
        String[] ts = t.split(":");
        Date now = new Date();
        int second = ts.length == 3 ? Integer.parseInt(ts[2]) : 0;
        now = new Date(Date.UTC(Integer.parseInt(ds[0]) - 1900, Integer.parseInt(ds[1]) - 1, Integer.parseInt(ds[2]),
                Integer.parseInt(ts[0]), Integer.parseInt(ts[1]), second) + now.getTimezoneOffset());
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
        startContainer = container;
    }

    public boolean hasStartContainer() {
        return startContainer != null;
    }

    public Container getEndContainer() {
        return endContainer;
    }

    public void setEndContainer(Container container) {
        endContainer = container;
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
        return xpointer.equals(annotation.xpointer) && author.equals(annotation.author);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 17 * xpointer.hashCode();
        result += 17 * author.hashCode();
        return result;
    }

}

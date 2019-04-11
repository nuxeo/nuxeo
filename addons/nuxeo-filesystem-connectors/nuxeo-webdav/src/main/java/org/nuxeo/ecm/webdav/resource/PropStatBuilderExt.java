/*
 * Copyright 2008, 2009 Daniel MANZKE
 *
 * This file is part of webdav-jaxrs.
 *
 * webdav-jaxrs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * webdav-jaxrs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with webdav-jaxrs.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nuxeo.ecm.webdav.resource;

import net.java.dev.webdav.jaxrs.xml.elements.Prop;
import net.java.dev.webdav.jaxrs.xml.elements.PropStat;
import net.java.dev.webdav.jaxrs.xml.properties.*;

import org.nuxeo.ecm.webdav.jaxrs.IsHidden;
import org.w3c.dom.Element;

import javax.ws.rs.core.Response.Status;

import java.util.*;

/**
 * Copy-pasted then modified from jaxrs-webdav.
 */
public class PropStatBuilderExt {
    private List<Object> properties;

    private Status status;

    private Set<String> names;

    public PropStatBuilderExt() {
        properties = new LinkedList<>();
        names = new HashSet<>();
    }

    public PropStatBuilderExt creationDate(Date dateTime) {
        if (!names.contains("creationdate")) {
            CreationDate create = new CreationDate(dateTime);
            properties.add(create);
            names.add("creationdate");
        }

        return this;
    }

    public PropStatBuilderExt lastModified(Date dateTime) {
        if (!names.contains("getlastmodified")) {
            GetLastModified lastModified = new GetLastModified(dateTime);
            properties.add(lastModified);
            names.add("getlastmodified");
        }

        return this;
    }

    public PropStatBuilderExt contentLength(long length) {
        if (!names.contains("getcontentlength")) {
            GetContentLength contentLength = new GetContentLength(length);
            properties.add(contentLength);
            names.add("getcontentlength");
        }

        return this;
    }

    public PropStatBuilderExt isResource(long length, String mime) {
        if (!names.contains("getcontenttype")) {
            GetContentType type = new GetContentType(mime);
            properties.add(type);
            names.add("getcontenttype");
            GetContentLength contentLength = new GetContentLength(length);
            properties.add(contentLength);
            names.add("getcontentlength");
        }

        return this;
    }

    public PropStatBuilderExt isCollection() {
        if (!names.contains("resourcetype")) {
            ResourceType type = ResourceType.COLLECTION;
            properties.add(type);
            names.add("resourcetype");
        }

        return this;
    }

    public PropStatBuilderExt displayName(String displayName) {
        if (!names.contains("displayname")) {
            DisplayName name = new DisplayName(displayName);
            properties.add(name);
            names.add("displayname");
        }

        return this;
    }

    public PropStatBuilderExt isHidden(boolean hide) {
        if (!names.contains("ishidden")) {
            IsHidden hidden = new IsHidden(hide ? 1 : 0);
            properties.add(hidden);
            names.add("ishidden");
        }

        return this;
    }

    public PropStat notFound(Prop allprops) {
        boolean empty = true;
        List<Object> notFound = new ArrayList<>();
        for (Object prop : allprops.getProperties()) {
            if (prop instanceof Element) {
                Element element = (Element) prop;
                String name = element.getLocalName();
                if (!names.contains(name)) {
                    notFound.add(prop);
                    empty = false;
                }
            }
        }

        PropStat stat = null;
        if (!empty) {
            Object[] objects = notFound.toArray(new Object[properties.size()]);
            Prop prop = new Prop(objects);
            stat = new PropStat(prop, new net.java.dev.webdav.jaxrs.xml.elements.Status(Status.NOT_FOUND));
        }

        return stat;
    }

    public PropStatBuilderExt status(Status status) {
        this.status = status;

        return this;
    }

    public PropStat build() {
        Object[] objects = properties.toArray(new Object[properties.size()]);
        Prop prop = new Prop(objects);
        PropStat stat = new PropStat(prop, new net.java.dev.webdav.jaxrs.xml.elements.Status(status));

        return stat;
    }
}

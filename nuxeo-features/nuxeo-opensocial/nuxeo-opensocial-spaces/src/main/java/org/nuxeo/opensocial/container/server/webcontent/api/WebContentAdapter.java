package org.nuxeo.opensocial.container.server.webcontent.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * Adapter around WebContent. WebContent is the base class for all container
 * parts. It exposes some basic properties that can be used by the container It
 * also wraps bidirectionnaly a WebContentData that is a simple Java bean that
 * is usable by the GWT part.
 *
 * @author Stéphane Fourrier
 */
public interface WebContentAdapter<T extends WebContentData> {
    public void feedFrom(T data) throws ClientException;

    public T getData() throws ClientException;

    public String getTitle() throws ClientException;

    public void setTitle(String title) throws ClientException;

    public long getPosition() throws ClientException;

    public void setPosition(long position) throws ClientException;

    public boolean isInAPortlet() throws ClientException;

    public void setInAPortlet(boolean isInAPortlet) throws ClientException;

    public boolean isCollapsed() throws ClientException;

    public void setCollapsed(boolean isCollapsed) throws ClientException;
}

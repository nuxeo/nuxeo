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
    void feedFrom(T data) throws ClientException;

    T getData() throws ClientException;

    String getTitle() throws ClientException;

    void setTitle(String title) throws ClientException;

    long getPosition() throws ClientException;

    void setPosition(long position) throws ClientException;

    boolean isInAPortlet() throws ClientException;

    void setInAPortlet(boolean isInAPortlet) throws ClientException;

    boolean isCollapsed() throws ClientException;

    void setCollapsed(boolean isCollapsed) throws ClientException;

    void update() throws ClientException;

}

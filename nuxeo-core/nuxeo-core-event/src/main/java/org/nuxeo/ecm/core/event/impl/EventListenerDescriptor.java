/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.script.Script;
import org.nuxeo.ecm.core.event.script.ScriptingEventListener;
import org.nuxeo.ecm.core.event.script.ScriptingPostCommitEventListener;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * XObject descriptor to declare event listeners
 */
@XObject("listener")
public class EventListenerDescriptor {

    public static final Log log = LogFactory.getLog(EventListenerDescriptor.class);

    @XNode("@name")
    protected String name;

    /**
     * The event listener class.
     */
    @XNode("@class")
    protected Class<?> clazz;

    /**
     * A script reference: URL, file path, or bundle entry.
     * Runtime variable are expanded. To specify a bundle entry use the URL schema "bundle:"
     */
    @XNode("@script")
    protected String script;

    /**
     * Applies only for scripts.
     */
    @XNode("@postCommit")
    protected boolean isPostCommit;

    /**
     * Applies only for post commit listener
     */
    @XNode("@async")
    protected Boolean isAsync;

    @XNode("@transactionTimeOut")
    protected Integer transactionTimeOut;

    /**
     * The priority to be used to order listeners.
     */
    @XNode("@priority")
    protected Integer priority;

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@retryCount")
    protected Integer retryCount;

    @XNode("@singlethread")
    protected boolean singleThreaded = false;

    protected Set<String> events;

    protected RuntimeContext rc;

    protected EventListener inLineListener;

    protected PostCommitEventListener postCommitEventListener;

    public int getPriority() {
        return priority == null ? 0 : priority.intValue();
    }

    public void setRuntimeContext(RuntimeContext rc) {
        this.rc = rc;
    }

    public RuntimeContext getRuntimeContext() {
        return rc;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Set<String> getEvents() {
        return events;
    }

    @XNodeList(value = "event", componentType = String.class, type = HashSet.class, nullByDefault = true)
    public void setEvents(Set<String> events) {
        this.events = events.isEmpty() ? null : events;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void initListener() throws Exception {
        if (clazz != null) {
            if (EventListener.class.isAssignableFrom(clazz)) {
                inLineListener = (EventListener) clazz.newInstance();
                isPostCommit = false;
            } else if (PostCommitEventListener.class.isAssignableFrom(clazz)) {
                postCommitEventListener = (PostCommitEventListener) clazz.newInstance();
                isPostCommit = true;
            }
        } else if (script != null) {
            if (isPostCommit) {
                postCommitEventListener = new ScriptingPostCommitEventListener(getScript());
            } else {
                inLineListener = new ScriptingEventListener(getScript());
            }
        } else {
            throw new IllegalArgumentException("Listener extension must define either a class or a script");
        }
    }

    public EventListener asEventListener() {
        return inLineListener;
    }

    public PostCommitEventListener asPostCommitListener() {
        return postCommitEventListener;
    }

    public Script getScript() throws Exception {
        if (rc != null) {
            URL url = rc.getBundle().getEntry(script);
            if (url == null) {
                // if not found using bundle entries try using classloader
                // in a test environment bundle entries may not work
                url = rc.getResource(script);
                if (url == null) {
                    throw new Exception("Script Not found: " + script);
                }
            }
            return Script.newScript(url);
        } else {
            return Script.newScript(script);
        }
    }

    public String getName() {
        if (name == null) {
            if (clazz != null) {
                name = clazz.getSimpleName();
            } else {
                name = script;
            }
        }
        return name;
    }

    public Integer getTransactionTimeout() {
        return transactionTimeOut;
    }


    public void merge(EventListenerDescriptor other) {

        this.isEnabled = other.isEnabled;

        if (other.clazz != null) {
            this.clazz = other.clazz;
            this.rc = other.rc;
        } else if (other.script != null) {
            this.script = other.script;
            this.clazz = null;
            this.rc = other.rc;
        }

        if (other.isAsync != null) {
            this.isAsync = other.isAsync;
        }

        if (other.events != null) {
            this.events = other.events;
        }

        if (other.transactionTimeOut != null) {
            this.transactionTimeOut = other.transactionTimeOut;
        }

        if (other.priority != null) {
            this.priority = other.priority;
        }

        if (other.retryCount != null) {
            this.retryCount = other.retryCount;
        }
    }

    public final boolean acceptEvent(String eventName) {
        return events == null || events.contains(eventName);
    }

    public void setIsAsync(Boolean isAsync) {
        this.isAsync = isAsync;
    }

    public boolean getIsAsync() {
        return isAsync == null ? false : isAsync.booleanValue();
    }

    public boolean isSingleThreaded() {
        return singleThreaded;
    }

    /**
     * Filters the event bundle to only keep events of interest to this
     * listener.
     *
     * @since 5.7
     */
    public EventBundle filterBundle(EventBundle bundle) {
        EventBundle filtered = new EventBundleImpl();
        for (Event event : bundle) {
            if (!acceptEvent(event.getName())) {
                continue;
            }
            PostCommitEventListener pcl = asPostCommitListener();
            if (pcl instanceof PostCommitFilteringEventListener
                    && !((PostCommitFilteringEventListener) pcl).acceptEvent(event)) {
                continue;
            }
            filtered.push(event);
        }
        return filtered;
    }

    /**
     * Checks if there's at least one event of interest in the bundle.
     *
     * @since 5.7
     */
    public boolean acceptBundle(EventBundle bundle) {
        for (Event event : bundle) {
            if (!acceptEvent(event.getName())) {
                continue;
            }
            PostCommitEventListener pcl = asPostCommitListener();
            if (pcl instanceof PostCommitFilteringEventListener
                    && !((PostCommitFilteringEventListener) pcl).acceptEvent(event)) {
                continue;
            }
            return true;
        }
        return false;
    }

}

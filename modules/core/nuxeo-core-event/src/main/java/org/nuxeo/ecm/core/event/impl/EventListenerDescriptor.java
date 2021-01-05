/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.ecm.core.api.NuxeoException;
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
@XRegistry(enable = false)
public class EventListenerDescriptor {

    public static final Log log = LogFactory.getLog(EventListenerDescriptor.class);

    @XNode("@name")
    protected String name;

    /**
     * The event listener class.
     */
    @XNode("@class")
    protected String className;

    /**
     * A script reference: URL, file path, or bundle entry. Runtime variable are expanded. To specify a bundle entry use
     * the URL schema "bundle:"
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

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean isEnabled = true;

    @XNode("@retryCount")
    protected Integer retryCount;

    @XNode("@singlethread")
    protected boolean singleThreaded = false;

    @XNodeList(value = "event", componentType = String.class, type = HashSet.class, nullByDefault = true)
    protected Set<String> events;

    protected RuntimeContext context;

    protected EventListener inLineListener;

    protected PostCommitEventListener postCommitEventListener;

    public int getPriority() {
        return priority == null ? 0 : priority.intValue();
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

    public void initListener(Context context) {
        try {
            if (className != null) {
                Class<?> klass = context.loadClass(className);
                if (EventListener.class.isAssignableFrom(klass)) {
                    inLineListener = (EventListener) klass.getDeclaredConstructor().newInstance();
                    isPostCommit = false;
                } else if (PostCommitEventListener.class.isAssignableFrom(klass)) {
                    postCommitEventListener = (PostCommitEventListener) klass.getDeclaredConstructor().newInstance();
                    isPostCommit = true;
                } else {
                    throw new IllegalArgumentException(
                            "Listener extension must define a class extending EventListener or PostCommitEventListener: '"
                                    + className + "'.");
                }
            } else if (script != null) {
                Script script = getScript(context);
                if (isPostCommit) {
                    postCommitEventListener = new ScriptingPostCommitEventListener(script);
                } else {
                    inLineListener = new ScriptingEventListener(script);
                }
            } else {
                throw new IllegalArgumentException("Listener extension must define either a class or a script");
            }
        } catch (ReflectiveOperationException | NoClassDefFoundError | IOException e) {
            throw new NuxeoException(e);
        }
    }

    public EventListener asEventListener() {
        return inLineListener;
    }

    public PostCommitEventListener asPostCommitListener() {
        return postCommitEventListener;
    }

    public Script getScript(Context context) throws IOException {
        if (context != null) {
            URL url = context.getResource(script);
            if (url == null) {
                throw new IOException("Script Not found: " + script);
            }
            return Script.newScript(url);
        } else {
            return Script.newScript(script);
        }
    }

    public String getName() {
        if (name == null) {
            if (className != null) {
                name = className;
            } else {
                name = script;
            }
        }
        return name;
    }

    public Integer getTransactionTimeout() {
        return transactionTimeOut;
    }

    public final boolean acceptEvent(String eventName) {
        return events == null || events.contains(eventName);
    }

    public boolean getIsAsync() {
        return Boolean.TRUE.equals(isAsync);
    }

    public boolean isSingleThreaded() {
        return singleThreaded;
    }

    /**
     * Filters the event bundle to only keep events of interest to this listener.
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

    @Override
    public String toString() {
        return "EventListenerDescriptor [name=" + name + ", className=" + className + ", isPostCommit=" + isPostCommit
                + ", isAsync=" + isAsync + "]";
    }

}

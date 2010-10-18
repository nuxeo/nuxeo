/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.storage.sql.InvalidationsQueue;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Mapper.Identification;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.RepositoryResolver;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;

/**
 * Servlet receiving remote {@link MapperClient} requests and sending them to an
 * actual mapper.
 */
public class MapperServlet extends HttpServlet {

    public static final String SERVER_THREAD_NAME_PREFIX = "Nuxeo-VCS-Server-";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MapperServlet.class);

    public static final String PARAM_RID = "rid";

    public static final String PARAM_MID = "mid";

    private final AtomicLong repositoryCounter = new AtomicLong(0);

    private final String repositoryName;

    private Repository repository;

    /** Event invalidations to return to each repository. */
    private final Map<String, InvalidationsQueue> eventQueues;

    public MapperServlet(String repositoryName) {
        this.repositoryName = repositoryName;
        eventQueues = Collections.synchronizedMap(new HashMap<String, InvalidationsQueue>());
    }

    public static String getName(String repositoryName) {
        return MapperServlet.class.getSimpleName() + '-' + repositoryName;
    }

    private boolean initialized;

    // currently connected sessions
    // TODO GC after timeout
    private Map<String, MapperInvoker> invokers;

    protected synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        repository = RepositoryResolver.getRepository(repositoryName);
        invokers = Collections.synchronizedMap(new HashMap<String, MapperInvoker>());
    }

    @Override
    public void destroy() {
        if (invokers != null) {
            for (Entry<String, MapperInvoker> es : invokers.entrySet()) {
                MapperInvoker invoker = es.getValue();
                try {
                    invoker.call(Mapper.CLOSE);
                    invoker.close();
                } catch (Throwable e) {
                    log.error("Cannot close invoker " + es.getKey());
                }
            }
        }
        super.destroy();
    }

    private final AtomicInteger threadNumber = new AtomicInteger();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        initialize();
        String rid = req.getParameter(PARAM_RID);
        if (rid == null || "".equals(rid)) {
            // assign a new repositoryId. client calls this from a synchronized
            // method
            rid = "" + repositoryCounter.incrementAndGet();
        }
        String mid = req.getParameter(PARAM_MID);
        if ("".equals(mid)) {
            mid = null;
        }
        InputStream is = req.getInputStream();
        try {
            // invoker
            MapperInvoker invoker;
            if (mid == null) {
                // new session
                String name = SERVER_THREAD_NAME_PREFIX
                        + threadNumber.incrementAndGet();
                InvalidationsQueue eventQueue = eventQueues.get(rid);
                if (eventQueue == null) {
                    eventQueues.put(rid, eventQueue = new InvalidationsQueue(
                            "servlet-for-" + rid));
                }
                String remoteIP = req.getRemoteAddr();
                String remotePrincipal = req.getHeader("X-Nuxeo-Principal");
                invoker = new MapperInvoker(repository, name, eventQueue, new MapperClientInfo(remoteIP, remotePrincipal));
                Identification id = (Identification) invoker.call(Mapper.GET_IDENTIFICATION);
                mid = id.mapperId;
                invokers.put(mid, invoker);
            } else {
                // existing session
                invoker = invokers.get(mid);
                if (invoker == null) {
                    throw new RuntimeException(
                            "Unknown session id (maybe timed out): " + mid);
                }
            }
            invoker.clientInfo.handledRequest(req);
            // set up output stream
            resp.setContentType("application/octet-stream");
            // resp.setCharacterEncoding("ISO-8859-1"); // important
            Writer writer = resp.getWriter();
            ObjectOutputStream oos = new ObjectOutputStream(
                    new OutputStreamToWriter(writer));

            // read method and args
            ObjectInputStream ois = new ObjectInputStream(is);
            String methodName = (String) ois.readObject();
            List<Object> args = new LinkedList<Object>();
            while (true) {
                Object object = ois.readObject();
                if (object == MapperClient.EOF) {
                    break;
                }
                args.add(object);
            }

            // invoke method
            Object res = invoker.call(methodName, args.toArray());
            // close?
            if (Mapper.CLOSE.equals(methodName)) {
                // close session
                invoker.close();
                invokers.remove(mid);
            }
            // getIdentification
            else if (Mapper.GET_IDENTIFICATION.equals(methodName)) {
                // add repositoryId to identification
                Identification id = (Identification) res;
                res = new Identification(rid, id.mapperId);
            }
            // write result
            oos.writeObject(res);
            oos.flush();
            oos.close();
        } catch (Throwable e) {
            log.error(e, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.toString());
        }
    }

    public Collection<MapperClientInfo> getClientInfos() {
        if (invokers == null) {
            return Collections.emptyList();
        }
        List<MapperClientInfo> infos = new ArrayList<MapperClientInfo>(invokers.size());
        for (MapperInvoker invoker : invokers.values()) {
            infos.add(invoker.clientInfo);
        }
        return infos;
    }
}

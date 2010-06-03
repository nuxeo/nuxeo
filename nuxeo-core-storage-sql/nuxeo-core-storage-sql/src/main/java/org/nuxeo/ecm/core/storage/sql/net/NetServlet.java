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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;

/**
 * Servlet receiving remote {@link NetMapper} requests and sending them to an
 * actual mapper.
 */
public class NetServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(NetServlet.class);

    private static final long serialVersionUID = 1L;

    private String repositoryName;

    private Repository repository;

    public NetServlet(RepositoryDescriptor repositoryDescriptor) {
        repositoryName = repositoryDescriptor.name;
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
        org.nuxeo.ecm.core.model.Repository repo;
        try {
            try {
                repo = NXCore.getRepository(repositoryName);
            } catch (NoSuchRepositoryException e) {
                // No JDNI binding (embedded or unit tests)
                repo = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                        repositoryName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (repo instanceof Repository) {
            // (JCA) ConnectionFactoryImpl already implements Repository
            repository = (Repository) repo;
        } else if (repo instanceof SQLRepository) {
            // (LocalSession not pooled) SQLRepository
            // from SQLRepositoryFactory called by descriptor at registration
            repository = ((SQLRepository) repo).repository;
        } else {
            throw new RuntimeException("Unknown repository class: "
                    + repo.getClass().getName());
        }
        invokers = Collections.synchronizedMap(new HashMap<String, MapperInvoker>());
    }

    @Override
    public void destroy() {
        if (invokers != null) {
            for (Entry<String, MapperInvoker> es : invokers.entrySet()) {
                MapperInvoker invoker = es.getValue();
                try {
                    invoker.call("close");
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
        String sid = req.getParameter("sid");
        if ("".equals(sid)) {
            sid = null;
        }
        InputStream is = req.getInputStream();
        try {
            // invoker
            MapperInvoker invoker;
            if (sid == null) {
                // new session
                String name = "Nuxeo-VCS-NetServlet-"
                        + threadNumber.incrementAndGet();
                invoker = new MapperInvoker(repository, name);
                sid = (String) invoker.call("getMapperId");
                // log.info("New sid " + sid);
                // System.out.println("New sid " + sid + " thread " + name);
                invokers.put(sid, invoker);
            } else {
                // existing session
                invoker = invokers.get(sid);
                if (invoker == null) {
                    throw new RuntimeException(
                            "Unknown session id (maybe timed out): " + sid);
                }
            }

            // set up output stream
            resp.setContentType("application/octet-stream");
            // resp.setCharacterEncoding("ISO-8859-1"); // important
            Writer writer = resp.getWriter();
            ObjectOutputStream oos = new ObjectOutputStream(
                    new OutputStreamToWriter(writer));

            // read method and args
            ObjectInputStream ois = new ObjectInputStream(is);
            String methodName = (String) ois.readObject();
            // log.info("  Sid " + sid + " method " + methodName);
            List<Object> args = new LinkedList<Object>();
            while (true) {
                Object object = ois.readObject();
                if (object == NetMapper.EOF) {
                    break;
                }
                args.add(object);
            }
            // System.out.println(sid + " " + methodName + " " + args);

            // invoke method
            Object res = invoker.call(methodName, args.toArray());
            // System.out.println("  -> " + res);

            // close?
            if ("close".equals(methodName)) {
                // close session
                invoker.close();
                invokers.remove(sid);
                // log.info("Closing sid " + sid);
                // System.out.println("Closing sid " + sid);
            }

            // write result
            oos.writeObject(res);
            oos.flush();
            oos.close();
        } catch (Throwable e) {
            // e.printStackTrace(); // System.out.println
            log.error(e, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.toString());
        }
    }

}

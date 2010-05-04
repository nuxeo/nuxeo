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

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.Session;
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
    private Map<String, Session> sessions;

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
        sessions = Collections.synchronizedMap(new HashMap<String, Session>());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        initialize();
        String methodName = req.getParameter("method");
        String sid = req.getParameter("sid");
        Session session = null;
        try {
            // method and arguments
            ObjectInputStream ois = new ObjectInputStream(req.getInputStream());
            ArrayList<Object> args = new ArrayList<Object>();
            try {
                while (true) {
                    args.add(ois.readObject());
                }
            } catch (EOFException e) {
                // stop
            }
            ois.close();

            // session
            if (sid == null) {
                session = repository.getConnection();
            } else {
                session = sessions.get(sid);
                if (session == null) {
                    throw new RuntimeException(
                            "Unknown session id (maybe timed out): " + sid);
                }
            }
            Mapper mapper = session.getMapper();
            if (sid == null) {
                sid = mapper.getMapperId();
                sessions.put(sid, session);
            }

            // set up output stream
            resp.setContentType("application/octet-stream");
            // resp.setCharacterEncoding("ISO-8859-1"); // important
            Writer writer = resp.getWriter();
            ObjectOutputStream oos = new ObjectOutputStream(
                    new OutputStreamToWriter(writer));

            // invoke method, special case for close
            Object res;
            if ("close".equals(methodName)) {
                session.close();
                sessions.remove(sid);
                res = null;
            } else {
                res = invoke(mapper, methodName, args.toArray());
            }

            // write result
            oos.writeObject(res);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            log.error(e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.toString());
        }
    }

    private static final Map<String, Method> mapperMethods = new HashMap<String, Method>();
    static {
        for (Method m : Mapper.class.getMethods()) {
            mapperMethods.put(m.getName(), m);
        }
    }

    protected static Object invoke(Mapper mapper, String methodName,
            Object[] args) throws StorageException, IOException {
        Method method = mapperMethods.get(methodName);
        if (method == null) {
            throw new StorageException("Unknown Mapper method: " + methodName);
        }
        try {
            return method.invoke(mapper, args);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                throw new StorageException(e.getCause());
            }
            throw new StorageException(e);
        }
    }

}

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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.abdera;

import java.util.HashMap;

import javax.activation.MimeType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.Request;
import org.apache.abdera.protocol.Resolver;
import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.MediaCollectionAdapter;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetBuilder;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.Transactional;
import org.apache.abdera.protocol.server.WorkspaceManager;
import org.apache.abdera.protocol.server.impl.AbstractProvider;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AbderaService extends AbstractProvider implements TargetBuilder, Resolver<Target> {

    private static final AbderaService instance = new AbderaService();

    public final static AbderaService getInstance() {
        return instance;
    }

    protected AbderaService() {
        init(new Abdera(), new HashMap<String, String>());
    }

    @Override
    protected TargetBuilder getTargetBuilder(RequestContext request) {
        return this;
    }

    @Override
    protected Resolver<Target> getTargetResolver(RequestContext request) {
        return this;
    }

    @Override
    protected WorkspaceManager getWorkspaceManager(RequestContext request) {
        throw new UnsupportedOperationException("Not Suported");
    }

    @Override
    public String urlFor(RequestContext request, Object key, Object param) {
        return request.urlFor(key, param);
    }

    public Target resolve(Request request) {
        //this is called in AbderaRequest ctor so we need to init target here
        return ((AbderaRequest)request).getTarget();
    }



    public AbderaRequest createFeedRequest(WebContext ctx) {
        return new AbderaRequest(TargetType.TYPE_COLLECTION, this, ctx);
    }

    public AbderaRequest createEntryRequest(WebContext ctx) {
        return new AbderaRequest(TargetType.TYPE_ENTRY, this, ctx);
    }

    public AbderaRequest createMediaRequest(WebContext ctx) {
        return new AbderaRequest(TargetType.TYPE_MEDIA, this, ctx);
    }

    public Response getResponse(ResponseContext context) {
        if (context == null) {
            return Response.status(500).build();
        }
        ResponseBuilder builder = Response.status(context.getStatus());
        long cl = context.getContentLength();
        String cc = context.getCacheControl();
        if (cl > -1) {
            builder.header("Content-Length", cl);
        }
        if (cc != null && cc.length() > 0) {
            builder.header("Cache-Control",cc);
        }
        MimeType ct = context.getContentType();
        if (ct != null) {
            builder.type(ct.toString());
        }
        String[] names = context.getHeaderNames();
        for (String name : names) {
            Object[] headers = context.getHeaders(name);
            for (Object value : headers) {
                builder.header(name, value);
//                if (value instanceof Date) {
//                    //TODO format header field here?
//                    builder.header(name, ((Date)value).getTime());
//                } else {
//                    builder.header(name, value.toString());
//                }
            }
        }
        return builder.entity(context).build();
    }


    public final static Response getFeed(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.getFeed(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response postEntry(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.postEntry(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response getEntry(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.getEntry(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response putEntry(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.putEntry(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response deleteEntry(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.deleteEntry(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response headEntry(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.headEntry(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response optionsEntry(WebContext ctx, CollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.optionsEntry(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response getMedia(WebContext ctx, MediaCollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.getMedia(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response putMedia(WebContext ctx, MediaCollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.putMedia(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response postMedia(WebContext ctx, MediaCollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.postMedia(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response deleteMedia(WebContext ctx, MediaCollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.deleteMedia(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response headMedia(WebContext ctx, MediaCollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.headMedia(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

    public final static Response optionsMedia(WebContext ctx, MediaCollectionAdapter adapter) {
        AbderaRequest request = getInstance().createFeedRequest(ctx);
        ResponseContext rc = null;
        Transactional tx = null;
        try {
            if (adapter instanceof Transactional) {
                tx =(Transactional)adapter;
                tx.start(request);
            }
            rc = adapter.optionsMedia(request);
            return getInstance().getResponse(rc);
        } catch (Throwable t) {
            if (tx != null) {
                tx.compensate(request, t);
                tx = null;
            }
            throw WebException.wrap(t);
        } finally {
            if (tx != null) {
                tx.end(request, rc);
            }
        }
    }

}

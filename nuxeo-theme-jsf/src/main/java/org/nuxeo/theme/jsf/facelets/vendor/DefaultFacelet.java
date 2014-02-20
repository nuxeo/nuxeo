/**
 * Licensed under the Common Development and Distribution License,
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.sun.com/cddl/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.nuxeo.theme.jsf.facelets.vendor;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;

import org.nuxeo.theme.ResourceResolver;
import org.nuxeo.theme.jsf.FacesResourceResolver;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Default Facelet implementation. Copied from facelets-1.1.15.B1 by Jean-Marc
 * Orliaguet <jmo@chalmers.se> - class made public 2010/10/24.
 *
 * @author Jacob Hookom
 * @version $Id: DefaultFacelet.java,v 1.9 2006/04/03 05:10:38 jhook Exp $
 */
public final class DefaultFacelet extends Facelet {

    private final Logger log = Logger.getLogger("facelets.facelet");

    private final static String APPLIED_KEY = "com.sun.facelets.APPLIED";

    private final String alias;

    private final ExpressionFactory elFactory;

    private final DefaultFaceletFactory factory;

    private final long createTime;

    private final long refreshPeriod;

    private final Map relativePaths;

    private final FaceletHandler root;

    private final URL src;

    public DefaultFacelet(DefaultFaceletFactory factory, ExpressionFactory el,
            URL src, String alias, FaceletHandler root) {
        this.factory = factory;
        this.elFactory = el;
        this.src = src;
        this.root = root;
        this.alias = alias;
        this.createTime = System.currentTimeMillis();
        this.refreshPeriod = this.factory.getRefreshPeriod();
        this.relativePaths = new WeakHashMap();
    }

    /**
     * @see com.sun.facelets.Facelet#apply(javax.faces.context.FacesContext,
     *      javax.faces.component.UIComponent)
     */
    @Override
    public void apply(FacesContext facesContext, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        DefaultFaceletContext ctx = new DefaultFaceletContext(facesContext,
                this);
        this.refresh(parent);
        ComponentSupport.markForDeletion(parent);
        ResourceResolver.setInstance(new FacesResourceResolver(
                facesContext.getExternalContext()));
        try {
            this.root.apply(ctx, parent);
        } finally {
            ResourceResolver.setInstance(null);
        }
        ComponentSupport.finalizeForDeletion(parent);
        this.markApplied(parent);
    }

    private final void refresh(UIComponent c) {
        if (this.refreshPeriod > 0) {

            // finally remove any children marked as deleted
            int sz = c.getChildCount();
            if (sz > 0) {
                UIComponent cc = null;
                List cl = c.getChildren();
                ApplyToken token;
                while (--sz >= 0) {
                    cc = (UIComponent) cl.get(sz);
                    if (!cc.isTransient()) {
                        token = (ApplyToken) cc.getAttributes().get(APPLIED_KEY);
                        if (token != null && token.time < this.createTime
                                && token.alias.equals(this.alias)) {
                            if (log.isLoggable(Level.INFO)) {
                                DateFormat df = SimpleDateFormat.getTimeInstance();
                                log.info("Facelet[" + this.alias
                                        + "] was modified @ "
                                        + df.format(new Date(this.createTime))
                                        + ", flushing component applied @ "
                                        + df.format(new Date(token.time)));
                            }
                            cl.remove(sz);
                        }
                    }
                }
            }

            // remove any facets marked as deleted
            if (c.getFacets().size() > 0) {
                Collection col = c.getFacets().values();
                UIComponent fc;
                ApplyToken token;
                for (Iterator itr = col.iterator(); itr.hasNext();) {
                    fc = (UIComponent) itr.next();
                    if (!fc.isTransient()) {
                        token = (ApplyToken) fc.getAttributes().get(APPLIED_KEY);
                        if (token != null && token.time < this.createTime
                                && token.alias.equals(this.alias)) {
                            if (log.isLoggable(Level.INFO)) {
                                DateFormat df = SimpleDateFormat.getTimeInstance();
                                log.info("Facelet[" + this.alias
                                        + "] was modified @ "
                                        + df.format(new Date(this.createTime))
                                        + ", flushing component applied @ "
                                        + df.format(new Date(token.time)));
                            }
                            itr.remove();
                        }
                    }
                }
            }
        }
    }

    private final void markApplied(UIComponent parent) {
        if (this.refreshPeriod > 0) {
            Iterator itr = parent.getFacetsAndChildren();
            UIComponent c;
            Map attr;
            ApplyToken token = new ApplyToken(this.alias,
                    System.currentTimeMillis() + this.refreshPeriod);
            while (itr.hasNext()) {
                c = (UIComponent) itr.next();
                if (!c.isTransient()) {
                    attr = c.getAttributes();
                    if (!attr.containsKey(APPLIED_KEY)) {
                        attr.put(APPLIED_KEY, token);
                    }
                }
            }
        }
    }

    /**
     * Return the alias name for error messages and logging
     *
     * @return alias name
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * Return this Facelet's ExpressionFactory instance
     *
     * @return internal ExpressionFactory instance
     */
    public ExpressionFactory getExpressionFactory() {
        return this.elFactory;
    }

    /**
     * The time when this Facelet was created, NOT the URL source code
     *
     * @return final timestamp of when this Facelet was created
     */
    public long getCreateTime() {
        return this.createTime;
    }

    /**
     * Delegates resolution to DefaultFaceletFactory reference. Also, caches
     * URLs for relative paths.
     *
     * @param path a relative url path
     * @return URL pointing to destination
     * @throws IOException if there is a problem creating the URL for the path
     *             specified
     */
    private URL getRelativePath(String path) throws IOException {
        URL url = (URL) this.relativePaths.get(path);
        if (url == null) {
            url = this.factory.resolveURL(this.src, path);
            this.relativePaths.put(path, url);
        }
        return url;
    }

    /**
     * The URL this Facelet was created from.
     *
     * @return the URL this Facelet was created from
     */
    public URL getSource() {
        return this.src;
    }

    /**
     * Given the passed FaceletContext, apply our child FaceletHandlers to the
     * passed parent
     *
     * @see FaceletHandler#apply(FaceletContext, UIComponent)
     * @param ctx the FaceletContext to use for applying our FaceletHandlers
     * @param parent the parent component to apply changes to
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    private void include(DefaultFaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        this.refresh(parent);
        this.root.apply(new DefaultFaceletContext(ctx, this), parent);
        this.markApplied(parent);
    }

    /**
     * Used for delegation by the DefaultFaceletContext. First pulls the URL
     * from {@link #getRelativePath(String) getRelativePath(String)}, then
     * calls {@link #include(FaceletContext, UIComponent, URL)
     * include(FaceletContext, UIComponent, URL)}.
     *
     * @see FaceletContext#includeFacelet(UIComponent, String)
     * @param ctx FaceletContext to pass to the included Facelet
     * @param parent UIComponent to apply changes to
     * @param path relative path to the desired Facelet from the FaceletContext
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    public void include(DefaultFaceletContext ctx, UIComponent parent,
            String path) throws IOException, FacesException, FaceletException,
            ELException {
        try {
            URL url = this.getRelativePath(path);
            this.include(ctx, parent, url);
        } catch (FileNotFoundException e) {
            if (log.isLoggable(Level.SEVERE)) {
                log.severe("Template not found at " + path);
            }
            Facelet f = new NotFoundFacelet(path);
            f.apply(ctx.getFacesContext(), parent);
        }
    }

    /**
     * Grabs a DefaultFacelet from referenced DefaultFaceletFacotry
     *
     * @see DefaultFaceletFactory#getFacelet(URL)
     * @param ctx FaceletContext to pass to the included Facelet
     * @param parent UIComponent to apply changes to
     * @param url URL source to include Facelet from
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    public void include(DefaultFaceletContext ctx, UIComponent parent, URL url)
            throws IOException, FacesException, FaceletException, ELException {
        Facelet f = this.factory.getFacelet(url);
        if (f instanceof DefaultFacelet) {
            ((DefaultFacelet) f).include(ctx, parent);
        } else {
            f.apply(ctx.getFacesContext(), parent);
        }
    }

    private static class ApplyToken implements Externalizable {
        public String alias;

        public long time;

        public ApplyToken() {
        }

        public ApplyToken(String alias, long time) {
            this.alias = alias;
            this.time = time;
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            this.alias = in.readUTF();
            this.time = in.readLong();
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(this.alias);
            out.writeLong(this.time);
        }
    }

    @Override
    public String toString() {
        return this.alias;
    }
}

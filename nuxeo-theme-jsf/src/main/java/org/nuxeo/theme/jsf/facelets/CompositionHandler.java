/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nuxeo.theme.jsf.facelets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.TemplateClient;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributeException;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.ui.DefineHandler;
import com.sun.facelets.tag.ui.ParamHandler;

/**
 * @author Jacob Hookom
 * @version $Id: CompositionHandler.java,v 1.15 2009/02/02 22:58:59 driscoll Exp
 *          $
 */
public final class CompositionHandler extends TagHandler implements
        TemplateClient {

    private static final Logger log = Logger.getLogger("facelets.tag.ui.composition");

    public final static String Name = "composition";

    protected final TagAttribute template;

    protected final Map handlers;

    protected final ParamHandler[] params;

    /**
     * @param config
     */
    public CompositionHandler(TagConfig config) {
        super(config);
        this.template = this.getAttribute("template");
        if (this.template != null) {
            this.handlers = new HashMap();
            Iterator itr = this.findNextByType(DefineHandler.class);
            DefineHandler d = null;
            while (itr.hasNext()) {
                d = (DefineHandler) itr.next();
                this.handlers.put(d.getName(), d);
                if (log.isLoggable(Level.FINE)) {
                    log.fine(tag + " found Define[" + d.getName() + "]");
                }
            }
            List paramC = new ArrayList();
            itr = this.findNextByType(ParamHandler.class);
            while (itr.hasNext()) {
                paramC.add(itr.next());
            }
            if (paramC.size() > 0) {
                this.params = new ParamHandler[paramC.size()];
                for (int i = 0; i < this.params.length; i++) {
                    this.params[i] = (ParamHandler) paramC.get(i);
                }
            } else {
                this.params = null;
            }
        } else {
            this.params = null;
            this.handlers = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.facelets.FaceletHandler#apply(com.sun.facelets.FaceletContext,
     * javax.faces.component.UIComponent)
     */
    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        if (this.template != null) {
            VariableMapper orig = ctx.getVariableMapper();
            if (this.params != null) {
                VariableMapper vm = new VariableMapperWrapper(orig);
                ctx.setVariableMapper(vm);
                for (int i = 0; i < this.params.length; i++) {
                    this.params[i].apply(ctx, parent);
                }
            }

            ctx.extendClient(this);
            String path = null;
            try {
                path = this.template.getValue(ctx);
                ctx.includeFacelet(parent, path);
            } catch (IOException e) {
                throw new TagAttributeException(this.tag, this.template,
                        "Invalid path : " + path);
            } finally {
                ctx.popClient(this);
                ctx.setVariableMapper(orig);
            }
        } else {
            this.nextHandler.apply(ctx, parent);
        }
    }

    @Override
    public boolean apply(FaceletContext ctx, UIComponent parent, String name)
            throws IOException, FacesException, FaceletException, ELException {
        if (name != null) {
            if (this.handlers == null) {
                return false;
            }
            DefineHandler handler = (DefineHandler) this.handlers.get(name);
            if (handler != null) {
                handler.applyDefinition(ctx, parent);
                return true;
            } else {
                return false;
            }
        } else {
            this.nextHandler.apply(ctx, parent);
            return true;
        }
    }

}

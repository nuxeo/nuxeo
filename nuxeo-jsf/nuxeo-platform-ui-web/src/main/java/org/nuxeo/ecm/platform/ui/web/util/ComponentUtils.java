/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     Sean Radford
 *
 * $Id: ComponentUtils.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectMany;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Generic component helper methods.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class ComponentUtils {

    public static final String WHITE_SPACE_CHARACTER = "&#x0020;";

    private static final Log log = LogFactory.getLog(ComponentUtils.class);

    private static final String VH_HEADER = "nuxeo-virtual-host";

    public static final String FORCE_NO_CACHE_ON_MSIE = "org.nuxeo.download.force.nocache.msie";

    // Utility class.
    private ComponentUtils() {
    }

    /**
     * Calls a component encodeBegin/encodeChildren/encodeEnd methods.
     */
    public static void encodeComponent(FacesContext context,
            UIComponent component) throws IOException {
        component.encodeBegin(context);
        component.encodeChildren(context);
        component.encodeEnd(context);
    }

    /**
     * Helper method meant to be called in the component constructor.
     * <p>
     * When adding sub components dynamically, the tree fetching could be a
     * problem so all possible sub components must be added.
     * <p>
     * Since 6.0, does not mark component as not rendered anymore, calls
     * {@link #hookSubComponent(FacesContext, UIComponent, UIComponent, String)}
     * directly.
     *
     * @param parent
     * @param child
     * @param facetName facet name to put the child in.
     */
    public static void initiateSubComponent(UIComponent parent,
            String facetName, UIComponent child) {
        parent.getFacets().put(facetName, child);
        hookSubComponent(null, parent, child, facetName);
    }

    /**
     * Add a sub component to a UI component.
     * <p>
     * Since 6.0, does not the set the component as rendered anymore.
     *
     * @param context
     * @param parent
     * @param child
     * @param defaultChildId
     * @return child comp
     */
    public static UIComponent hookSubComponent(FacesContext context,
            UIComponent parent, UIComponent child, String defaultChildId) {
        // build a valid id using the parent id so that it's found everytime.
        String childId = child.getId();
        if (defaultChildId != null) {
            // override with default
            childId = defaultChildId;
        }
        // make sure it's set
        if (childId == null) {
            childId = context.getViewRoot().createUniqueId();
        }
        // reset client id
        child.setId(childId);
        child.setParent(parent);
        return child;
    }

    /**
     * Copies attributes and value expressions with given name from parent
     * component to child component.
     */
    public static void copyValues(UIComponent parent, UIComponent child,
            String[] valueNames) {
        Map<String, Object> parentAttributes = parent.getAttributes();
        Map<String, Object> childAttributes = child.getAttributes();
        for (String name : valueNames) {
            // attributes
            if (parentAttributes.containsKey(name)) {
                childAttributes.put(name, parentAttributes.get(name));
            }
            // value expressions
            ValueExpression ve = parent.getValueExpression(name);
            if (ve != null) {
                child.setValueExpression(name, ve);
            }
        }
    }

    public static void copyLinkValues(UIComponent parent, UIComponent child) {
        String[] valueNames = { "accesskey", "charset", "coords", "dir",
                "disabled", "hreflang", "lang", "onblur", "onclick",
                "ondblclick", "onfocus", "onkeydown", "onkeypress", "onkeyup",
                "onmousedown", "onmousemove", "onmouseout", "onmouseover",
                "onmouseup", "rel", "rev", "shape", "style", "styleClass",
                "tabindex", "target", "title", "type" };
        copyValues(parent, child, valueNames);
    }

    public static Object getAttributeValue(UIComponent component,
            String attributeName, Object defaultValue) {
        Object value = component.getAttributes().get(attributeName);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static Object getAttributeOrExpressionValue(FacesContext context,
            UIComponent component, String attributeName, Object defaultValue) {
        Object value = component.getAttributes().get(attributeName);
        if (value == null) {
            ValueExpression schemaExpr = component.getValueExpression(attributeName);
            value = schemaExpr.getValue(context.getELContext());
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static String download(FacesContext faces, Blob blob, String filename) {
        if (!faces.getResponseComplete()) {
            // do not perform download in an ajax request
            boolean ajaxRequest = faces.getPartialViewContext().isAjaxRequest();
            if (ajaxRequest) {
                return null;
            }
            if (blob == null) {
                log.error("No bytes available for the file: " + filename);
            } else {
                ExternalContext econtext = faces.getExternalContext();
                HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
                if (filename == null || filename.length() == 0) {
                    filename = "file";
                }
                HttpServletRequest request = (HttpServletRequest) econtext.getRequest();

                String digest = null;
                if (blob instanceof StorageBlob) {
                    digest = ((StorageBlob) blob).getBinary().getDigest();
                }

                try {
                    String previousToken = request.getHeader("If-None-Match");
                    if (previousToken != null && previousToken.equals(digest)) {
                        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    } else {
                        response.setHeader("ETag", digest);
                        response.setHeader("Content-Disposition",
                                ServletHelper.getRFC2231ContentDisposition(
                                        request, filename));

                        addCacheControlHeaders(request, response);

                        log.debug("Downloading with mime/type : "
                                + blob.getMimeType());
                        response.setContentType(blob.getMimeType());
                        long fileSize = blob.getLength();
                        if (fileSize > 0) {
                            response.setContentLength((int) fileSize);
                        }
                        blob.transferTo(response.getOutputStream());
                        response.flushBuffer();
                    }
                } catch (IOException e) {
                    log.error("Error while downloading the file: " + filename,
                            e);
                }
                faces.responseComplete();
            }
        }
        return null;
    }

    public static String downloadFile(FacesContext faces, String filename,
            File file) {
        FileBlob fileBlob = new FileBlob(file);
        return download(faces, fileBlob, filename);
    }

    protected static boolean forceNoCacheOnMSIE() {
        // see NXP-7759
        return Framework.isBooleanPropertyTrue(FORCE_NO_CACHE_ON_MSIE);
    }

    /**
     * Internet Explorer file downloads over SSL do not work with certain HTTP
     * cache control headers
     * <p>
     * See http://support.microsoft.com/kb/323308/
     * <p>
     * What is not mentioned in the above Knowledge Base is that
     * "Pragma: no-cache" also breaks download in MSIE over SSL
     */
    private static void addCacheControlHeaders(HttpServletRequest request,
            HttpServletResponse response) {
        String userAgent = request.getHeader("User-Agent");
        boolean secure = request.isSecure();
        if (!secure) {
            String nvh = request.getHeader(VH_HEADER);
            if (nvh != null) {
                secure = nvh.startsWith("https");
            }
        }
        log.debug("User-Agent: " + userAgent);
        log.debug("secure: " + secure);
        if (userAgent.contains("MSIE") && (secure || forceNoCacheOnMSIE())) {
            log.debug("Setting \"Cache-Control: max-age=15, must-revalidate\"");
            response.setHeader("Cache-Control", "max-age=15, must-revalidate");
        } else {
            log.debug("Setting \"Cache-Control: private\" and \"Pragma: no-cache\"");
            response.setHeader("Cache-Control", "private, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }
    }

    // hook translation passing faces context

    public static String translate(FacesContext context, String messageId) {
        return translate(context, messageId, (Object[]) null);
    }

    public static String translate(FacesContext context, String messageId,
            Object... params) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        return I18NUtils.getMessageString(bundleName, messageId,
                evaluateParams(context, params), locale);
    }

    public static void addErrorMessage(FacesContext context,
            UIComponent component, String message) {
        addErrorMessage(context, component, message, null);
    }

    public static void addErrorMessage(FacesContext context,
            UIComponent component, String message, Object[] params) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        message = I18NUtils.getMessageString(bundleName, message,
                evaluateParams(context, params), locale);
        FacesMessage msg = new FacesMessage(message);
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        context.addMessage(component.getClientId(context), msg);
    }

    /**
     * Evaluates parameters to pass to translation methods if they are value
     * expressions.
     *
     * @since 5.7
     */
    protected static Object[] evaluateParams(FacesContext context,
            Object[] params) {
        if (params == null) {
            return null;
        }
        Object[] res = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Object val = params[i];
            if (val instanceof String
                    && ComponentTagUtils.isValueReference((String) val)) {
                ValueExpression ve = context.getApplication().getExpressionFactory().createValueExpression(
                        context.getELContext(), (String) val, Object.class);
                res[i] = ve.getValue(context.getELContext());
            } else {
                res[i] = val;
            }
        }
        return res;
    }

    /**
     * Gets the base naming container from anchor.
     * <p>
     * Gets out of suggestion box as it's a naming container and we can't get
     * components out of it with a relative path => take above first found
     * container.
     *
     * @since 5.3.1
     */
    public static UIComponent getBase(UIComponent anchor) {
        UIComponent base = anchor;
        UIComponent container = anchor.getNamingContainer();
        if (container != null) {
            UIComponent supContainer = container.getNamingContainer();
            if (supContainer != null) {
                container = supContainer;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Resolved base '%s' for anchor '%s'",
                    base.getId(), anchor.getId()));
        }
        return base;
    }

    /**
     * Returns the component specified by the {@code componentId} parameter
     * from the {@code base} component.
     * <p>
     * Does not throw any exception if the component is not found, returns
     * {@code null} instead.
     *
     * @since 5.4
     */
    @SuppressWarnings("unchecked")
    public static <T> T getComponent(UIComponent base, String componentId,
            Class<T> expectedComponentClass) {
        if (componentId == null) {
            log.error("Cannot retrieve component with a null id");
            return null;
        }
        try {
            UIComponent component = ComponentRenderUtils.getComponent(base,
                    componentId);
            if (component == null) {
                log.error("Could not find component with id: " + componentId);
            } else {
                try {
                    return (T) component;
                } catch (ClassCastException e) {
                    log.error(String.format(
                            "Invalid component with id %s: %s, expected a "
                                    + "component with interface %s",
                            componentId, component, expectedComponentClass));
                }
            }
        } catch (Exception e) {
            log.error("Error when trying to retrieve component with id "
                    + componentId, e);
        }
        return null;
    }

    static void clearTargetList(UIEditableList targetList) {
        int rc = targetList.getRowCount();
        for (int i = 0; i < rc; i++) {
            targetList.removeValue(0);
        }
    }

    static void addToTargetList(UIEditableList targetList, SelectItem[] items) {
        for (int i = 0; i < items.length; i++) {
            targetList.addValue(items[i].getValue());
        }
    }

    /**
     * Move items up inside the target select
     */
    public static void shiftItemsUp(UISelectMany targetSelect,
            UISelectItems targetItems, UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        shiftUp(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    public static void shiftItemsDown(UISelectMany targetSelect,
            UISelectItems targetItems, UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        shiftDown(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    public static void shiftItemsFirst(UISelectMany targetSelect,
            UISelectItems targetItems, UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        all = shiftFirst(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    public static void shiftItemsLast(UISelectMany targetSelect,
            UISelectItems targetItems, UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        all = shiftLast(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    /**
     * Make a new SelectItem[] with items whose ids belong to selected first,
     * preserving inner ordering of selected and its complement in all.
     * <p>
     * Again this assumes that selected is an ordered sub-list of all
     * </p>
     *
     * @param selected ids of selected items
     * @param all
     * @return
     */
    static SelectItem[] shiftFirst(String[] selected, SelectItem[] all) {
        SelectItem[] res = new SelectItem[all.length];
        int sl = selected.length;
        int i = 0;
        int j = sl;
        for (SelectItem item : all) {
            if (i < sl && item.getValue().toString().equals(selected[i])) {
                res[i++] = item;
            } else {
                res[j++] = item;
            }
        }
        return res;
    }

    /**
     * Make a new SelectItem[] with items whose ids belong to selected last,
     * preserving inner ordering of selected and its complement in all.
     * <p>
     * Again this assumes that selected is an ordered sub-list of all
     * </p>
     *
     * @param selected ids of selected items
     * @param all
     * @return
     */
    static SelectItem[] shiftLast(String[] selected, SelectItem[] all) {
        SelectItem[] res = new SelectItem[all.length];
        int sl = selected.length;
        int cut = all.length - sl;
        int i = 0;
        int j = 0;
        for (SelectItem item : all) {
            if (i < sl && item.getValue().toString().equals(selected[i])) {
                res[cut + i++] = item;
            } else {
                res[j++] = item;
            }
        }
        return res;
    }

    static void swap(Object[] ar, int i, int j) {
        Object t = ar[i];
        ar[i] = ar[j];
        ar[j] = t;
    }

    static void shiftUp(String[] selected, SelectItem[] all) {
        int pos = -1;
        for (int i = 0; i < selected.length; i++) {
            String s = selected[i];
            // "pos" is the index of previous "s"
            int previous = pos;
            while (!all[++pos].getValue().equals(s)) {
            }
            // now current "s" is at "pos" index
            if (pos > previous + 1) {
                swap(all, pos, --pos);
            }
        }
    }

    static void shiftDown(String[] selected, SelectItem[] all) {
        int pos = all.length;
        for (int i = selected.length - 1; i >= 0; i--) {
            String s = selected[i];
            // "pos" is the index of previous "s"
            int previous = pos;
            while (!all[--pos].getValue().equals(s)) {
            }
            // now current "s" is at "pos" index
            if (pos < previous - 1) {
                swap(all, pos, ++pos);
            }
        }
    }

    /**
     * Move items from components to others.
     */
    public static void moveItems(UISelectMany sourceSelect,
            UISelectItems sourceItems, UISelectItems targetItems,
            UIEditableList hiddenTargetList, boolean setTargetIds) {
        String[] selected = (String[]) sourceSelect.getSelectedValues();
        if (selected == null) {
            // nothing to do
            return;
        }
        List<String> selectedList = Arrays.asList(selected);

        SelectItem[] all = (SelectItem[]) sourceItems.getValue();
        List<SelectItem> toMove = new ArrayList<SelectItem>();
        List<SelectItem> toKeep = new ArrayList<SelectItem>();
        List<String> hiddenIds = new ArrayList<String>();
        if (all != null) {
            for (SelectItem item : all) {
                String itemId = item.getValue().toString();
                if (selectedList.contains(itemId)) {
                    toMove.add(item);
                } else {
                    toKeep.add(item);
                    if (!setTargetIds) {
                        hiddenIds.add(itemId);
                    }
                }
            }
        }
        // reset left values
        sourceItems.setValue(toKeep.toArray(new SelectItem[] {}));
        sourceSelect.setSelectedValues(new Object[0]);

        // change right values
        List<SelectItem> newSelectItems = new ArrayList<SelectItem>();
        SelectItem[] oldSelectItems = (SelectItem[]) targetItems.getValue();
        if (oldSelectItems == null) {
            newSelectItems.addAll(toMove);
        } else {
            newSelectItems.addAll(Arrays.asList(oldSelectItems));
            List<String> oldIds = new ArrayList<String>();
            for (SelectItem oldItem : oldSelectItems) {
                String id = oldItem.getValue().toString();
                oldIds.add(id);
            }
            if (setTargetIds) {
                hiddenIds.addAll(0, oldIds);
            }
            for (SelectItem toMoveItem : toMove) {
                String id = toMoveItem.getValue().toString();
                if (!oldIds.contains(id)) {
                    newSelectItems.add(toMoveItem);
                    if (setTargetIds) {
                        hiddenIds.add(id);
                    }
                }
            }
        }
        targetItems.setValue(newSelectItems.toArray(new SelectItem[] {}));

        // update hidden values
        int numValues = hiddenTargetList.getRowCount();
        if (numValues > 0) {
            for (int i = numValues - 1; i > -1; i--) {
                hiddenTargetList.removeValue(i);
            }
        }
        for (String newHiddenValue : hiddenIds) {
            hiddenTargetList.addValue(newHiddenValue);
        }
    }

    /**
     * Move items from components to others.
     */
    public static void moveAllItems(UISelectItems sourceItems,
            UISelectItems targetItems, UIEditableList hiddenTargetList,
            boolean setTargetIds) {
        SelectItem[] all = (SelectItem[]) sourceItems.getValue();
        List<SelectItem> toMove = new ArrayList<SelectItem>();
        List<SelectItem> toKeep = new ArrayList<SelectItem>();
        List<String> hiddenIds = new ArrayList<String>();
        if (all != null) {
            for (SelectItem item : all) {
                if (!item.isDisabled()) {
                    toMove.add(item);
                } else {
                    toKeep.add(item);
                }
            }
        }
        // reset left values
        sourceItems.setValue(toKeep.toArray(new SelectItem[] {}));

        // change right values
        List<SelectItem> newSelectItems = new ArrayList<SelectItem>();
        SelectItem[] oldSelectItems = (SelectItem[]) targetItems.getValue();
        if (oldSelectItems == null) {
            newSelectItems.addAll(toMove);
        } else {
            newSelectItems.addAll(Arrays.asList(oldSelectItems));
            List<String> oldIds = new ArrayList<String>();
            for (SelectItem oldItem : oldSelectItems) {
                String id = oldItem.getValue().toString();
                oldIds.add(id);
            }
            if (setTargetIds) {
                hiddenIds.addAll(0, oldIds);
            }
            for (SelectItem toMoveItem : toMove) {
                String id = toMoveItem.getValue().toString();
                if (!oldIds.contains(id)) {
                    newSelectItems.add(toMoveItem);
                    if (setTargetIds) {
                        hiddenIds.add(id);
                    }
                }
            }
        }
        targetItems.setValue(newSelectItems.toArray(new SelectItem[] {}));

        // update hidden values
        int numValues = hiddenTargetList.getRowCount();
        if (numValues > 0) {
            for (int i = numValues - 1; i > -1; i--) {
                hiddenTargetList.removeValue(i);
            }
        }
        for (String newHiddenValue : hiddenIds) {
            hiddenTargetList.addValue(newHiddenValue);
        }
    }

}

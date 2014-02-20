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
 *
 * Contributors:
 *     Nuxeo
 *
 * $Id: HtmlLibrary.java 24932 2007-09-13 16:32:19Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.tag.jsf.html;

import org.nuxeo.ecm.platform.ui.web.component.message.NXMessagesRenderer;
import org.nuxeo.ecm.platform.ui.web.renderer.NXCheckboxRenderer;
import org.nuxeo.ecm.platform.ui.web.renderer.NXImageRenderer;
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.MetaActionSourceTagHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.MetaValueHolderTagHandler;

import com.sun.faces.facelets.tag.jsf.html.AbstractHtmlLibrary;

/**
 * Replicate the HTML Library with facelet handlers to use a specific
 * namespace.
 *
 * @author Jacob Hookom
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class HtmlLibrary extends AbstractHtmlLibrary {

    public static final String Namespace = "http://nuxeo.org/nxweb/html";

    public static final HtmlLibrary Instance = new HtmlLibrary();

    public HtmlLibrary() {
        super(Namespace);

        this.addHtmlComponent("column", "javax.faces.Column", null);

        this.addComponent("commandButton", "javax.faces.HtmlCommandButton",
                "javax.faces.Button", MetaActionSourceTagHandler.class);

        this.addComponent("commandLink", "javax.faces.HtmlCommandLink",
                "javax.faces.Link", MetaActionSourceTagHandler.class);

        this.addHtmlComponent("dataTable", "javax.faces.HtmlDataTable",
                "javax.faces.Table");

        this.addHtmlComponent("form", "javax.faces.HtmlForm",
                "javax.faces.Form");

        this.addHtmlComponent("graphicImage", "javax.faces.HtmlGraphicImage",
                NXImageRenderer.RENDERER_TYPE);

        this.addHtmlComponent("inputHidden", "javax.faces.HtmlInputHidden",
                "javax.faces.Hidden");

        this.addHtmlComponent("inputSecret", "javax.faces.HtmlInputSecret",
                "javax.faces.Secret");

        this.addHtmlComponent("inputText", "javax.faces.HtmlInputText",
                "javax.faces.Text");

        this.addHtmlComponent("inputTextarea", "javax.faces.HtmlInputTextarea",
                "javax.faces.Textarea");

        this.addHtmlComponent("message", "javax.faces.HtmlMessage",
                "javax.faces.Message");

        this.addHtmlComponent("messages", "javax.faces.HtmlMessages",
                NXMessagesRenderer.RENDERER_TYPE);

        this.addHtmlComponent("outputFormat", "javax.faces.HtmlOutputFormat",
                "javax.faces.Format");

        this.addHtmlComponent("outputLabel", "javax.faces.HtmlOutputLabel",
                "javax.faces.Label");

        this.addHtmlComponent("outputLink", "javax.faces.HtmlOutputLink",
                "javax.faces.Link");

        // meta value wired
        this.addComponent("metaOutputLink", "javax.faces.HtmlOutputLink",
                "javax.faces.Link", MetaValueHolderTagHandler.class);

        this.addComponent("outputText", "javax.faces.HtmlOutputText",
                "javax.faces.Text", MetaValueHolderTagHandler.class);

        this.addHtmlComponent("panelGrid", "javax.faces.HtmlPanelGrid",
                "javax.faces.Grid");

        this.addHtmlComponent("panelGroup", "javax.faces.HtmlPanelGroup",
                "javax.faces.Group");

        this.addHtmlComponent("selectBooleanCheckbox",
                "javax.faces.HtmlSelectBooleanCheckbox",
                NXCheckboxRenderer.RENDERER_TYPE);

        this.addHtmlComponent("selectManyCheckbox",
                "javax.faces.HtmlSelectManyCheckbox", "javax.faces.Checkbox");

        this.addHtmlComponent("selectManyListbox",
                "javax.faces.HtmlSelectManyListbox", "javax.faces.Listbox");

        this.addHtmlComponent("selectManyMenu",
                "javax.faces.HtmlSelectManyMenu", "javax.faces.Menu");

        this.addHtmlComponent("selectOneListbox",
                "javax.faces.HtmlSelectOneListbox", "javax.faces.Listbox");

        this.addHtmlComponent("selectOneMenu", "javax.faces.HtmlSelectOneMenu",
                "javax.faces.Menu");

        this.addHtmlComponent("selectOneRadio",
                "javax.faces.HtmlSelectOneRadio", "javax.faces.Radio");
    }

    @Override
    public void addHtmlComponent(String name, String componentType,
            String rendererType) {
        super.addComponent(name, componentType, rendererType,
                GenericHtmlComponentHandler.class);
    }

}

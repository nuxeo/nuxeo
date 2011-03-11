/*
 * This software is published under the Apache 2.0 licenses.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Erik Scholtz
 * Web: http://blog.elitecoderz.net
 */

package org.nuxeo.opensocial.wysiwyg.client;

import java.util.HashMap;

import org.nuxeo.opensocial.wysiwyg.client.resources.ImagesBundle;
import org.nuxeo.opensocial.wysiwyg.client.resources.RichTextEditorConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.RichTextArea.FontSize;
import com.google.gwt.user.client.ui.RichTextArea.Formatter;

public class RichTextToolbar extends Composite {
    public static RichTextEditorConstants constants = GWT.create(RichTextEditorConstants.class);

    /** Local CONSTANTS **/
    // ImageMap and CSS related
    private static final String CSS_ROOT_NAME = "RichTextToolbar";
    public static ImagesBundle images = GWT.create(ImagesBundle.class);

    // Color and Fontlists - First Value (key) is the Name to display, Second
    // Value (value) is the HTML-Definition
    public final static HashMap<String, String> GUI_COLORLIST = new HashMap<String, String>();
    static {
        GUI_COLORLIST.put(constants.white(), "#FFFFFF");
        GUI_COLORLIST.put(constants.black(), "#000000");
        GUI_COLORLIST.put(constants.red(), "red");
        GUI_COLORLIST.put(constants.green(), "green");
        GUI_COLORLIST.put(constants.yellow(), "yellow");
        GUI_COLORLIST.put(constants.blue(), "blue");
    }

    public final static HashMap<String, String> GUI_FONTLIST = new HashMap<String, String>();
    static {
        GUI_FONTLIST.put("Times New Roman", "Times New Roman");
        GUI_FONTLIST.put("Arial", "Arial");
        GUI_FONTLIST.put("Courier New", "Courier New");
        GUI_FONTLIST.put("Georgia", "Georgia");
        GUI_FONTLIST.put("Trebuchet", "Trebuchet");
        GUI_FONTLIST.put("Verdana", "Verdana");
    }

    public final static HashMap<String, FontSize> GUI_FONTSIZELIST = new HashMap<String, FontSize>();
    static {
        GUI_FONTSIZELIST.put(FontSize.XX_SMALL.toString(), FontSize.XX_SMALL);
        GUI_FONTSIZELIST.put(FontSize.X_SMALL.toString(), FontSize.X_SMALL);
        GUI_FONTSIZELIST.put(FontSize.SMALL.toString(), FontSize.SMALL);
        GUI_FONTSIZELIST.put(FontSize.MEDIUM.toString(), FontSize.MEDIUM);
        GUI_FONTSIZELIST.put(FontSize.LARGE.toString(), FontSize.LARGE);
        GUI_FONTSIZELIST.put(FontSize.X_LARGE.toString(), FontSize.X_LARGE);
        GUI_FONTSIZELIST.put(FontSize.XX_LARGE.toString(), FontSize.XX_LARGE);
    }

    /** Private Variables **/
    // The main (Vertical)-Panel and the two inner (Horizontal)-Panels
    private VerticalPanel outer;
    private FlowPanel topPanel;
    private FlowPanel bottomPanel;

    // The RichTextArea this Toolbar referes to and the Interfaces to access the
    // RichTextArea
    private RichTextArea styleText;
    private Formatter styleTextFormatter;

    // We use an internal class of the ClickHandler and the KeyUpHandler to be
    // private to others with these events
    private EventHandler evHandler;

    // The Buttons of the Menubar
    private ToggleButton bold;
    private ToggleButton italic;
    private ToggleButton underline;
    private PushButton alignleft;
    private PushButton alignmiddle;
    private PushButton alignright;
    private PushButton orderlist;
    private PushButton unorderlist;
    private PushButton generatelink;
    private PushButton breaklink;
    private PushButton insertline;
    private PushButton insertimage;

    private ListBox fontlist;
    private ListBox colorlist;
    private ListBox fontsizelist;

    /** Constructor of the Toolbar **/
    public RichTextToolbar(RichTextArea richtext) {
        // Initialize the main-panel
        outer = new VerticalPanel();

        // Initialize the two inner panels
        topPanel = new FlowPanel();
        bottomPanel = new FlowPanel();
        topPanel.setStyleName(CSS_ROOT_NAME);
        bottomPanel.setStyleName(CSS_ROOT_NAME);

        // Save the reference to the RichText area we refer to and get the
        // interfaces to the stylings

        styleText = richtext;
        styleTextFormatter = styleText.getFormatter();

        // Add the two inner panels to the main panel
        outer.add(topPanel);
        outer.add(bottomPanel);

        // Some graphical stuff to the main panel and the initialisation of the
        // new widget
        outer.setWidth("100%");
        outer.setStyleName(CSS_ROOT_NAME);
        initWidget(outer);

        evHandler = new EventHandler();

        // Add KeyUp and Click-Handler to the RichText, so that we can actualize
        // the toolbar if neccessary
        styleText.addKeyUpHandler(evHandler);
        styleText.addClickHandler(evHandler);

        // Now lets fill the new toolbar with life
        buildTools();
    }

    /** Click Handler of the Toolbar **/
    private class EventHandler implements ClickHandler, KeyUpHandler,
            ChangeHandler {
        public void onClick(ClickEvent event) {
            if (event.getSource()
                    .equals(bold)) {
                styleTextFormatter.toggleBold();
            } else if (event.getSource()
                    .equals(italic)) {
                styleTextFormatter.toggleItalic();
            } else if (event.getSource()
                    .equals(underline)) {
                styleTextFormatter.toggleUnderline();
            } else if (event.getSource()
                    .equals(alignleft)) {
                styleTextFormatter.setJustification(RichTextArea.Justification.LEFT);
            } else if (event.getSource()
                    .equals(alignmiddle)) {
                styleTextFormatter.setJustification(RichTextArea.Justification.CENTER);
            } else if (event.getSource()
                    .equals(alignright)) {
                styleTextFormatter.setJustification(RichTextArea.Justification.RIGHT);
            } else if (event.getSource()
                    .equals(orderlist)) {
                styleTextFormatter.insertOrderedList();
            } else if (event.getSource()
                    .equals(unorderlist)) {
                styleTextFormatter.insertUnorderedList();
            } else if (event.getSource()
                    .equals(generatelink)) {
                String url = Window.prompt(constants.insertLinkUrl(), "http://");
                if (url != null) {
                    styleTextFormatter.createLink(url);
                }
            } else if (event.getSource()
                    .equals(breaklink)) {
                styleTextFormatter.removeLink();
            } else if (event.getSource()
                    .equals(insertimage)) {
                String url = Window.prompt(constants.insertImageUrl(),
                        "http://");
                if (url != null) {
                    styleTextFormatter.insertImage(url);
                }
            } else if (event.getSource()
                    .equals(insertline)) {
                styleTextFormatter.insertHorizontalRule();
            } else if (event.getSource()
                    .equals(styleText)) {
                // Change invoked by the richtextArea
            }
            updateStatus();
        }

        public void onKeyUp(KeyUpEvent event) {
            updateStatus();
        }

        public void onChange(ChangeEvent event) {
            if (event.getSource()
                    .equals(fontlist)) {
                styleTextFormatter.setFontName(fontlist.getValue(fontlist.getSelectedIndex()));
            } else if (event.getSource()
                    .equals(colorlist)) {
                styleTextFormatter.setForeColor(colorlist.getValue(colorlist.getSelectedIndex()));
            } else if (event.getSource()
                    .equals(fontsizelist)) {
                FontSize fontsize = GUI_FONTSIZELIST.get(fontsizelist.getValue(fontsizelist.getSelectedIndex()));
                if (fontsize != null) {
                    styleTextFormatter.setFontSize(fontsize);
                }
            }
        }
    }

    /**
     * Native JavaScript that returns the selected text and position of the
     * start
     **/
    public static native JsArrayString getSelection(Element elem) /*-{
                                                                  var txt = "";
                                                                  var pos = 0;
                                                                  var range;
                                                                  var parentElement;
                                                                  var container;

                                                                  if (elem.contentWindow.getSelection) {
                                                                  txt = elem.contentWindow.getSelection();
                                                                  pos = elem.contentWindow.getSelection().getRangeAt(0).startOffset;
                                                                  } else if (elem.contentWindow.document.getSelection) {
                                                                  txt = elem.contentWindow.document.getSelection();
                                                                  pos = elem.contentWindow.document.getSelection().getRangeAt(0).startOffset;
                                                                  } else if (elem.contentWindow.document.selection) {
                                                                  range = elem.contentWindow.document.selection.createRange();
                                                                  txt = range.text;
                                                                  parentElement = range.parentElement();
                                                                  container = range.duplicate();
                                                                  container.moveToElementText(parentElement);
                                                                  container.setEndPoint('EndToEnd', range);
                                                                  pos = container.text.length - range.text.length;
                                                                  }
                                                                  return [""+txt,""+pos];
                                                                  }-*/;

    /**
     * Private method to set the toggle buttons and disable/enable buttons which
     * do not work in html-mode
     **/
    private void updateStatus() {
        if (styleTextFormatter != null) {
            bold.setDown(styleTextFormatter.isBold());
            italic.setDown(styleTextFormatter.isItalic());
            underline.setDown(styleTextFormatter.isUnderlined());
            colorlist.setItemSelected(0, true);
            fontlist.setItemSelected(0, true);
            fontsizelist.setItemSelected(0, true);
        }

        breaklink.setEnabled(true);
    }

    /** Initialize the options on the toolbar **/
    private void buildTools() {
        // Init the TOP Panel first
        topPanel.add(bold = createToggleButton(images.icons(), 0, 0, 20, 20,
                constants.bold()));
        topPanel.add(italic = createToggleButton(images.icons(), 0, 60, 20, 20,
                constants.italic()));
        topPanel.add(underline = createToggleButton(images.icons(), 0, 140, 20,
                20, constants.underline()));
        topPanel.add(alignleft = createPushButton(images.icons(), 0, 460, 20,
                20, constants.alignLeft()));
        topPanel.add(alignmiddle = createPushButton(images.icons(), 0, 420, 20,
                20, constants.alignCenter()));
        topPanel.add(alignright = createPushButton(images.icons(), 0, 480, 20,
                20, constants.alignRight()));
        topPanel.add(orderlist = createPushButton(images.icons(), 0, 80, 20,
                20, constants.orderList()));
        topPanel.add(unorderlist = createPushButton(images.icons(), 0, 20, 20,
                20, constants.unorderList()));
        topPanel.add(generatelink = createPushButton(images.icons(), 0, 500,
                20, 20, constants.link()));
        topPanel.add(breaklink = createPushButton(images.icons(), 0, 640, 20,
                20, constants.breakLine()));
        topPanel.add(insertimage = createPushButton(images.icons(), 0, 380, 20,
                20, constants.image()));

        // Init the BOTTOM Panel
        bottomPanel.add(fontlist = createFontList());
        bottomPanel.add(colorlist = createColorList());
        bottomPanel.add(fontsizelist = createFontSizeList());
    }

    /** Method to create a Toggle button for the toolbar **/
    private ToggleButton createToggleButton(ImageResource resource,
            Integer top, Integer left, Integer width, Integer height, String tip) {
        Image extract = new Image(resource);
        extract.setVisibleRect(left, top, width, height);
        ToggleButton tb = new ToggleButton(extract);
        tb.setHeight(height + "px");
        tb.setWidth(width + "px");
        tb.addClickHandler(evHandler);
        if (tip != null) {
            tb.setTitle(tip);
        }
        return tb;
    }

    /** Method to create a Push button for the toolbar **/
    private PushButton createPushButton(ImageResource resource, Integer top,
            Integer left, Integer width, Integer height, String tip) {
        Image extract = new Image(resource);
        extract.setVisibleRect(left, top, width, height);
        PushButton tb = new PushButton(extract);
        tb.setHeight(height + "px");
        tb.setWidth(width + "px");
        tb.addClickHandler(evHandler);
        if (tip != null) {
            tb.setTitle(tip);
        }
        return tb;
    }

    /** Method to create the fontlist for the toolbar **/
    private ListBox createFontList() {
        ListBox mylistBox = new ListBox();
        mylistBox.addChangeHandler(evHandler);
        mylistBox.setVisibleItemCount(1);

        mylistBox.addItem(constants.fontsListName());
        for (String name : GUI_FONTLIST.keySet()) {
            mylistBox.addItem(name, GUI_FONTLIST.get(name));
        }

        return mylistBox;
    }

    /** Method to create the fontsize for the toolbar **/
    private ListBox createFontSizeList() {
        ListBox mylistBox = new ListBox();
        mylistBox.addChangeHandler(evHandler);
        mylistBox.setVisibleItemCount(1);

        mylistBox.addItem(constants.fontsSizeListName());
        for (String name : GUI_FONTSIZELIST.keySet()) {
            mylistBox.addItem(name, GUI_FONTSIZELIST.get(name)
                    .toString());
        }

        return mylistBox;
    }

    /** Method to create the colorlist for the toolbar **/
    private ListBox createColorList() {
        ListBox mylistBox = new ListBox();
        mylistBox.addChangeHandler(evHandler);
        mylistBox.setVisibleItemCount(1);

        mylistBox.addItem(constants.colorsListName());
        for (String name : GUI_COLORLIST.keySet()) {
            mylistBox.addItem(name, GUI_COLORLIST.get(name));
        }

        return mylistBox;
    }

}
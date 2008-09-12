
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.formats.FormatFactory
import org.nuxeo.theme.formats.widgets.Widget
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events

String id = Request.getParameter("element_id")
String property = Request.getParameter("property")
String value = Request.getParameter("value")

Element element = ThemeManager.getElementById(id);
if (element == null) {
    return;
}

Style style = (Style) ElementFormatter.getFormatFor(element, "style");
if (style == null) {
    style = (Style) FormatFactory.create("style");
    Manager.getThemeManager().registerFormat(style);
    ElementFormatter.setFormat(element, style);
}

Widget widget = (Widget) ElementFormatter.getFormatFor(element, "widget");
if (widget == null) {
    return;
}

String viewName = widget.getName();

Properties properties = style.getPropertiesFor(viewName, "");
if (properties == null) {
    properties = new Properties();
}
if (value == null) {
    properties.remove(property);
} else {
    properties.setProperty(property, value);
}
style.setPropertiesFor(viewName, "", properties);

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null));
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(style, null));


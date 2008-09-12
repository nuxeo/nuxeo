
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.formats.Format
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.elements.ElementFormatter

id = Request.getParameter("id");
viewName = Request.getParameter("viewName");

Element element = ThemeManager.getElementById(id);
FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "widget");
Format widget = ElementFormatter.getFormatByType(element, widgetType);
if (widget == null) {
    widget = FormatFactory.create("widget");
    themeManager.registerFormat(widget);
}
widget.setName(viewName);
ElementFormatter.setFormat(element, widget);

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null));


import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.ElementFormatter;

id = Request.getParameter("id");
position = Request.getParameter("position");

Element element = ThemeManager.getElementById(id);
Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout");

if (layout == null) {
    layout = (Layout) FormatFactory.create("layout");
    themeManager.registerFormat(layout);
    ElementFormatter.setFormat(element, layout);
}

if (element instanceof SectionElement) {
    if (position.equals("left")) {
        layout.setProperty("margin-left", "0");
        layout.setProperty("margin-right", "auto");
    } else if (position.equals("center")) {
        layout.setProperty("margin-left", "auto");
        layout.setProperty("margin-right", "auto");
    } else if (position.equals("right")) {
        layout.setProperty("margin-left", "auto");
        layout.setProperty("margin-right", "0");
    }
} else {
    if (position.equals("left")) {
        layout.setProperty("text-align", "left");
    } else if (position.equals("center")) {
        layout.setProperty("text-align", "center");
    } else if (position.equals("right")) {
        layout.setProperty("text-align", "right");
    }
}

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null));

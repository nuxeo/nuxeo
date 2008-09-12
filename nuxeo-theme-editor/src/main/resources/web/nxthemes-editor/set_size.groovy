
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.formats.Format

id = Request.getParameter("id");
width = Request.getParameter("width");

Format layout = ThemeManager.getFormatById(id);
layout.setProperty("width", width);

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(layout, null));

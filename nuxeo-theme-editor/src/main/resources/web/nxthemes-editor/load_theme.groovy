
import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.themes.ThemeIOException

src = Request.getParameter("src")

try {
    Manager.getThemeManager().loadTheme(src);
} catch (ThemeIOException e) {
    return false;
}

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null));
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null));
return true;

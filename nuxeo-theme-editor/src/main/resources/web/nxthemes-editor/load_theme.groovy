import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.themes.ThemeIOException

src = Request.getParameter("src")

res = 1
try {
    Manager.getThemeManager().loadTheme(src)
} catch (ThemeIOException e) {
    res = 0
}

if (res) {
    EventManager eventManager = Manager.getEventManager()
    eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null))
    eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null))
}

Response.writer.write(res)

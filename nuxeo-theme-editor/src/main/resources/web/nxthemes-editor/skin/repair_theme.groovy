import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager

themeName = Request.getParameter("theme_name")
theme = Manager.getThemeManager().getThemeByName(themeName)

res = 1
if (theme == null) {
    res = 0
}

if (res) {
    ThemeManager.repairTheme(theme)

    EventManager eventManager = Manager.getEventManager()
    eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(theme, null))
    eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(theme, null))
}

Response.writer.write(res)

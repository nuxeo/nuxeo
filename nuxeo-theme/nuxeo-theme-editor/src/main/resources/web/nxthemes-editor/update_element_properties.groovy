import net.sf.json.JSONObject

import org.nuxeo.theme.Manager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.properties.FieldIO

id = Request.getParameter("id")
propertyMap = JSONObject.fromObject(Request.getParameter("property_map"))

Element element = ThemeManager.getElementById(id)
Properties properties = new Properties()
for (Object key : propertyMap.keySet()) {
    properties.put(key, propertyMap.get(key))
}

FieldIO.updateFieldsFromProperties(element, properties)

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))

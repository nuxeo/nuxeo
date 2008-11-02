import org.nuxeo.runtime.api.Framework

service = Framework.getRuntime().getComponent("org.nuxeo.theme.webwidgets.Service")
return service.getWidgetCategories()


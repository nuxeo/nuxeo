import org.nuxeo.runtime.api.Framework

widgetCategory = Context.runScript("getSelectedWidgetCategory.groovy")

service = Framework.getRuntime().getComponent("org.nuxeo.theme.webwidgets.Service")
return service.getWidgetTypes(widgetCategory)

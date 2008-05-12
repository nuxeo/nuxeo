import net.sf.json.JSONObject
import net.sf.json.JSONArray
import net.sf.json.JsonConfig
import org.nuxeo.ecm.core.api.security.ACP
import net.sf.json.util.PropertyFilter
import net.sf.json.filters.TruePropertyFilter
import org.apache.commons.lang.StringUtils    

writer = Response.getWriter()

qfield = 'ecm:fulltext'
qtext = ''
qsort = 'dc:modified'
qorder = 'DESC'

if (Request.getParameter('qtype')) {
    qfield = Request.getParameter('qtype')
}
if (Request.getParameter('query')) {
    qtext = Request.getParameter('query')
}
if (Request.getParameter('sortname')) {
    qsort = Request.getParameter('sortname')
}
if (Request.getParameter('sortorder')) {
    qorder = StringUtils.upperCase(Request.getParameter('sortorder'))
}

if (qfield && qtext) {
    query = "SELECT * FROM Document WHERE  (${qfield} = '${qtext}') AND (ecm:path = '${Root.getRepositoryPath()}') AND (ecm:isCheckedInVersion = 0) ORDER BY ${qsort}"
} else {
    query = "SELECT * FROM Document WHERE (ecm:isCheckedInVersion = 0)  AND (ecm:path = '${Root.getRepositoryPath()}') ORDER BY ${qsort} ${qorder}"
}
//writer.write(query)
rdoc = Context.search(query)

rows = []
for (d in rdoc) {
    def c = [:]
    c['id'] = d.id
    c['cell'] = []
    c['yop'] = []
    c['cell'].add(d.getPropertyValue('dc:title').toString())
    c['cell'].add(d.getPropertyValue('dc:modified').getTime().toString())
    c['cell'].add(d.getPropertyValue('dc:creator').toString())
    c['cell'].add(d.getCurrentLifeCycleState().toString())
    rows.add(c)
}

dlist = [page: 1, total: 1, rows: rows]


jslist = JSONObject.fromObject(dlist)

writer.write(jslist.toString())

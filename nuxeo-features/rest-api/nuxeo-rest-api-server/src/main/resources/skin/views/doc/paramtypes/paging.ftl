{
  "paramType": "query",
  "name": "currentPageIndex",
  "description": "Index of the current page",
  "dataType": "long",
  "required": false
},
{
  "paramType": "query",
  "name": "offset",
  "description": "Offset of the page to retrieve. If set, the 'currentPageIndex' parameter is ignored.",
  "dataType": "long",
  "required": false
},
{
  "paramType": "query",
  "name": "pageSize",
  "description": "Size of the page to retrieve. Ignored if offset set",
  "dataType": "long",
  "required": false
},
{
  "paramType": "query",
  "name": "maxResults",
  "description": "Maximum results to retrieve",
  "dataType": "long",
  "required": false
},
{
  "paramType": "query",
  "name": "sortBy",
  "description": "Property to sort by, for example 'dc:title'",
  "dataType": "string",
  "required": false
},
{
  "paramType": "query",
  "name": "sortOrder",
  "description": "Sort order, accepts 'asc' or 'desc', default is 'desc'",
  "dataType": "string",
  "required": false
}

"facet": {
  "id": "facet",
  "properties": {
    "name": {
      "type": "string",
      "required": true
    },
    "schemas": {
      "type": "array",
      "items": {
        <#include "views/doc/datatypes/schema.ftl"/>
      }
    }
  }
}

"GroupRef" : {
  "id": "GroupRef",
  "uniqueItems": false,
  "properties": {
    "name": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "label": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "url": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    }
   }
 },

"NuxeoPrincipal" : {
  "id": "NuxeoPrincipal",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "id": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "properties": {
      "uniqueItems": false,
      "type": "container",
      "items":{
          "$ref":"Property"
        }
    },
    "extendedGroups": {
      "uniqueItems": false,
      "type": "container",
      "items":{
          "$ref":"GroupRef"
        }
    },
    "isAdministrator": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    },
    "isAnonymous": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    }
  }
},
"NuxeoPrincipalList" : {
  "id": "NuxeoPrincipalList",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    <#include "views/doc/datatypes/paginable.ftl"/>,
    "entries": {
      "uniqueItems": false,
      "type": "array",
      "items": {
        "$ref":"NuxeoPrincipal"
      },
      "required": true
    }

  }
}



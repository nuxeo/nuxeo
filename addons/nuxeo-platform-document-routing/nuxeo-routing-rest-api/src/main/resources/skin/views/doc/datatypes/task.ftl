"task" : {
    "id": "task",
    "type": "any",
    "required": false,
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
            "required": false
        },
        "name": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "workflowId": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "state": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "directive": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "created": {
            "uniqueItems": false,
            "type": "Date",
            "required": false
        },
        "dueDate": {
            "uniqueItems": false,
            "type": "Date",
            "required": false
        },
	"type": {
            "uniqueItems": false,
            "type": "Date",
            "required": false
        },
	"nodeName": {
            "uniqueItems": false,
            "type": "Date",
            "required": false
        },
        "targetDocumentIds": {
            "type": "array",
            "items" : {
              "type":"string"
            },
            "required": false
        },
        "actors": {
            "type": "array",
            "items" : {
              "type":"string"
            },
            "required": false
        },
		"comments": {
            "type": "array",
            "items":{
              "$ref":"taskComments"
            },
            "required": false
        },
		"variables": {
            "uniqueItems": true,
            "type": "container",
            "items":{
              "$ref":"Property"
            }
        },
        "taskInfo": {
            "uniqueItems": true,
            "type": "taskInfo",
            "required": false
        }
    }

},

"taskInfo" : {
      "id": "taskInfo",
      "uniqueItems": false,
      "properties": {
        "actions": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"taskAction"
          },
          "required": true
        },
        "layoutResource": {
          "uniqueItems": false,
          "type": "layoutResource",
          "required": true
        }
      }
},

"layoutResource" : {
  "id": "layoutResource",
      "uniqueItems": false,
      "properties": {
        "name": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "url": {
          "uniqueItems": false,
          "type": "layoutResource",
          "required": true
        }
      }
},

"taskComments" : {
      "id": "taskComments",
      "uniqueItems": false,
      "properties": {
        "author": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
       "author": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "date": {
          "uniqueItems": false,
          "type": "Date",
          "required": true
        }
      }
},

"taskAction" : {
      "id": "taskAction",
      "uniqueItems": false,
      "properties": {
        "name": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "url": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
		"label": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        }
      }
},

 "tasks" : {
      "id": "tasks",
      "uniqueItems": false,
      "properties": {
        "entity-type": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "entries": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"task"
          },
          "required": true
        }
      }
},

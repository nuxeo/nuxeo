<#include "views/doc/macros.ftl"/>
{
  "apiVersion": "1.0",
  "swaggerVersion": "1.2",
  "basePath": "${Context.serverURL}${Context.modulePath}",
  "produces": [
  "application/json",
  "application/json+nxentity"
  ],
  "apis": [
    <@block name="apis" />
  ],
  "models":{
    <@block name="models" />
  }
}
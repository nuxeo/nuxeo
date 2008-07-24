
package org.nuxeo.ecm.flex.dto
{
  import mx.collections.ArrayCollection;
  import flash.utils.IExternalizable;
  import flash.utils.IDataInput;
  import flash.utils.IDataOutput;
  import mx.core.IUID;

  [RemoteClass(alias="org.nuxeo.ecm.flex.javadto.FlexDocumentModel")]
  public class FlexDocumentModel implements IExternalizable, IUID

  {
    private var _docRef:String;
    private var _name:String;
    private var _path:String;
    private var _lifeCycleState:String;
    private var _data:Object;
    private var _dirty:Object;
    private var _type:String;
  private var _isFolder:Boolean;

    public function FlexDocumentModel()
    {
      _name="init_from_flex";
    }

    public function setup(type:String,name:String,parentPath:String):void
    {
      _type=type;
      _name=name;
      _path=parentPath+_name;
      _dirty = new Object();
      _data = new Object();
    }

    public function get uid(): String
    {
            return _docRef;
    }

    public function set uid(uid:String): void
    {

    }

    public function readExternal(input:IDataInput):void {
            _docRef = input.readUTF();
      _name = input.readUTF();
      _path = input.readUTF();
      _lifeCycleState = input.readUTF();
      _type=input.readUTF();
      _isFolder=input.readBoolean();
      _data = input.readObject();
      _dirty = new Object();
        }

    public function writeExternal(output:IDataOutput):void {
            output.writeUTF(_docRef);
            output.writeUTF(_name);
            output.writeUTF(_path);
            output.writeUTF(_lifeCycleState);
            output.writeUTF(_type);
      output.writeObject(_dirty);
            //output.writeObject(_data);
        }

    public function get id():String
    {
      return _docRef;
    }

    public function get name():String
    {
      return _name;
    }

    public function get doctype():String
    {
      return _type;
    }

    public function get contentdata():Object
    {
      return _data;
    }

    public function set name(value:String):void
    {
      _name= value;
    }

    public function getTitle():String
    {
      //return "fakeTitle";
      return _data.dublincore.title;
    }

    public function setTitle(value:String):void
    {
      _data.dublincore.title=value;
      _dirty.dublincore_title=value;
    }

    public function getProperty(schemaName:String, fieldName:String):String
    {
      return _data[schemaName][fieldName];
    }

    public function setProperty(schemaName:String, fieldName:String, value:String):void
    {
      _data[schemaName][fieldName]=value;
      _dirty[schemaName+":"+fieldName]=value;
    }

    public function isFolder():Boolean
    {
      return _isFolder;
    }

    public function getSchemas():Array
    {
      var schemas:Array = new Array();
      var schemaName:Object;
      for (schemaName in _data)
      {
      schemas.push(schemaName);
      }
      return schemas;
    }

    public function getSchema(schemaName:String):Object
    {
      return _data[schemaName];
    }

    public function getFieldNames(schemaName:String):Array
    {
      var fieldNames:Array = new Array();
      var fieldName:Object;
      for (fieldName in _data[schemaName])
      {
      fieldNames.push(fieldName);
      }
      return fieldNames;
    }


  }
}

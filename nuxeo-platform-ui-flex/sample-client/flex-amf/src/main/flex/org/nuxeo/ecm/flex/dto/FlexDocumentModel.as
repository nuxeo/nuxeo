
package org.nuxeo.ecm.flex.dto
{
	import mx.collections.ArrayCollection;
	import flash.utils.IExternalizable;
	import flash.utils.IDataInput;
	import flash.utils.IDataOutput;

	[RemoteClass(alias="org.nuxeo.ecm.flex.javadto.FlexDocumentModel")]
	public class FlexDocumentModel implements IExternalizable 
	{
		private var _docRef:String;
		private var _name:String;
		private var _parentPath:String;
		private var _lifeCycleState:String;
		//private var _data:Object;
		
		public function FlexDocumentModel()
		{
			_name="init from flex";
		}
		
		public function readExternal(input:IDataInput):void {
        		_docRef = input.readUTF();
			_name = input.readUTF();	
			_parentPath = input.readUTF();	
			_lifeCycleState = input.readUTF();	
        		//_data = input.readObject();
    		}
    
		public function writeExternal(output:IDataOutput):void {
        		output.writeUTF(_docRef);
        		output.writeUTF(_name);
        		output.writeUTF(_parentPath);
        		output.writeUTF(_lifeCycleState);
        		//output.writeObject(_data);
    		}

		public function get name():String
		{
			return _name;
		}
		
		public function set name(value:String):void
		{
			_name= value;
		}

		public function getTitle():String
		{
			return "title";
			//return _data.dublincore.title;
		}

	}	
}

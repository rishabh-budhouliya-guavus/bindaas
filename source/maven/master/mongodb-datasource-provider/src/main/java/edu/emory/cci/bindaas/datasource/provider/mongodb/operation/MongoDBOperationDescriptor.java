package edu.emory.cci.bindaas.datasource.provider.mongodb.operation;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

public class MongoDBOperationDescriptor {

	@Expose private MongoDBOperation _operation;
	@Expose private JsonObject _operation_args;
	
	public MongoDBOperation get_operation() {
		return _operation;
	}
	public void set_operation(MongoDBOperation _operation) {
		this._operation = _operation;
	}
	public JsonObject get_operation_args() {
		return _operation_args;
	}
	public void set_operation_args(JsonObject _operation_args) {
		this._operation_args = _operation_args;
	}
	
}

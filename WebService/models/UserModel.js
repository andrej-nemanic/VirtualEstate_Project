var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var UserSchema = new Schema({
	'id' : Number,
	'name' : String,
	'personalData' : Array,
	'email' : String,
	'password' : String,
	'type' : Object.freeze({
		OWNER: 'owner',
		BUYER: 'buyer'
	})
});

module.exports = mongoose.model('User', UserSchema);

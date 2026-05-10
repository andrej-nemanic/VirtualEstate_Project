var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var PropertySchema = new Schema({
	'id' : Number,
	'location' : {
	 	type: Schema.Types.ObjectId,
	 	ref: 'Location'
	},
	'type' : {
        type: String,
        enum: ['house', 'apartment', 'land', 'condominium'],
        default: 'house'
    },
	'size' : Number,
	'price' : Number,
	'buildYear' : Number,
	'description' : String,
	'pictures' : Array,
	'dateOfPosting' : Date,
	'propertyLink' : String
});

module.exports = mongoose.model('Property', PropertySchema);

/*
@Serializable
data class Property(
    val id: Int? = null,
    val address: String,
    val city: String,
    val type: String,
    val size: Double,
    val price: Double,
    val buildYear: Int,
    val description: String? = null
)
*/
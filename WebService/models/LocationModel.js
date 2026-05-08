var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var LocationSchema = new Schema({
	'location' : {
        type: { type: String, enum: ['Point'], default: 'Point' },
        coordinates: { type: [Number], required: true } // [longitude, latitude]
    },
	'address' : String,
	'city' : String
});

LocationSchema.index({ location: "2dsphere" });

module.exports = mongoose.model('Location', LocationSchema);

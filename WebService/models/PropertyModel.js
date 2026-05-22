var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var PropertySchema = new Schema({
    'region': String,
    'city': String,
    'neighborhood': String,
    'offerType': String,
    'propertyType': String,
    'size': Number,
    'price': Number,
    'description': String,
    'propertyLink': String,
    'imageUrl': String,
    'coordinates': {
        type: { type: String, enum: ['Point'], default: 'Point' },
        coordinates: { type: [Number], default: [0, 0] }
    }
});

PropertySchema.index({ coordinates: '2dsphere' });

module.exports = mongoose.model('Property', PropertySchema);

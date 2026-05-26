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
    'source': String,
    'coordinates': {
        type: { type: String, enum: ['Point'] },
        coordinates: { type: [Number] }
    }
}, { timestamps: true });

PropertySchema.index({ coordinates: '2dsphere' }, { sparse: true });

module.exports = mongoose.model('Property', PropertySchema);

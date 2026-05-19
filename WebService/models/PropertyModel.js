var mongoose = require('mongoose');
var Schema   = mongoose.Schema;

var PropertySchema = new Schema({
    'id': Number,
    'address': String,
    'city': String,
    'coordinates': {
        type: { type: String, enum: ['Point'], default: 'Point' },
        coordinates: { type: [Number], default: [0, 0] }
    },
    'type': {
        type: String,
        enum: ['house', 'apartment', 'land', 'condominium'],
        default: 'house'
    },
    'size': Number,
    'price': Number,
    'buildYear': Number,
    'description': String,
    'pictures': Array,
    'dateOfPosting': Date,
    'propertyLink': String
});

PropertySchema.index({ coordinates: '2dsphere' });

module.exports = mongoose.model('Property', PropertySchema);

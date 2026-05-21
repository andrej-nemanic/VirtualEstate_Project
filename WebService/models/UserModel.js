var mongoose = require('mongoose');
var Schema   = mongoose.Schema;
var bcrypt = require('bcrypt');

var UserSchema = new Schema({
    'name': String,
    'email': String,
    'password': String,
    'isAdmin': { type: Boolean, default: false }
});

UserSchema.pre('save', async function() {
    var user = this;
    if (!user.isModified('password')) return;
    try {
        const hash = await bcrypt.hash(user.password, 10);
        user.password = hash;
    } catch (err) {
        throw err;
    }
});

UserSchema.methods.comparePassword = function(candidatePassword, cb) {
    bcrypt.compare(candidatePassword, this.password, function(err, isMatch) {
        if (err) return cb(err);
        cb(null, isMatch);
    });
};

module.exports = mongoose.model('User', UserSchema);

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

// Funkcija pred shranjevanjem - šifriranje gesla
UserSchema.pre('save', function(next) {
    var user = this;
    // Šifriraj le, če je geslo novo ali spremenjeno
    if (!user.isModified('password')) return next();

    bcrypt.hash(user.password, 10, function(err, hash) {
        if (err) return next(err);
        user.password = hash;
        next();
    });
});

// Metoda za preverjanje gesla
UserSchema.methods.comparePassword = function(candidatePassword, cb) {
    bcrypt.compare(candidatePassword, this.password, function(err, isMatch) {
        if (err) return cb(err);
        cb(null, isMatch);
    });
};

module.exports = mongoose.model('User', UserSchema);

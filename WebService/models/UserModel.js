var mongoose = require('mongoose');
var Schema   = mongoose.Schema;
var bcrypt = require('bcrypt');

var UserSchema = new Schema({
	'id' : Number,
	'name' : String,
	'personalData' : Array,
	'email' : String,
	'password' : String,
    'type' : {
        type: String,
        enum: ['owner', 'buyer'],
        default: 'buyer'
    }
});

// Funkcija pred shranjevanjem - šifriranje gesla
UserSchema.pre('save', async function() {
    var user = this;
    
    // Če geslo ni bilo spremenjeno, preprosto prekinemo izvajanje (enako kot včasih next())
    if (!user.isModified('password')) return;

    try {
        // Uporabimo bcrypt asinhrono z await, kar je standard za novejši Node.js
        const hash = await bcrypt.hash(user.password, 10);
        user.password = hash;
    } catch (err) {
        // Če pride do napake pri šifriranju, jo vržemo naprej, da jo Mongoose uname
        throw err;
    }
});

// Metoda za preverjanje gesla
UserSchema.methods.comparePassword = function(candidatePassword, cb) {
    bcrypt.compare(candidatePassword, this.password, function(err, isMatch) {
        if (err) return cb(err);
        cb(null, isMatch);
    });
};

module.exports = mongoose.model('User', UserSchema);

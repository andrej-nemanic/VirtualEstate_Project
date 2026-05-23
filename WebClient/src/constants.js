export const OFFER_TYPES = ['Prodaja', 'Oddaja'];

export const SOURCE_OPTIONS = ['nepremicnina.si', '24nep.si', 'generator', 'ročno'];

export const PASSWORD_MIN_LENGTH = 8;
export const EMAIL_REGEX = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function validatePassword(password) {
  if (!password || typeof password !== 'string') return 'Geslo je obvezno.';
  if (password.length < PASSWORD_MIN_LENGTH) return `Geslo mora imeti vsaj ${PASSWORD_MIN_LENGTH} znakov.`;
  if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
    return 'Geslo mora vsebovati vsaj eno črko in eno številko.';
  }
  return null;
}

export function validateEmail(email) {
  if (!email || typeof email !== 'string') return 'E-pošta je obvezna.';
  if (!EMAIL_REGEX.test(email)) return 'Neveljaven format e-pošte.';
  return null;
}

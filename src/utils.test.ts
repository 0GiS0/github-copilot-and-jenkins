import { sum, average, isValidEmail, generateRandomString, deepClone, formatDate } from './utils';

describe('Utils', () => {
  describe('sum', () => {
    it('should return sum of numbers', () => {
      expect(sum([1, 2, 3, 4, 5])).toBe(15);
    });

    it('should return 0 for empty array', () => {
      expect(sum([])).toBe(0);
    });

    it('should handle negative numbers', () => {
      expect(sum([-1, 1, -2, 2])).toBe(0);
    });
  });

  describe('average', () => {
    it('should return average of numbers', () => {
      expect(average([10, 20, 30])).toBe(20);
    });

    it('should return 0 for empty array', () => {
      expect(average([])).toBe(0);
    });
  });

  describe('isValidEmail', () => {
    it('should validate correct emails', () => {
      expect(isValidEmail('test@example.com')).toBe(true);
      expect(isValidEmail('user.name@domain.org')).toBe(true);
    });

    it('should reject invalid emails', () => {
      expect(isValidEmail('invalid')).toBe(false);
      expect(isValidEmail('no@domain')).toBe(false);
      expect(isValidEmail('@nodomain.com')).toBe(false);
    });
  });

  describe('generateRandomString', () => {
    it('should generate string of specified length', () => {
      expect(generateRandomString(10)).toHaveLength(10);
      expect(generateRandomString(20)).toHaveLength(20);
    });

    it('should generate different strings', () => {
      const str1 = generateRandomString(20);
      const str2 = generateRandomString(20);
      expect(str1).not.toBe(str2);
    });
  });

  describe('deepClone', () => {
    it('should create independent copy', () => {
      const original = { a: 1, b: { c: 2 } };
      const cloned = deepClone(original);
      
      cloned.b.c = 999;
      
      expect(original.b.c).toBe(2);
      expect(cloned.b.c).toBe(999);
    });
  });

  describe('formatDate', () => {
    it('should format date as YYYY-MM-DD', () => {
      const date = new Date('2024-03-15T10:30:00Z');
      expect(formatDate(date)).toBe('2024-03-15');
    });
  });
});

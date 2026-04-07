import { sum, average, formatDate, isValidEmail, generateRandomString, deepClone } from './utils';

/**
 * Demo application entry point
 * Showcases various utility functions
 */

interface User {
  id: number;
  name: string;
  email: string;
  createdAt: Date;
}

function createUser(id: number, name: string, email: string): User {
  return {
    id,
    name,
    email,
    createdAt: new Date()
  };
}

function displayUserInfo(user: User): void {
  console.log(`
╔════════════════════════════════════════╗
║           User Information             ║
╠════════════════════════════════════════╣
║ ID: ${user.id.toString().padEnd(34)}║
║ Name: ${user.name.padEnd(32)}║
║ Email: ${user.email.padEnd(31)}║
║ Created: ${formatDate(user.createdAt).padEnd(29)}║
╚════════════════════════════════════════╝
  `);
}

function runDemo(): void {
  console.log('🚀 GitHub Copilot CLI + Jenkins Demo Application\n');

  // Demo: Math utilities
  const numbers = [10, 20, 30, 40, 50];
  console.log(`📊 Numbers: [${numbers.join(', ')}]`);
  console.log(`   Sum: ${sum(numbers)}`);
  console.log(`   Average: ${average(numbers)}\n`);

  // Demo: Email validation
  const emails = ['valid@example.com', 'invalid-email', 'another@test.org'];
  console.log('📧 Email Validation:');
  emails.forEach(email => {
    const status = isValidEmail(email) ? '✅ Valid' : '❌ Invalid';
    console.log(`   ${email}: ${status}`);
  });
  console.log();

  // Demo: Random string generation
  console.log('🎲 Random Strings:');
  for (let i = 0; i < 3; i++) {
    console.log(`   ${generateRandomString(16)}`);
  }
  console.log();

  // Demo: User creation and display
  const user = createUser(1, 'John Doe', 'john@example.com');
  displayUserInfo(user);

  // Demo: Deep clone
  const clonedUser = deepClone(user);
  clonedUser.name = 'Jane Doe';
  console.log('🔄 Original user name:', user.name);
  console.log('🔄 Cloned user name:', clonedUser.name);
  console.log();

  console.log('✨ Demo completed successfully!');
}

// Run the demo
runDemo();

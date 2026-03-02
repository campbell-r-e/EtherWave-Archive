import type { Config } from 'jest';

const config: Config = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testEnvironment: 'jsdom',
  coverageProvider: 'v8',
  collectCoverageFrom: [
    'src/app/**/*.ts',
    '!src/app/**/*.spec.ts',
    '!src/app/**/*.d.ts',
  ],
  coverageThreshold: {
    global: { statements: 90, branches: 85, functions: 90, lines: 90 }
  },
  maxWorkers: '50%',
  moduleNameMapper: {
    '^leaflet\\.heat$': '<rootDir>/src/__mocks__/leaflet.ts',
    '^leaflet(.*)$': '<rootDir>/src/__mocks__/leaflet.ts',
  },
  transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$)'],
};

export default config;

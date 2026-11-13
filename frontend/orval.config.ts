import { defineConfig } from 'orval';

export default defineConfig({
  issuetracker: {
    input: 'http://localhost:8080/v3/api-docs',
    output: {
      schemas: './src/types/api',
      clean: true,
    },
  },
});

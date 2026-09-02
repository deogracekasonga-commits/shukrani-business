import 'dotenv/config';
import { seedCatalog } from './src/catalog/index.js';
import { createApp } from './src/dashboard/app.js';

seedCatalog();

const app = createApp();
const port = process.env.PORT || 3000;

app.listen(port, () => {
  console.log(`Agent marketing Shukrani Business — dashboard sur http://localhost:${port}`);
});

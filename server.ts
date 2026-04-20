import express from 'express';
import path from 'path';
import { createServer as createViteServer } from 'vite';

// --- DATA LAYER (SIMULATION OF AVAJE/JAVA LOGIC) ---

type Status = 'AVAILABLE' | 'PENDING' | 'SOLD';

interface Category {
  id: number;
  name: string;
}

interface Pet {
  id: number;
  name: string;
  status: Status;
  categoryId: number;
}

// In-memory "Database"
let categories: Category[] = [
  { id: 1, name: 'Dogs' },
  { id: 2, name: 'Cats' },
  { id: 3, name: 'Birds' }
];

let pets: Pet[] = [
  { id: 1, name: 'Rex', status: 'AVAILABLE', categoryId: 1 },
  { id: 2, name: 'Buddy', status: 'PENDING', categoryId: 1 },
  { id: 3, name: 'Micka', status: 'AVAILABLE', categoryId: 2 },
  { id: 4, name: 'Tweety', status: 'SOLD', categoryId: 3 }
];

let orders: [];
let nextPetId = 5;

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// --- API ROUTES ---

app.get('/api/pets', (req, res) => {
  res.json(pets.map(p => ({
    ...p,
    category: categories.find(c => c.id === p.categoryId)
  })));
});

app.get('/api/categories', (req, res) => {
  res.json(categories);
});

app.post('/api/pets', (req, res) => {
  const { id, name, status, categoryId } = req.body;
  if (id) {
    const idx = pets.findIndex(p => p.id === Number(id));
    if (idx !== -1) {
      pets[idx] = { ...pets[idx], name, status, categoryId: Number(categoryId) };
    }
  } else {
    pets.push({ id: nextPetId++, name, status, categoryId: Number(categoryId) });
  }
  res.redirect('/');
});

app.post('/api/pets/buy', (req, res) => {
  const { petId } = req.body;
  const pet = pets.find(p => p.id === Number(petId));
  if (pet && pet.status === 'AVAILABLE') {
    pet.status = 'SOLD';
  }
  res.redirect('/');
});

app.post('/api/pets/delete', (req, res) => {
  const { petId } = req.body;
  pets = pets.filter(p => p.id !== Number(petId));
  res.redirect('/');
});

// --- STATIC RESOURCES (JAKARTA STANDARD) ---
const javaStaticPath = path.join(process.cwd(), 'src/main/resources/META-INF/resources');
app.use(express.static(javaStaticPath));

// --- VITE MIDDLEWARE ---

async function startServer() {
  const PORT = 3000;

  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));

    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://localhost:${PORT}`);
    console.log('AI Studio Note: Full Java/Avaje source is available in src/main/java.');
  });
}

startServer();

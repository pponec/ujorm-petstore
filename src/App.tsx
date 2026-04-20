import { useState, useEffect } from 'react';
import { ShoppingCart, Edit, Trash2, Plus, Info } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

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
  category?: Category;
}

export default function App() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [editingPet, setEditingPet] = useState<Pet | null>(null);
  const [loading, setLoading] = useState(true);

  // Form states
  const [formName, setFormName] = useState('');
  const [formStatus, setFormStatus] = useState<Status>('AVAILABLE');
  const [formCategoryId, setFormCategoryId] = useState<number>(1);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [pRes, cRes] = await Promise.all([
        fetch('/api/pets'),
        fetch('/api/categories')
      ]);
      const pData = await pRes.json();
      const cData = await cRes.json();
      setPets(pData);
      setCategories(cData);
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (pet: Pet) => {
    setEditingPet(pet);
    setFormName(pet.name);
    setFormStatus(pet.status);
    setFormCategoryId(pet.categoryId);
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
  };

  const resetForm = () => {
    setEditingPet(null);
    setFormName('');
    setFormStatus('AVAILABLE');
    setFormCategoryId(categories[0]?.id || 1);
  };

  const statusBadge = (status: Status) => {
    const colors = {
      AVAILABLE: 'bg-green-100 text-green-800 border-green-200',
      PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
      SOLD: 'bg-slate-100 text-slate-800 border-slate-200'
    };
    return (
      <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium border ${colors[status]}`}>
        {status.charAt(0) + status.slice(1).toLowerCase()}
      </span>
    );
  };

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50 py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <header className="flex flex-col md:flex-row md:items-center justify-between mb-12 border-b border-slate-200 pb-8">
          <div>
            <h1 className="text-4xl font-extrabold text-slate-900 tracking-tight mb-2">
              Ujorm PetStore
            </h1>
            <p className="text-slate-500 text-lg">
              Preview of the Java application (Node.js implementation)
            </p>
          </div>
          <div className="mt-6 md:mt-0">
            <img 
              src="https://picsum.photos/seed/ujorm/150/150" 
              alt="Ujorm Logo" 
              className="w-24 h-24 rounded-2xl shadow-sm border border-slate-200"
              referrerPolicy="no-referrer"
            />
          </div>
        </header>

        {/* Java Note */}
        <div className="mb-8 bg-blue-50 border border-blue-100 rounded-xl p-4 flex items-start gap-4">
          <Info className="w-5 h-5 text-blue-500 mt-0.5 shrink-0" />
          <div>
            <p className="text-blue-800 font-medium">Developer Note</p>
            <p className="text-blue-600 text-sm">
              The full Maven project files (pom.xml, Java Record Entities, Servlets, Dao, etc.) are available in the file explorer. 
              This interactive preview uses a Node.js shim to demonstrate functionality within the AI Studio environment.
            </p>
          </div>
        </div>

        {/* Pets Table */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden mb-12">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50/50">
            <h2 className="text-xl font-bold text-slate-800">Available Pets</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider font-semibold">
                  <th className="px-6 py-4">ID</th>
                  <th className="px-6 py-4">Name</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                <AnimatePresence initial={false}>
                  {pets.map((pet) => (
                    <motion.tr 
                      key={pet.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="px-6 py-4 text-slate-400 font-mono text-sm">#{pet.id}</td>
                      <td className="px-6 py-4 font-semibold text-slate-800">{pet.name}</td>
                      <td className="px-6 py-4">{statusBadge(pet.status)}</td>
                      <td className="px-6 py-4 text-slate-600">{pet.category?.name}</td>
                      <td className="px-6 py-4 text-right whitespace-nowrap">
                        <div className="flex justify-end gap-2">
                          <form action="/api/pets/buy" method="POST">
                            <input type="hidden" name="petId" value={pet.id} />
                            <button 
                              type="submit"
                              disabled={pet.status !== 'AVAILABLE'}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500 text-white text-sm font-medium hover:bg-emerald-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                            >
                              <ShoppingCart className="w-4 h-4" />
                              Buy
                            </button>
                          </form>
                          <button 
                            onClick={() => handleEdit(pet)}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 hover:border-blue-400 hover:text-blue-600 text-slate-600 text-sm font-medium transition-all"
                          >
                            <Edit className="w-4 h-4" />
                            Edit
                          </button>
                          <form action="/api/pets/delete" method="POST">
                            <input type="hidden" name="petId" value={pet.id} />
                            <button 
                              type="submit"
                              className="inline-flex items-center px-3 py-1.5 rounded-lg border border-slate-200 hover:border-red-400 hover:text-red-600 text-slate-400 text-sm font-medium transition-all"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </form>
                        </div>
                      </td>
                    </motion.tr>
                  ))}
                </AnimatePresence>
              </tbody>
            </table>
          </div>
        </div>

        {/* Add/Edit Form */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-200 bg-slate-50/50 flex justify-between items-center">
            <h2 className="text-xl font-bold text-slate-800">
              {editingPet ? 'Update Pet' : 'Register New Pet'}
            </h2>
            {editingPet && (
              <button onClick={resetForm} className="text-xs text-blue-600 hover:underline">
                Clear
              </button>
            )}
          </div>
          <form action="/api/pets" method="POST" className="p-6">
            {editingPet && <input type="hidden" name="id" value={editingPet.id} />}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              <div className="md:col-span-1">
                <label className="block text-sm font-semibold text-slate-700 mb-2">Pet Name</label>
                <input 
                  name="name" 
                  value={formName} 
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="e.g. Rex" 
                  required
                  className="block w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-medium text-slate-800 placeholder:text-slate-300"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">Availability</label>
                <select 
                  name="status"
                  value={formStatus}
                  onChange={(e) => setFormStatus(e.target.value as Status)}
                  className="block w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-medium text-slate-800"
                >
                  <option value="AVAILABLE">Available</option>
                  <option value="PENDING">Pending</option>
                  <option value="SOLD">Sold</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">Category</label>
                <select 
                  name="categoryId"
                  value={formCategoryId}
                  onChange={(e) => setFormCategoryId(Number(e.target.value))}
                  className="block w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-medium text-slate-800"
                >
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex items-end">
                <button 
                  type="submit"
                  className="w-full h-11 flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl shadow-lg shadow-blue-500/20 active:scale-95 transition-all"
                >
                  {editingPet ? <Edit className="w-5 h-5" /> : <Plus className="w-5 h-5" />}
                  {editingPet ? 'Update' : 'Register'}
                </button>
              </div>
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}

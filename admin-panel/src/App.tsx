import React, { useState, useEffect } from 'react';

// Basic Login Component
function Login({ onLogin }: { onLogin: (token: string) => void }) {
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    try {
      const res = await fetch('/api/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password })
      });
      
      if (!res.ok) {
        throw new Error('Invalid password');
      }
      
      const data = await res.json();
      onLogin(data.token);
    } catch (err) {
      setError('Invalid password or server error');
    }
  };

  return (
    <div className="login-wrapper">
      <div className="glass-panel login-card">
        <h2>Workouted Admin</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Admin Password</label>
            <input 
              type="password" 
              className="input" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password..."
            />
            {error && <p className="error-text">{error}</p>}
          </div>
          <button type="submit" className="btn" style={{ width: '100%' }}>Login</button>
        </form>
      </div>
    </div>
  );
}

// UserDetails Component
function UserDetails({ userId, token, onBack }: { userId: number, token: string, onBack: () => void }) {
  const [details, setDetails] = useState<any>(null);

  useEffect(() => {
    const fetchDetails = async () => {
      try {
        const res = await fetch(`/api/admin/users/${userId}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (res.ok) {
          setDetails(await res.json());
        }
      } catch (err) {
        console.error('Error fetching user details', err);
      }
    };
    fetchDetails();
  }, [userId, token]);

  const deleteWorkout = async (workoutId: number) => {
    if (!confirm('Are you sure you want to delete this workout?')) return;
    try {
      const res = await fetch(`/api/admin/workouts/${workoutId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        setDetails((prev: any) => ({
          ...prev,
          workouts: prev.workouts.filter((w: any) => w.id !== workoutId)
        }));
      }
    } catch (err) {
      console.error('Error deleting workout', err);
    }
  };

  if (!details) return <div style={{ padding: '2rem' }}>Loading user details...</div>;

  const { user, ranks, workouts } = details;

  return (
    <div>
      <div style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <button onClick={onBack} className="btn" style={{ padding: '0.4rem 0.8rem', background: 'var(--bg-main)', color: 'var(--text-main)', border: '1px solid var(--border-color)' }}>
          &larr; Back
        </button>
        <h2 style={{ fontWeight: 600 }}>{user.username}'s Profile</h2>
      </div>

      <div className="dashboard-grid">
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem', fontSize: '1rem' }}>Info</h3>
          <p><strong>Height:</strong> {user.height || '-'} cm</p>
          <p><strong>Weight:</strong> {user.weight || '-'} kg</p>
          <p><strong>Age:</strong> {user.age || '-'}</p>
          <p><strong>Friend Code:</strong> {user.friendCode}</p>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem', fontSize: '1rem' }}>Muscle Ranks</h3>
          {ranks.length === 0 ? <p className="stat-label">No ranks yet.</p> : (
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {ranks.map((r: any) => (
                <li key={r.muscleId} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.25rem' }}>
                  <span style={{ textTransform: 'capitalize' }}>{r.muscleId}</span>
                  <strong style={{ color: 'var(--primary)' }}>{r.rankName}</strong>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="glass-panel" style={{ padding: '1.5rem' }}>
        <h3 style={{ marginBottom: '1rem', fontSize: '1rem' }}>Workouts ({workouts.length})</h3>
        {workouts.length === 0 ? <p className="stat-label">No workouts found.</p> : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Date</th>
                  <th>Sets</th>
                  <th>Notes</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {workouts.map((w: any) => (
                  <tr key={w.id}>
                    <td>{w.id}</td>
                    <td>{new Date(w.timestamp).toLocaleString()}</td>
                    <td>{w.setsCount}</td>
                    <td>{w.notes || '-'}</td>
                    <td>
                      <button onClick={() => deleteWorkout(w.id)} className="btn btn-danger" style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem' }}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

// Dashboard Component
function Dashboard({ token, onLogout }: { token: string, onLogout: () => void }) {
  const [stats, setStats] = useState({ usersCount: 0, workoutsCount: 0 });
  const [users, setUsers] = useState<any[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const headers = { 'Authorization': `Bearer ${token}` };
        const [statsRes, usersRes] = await Promise.all([
          fetch('/api/admin/stats', { headers }),
          fetch('/api/admin/users', { headers })
        ]);
        
        if (statsRes.ok) setStats(await statsRes.json());
        if (usersRes.ok) setUsers(await usersRes.json());
      } catch (err) {
        console.error('Error fetching data', err);
      }
    };
    
    fetchStats();
  }, [token]);

  const deleteUser = async (id: number) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    
    try {
      const res = await fetch(`/api/admin/users/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (res.ok) {
        setUsers(users.filter(u => u.id !== id));
        setStats(s => ({ ...s, usersCount: s.usersCount - 1 }));
      }
    } catch (err) {
      console.error('Failed to delete user');
    }
  };

  return (
    <div className="app-container">
      <header className="header glass-panel" style={{ borderRadius: 0, borderTop: 0, borderLeft: 0, borderRight: 0 }}>
        <h1>Workouted Admin</h1>
        <button onClick={onLogout} className="btn btn-danger" style={{ padding: '0.5rem 1rem' }}>Logout</button>
      </header>
      
      <main className="main-content">
        {selectedUserId ? (
          <UserDetails userId={selectedUserId} token={token} onBack={() => setSelectedUserId(null)} />
        ) : (
          <>
            <div className="dashboard-grid">
              <div className="stat-card glass-panel">
                <span className="stat-label">Total Users</span>
                <span className="stat-value">{stats.usersCount}</span>
              </div>
              <div className="stat-card glass-panel">
                <span className="stat-label">Total Workouts</span>
                <span className="stat-value">{stats.workoutsCount}</span>
              </div>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h2 style={{ marginBottom: '1.5rem', fontWeight: 600 }}>Users Management</h2>
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Username</th>
                      <th>Friend Code</th>
                      <th>Height / Weight</th>
                      <th>Age</th>
                      <th>Created At</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map(user => (
                      <tr key={user.id}>
                        <td>{user.id}</td>
                        <td>{user.username}</td>
                        <td><span style={{ color: 'var(--primary)', fontWeight: 600 }}>{user.friendCode}</span></td>
                        <td>{user.height || '-'} cm / {user.weight || '-'} kg</td>
                        <td>{user.age || '-'}</td>
                        <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                        <td>
                          <button onClick={() => setSelectedUserId(user.id)} className="btn" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem', marginRight: '0.5rem', background: 'var(--bg-card)', color: 'var(--text-main)', border: '1px solid var(--border-color)' }}>View</button>
                          <button onClick={() => deleteUser(user.id)} className="btn btn-danger" style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}>Delete</button>
                        </td>
                      </tr>
                    ))}
                    {users.length === 0 && (
                      <tr>
                        <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                          No users found.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('admin_token'));

  const handleLogin = (newToken: string) => {
    localStorage.setItem('admin_token', newToken);
    setToken(newToken);
  };

  const handleLogout = () => {
    localStorage.removeItem('admin_token');
    setToken(null);
  };

  if (!token) {
    return <Login onLogin={handleLogin} />;
  }

  return <Dashboard token={token} onLogout={handleLogout} />;
}

export default App;

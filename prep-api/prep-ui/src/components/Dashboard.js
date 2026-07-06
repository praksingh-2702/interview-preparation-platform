import React, { useState, useEffect } from 'react';
import api from '../services/api';

const ALL_TOPICS = [
    "Arrays", "Binary Search", "Bit Manipulation", "Linked Lists", 
    "Stacks & Queues", "Recursion & Backtracking", "Greedy Algorithms", 
    "Trees", "Graphs", "Dynamic Programming"
];

const Dashboard = ({ onLogout }) => {
    const [questions, setQuestions] = useState([]);
    const [progressMap, setProgressMap] = useState({});
    const [companyFilter, setCompanyFilter] = useState('');
    const [difficultyFilter, setDifficultyFilter] = useState('');
    const [loading, setLoading] = useState(false);
    
    const [expandedTopics, setExpandedTopics] = useState({ "Arrays": true, "Linked Lists": true });
    const token = localStorage.getItem('token');

    const fetchData = async () => {
        setLoading(true);
        try {
            const currentToken = localStorage.getItem('token');
            const qRes = await api.get('/api/questions', {
                params: { company: companyFilter, difficulty: difficultyFilter },
                headers: { Authorization: `Bearer ${currentToken}` }
            });
            setQuestions(qRes.data);

            const progRes = await api.get('/api/progress/my-status', {
                headers: { Authorization: `Bearer ${currentToken}` }
            });
            
            // CRITICAL FIX: Normalize map keys to Strings explicitly
            const mappedProgress = {};
            if (progRes.data && typeof progRes.data === 'object' && !Array.isArray(progRes.data)) {
                Object.keys(progRes.data).forEach(key => {
                    mappedProgress[String(key)] = progRes.data[key];
                });
                setProgressMap(mappedProgress);
            } else if (Array.isArray(progRes.data)) {
                progRes.data.forEach(item => {
                    mappedProgress[String(item.question.id)] = {
                        solved: item.solved,
                        revised: item.revised
                    };
                });
                setProgressMap(mappedProgress);
            } else {
                setProgressMap({});
            }
        } catch (error) {
            console.error("Error pulling platform info:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [companyFilter, difficultyFilter]);

    const handleCheckboxToggle = async (questionId, field, currentValue) => {
        const stringId = String(questionId);
        const currentRecord = progressMap[stringId] || { solved: false, revised: false };
        
        const updatedPayload = {
            questionId,
            solved: field === 'solved' ? !currentValue : currentRecord.solved,
            revised: field === 'revised' ? !currentValue : currentRecord.revised
        };

        // Optimistically update frontend state UI instantly to unlock animations
        setProgressMap(prev => ({
            ...prev,
            [stringId]: {
                solved: updatedPayload.solved,
                revised: updatedPayload.revised
            }
        }));

        try {
            const currentToken = localStorage.getItem('token');
            await api.post('/api/progress/toggle', updatedPayload, {
                headers: { Authorization: `Bearer ${currentToken}` }
            });
        } catch (error) {
            console.error("Failed to update execution progress metrics:", error);
            // Revert state back if network fails completely
            setProgressMap(prev => ({
                ...prev,
                [stringId]: currentRecord
            }));
        }
    };

    const toggleTopic = (topic) => {
        setExpandedTopics(prev => ({ ...prev, [topic]: !prev[topic] }));
    };

    // --- SEGMENTED DIFFICULTY CALCULATIONS ---
    const getCountByDiff = (diff) => questions.filter(q => q.difficulty === diff).length;
    const getSolvedByDiff = (diff) => questions.filter(q => q.difficulty === diff && progressMap[String(q.id)]?.solved).length;

    const easyTotal = getCountByDiff("EASY"), easySolved = getSolvedByDiff("EASY");
    const medTotal = getCountByDiff("MEDIUM"), medSolved = getSolvedByDiff("MEDIUM");
    const hardTotal = getCountByDiff("HARD"), hardSolved = getSolvedByDiff("HARD");
    
    const overallTotal = questions.length;
    const overallSolved = easySolved + medSolved + hardSolved;
    const overallPct = overallTotal > 0 ? Math.round((overallSolved / overallTotal) * 100) : 0;

    return (
        <div style={{ background: '#0d1117', minHeight: '100vh', color: '#c9d1d9', padding: '30px 20px', fontFamily: '-apple-system, sans-serif' }}>
            <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
                
                {/* Header Navbar - Clean Rebranded Typography Without Emojis */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '25px' }}>
                    <h2 style={{ margin: 0, fontSize: '24px', color: '#f0f6fc', letterSpacing: '-0.5px' }}>
                        FreakCode <span style={{ color: '#58a6ff', fontWeight: '400' }}>Top 100</span>
                    </h2>
                    <button onClick={onLogout} style={{ padding: '8px 16px', background: '#da3637', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: '600' }}>Logout</button>
                </div>

                {/* Split Metrics Block */}
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '20px', marginBottom: '25px' }}>
                    
                    {/* Linear Details Bar Panel */}
                    <div style={{ background: '#161b22', border: '1px solid #30363d', borderRadius: '12px', padding: '20px', display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: '15px' }}>
                        <div>
                            <h4 style={{ margin: '0 0 5px 0', color: '#8b949e', fontSize: '12px', textTransform: 'uppercase' }}>Syllabus Coverage Tracking</h4>
                            <div style={{ fontSize: '20px', fontWeight: '700', color: '#f0f6fc' }}>{overallSolved} / {overallTotal} Mastered</div>
                        </div>
                        <div style={{ width: '100%', height: '8px', background: '#21262d', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{ width: `${overallPct}%`, height: '100%', background: 'linear-gradient(90deg, #58a6ff, #238636)', transition: '0.3s' }} />
                        </div>
                        <div style={{ display: 'flex', gap: '20px', fontSize: '13px' }}>
                            <span style={{ color: '#238636' }}>Easy: {easySolved}/{easyTotal}</span>
                            <span style={{ color: '#d29922' }}>Medium: {medSolved}/{medTotal}</span>
                            <span style={{ color: '#f85149' }}>Hard: {hardSolved}/{hardTotal}</span>
                        </div>
                    </div>

                    {/* Circular Segmented Indicator Ring */}
                    <div style={{ background: '#161b22', border: '1px solid #30363d', borderRadius: '12px', padding: '15px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                        <div style={{ width: '100px', height: '100px', borderRadius: '50%', background: `conic-gradient(#238636 0% ${overallTotal ? (easySolved/overallTotal)*360 : 0}deg, #d29922 ${overallTotal ? (easySolved/overallTotal)*360 : 0}deg ${overallTotal ? ((easySolved+medSolved)/overallTotal)*360 : 0}deg, #f85149 ${overallTotal ? ((easySolved+medSolved)/overallTotal)*360 : 0}deg 360deg)`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <div style={{ width: '76px', height: '76px', borderRadius: '50%', background: '#161b22', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                                <span style={{ fontSize: '18px', fontWeight: '700', color: '#f0f6fc' }}>{overallPct}%</span>
                                <span style={{ fontSize: '10px', color: '#8b949e' }}>Done</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Filter Toolbar */}
                <div style={{ display: 'flex', gap: '15px', marginBottom: '25px', background: '#161b22', padding: '14px', borderRadius: '12px', border: '1px solid #30363d' }}>
                    <select value={companyFilter} onChange={(e) => setCompanyFilter(e.target.value)} style={{ padding: '8px 12px', background: '#21262d', color: '#c9d1d9', border: '1px solid #30363d', borderRadius: '6px' }}>
                        <option value="">All Companies</option>
                        <option value="Google">Google</option><option value="Amazon">Amazon</option><option value="PhonePe">PhonePe</option><option value="Microsoft">Microsoft</option>
                    </select>
                    <select value={difficultyFilter} onChange={(e) => setDifficultyFilter(e.target.value)} style={{ padding: '8px 12px', background: '#21262d', color: '#c9d1d9', border: '1px solid #30363d', borderRadius: '6px' }}>
                        <option value="">All Difficulties</option>
                        <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
                    </select>
                </div>

                {/* Collapsible Accordion Syllabus Blocks */}
                {loading ? <p style={{ textAlign: 'center', color: '#8b949e' }}>Refreshing directory index mappings...</p> : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        {ALL_TOPICS.map(topic => {
                            // 1. Group questions belonging to this specific DSA topic category
                            const categoryQuestions = questions.filter(q => q.category && q.category.toLowerCase() === topic.toLowerCase());
                            
                            // 🚀 2. CRITICAL FIX: Intercept the loop and filter the sub-rows matching active selection states
                            const topicQuestions = categoryQuestions.filter(q => {
                                const matchesCompany = !companyFilter || (q.companies && Array.from(q.companies).includes(companyFilter));
                                const matchesDifficulty = !difficultyFilter || q.difficulty === difficultyFilter;
                                return matchesCompany && matchesDifficulty;
                            });

                            const isExpanded = !!expandedTopics[topic];
                            
                            // If filters are active and this accordion category has zero inner matches, hide it completely
                            if ((companyFilter || difficultyFilter) && topicQuestions.length === 0) return null;

                            return (
                                <div key={topic} style={{ background: '#161b22', border: '1px solid #30363d', borderRadius: '8px', overflow: 'hidden' }}>
                                    
                                    {/* Collapsible Header Banner */}
                                    <div onClick={() => toggleTopic(topic)} style={{ padding: '14px 16px', background: '#21262d', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center', userSelect: 'none' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                            <span style={{ color: '#8b949e', fontSize: '10px', transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.2s', display: 'inline-block' }}>▶</span>
                                            <span style={{ fontWeight: '600', color: '#f0f6fc', fontSize: '15px' }}>{topic}</span>
                                            <span style={{ background: '#30363d', color: '#8b949e', padding: '2px 8px', borderRadius: '10px', fontSize: '11px' }}>{topicQuestions.length}</span>
                                        </div>
                                    </div>

                                    {/* Nested Table Container */}
                                    {isExpanded && (
                                        <div style={{ borderTop: '1px solid #30363d' }}>
                                            {topicQuestions.length === 0 ? (
                                                <p style={{ padding: '20px', margin: 0, fontSize: '13px', color: '#8b949e', textAlign: 'center' }}>No problems seeded under this heading yet.</p>
                                            ) : (
                                                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                                                    <tbody>
                                                        {topicQuestions.map(q => {
                                                            const isSolvedChecked = !!progressMap[String(q.id)]?.solved;
                                                            const isRevisedChecked = !!progressMap[String(q.id)]?.revised;

                                                            return (
                                                                <tr key={q.id} style={{ borderBottom: '1px solid #21262d' }}>
                                                                    <td style={{ padding: '14px 16px', fontWeight: '600', width: '25%' }}>
                                                                        <a href={q.problemUrl} target="_blank" rel="noreferrer" style={{ color: '#58a6ff', textDecoration: 'none' }}>
                                                                            {q.title}
                                                                        </a>
                                                                    </td>
                                                                    <td style={{ padding: '14px 16px', color: '#8b949e', fontSize: '13px', width: '40%' }}>{q.description}</td>
                                                                    <td style={{ padding: '14px 16px', width: '10%' }}>
                                                                        <span style={{ padding: '3px 8px', borderRadius: '12px', fontSize: '11px', color: 'white', fontWeight: '600', background: q.difficulty === 'EASY' ? '#238636' : q.difficulty === 'MEDIUM' ? '#d29922' : '#f85149' }}>
                                                                            {q.difficulty}
                                                                        </span>
                                                                    </td>
                                                                    <td style={{ padding: '14px 16px', fontSize: '13px', color: '#c9d1d9', width: '10%' }}>
                                                                        {q.companies ? Array.from(q.companies).join(', ') : 'General'}
                                                                    </td>
                                                                    <td style={{ padding: '14px 16px', width: '15%' }}>
                                                                        <div style={{ display: 'flex', gap: '15px', justifyContent: 'flex-end', alignItems: 'center' }}>
                                                                            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#238636', cursor: 'pointer', userSelect: 'none' }}>
                                                                                <input 
                                                                                    type="checkbox" 
                                                                                    checked={isSolvedChecked} 
                                                                                    onChange={() => handleCheckboxToggle(q.id, 'solved', isSolvedChecked)}
                                                                                    style={{ accentColor: '#238636', width: '15px', height: '15px', cursor: 'pointer' }}
                                                                                />
                                                                                Solved
                                                                            </label>
                                                                            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#d29922', cursor: 'pointer', userSelect: 'none' }}>
                                                                                <input 
                                                                                    type="checkbox" 
                                                                                    checked={isRevisedChecked} 
                                                                                    onChange={() => handleCheckboxToggle(q.id, 'revised', isRevisedChecked)}
                                                                                    style={{ accentColor: '#d29922', width: '15px', height: '15px', cursor: 'pointer' }}
                                                                                />
                                                                                Revised
                                                                            </label>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            );
                                                        })}
                                                    </tbody>
                                                </table>
                                            )}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

export default Dashboard;
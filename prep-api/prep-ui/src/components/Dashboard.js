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
    
    // Track which topic headings are expanded or collapsed
    const [expandedTopics, setExpandedTopics] = useState({ "Arrays": true, "Linked Lists": true });

    const token = localStorage.getItem('token');

    const fetchData = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            const qRes = await api.get('/api/questions', {
                params: { company: companyFilter, difficulty: difficultyFilter },
                headers: { Authorization: `Bearer ${token}` }
            });
            setQuestions(qRes.data);

            const progRes = await api.get('/api/progress/my-status', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setProgressMap(progRes.data);
        } catch (error) {
            console.error("Error pulling platform info:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [companyFilter, difficultyFilter]);

    const handleStatusClick = async (questionId, statusType, problemUrl) => {
        try {
            await api.post('/api/progress/complete', { questionId, statusType }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            // Instant map reload trigger
            const res = await api.get('/api/progress/my-status', { headers: { Authorization: `Bearer ${token}` } });
            setProgressMap(res.data);
            window.open(problemUrl, '_blank', 'noreferrer');
        } catch (error) {
            console.error("Progress trace failure:", error);
            window.open(problemUrl, '_blank', 'noreferrer');
        }
    };

    const toggleTopic = (topic) => {
        setExpandedTopics(prev => ({ ...prev, [topic]: !prev[topic] }));
    };

    // --- SEGMENTED DIFFICULTY CALCULATIONS ---
    const getCountByDiff = (diff) => questions.filter(q => q.difficulty === diff).length;
    const getSolvedByDiff = (diff) => questions.filter(q => q.difficulty === diff && (progressMap[q.id] === "FIRST_SOLVE" || progressMap[q.id] === "REVISION_DONE")).length;

    const easyTotal = getCountByDiff("EASY"), easySolved = getSolvedByDiff("EASY");
    const medTotal = getCountByDiff("MEDIUM"), medSolved = getSolvedByDiff("MEDIUM");
    const hardTotal = getCountByDiff("HARD"), hardSolved = getSolvedByDiff("HARD");
    
    const overallTotal = questions.length;
    const overallSolved = easySolved + medSolved + hardSolved;
    const overallPct = overallTotal > 0 ? Math.round((overallSolved / overallTotal) * 100) : 0;

    return (
        <div style={{ background: '#0d1117', minHeight: '100vh', color: '#c9d1d9', padding: '30px 20px', fontFamily: '-apple-system, sans-serif' }}>
            <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
                
                {/* Header Navbar */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '25px' }}>
                    <h2 style={{ margin: 0, fontSize: '24px', color: '#f0f6fc' }}>🚀 PrepMind <span style={{ color: '#58a6ff', fontWeight: '400' }}>Tech Engine</span></h2>
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
                            <span style={{ color: '#238636' }}>● Easy: {easySolved}/{easyTotal}</span>
                            <span style={{ color: '#d29922' }}>● Medium: {medSolved}/{medTotal}</span>
                            <span style={{ color: '#f85149' }}>● Hard: {hardSolved}/{hardTotal}</span>
                        </div>
                    </div>

                    {/* Circular Segmented Tracking Indicator Ring */}
                    <div style={{ background: '#161b22', border: '1px solid #30363d', borderRadius: '12px', padding: '15px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', position: 'relative' }}>
                        <div style={{ width: '100px', height: '100px', borderRadius: '50%', background: `conic-gradient(#238636 0% ${easyTotal ? (easySolved/overallTotal)*360 : 0}deg, #d29922 ${easyTotal ? (easySolved/overallTotal)*360 : 0}deg ${medTotal ? ((easySolved+medSolved)/overallTotal)*360 : 0}deg, #f85149 ${medTotal ? ((easySolved+medSolved)/overallTotal)*360 : 0}deg 360deg)`, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
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
                            // FIXED: Changed q.topic to q.category to read from your PostgreSQL database fields correctly
                            const topicQuestions = questions.filter(q => q.category && q.category.toLowerCase() === topic.toLowerCase());
                            const isExpanded = !!expandedTopics[topic];
                            
                            if ((companyFilter || difficultyFilter) && topicQuestions.length === 0) return null;

                            return (
                                <div key={topic} style={{ background: '#161b22', border: '1px solid #30363d', borderRadius: '8px', overflow: 'hidden' }}>
                                    
                                    {/* Collapsible Header Banner */}
                                    <div onClick={() => toggleTopic(topic)} style={{ padding: '14px 16px', background: '#21262d', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center', userSelect: 'none' }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                            <span style={{ color: '#8b949e', fontSize: '12px', transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }}>▶</span>
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
                                                            const currentStatus = progressMap[q.id] || "";
                                                            const isFirstSolved = currentStatus === "FIRST_SOLVE" || currentStatus === "REVISION_DONE";
                                                            const isRevised = currentStatus === "REVISION_DONE";

                                                            return (
                                                                <tr key={q.id} style={{ borderBottom: '1px solid #21262d' }}>
                                                                    <td style={{ padding: '14px 16px', fontWeight: '600', color: '#f0f6fc', width: '25%' }}>{q.title}</td>
                                                                    <td style={{ padding: '14px 16px', color: '#8b949e', fontSize: '13px', width: '45%' }}>{q.description}</td>
                                                                    <td style={{ padding: '14px 16px', width: '10%' }}>
                                                                        <span style={{ padding: '3px 8px', borderRadius: '12px', fontSize: '11px', color: 'white', fontWeight: '600', background: q.difficulty === 'EASY' ? '#238636' : q.difficulty === 'MEDIUM' ? '#d29922' : '#f85149' }}>
                                                                            {q.difficulty}
                                                                        </span>
                                                                    </td>
                                                                    <td style={{ padding: '14px 16px', fontSize: '13px', color: '#58a6ff', width: '10%' }}>
                                                                        {q.companies ? Array.from(q.companies).join(', ') : 'General'}
                                                                    </td>
                                                                    <td style={{ padding: '14px 16px', display: 'flex', gap: '8px', justifyContent: 'flex-end', alignItems: 'center' }}>
                                                                        <button onClick={() => handleStatusClick(q.id, "FIRST_SOLVE", q.problemUrl)} style={{ padding: '5px 10px', border: '1px solid #238636', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: '600', background: isFirstSolved ? '#238636' : 'transparent', color: isFirstSolved ? 'white' : '#238636' }}>
                                                                            {isFirstSolved ? "✅ Solved" : "Solve"}
                                                                        </button>
                                                                        <button onClick={() => handleStatusClick(q.id, "REVISION_DONE", q.problemUrl)} style={{ padding: '5px 10px', border: '1px solid #d29922', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: '600', background: isRevised ? '#d29922' : 'transparent', color: isRevised ? 'white' : '#d29922' }}>
                                                                            {isRevised ? "🔄 Revised" : "Revise"}
                                                                        </button>
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
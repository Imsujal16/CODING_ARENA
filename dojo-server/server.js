'use strict';

const http  = require('http');
const { Server } = require('socket.io');
const axios = require('axios');
const fs = require('fs');

const PORT     = process.env.PORT || 3001;
const API_BASE = 'https://leetcodee-sigma.vercel.app/api';

const mcqData = JSON.parse(fs.readFileSync('./mcq_questions.json', 'utf8'));

const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
        ok: true,
        service: 'dojo-battle-server',
        socket: 'Socket.IO ready'
    }));
});
const io     = new Server(server, { cors: { origin: '*' } });

// ── In-memory state ──────────────────────────────────────────────────────────
const rooms   = {};   // roomCode → Room
const players = {};   // socketId → { roomCode, userId, username }
const quickMatchQueue = [];

// ── Helpers ──────────────────────────────────────────────────────────────────
function randomCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];
    return rooms[code] ? randomCode() : code;
}

async function fetchDailyProblem() {
    try {
        const { data } = await axios.get(`${API_BASE}/leetcode/daily`, { timeout: 8000 });
        return {
            title:     data.title      || data.titleSlug  || 'Daily Challenge',
            titleSlug: data.titleSlug  || data.slug       || 'two-sum',
            difficulty:data.difficulty || 'Medium',
            url: `https://leetcode.com/problems/${data.titleSlug || 'two-sum'}/`
        };
    } catch {
        return { title: 'Two Sum', titleSlug: 'two-sum', difficulty: 'Easy',
                 url: 'https://leetcode.com/problems/two-sum/' };
    }
}

async function checkSolved(username, problemSlug, afterTimestamp) {
    if (!username || !problemSlug) return false;
    try {
        const { data } = await axios.post(`${API_BASE}/leetcode/check-solved`,
            { username, problemSlug, afterTimestamp }, { timeout: 8000 });
        return data.solved === true;
    } catch { return false; }
}

function formatTime(ms) {
    const s = Math.floor(ms / 1000);
    return `${String(Math.floor(s / 60)).padStart(2,'0')}:${String(s % 60).padStart(2,'0')}`;
}

function makePlayer(socket, data, fallbackName) {
    return {
        socketId:        socket.id,
        userId:          data.userId          || socket.id,
        username:        data.username        || fallbackName,
        avatarUrl:       data.avatarUrl       || '',
        leetcodeUsername:data.leetcodeUsername || data.username || fallbackName
    };
}

function removeFromQuickMatch(socketId) {
    const index = quickMatchQueue.findIndex(entry => entry.socketId === socketId);
    if (index >= 0) quickMatchQueue.splice(index, 1);
}

// ── Battle: end & cleanup ────────────────────────────────────────────────────
function endBattle(roomCode, winnerSocketId, loserSocketId, reason) {
    const room = rooms[roomCode];
    if (!room || room.status === 'finished') return;
    room.status = 'finished';
    if (room.pollInterval) clearInterval(room.pollInterval);

    const winner = room.host?.socketId === winnerSocketId ? room.host : room.guest;
    const loser  = room.host?.socketId === loserSocketId  ? room.host : room.guest;
    const elapsed = Date.now() - room.startTime;

    if (winnerSocketId) {
        io.to(winnerSocketId).emit('battle:end', {
            won: true,
            opponentName:  loser?.username  || 'Opponent',
            problemTitle:  room.problem?.title || (room.mode === 'mcq' ? 'MCQ Battle' : ''),
            problemSlug:   room.problem?.titleSlug || '',
            time:          formatTime(elapsed),
            accuracy:      100,
            xpGained:      250,
            coins:         room.entryFee || 0,
            reason:        reason || 'solved'
        });
    }
    if (loserSocketId) {
        io.to(loserSocketId).emit('battle:end', {
            won:           false,
            opponentName:  winner?.username || 'Opponent',
            problemTitle:  room.problem?.title || (room.mode === 'mcq' ? 'MCQ Battle' : ''),
            problemSlug:   room.problem?.titleSlug || '',
            time:          formatTime(elapsed),
            accuracy:      0,
            xpGained:      25,
            coins:         -(room.entryFee || 0),
            reason:        reason || 'opponent_won'
        });
    }
    setTimeout(() => { delete rooms[roomCode]; }, 60000);
}

// ── Battle: win-condition polling ────────────────────────────────────────────
function startWinPolling(roomCode) {
    const room = rooms[roomCode];
    if (!room) return;
    room.pollInterval = setInterval(async () => {
        const r = rooms[roomCode];
        if (!r || r.status !== 'in_progress') { clearInterval(r?.pollInterval); return; }

        const slug = r.problem?.titleSlug;
        const ts   = r.startTime;

        if (r.host) {
            const won = await checkSolved(r.host.leetcodeUsername, slug, ts);
            if (won) { endBattle(roomCode, r.host.socketId, r.guest?.socketId, 'solved'); return; }
        }
        if (r.guest) {
            const won = await checkSolved(r.guest.leetcodeUsername, slug, ts);
            if (won) { endBattle(roomCode, r.guest.socketId, r.host?.socketId, 'solved'); return; }
        }
    }, 15000);
}

// ── Register listeners helper (used in both create & join paths) ──────────
async function startBattleInRoom(roomCode) {
    const room = rooms[roomCode];
    if (!room) return;
    
    if (room.mode === 'mcq') {
        room.status = 'voting';
        io.to(roomCode).emit('battle:voting_start', { topics: Object.keys(mcqData) });
        return;
    }

    console.log(`[Battle] Fetching problem for room ${roomCode}...`);
    const problem    = await fetchDailyProblem();
    room.problem     = problem;
    room.status      = 'in_progress';
    room.startTime   = Date.now();
    io.to(roomCode).emit('battle:start', { problem, mode: 'coding' });
    console.log(`[Battle] Started in ${roomCode}: ${problem.title}`);
    startWinPolling(roomCode);
}

// ── Socket.IO ─────────────────────────────────────────────────────────────────
io.on('connection', (socket) => {
    console.log(`[+] ${socket.id}`);

    // ── Create room ──────────────────────────────────────────────────────────
    socket.on('room:create', (data) => {
        removeFromQuickMatch(socket.id);
        const code = randomCode();
        rooms[code] = {
            code,
            host: makePlayer(socket, data, 'Player1'),
            guest:      null,
            difficulty: data.difficulty || 'MEDIUM',
            mode:       data.mode       || 'coding', // 'coding' or 'mcq'
            entryFee:   data.entryFee   || 0,
            status:     'waiting',
            problem:    null,
            startTime:  null,
            pollInterval: null,
            votes:      {},
            scores:     {}
        };
        players[socket.id] = { roomCode: code, userId: data.userId, username: data.username };
        socket.join(code);
        socket.emit('room:created', { room: rooms[code] });
        console.log(`[Room] ${code} created by ${data.username}`);
    });

    // ── Join room ────────────────────────────────────────────────────────────
    socket.on('room:join', async (data) => {
        removeFromQuickMatch(socket.id);
        const room = rooms[data.roomCode];
        if (!room)                      { socket.emit('room:error', { message: 'Room not found' });        return; }
        if (room.status !== 'waiting')  { socket.emit('room:error', { message: 'Room already started' }); return; }
        if (room.host.userId === data.userId) { socket.emit('room:error', { message: 'Cannot join your own room' }); return; }

        room.guest = makePlayer(socket, data, 'Player2');
        room.status = 'starting';
        players[socket.id] = { roomCode: data.roomCode, userId: data.userId, username: data.username };
        socket.join(data.roomCode);

        io.to(room.host.socketId).emit('room:joined',  { player: room.guest });
        io.to(room.guest.socketId).emit('room:joined',  { player: room.host });
        io.to(data.roomCode).emit('room:update',  { room });
        await startBattleInRoom(data.roomCode);
    });

    // ── MCQ Events ───────────────────────────────────────────────────────────
    socket.on('match:find', async (data) => {
        removeFromQuickMatch(socket.id);
        const seeker = {
            socketId: socket.id,
            player: makePlayer(socket, data, 'Player'),
            difficulty: data.difficulty || 'medium',
            mode: data.mode || 'coding',
            entryFee: data.entryFee || 0
        };

        const matchIndex = quickMatchQueue.findIndex(entry =>
            entry.socketId !== socket.id &&
            entry.mode === seeker.mode &&
            entry.difficulty === seeker.difficulty
        );

        if (matchIndex < 0) {
            quickMatchQueue.push(seeker);
            socket.emit('match:waiting');
            return;
        }

        const opponent = quickMatchQueue.splice(matchIndex, 1)[0];
        const code = randomCode();
        rooms[code] = {
            code,
            host: opponent.player,
            guest: seeker.player,
            difficulty: seeker.difficulty,
            mode: seeker.mode,
            entryFee: seeker.entryFee,
            status: 'starting',
            problem: null,
            startTime: null,
            pollInterval: null,
            votes: {},
            scores: {}
        };

        players[opponent.socketId] = {
            roomCode: code,
            userId: opponent.player.userId,
            username: opponent.player.username
        };
        players[socket.id] = {
            roomCode: code,
            userId: seeker.player.userId,
            username: seeker.player.username
        };

        io.sockets.sockets.get(opponent.socketId)?.join(code);
        socket.join(code);
        io.to(opponent.socketId).emit('room:joined', { player: seeker.player });
        io.to(socket.id).emit('room:joined', { player: opponent.player });
        io.to(code).emit('room:update', { room: rooms[code] });
        await startBattleInRoom(code);
    });
    socket.on('topic:vote', (data) => {
        const p = players[socket.id];
        if (!p) return;
        const room = rooms[p.roomCode];
        if (!room || room.status !== 'voting') return;

        room.votes[socket.id] = data.topic;
        const hostVoted = !!room.votes[room.host.socketId];
        const guestVoted = !!room.votes[room.guest.socketId];

        if (hostVoted && guestVoted) {
            let selectedTopic = room.votes[room.host.socketId];
            if (room.votes[room.host.socketId] !== room.votes[room.guest.socketId]) {
                selectedTopic = "Mixed"; // Or just pick one randomly if you prefer
            }
            
            let questions = [];
            if (selectedTopic === "Mixed") {
                const hostQs = mcqData[room.votes[room.host.socketId]] || [];
                const guestQs = mcqData[room.votes[room.guest.socketId]] || [];
                questions = [...hostQs.slice(0, 3), ...guestQs.slice(0, 2)];
            } else {
                questions = (mcqData[selectedTopic] || []).slice(0, 5);
            }

            room.status = 'in_progress';
            room.startTime = Date.now();
            io.to(p.roomCode).emit('topic:selected', { topic: selectedTopic });
            setTimeout(() => {
                io.to(p.roomCode).emit('battle:start', { mode: 'mcq', questions });
            }, 2000);
        }
    });

    socket.on('mcq:submit', (data) => {
        const p = players[socket.id];
        if (!p) return;
        const room = rooms[p.roomCode];
        if (!room || room.status !== 'in_progress') return;

        room.scores[socket.id] = data.score;
        const hostSubmitted = room.scores[room.host.socketId] !== undefined;
        const guestSubmitted = room.scores[room.guest.socketId] !== undefined;

        if (hostSubmitted && guestSubmitted) {
            const hostScore = room.scores[room.host.socketId];
            const guestScore = room.scores[room.guest.socketId];
            let winnerId = null;
            let loserId = null;

            if (hostScore > guestScore) {
                winnerId = room.host.socketId; loserId = room.guest.socketId;
            } else if (guestScore > hostScore) {
                winnerId = room.guest.socketId; loserId = room.host.socketId;
            } else {
                winnerId = room.host.socketId; loserId = room.guest.socketId; // tie breaker
            }

            endBattle(p.roomCode, winnerId, loserId, 'Score: ' + Math.max(hostScore, guestScore) + '/5');
        }
    });

    // ── Disconnect ───────────────────────────────────────────────────────────
    socket.on('disconnect', () => {
        removeFromQuickMatch(socket.id);
        const p = players[socket.id];
        if (p) {
            const room = rooms[p.roomCode];
            if (room && room.status !== 'finished') {
                const otherSocketId = room.host?.socketId === socket.id
                    ? room.guest?.socketId : room.host?.socketId;
                endBattle(p.roomCode, otherSocketId, socket.id, 'opponent_disconnected');
            }
            delete players[socket.id];
        }
        console.log(`[-] ${socket.id}`);
    });
});

// ── Start ─────────────────────────────────────────────────────────────────────
server.listen(PORT, '0.0.0.0', () => {
    console.log(`\n🥊 Dojo Battle Server  →  port ${PORT}`);
    console.log('🌐 Set arena.device.socketUrl to your deployed server URL\n');
});

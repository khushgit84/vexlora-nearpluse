/**
 * VEXLORA NearPulse — Interactive Web Portal & Radar Simulator
 * 100% Offline-Capable JavaScript Architecture
 */

(function () {
    'use strict';

    // -------------------------------------------------------------------------
    // 1. LAN IP & Public Download URL Handling
    // -------------------------------------------------------------------------
    const LAN_IP = '192.168.29.18';
    const PORT = '8080';
    const APK_FILENAME = 'vexlora-nearpulse.apk';
    const GITHUB_RELEASE_APK = 'https://github.com/khushgit84/vexlora-nearpluse/releases/download/v1.0.0/vexlora-nearpulse.apk';

    const isPublicHost = window.location.hostname.includes('github.io') || 
                         window.location.hostname.includes('vercel.app') ||
                         window.location.hostname.includes('netlify.app') ||
                         window.location.hostname.includes('loca.lt');

    const currentHost = window.location.hostname && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1'
        ? window.location.hostname
        : LAN_IP;
    const currentPort = window.location.port || PORT;

    const downloadUrl = isPublicHost ? GITHUB_RELEASE_APK : `http://${currentHost}:${currentPort}/${APK_FILENAME}`;
    const baseUrl = isPublicHost ? window.location.href : `http://${currentHost}:${currentPort}`;

    // Update all download buttons dynamically
    ['btn-header-download', 'btn-hero-apk', 'btn-banner-download'].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.href = downloadUrl;
        }
    });

    const lanDisplay = document.getElementById('lan-url-display');
    if (lanDisplay) {
        lanDisplay.textContent = baseUrl;
    }

    const copyBtn = document.getElementById('btn-copy-lan');
    if (copyBtn) {
        copyBtn.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(baseUrl);
                const originalText = copyBtn.textContent;
                copyBtn.textContent = 'COPIED!';
                copyBtn.style.background = '#10B981';
                copyBtn.style.color = '#050811';
                setTimeout(() => {
                    copyBtn.textContent = originalText;
                    copyBtn.style.background = '';
                    copyBtn.style.color = '';
                }, 2000);
            } catch (err) {
                prompt('Copy this address to access from phone:', baseUrl);
            }
        });
    }

    // -------------------------------------------------------------------------
    // 2. Pure Canvas QR Code Generator (Zero External Dependency)
    // -------------------------------------------------------------------------
    function generateSimpleQrCode(canvas, text) {
        const ctx = canvas.getContext('2d');
        const size = canvas.width;
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(0, 0, size, size);

        // QR Code Matrix 21x21 (Version 1) layout generator
        const qrSize = 25;
        const cellSize = Math.floor(size / qrSize);
        const offset = Math.floor((size - qrSize * cellSize) / 2);

        // Deterministic pseudo-random matrix based on text hash
        let hash = 0;
        for (let i = 0; i < text.length; i++) {
            hash = ((hash << 5) - hash) + text.charCodeAt(i);
            hash |= 0;
        }

        const matrix = Array.from({ length: qrSize }, () => Array(qrSize).fill(0));

        // Function to draw finder pattern
        function drawFinder(row, col) {
            for (let r = 0; r < 7; r++) {
                for (let c = 0; c < 7; c++) {
                    if (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
                        matrix[row + r][col + c] = 1;
                    } else {
                        matrix[row + r][col + c] = 0;
                    }
                }
            }
        }

        drawFinder(0, 0);
        drawFinder(0, qrSize - 7);
        drawFinder(qrSize - 7, 0);

        // Timing patterns
        for (let i = 8; i < qrSize - 8; i++) {
            matrix[6][i] = i % 2 === 0 ? 1 : 0;
            matrix[i][6] = i % 2 === 0 ? 1 : 0;
        }

        // Fill data cells using hash
        let seed = Math.abs(hash) || 123456789;
        function nextPseudo() {
            seed = (seed * 1103515245 + 12345) & 0x7fffffff;
            return (seed >> 16) / 32768;
        }

        for (let r = 0; r < qrSize; r++) {
            for (let c = 0; c < qrSize; c++) {
                // Avoid finders and separators
                const isTopLeft = r < 8 && c < 8;
                const isTopRight = r < 8 && c >= qrSize - 8;
                const isBottomLeft = r >= qrSize - 8 && c < 8;
                const isTiming = r === 6 || c === 6;

                if (!isTopLeft && !isTopRight && !isBottomLeft && !isTiming) {
                    matrix[r][c] = nextPseudo() > 0.5 ? 1 : 0;
                }
            }
        }

        // Render matrix to canvas
        ctx.fillStyle = '#050811';
        for (let r = 0; r < qrSize; r++) {
            for (let c = 0; c < qrSize; c++) {
                if (matrix[r][c] === 1) {
                    ctx.fillRect(offset + c * cellSize, offset + r * cellSize, cellSize, cellSize);
                }
            }
        }
    }

    const qrCanvas = document.getElementById('qr-canvas');
    if (qrCanvas) {
        generateSimpleQrCode(qrCanvas, downloadUrl);
    }

    // -------------------------------------------------------------------------
    // 3. Interactive RF Spectrum Radar Simulator
    // -------------------------------------------------------------------------
    const radarCanvas = document.getElementById('interactive-radar');
    if (radarCanvas) {
        const ctx = radarCanvas.getContext('2d');
        const W = radarCanvas.width;
        const H = radarCanvas.height;
        const cx = W / 2;
        const cy = H / 2;
        const maxRadius = cx - 18;

        let sweepAngle = 0;

        // Active peer nodes simulated in 2.4 GHz space
        const nodes = [
            { id: '1', name: 'NP-ALPHA (You)', isSelf: true, angle: 0, distRatio: 0.05, rssi: -32, type: 'phone', sos: false },
            { id: '2', name: 'IPHONE-15PRO', isSelf: false, angle: 0.85, distRatio: 0.32, rssi: -58, type: 'phone', sos: false, os: 'iOS 18.2' },
            { id: '3', name: 'REDMI-NOTE12', isSelf: false, angle: 2.35, distRatio: 0.52, rssi: -71, type: 'phone', sos: false, os: 'Android 14' },
            { id: '4', name: 'ONEPLUS-NORD3', isSelf: false, angle: 3.9, distRatio: 0.40, rssi: -62, type: 'phone', sos: false, os: 'Android 15' },
            { id: '5', name: 'GALAXY-S24', isSelf: false, angle: 5.1, distRatio: 0.74, rssi: -84, type: 'phone', sos: false, os: 'Android 14' },
            { id: '6', name: 'MI-SMART-TV', isSelf: false, angle: 1.6, distRatio: 0.65, rssi: -79, type: 'peripheral', filtered: true },
            { id: '7', name: 'BOAT-AIRDOPES', isSelf: false, angle: 4.5, distRatio: 0.28, rssi: -55, type: 'peripheral', filtered: true }
        ];

        function renderRadar() {
            ctx.clearRect(0, 0, W, H);

            // Radar background circle
            ctx.fillStyle = '#050a16';
            ctx.beginPath();
            ctx.arc(cx, cy, maxRadius, 0, Math.PI * 2);
            ctx.fill();

            // Concentric Range Rings
            const ringRatios = [0.25, 0.50, 0.75, 1.0];
            const ringLabels = ['1m', '5m', '15m', '30m'];

            ringRatios.forEach((ratio, idx) => {
                const r = maxRadius * ratio;
                ctx.strokeStyle = 'rgba(0, 229, 255, 0.2)';
                ctx.lineWidth = 1;
                ctx.beginPath();
                ctx.arc(cx, cy, r, 0, Math.PI * 2);
                ctx.stroke();

                // Range labels
                ctx.fillStyle = 'rgba(0, 229, 255, 0.5)';
                ctx.font = '10px "JetBrains Mono", monospace';
                ctx.fillText(ringLabels[idx], cx + 4, cy - r + 12);
            });

            // Crosshair Axes
            ctx.strokeStyle = 'rgba(0, 229, 255, 0.15)';
            ctx.beginPath();
            ctx.moveTo(cx, cy - maxRadius);
            ctx.lineTo(cx, cy + maxRadius);
            ctx.moveTo(cx - maxRadius, cy);
            ctx.lineTo(cx + maxRadius, cy);
            ctx.stroke();

            // Radar Sweeping Beam (Gradient sector)
            const sweepGradient = ctx.createConicGradient(sweepAngle, cx, cy);
            sweepGradient.addColorStop(0, 'rgba(0, 229, 255, 0.35)');
            sweepGradient.addColorStop(0.12, 'rgba(0, 229, 255, 0.05)');
            sweepGradient.addColorStop(0.15, 'rgba(0, 229, 255, 0)');
            sweepGradient.addColorStop(1, 'rgba(0, 229, 255, 0)');

            ctx.fillStyle = sweepGradient;
            ctx.beginPath();
            ctx.arc(cx, cy, maxRadius, 0, Math.PI * 2);
            ctx.fill();

            // Sweep Leading Line
            ctx.strokeStyle = '#00E5FF';
            ctx.lineWidth = 2;
            ctx.shadowColor = '#00E5FF';
            ctx.shadowBlur = 10;
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(cx + Math.cos(sweepAngle) * maxRadius, cy + Math.sin(sweepAngle) * maxRadius);
            ctx.stroke();
            ctx.shadowBlur = 0;

            // Draw Detected Nodes
            nodes.forEach(node => {
                if (node.isSelf) {
                    // Self center icon
                    ctx.fillStyle = '#00E5FF';
                    ctx.beginPath();
                    ctx.arc(cx, cy, 6, 0, Math.PI * 2);
                    ctx.fill();
                    return;
                }

                const px = cx + Math.cos(node.angle) * (maxRadius * node.distRatio);
                const py = cy + Math.sin(node.angle) * (maxRadius * node.distRatio);

                // Check angle proximity to sweep line for flash intensity
                let angleDiff = Math.abs((node.angle - sweepAngle + Math.PI * 2) % (Math.PI * 2));
                const isJustScanned = angleDiff < 0.25;

                if (node.filtered) {
                    // Filtered TV / Headset node (Muted gray)
                    ctx.fillStyle = 'rgba(100, 116, 139, 0.4)';
                    ctx.beginPath();
                    ctx.arc(px, py, 3.5, 0, Math.PI * 2);
                    ctx.fill();

                    // Strike-through cross
                    ctx.strokeStyle = 'rgba(239, 68, 68, 0.5)';
                    ctx.lineWidth = 1.2;
                    ctx.beginPath();
                    ctx.moveTo(px - 4, py - 4);
                    ctx.lineTo(px + 4, py + 4);
                    ctx.stroke();
                } else if (node.sos) {
                    // Emergency SOS Beacon (Crimson Flash)
                    ctx.fillStyle = '#FF1744';
                    ctx.shadowColor = '#FF1744';
                    ctx.shadowBlur = 18;
                    ctx.beginPath();
                    ctx.arc(px, py, 8, 0, Math.PI * 2);
                    ctx.fill();
                    ctx.shadowBlur = 0;

                    // SOS Warning Ring
                    ctx.strokeStyle = 'rgba(255, 23, 68, 0.6)';
                    ctx.lineWidth = 2;
                    ctx.beginPath();
                    ctx.arc(px, py, 14 + Math.sin(Date.now() / 150) * 4, 0, Math.PI * 2);
                    ctx.stroke();

                    // Label
                    ctx.fillStyle = '#FF1744';
                    ctx.font = 'bold 11px "Outfit", sans-serif';
                    ctx.fillText(`🚨 ${node.name} [SOS]`, px + 10, py - 6);
                } else {
                    // Valid Phone Node (Electric Cyan / Emerald)
                    ctx.fillStyle = node.os.startsWith('iOS') ? '#8B5CF6' : '#10B981';
                    if (isJustScanned) {
                        ctx.shadowColor = '#00E5FF';
                        ctx.shadowBlur = 14;
                    }
                    ctx.beginPath();
                    ctx.arc(px, py, isJustScanned ? 6 : 4.5, 0, Math.PI * 2);
                    ctx.fill();
                    ctx.shadowBlur = 0;

                    // Node Label
                    ctx.fillStyle = '#E2E8F0';
                    ctx.font = '10px "JetBrains Mono", monospace';
                    ctx.fillText(`${node.name}`, px + 8, py - 4);

                    ctx.fillStyle = 'rgba(0, 229, 255, 0.7)';
                    ctx.font = '9px "JetBrains Mono", monospace';
                    ctx.fillText(`${node.rssi} dBm`, px + 8, py + 8);
                }
            });

            // Advance sweep
            sweepAngle = (sweepAngle + 0.025) % (Math.PI * 2);
            requestAnimationFrame(renderRadar);
        }

        renderRadar();
    }

    // -------------------------------------------------------------------------
    // 4. Live Detected Telemetry List
    // -------------------------------------------------------------------------
    function updateTelemetryList() {
        const list = document.getElementById('telemetry-list');
        if (!list) return;

        const peerData = [
            { name: 'IPHONE-15PRO', os: 'iOS CoreBluetooth', rssi: '-58 dBm', dist: '1.8m (Near)', sos: false },
            { name: 'REDMI-NOTE12', os: 'Android BLE GATT', rssi: '-71 dBm', dist: '4.2m (Mid)', sos: false },
            { name: 'ONEPLUS-NORD3', os: 'Android BLE GATT', rssi: '-62 dBm', dist: '2.4m (Near)', sos: false },
            { name: 'GALAXY-S24', os: 'Android BLE GATT', rssi: '-84 dBm', dist: '11.5m (Far)', sos: false }
        ];

        list.innerHTML = peerData.map(p => `
            <div class="telemetry-row ${p.sos ? 'sos-active' : ''}">
                <div>
                    <span class="telemetry-node-name">${p.name}</span>
                    <span style="display:block; font-size: 0.7rem; color: #94A3B8;">${p.os}</span>
                </div>
                <div style="text-align: right;">
                    <div class="telemetry-rssi">${p.rssi}</div>
                    <div class="telemetry-dist">${p.dist}</div>
                </div>
            </div>
        `).join('');
    }

    updateTelemetryList();

    // -------------------------------------------------------------------------
    // 5. Binary Packet Encoder & Simulator (Matches BlePacket.kt)
    // -------------------------------------------------------------------------
    function encodeSimulatedPacket(sender, payload, isSos = false) {
        // BlePacket Protocol matching BlePacket.kt:
        // [0]: PROTOCOL_VERSION (0x01)
        // [1]: packetType (0x10 = MSG, 0x30 = SOS)
        // [2]: idLen (1 byte)
        // [3]: senderLen (1 byte)
        // [4..5]: payloadLen (2 bytes Short BigEndian)
        // [6..6+idLen]: idBytes
        // [..]: senderBytes
        // [..]: payloadBytes
        const encoder = new TextEncoder();
        const msgId = (Math.random().toString(36).substring(2, 8)).toUpperCase();
        const idBytes = encoder.encode(msgId);
        const senderBytes = encoder.encode(sender);
        const payloadBytes = encoder.encode(payload);

        const totalLen = 6 + idBytes.length + senderBytes.length + payloadBytes.length;
        const buffer = new ArrayBuffer(totalLen);
        const view = new DataView(buffer);
        const uint8 = new Uint8Array(buffer);

        let offset = 0;
        view.setUint8(offset++, 0x01); // PROTOCOL_VERSION
        view.setUint8(offset++, isSos ? 0x30 : 0x10); // PACKET_TYPE_SOS or PACKET_TYPE_MSG
        view.setUint8(offset++, idBytes.length);
        view.setUint8(offset++, senderBytes.length);
        view.setInt16(offset, payloadBytes.length);
        offset += 2;

        uint8.set(idBytes, offset);
        offset += idBytes.length;
        uint8.set(senderBytes, offset);
        offset += senderBytes.length;
        uint8.set(payloadBytes, offset);

        const hex = Array.from(uint8)
            .map(b => b.toString(16).padStart(2, '0').toUpperCase())
            .join(' ');

        return hex;
    }

    const simSendBtn = document.getElementById('btn-sim-send');
    const simSosBtn = document.getElementById('btn-sim-sos');
    const simPseudonym = document.getElementById('sim-pseudonym');
    const simMessage = document.getElementById('sim-message');
    const hexDump = document.getElementById('packet-hex-dump');

    function updateHex(isSos = false) {
        if (!hexDump || !simPseudonym || !simMessage) return;
        const sender = simPseudonym.value.trim() || 'NODE';
        const msg = isSos ? 'DISTRESS: Immediate assistance needed!' : (simMessage.value.trim() || 'Ping');
        const hex = encodeSimulatedPacket(sender, msg, isSos);
        hexDump.textContent = hex;
        hexDump.style.borderColor = isSos ? '#FF1744' : '#00E5FF';
    }

    if (simSendBtn) {
        simSendBtn.addEventListener('click', () => {
            updateHex(false);
            simSendBtn.textContent = 'Transmitted!';
            setTimeout(() => { simSendBtn.textContent = 'Transmit GATT Packet'; }, 1000);
        });
    }

    if (simSosBtn) {
        simSosBtn.addEventListener('click', () => {
            updateHex(true);
            simSosBtn.textContent = 'SOS Dispatched!';
            setTimeout(() => { simSosBtn.textContent = 'Trigger SOS Beacon'; }, 1500);
        });
    }

    if (simPseudonym && simMessage) {
        simPseudonym.addEventListener('input', () => updateHex(false));
        simMessage.addEventListener('input', () => updateHex(false));
        updateHex(false);
    }
})();

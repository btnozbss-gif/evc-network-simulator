// ==========================================
// SYSTEM VARIABLES
// ==========================================
let makine;
const istasyonlar = [];
let aktifModalIstasyonu = null;
let aktifGrafik = null;
let otomatikBaglanmaAktif = true;
let reconnectDelay = 2000;
const maxReconnectDelay = 30000;
let reconnectTimer = null;
const terminal = document.getElementById("terminal");
const grid = document.getElementById("grid-container");

// ==========================================
// SESSION AND LOGIN PROCEDURES
// ==========================================
window.onload = function () {
    if (sessionStorage.getItem("oturum_gecerli") === "evet") {
        document.getElementById("login-ekrani").style.display = "none";
        sistemiHazirla();
        sunucuyaBaglan();
    }
};

document.getElementById("sifre").addEventListener("keypress", function (event) {
    if (event.key === "Enter") girisYap();
});

function girisYap() {
    const kulAdi = document.getElementById("kullanici-adi").value;
    const sfr = document.getElementById("sifre").value;
    if (kulAdi === "admin" && sfr === "vestel123") {
        document.getElementById("login-ekrani").style.display = "none";
        sessionStorage.setItem("oturum_gecerli", "evet");
        logYaz("Sistem yöneticisi (admin) başarıyla doğrulandı.", "#00E676");
        sistemiHazirla();
    } else {
        document.getElementById("login-hata").style.display = "block";
    }
}

function cikisYap() {
    sessionStorage.removeItem("oturum_gecerli");
    document.getElementById("login-ekrani").style.display = "flex";
    document.getElementById("sifre").value = "";
    document.getElementById("login-hata").style.display = "none";

    otomatikBaglanmaAktif = false;
    if (reconnectTimer) clearTimeout(reconnectTimer);

    if (makine && makine.readyState === WebSocket.OPEN) {
        makine.close();
    }
    logYaz("Sistemden çıkış yapıldı. Oturum kilitlendi.", "#FF9800");
}
// ==========================================
// TRIGGER MESSAGE FUNCTION
// ==========================================
function tetikle(mesajTipi) {
    if (!makine || makine.readyState !== WebSocket.OPEN) {
        toastGoster("Sunucuya bağlı değilsiniz!", "#FF1744", "🚨");
        return;
    }
    if (!aktifModalIstasyonu) return;

    const paket = {
        action: "RemoteCommand",
        stationId: aktifModalIstasyonu,
        command: "TriggerMessage",
        requestedMessage: mesajTipi
    };

    makine.send(JSON.stringify(paket));
    logYaz(`[KOMUT] Merkezden zorla '${mesajTipi}' paketi istendi.`, "#00E5FF", aktifModalIstasyonu);
    toastGoster(`Cihazdan ${mesajTipi} verisi talep edildi.`, "#00E5FF", "📡");
}
// ==========================================
// BILLING ENGINE - HOLOGRAM
// ==========================================
function faturaGoster(stationId, txId, kwh) {
    const birimFiyat = 9.50;
    const baglantiUcreti = 15.00;

    const enerjiTutari = parseFloat(kwh) * birimFiyat;
    const toplamTutar = (enerjiTutari + baglantiUcreti).toFixed(2);

    const faturaDiv = document.createElement("div");
    faturaDiv.className = "fatura-makbuz-holo";
    faturaDiv.innerHTML = `
        <div class="laser-cizgi"></div>
        <div class="fatura-baslik">⚡ HOLOGRAM_RECEIPT ⚡</div>
        <div class="fatura-detay"><span>TARGET_STATION:</span> <span class="vurgu">${stationId}</span></div>
        <div class="fatura-detay"><span>TRANSACTION_ID:</span> <span>${txId || 'SYS_ERR_00'}</span></div>
        <div class="fatura-detay"><span>ENERGY_CONSUMED:</span> <span class="vurgu">${kwh} kWh</span></div>
        <div class="fatura-detay"><span>RATE:</span> <span>${birimFiyat.toFixed(2)} ₺/kWh</span></div>
        <div class="fatura-detay"><span>CONNECT_FEE:</span> <span>${baglantiUcreti.toFixed(2)} ₺</span></div>
        <hr class="siber-ayrac">
        <div class="fatura-toplam"><span>TOTAL_DUE:</span> <span class="tutar-parlak">${toplamTutar} ₺</span></div>
        <button class="fatura-btn-holo" onclick="this.parentElement.remove()">[ ONAYLA VE BAĞLANTIYI KES ]</button>
    `;

    document.body.appendChild(faturaDiv);

    toastGoster("Şarj tamamlandı. Holografik veri transferi başarılı.", "#00E5FF", "💠");
}

// ==========================================
// CSMS INITIALIZATION AND INTERFACE RESET
// ==========================================
function sistemiHazirla() {
    grid.innerHTML = "";
    istasyonlar.length = 0;
    istatistikleriHesapla();
    logYaz("CSMS Paneli hazır. Fiziksel cihazlardan bağlantı bekleniyor...", "#00E5FF");

    document.getElementById("btn-baglan").disabled = false;
}

// ==========================================
// DYNAMIC TYPEWRITER (CSS-BASED) LOG ENGINE
// ==========================================
function logYaz(mesaj, renk = "#aaa", istasyonId = null) {
    const saat = new Date().toLocaleTimeString();

    const hataMi = mesaj.includes("FAULTED") || mesaj.includes("koptu") || mesaj.includes("kapatıldı") || mesaj.includes("Hata");
    const ekstraSinif = hataMi ? "glitch-efekti" : "";

    const satirDiv = document.createElement("div");
    satirDiv.className = "log-satiri";
    satirDiv.innerHTML = `<span class="log-saat">[${saat}]</span> <span class="log-metin ${ekstraSinif} css-daktilo" style="color: ${renk};">${mesaj}</span>`;

    terminal.appendChild(satirDiv);
    terminal.scrollTop = terminal.scrollHeight;

    if (terminal.childElementCount > 150) {
        terminal.removeChild(terminal.firstChild);
    }

    if (istasyonId) {
        const cihaz = istasyonlar.find(c => c.id === istasyonId);
        if (cihaz) {
            const statikLog = `<div class="log-satiri"><span class="log-saat">[${saat}]</span> <span class="${ekstraSinif}" style="color: ${renk};">${mesaj}</span></div>`;
            cihaz.logs.push(statikLog);

            if (aktifModalIstasyonu === istasyonId) {
                const mTerminal = document.getElementById("modal-terminal");
                const mSatirDiv = satirDiv.cloneNode(true);

                mTerminal.appendChild(mSatirDiv);
                mTerminal.scrollTop = mTerminal.scrollHeight;
            }
        }
    }
}
// ==========================================
// WEBSOCKET BRIDGE (COMMUNICATION WITH JAVA)
// ==========================================
function sunucuyaBaglan() {
    otomatikBaglanmaAktif = true;
    if (reconnectTimer) clearTimeout(reconnectTimer);
    logYaz("UI Sunucusuna (Port 8888) bağlanılıyor...", "#FF9800");
    makine = new WebSocket("ws://localhost:8888");

    const btn = document.getElementById("btn-baglan");
    btn.disabled = true;
    btn.innerText = "🔌 Bağlanıyor...";

    makine.onopen = function () {
        logYaz("Bağlantı Başarılı! Java sunucusu dinleniyor.", "#00E676");
        btn.innerText = "BAĞLI (PORT: 8888)";
        btn.style.backgroundColor = "#00E676";
        toastGoster("Java Sunucusuna Bağlanıldı", "#00E676", "🚀");

        reconnectDelay = 2000;
    };

    makine.onmessage = function (event) {
        try {
            const data = JSON.parse(event.data);
            const action = data.action;
            const stationId = data.stationId;

            if (stationId) dataAkisiTetikle(stationId);

            switch (action) {
                case "BootNotification":
                    cihazKutusuOlustur(stationId);
                    logYaz(`[BOOT] ${stationId} uyandı. Model: ${data.chargePointModel}`, "#00E676", stationId);
                    break;

                case "StatusNotification":
                    const hedefCihaz = istasyonlar.find(c => c.id === stationId);

                    if (hedefCihaz && hedefCihaz.transactionId !== null && data.status.toUpperCase() === "AVAILABLE") {
                        logYaz(`[SYS_IGNORE] ${stationId} Kasa (Connector 0) Available bildirdi. Şarj ezilmedi.`, "#888", stationId);
                        break;
                    }

                    arayuzuGuncelle(stationId, data.status);
                    logYaz(`[STATUS] ${stationId} -> ${data.status} (${data.errorCode})`, "#FFEA00", stationId);
                    break;

                case "StartTransaction":
                    const baslayanCihaz = istasyonlar.find(c => c.id === stationId);
                    if (baslayanCihaz) {
                        baslayanCihaz.transactionId = data.transactionId;
                        baslayanCihaz.harcananKwh = "0.00";
                    }
                    logYaz(`[ŞARJ BAŞLADI] ${stationId} - İşlem ID: ${data.transactionId}`, "#00E5FF", stationId);
                    break;

                case "StopTransaction":
                    const duranCihaz = istasyonlar.find(c => c.id === stationId);
                    let hesaplananKwh = (data.meterStop / 1000).toFixed(2);

                    if (duranCihaz) {
                        duranCihaz.transactionId = null;
                        duranCihaz.harcananKwh = hesaplananKwh;
                    }
                    logYaz(`[ŞARJ BİTTİ] ${stationId} - Toplam Enerji: ${data.meterStop} Wh`, "#D500F9", stationId);

                    faturaGoster(stationId, data.transactionId, hesaplananKwh);

                    if (aktifModalIstasyonu === stationId) modalGorselleriniGuncelle();
                    break;

                case "MeterValues":
                    const cihaz = istasyonlar.find(c => c.id === stationId);
                    if (cihaz) {
                        if (!cihaz.transactionId && data.transactionId) {
                            cihaz.transactionId = data.transactionId;
                            logYaz(`[SİSTEM] Kurtarılan İşlem ID'si: ${data.transactionId} hafızaya alındı.`, "#FF9800", stationId);
                        }

                        const kwDeger = (parseFloat(data.meterValue) / 1000).toFixed(1);
                        cihaz.anlikKw = kwDeger;

                        cihaz.harcananKwh = (parseFloat(cihaz.harcananKwh || 0) + (kwDeger * 0.01)).toFixed(2);

                        cihaz.meterCount = (cihaz.meterCount || 0) + 1;
                        if (cihaz.meterCount % 5 === 0) {
                            logYaz(`[ENERJİ AKIŞI] ${stationId}: ${cihaz.anlikKw} kW güç çekiliyor.`, "#00E5FF", stationId);
                        }

                        if (aktifModalIstasyonu === stationId) {
                            grafigeVeriEkle(cihaz.anlikKw);
                            modalGorselleriniGuncelle();
                        }
                    }
                    break;

                case "UpdateCardList":
                    kartListesiniCiz(data.cards);
                    break;

                case "Heartbeat":
                    logYaz(`[HEARTBEAT] ${stationId} yaşıyor.`, "#607D8B", stationId);
                    break;

                case "StationDisconnected": {
                    const silinecekKutu = document.getElementById("kutu-" + stationId);
                    if (silinecekKutu && silinecekKutu.parentNode) {
                        silinecekKutu.parentNode.removeChild(silinecekKutu);
                    }

                    const sIndex = istasyonlar.findIndex(c => c.id === stationId);
                    if (sIndex > -1) {
                        istasyonlar.splice(sIndex, 1);
                    }

                    istatistikleriHesapla();

                    if (aktifModalIstasyonu === stationId) {
                        modalKapat();
                        toastGoster("İstasyon bağlantısı koptuğu için panel kapatıldı.", "#FF1744", "🔌");
                    }

                    logYaz(`[SİSTEM] ${stationId} ağdan koptu ve arayüzden kaldırıldı.`, "#FF1744");
                    break;
                }
            }
        } catch (e) {
            console.log("Mesaj islenemedi: ", e);
        }
    };

    makine.onclose = function () {
        logYaz("Java sunucusu ile bağlantı koptu veya kapatıldı.", "#FF1744");

        const btn = document.getElementById("btn-baglan");
        btn.disabled = false;
        btn.innerText = "🔌 Sunucuya Bağlan";
        btn.style.backgroundColor = "#005A9E";

        document.getElementById("grid-container").innerHTML = "";
        istasyonlar.length = 0;
        istatistikleriHesapla();

        if (aktifModalIstasyonu) {
            modalKapat();
        }

        toastGoster("Sunucu Bağlantısı Kesildi! İstasyonlar ağdan düşürüldü.", "#FF1744", "🚨");

        if (otomatikBaglanmaAktif) {
            logYaz(`Sistem ${reconnectDelay / 1000} saniye sonra tekrar bağlanmayı deneyecek...`, "#FF9800");
            btn.innerText = `🔌 Yeniden Bağlanıyor (${reconnectDelay / 1000}s)...`;
            btn.disabled = true;
            btn.style.backgroundColor = "#FF9800";

            reconnectTimer = setTimeout(() => {
                sunucuyaBaglan();
            }, reconnectDelay);

            reconnectDelay = Math.min(reconnectDelay * 2, maxReconnectDelay);
        }
    };
}
// ==========================================
// CARD MANAGEMENT COMMANDS (SENT TO JAVA)
// ==========================================
function kartEkleKomutuGonder(yeniKartId) {
    if (!makine || makine.readyState !== WebSocket.OPEN) {
        alert("Hata: Sunucuya bağlı değilsiniz!");
        return;
    }
    const paket = { action: "AddCard", idTag: yeniKartId };
    makine.send(JSON.stringify(paket));
    logYaz(`[KOMUT] Java'ya kart ekleme isteği gönderildi: ${yeniKartId}`, "#ccc");
}

function kartSilKomutuGonder(silinecekKartId) {
    if (!makine || makine.readyState !== WebSocket.OPEN) return;
    const paket = { action: "RemoveCard", idTag: silinecekKartId };
    makine.send(JSON.stringify(paket));
    logYaz(`[KOMUT] Java'ya kart silme isteği gönderildi: ${silinecekKartId}`, "#ccc");
}
// ==========================================
// REMOTE OPERATIONS MANAGEMENT
// ==========================================
function uzaktanKomutGonder(komutAdi) {
    if (!makine || makine.readyState !== WebSocket.OPEN) {
        alert("Hata: Sunucuya bağlı değilsiniz!");
        return;
    }
    if (!aktifModalIstasyonu) return;

    let idTagGonderilecek = null;

    if (komutAdi === "RemoteStartTransaction") {
        idTagGonderilecek = aktifUzaktanKart;
    } else {
        const onay = confirm(`DİKKAT: ${aktifModalIstasyonu} cihazına '${komutAdi}' komutu gönderilecek. Onaylıyor musunuz?`);
        if (!onay) return;
    }

    const cihaz = istasyonlar.find(c => c.id === aktifModalIstasyonu);

    const paket = {
        action: "RemoteCommand",
        stationId: aktifModalIstasyonu,
        command: komutAdi,
        transactionId: cihaz ? cihaz.transactionId : null,
        idTag: idTagGonderilecek
    };

    makine.send(JSON.stringify(paket));
    logYaz(`[KOMUT] Merkezden cihaza ${komutAdi} isteği gönderildi. (Kart: ${idTagGonderilecek || "Yok"})`, "#FF9800", aktifModalIstasyonu);
}
function sayfaDegistir(sayfaId, tiklananButon) {
    document.querySelectorAll('.sayfa').forEach(sayfa => {
        sayfa.classList.remove('aktif');
    });
    document.getElementById('sayfa-' + sayfaId).classList.add('aktif');

    document.querySelectorAll('.menu-item').forEach(buton => {
        buton.classList.remove('aktif');
    });
    tiklananButon.classList.add('aktif');
}
function toastGoster(mesaj, renkKodu = "#00E676", ikon = "🔌") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = "toast-mesaj";
    toast.style.borderLeftColor = renkKodu;
    toast.innerHTML = `<span>${ikon}</span> <span>${mesaj}</span>`;

    container.appendChild(toast);

    setTimeout(() => {
        if (toast.parentNode) toast.remove();
    }, 4000);
}

// ==========================================
// SCREEN DRAWING AND UPDATE FUNCTIONS
// ==========================================
function cihazKutusuOlustur(id) {
    if (istasyonlar.find(c => c.id === id)) return;

    istasyonlar.push({
        id: id,
        status: "AVAILABLE",
        transactionId: null,
        sarjYuzdesi: 0,
        anlikKw: 0.0,
        harcananKwh: 0.00,
        logs: []
    });

    let kutu = document.createElement("div");
    kutu.className = "cihaz-kutu st-available";
    kutu.id = "kutu-" + id;
    kutu.onclick = () => modalAc(id);
    kutu.innerHTML = `<div class="cihaz-id">${id}</div><div class="cihaz-durum" id="durum-${id}">AVAILABLE</div>`;
    grid.appendChild(kutu);

    istatistikleriHesapla();
}

function arayuzuGuncelle(id, durum) {
    const cihaz = istasyonlar.find(c => c.id === id);
    if (!cihaz) return;

    cihaz.status = durum.toUpperCase();

    let cssDurumu = durum.toLowerCase();
    if (cssDurumu === "suspendedev" || cssDurumu === "suspendedevse") {
        cssDurumu = "charging";
    }

    const kutu = document.getElementById("kutu-" + id);
    const durumMetni = document.getElementById("durum-" + id);

    kutu.className = "cihaz-kutu st-" + cssDurumu;
    durumMetni.innerText = cihaz.status;

    istatistikleriHesapla();
    if (aktifModalIstasyonu === id) modalGorselleriniGuncelle();
}

function istatistikleriHesapla() {
    const toplam = istasyonlar.length;
    document.getElementById("stat-toplam").innerText = toplam;

    if (toplam === 0) return;

    const arr = istasyonlar.map(c => c.status);
    const counts = {
        Available: arr.filter(s => s === "AVAILABLE").length,
        Preparing: arr.filter(s => s === "PREPARING").length,
        Charging: arr.filter(s => s === "CHARGING" || s === "SUSPENDEDEV" || s === "SUSPENDEDEVSE").length,
        Finishing: arr.filter(s => s === "FINISHING").length,
        Faulted: arr.filter(s => s === "FAULTED").length,
        Unavailable: arr.filter(s => s === "UNAVAILABLE").length
    };

    for (const [durum, sayi] of Object.entries(counts)) {
        document.getElementById(`stat-${durum.toLowerCase()}`).innerText = sayi;
        document.getElementById(`bar-${durum.toLowerCase()}`).style.width = `${(sayi / toplam) * 100}%`;
    }
}
function grafikBaslatVeyaSifirla() {
    const ctx = document.getElementById('sarj-grafigi').getContext('2d');
    if (aktifGrafik) { aktifGrafik.destroy(); }

    aktifGrafik = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Anlık Güç (kW)',
                data: [],
                borderColor: '#00E5FF',
                backgroundColor: 'rgba(0, 229, 255, 0.1)',
                borderWidth: 2,
                pointRadius: 0,
                pointHitRadius: 10,
                tension: 0.4,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#888', font: { size: 10, family: 'monospace' } }
                },
                y: {
                    grid: { color: 'rgba(255,255,255,0.05)' },
                    ticks: { color: '#888', font: { size: 10, family: 'monospace' } },
                    min: 0,
                    max: 25
                }
            },
            animation: { duration: 400 }
        }
    });
}

function grafigeVeriEkle(kwDegeri) {
    if (!aktifGrafik) return;
    const simdi = new Date();
    const saat = simdi.toLocaleTimeString('tr-TR', { hour12: false });

    aktifGrafik.data.labels.push(saat);
    aktifGrafik.data.datasets[0].data.push(kwDegeri);

    if (aktifGrafik.data.labels.length > 20) {
        aktifGrafik.data.labels.shift();
        aktifGrafik.data.datasets[0].data.shift();
    }
    aktifGrafik.update();
}

// ==========================================
// MODAL (VICTRQN DETAIL SCREEN) MANAGEMENT
// ==========================================
function modalAc(id) {
    grafikBaslatVeyaSifirla();
    aktifModalIstasyonu = id;
    document.getElementById("modal-istasyon-isim").innerHTML = `<span style="color:#888; font-size:14px; margin-right:5px;">STATION:</span> ${id}`;

    document.getElementById("istasyon-detay-panel").classList.add("acik");

    modalGorselleriniGuncelle();

    const cihaz = istasyonlar.find(c => c.id === id);
    const mTerminal = document.getElementById("modal-terminal");
    mTerminal.innerHTML = cihaz.logs.join("") || "> Log kaydı bulunmuyor.<br>";
    mTerminal.scrollTop = mTerminal.scrollHeight;
}

function modalKapat() {
    document.getElementById("istasyon-detay-panel").classList.remove("acik");
    aktifModalIstasyonu = null;
}

function modalGorselleriniGuncelle() {
    if (!aktifModalIstasyonu) return;
    const cihaz = istasyonlar.find(c => c.id === aktifModalIstasyonu);
    if (!cihaz) return;

    const kwGosterge = document.getElementById("modal-kw-deger");
    const kwhGosterge = document.getElementById("modal-kwh-deger");
    const donut = document.getElementById("modal-donut");
    const topology = document.getElementById("sim-topology");
    const panel = document.getElementById("istasyon-detay-panel");

    const renkHaritasi = {
        "AVAILABLE": "var(--available)",
        "PREPARING": "var(--preparing)",
        "CHARGING": "var(--charging)",
        "SUSPENDEDEV": "var(--charging)",
        "SUSPENDEDEVSE": "var(--charging)",
        "FINISHING": "var(--finishing)",
        "FAULTED": "var(--faulted)",
        "UNAVAILABLE": "var(--unavailable)"
    };

    const aktifRenk = renkHaritasi[cihaz.status] || "var(--border-renk)";
    panel.style.borderLeftColor = aktifRenk;
    panel.style.boxShadow = `-10px 0 40px ${aktifRenk}33`;

    if (cihaz.status === "CHARGING" || cihaz.status === "SUSPENDEDEV" || cihaz.status === "SUSPENDEDEVSE") {
        kwGosterge.innerText = cihaz.anlikKw || "0.0";
        kwGosterge.style.color = "#00E5FF";
        kwhGosterge.innerHTML = `${cihaz.harcananKwh || "0.00"} <span>kWh</span>`;

        const gucOrani = Math.min((parseFloat(cihaz.anlikKw) / 22) * 100, 100) || 0;
        donut.style.background = `conic-gradient(var(--charging) ${gucOrani}%, #1A212D ${gucOrani}%)`;
        topology.classList.add("sarjda");

    } else if (cihaz.status === "FINISHING") {
        kwGosterge.innerText = "0.0";
        kwGosterge.style.color = "#FFF";
        kwhGosterge.innerHTML = `${cihaz.harcananKwh || "0.00"} <span>kWh</span>`;

        donut.style.background = `conic-gradient(var(--finishing) 100%, #1A212D 100%)`;
        topology.classList.remove("sarjda");

    } else {
        kwGosterge.innerText = "0.0";
        kwGosterge.style.color = "#555";
        kwhGosterge.innerHTML = `0.00 <span>kWh</span>`;

        donut.style.background = `conic-gradient(#1A212D 100%, transparent 0)`;
        topology.classList.remove("sarjda");
    }
}
// ==========================================
// CARD MANAGEMENT USER INTERFACE (UI) DRAWING FUNCTIONS
// ==========================================

let aktifUzaktanKart = "ADMIN_REMOTE";

function kartEkleArayuz() {
    const input = document.getElementById("yeni-kart-input");
    const kartId = input.value.trim().toUpperCase();

    if (kartId !== "") {
        const paket = { action: "AddCard", idTag: kartId };
        makine.send(JSON.stringify(paket));

        aktifUzaktanKart = kartId;

        logYaz(`[UI COMMAND] Added Card: ${kartId}`);
        input.value = "";
        alert(`Kart başarıyla eklendi! Artık uzaktan şarj başlatıldığında otomatik olarak [${kartId}] kullanılacak.`);
    } else {
        alert("Lütfen geçerli bir Kart ID girin.");
    }
}

function kartListesiniCiz(kartlar) {
    const container = document.getElementById("kart-listesi-container");
    container.innerHTML = "";

    if (!kartlar || kartlar.length === 0) {
        container.innerHTML = "<div class='bos-liste-uyari'>Sistemde kayıtlı yetkili kart yok.</div>";
        return;
    }

    kartlar.forEach(kartId => {
        const satir = document.createElement("div");
        satir.className = "kart-satir";

        satir.innerHTML = `
            <span class="kart-id-metin">${kartId}</span>
            <button class="kart-sil-btn" onclick="kartSilKomutuGonder('${kartId}')">SİL</button>
        `;

        container.appendChild(satir);
    });
}
function dataAkisiTetikle(id) {
    if (aktifModalIstasyonu === id) {
        const topology = document.getElementById("sim-topology");
        topology.classList.add("data-aktif");
        setTimeout(() => {
            topology.classList.remove("data-aktif");
        }, 800);
    }
}
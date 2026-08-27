package com.myt.backend.routes

import com.myt.backend.state.DashboardStateStore
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** M41 — read-only web dashboard stub (Phase 3). */
fun Route.dashboardRoutes() {
    route("/dash") {
        get {
            val s = DashboardStateStore.state
            call.respondText(
                """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>MyT Dashboard</title>
                  <style>
                    body { font-family: system-ui, sans-serif; background:#0a1018; color:#e8e8ed; margin:0; padding:24px; }
                    .card { background:#121820; border:1px solid #2a3544; border-radius:12px; padding:16px; margin-bottom:12px; }
                    h1 { font-size:1.4rem; }
                    .muted { color:#9a9aa3; font-size:0.9rem; }
                    .grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(140px,1fr)); gap:12px; }
                    .value { font-size:1.6rem; font-weight:700; margin-top:6px; }
                  </style>
                </head>
                <body>
                  <h1>MyT Web Dashboard (M41)</h1>
                  <p class="muted">${s.displayName} · ${s.vin} — Phase 2 auth + live telemetry 연동 전 데모 상태입니다.</p>
                  <div class="grid">
                    <div class="card"><div class="muted">SOC</div><div class="value" id="soc">${s.soc} %</div></div>
                    <div class="card"><div class="muted">Speed</div><div class="value" id="speed">${s.speedKmh} km/h</div></div>
                    <div class="card"><div class="muted">Range</div><div class="value" id="range">${s.rangeKm} km</div></div>
                    <div class="card"><div class="muted">Lock</div><div class="value" id="lock">${if (s.locked) "LOCKED" else "UNLOCKED"}</div></div>
                  </div>
                  <script>
                    async function refresh() {
                      const r = await fetch('/api/v1/dashboard/state');
                      const d = await r.json();
                      document.getElementById('soc').textContent = (d.soc ?? '—') + ' %';
                      document.getElementById('speed').textContent = (d.speedKmh ?? '—') + ' km/h';
                      document.getElementById('range').textContent = (d.rangeKm ?? '—') + ' km';
                      document.getElementById('lock').textContent = d.locked ? 'LOCKED' : 'UNLOCKED';
                    }
                    setInterval(refresh, 3000);
                  </script>
                </body>
                </html>
                """.trimIndent(),
                contentType = io.ktor.http.ContentType.Text.Html,
            )
        }
    }
    get("/api/v1/dashboard/state") {
        call.respond(DashboardStateStore.state)
    }
}

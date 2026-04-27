// LoanHub Main JS (clean)

(function () {
  // Mobile nav toggle
  const hamburger = document.getElementById("hamburger");
  const navLinks = document.getElementById("navLinks");

  if (hamburger && navLinks) {
    hamburger.addEventListener("click", () => {
      navLinks.classList.toggle("show");
    });
  }

  // Login dropdown
  const dropdown = document.querySelector(".dropdown");
  const loginBtn = document.getElementById("loginBtn");

  if (dropdown && loginBtn) {
    loginBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      dropdown.classList.toggle("open");
    });

    document.addEventListener("click", () => {
      dropdown.classList.remove("open");
    });
  }

  // Quick Tools tabs (EMI / Track)
  const toolTabs = document.querySelectorAll(".tool-tab");
  const toolPanes = document.querySelectorAll(".tool-pane");

  if (toolTabs.length && toolPanes.length) {
    toolTabs.forEach((btn) => {
      btn.addEventListener("click", () => {
        const target = btn.dataset.tab;

        toolTabs.forEach((b) => b.classList.remove("active"));
        toolPanes.forEach((p) => p.classList.remove("active"));

        btn.classList.add("active");
        document.getElementById(target)?.classList.add("active");
      });
    });
  }

  // FAQ accordion
  document.querySelectorAll("[data-accordion]").forEach((wrap) => {
    wrap.querySelectorAll(".faq-item .faq-q").forEach((q) => {
      q.addEventListener("click", () => {
        const item = q.closest(".faq-item");
        item.classList.toggle("open");
      });
    });
  });

  // ===== Helper functions
  function formatINR(n, round = true) {
    const val = round ? Math.round(n) : n;
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 0
    }).format(val);
  }

  function calcEmi(P, annual, months) {
    const r = annual / 12 / 100;
    if (P <= 0 || annual <= 0 || months <= 0) return 0;
    return (P * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
  }

  // ===== EMI Quick Preview (home tools)
  const qLoan = document.getElementById("loanAmount");
  const qRate = document.getElementById("interestRate");
  const qTenure = document.getElementById("tenureMonths");
  const qEmi = document.getElementById("emiBig");

  function refreshQuickEmi() {
    if (!qLoan || !qRate || !qTenure || !qEmi) return;
    const P = Number(qLoan.value || 0);
    const annual = Number(qRate.value || 0);
    const n = Number(qTenure.value || 0);
    qEmi.textContent = formatINR(calcEmi(P, annual, n));
  }

  [qLoan, qRate, qTenure].forEach((el) => {
    if (!el) return;
    el.addEventListener("input", refreshQuickEmi);
  });
  refreshQuickEmi();

  // ===== EMI Full Page Extra (donut + schedule)
  (function () {
    const $ = (id) => document.getElementById(id);

    const loanAmountEl = $("loanAmount");
    const interestRateEl = $("interestRate");
    const tenureMonthsEl = $("tenureMonths");
    const emiBigEl = $("emiBig");

    const loanAmountLabel = $("loanAmountLabel");
    const interestRateLabel = $("interestRateLabel");
    const tenureLabel = $("tenureLabel");

    const loanAmountInput = $("loanAmountInput");
    const interestRateInput = $("interestRateInput");
    const tenureMonthsInput = $("tenureMonthsInput");

    const totalAmountEl = $("totalAmount");
    const totalInterestEl = $("totalInterest");
    const tableBody = $("emiTableBody");
    const pieCanvas = $("pieCanvas");

    // if not on EMI page, exit
    if (!loanAmountEl || !interestRateEl || !tenureMonthsEl || !emiBigEl) return;
    if (!totalAmountEl && !tableBody && !pieCanvas) return;

    function drawDonut(principal, interest) {
      if (!pieCanvas) return;
      const ctx = pieCanvas.getContext("2d");
      const w = pieCanvas.width, h = pieCanvas.height;
      ctx.clearRect(0, 0, w, h);

      const total = principal + interest;
      const pAng = total ? (principal / total) * Math.PI * 2 : 0;
      const cx = w / 2, cy = h / 2;
      const r = Math.min(w, h) / 2 - 10;

      ctx.beginPath();
      ctx.lineWidth = 22;
      ctx.strokeStyle = "rgba(15,23,42,.10)";
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();

      ctx.beginPath();
      ctx.strokeStyle = getComputedStyle(document.documentElement).getPropertyValue("--navyDeep").trim() || "#183456";
      ctx.arc(cx, cy, r, -Math.PI / 2, -Math.PI / 2 + pAng);
      ctx.stroke();

      ctx.beginPath();
      ctx.strokeStyle = getComputedStyle(document.documentElement).getPropertyValue("--yellow").trim() || "#f4b400";
      ctx.arc(cx, cy, r, -Math.PI / 2 + pAng, -Math.PI / 2 + Math.PI * 2);
      ctx.stroke();

      ctx.beginPath();
      ctx.fillStyle = "#fff";
      ctx.arc(cx, cy, r - 22, 0, Math.PI * 2);
      ctx.fill();
    }

    function buildSchedule(P, annual, n) {
      const r = annual / 12 / 100;
      const emi = calcEmi(P, annual, n);
      let balance = P;

      const rows = [];
      for (let m = 1; m <= n; m++) {
        const interest = balance * r;
        let principal = emi - interest;

        if (m === n) principal = balance;
        balance = Math.max(0, balance - principal);

        rows.push({ month: m, emi, principal, interest, balance });
      }
      return { emi, rows };
    }

    function syncInputs() {
      if (loanAmountInput) loanAmountInput.value = loanAmountEl.value;
      if (interestRateInput) interestRateInput.value = interestRateEl.value;
      if (tenureMonthsInput) tenureMonthsInput.value = tenureMonthsEl.value;

      if (loanAmountLabel) loanAmountLabel.textContent = formatINR(Number(loanAmountEl.value || 0));
      if (interestRateLabel) interestRateLabel.textContent = Number(interestRateEl.value || 0).toFixed(1);
      if (tenureLabel) tenureLabel.textContent = String(tenureMonthsEl.value || 0);
    }

    function renderAll() {
      const P = Number(loanAmountEl.value || 0);
      const annual = Number(interestRateEl.value || 0);
      const n = Number(tenureMonthsEl.value || 0);

      syncInputs();

      const { emi, rows } = buildSchedule(P, annual, n);
      emiBigEl.textContent = formatINR(emi);

      const totalPay = emi * n;
      const totalInterest = Math.max(0, totalPay - P);

      if (totalAmountEl) totalAmountEl.textContent = formatINR(totalPay);
      if (totalInterestEl) totalInterestEl.textContent = formatINR(totalInterest);

      drawDonut(P, totalInterest);

      if (tableBody) {
        tableBody.innerHTML = rows.slice(0, 240).map(r => `
          <tr>
            <td>${r.month}</td>
            <td>${formatINR(r.emi)}</td>
            <td>${formatINR(r.principal)}</td>
            <td>${formatINR(r.interest)}</td>
            <td>${formatINR(r.balance)}</td>
          </tr>
        `).join("");
      }
    }

    [loanAmountEl, interestRateEl, tenureMonthsEl].forEach(el => {
      el.addEventListener("input", renderAll);
    });

    function bindNumberToRange(numEl, rangeEl, stepFix) {
      if (!numEl || !rangeEl) return;
      numEl.addEventListener("input", () => {
        let v = Number(numEl.value || rangeEl.min);
        const min = Number(rangeEl.min), max = Number(rangeEl.max);
        v = Math.min(max, Math.max(min, v));
        if (stepFix) v = stepFix(v);
        rangeEl.value = String(v);
        renderAll();
      });
    }

    bindNumberToRange(loanAmountInput, loanAmountEl, (v) => Math.round(v / 10000) * 10000);
    bindNumberToRange(interestRateInput, interestRateEl, (v) => Math.round(v * 10) / 10);
    bindNumberToRange(tenureMonthsInput, tenureMonthsEl, (v) => Math.round(v));

    renderAll();
  })();

  // ===== Track Application (open result + scroll)
  (function () {
    const trackBtn = document.getElementById("trackBtn");
    const trackInput = document.getElementById("trackInput");
    const trackResult = document.getElementById("trackResult");

    if (!trackBtn || !trackInput || !trackResult) return;

    function renderTrack(title, sub) {
      trackResult.innerHTML = `
        <div class="track-result-title">${title}</div>
        <div class="track-result-sub">${sub}</div>
      `;
      trackResult.classList.remove("hidden");
      trackResult.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    trackBtn.addEventListener("click", () => {
      const v = (trackInput.value || "").trim();

      if (!v) {
        renderTrack(`No application found with number ""`, `Try: LH-2024-0001, LH-2024-0002, etc.`);
        return;
      }

      if (v === "LH-2024-0001") {
        renderTrack(`Status for "${v}"`, `APPROVED • Disbursement in progress (Demo)`);
      } else if (v === "LH-2024-0002") {
        renderTrack(`Status for "${v}"`, `UNDER REVIEW • Verification pending (Demo)`);
      } else {
        renderTrack(`No application found with number "${v}"`, `Try: LH-2024-0001, LH-2024-0002, etc.`);
      }
    });

    trackInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") trackBtn.click();
    });
  })();

})();






(function () {
  const dataEl = document.getElementById("toastData");
  if (!dataEl) return;

  const msg = dataEl.dataset.msg;
  const type = (dataEl.dataset.type || "info").toLowerCase();

  if (!msg) return;

  const toast = document.getElementById("toast");
  const text = document.getElementById("toastText");
  const icon = document.getElementById("toastIcon");
  const bar  = document.getElementById("toastBar");
  const closeBtn = document.getElementById("toastClose");

  // Colors by type
  let bg = "#ef4444";     // error default
  let barColor = "#ef4444";
  let symbol = "!";
  if (type === "success") { bg = "#16a34a"; barColor = "#16a34a"; symbol = "✓"; }
  if (type === "info")    { bg = "#2563eb"; barColor = "#2563eb"; symbol = "i"; }

  text.textContent = msg;
  icon.style.background = bg;
  icon.textContent = symbol;
  bar.style.background = barColor;

  // Show toast
  toast.style.display = "block";

  // Progress animation
  const duration = 2500; // 2.5 sec
  bar.style.transition = "none";
  bar.style.width = "0%";

  // next tick start animation
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      bar.style.transition = `width ${duration}ms linear`;
      bar.style.width = "100%";
    });
  });

  let timer = setTimeout(hide, duration);

  function hide() {
    toast.style.display = "none";
    bar.style.transition = "none";
    bar.style.width = "0%";
    clearTimeout(timer);
  }

  closeBtn.addEventListener("click", hide);
})();





// ✅ remove logout=1 from url so refresh won't re-trigger toast
if (window.location.search.includes("logout=1")) {
  const url = new URL(window.location.href);
  url.searchParams.delete("logout");
  window.history.replaceState({}, document.title, url.pathname + url.search);
}


(function () {
  const data = document.getElementById("toastData");
  if (!data) return;

  const msg = data.getAttribute("data-msg");
  const type = data.getAttribute("data-type");

  if (!msg) return;

  showToast(msg, type); // assume tumhara toast function exists

  // ✅ 3 sec baad toast hide (agar showToast me already timer nahi hai)
  setTimeout(() => {
    const toast = document.getElementById("toast");
    if (toast) toast.style.display = "none";
  }, 3000);
})();



// ==========================================
// 🚀 CUSTOMER REGISTRATION STRICT VALIDATION
// ==========================================
(function () {
    const regForm = document.querySelector('form[action*="register"]');
    if (!regForm) return;

    const phoneInp = regForm.querySelector('input[name="phone"]');
    const pinInp = regForm.querySelector('input[name="pincode"]');
    const passInp = regForm.querySelector('input[name="password"]');
    const confInp = regForm.querySelector('input[name="confirmPassword"]');

    // 1. Phone: Sirf 10 digit allow karo
    if (phoneInp) {
        phoneInp.addEventListener('input', function(e) {
            this.value = this.value.replace(/[^0-9]/g, '').slice(0, 10);
        });
    }

    // 2. Pincode: Sirf 6 digit (Indian Standard)
    if (pinInp) {
        pinInp.addEventListener('input', function(e) {
            this.value = this.value.replace(/[^0-9]/g, '').slice(0, 6);
        });
    }

    // 3. Final Submit Validation
    regForm.addEventListener('submit', function (e) {
        let hasError = false;
        let errorMsg = "";

        if (phoneInp.value.length !== 10) {
            errorMsg = "Mobile number must be exactly 10 digits.";
            hasError = true;
        } else if (pinInp.value.length !== 6) {
            errorMsg = "Pincode must be exactly 6 digits.";
            hasError = true;
        } else if (passInp.value !== confInp.value) {
            errorMsg = "Passwords do not match!";
            hasError = true;
        }

        if (hasError) {
            e.preventDefault();
            // Agar aapka showToast function globally available hai
            if (typeof showToast === "function") {
                showToast(errorMsg, "error");
            } else {
                alert(errorMsg);
            }
        }
    });
})();

// ==========================================
// 🧹 TOAST CLEANUP & LOGIC FIX
// ==========================================
// (Aapne niche do baar toast ka code likha hai,
// niche wale block ko hata kar sirf ek clean logic rakho)
(function () {
    const dataEl = document.getElementById("toastData");
    if (!dataEl) return;

    const msg = dataEl.dataset.msg || dataEl.getAttribute("data-msg");
    const type = (dataEl.dataset.type || dataEl.getAttribute("data-type") || "info").toLowerCase();

    if (!msg || msg.trim() === "") return;

    const toast = document.getElementById("toast");
    const text = document.getElementById("toastText");
    const icon = document.getElementById("toastIcon");
    const bar  = document.getElementById("toastBar");

    if (!toast || !text) return;

    // Setup Appearance
    let bg = "#ef4444"; // error
    let symbol = "!";
    if (type === "success") { bg = "#16a34a"; symbol = "✓"; }
    if (type === "info")    { bg = "#2563eb"; symbol = "i"; }

    text.textContent = msg;
    if(icon) {
        icon.style.background = bg;
        icon.textContent = symbol;
    }
    if(bar) bar.style.background = bg;

    // Show
    toast.style.display = "block";

    // Auto-hide after 3.5 seconds
    setTimeout(() => {
        toast.style.display = "none";
    }, 3500);
})();
// staff_dashboard.js (NO CONFLICT) - only st- elements
(function () {
  // ===== Staff user dropdown (top-right)
  const stUser = document.getElementById("stUser");
  const stUserBtn = document.getElementById("stUserBtn");

  if (stUser && stUserBtn) {
    stUserBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      stUser.classList.toggle("open");
    });

    document.addEventListener("click", () => {
      stUser.classList.remove("open");
    });
  }

  // ===== Demo: Officer actions
  const byId = (id) => document.getElementById(id);

  const reviewBtn = byId("stReviewBtn");
  if (reviewBtn) {
    reviewBtn.addEventListener("click", () => {
      alert("Demo: Open Review page (connect with backend routing later).");
    });
  }

  const markInReview = byId("stMarkInReview");
  if (markInReview) {
    markInReview.addEventListener("click", () => {
      alert("Demo: Status changed to IN_REVIEW.");
    });
  }

  const requestInfo = byId("stRequestInfo");
  if (requestInfo) {
    requestInfo.addEventListener("click", () => {
      alert("Demo: Needs Info request sent to customer.");
    });
  }

  const forwardRisk = byId("stForwardRisk");
  if (forwardRisk) {
    forwardRisk.addEventListener("click", () => {
      alert("Demo: Forwarded to Risk Officer for evaluation.");
    });
  }

  // ===== Demo: Risk submit
  const riskSubmit = byId("stRiskSubmit");
  if (riskSubmit) {
    riskSubmit.addEventListener("click", () => {
      alert("Demo: Risk evaluation submitted.");
    });
  }

  // ===== Demo: Manager decision
  const approve = byId("stApproveLoan");
  if (approve) {
    approve.addEventListener("click", () => {
      alert("Demo: Loan Approved ✅");
    });
  }

  const reject = byId("stRejectLoan");
  if (reject) {
    reject.addEventListener("click", () => {
      alert("Demo: Loan Rejected ❌");
    });
  }
})();


// ===== Toast Notification Logic =====
(function() {
    // Thymeleaf se aane wale attributes ko check karo (agar hidden inputs ya global variable mein ho)
    // Lekin sabse best tarika hai DOM se trigger karna
    const toast = document.getElementById("toast");
    if (toast) {
        // Show toast
        toast.classList.add("show");

        // 4 seconds baad automatic hide kar do
        setTimeout(() => {
            toast.classList.remove("show");
        }, 4000);
    }
})();
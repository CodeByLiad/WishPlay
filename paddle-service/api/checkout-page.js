// Public HTTP function that hosts the Paddle.js checkout page for transactions

module.exports = async function handler(req, res) {
  const ptxn = req.query._ptxn || req.query.txn || "";
  const isSandbox = (process.env.PADDLE_ENV || "sandbox").toLowerCase() === "sandbox";
  const clientToken = process.env.PADDLE_CLIENT_TOKEN || "";


  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.status(200).send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>WishPlay Pro Checkout</title>
  <script src="https://cdn.paddle.com/paddle/v2/paddle.js"></script>
  <style>
    body {
      background-color: #16132E;
      color: #F8F5FF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      margin: 0;
      padding: 20px;
      box-sizing: border-box;
      text-align: center;
    }
    .card {
      background: #231F46;
      border: 1px solid rgba(255, 185, 56, 0.2);
      border-radius: 24px;
      padding: 36px 28px;
      max-width: 420px;
      width: 100%;
      box-shadow: 0 12px 36px rgba(0,0,0,0.4);
    }
    .badge {
      background: #FFB938;
      color: #16132E;
      font-weight: 800;
      font-size: 13px;
      padding: 6px 14px;
      border-radius: 20px;
      display: inline-block;
      margin-bottom: 16px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    h1 { margin: 0 0 10px; font-size: 24px; font-weight: 800; }
    p { color: #A5A1C8; font-size: 15px; margin: 0 0 28px; line-height: 1.5; }
    .btn {
      background: #FFB938;
      color: #16132E;
      border: none;
      padding: 14px 28px;
      border-radius: 28px;
      font-size: 16px;
      font-weight: 700;
      cursor: pointer;
      width: 100%;
      box-sizing: border-box;
      transition: transform 0.2s, opacity 0.2s;
    }
    .btn:active { transform: scale(0.98); }
    .hint { margin-top: 18px; font-size: 13px; color: #79769B; }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">WishPlay Pro</div>
    <h1>Complete Checkout</h1>
    <p>Tap below to pay securely with card or PayPal via Paddle.</p>
    <button id="payBtn" class="btn" onclick="openCheckout()">Open Secure Checkout</button>
    <div class="hint">Secured by Paddle · 256-bit encryption</div>
  </div>

  <script>
    ${isSandbox ? 'Paddle.Environment.set("sandbox");' : ''}
    if ("${clientToken}") {
      Paddle.Initialize({
        token: "${clientToken}",
        eventCallback: function(data) {
          if (data && data.name === "checkout.completed") {
            document.body.innerHTML = '<div class="card"><div class="badge">Success</div><h1>Payment Complete!</h1><p>WishPlay Pro has been unlocked. You can now return to the app.</p></div>';
          }
        }
      });
    }

    function openCheckout() {
      const ptxn = "${ptxn}";
      if (ptxn) {
        Paddle.Checkout.open({ transactionId: ptxn });
      } else {
        alert("Transaction ID missing.");
      }
    }

    window.addEventListener("load", function() {
      setTimeout(openCheckout, 400);
    });
  </script>
</body>
</html>`);
};

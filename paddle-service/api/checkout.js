// Vercel Serverless Function: /api/checkout
// Creates a Paddle transaction for the user and redirects to checkout.

module.exports = async function handler(req, res) {
  // Enable CORS
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    return res.status(200).end();
  }

  const uid = req.query.uid || req.body?.uid || "";
  if (!uid) {
    return res.status(400).send("Missing user ID (?uid=...)");
  }

  const apiKey = process.env.PADDLE_API_KEY || "";
  const priceId = process.env.PADDLE_PRICE_ID || "";
  const isSandbox = (process.env.PADDLE_ENV || "sandbox").toLowerCase() === "sandbox";
  const baseUrl = isSandbox ? "https://sandbox-api.paddle.com" : "https://api.paddle.com";

  if (!apiKey || !priceId) {
    return res.status(500).json({ error: "Paddle credentials not configured in environment" });
  }


  try {
    const response = await fetch(`${baseUrl}/transactions`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        items: [
          {
            price_id: priceId,
            quantity: 1,
          },
        ],
        custom_data: {
          user_id: uid,
        },
      }),
    });

    if (!response.ok) {
      const errText = await response.text();
      console.error("Paddle transaction creation failed:", response.status, errText);
      return res.status(502).json({ error: "Paddle API error", details: errText });
    }

    const data = await response.json();
    const txnId = data?.data?.id;
    let checkoutUrl = data?.data?.checkout?.url;

    // If checkoutUrl is not set or points to dead Cloud Function, point to /paddleCheckout
    if (!checkoutUrl || checkoutUrl.includes("cloudfunctions.net")) {
      const proto = req.headers["x-forwarded-proto"] || "https";
      const host = req.headers.host || "localhost:3000";
      checkoutUrl = `${proto}://${host}/paddleCheckout?_ptxn=${txnId}`;
    }

    // If request accepts HTML or from a browser link, redirect immediately
    if (req.headers.accept?.includes("text/html") || req.method === "GET") {
      return res.redirect(302, checkoutUrl);
    }

    // Otherwise return JSON
    return res.status(200).json({ url: checkoutUrl, transactionId: txnId });

  } catch (err) {
    console.error("Error in /api/checkout:", err);
    return res.status(500).json({ error: err.message });
  }
};

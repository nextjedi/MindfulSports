// Supabase Edge Function: rc-webhook
// Handles RevenueCat webhook events and syncs subscription state
// to the `subscriptions` table.
//
// Register this URL in each RevenueCat project:
//   Dashboard → Integrations → Webhooks → https://<project>.supabase.co/functions/v1/rc-webhook
// Set the Authorization header secret in RevenueCat and store it as:
//   REVENUECAT_WEBHOOK_SECRET (Supabase secret)

import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPPORTED_EVENTS = new Set([
  "INITIAL_PURCHASE",
  "TRIAL_STARTED",
  "TRIAL_CONVERTED",
  "RENEWAL",
  "CANCELLATION",
  "EXPIRATION",
  "BILLING_ISSUE",
  "UNCANCELLATION",
]);

/** Derives sport_id from a RevenueCat product identifier.
 *  e.g. "mindful_tennis_monthly" → "tennis"
 *       "mindful_badminton_annual" → "badminton"
 *       "quarterly_subscription" → "tennis" (legacy non-standard ID)
 */
function sportIdFromProductId(productId: string): string {
  // Strip Google Play base plan suffix (e.g., "mindful_tennis_weekly:p1w-autorenewing")
  const baseId = productId.split(":")[0];
  // Standard pattern: mindful_<sport>_<plan>
  const match = baseId.match(/^mindful_([a-z]+)_/);
  if (match) return match[1];
  // Non-standard legacy products (e.g., "quarterly_subscription") — default to "tennis"
  return "tennis";
}

/** Derives plan from a RevenueCat product identifier.
 *  Handles Google Play base plan suffixes and non-standard product IDs.
 *  e.g. "mindful_tennis_weekly" → "weekly"
 *       "mindful_tennis_weekly:p1w-autorenewing" → "weekly"
 *       "quarterly_subscription" → "quarterly"
 */
function planFromProductId(productId: string): string | null {
  // Strip Google Play base plan suffix before parsing
  const baseId = productId.split(":")[0];
  // Match known plan keywords in order of specificity
  const planKeywords = ["lifetime", "annual", "quarterly", "monthly", "weekly"];
  for (const keyword of planKeywords) {
    if (baseId.includes(keyword)) return keyword;
  }
  // Fallback: last underscore-separated segment
  const match = baseId.match(/_([a-z]+)$/);
  return match ? match[1] : null;
}

/** Maps a RevenueCat event type to a subscription status. */
function statusFromEvent(eventType: string): string {
  switch (eventType) {
    case "TRIAL_STARTED":
      return "trial";
    case "INITIAL_PURCHASE":
    case "TRIAL_CONVERTED":
    case "RENEWAL":
    case "UNCANCELLATION":
      return "active";
    case "CANCELLATION":
      return "cancelled";
    case "EXPIRATION":
      return "expired";
    case "BILLING_ISSUE":
      return "grace_period";
    default:
      return "active";
  }
}

/** Parses the store identifier from RevenueCat's store field. */
function parseStore(store: string): string | null {
  if (store === "APP_STORE" || store === "AMAZON") return "app_store";
  if (store === "PLAY_STORE") return "play_store";
  return null;
}

Deno.serve(async (req: Request) => {
  // Only accept POST
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  // Verify RevenueCat webhook secret
  const authHeader = req.headers.get("Authorization");
  const webhookSecret = Deno.env.get("REVENUECAT_WEBHOOK_SECRET");
  if (!webhookSecret || authHeader !== webhookSecret) {
    console.error("Webhook authorization failed");
    return new Response("Unauthorized", { status: 401 });
  }

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }

  const event = body.event as Record<string, unknown> | undefined;
  if (!event) {
    return new Response("Missing event payload", { status: 400 });
  }

  const eventType = event.type as string;
  if (!SUPPORTED_EVENTS.has(eventType)) {
    // Acknowledge but ignore unsupported event types
    console.log(`Ignoring unsupported event type: ${eventType}`);
    return new Response(JSON.stringify({ received: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }

  // app_user_id is the Supabase user ID (set during RC SDK initialization)
  const userId = event.app_user_id as string | undefined;
  if (!userId) {
    return new Response("Missing app_user_id", { status: 400 });
  }

  const productId = event.product_id as string | undefined;
  if (!productId) {
    return new Response("Missing product_id", { status: 400 });
  }

  const sportId = sportIdFromProductId(productId);
  const status = statusFromEvent(eventType);
  const store = parseStore(event.store as string ?? "");
  const rcCustomerId = event.id as string | undefined;
  const isTrial = eventType === "TRIAL_STARTED";

  // Timestamps from RevenueCat come as ISO strings or epoch seconds
  const trialEndsAt = event.expiration_at_ms != null
    ? Number(event.expiration_at_ms)
    : event.trial_end_date
    ? new Date(event.trial_end_date as string).getTime()
    : null;

  const currentPeriodEnd = event.expiration_at_ms != null
    ? Number(event.expiration_at_ms)
    : null;

  const plan = planFromProductId(productId);

  const now = Date.now();

  // Use service role to bypass RLS — webhook runs server-side
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { error } = await supabase.from("subscriptions").upsert(
    {
      user_id: userId,
      sport_id: sportId,
      rc_customer_id: rcCustomerId ?? null,
      status,
      plan: plan ?? null,
      is_trial: isTrial,
      trial_ends_at: trialEndsAt ?? null,
      current_period_end: currentPeriodEnd ?? null,
      store: store ?? null,
      updated_at: now,
    },
    {
      onConflict: "user_id,sport_id,store",
      ignoreDuplicates: false,
    },
  );

  if (error) {
    console.error("Supabase upsert error:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  console.log(`Processed ${eventType} for user=${userId} sport=${sportId} plan=${plan} status=${status} product=${productId}`);

  return new Response(JSON.stringify({ received: true }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});

/**
 * Delivers one notification to every phone its member is signed in on, through Firebase Cloud
 * Messaging. The database calls this after inserting into `notifications`, with the row's id and
 * the shared secret in `x-push-secret`. The message is data-only, so the app posts it on its own
 * channels and a tap opens the notification's subject.
 */
import { createClient } from "jsr:@supabase/supabase-js@2";

interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
}

const serviceAccount: ServiceAccount = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT") ?? "{}");
const webhookSecret = Deno.env.get("PUSH_WEBHOOK_SECRET") ?? "";
const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

let cachedToken: { value: string; expiresAt: number } | null = null;

/** An OAuth access token for FCM, from a JWT the service account signs; reused until a minute before it expires. */
async function accessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt - 60 > now) return cachedToken.value;
  const encode = (value: object) => base64Url(new TextEncoder().encode(JSON.stringify(value)));
  const unsigned = `${encode({ alg: "RS256", typ: "JWT" })}.${encode({
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  })}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(serviceAccount.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)));
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsigned}.${base64Url(signature)}`,
    }),
  });
  if (!response.ok) throw new Error(`Token exchange failed: ${response.status} ${await response.text()}`);
  const body = await response.json();
  cachedToken = { value: body.access_token, expiresAt: now + body.expires_in };
  return cachedToken.value;
}

function base64Url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): ArrayBuffer {
  const base64 = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0)).buffer;
}

Deno.serve(async (request) => {
  if (request.headers.get("x-push-secret") !== webhookSecret || !webhookSecret) {
    return new Response("Forbidden", { status: 403 });
  }
  if (!serviceAccount.private_key) return new Response("FCM_SERVICE_ACCOUNT is not set", { status: 503 });

  const { notification_id: notificationId } = await request.json();
  const { data: notification, error } = await supabase
    .from("notifications")
    .select("id, user_id, house_id, type, title, body, created_at")
    .eq("id", notificationId)
    .single();
  if (error || !notification) return new Response("Notification not found", { status: 404 });

  const { data: devices } = await supabase.from("device_tokens").select("token").eq("user_id", notification.user_id);
  if (!devices?.length) return new Response("No devices", { status: 200 });

  const token = await accessToken();
  const endpoint = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`;
  const stale: string[] = [];
  await Promise.all(devices.map(async ({ token: deviceToken }) => {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token: deviceToken,
          android: { priority: "high" },
          data: {
            notification_id: notification.id,
            house_id: notification.house_id ?? "",
            type: notification.type,
            title: notification.title,
            body: notification.body,
            created_at: notification.created_at,
          },
        },
      }),
    });
    if (response.status === 404) stale.push(deviceToken);
  }));
  if (stale.length) await supabase.from("device_tokens").delete().in("token", stale);
  return new Response(JSON.stringify({ sent: devices.length - stale.length, removed: stale.length }), {
    headers: { "Content-Type": "application/json" },
  });
});

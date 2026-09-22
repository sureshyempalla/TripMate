const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const LOOKAHEAD_MINUTES = 35; // slightly more than the max reminderLeadMinutes option
const RUN_EVERY_MINUTES = 5;

/**
 * Runs every 5 minutes. For each activity starting within the lookahead
 * window whose own reminderLeadMinutes has now been reached, sends an FCM
 * push to every device token registered for that trip's owner, then marks
 * the activity so it isn't notified twice.
 *
 * This lives server-side (rather than scheduling a local notification on
 * the device when the activity is created) so the reminder still fires if
 * the app has been killed, the phone rebooted, or the activity was edited
 * from a different device.
 */
exports.sendUpcomingActivityReminders = onSchedule(
  { schedule: `every ${RUN_EVERY_MINUTES} minutes`, timeZone: "Etc/UTC" },
  async () => {
    const db = getFirestore();
    const now = Date.now();
    const windowEnd = now + LOOKAHEAD_MINUTES * 60_000;

    const candidates = await db
      .collection("activities")
      .where("startAtEpochMillis", ">=", now)
      .where("startAtEpochMillis", "<=", windowEnd)
      .get();

    const dueActivities = candidates.docs.filter((doc) => {
      const data = doc.data();
      if (data.reminderSentAt) return false;
      const leadMs = (data.reminderLeadMinutes ?? 30) * 60_000;
      return data.startAtEpochMillis - leadMs <= now;
    });

    if (dueActivities.length === 0) return;

    // Group by trip so we fetch each trip's owner (and their device tokens)
    // only once even if several of their activities are due this run.
    const tripIds = [...new Set(dueActivities.map((d) => d.data().tripId))];
    const tripSnaps = await db.getAll(
      ...tripIds.map((id) => db.collection("trips").doc(id))
    );
    const tripById = new Map(tripSnaps.map((snap) => [snap.id, snap.data()]));

    const ownerIds = [...new Set(tripSnaps.map((s) => s.data()?.ownerId).filter(Boolean))];
    const userSnaps = await db.getAll(
      ...ownerIds.map((id) => db.collection("users").doc(id))
    );
    const tokensByOwner = new Map(
      userSnaps.map((snap) => [snap.id, snap.data()?.deviceTokens ?? []])
    );

    const messaging = getMessaging();
    const batch = db.batch();

    for (const doc of dueActivities) {
      const activity = doc.data();
      const trip = tripById.get(activity.tripId);
      if (!trip) continue;

      const tokens = tokensByOwner.get(trip.ownerId) ?? [];
      if (tokens.length > 0) {
        await messaging.sendEachForMulticast({
          tokens,
          notification: {
            title: `Next up: ${activity.title}`,
            body: activity.place?.name ?? "Tap for details",
          },
          data: {
            activityId: doc.id,
            activityTitle: activity.title,
            placeName: activity.place?.name ?? "",
            tripId: activity.tripId,
          },
        });
      }

      batch.update(doc.ref, { reminderSentAt: Timestamp.now() });
    }

    await batch.commit();
  }
);

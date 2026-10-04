const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const db = getFirestore();

exports.notifyGroupOnMemberReply = onDocumentCreated(
  {
    document: "activity_responses/{responseId}",
    region: "australia-southeast2",
  },
  async (event) => {
    const response = event.data?.data();
    if (!response) {
      return;
    }

    const responderUid = response.userId;
    const groupId = response.groupId;
    const activityId = response.activityId;

    if (!responderUid || !groupId) {
      console.log("Reply notification skipped: missing userId or groupId.");
      return;
    }

    const [groupSnapshot, responderSnapshot, activitySnapshot] =
      await Promise.all([
        db.collection("groups").doc(groupId).get(),
        db.collection("users").doc(responderUid).get(),
        activityId
          ? db.collection("activities").doc(activityId).get()
          : Promise.resolve(null),
      ]);

    if (!groupSnapshot.exists) {
      console.log(
        `Reply notification skipped: group ${groupId} does not exist.`
      );
      return;
    }

    const group = groupSnapshot.data() || {};
    const memberIds = Array.isArray(group.memberIds)
      ? group.memberIds.filter(
          (uid) => typeof uid === "string" && uid !== responderUid
        )
      : [];

    if (memberIds.length === 0) {
      console.log("Reply notification skipped: no other group members.");
      return;
    }

    const userSnapshots = await Promise.all(
      memberIds.map((uid) => db.collection("users").doc(uid).get())
    );

    const tokens = [
      ...new Set(
        userSnapshots
          .map((snapshot) => snapshot.get("fcmToken"))
          .filter((token) => typeof token === "string" && token.length > 0)
      ),
    ];

    if (tokens.length === 0) {
      console.log("Reply notification skipped: no recipient FCM tokens.");
      return;
    }

    const responderName =
      responderSnapshot.get("displayName") || "A group member";
    const groupName = group.name || "your group";
    const activityTitle =
      activitySnapshot && activitySnapshot.exists
        ? activitySnapshot.get("title")
        : null;

    const body = activityTitle
      ? `${responderName} replied to "${activityTitle}" in ${groupName}.`
      : `${responderName} added a new reply in ${groupName}.`;

    const result = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "New reply in Knot",
        body,
      },
      data: {
        type: "member_reply",
        groupId,
        activityId: activityId || "",
        responderUid,
      },
      android: {
        priority: "high",
      },
    });

    console.log(
      `Member reply notification sent: ${result.successCount} success, ${result.failureCount} failure.`
    );
  }
);

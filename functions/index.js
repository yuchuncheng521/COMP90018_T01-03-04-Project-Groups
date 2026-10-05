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


exports.notifyAssignedMemberOnNewPrompt = onDocumentCreated(
  {
    document: "activities/{activityId}",
    region: "australia-southeast2",
  },
  async (event) => {
    const activity = event.data?.data();
    if (!activity) {
      return;
    }

    const assignedTo = activity.assignedTo;
    const createdBy = activity.createdBy;

    // createActivity fans out one activity document per group member.
    // Do not notify the person who created the prompt about their own copy.
    if (!assignedTo || !createdBy || assignedTo === createdBy) {
      return;
    }

    const [recipientSnapshot, creatorSnapshot] = await Promise.all([
      db.collection("users").doc(assignedTo).get(),
      db.collection("users").doc(createdBy).get(),
    ]);

    const token = recipientSnapshot.get("fcmToken");
    if (typeof token !== "string" || token.length === 0) {
      console.log(
        `New prompt notification skipped: no FCM token for ${assignedTo}.`
      );
      return;
    }

    const creatorName =
      creatorSnapshot.get("displayName") || "A group member";
    const groupName = activity.groupName || "your group";
    const promptTitle = activity.title || "a new activity";

    const messageId = await getMessaging().send({
      token,
      notification: {
        title: "New activity in Knot",
        body: `${creatorName} added "${promptTitle}" in ${groupName}.`,
      },
      data: {
        type: "new_prompt",
        groupId: activity.groupId || "",
        activityId: event.params.activityId || "",
        createdBy,
      },
      android: {
        priority: "high",
      },
    });

    console.log(
      `New prompt notification sent to ${assignedTo}: ${messageId}`
    );
  }
);

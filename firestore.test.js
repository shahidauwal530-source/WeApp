const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const CHARLIE_UID = "charlie_uid_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastTs = () => new Date(Date.now() - 5000);
const futureTs = () => new Date(Date.now() + 120000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users_public").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("reports").where("reporterId", "==", ALICE_UID).get());
});

test("Global private user profile + Facebook privacy/security/notification updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTs();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "kwame_accra",
      displayName: "Kwame Mensah",
      email: "kwame@example.com",
      phone: "+233241234567",
      countryCode: "GH",
      countryName: "Ghana",
      countryFlag: "🇬🇭",
      city: "Accra",
      currencyCode: "GHS",
      bio: "Gaming & creator",
      plan: "FREE",
      coins: 100,
      rankPoints: 10,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      darkMode: true,
      themeMode: "dark",
      fontSize: "medium",
      language: "en",
      accountStatus: "active",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Update Facebook-style privacy & notification & security settings
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      privacyPosts: "Friends",
      privacyMessage: "Everyone",
      twoFactorEnabled: true,
      notifLikes: false,
      language: "pcm",
      updatedAt: pastTs(),
    })
  );

  // Bob cannot read or update Alice's private PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ twoFactorEnabled: false, updatedAt: pastTs() }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true, updatedAt: pastTs() }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ city: "London", updatedAt: futureTs() }));
});

test("Safety lists, notifications, sessions, hidden posts, and moderation reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Safety list: block Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("safety_lists").doc(BOB_UID).set({
      userId: ALICE_UID,
      targetUserId: BOB_UID,
      targetName: "Bob",
      listType: "blocked",
      createdAt: pastTs(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("safety_lists").get());

  // Sessions
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").set({
      userId: ALICE_UID,
      deviceName: "Pixel 8 Pro",
      city: "Accra",
      countryName: "Ghana",
      activeFlag: true,
      createdAt: pastTs(),
      updatedAt: pastTs(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").get());

  // Notifications
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_1").set({
      userId: ALICE_UID,
      actorId: ALICE_UID,
      actorName: "WeApp",
      notifType: "welcome",
      title: "Welcome!",
      body: "100 WeCoins added",
      readFlag: false,
      createdAt: pastTs(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_1").update({
      readFlag: true,
    })
  );

  // Reports
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      reason: "Spam",
      details: "Repeated promotional spam",
      createdAt: pastTs(),
    })
  );
  await assertFails(
    bobDb.collection("reports").where("reporterId", "==", ALICE_UID).get()
  );
});

test("Global posts, chats, challenges, marketplace", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTs();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Kwame",
      authorCity: "Accra",
      authorCountryCode: "GH",
      authorCountryFlag: "🇬🇭",
      authorPlan: "PRO",
      content: "Hello world from Accra!",
      category: "Social",
      postPrivacy: "Everyone",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("posts").get());

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      creatorId: ALICE_UID,
      creatorName: "Kwame",
      peerId: BOB_UID,
      peerName: "Sadiq",
      lastMessage: "Hi",
      lastSenderId: ALICE_UID,
      typingUserIds: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(charlieDb.collection("chats").doc("chat_1").get());
});

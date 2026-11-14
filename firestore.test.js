const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const WORKSHOP_UID = "workshop_123";
const OWNER_UID = "owner_456";
const STRANGER_UID = "stranger_789";
const TEST_VIN = "1HGCR2F83HA123456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read inventory, interventions, or vehicles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("inventory").get());
  await assertFails(unauthDb.collection("interventions").get());
  await assertFails(unauthDb.collection("vehicles").get());
});

test("Workshop: can create inventory item with required fields", async () => {
  const workshopDb = testEnv.authenticatedContext(WORKSHOP_UID).firestore();
  await assertSucceeds(
    workshopDb.collection("inventory").doc("item_1").set({
      workshopId: WORKSHOP_UID,
      partNumber: "BOSCH-PAD-01",
      name: "Pastillas de freno delanteras",
      category: "Frenos",
      stock: 12,
      minStock: 4,
      unitPrice: 38990,
      createdAt: new Date(),
    })
  );
});

test("Owner: cannot read or list workshop inventory", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("inventory").doc("item_1").set({
      workshopId: WORKSHOP_UID,
      partNumber: "BOSCH-PAD-01",
      name: "Pastillas de freno delanteras",
      category: "Frenos",
      stock: 12,
      minStock: 4,
      unitPrice: 38990,
      createdAt: new Date(),
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertFails(ownerDb.collection("inventory").doc("item_1").get());
  await assertFails(ownerDb.collection("inventory").where("workshopId", "==", WORKSHOP_UID).get());
});

test("Workshop: can create intervention with watermarked photo and signature flags", async () => {
  const workshopDb = testEnv.authenticatedContext(WORKSHOP_UID).firestore();
  await assertSucceeds(
    workshopDb.collection("interventions").doc("int_1").set({
      vin: TEST_VIN,
      plate: "BB-BB-10",
      workshopId: WORKSHOP_UID,
      workshopName: "Taller Mecánico San Joaquín",
      ownerId: OWNER_UID,
      date: "2026-11-12",
      mileage: 45200,
      workDone: "Mantenimiento 45.000 km y cambio pastillas",
      partsUsed: "Pastillas cerámicas Bosch",
      photoBase64: "data:image/jpeg;base64,samplephoto",
      photoGps: "-33.4988, -70.6133",
      photoTimestamp: "12/11/2026 15:30",
      hasPhoto: true,
      signatureBase64: "sample_signature_data",
      hasSignature: true,
      signatureDisclaimer: "Trazo capturado en el dispositivo, no una firma electrónica certificada",
      createdAt: new Date(),
    })
  );
});

test("Owner: can read and query interventions associated with their ownerId", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("interventions").doc("int_1").set({
      vin: TEST_VIN,
      plate: "BB-BB-10",
      workshopId: WORKSHOP_UID,
      workshopName: "Taller Mecánico San Joaquín",
      ownerId: OWNER_UID,
      date: "2026-11-12",
      mileage: 45200,
      workDone: "Mantenimiento preventivo",
      hasPhoto: false,
      hasSignature: false,
      createdAt: new Date(),
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(ownerDb.collection("interventions").doc("int_1").get());
  await assertSucceeds(
    ownerDb.collection("interventions").where("ownerId", "==", OWNER_UID).get()
  );

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("interventions").doc("int_1").get());
});

test("Vehicle: Owner registers by VIN and Workshop role can lookup by VIN and update mileage", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(
    ownerDb.collection("vehicles").doc(TEST_VIN).set({
      vin: TEST_VIN,
      plate: "BB-BB-10",
      brand: "Toyota",
      model: "Corolla",
      year: 2021,
      mileage: 45000,
      ownerId: OWNER_UID,
      servicingWorkshopIds: [],
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    ownerDb.collection("vehicles").where("ownerId", "==", OWNER_UID).get()
  );

  // Workshop profile exists with role taller
  const workshopDb = testEnv.authenticatedContext(WORKSHOP_UID).firestore();
  await assertSucceeds(
    workshopDb.collection("users").doc(WORKSHOP_UID).set({
      userId: WORKSHOP_UID,
      email: "taller@vincheck.cl",
      displayName: "Taller San Joaquín",
      role: "taller",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(workshopDb.collection("vehicles").doc(TEST_VIN).get());
  await assertSucceeds(
    workshopDb.collection("vehicles").doc(TEST_VIN).update({
      mileage: 45200,
      servicingWorkshopIds: [WORKSHOP_UID],
      updatedAt: new Date(),
    })
  );

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("vehicles").doc(TEST_VIN).get());
});

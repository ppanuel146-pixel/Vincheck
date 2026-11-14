package com.example.data

import com.example.data.models.AlertUrgency
import com.example.data.models.DtcCode
import com.example.data.models.InventoryItem
import com.example.data.models.Intervention
import com.example.data.models.MaintenanceAlert
import com.example.data.models.Vehicle

object DemoData {

  val demoVehicles = listOf(
    Vehicle(
      id = "1HGCR2F83HA102938",
      vin = "1HGCR2F83HA102938",
      plate = "BB-BB-10",
      brand = "Toyota",
      model = "Corolla 1.8 XEI",
      year = 2021,
      mileage = 45200,
      ownerId = "owner_demo",
      servicingWorkshopIds = listOf("workshop_demo")
    ),
    Vehicle(
      id = "KMHCT81BPJU283912",
      vin = "KMHCT81BPJU283912",
      plate = "LG-TR-84",
      brand = "Hyundai",
      model = "Tucson 2.0 CRDi",
      year = 2020,
      mileage = 62150,
      ownerId = "owner_demo",
      servicingWorkshopIds = listOf("workshop_demo")
    )
  )

  val demoDtcCodes = listOf(
    DtcCode(
      code = "P0300",
      system = "Tren Motriz (Powertrain)",
      description = "Fallo de encendido detectado en cilindros múltiples/aleatorios.",
      nextStep = "Inspeccionar bujías de encendido, bobinas individuales (COP) y medir presión en riel de combustible con manómetro.",
      severity = "Alta",
      isDemo = true
    ),
    DtcCode(
      code = "P0420",
      system = "Emisiones / Escape",
      description = "Eficiencia del sistema catalizador por debajo del umbral (Banco 1).",
      nextStep = "Monitorear con osciloscopio la señal de la sonda lambda posterior (O2S2). Descartar fugas de gases antes del catalizador.",
      severity = "Media",
      isDemo = true
    ),
    DtcCode(
      code = "P0171",
      system = "Gestión de Combustible",
      description = "Sistema demasiado pobre (Banco 1) - exceso de aire o defecto de inyección.",
      nextStep = "Realizar prueba de humo en colector de admisión para detectar entradas de aire no medidas y limpiar sensor MAF.",
      severity = "Media",
      isDemo = true
    ),
    DtcCode(
      code = "P0115",
      system = "Refrigeración Motor",
      description = "Fallo en circuito del sensor de temperatura del refrigerante motor (ECT).",
      nextStep = "Verificar curva resistencia-temperatura (NTC) del sensor ECT y continuidad eléctrica del arnés hacia terminal de ECU.",
      severity = "Baja",
      isDemo = true
    ),
    DtcCode(
      code = "P0500",
      system = "Transmisión / Velocidad",
      description = "Sensor de velocidad del vehículo (VSS) - señal errática o ausente.",
      nextStep = "Comprobar señal de onda cuadrada del sensor VSS en osciloscopio y verificar estado del engranaje en cola de caja.",
      severity = "Media",
      isDemo = true
    ),
    DtcCode(
      code = "P0401",
      system = "Recirculación de Gases (EGR)",
      description = "Flujo insuficiente detectado en el sistema de recirculación de gases de escape (EGR).",
      nextStep = "Comprobar apertura de válvula EGR con escáner bidireccional y limpiar conductos de admisión obstruidos por carbonilla.",
      severity = "Media",
      isDemo = true
    ),
    DtcCode(
      code = "C0035",
      system = "Chasis / Frenos ABS",
      description = "Fallo en circuito del sensor de velocidad de rueda delantera izquierda (WSS).",
      nextStep = "Medir señal de sensor inductivo o efecto Hall en rueda delantera izquierda e inspeccionar anillo fónico y cableado.",
      severity = "Alta",
      isDemo = true
    ),
    DtcCode(
      code = "U0100",
      system = "Red de Comunicación CAN",
      description = "Pérdida de comunicación con el módulo de control del motor (ECM/PCM).",
      nextStep = "Verificar resistencia de terminación de 60 Ohms en pines 6 (CAN-H) y 14 (CAN-L) del conector DLC OBD-II y revisar fusibles de alimentación ECM.",
      severity = "Alta",
      isDemo = true
    )
  )

  fun calculateMaintenanceAlerts(currentKm: Int): List<MaintenanceAlert> {
    return listOf(
      createAlert(
        id = "alt_oil",
        component = "Aceite de Motor y Filtro",
        intervalKm = 10000,
        currentKm = currentKm,
        recommendation = "Reemplazar aceite sintético 5W-30 norma API SP y filtro de aceite blindado."
      ),
      createAlert(
        id = "alt_air",
        component = "Filtro de Aire y Polen",
        intervalKm = 20000,
        currentKm = currentKm,
        recommendation = "Sustituir elemento filtrante de aire motor y filtro de cabina antipolen."
      ),
      createAlert(
        id = "alt_brakes",
        component = "Pastillas y Líquido de Freno",
        intervalKm = 30000,
        currentKm = currentKm,
        recommendation = "Verificar espesor de material de fricción (mínimo 3.0 mm) y medir punto de ebullición del líquido DOT 4."
      ),
      createAlert(
        id = "alt_spark",
        component = "Bujías de Iridio / Platino",
        intervalKm = 50000,
        currentKm = currentKm,
        recommendation = "Inspección de luz de electrodos (0.9 a 1.1 mm) y calibración o sustitución de bujías."
      ),
      createAlert(
        id = "alt_timing",
        component = "Kit de Distribución y Bomba de Agua",
        intervalKm = 80000,
        currentKm = currentKm,
        recommendation = "Reemplazo preventivo de correa dentada, tensores dinámicos y bomba de refrigerante."
      )
    )
  }

  private fun createAlert(
    id: String,
    component: String,
    intervalKm: Int,
    currentKm: Int,
    recommendation: String
  ): MaintenanceAlert {
    val completedIntervals = currentKm / intervalKm
    val targetKm = (completedIntervals + 1) * intervalKm
    val remainingKm = targetKm - currentKm

    val urgency = when {
      remainingKm <= 0 -> AlertUrgency.OVERDUE
      remainingKm <= 1500 -> AlertUrgency.DUE_SOON
      else -> AlertUrgency.OK
    }

    return MaintenanceAlert(
      id = id,
      component = component,
      intervalKm = intervalKm,
      targetKm = targetKm,
      currentKm = currentKm,
      remainingKm = remainingKm,
      urgency = urgency,
      recommendation = recommendation,
      emailPrepared = true
    )
  }

  val demoInterventions = listOf(
    Intervention(
      id = "int_demo_1",
      vin = "1HGCR2F83HA102938",
      plate = "BB-BB-10",
      workshopId = "workshop_demo",
      workshopName = "Taller Mecánico San Joaquín (Duoc UC)",
      ownerId = "owner_demo",
      date = "10/11/2026",
      mileage = 45000,
      workDone = "Mantenimiento preventivo de 45.000 km. Cambio de pastillas delanteras y rectificado de discos.",
      partsUsed = "Pastillas de freno cerámicas Bosch (Ref: BOSCH-PAD-01) + Fluido DOT 4",
      photoBase64 = "",
      photoGps = "-33.4988, -70.6133 (±4.2m)",
      photoTimestamp = "10/11/2026 14:15",
      hasPhoto = false,
      signatureBase64 = "",
      hasSignature = false,
      signatureDisclaimer = "Trazo capturado en el dispositivo, no una firma electrónica certificada"
    ),
    Intervention(
      id = "int_demo_2",
      vin = "1HGCR2F83HA102938",
      plate = "BB-BB-10",
      workshopId = "workshop_demo",
      workshopName = "Taller Central Duoc",
      ownerId = "owner_demo",
      date = "15/05/2026",
      mileage = 35000,
      workDone = "Cambio de aceite sintético 5W-30 y filtro de aceite. Rotación de neumáticos y balanceo.",
      partsUsed = "Aceite Mobil 1 5W-30 + Filtro Mann W68/3",
      photoBase64 = "",
      photoGps = "-33.4990, -70.6140 (±5.0m)",
      photoTimestamp = "15/05/2026 10:20",
      hasPhoto = false,
      signatureBase64 = "",
      hasSignature = false,
      signatureDisclaimer = "Trazo capturado en el dispositivo, no una firma electrónica certificada"
    )
  )

  val demoInventory = listOf(
    InventoryItem(
      id = "inv_demo_1",
      workshopId = "workshop_demo",
      partNumber = "BOSCH-PAD-01",
      name = "Pastillas de freno cerámicas delanteras",
      category = "Frenos",
      stock = 14,
      minStock = 4,
      unitPrice = 38990
    ),
    InventoryItem(
      id = "inv_demo_2",
      workshopId = "workshop_demo",
      partNumber = "MANN-OIL-68",
      name = "Filtro de aceite blindado M20x1.5",
      category = "Filtros",
      stock = 25,
      minStock = 6,
      unitPrice = 8500
    ),
    InventoryItem(
      id = "inv_demo_3",
      workshopId = "workshop_demo",
      partNumber = "MOBIL-5W30-4L",
      name = "Aceite 100% sintético 5W-30 (Bidón 4L)",
      category = "Lubricantes",
      stock = 18,
      minStock = 5,
      unitPrice = 34990
    ),
    InventoryItem(
      id = "inv_demo_4",
      workshopId = "workshop_demo",
      partNumber = "NGK-IRID-IX4",
      name = "Juego de 4 Bujías de Iridio NGK",
      category = "Encendido",
      stock = 6,
      minStock = 3,
      unitPrice = 42500
    )
  )
}

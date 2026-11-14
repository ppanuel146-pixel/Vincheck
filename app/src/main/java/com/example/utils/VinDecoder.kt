package com.example.utils

data class VinBreakdown(
  val vin: String,
  val wmi: String,
  val regionOrCountry: String,
  val manufacturerHint: String,
  val vds: String,
  val modelYearHint: String,
  val plantAndSerial: String
)

object VinDecoder {

  fun decode(rawVin: String): VinBreakdown? {
    val clean = rawVin.uppercase().trim()
    if (!ValidationUtils.isValidVin(clean)) return null

    val wmi = clean.substring(0, 3)
    val vds = clean.substring(3, 9)
    val yearChar = clean[9]
    val plantAndSerial = clean.substring(10, 17)

    val region = when (clean[0]) {
      '1', '4', '5' -> "Estados Unidos (Norteamérica)"
      '2' -> "Canadá (Norteamérica)"
      '3' -> "México (Norteamérica)"
      '6' -> "Australia / Oceanía"
      '8' -> "Argentina / Chile / Sudamérica"
      '9' -> "Brasil / Colombia (Sudamérica)"
      'J' -> "Japón (Asia)"
      'K' -> "Corea del Sur (Asia)"
      'L' -> "China (Asia)"
      'M' -> "India / Indonesia / Tailandia"
      'S' -> "Reino Unido / Polonia (Europa)"
      'T' -> "República Checa / Hungría (Europa)"
      'V' -> "Francia / España (Europa)"
      'W' -> "Alemania (Europa)"
      'Y' -> "Suecia / Finlandia (Europa)"
      'Z' -> "Italia (Europa)"
      else -> "Región internacional (${clean[0]})"
    }

    val manufacturer = when {
      wmi.startsWith("1HG") || wmi.startsWith("2HG") || wmi.startsWith("JHM") -> "Honda"
      wmi.startsWith("1NX") || wmi.startsWith("2T1") || wmi.startsWith("4T1") || wmi.startsWith("JTD") || wmi.startsWith("JT") || wmi.startsWith("9BR") -> "Toyota"
      wmi.startsWith("KMH") || wmi.startsWith("KM8") || wmi.startsWith("5NP") -> "Hyundai"
      wmi.startsWith("KNA") || wmi.startsWith("KND") || wmi.startsWith("3KP") -> "Kia"
      wmi.startsWith("1G1") || wmi.startsWith("3G1") || wmi.startsWith("9BG") || wmi.startsWith("KL1") -> "Chevrolet / GM"
      wmi.startsWith("JS2") || wmi.startsWith("TSM") || wmi.startsWith("MA3") -> "Suzuki"
      wmi.startsWith("JN1") || wmi.startsWith("3N1") || wmi.startsWith("1N4") -> "Nissan"
      wmi.startsWith("JM1") || wmi.startsWith("3MZ") -> "Mazda"
      wmi.startsWith("WVW") || wmi.startsWith("3VW") || wmi.startsWith("9BW") -> "Volkswagen"
      wmi.startsWith("VF3") -> "Peugeot"
      wmi.startsWith("VF1") -> "Renault"
      wmi.startsWith("1FA") || wmi.startsWith("1FT") || wmi.startsWith("3FA") -> "Ford"
      wmi.startsWith("JA3") || wmi.startsWith("MMB") -> "Mitsubishi"
      wmi.startsWith("JF1") || wmi.startsWith("JF2") -> "Subaru"
      else -> "Fabricante WMI $wmi"
    }

    val yearHint = when (yearChar) {
      'A' -> "2010 (o 1980)"
      'B' -> "2011 (o 1981)"
      'C' -> "2012 (o 1982)"
      'D' -> "2013 (o 1983)"
      'E' -> "2014 (o 1984)"
      'F' -> "2015 (o 1985)"
      'G' -> "2016 (o 1986)"
      'H' -> "2017 (o 1987)"
      'J' -> "2018 (o 1988)"
      'K' -> "2019 (o 1989)"
      'L' -> "2020 (o 1990)"
      'M' -> "2021 (o 1991)"
      'N' -> "2022 (o 1992)"
      'P' -> "2023 (o 1993)"
      'R' -> "2024 (o 1994)"
      'S' -> "2025 (o 1995)"
      'T' -> "2026 (o 1996)"
      'V' -> "1997 (o 2027)"
      '1' -> "2001"
      '2' -> "2002"
      '3' -> "2003"
      '4' -> "2004"
      '5' -> "2005"
      '6' -> "2006"
      '7' -> "2007"
      '8' -> "2008"
      '9' -> "2009"
      else -> "Año código '$yearChar'"
    }

    return VinBreakdown(
      vin = clean,
      wmi = wmi,
      regionOrCountry = region,
      manufacturerHint = manufacturer,
      vds = vds,
      modelYearHint = yearHint,
      plantAndSerial = plantAndSerial
    )
  }
}

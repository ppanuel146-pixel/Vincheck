package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationUtilsTest {

  @Test
  fun extractVinFromQrPayload_returnsRawValidVin() {
    val vin = ValidationUtils.extractVinFromQrPayload("  1hgcr2f83ha102938 ")
    assertEquals("1HGCR2F83HA102938", vin)
  }

  @Test
  fun extractVinFromQrPayload_stripsCode39ImportPrefix() {
    val vin = ValidationUtils.extractVinFromQrPayload("I1HGCR2F83HA102938")
    assertEquals("1HGCR2F83HA102938", vin)
  }

  @Test
  fun extractVinFromQrPayload_extractsFromUrlAndJson() {
    val fromUrl = ValidationUtils.extractVinFromQrPayload("https://vincheck.duoc.cl/lookup?vin=kmhct41d3cu123456&plate=BBBB10")
    assertEquals("KMHCT41D3CU123456", fromUrl)

    val fromJson = ValidationUtils.extractVinFromQrPayload("""{"vin":"wvwzzz1kzaw012345","year":2010}""")
    assertEquals("WVWZZZ1KZAW012345", fromJson)
  }

  @Test
  fun extractVinFromQrPayload_rejectsInvalidCharactersIOQ() {
    assertNull(ValidationUtils.extractVinFromQrPayload("1HGCR2F83IA102938"))
    assertNull(ValidationUtils.extractVinFromQrPayload("1HGCR2F83OA102938"))
    assertNull(ValidationUtils.extractVinFromQrPayload("1HGCR2F83QA102938"))
    assertNull(ValidationUtils.extractVinFromQrPayload("SHORTVIN123"))
  }
}

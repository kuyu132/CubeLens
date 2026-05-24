package com.cubelens.util

import com.cubelens.model.CubeFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FaceletsLoaderTest {

  @Test
  fun scansFromSolvedFacelets_hasSixFaces() {
    val scans = FaceletsLoader.scansFromFacelets(ScrambleUtils.SOLVED_FACELETS)
    assertNotNull(scans)
    assertEquals(6, scans!!.size)
    assertEquals(9, scans.getValue(CubeFace.U).colors.size)
  }
}

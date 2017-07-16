package de.innfactory.akkaliftml.util

import org.scalatest.{FunSuite, Matchers}

class MLPreferencesTest extends FunSuite with Matchers  {

  test("testSetCurrentALSModel") {
    MLPreferences.setCurrentALSModel("test")
    val als = MLPreferences.getCurrentALSModel()
    assert(als equals "test")
    MLPreferences.setCurrentALSModel("")
    val alsEmpty = MLPreferences.getCurrentALSModel()
    assert(alsEmpty equals "")
  }

}

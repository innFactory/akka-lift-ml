package de.innfactory.akkaliftml.als

import org.scalatest.{FlatSpec, Matchers, OptionValues}

class AlsModelTest extends FlatSpec with Matchers with OptionValues {
  "An AlsModel" should " created successfully" in {
    val alsModel = AlsModel("test")
    assert(alsModel.ratings eq "test")
  }
}

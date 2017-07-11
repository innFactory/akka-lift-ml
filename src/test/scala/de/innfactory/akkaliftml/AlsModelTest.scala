package de.innfactory.akkaliftml

import de.innfactory.akkaliftml.als.AlsModel

class AlsModelTest extends MLSpec {
  "An AlsModel" should " created successfully" in {
    val alsModel = AlsModel("test")
    assert(alsModel.ratings eq "test")
  }
}

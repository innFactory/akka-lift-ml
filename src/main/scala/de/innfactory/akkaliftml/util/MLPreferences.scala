package de.innfactory.akkaliftml.util

import java.util.prefs.Preferences

object MLPreferences {
  private val mlPreferences: Preferences = Preferences.userRoot.node("/de/innfactory/akkaliftml")

  private final val KEY_ALS = "CURRENT_ALS_MODEL_PATH"

  def setCurrentALSModel(model : String) = {
    mlPreferences.put(KEY_ALS, model)
  }
  def getCurrentALSModel() : String = {
    mlPreferences.get(KEY_ALS, "")
  }
}

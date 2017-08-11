package de.innfactory.akkaliftml.models

import com.byteslounge.slickrepo.meta.Entity

case class AlsTraining(override val id: Option[Long],
                             path: String, rmse: Double) extends Entity[AlsTraining, Long] {
  override def withId(id: Long): AlsTraining = this.copy(id = Some(id))
}
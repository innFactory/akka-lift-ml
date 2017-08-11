package de.innfactory.akkaliftml.models.db

import com.byteslounge.slickrepo.meta.Keyed
import com.byteslounge.slickrepo.repository.Repository
import de.innfactory.akkaliftml.models.AlsTraining
import slick.ast.BaseTypedType
import slick.jdbc.JdbcProfile

class AlsRepository()(implicit override val driver: JdbcProfile) extends Repository[AlsTraining, Long](driver) {

  import driver.api._

  val pkType = implicitly[BaseTypedType[Long]]
  val tableQuery = TableQuery[AlsTrainings]
  type TableType = AlsTrainings


  class AlsTrainings(tag: slick.lifted.Tag) extends Table[AlsTraining](tag, "alstraining") with Keyed[Long] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def path = column[String]("path")

    def rmse = column[Double]("rmse")

    def * = (id.?, path, rmse) <> ((AlsTraining.apply _).tupled, AlsTraining.unapply)
  }

  def findLatestAlsModel() : DBIO[Seq[AlsTraining]] = {
    tableQuery.sortBy(_.id.desc).take(1).result
  }

}

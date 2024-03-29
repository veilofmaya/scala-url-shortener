package service.models

import play.api.Logger
import service.models._
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object DbUtils {

  lazy val db = Database.forConfig("defaultDb")

  type TQuery = TableQuery[_ <: Table[_]]

  val tables: Map[String, TQuery] = Map(
    ShortUrls.tableName -> ShortUrls.query
  )

  def checkAndCreateTables(implicit ec: ExecutionContext) = {
    Logger.logger.info("Creating non existing tables.")
    def nonExistingTables: Future[Seq[TQuery]] = Future.sequence {
      tables.map { case (tableName, tableQuery) =>
        db.run(MTable.getTables(tableName).headOption.map(t => if(t.isEmpty) Some(tableQuery) else None))
      }.toSeq
    }.map { t: Seq[Option[TQuery]] => t.flatten }

    def create: Future[Any] = nonExistingTables.flatMap {
      case Nil => Future.successful(())
      case tQueries => db.run(DBIO.sequence(tQueries.map(_.schema).map(_.create)).transactionally)
    }
    Await.result(create, Duration.Inf)
    Logger.logger.info("Done creating tables.")
  }
}

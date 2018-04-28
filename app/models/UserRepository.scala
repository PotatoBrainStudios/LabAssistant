package models

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SlickUserRepository])
trait UserRepository{
  def create(name: String, age: Int) : Future[User]
  def list() : Future[Seq[User]]
}
/**
 * A repository for users.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class SlickUserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends UserRepository {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.

  import dbConfig._
  import profile.api._

  /**
   * Here we define the table. It will have a name of users
   */
  private class Users(tag: Tag) extends Table[User](tag, "USERS") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def name = column[String]("name")

    /** The age column */
    def age = column[Int]("age")

    def * = (id, name, age) <> ((User.apply _).tupled, User.unapply)
  }

  /**
   * The starting point for all queries on the users table.
   */
  private val users = TableQuery[Users]

  /**
   * Create a person with the given name and age.
   *
   * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
   * id for that person.
   */
  def create(name: String, age: Int): Future[User] = db.run {
    (users.map(p => (p.name, p.age))
      returning users.map(_.id)
      into ((nameAge, id) => User(id, nameAge._1, nameAge._2))) += (name, age)
  }

  /**
   * List all the users in the database.
   */
  def list(): Future[Seq[User]] = db.run {
    users.filter(_.name < "Madsen").result
  }
}

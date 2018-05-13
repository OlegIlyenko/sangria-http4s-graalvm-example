import language.postfixOps
import cats.effect.IO
import ch.qos.logback.classic.{Level, Logger}
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.blaze.Http1Client
import org.http4s.circe._
import org.http4s.client._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution._
import sangria.execution.deferred.DeferredResolver
import sangria.parser.QueryParser
import sangria.marshalling.circe._
import sangria.introspection.introspectionQuery
import sangria.schema.Schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object GraphQLUtil {
  def executeGraphQL(query: Document, operationName: Option[String], variables: Json) =
    Executor.execute(SchemaDefinition.StarWarsSchema, query, new CharacterRepo,
      variables = if (variables.isNull) Json.obj() else variables,
      operationName = operationName,
      deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters))

  def executeAndPrintGraphQL(query: String) =
    QueryParser.parse(query) match {
      case Success(doc) ⇒
        println(Await.result(executeGraphQL(doc, None, Json.obj()), 10 seconds).spaces2)
      case Failure(error) ⇒
        Console.err.print(error.getMessage())
    }

  def compare(url1: String, url2: String) = {
    val client = Http1Client[IO]().unsafeRunSync
    val schema1 = loadSchema(client, url1)
    val schema2 = loadSchema(client, url2)

    val changes = schema2.compare(schema1)

    val report =
      if (changes.nonEmpty) {
        val rendered = changes
          .map { change ⇒
            val breaking =
              if(change.breakingChange) " (breaking change)"
              else ""

            s" * ${change.description}$breaking"
          }
          .mkString("\n", "\n", "")

        s"Changes: $rendered"
      } else "No Changes"

    println(report)
  }

  def loadSchema(client: Client[IO], url: String) = {
    LoggerFactory.getLogger("org.http4s").asInstanceOf[Logger].setLevel(Level.ERROR) // quick & dirty: disable messy logs :P

    val data = Json.obj(
      "query" → Json.fromString(introspectionQuery.renderCompact),
      "operationName" → Json.fromString("IntrospectionQuery"))

    val res = client.expect[Json](POST(Uri.fromString(url).fold(e ⇒ sys.error(e.message), identity), data)).unsafeRunSync()

    Schema.buildFromIntrospection(res)
  }

  val exceptionHandler = ExceptionHandler {
    case (_, e) ⇒ HandledException(e.getMessage)
  }
}

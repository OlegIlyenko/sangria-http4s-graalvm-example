import cats.effect._
import cats.implicits._
import io.circe.Json
import io.circe.jawn._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import org.http4s.circe._
import io.circe.optics.JsonPath._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, QueryAnalysisError}
import sangria.parser.{QueryParser, SyntaxError}
import sangria.marshalling.circe._

import scala.util.{Failure, Success}

object Http4sServer {
  val service = HttpService[IO] {
    case request @ GET -> Root ⇒
      StaticFile.fromResource("/assets/graphiql.html", Some(request)).getOrElseF(NotFound())

    case request @ POST -> Root / "graphql" ⇒
      request.as[Json].flatMap { body ⇒
        val query = root.query.string.getOption(body)
        val operationName = root.operationName.string.getOption(body)
        val variablesStr = root.variables.string.getOption(body)

        def execute = query.map(QueryParser.parse(_)) match {
          case Some(Success(ast)) ⇒
            variablesStr.map(parse) match {
              case Some(Left(error)) ⇒ Future.successful(BadRequest(formatError(error)))
              case Some(Right(json)) ⇒ executeGraphQL(ast, operationName, json)
              case None ⇒ executeGraphQL(ast, operationName, root.variables.json.getOption(body) getOrElse Json.obj())
            }
          case Some(Failure(error)) ⇒ Future.successful(BadRequest(formatError(error)))
          case None ⇒ Future.successful(BadRequest(formatError("No query to execute")))
        }

        IO.fromFuture(IO(execute)).flatten
      }
  }

  def executeGraphQL(query: Document, operationName: Option[String], variables: Json) =
    GraphQLUtil.executeGraphQL(query, operationName, variables)
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest(error.resolveError)
        case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
      }

  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError ⇒
      Json.obj("errors" → Json.arr(
        Json.obj(
          "message" → Json.fromString(syntaxError.getMessage),
          "locations" → Json.arr(Json.obj(
            "line" → Json.fromBigInt(syntaxError.originalError.position.line),
            "column" → Json.fromBigInt(syntaxError.originalError.position.column))))))
    case NonFatal(e) ⇒
      formatError(e.getMessage)
    case e ⇒
      throw e
  }

  def formatError(message: String): Json =
    Json.obj("errors" → Json.arr(Json.obj("message" → Json.fromString(message))))

  def start() = {
    BlazeBuilder[IO].bindHttp(8080, "0.0.0.0").mountService(service, "/")
      .start.map(Util.awaitServerShutdown).unsafeRunSync()
  }
}

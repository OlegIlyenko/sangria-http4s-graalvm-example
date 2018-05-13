import language.postfixOps
import io.circe.Json
import sangria.ast.Document
import sangria.execution._
import sangria.execution.deferred.DeferredResolver
import sangria.parser.QueryParser
import sangria.marshalling.circe._

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

  val exceptionHandler = ExceptionHandler {
    case (_, e) ⇒ HandledException(e.getMessage)
  }
}

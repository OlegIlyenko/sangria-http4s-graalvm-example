object Main {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      usage
    else
      args(0) match {
        case "server" ⇒
          Http4sServer.start()

        case "query" if args.size == 2 ⇒
          GraphQLUtil.executeAndPrintGraphQL(args(1))

        case _ ⇒
          usage
      }
  }

  def usage = {
    println(
      """Usage: app command [arguments..]
        |
        |Following commands are available:
        |
        |  server                Start the GraphQL server
        |  query <GRAPHQL_QUERY> Execute provided query and exit
        |""".stripMargin)

    sys.exit(1)
  }
}
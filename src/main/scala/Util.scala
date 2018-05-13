import cats.effect.IO
import org.http4s.server.Server

object Util {
  def awaitServerShutdown(server: Server[IO]): Unit = {
    // proper ctrl-c handling
    try sun.misc.Signal.handle(new sun.misc.Signal("INT"), _ ⇒ server.shutdownNow()) catch {
      case e: IllegalArgumentException ⇒ // it's ok, just ignore
    }

    server.awaitShutdown()
  }
}
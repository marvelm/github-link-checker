package code.util

import net.liftweb.actor.LAFuture
import scala.concurrent.{ExecutionContext, Future}
import net.liftweb.common.{Empty, Full, Failure}

object Util {
  implicit def scalaFutureToLAFuture[T](scf: Future[T])(implicit ec: ExecutionContext): LAFuture[T] = {
    val laf = new LAFuture[T]()
    scf.onSuccess{
      case v: T => laf.satisfy(v)
      case _ => laf.abort()
    }

    scf.onFailure {
      case e: Throwable => laf.fail(Failure(e.getMessage, Full(e), Empty))
    }

    laf
  }
}

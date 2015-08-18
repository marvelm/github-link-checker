package code.util

import net.liftweb.actor.LAFuture
import net.liftweb.common.{ Box, Empty, Full, Failure }
import net.liftweb.util.Helpers._
import net.liftweb.util.CanBind
import net.liftweb.http.js.{ JE, JsCmd }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.S
import net.liftweb.http.SHtml

import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.{ Elem, NodeSeq }

object Util {
  implicit def scalaFutureToLAFuture[T](scf: Future[T])(implicit ec: ExecutionContext): LAFuture[T] = {
    val laf = new LAFuture[T]()
    scf.onSuccess {
      case v: T => laf.satisfy(v)
      case _ => laf.abort()
    }

    scf.onFailure {
      case t: Throwable => laf.fail(Failure(t.getMessage, Full(t), Empty))
      case e: Exception => laf.fail(e)
    }

    laf
  }

  /* 'FutureIsHere' and 'laFutureNSTTransform' are from Diego Medina's (fmpwizard) blog post.
   https://fmpwizard.telegr.am/blog/async-snippets-in-lift
   */

  case class FutureIsHere(la: LAFuture[NodeSeq], id: String) extends JsCmd {
    val updatePage: JsCmd =
      if (la.isSatisfied)
        Replace(id, la.get)
      else
        tryAgain()

    private def tryAgain(): JsCmd = {
      val funcName: String = S.request.flatMap(_._params.toList.headOption.map(_._1)).openOr("")
      val retry = "setTimeout(function(){liftAjax.lift_ajaxHandler('%s=true', null, null, null)}, 3000)".format(funcName)
      JE.JsRaw(retry).cmd
    }

    override val toJsCmd = updatePage.toJsCmd
  }

  implicit def laFutureNSTransform: CanBind[LAFuture[NodeSeq]] = new CanBind[LAFuture[NodeSeq]] {
    def apply(future: => LAFuture[NodeSeq])(ns: NodeSeq): Seq[NodeSeq] = {
      val elem: Option[Elem] = ns match {
        case e: Elem => Some(e)
        case nodeSeq if nodeSeq.length == 1 && nodeSeq(0).isInstanceOf[Elem] => Box.asA[Elem](nodeSeq(0))
        case nodeSeq => None
      }

      val id: String = elem.map(_.attributes.filter(att => att.key == "id")).map { meta =>
        tryo(meta.value.text).getOrElse(nextFuncName)
      } getOrElse("")

      val ret: Option[NodeSeq] = ns.toList match {
        case head :: tail => {
          elem.map ( e =>
            e % ("id" -> id) ++ tail ++ Script(OnLoad(SHtml.ajaxInvoke(() => FutureIsHere(future, id)).exp.cmd))
          )
        }

        case empty => None
      }

      ret getOrElse NodeSeq.Empty
    }
  }
}

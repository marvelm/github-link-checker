package code.snippet

import net.liftweb.util.Helpers.{
  intToTimeSpan => _,
  intToTimeSpanBuilder => _,
  _
}
import net.liftweb.http.S
import net.liftweb.common.{ Box, Full, Empty }
import net.liftweb.actor.LAFuture

import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure

import code.checker.{ Checker, Repository }
import code.util.Util._

object CheckerSnippet {
  val VALID = "valid"
  val INVALID = "invalid"
  val CHECKING = "checking"

  def asyncCheckRepo(repo: Repository): LAFuture[NodeSeq] = {
    S.set(repo.toString, CHECKING)
    val laf = new LAFuture[NodeSeq]
    val scf = Checker.getRepo(repo)
    scf.onSuccess {
      case _ =>
        S.set(repo.toString, VALID)
        laf.satisfy(<div>Repository({ repo.toString() }) is valid</div>)
    }
    scf.onFailure {
      case _ =>
        S.set(repo.toString, INVALID)
        laf.satisfy(<div>Repository({ repo.toString() }) is invalid</div>)
    }
    laf
  }

  def error(n: NodeSeq) = {
    "#error" #>
      <div id="error">
        { n }
        <div>
          Redirecting in 1 second.
          <a href="/">Click here if you are not redirected.</a>
        </div>
        <script>
          setTimeout(function() {{
        window.location.href = "/";
      }}, 1000);
        </script>
      </div>
  }

  def render = S.param("repo") match {
    case Full(repoName) =>
      val parts = repoName.split("/")
      if (parts.length == 2) {
        val owner = parts(0)
        val name = parts(1)
        val repo = Repository(owner, name)

        Await.ready(Checker.getReadmeLinks(repo), 10.seconds).value.get match {
          case Success(links) =>
            Await.ready(Checker.checkLinks(links), 30.seconds).value.get match {
              case Success(links) =>
                val brokenLinks = links.filter(!_.valid)
                "#links" #>
                  <div id="links">
                  {for(link <- brokenLinks) yield
                     <div>{link.url.toString}</div>}
                  </div>
            }

          case Failure(e) =>
            "#error" #> <div>Failed to retrieve links</div>
        }

      } else
        error(
          <div>
            The repository should be of the format: owner/repository.
            For example: marvelm/github-link-checker.
          </div>
        )
    case Empty =>
      error(
        <div>
          You must specify a repository.
        </div>
      )
  }
}

package code.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.common.{ Box, Full, Empty }
import net.liftweb.actor.LAFuture

import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

import code.checker.{ Checker, Repository }
import code.util.Util._

object CheckerSnippet {
  val VALID = "valid"
  val INVALID = "invalid"
  def asyncCheckRepo(repo: Repository): LAFuture[NodeSeq] = {
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
        ".error" #> asyncCheckRepo(repo)
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

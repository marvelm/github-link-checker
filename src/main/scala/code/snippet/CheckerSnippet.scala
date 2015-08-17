package code.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.common.{Box, Full, Empty}
import scala.xml.NodeSeq

object CheckerSnippet {
  def error(n: NodeSeq): NodeSeq = {
    <div>
      {n}
      <div>
       <a href="/">Click here to try again</a>
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
      } else {
        error(
          <div>
            The repository should be of the format: owner/repository.
            For example: marvelm/github-link-checker.
            </div>
        )
      }
    case Empty =>
      error(
        <div>
          You must specify a repository.
          </div>
      )
  }
}

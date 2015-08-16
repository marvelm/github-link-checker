package code.checker

import net.liftweb.common.{Box, Full, Empty}
import net.liftweb.json._
import dispatch._, Defaults._
import org.jsoup.Jsoup
import io.mola.galimatias.URL
import scala.collection.JavaConversions._

case class Repository(owner: String, name: String)

object Checker {
  private val github = host("api.github.com").secure

  /**
    Generates a request to "https://api.github.com/repos/:owner/:repo"
    */
  private def repos(repo: Repository) = github / "repos" / repo.owner / repo.name

  /**
    getRepo should be used for checking if a repository exists.
    It returns a representation of the JSON reponse from the GitHub API.
    https://developer.github.com/v3/repos/#get
    */
  def getRepo(repo: Repository): scala.concurrent.Future[JValue] = {
    val svc = repos(repo)
    Http(svc OK as.String).map(parse)
  }

  private def getReadmeLinks(repo: Repository): Future[Set[URL]] = {
    val readmeSvc = repos(repo) / "readme"

    for {
      readmeResp <- Http(readmeSvc OK as.String)
      readmejs = parse(readmeResp)
      JString(readmeLoc) = readmejs \ "_links" \ "html"
      readmeHtml <- Http(url(readmeLoc) OK as.String)
    } yield {
      val doc = Jsoup.parse(readmeHtml)
      val links = doc.select("article.markdown-body.entry-content").select("a")
      val readmeUrl = URL.parse(readmeLoc)
      links.map { link =>
        readmeUrl.resolve(link.attr("href"))
      }.toSet
    }
  }
}

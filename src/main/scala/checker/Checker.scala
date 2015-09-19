package checker

import org.scalajs.dom
import org.scalajs.dom.{html, document}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class Link(url: String, page: String, linkText: String, broken: Boolean)

case class Repo(owner: String, name: String) {
  val githubUrl = s"http://github.com/$owner/$name"
  val ghPagesUrl = s"http://$owner.github.io/$name"
}

@JSExport
object Checker extends js.JSApp {
  @JSExport
  val display = document.getElementById("display").asInstanceOf[html.Div]

  @JSExport
  val input = document.getElementById("input").asInstanceOf[html.Input]

  @JSExport
  def main() {
    println("Hello world")
  }

  @JSExport
  def inputChange() {
    val repo = {
      val arr = input.value.split("/")
      Repo(arr(0), arr(1))
    }
    checkRepository(repo)
  }

  private def checkRepository(repo: Repo) = {
    for {
      rootDoc <- Ajax.get(repo.githubUrl).map(_.responseXML)
      readmeLinks <- getReadmeLinks(repo, rootDoc)
    } {
      display.innerHTML = ""
      for (link <- readmeLinks) {
        val div = document.createElement("div")
        div.innerHTML = link.toString
        display.appendChild(div)
      }
    }
  }

  private implicit def nodeList2Seq(nl: NodeList): IndexedSeq[Node] =
    for (i <- 0 to nl.length) yield nl(i)

  private def getLinks(root: String, el: Element) = {
    val links = for (a <- el.getElementsByTagName("a")) yield {
      val href = a.attributes.getNamedItem("href")
      val checkedLinks = Ajax.get(href.value).map { req =>
        val broken = req.status.toString.startsWith("4")
        Link(href.value, root, a.textContent, broken)
      }
      checkedLinks
    }
    Future.sequence(links)
  }

  private def getReadmeLinks(repo: Repo, doc: Document) = {
    val readme = doc.getElementById("readme")
    getLinks(repo.githubUrl, readme)
  }
}
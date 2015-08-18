package code.comet

import net.liftweb.http.{ CometActor, CometListener, ListenerManager }
import net.liftweb.actor.LiftActor
import code.checker.CheckedLink

class CheckerComet(name: String) extends CometActor with CometListener {
  private var links = Vector[CheckedLink]()

  def registerWith = RunChecker()

  override def lowPriority = {
    case links: Vector[CheckedLink] =>
      this.links = links
  }

  def render = {
    <div>
      {
        for (link <- links) yield {
          <div>
            { link.toString() }
          </div>
        }
      }
    </div>
  }
}

class RunChecker extends LiftActor with ListenerManager {
  private var links = Vector[CheckedLink]()

  def createUpdate = links

  override def lowPriority = {
    case link: CheckedLink =>
      links :+= link
      updateListeners()
  }
}

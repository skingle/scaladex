package ch.epfl.scala.index
package server
package routes

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.model.Uri._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import ch.epfl.scala.index.model.misc.UserState
import ch.epfl.scala.index.server.TwirlSupport._
import ch.epfl.scala.index.views.search.html.searchresult
import ch.epfl.scala.search.Page
import ch.epfl.scala.search.SearchParams
import ch.epfl.scala.services.SearchEngine
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

class SearchPages(production: Boolean, searchEngine: SearchEngine, session: GithubUserSession)(implicit ec: ExecutionContext) {
  import session.implicits._

  private def search(params: SearchParams, user: Option[UserState], uri: String) =
    complete {
      val resultsF = searchEngine.find(params)
      val topicsF = searchEngine.getTopics(params)
      val platformTypesF = searchEngine.getPlatformTypes(params)
      val scalaVersionsF = searchEngine.getScalaVersions(params)
      val scalaJsVersionsF = searchEngine.getScalaJsVersions(params)
      val scalaNativeVersionsF = searchEngine.getScalaNativeVersions(params)
      val sbtVersionsF = searchEngine.getSbtVersions(params)

      for {
        Page(pagination, projects) <- resultsF
        topics <- topicsF
        targetTypes <- platformTypesF
        scalaVersions <- scalaVersionsF
        scalaJsVersions <- scalaJsVersionsF
        scalaNativeVersions <- scalaNativeVersionsF
        sbtVersions <- sbtVersionsF
      } yield searchresult(
        production,
        params,
        uri,
        pagination,
        projects,
        user,
        params.userRepos.nonEmpty,
        topics,
        targetTypes,
        scalaVersions,
        scalaJsVersions,
        scalaNativeVersions,
        sbtVersions
      )
    }

  private val searchPath = "search"

  val routes: Route =
    get(
      concat(
        path(searchPath)(
          optionalSession(refreshable, usingCookies) { userId =>
            val user = session.getUser(userId)
            searchParams(user)(params => search(params, user, searchPath))
          }
        ),
        path(Segment)(organization =>
          optionalSession(refreshable, usingCookies) { userId =>
            val user = session.getUser(userId)
            searchParams(user)(params =>
              search(
                params.copy(
                  queryString = s"${params.queryString} AND organization:$organization"
                ),
                user,
                organization
              )
            )
          }
        )
      )
    )
}

package ch.epfl.scala.index
package server
package routes

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.http.scaladsl.model.Uri._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import ch.epfl.scala.index.model._
import ch.epfl.scala.index.model.misc._
import ch.epfl.scala.index.model.release._
import ch.epfl.scala.index.newModel.NewProject
import ch.epfl.scala.index.newModel.NewRelease
import ch.epfl.scala.index.server.TwirlSupport._
import ch.epfl.scala.services.LocalStorageApi
import ch.epfl.scala.services.WebDatabase
import ch.epfl.scala.services.storage.DataPaths
import ch.epfl.scala.utils.ScalaExtensions._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.typesafe.scalalogging.LazyLogging
import play.twirl.api.HtmlFormat

class ProjectPages(
    production: Boolean,
    db: WebDatabase,
    localStorage: LocalStorageApi,
    session: GithubUserSession,
    paths: DataPaths,
    env: Env
)(implicit executionContext: ExecutionContext)
    extends LazyLogging {
  import session.implicits._

  private def getEditPage(
      projectRef: NewProject.Reference,
      userInfo: UserState
  ): Future[(StatusCode, HtmlFormat.Appendable)] =
    for {
      projectOpt <- db.findProject(projectRef)
      releases <- db.findReleases(projectRef)
    } yield projectOpt
      .map { p =>
        val page = views.project.html.editproject(production, p, releases, Some(userInfo))
        (StatusCodes.OK, page)
      }
      .getOrElse((StatusCodes.NotFound, views.html.notfound(production, Some(userInfo))))

  private def filterVersions(
      p: NewProject,
      allVersions: Seq[SemanticVersion]
  ): Seq[SemanticVersion] =
    (if (p.dataForm.strictVersions) allVersions.filter(_.isSemantic)
     else allVersions).distinct.sorted.reverse

  private def getProjectPage(
      organization: NewProject.Organization,
      repository: NewProject.Repository,
      target: Option[String],
      artifact: NewRelease.ArtifactName,
      version: Option[SemanticVersion],
      user: Option[UserState]
  ): Future[(StatusCode, HtmlFormat.Appendable)] = {
    val selection = ReleaseSelection.parse(
      platform = target,
      artifactName = Some(artifact),
      version = version.map(_.toString),
      selected = None
    )
    val projectRef =
      NewProject.Reference(organization, repository)

    db.findProject(projectRef).flatMap {
      case Some(project) =>
        for {
          releases <- db.findReleases(projectRef)
          // the selected Release
          selectedRelease <- selection
            .filterReleases(releases, project)
            .headOption
            .toFuture(new Exception(s"no release found for $projectRef"))
          directDependencies <- db.findDirectDependencies(selectedRelease)
          reverseDependency <- db.findReverseDependencies(selectedRelease)
          // compute stuff
          allVersions = releases.map(_.version)
          filteredVersions = filterVersions(project, allVersions)
          platforms = releases.map(_.platform).distinct.sorted.reverse
          artifactNames = releases.map(_.artifactName).distinct.sortBy(_.value)
          twitterCard = project.twitterSummaryCard
        } yield (
          StatusCodes.OK,
          views.project.html.project(
            production,
            env,
            project,
            artifactNames,
            filteredVersions,
            platforms,
            selectedRelease,
            user,
            canEdit = true,
            Some(twitterCard),
            releases.size,
            directDependencies,
            reverseDependency
          )
        )
      case None =>
        Future.successful((StatusCodes.NotFound, views.html.notfound(production, user)))
    }
  }

  val routes: Route =
    concat(
      post(
        path("edit" / organizationM / repositoryM)((organization, repository) =>
          optionalSession(refreshable, usingCookies)(_ =>
            pathEnd(
              editForm { form =>
                val ref = NewProject.Reference(organization, repository)
                val updated = for {
                  _ <- localStorage.saveDataForm(ref, form)
                  updated <- db.updateProjectForm(ref, form)
                } yield updated
                onComplete(updated) {
                  case Success(()) =>
                    redirect(
                      Uri(s"/$organization/$repository"),
                      StatusCodes.SeeOther
                    )
                  case Failure(e) =>
                    println(s"error sorry ${e.getMessage()}")
                    redirect(
                      Uri(s"/$organization/$repository"),
                      StatusCodes.SeeOther
                    ) // maybe we can print that it wasn't saved
                }
              }
            )
          )
        )
      ),
      get {
        path("artifacts" / organizationM / repositoryM)((org, repo) =>
          optionalSession(refreshable, usingCookies) { userId =>
            val user = session.getUser(userId)
            val ref = NewProject.Reference(org, repo)
            val res =
              for {
                projectOpt <- db.findProject(ref)
                project <- projectOpt.toFuture(
                  new Exception(s"project ${ref} not found")
                )
                releases <- db.findReleases(project.reference)
                // some computation
                targetTypesWithScalaVersion = releases
                  .groupBy(_.platform.platformType)
                  .map {
                    case (targetType, releases) =>
                      (
                        targetType,
                        releases
                          .map(_.fullPlatformVersion)
                          .distinct
                          .sorted
                          .reverse
                      )
                  }
                artifactsWithVersions = releases
                  .groupBy(_.version)
                  .map {
                    case (semanticVersion, releases) =>
                      (
                        semanticVersion,
                        releases.groupBy(_.artifactName).map {
                          case (artifactName, releases) =>
                            (
                              artifactName,
                              releases.map(r => (r, r.fullPlatformVersion))
                            )
                        }
                      )
                  }
                  .toSeq
                  .sortBy(_._1)
                  .reverse
              } yield (
                project,
                targetTypesWithScalaVersion,
                artifactsWithVersions
              )

            onComplete(res) {
              case Success(
                    (
                      project,
                      targetTypesWithScalaVersion,
                      artifactsWithVersions
                    )
                  ) =>
                complete(
                  views.html.artifacts(
                    production,
                    project,
                    user,
                    targetTypesWithScalaVersion,
                    artifactsWithVersions
                  )
                )
              case Failure(e) =>
                complete(StatusCodes.NotFound, views.html.notfound(production, user))

            }
          }
        )
      },
      get {
        path("edit" / organizationM / repositoryM)((organization, repository) =>
          optionalSession(refreshable, usingCookies)(userId =>
            pathEnd {
              val projectRef =
                NewProject.Reference(organization, repository)
              session.getUser(userId) match {
                case Some(userState) if userState.canEdit(projectRef) =>
                  complete(getEditPage(projectRef, userState))
                case maybeUser =>
                  complete(
                    (
                      StatusCodes.Forbidden,
                      views.html.forbidden(production, maybeUser)
                    )
                  )
              }
            }
          )
        )
      },
      get {
        path(organizationM / repositoryM)((organization, repository) =>
          optionalSession(refreshable, usingCookies)(userId =>
            parameters(("artifact".?, "version".?, "target".?, "selected".?)) { (artifact, version, target, selected) =>
              val projectRef = NewProject.Reference(organization, repository)
              val fut: Future[StandardRoute] = db.findProject(projectRef).flatMap {
                case Some(NewProject(_, _, _, GithubStatus.Moved(_, newOrg, newRepo), _, _)) =>
                  Future.successful(redirect(Uri(s"/$newOrg/$newRepo"), StatusCodes.PermanentRedirect))
                case Some(project) =>
                  val releaseFut: Future[StandardRoute] =
                    getSelectedRelease(
                      db,
                      project,
                      platform = target,
                      artifact = artifact.map(NewRelease.ArtifactName.apply),
                      version = version,
                      selected = selected
                    ).map(_.map { release =>
                      val targetParam = s"?target=${release.platform.encode}"
                      redirect(
                        s"/$organization/$repository/${release.artifactName}/${release.version}/$targetParam",
                        StatusCodes.TemporaryRedirect
                      )
                    }.getOrElse(complete(StatusCodes.NotFound, views.html.notfound(production, session.getUser(userId)))))
                  releaseFut
                case None =>
                  Future.successful(complete(StatusCodes.NotFound, views.html.notfound(production, session.getUser(userId))))
              }

              onSuccess(fut)(identity)
            }
          )
        )
      },
      get {
        path(organizationM / repositoryM / artifactM)((organization, repository, artifact) =>
          optionalSession(refreshable, usingCookies)(userId =>
            parameter("target".?) { target =>
              val user = session.getUser(userId)
              val res = getProjectPage(
                organization,
                repository,
                target,
                artifact,
                None,
                user
              )
              onComplete(res) {
                case Success((code, some)) => complete(code, some)
                case Failure(e) =>
                  complete(StatusCodes.NotFound, views.html.notfound(production, user))
              }
            }
          )
        )
      },
      get {
        path(organizationM / repositoryM / artifactM / versionM)((organization, repository, artifact, version) =>
          optionalSession(refreshable, usingCookies) { userId =>
            parameter("target".?) { target =>
              val user = session.getUser(userId)
              val res = getProjectPage(
                organization,
                repository,
                target,
                artifact,
                Some(version),
                user
              )
              onComplete(res) {
                case Success((code, some)) => complete(code, some)
                case Failure(e) =>
                  complete(StatusCodes.NotFound, views.html.notfound(production, user))
              }
            }
          }
        )
      }
    )
}

package scaladex.server.route.api

import endpoints4s.openapi
import endpoints4s.openapi.model.Info
import endpoints4s.openapi.model.OpenApi
import scaladex.core.api.Endpoints

/**
 * Documentation of the public HTTP API of Scaladex
 */
object ApiDocumentation extends Endpoints with openapi.Endpoints with openapi.JsonEntitiesFromSchemas {
  val api: OpenApi = openApi(Info(title = "Scaladex API", version = "0.1.0"))(
    listProjects,
    listProjectArtifacts,
    getArtifact,
    autocomplete
  )
}

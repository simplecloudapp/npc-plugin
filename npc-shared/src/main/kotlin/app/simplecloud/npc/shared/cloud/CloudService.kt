package app.simplecloud.npc.shared.cloud

import app.simplecloud.api.CloudApi

/**
 * @author Niklas Nieberler
 */

object CloudService {

    val cloudApi = CloudApi.create()

    val eventHandler = CloudEventHandler(cloudApi)

}
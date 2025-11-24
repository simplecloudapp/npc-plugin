package app.simplecloud.npc.shared.controller

import app.simplecloud.api.CloudApi

/**
 * @author Niklas Nieberler
 */

object ControllerService {

    val cloudApi = CloudApi.create()

    val eventHandler = ControllerEventHandler(cloudApi)

}
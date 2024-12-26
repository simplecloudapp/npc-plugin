package app.simplecloud.npc.shared.controller

import app.simplecloud.controller.api.ControllerApi

/**
 * @author Niklas Nieberler
 */

object ControllerService {

    val controllerApi = ControllerApi.createCoroutineApi()

    val eventHandler = ControllerEventHandler(controllerApi)

}
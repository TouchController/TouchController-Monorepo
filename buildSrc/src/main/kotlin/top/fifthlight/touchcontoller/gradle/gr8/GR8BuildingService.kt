package top.fifthlight.touchcontoller.gradle.gr8

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class GR8BuildingService: BuildService<BuildServiceParameters.None>, AutoCloseable {
    override fun close() {}
}

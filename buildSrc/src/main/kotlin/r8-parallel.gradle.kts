import com.gradleup.gr8.Gr8Task
import top.fifthlight.touchcontoller.gradle.gr8.GR8BuildingService

val gr8BuildingService = project.gradle.sharedServices.registerIfAbsent("gr8", GR8BuildingService::class.java) {
    maxParallelUsages = 1
}

tasks.withType<Gr8Task> {
    usesService(gr8BuildingService)
}

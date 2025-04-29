import com.gradleup.gr8.Gr8Task
import top.fifthlight.touchcontoller.gradle.service.GR8BuildingService

val maxR8Tasks = System.getenv("MAX_R8_TASKS")?.toIntOrNull() ?: 2

val gr8BuildingService = project.gradle.sharedServices.registerIfAbsent("gr8", GR8BuildingService::class.java) {
    maxParallelUsages = maxR8Tasks
}

tasks.withType<Gr8Task> {
    usesService(gr8BuildingService)
}

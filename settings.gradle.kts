rootProject.name = "aliucord-plugins"
include(":FirstMessage")
include(":SplitMessages")
includeNoCI(":OpenDebug")

fun includeNoCI(vararg projectPaths: String?) {
    if (System.getenv("CI") != "true") include(*projectPaths)
}

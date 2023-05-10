package com.soywiz.korge.intellij.deps

import com.soywiz.korge.intellij.KorgeProjectExt
import com.soywiz.korge.intellij.internal.Yaml
import com.soywiz.korge.intellij.internal.dyn
import org.yaml.snakeyaml.DumperOptions

object DepsKProjectYml {
    val syaml = org.yaml.snakeyaml.Yaml(DumperOptions().also {
        it.isPrettyFlow = true
        it.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    })


    fun extractDeps(yaml: String): List<String> {
        if (yaml.contains("!!")) error("Can't include '!!' in the file for security reasons")
        val info = syaml.load<Map<String, Any?>?>(yaml)
        return (info?.get("dependencies") as? List<String>?)?.map { it.toString() } ?: emptyList()
        //val model = NewKProjectModel.loadFile(MemoryFileRef("deps.kproject.yml", yaml.toByteArray(Charsets.UTF_8)))
        //model.dependencies.map { it.toString() }
        //if (yaml.contains("!!")) error("Can't include '!!' in the file for security reasons")
        /*
        val info = Yaml.decode(yaml).dyn
        if (!info.contains("dependencies")) info["dependencies"] = mutableMapOf<String, String>()
        val dependencies = info["dependencies"].list.map { it.str }
        println("YAML: $yaml")
        println("YAML.info: $info")
        println("YAML.dependencies: $dependencies")
        return dependencies
        */
    }

    fun addDep(yaml: String, dep: String, removeUrl: String?): String {
        val info = syaml.load<MutableMap<String, Any?>?>(yaml) ?: mutableMapOf()
        if (info["dependencies"] !is MutableList<*>) {
            info["dependencies"] = mutableListOf<String>()
        }
        val deps = (info["dependencies"] as MutableList<String>)
        deps.removeIf { it == removeUrl }
        deps.add(dep)
        return syaml.dump(info)
    }

    fun createEmpty(): String = """
        dependencies:
    """.trimIndent()
}

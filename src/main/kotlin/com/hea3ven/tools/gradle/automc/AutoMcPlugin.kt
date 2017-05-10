package com.hea3ven.tools.gradle.automc

import net.minecraftforge.gradle.user.TaskSourceCopy
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class AutoMcPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		val ext = project.findProperty("ext") as ExtraPropertiesExtension
		val mc = project.findProperty("minecraft") as ForgeExtension

		val projectName = ext.get("project_name") as String
		val projectVersion = ext.get("project_version") as String
		val projectGroup = ext.get("project_group") as String
		val projectCoremod = when {
			ext.has("project_coremod") -> ext.get("project_coremod") as String
			else -> null
		}
		val projectPropertiesFile = when {
			ext.has("project_prop_file") -> ext.get("project_prop_file")
			else -> null
		} as String?
		val versionMc = ext.get("version_mc") as String
		val versionForge = ext.get("version_forge") as String
		val versionForgeBranch = when {
			ext.has("version_forge_branch") -> ext.get("version_forge_branch")
			else -> null
		}
		val versionMappings = ext.get("version_mappings") as String
//		if (projectVersion == null || versionMc == null || versionForge == null)
//			return
		project.version = versionMc + "-" + projectVersion
		project.group = projectGroup
		project.setProperty("archivesBaseName", projectName)
		project.setProperty("sourceCompatibility", "1.8")
		project.setProperty("targetCompatibility", "1.8")
		var fullMcVersion = versionMc + "-" + versionForge
		if (versionForgeBranch != null)
			fullMcVersion += "-" + versionForgeBranch
		mc.version = fullMcVersion
		mc.mappings = versionMappings
		mc.runDir = "run"
		if (projectCoremod != null) {
			mc.coreMod = projectCoremod
			(project.findProperty("jar") as Jar).manifest.attributes["FMLCorePluginContainsFMLMod"] = "true"
		}
		if (projectPropertiesFile != null) {
			mc.replaceIn(projectPropertiesFile)
			mc.replace("PROJECTVERSION", project.version)
			mc.replace("MCVERSION", versionMc)
			mc.replace("FORGEVERSION", versionForge)
			mc.replace("FORGEDEPENDENCY", "required-after:Forge@[$versionForge,)")
		}

		if (project.plugins.hasPlugin("kotlin")) {
			System.out.println("has kotlin")
			val java = project.convention.plugins.get("java") as JavaPluginConvention
			for (set in java.sourceSets) {
				val taskName = "source" + set.name.capitalize() + "Kotlin"
				val task = project.tasks.create(taskName, TaskSourceCopy::class.java)
				val langSet = (set as HasConvention).convention.plugins["kotlin"] as KotlinSourceSet
				task.setSource(langSet.kotlin)
				val dir = File(project.buildDir, "sources/" + set.name + "/kotlin")
				task.setOutput(dir)
				val compileTask = project.tasks.getByName(set.getCompileTaskName("kotlin")) as KotlinCompile
				compileTask.dependsOn(task)
				compileTask.setSource(dir)
			}
		}
	}
}
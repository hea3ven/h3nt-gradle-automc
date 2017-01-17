package com.hea3ven.tools.gradle.automc

import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.*
import org.junit.Test

class AutoMcPluginTest {
	val defaultProps = mapOf(
			"project_name" to "TestMod",
			"project_version" to "1.2.3",
			"project_group" to "com.test.mod",
			"project_prop_file" to "src/main/java/ProjectProperties.java",
			"version_mc" to "1.10.2",
			"version_forge" to "12.18.1.2025",
			"version_mappings" to "snapshot_20160419"
	)

	@Test
	fun setsProjectVersion() {
		val proj = createProject()

		assertEquals("1.10.2-1.2.3", proj.version)
	}

	@Test
	fun setsProjectGroup() {
		val proj = createProject()

		assertEquals("com.test.mod", proj.group)
	}

	@Test
	fun setsProjectArchivesBaseName() {
		val proj = createProject()

		assertEquals("TestMod", proj.getProp<String>("archivesBaseName"))
	}

	@Test
	fun setsProjectJavaVersionTo8() {
		val proj = createProject()

		assertEquals(JavaVersion.VERSION_1_8, proj.getProp<String>("sourceCompatibility"))
		assertEquals(JavaVersion.VERSION_1_8, proj.getProp<String>("targetCompatibility"))
	}

	@Test
	fun setsMinecraftVersion() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("1.10.2", mc.version)
		assertEquals("12.18.1.2025", mc.forgeVersion)
	}

	@Test
	fun setsMinecraftVersionWithBranch() {
		val proj = createProject(mapOf("version_mc" to "1.10",
				"version_forge" to "12.18.0.1994",
				"version_forge_branch" to "1.10.0"))

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("1.10", mc.version)
		assertEquals("12.18.0.1994-1.10.0", mc.forgeVersion)
	}

	@Test
	fun setsMappingsVersion() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("snapshot_20160419", mc.mappings)
	}

	@Test
	fun setsRunDir() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("run", mc.runDir)
	}

	@Test
	fun doesntSetsCoreModByDefault() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals(null, mc.coreMod)
		assertFalse(proj.getProp<Jar>("jar").manifest.attributes.containsKey("FMLCorePluginContainsFMLMod"))
	}

	@Test
	fun setsCoreMod() {
		val proj = createProject(mapOf("project_coremod" to "com.test.mod.LoadingPluginTestMod"))

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("com.test.mod.LoadingPluginTestMod", mc.coreMod)
		assertEquals("true", proj.getProp<Jar>("jar").manifest.attributes["FMLCorePluginContainsFMLMod"])
	}

	@Test(expected = PluginApplicationException::class)
	fun errorWhenMissingProjectName() {
		val props = mutableMapOf(*(defaultProps.map { it.key to it.value }.toTypedArray()))
		props.remove("project_name")
		createProject(props, false)
	}

	@Test(expected = PluginApplicationException::class)
	fun errorWhenMissingProjectVersion() {
		val props = mutableMapOf(*(defaultProps.map { it.key to it.value }.toTypedArray()))
		props.remove("project_version")
		createProject(props, false)
	}

	@Test(expected = PluginApplicationException::class)
	fun errorWhenMissingProjectGroup() {
		val props = mutableMapOf(*(defaultProps.map { it.key to it.value }.toTypedArray()))
		props.remove("project_group")
		createProject(props, false)
	}

	@Test(expected = PluginApplicationException::class)
	fun errorWhenMissingVersionMc() {
		val props = mutableMapOf(*(defaultProps.map { it.key to it.value }.toTypedArray()))
		props.remove("version_mc")
		createProject(props, false)
	}

	@Test(expected = PluginApplicationException::class)
	fun errorWhenMissingVersionForge() {
		val props = mutableMapOf(*(defaultProps.map { it.key to it.value }.toTypedArray()))
		props.remove("version_forge")
		createProject(props, false)
	}

	@Test(expected = PluginApplicationException::class)
	fun errorWhenMissingVersionMappings() {
		val props = mutableMapOf(*(defaultProps.map { it.key to it.value }.toTypedArray()))
		props.remove("version_mappings")
		createProject(props, false)
	}

	@Test
	fun createsCopySourceTaskWhenKotlinPluginIsEnabled() {
		val proj = ProjectBuilder.builder().build()
		val ext = proj.getProp<ExtraPropertiesExtension>("ext")
		for (propName in defaultProps.keys) {
			ext.set(propName, defaultProps[propName])
		}
		proj.buildscript.repositories.mavenCentral()
		proj.buildscript.dependencies.add("classpath",
				DefaultClientModule("org.jetbrains.kotlin", "kotlin-gradle-plugin", "1.0.3"))
		proj.pluginManager.apply("kotlin")
		proj.pluginManager.apply("net.minecraftforge.gradle.forge")
		proj.pluginManager.apply("com.hea3ven.tools.gradle.automc")

		assertNotNull(proj.tasks.getByName("sourceApiKotlin"))
		assertNotNull(proj.tasks.getByName("sourceMainKotlin"))
		assertNotNull(proj.tasks.getByName("sourceTestKotlin"))
	}

	@Test
	fun addsProjectPropertiesFileToBeProcessed() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertTrue(mc.includes.contains("src/main/java/ProjectProperties.java"))
	}

	@Test
	fun addsProjectVersionAsPropertyToBeReplaced() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("1.10.2-1.2.3", mc.replacements.get("PROJECTVERSION"))
	}

	@Test
	fun addsMcVersionAsPropertyToBeReplaced() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("1.10.2", mc.replacements.get("MCVERSION"))
	}

	@Test
	fun addsForgeVersionAsPropertyToBeReplaced() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("12.18.1.2025", mc.replacements.get("FORGEVERSION"))
	}

	@Test
	fun addsForgeDependencyAsPropertyToBeReplaced() {
		val proj = createProject()

		val mc = proj.getProp<ForgeExtension>("minecraft")
		assertEquals("required-after:Forge@[12.18.1.2025,)", mc.replacements.get("FORGEDEPENDENCY"))
	}

	private fun createProject(props: Map<String, String> = emptyMap(), useDefaults: Boolean = true): Project {
		val proj = ProjectBuilder.builder().build()
		val ext = proj.getProp<ExtraPropertiesExtension>("ext")
		for (propName in defaultProps.keys.union(props.keys)) {
			if (props.containsKey(propName) || useDefaults)
				ext.set(propName, props[propName] ?: defaultProps[propName])
		}
		proj.pluginManager.apply("net.minecraftforge.gradle.forge")
		proj.pluginManager.apply("com.hea3ven.tools.gradle.automc")
		return proj
	}

	@Suppress("UNCHECKED_CAST")
	private fun <T> Project.getProp(name: String) = findProperty(name) as T
}

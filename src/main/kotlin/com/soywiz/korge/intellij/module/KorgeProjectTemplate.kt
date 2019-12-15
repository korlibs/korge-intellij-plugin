package com.soywiz.korge.intellij.module

import org.intellij.lang.annotations.*
import javax.xml.bind.*
import javax.xml.bind.annotation.*

@Suppress("unused")
@XmlRootElement(name = "korge-templates")
open class KorgeProjectTemplate {
	@XmlElement(name = "versions")
	val versions = Versions()

	@XmlElement(name = "features")
	val features = Features()

	@XmlElement(name = "files")
	val files = Files()

	class Versions {
		@set:XmlElement(name = "version")
		var versions = arrayListOf<Version>()

		class Version {
			@set:XmlValue
			var text: String = ""
		}
	}

	class Features {
		@set:XmlElement(name = "feature")
		var features = arrayListOf<Feature>()

		class Feature {
			@set:XmlAttribute(name = "id")
			var id: String = ""

			@set:XmlAttribute(name = "dependencies")
			var dependencies: String = ""

			@set:XmlAttribute(name = "name")
			var name: String = ""

			@set:XmlAttribute(name = "description")
			var description: String = ""

			@set:XmlAttribute(name = "documentation")
			var documentation: String = ""
		}
	}

	class Files {
		@set:XmlElement(name = "file")
		var files = arrayListOf<TFile>()

		class TFile {
			@set:XmlAttribute(name = "path")
			var path: String = ""

			@set:XmlValue
			var content: String = ""
		}
	}

	companion object {
		fun fromXml(@Language("XML") xml: String): KorgeProjectTemplate = JAXBContext.newInstance(KorgeProjectTemplate::class.java).createUnmarshaller().unmarshal(xml.reader()) as KorgeProjectTemplate
		fun fromEmbeddedResource(): KorgeProjectTemplate =
			fromXml(KorgeProjectTemplate::class.java.getResource("/com/soywiz/korge/intellij/korge-templates.xml")?.readText() ?: error("Can't find ¡korge-templates.xml' from esources"))
	}
}
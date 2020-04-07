// @TODO: @WARNING: Duplicated from KorGE to be able to modify it. Please, copy again to KorGE once this is stable
package com.soywiz.korge.intellij.editor.tile

import com.soywiz.klogger.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

class TiledMapData(
	var width: Int = 0,
	var height: Int = 0,
	var tilewidth: Int = 0,
	var tileheight: Int = 0,
	val allLayers: MutableList<TiledMap.Layer> = arrayListOf(),
	val tilesets: MutableList<TileSetData> = arrayListOf()
) {
	val maxGid get() = tilesets.map { it.firstgid + it.tilecount }.max() ?: 0
	val pixelWidth: Int get() = width * tilewidth
	val pixelHeight: Int get() = height * tileheight
	inline val tileLayers get() = allLayers.tiles
	inline val imageLayers get() = allLayers.images
	inline val objectLayers get() = allLayers.objects
	fun getObjectByName(name: String) = objectLayers.mapNotNull { it.getByName(name) }.firstOrNull()
	fun clone() = TiledMapData(
		width, height, tilewidth, tileheight,
		allLayers.map { it.clone() }.toMutableList(),
		tilesets.map { it.clone() }.toMutableList()
	)
}

fun TiledMap.Layer.Objects.Object.getPos(map: TiledMapData): IPoint =
	IPoint(bounds.x / map.tilewidth, bounds.y / map.tileheight)

fun TiledMapData?.getObjectPosByName(name: String): IPoint? {
	val obj = this?.getObjectByName(name) ?: return null
	return obj.getPos(this)
}

data class TerrainData(
	val name: String,
	val tile: Int
)

data class AnimationFrameData(
	val tileid: Int, val duration: Int
)

data class TerrainInfo(val info: List<Int?>) {
	operator fun get(x: Int, y: Int): Int? = if (x in 0..1 && y in 0..1) info[y * 2 + x] else null
}

data class TileData(
	val id: Int,
	val terrain: List<Int?>? = null,
	val probability: Double = 1.0,
	val frames: List<AnimationFrameData>? = null
) {
	val terrainInfo = TerrainInfo(terrain ?: listOf(null, null ,null, null))
}

data class TileSetData constructor(
	val name: String = "unknown",
	val firstgid: Int = 1,
	val tilewidth: Int,
	val tileheight: Int,
	val tilecount: Int,
	val columns: Int,
	val image: Xml?,
	val imageSource: String,
	val width: Int,
	val height: Int,
	val tilesetSource: String? = null,
	val terrains: List<TerrainData> = listOf(),
	val tiles: List<TileData> = listOf()
) {
	fun clone() = copy()
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(TiledMapFactory::class)
class TiledMap constructor(
	var data: TiledMapData,
	var tilesets: List<TiledTileset>
) {
	val width get() = data.width
	val height get() = data.height
	val tilewidth get() = data.tilewidth
	val tileheight get() = data.tileheight
	val pixelWidth: Int get() = data.pixelWidth
	val pixelHeight: Int get() = data.pixelHeight
	val allLayers get() = data.allLayers
	val tileLayers get() = data.tileLayers
	val imageLayers get() = data.imageLayers
	val objectLayers get() = data.objectLayers
    val nextGid get() = tilesets.map { it.firstgid + it.tileset.textures.size }.max() ?: 1

	fun clone() = TiledMap(data.clone(), tilesets.map { it.clone() })

	data class TiledTileset(
		val tileset: TileSet,
		val data: TileSetData = TileSetData(
			name = "unknown",
			firstgid = 1,
			tilewidth = tileset.width,
			tileheight = tileset.height,
			tilecount = tileset.textures.size,
			columns = tileset.base.width / tileset.width,
			image = null,
			imageSource = "",
			width = tileset.base.width,
			height = tileset.base.height,
			tilesetSource = null,
			terrains = listOf(),
			tiles = tileset.textures.mapIndexed { index, bmpSlice -> TileData(index) }
		),
		val firstgid: Int = 1
	) {
		fun clone(): TiledTileset = TiledTileset(tileset.clone(), data.clone(), firstgid)
	}

	sealed class Layer {
		var id: Int = 1
		var name: String = ""
		var visible: Boolean = true
		var locked: Boolean = false
		var draworder: String = ""
		var color: RGBA = Colors.WHITE
		var opacity = 1.0
		var offsetx: Double = 0.0
		var offsety: Double = 0.0
		val properties = LinkedHashMap<String, Any>()
		companion object {
			val BASE_PROPS = listOf(
				Layer::id,
				Layer::name, Layer::visible, Layer::locked, Layer::draworder,
				Layer::color, Layer::opacity, Layer::offsetx, Layer::offsety
			)
		}
		open fun copyFrom(other: Layer) {
			for (prop in BASE_PROPS) {
				val p = prop as KMutableProperty1<Layer, Any>
				p.set(this, p.get(other))
			}
			this.properties.clear()
			this.properties.putAll(other.properties)
		}
		abstract fun clone(): Layer

		class Tiles(
			var map: Bitmap32 = Bitmap32(0, 0),
			var encoding: String = "csv",
			var compression: String = ""
		) : Layer() {
			val data get() = map
			val width: Int get() = map.width
			val height: Int get() = map.height
			val area: Int get() = width * height
			operator fun set(x: Int, y: Int, value: Int) = run { data.setInt(x, y, value) }
			operator fun get(x: Int, y: Int): Int = data.getInt(x, y)
			override fun clone(): Tiles = Tiles(map.clone(), encoding, compression).also { it.copyFrom(this) }
		}

		data class ObjectInfo(
			val id: Int, val name: String, val type: String,
			val bounds: IRectangleInt,
			val objprops: Map<String, Any>
		)

		class Objects(
			val objects: MutableList<Object> = arrayListOf<Object>()
		) : Layer() {
			interface Object {
				val info: ObjectInfo
			}

			interface Poly : Object {
				val points: List<IPoint>
			}

			data class Rect(override val info: ObjectInfo) : Object
			data class Ellipse(override val info: ObjectInfo) : Object
			data class Polyline(override val info: ObjectInfo, override val points: List<IPoint>) : Poly
			data class Polygon(override val info: ObjectInfo, override val points: List<IPoint>) : Poly

			override fun clone(): Objects = Objects(objects.toMutableList()).also { it.copyFrom(this) }

			fun getById(id: Int): Object? = objects.firstOrNull { it.id == id }
			fun getByName(name: String): Object? = objects.firstOrNull { it.name == name }
		}

		class Image(
			var width: Int = 0,
			var height: Int = 0,
			var source: String = "",
			var image: Bitmap = Bitmap32(0, 0)
		) : Layer() {
			override fun clone(): Image = Image(width, height, source, image.clone()).also { it.copyFrom(this) }
		}
	}
}

private fun TileSet.clone(): TileSet = TileSet(this.textures, this.width, this.height, this.base)

fun Bitmap.clone(): Bitmap = when (this) {
	is Bitmap32 -> this.clone()
	else -> TODO()
}

val TiledMap.Layer.Objects.Object.id get() = this.info.id
val TiledMap.Layer.Objects.Object.name get() = this.info.name
val TiledMap.Layer.Objects.Object.bounds get() = this.info.bounds
val TiledMap.Layer.Objects.Object.objprops get() = this.info.objprops

inline val Iterable<TiledMap.Layer>.tiles get() = this.filterIsInstance<TiledMap.Layer.Tiles>()
inline val Iterable<TiledMap.Layer>.images get() = this.filterIsInstance<TiledMap.Layer.Image>()
inline val Iterable<TiledMap.Layer>.objects get() = this.filterIsInstance<TiledMap.Layer.Objects>()

val tilemapLog = Logger("tilemap")

class TiledFile(val name: String)

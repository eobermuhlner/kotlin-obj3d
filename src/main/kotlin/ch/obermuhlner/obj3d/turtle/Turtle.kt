package ch.obermuhlner.obj3d.turtle

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class Turtle(
        var center: Vector3 = Vector3(0f, 0f, 0f),
        var upDirection: Vector3 = Vector3.Z,
        var forwardDirection: Vector3 = Vector3.Y,
        var builder: TurtleModelBuilder = TurtleModelBuilder()) {

    private var lastCornerPoints = listOf<MeshPartBuilder.VertexInfo>()
    var corners = mutableListOf<Corner>()
    var sides = mutableListOf<Side>()

    var uvScale = Vector2(1.0f, 1.0f)

    val sideDirection: Vector3
        get() {
            return Vector3(forwardDirection).crs(upDirection).nor()
        }

    private fun angleBetween(vector1: Vector3, vector2: Vector3, planeNormal: Vector3): Float {
        return MathUtils.atan2(Vector3(vector1).crs(vector2).dot(planeNormal), Vector3(vector1).dot(vector2)) * MathUtils.radiansToDegrees
    }

    fun regularPolygon(cornerCount: Int, radius: Float, material: Material) {
        regularPolygon(cornerCount, { _ -> radius}, { _ -> material})
    }

    fun regularPolygon(cornerCount: Int, radiusFunc: (Int) -> Float, materialFunc: (Int) -> Material) {
        polygon(cornerCount, { index -> 360f / cornerCount * index}, radiusFunc, materialFunc)
    }

    fun polygon(material: Material, vararg corners: Vector2) {
        polygon({_ -> material}, *corners)
    }

    fun polygon(materialFunc: (Int) -> Material, vararg corners: Vector2) {
        polygon(corners.size, {index -> corners[index]}, materialFunc)
    }

    fun polygon(cornerCount: Int, vector2Func: (Int) -> Vector2, materialFunc: (Int) -> Material) {
        polygon(cornerCount, {index -> vector2Func(index).angle()}, {index -> vector2Func(index).len()}, materialFunc)
    }

    fun polygon(cornerCount: Int, angleFunc: (Int) -> Float, radiusFunc: (Int) -> Float, materialFunc: (Int) -> Material) {
        corners.clear()
        sides.clear()

        for (i in 0 until cornerCount) {
            corners.add(Corner(radiusFunc(i), angleFunc(i)))
            sides.add(Side(materialFunc(i)))
        }
    }

    private fun polygon(materialFunc: (Int) -> Material, vararg cornerPoints: MeshPartBuilder.VertexInfo) {
        corners.clear()
        sides.clear()

        for (pointIndex in cornerPoints.indices) {
            val point = cornerPoints[pointIndex]
            val radiusVec = Vector3(point.position).sub(center)
            val distance = radiusVec.len()
            val angle = angleBetween(upDirection, radiusVec, forwardDirection)
            corners.add(Corner(distance, angle))
            sides.add(Side(materialFunc(pointIndex)))
        }

        val p = mutableListOf(*cornerPoints)
        val extra = MeshPartBuilder.VertexInfo()
        extra.set(cornerPoints[0])
        p.add(extra)
        lastCornerPoints = p
    }

    var radius: Float
        get() {
            return this.corners.map { it.radius }.max() ?: 0f
        }
        set(value) {
            radius({_, _ -> value})
        }

    fun radius(radiusFunc: (Int, Float) -> Float) {
        for (index in corners.indices) {
            corners[index].radius = radiusFunc(index, corners[index].radius)
        }
    }

    var angle: Float
        get() {
            return corners[0].angle
        }
        set(value) {
            angle({_, _ -> value})
        }

    fun angle(angleFunc: (Int, Float) -> Float) {
        for (index in corners.indices) {
            corners[index].angle = angleFunc(index, corners[index].angle)
        }
    }

    var material: Material
        get() {
            return sides[0].material
        }
        set(value) {
            material({_, _ -> value})
        }

    fun material(materialFunc: (Int, Material) -> Material) {
        for (index in sides.indices) {
            sides[index].material = materialFunc(index, sides[index].material)
        }
    }

    var smooth: Boolean
        get() {
            return sides[0].smooth
        }
        set(value) {
            smooth({_, _ -> value})
        }

    fun smooth(smoothFunc: (Int, Boolean) -> Boolean) {
        for (index in sides.indices) {
            sides[index].smooth = smoothFunc(index, sides[index].smooth)
        }
    }

    fun rotate(angle: Float) {
        upDirection.rotate(forwardDirection, angle)
    }

    fun moveForward(step: Float) {
        val delta = Vector3(forwardDirection).scl(step)
        center.add(delta)
    }

    fun forward(step: Float) {
        val newSize = corners.size
        val lastSize = lastCornerPoints.size - 1

        forward(step, { virtualSideIndex, virtualSideCount ->
            (virtualSideIndex.toFloat() / virtualSideCount * lastSize).toInt()
        }, {virtualSideIndex, virtualSideCount ->
            (virtualSideIndex.toFloat() / virtualSideCount * newSize).toInt()
        })
    }

    fun forward(step: Float, virtualSideToLastCornerIndex: (Int, Int) -> Int, virtualSideToNewCornerIndex: (Int, Int) -> Int) {
        val delta = Vector3(forwardDirection).scl(step)
        center.add(delta)
        var u = 0f

        var lastPosition: Vector3? = null
        val cornerPoints = mutableListOf<MeshPartBuilder.VertexInfo>()
        for (corner in corners) {
            val cornerPoint = MeshPartBuilder.VertexInfo()
            cornerPoint.hasPosition = true
            cornerPoint.hasNormal = true
            cornerPoint.position.set(upDirection).rotate(forwardDirection, corner.angle)
            cornerPoint.normal.set(cornerPoint.position).nor()
            cornerPoint.position.scl(corner.radius).add(center)

            if (lastPosition != null) {
                u += Vector3(cornerPoint.position).sub(lastPosition).len() * uvScale.x
            }
            cornerPoint.uv.x = u
            cornerPoint.hasUV = true

            cornerPoints.add(cornerPoint)

            lastPosition = cornerPoint.position
        }

        // add an extra cornerPoint (same position as cornerPoints[0] but different uv)
        val cornerPoint = MeshPartBuilder.VertexInfo()
        cornerPoint.set(cornerPoints[0])
        if (lastPosition != null) {
            u += Vector3(cornerPoint.position).sub(lastPosition).len() * uvScale.x
        }
        cornerPoint.uv.x = u
        cornerPoint.hasUV = true
        cornerPoints.add(cornerPoint)

        if (lastCornerPoints.isNotEmpty()) {
            val lastSideCount = lastCornerPoints.size - 1
            val sideCount = sides.size
            val virtualSideCount = Math.max(sideCount, lastSideCount)
            for (virtualSideIndex in 0 until virtualSideCount) {
                val lastSideIndex = virtualSideToLastCornerIndex(virtualSideIndex, virtualSideCount)
                val newSideIndex = virtualSideToNewCornerIndex(virtualSideIndex, virtualSideCount)
                val lastSideNextIndex = virtualSideToLastCornerIndex(virtualSideIndex + 1, virtualSideCount)
                val newSideNextIndex = virtualSideToNewCornerIndex(virtualSideIndex + 1, virtualSideCount)

                val side = sides[newSideIndex]
                val part = builder.part(side.material)

                val corner1 = lastCornerPoints[lastSideIndex]
                val corner2 = lastCornerPoints[lastSideNextIndex]
                val corner3 = cornerPoints[newSideNextIndex]
                val corner4 = cornerPoints[newSideIndex]

                corner3.uv.y = corner2.uv.y + Vector3(corner3.position).sub(corner2.position).len() * uvScale.y
                corner4.uv.y = corner1.uv.y + Vector3(corner4.position).sub(corner1.position).len() * uvScale.y

                val subTurtle = side.turtle
                side.turtle = null

                if (subTurtle != null) {
                    val vectorU = Vector3(corner2.position).sub(corner1.position).scl(0.5f)
                    val vectorV = Vector3(corner4.position).sub(corner1.position).scl(0.5f)
                    val center = Vector3(corner1.position).add(vectorU).add(vectorV)
                    val normal = Vector3(vectorU).crs(vectorV).nor()
                    subTurtle.center = center
                    subTurtle.upDirection = vectorU.nor()
                    subTurtle.forwardDirection = normal
                    subTurtle.builder = builder
                    subTurtle.uvScale = uvScale
                    subTurtle.polygon({ _ -> side.material}, corner1, corner2, corner3, corner4)
                    subTurtle.smooth = side.smooth
                } else {
                    if (!side.smooth) {
                        val u = if (corner1 != corner2) Vector3(corner2.position).sub(corner1.position) else Vector3(corner4.position).sub(corner3.position)
                        val v = if (corner1 != corner4) Vector3(corner4.position).sub(corner1.position) else Vector3(corner3.position).sub(corner2.position)
                        corner1.normal.set(u).crs(v).nor()
                        corner2.normal.set(corner1.normal)
                        corner3.normal.set(corner1.normal)
                        corner4.normal.set(corner1.normal)
                    }
                    part.rect(corner1, corner2, corner3, corner4)
                }
            }
        }
        lastCornerPoints = cornerPoints
    }

    fun close() {
        radius = 0f
        forward(0f)
    }

    fun closeSingleSided(sideIndex: Int = 0) {
        val side = sides[sideIndex]
        val part = builder.part(side.material)

        when (lastCornerPoints.size) {
            3 -> part.triangle(lastCornerPoints[0], lastCornerPoints[1], lastCornerPoints[2])
            4 -> part.rect(lastCornerPoints[0], lastCornerPoints[1], lastCornerPoints[2], lastCornerPoints[3])
            else -> {
                // FIXME add polygon
            }
        }
    }

    fun end(): Model {
        return builder.modelBuilder.end()
    }

    private fun debugPoint(pos: Vector3, color: Color = Color.RED, size: Float = 1f) {
        builder.part(Material(ColorAttribute.createDiffuse(color))).box(pos.x, pos.y, pos.z, size, size, size)
    }

    private fun debugVector(pos: Vector3, vector: Vector3, color: Color = Color.BLUE) {
        debugLine(pos, Vector3(pos).add(vector))
    }

    private fun debugLine(pos1: Vector3, pos2: Vector3, color: Color = Color.BLUE) {
        builder.part(Material(ColorAttribute.createDiffuse(color)), GL20.GL_LINES).line(pos1, pos2)
    }
}

class Corner(
        var radius: Float,
        var angle: Float)

class Side(
        var material: Material,
        var smooth: Boolean = false,
        var turtle: Turtle? = null) {

    fun turtle(): Turtle {
        val t = Turtle()
        turtle = t
        return t
    }
}

class TurtleModelBuilder(
        val modelBuilder: ModelBuilder = ModelBuilder(),
        var attributes: Long = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()) {

    init {
        modelBuilder.begin()
    }

    private var partIndex = 0
    private var currentMaterial: Material? = null
    private var currentPart: MeshPartBuilder? = null

    fun part(material: Material): MeshPartBuilder {
        return part(material, GL20.GL_TRIANGLES)
    }

    fun part(material: Material, primitiveType: Int): MeshPartBuilder {
        val oldPart = currentPart
        if (oldPart == null || currentMaterial == null || material != currentMaterial) {
            partIndex++
            val newPart = modelBuilder.part("$partIndex", primitiveType, attributes, material)
            currentPart = newPart
            currentMaterial = material
            return newPart
        }
        return oldPart
    }
}

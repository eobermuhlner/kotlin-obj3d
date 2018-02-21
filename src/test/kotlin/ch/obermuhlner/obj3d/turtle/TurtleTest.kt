package ch.obermuhlner.obj3d.turtle

import com.badlogic.gdx.graphics.g3d.Material
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class TurtleTest : GdxTest() {
    @Test fun empty() {
        val turtle = Turtle()
        turtle.startRegularPolygon(3, 1f, Material())
        turtle.forward(0f)
        turtle.forward(1f)

        val model = turtle.end()

        assertEquals(1, model.meshes.size)

        val vertices = model.meshes[0].getVertices(FloatArray(model.meshes[0].vertexSize))
        println("vertices " + Arrays.toString(vertices))

        val indices = ShortArray(model.meshes[0].numIndices)
        model.meshes[0].getIndices(indices)
        println("indices " + Arrays.toString(indices))
    }
}

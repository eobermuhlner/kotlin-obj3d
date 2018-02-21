package ch.obermuhlner.obj3d.turtle

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import org.junit.BeforeClass
import org.mockito.Mockito

open class GdxTest {
    companion object {
        lateinit var application: Application

        @BeforeClass @JvmStatic fun setup() {
            application = HeadlessApplication(HeadlessApplicationListener())
            Gdx.gl20 = Mockito.mock(GL20::class.java)
            Gdx.gl = Gdx.gl20
        }
    }

    class HeadlessApplicationListener() : ApplicationListener {
        override fun create() {}
        override fun render() {}
        override fun pause() {}
        override fun resume() {}
        override fun resize(width: Int, height: Int) {}
        override fun dispose() {}
    }
}

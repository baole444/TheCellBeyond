package components;

import org.joml.Vector2f;
import render.Texture;

public class Sprite {

    private Texture texture = null;
    private Vector2f[] texCoord = {
                new Vector2f(1, 1),
                new Vector2f(1, 0),
                new Vector2f(0, 0),
                new Vector2f(0, 1)
        };
    //public Sprite(Texture texture) {
    //    this.texture = texture;
    //    Vector2f[] texCoord = {
    //            new Vector2f(1, 1),
    //            new Vector2f(1, 0),
    //            new Vector2f(0, 0),
    //            new Vector2f(0, 1)
    //    };
    //    this.texCoord = texCoord;
    //}
//
    //public Sprite(Texture texture, Vector2f[] texCoord) {
    //    this.texture = texture;
    //    this.texCoord = texCoord;
    //}

    public Texture loadTex() {
        return this.texture;
    }

    public Vector2f[] loadTexCrd() {
        return this.texCoord;
    }

    public void setTex(Texture tex) {
        this.texture = tex;
    }

    public void setTexCrd(Vector2f[] texCrd) {
        this.texCoord = texCrd;
    }

}

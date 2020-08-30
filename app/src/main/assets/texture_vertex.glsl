attribute vec4 aPosition;
attribute vec2 aTexCoordinate;
varying vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoordinate;
    gl_Position = aPosition;
}
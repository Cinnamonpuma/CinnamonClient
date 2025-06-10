#version 120

attribute vec4 Position;
attribute vec4 Color;
varying vec2 texCoord;
varying vec4 vertexColor;

void main() {
    gl_Position = Position;
    vertexColor = Color;
    texCoord = Position.xy * 0.5 + 0.5;
}
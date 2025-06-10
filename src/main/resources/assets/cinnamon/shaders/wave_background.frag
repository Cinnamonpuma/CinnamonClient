#version 120

uniform float time;
uniform vec2 resolution;
varying vec2 texCoord;
varying vec4 vertexColor;

void main() {
    vec2 uv = texCoord;
    
    // Create animated waves
    float wave = sin(uv.x * 10.0 + time) * 0.5 + 0.5;
    wave *= sin(uv.y * 8.0 + time * 0.8) * 0.5 + 0.5;
    
    // Create a gradient background
    vec3 col = mix(
        vec3(0.2, 0.3, 0.8), // Dark blue
        vec3(0.4, 0.6, 1.0), // Light blue
        wave
    );
    
    gl_FragColor = vec4(col * vertexColor.rgb, vertexColor.a);
}
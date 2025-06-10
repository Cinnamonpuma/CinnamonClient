#version 150

in vec2 in_Position;
in vec2 in_TexCoord; // Will be passed from application

out vec2 out_TexCoord;

void main() {
    gl_Position = vec4(in_Position, 0.0, 1.0);
    out_TexCoord = in_TexCoord;
}

#version 150

uniform float time;
uniform vec2 resolution;

in vec2 out_TexCoord; // Received from vertex shader, can be used if texturing

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy; // Normalized screen coordinates (0.0 to 1.0)
    
    float waveSpeed = 2.0;
    float waveFrequency = 10.0;
    float waveAmplitude = 0.02;
    
    // Create a wave pattern using sine and cosine
    float wave = sin(uv.x * waveFrequency + time * waveSpeed) * cos(uv.y * waveFrequency * 0.5 + time * waveSpeed * 0.5);
    
    // Add displacement based on the wave
    float displacement = wave * waveAmplitude;
    
    // Base color (e.g., a blueish tone)
    vec3 baseColor = vec3(0.1, 0.3, 0.7);
    
    // Modify color based on displacement
    vec3 color = baseColor + vec3(displacement, displacement, displacement * 1.5); // Make waves lighter/brighter
    
    // Add a subtle secondary wave for more visual interest
    float wave2Frequency = 5.0;
    float wave2Amplitude = 0.03;
    float wave2 = sin(uv.y * wave2Frequency - time * waveSpeed * 0.7) * cos(uv.x * wave2Frequency * 0.8 - time * waveSpeed * 0.3);
    float displacement2 = wave2 * wave2Amplitude;
    color += vec3(displacement2 * 0.5, displacement2 * 1.2, displacement2);


    // Ensure color values are within [0, 1]
    color = clamp(color, 0.0, 1.0);
    
    fragColor = vec4(color, 1.0);
}

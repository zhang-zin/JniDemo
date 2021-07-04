//base 片元着色器

precision mediump float;

uniform sampler2D vTexture;//采样器

varying vec2 aCoord;

void main(){

    vec4 rgba = texture2D(vTexture, aCoord);
    gl_FragColor = vec4(1.-rgba.r, 1.-rgba.g, 1.-rgba.b, rgba.a);
}
//base 片元着色器

precision mediump float;

uniform sampler2D vTexture;//采样器

varying vec2 aCoord;

void main(){

    gl_FragColor = texture2D(vTexture, aCoord);
}
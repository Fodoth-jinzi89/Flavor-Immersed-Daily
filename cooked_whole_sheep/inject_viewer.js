// 将 bbmodel 数据注入 viewer.html
const fs = require('fs');
const bb = JSON.parse(fs.readFileSync('cooked_whole_sheep/cooked_whole_sheep.bbmodel', 'utf8'));
let html = fs.readFileSync('cooked_whole_sheep/viewer.html', 'utf8');
html = html.replace('__RAW_DATA__', JSON.stringify(bb));
fs.writeFileSync('cooked_whole_sheep/viewer_ready.html', html);
console.log('done:', html.length, 'bytes');

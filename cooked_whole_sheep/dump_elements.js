// 打印 bbmodel 所有元素（含层级归属），并翻转成渲染坐标
const fs = require('fs');
const bb = JSON.parse(fs.readFileSync('cooked_whole_sheep/cooked_whole_sheep.bbmodel', 'utf8'));

// uuid -> name
const uuidToName = {};
for (const el of bb.elements) {
    uuidToName[el.uuid] = el.name || '(unnamed)';
}

function listOutline(items, depth, parentName) {
    for (const it of items || []) {
        const name = uuidToName[it.uuid] || it.name || '(unnamed)';
        console.log(' '.repeat(depth * 2) + `${name}  (parent: ${parentName})`);
        if (it.children && it.children.length) listOutline(it.children, depth + 1, name);
    }
}
console.log('=== OUTLINER with names ===');
listOutline(bb.outliner, 0, 'ROOT');

// 元素表
console.log('\n=== ELEMENTS (raw space, Y-down) ===');
for (const el of bb.elements) {
    const f = el.from, t = el.to;
    const rot = el.rotation || [0, 0, 0];
    const origin = el.origin || ['-'];
    const yc = (f[1] + t[1]) / 2;
    console.log(`${(el.name || '(unnamed)').padEnd(14)} from[${f.map(v => v.toFixed(1))}] to[${t.map(v => v.toFixed(1))}] yCenter=${yc.toFixed(1)} rot=${rot} origin=${origin.map(v => (typeof v === 'number' ? v.toFixed(1) : v))}`);
}

// 按翻转公式(24 - y) 排序查看翻转后的垂直分布
console.log('\n=== ELEMENTS flipped to render space (mcY = 24 - rawY), sorted by mcY center ===');
const flipped = bb.elements.map(el => {
    const f = el.from, t = el.to;
    return {
        name: el.name || '(unnamed)',
        mcYcenter: 24 - (f[1] + t[1]) / 2,
        mcYrange: [24 - t[1], 24 - f[1]],
        mcXrange: [-t[0], -f[0]],
        mcZrange: [f[2], t[2]]
    };
}).sort((a, b) => a.mcYcenter - b.mcYcenter);
for (const e of flipped) {
    console.log(`${e.name.padEnd(14)} mcY ${e.mcYrange[0].toFixed(1)}-${e.mcYrange[1].toFixed(1)}  x ${e.mcXrange[0].toFixed(1)}-${e.mcXrange[1].toFixed(1)}  z ${e.mcZrange[0].toFixed(1)}-${e.mcZrange[1].toFixed(1)}`);
}

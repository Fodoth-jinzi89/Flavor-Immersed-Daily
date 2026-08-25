// 对比 cooked_whole_sheepN.java（导出原件）与 CookedWholeSheepModelN.java（当前项目模型类）的 createBodyLayer 内容
const fs = require('fs');
const path = require('path');

function extractBody(src) {
    const marker = 'public static LayerDefinition createBodyLayer() {';
    const idx = src.indexOf(marker);
    if (idx < 0) return null;
    let depth = 0, start = idx + marker.length;
    for (let i = start; i < src.length; i++) {
        if (src[i] === '{') depth++;
        else if (src[i] === '}') { depth--; if (depth === 0) return src.slice(start, i); }
    }
    return null;
}

for (let stage = 0; stage <= 9; stage++) {
    const exportPath = path.join(__dirname, `cooked_whole_sheep${stage}.java`);
    const modelPath = path.join(__dirname, '..', 'src', 'main', 'java', 'com', 'flavor_immersed_daily', 'client', 'model', `CookedWholeSheepModel${stage}.java`);
    const exp = fs.readFileSync(exportPath, 'utf8');
    const mdl = fs.readFileSync(modelPath, 'utf8');
    const expBody = extractBody(exp);
    const mdlBody = extractBody(mdl);
    if (expBody === null || mdlBody === null) { console.log(`stage ${stage}: FAILED to extract`); continue; }
    const norm = s => s.replace(/\s+/g, ' ').trim();
    if (norm(expBody) === norm(mdlBody)) {
        console.log(`stage ${stage}: 一致 ✓`);
    } else {
        console.log(`stage ${stage}: 不一致 ✗ (export ${expBody.length} chars vs model ${mdlBody.length} chars)`);
        // 找出第一个不同点
        const a = norm(expBody), b = norm(mdlBody);
        let diff = -1;
        for (let i = 0; i < Math.min(a.length, b.length); i++) {
            if (a[i] !== b[i]) { diff = i; break; }
        }
        if (diff >= 0) {
            console.log(`   export 附近: ...${a.slice(Math.max(0, diff - 40), diff + 60)}...`);
            console.log(`   model  附近: ...${b.slice(Math.max(0, diff - 40), diff + 60)}...`);
        }
    }
}
